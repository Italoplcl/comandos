package dev.esslite;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

public final class HomeManager {

    public record Home(String world, double x, double y, double z, float yaw, float pitch, String icon) {
        Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z, yaw, pitch);
        }

        static Home of(Location l) {
            return new Home(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch(), autoIcon(l));
        }
        public Home withIcon(String material) { return new Home(world, x, y, z, yaw, pitch, material); }
        private static String autoIcon(Location l) { return switch (l.getWorld().getEnvironment()) { case NETHER -> "NETHERRACK"; case THE_END -> "END_STONE"; default -> "GRASS_BLOCK"; }; }
    }

    private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{1,16}");

    private final EssLite plugin;
    private final File file;
    private final Object ioLock = new Object();
    private TeleportService teleports;
    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();
    private final Map<UUID, Map<String, Home>> deleted = new HashMap<>();
    private final Map<UUID, Map<String, Home>> previous = new HashMap<>();

    public HomeManager(EssLite plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

    public void teleports(TeleportService value) { this.teleports = value; }

    /** @return el nombre normalizado o null si es invalido. */
    public static String normalize(String raw) {
        String n = raw.toLowerCase(Locale.ROOT);
        return VALID.matcher(n).matches() ? n : null;
    }

    public Map<String, Home> all(UUID id) {
        Map<String, Home> m = homes.get(id);
        return m == null ? Map.of() : Collections.unmodifiableMap(m);
    }

    public void set(UUID id, String name, Home home) {
        Map<String, Home> map = homes.computeIfAbsent(id, k -> new TreeMap<>());
        Home old = map.put(name, home);
        if (old != null) previous.computeIfAbsent(id, k -> new TreeMap<>()).put(name, old);
        saveAsync();
    }

    public Home deleted(UUID id, String name) { Map<String, Home> m=deleted.get(id); return m==null?null:m.get(name); }
    public Home previous(UUID id, String name) { Map<String, Home> m=previous.get(id); return m==null?null:m.get(name); }
    public boolean restoreDeleted(UUID id,String name){ Home h=deleted(id,name); if(h==null)return false; set(id,name,h); deleted.get(id).remove(name); return true; }
    public boolean restorePrevious(UUID id,String name){ Home h=previous(id,name); if(h==null)return false; set(id,name,h); previous.get(id).remove(name); return true; }

    public boolean remove(UUID id, String name) {
        Map<String, Home> m = homes.get(id);
        if (m == null) return false;
        Home removed=m.remove(name); if (removed == null) return false;
        deleted.computeIfAbsent(id,k->new TreeMap<>()).put(name,removed);
        if (m.isEmpty()) homes.remove(id);
        saveAsync();
        return true;
    }

    public void teleport(Player p, String name, Home home) {
        Location loc = home.toLocation();
        if (loc == null) {
            p.sendMessage(plugin.msg("home-world-missing"));
            return;
        }
        if (teleports == null) { p.sendMessage(plugin.msg("teleport-failed")); return; }
        teleports.teleport(p, () -> home.toLocation(), TeleportService.Kind.STORED, "home",
                () -> p.sendMessage(plugin.msg("home-teleported", Placeholder.unparsed("name", name))));
    }

    // ---------- persistencia ----------

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String u : yml.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(u);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection sec = yml.getConfigurationSection(u);
            if (sec == null) continue;
            Map<String, Home> map = new TreeMap<>();
            for (String name : sec.getKeys(false)) {
                ConfigurationSection h = sec.getConfigurationSection(name);
                if (h == null || h.getString("world") == null) continue;
                map.put(name, new Home(h.getString("world"), h.getDouble("x"), h.getDouble("y"),
                        h.getDouble("z"), (float) h.getDouble("yaw"), (float) h.getDouble("pitch"), h.getString("icon", "GRASS_BLOCK")));
            }
            if (!map.isEmpty()) homes.put(id, map);
        }
    }

    private String serialize() {
        YamlConfiguration yml = new YamlConfiguration();
        for (var owner : homes.entrySet()) {
            for (var h : owner.getValue().entrySet()) {
                String base = owner.getKey() + "." + h.getKey() + ".";
                Home v = h.getValue();
                yml.set(base + "world", v.world());
                yml.set(base + "x", v.x());
                yml.set(base + "y", v.y());
                yml.set(base + "z", v.z());
                yml.set(base + "yaw", v.yaw());
                yml.set(base + "pitch", v.pitch());
                yml.set(base + "icon", v.icon());
            }
        }
        return yml.saveToString();
    }

    /** Serializa en el hilo principal y escribe el archivo en otro hilo. */
    private void saveAsync() {
        String data = serialize();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> write(data));
    }

    public void saveNow() {
        write(serialize());
    }

    private void write(String data) {
        synchronized (ioLock) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Path tmp = new File(file.getParentFile(), "homes.yml.tmp").toPath();
                Files.writeString(tmp, data, StandardCharsets.UTF_8);
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().severe("No se pudo guardar homes.yml: " + ex.getMessage());
            }
        }
    }
}
