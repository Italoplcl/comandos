package dev.mando;

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

    public record Home(String world, double x, double y, double z, float yaw, float pitch) {
        Location toLocation() {
            World w = Bukkit.getWorld(world);
            return w == null ? null : new Location(w, x, y, z, yaw, pitch);
        }

        static Home of(Location l) {
            return new Home(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), 0.0f);
        }
    }

    private static final Pattern VALID = Pattern.compile("[a-z0-9_-]{1,16}");

    private final MandoPlugin plugin;
    private final File file;
    private final Object ioLock = new Object();\n    private final File historyFile;\n    private final YamlConfiguration history;
    private final Map<UUID, Map<String, Home>> homes = new HashMap<>();

    public HomeManager(MandoPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        load();
    }

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
        homes.computeIfAbsent(id, k -> new TreeMap<>()).put(name, home);
        saveAsync();
    }

    public boolean remove(UUID id, String name) {
        Map<String, Home> m = homes.get(id);
        if (m == null) return false;\n        Home removed=m.get(name); if(removed==null)return false;\n        archive(id,name,removed); m.remove(name);
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
        p.teleportAsync(loc).thenAccept(ok -> p.sendMessage(
                plugin.msg(ok ? "home-teleported" : "teleport-failed", Placeholder.unparsed("name", name))));
    }

    public java.util.List<String> deleted(UUID id){return new java.util.ArrayList<>(history.getStringList(id+".deleted-order"));}\n    public Home deletedHome(UUID id,String key){return readHome(history,id+".deleted."+key+".location");}\n    public boolean restoreDeleted(UUID id,String key,String asName){Home h=deletedHome(id,key);if(h==null||homes.getOrDefault(id,Map.of()).containsKey(asName))return false;homes.computeIfAbsent(id,k->new TreeMap<>()).put(asName,h);history.set(id+".deleted."+key,null);java.util.List<String> order=new java.util.ArrayList<>(history.getStringList(id+".deleted-order"));order.remove(key);history.set(id+".deleted-order",order);saveHistory();saveAsync();return true;}\n    private void archive(UUID id,String name,Home h){String key=System.currentTimeMillis()+"-"+name;writeHome(history,id+".deleted."+key+".location",h);history.set(id+".deleted."+key+".name",name);history.set(id+".deleted."+key+".deleted-at",java.time.Instant.now().toString());java.util.List<String> order=new java.util.ArrayList<>(history.getStringList(id+".deleted-order"));order.add(0,key);int max=plugin.getConfig().getInt("homes.deleted-history",5);while(order.size()>max){String old=order.remove(order.size()-1);history.set(id+".deleted."+old,null);}history.set(id+".deleted-order",order);saveHistory();}\n    private void writeHome(YamlConfiguration y,String b,Home h){y.set(b+".world",h.world());y.set(b+".x",h.x());y.set(b+".y",h.y());y.set(b+".z",h.z());y.set(b+".yaw",h.yaw());y.set(b+".pitch",h.pitch());}\n    private Home readHome(YamlConfiguration y,String b){String w=y.getString(b+".world");if(w==null)return null;return new Home(w,y.getDouble(b+".x"),y.getDouble(b+".y"),y.getDouble(b+".z"),(float)y.getDouble(b+".yaw"),(float)y.getDouble(b+".pitch"));}\n    private synchronized void saveHistory(){try{historyFile.getParentFile().mkdirs();history.save(historyFile);}catch(IOException e){plugin.getLogger().warning("Home history: "+e.getMessage());}}\n\n    // ---------- persistencia ----------

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
                        h.getDouble("z"), (float) h.getDouble("yaw"), (float) h.getDouble("pitch")));
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
