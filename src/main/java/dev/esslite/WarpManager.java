package dev.esslite;

import dev.esslite.HomeManager.Home;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class WarpManager {

    private final EssLite plugin;
    private final File file;
    private final Object ioLock = new Object();
    private final Map<String, Home> warps = new TreeMap<>();

    public WarpManager(EssLite plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "warps.yml");
        load();
    }

    public Map<String, Home> all() {
        return Collections.unmodifiableMap(warps);
    }

    public Home get(String name) {
        return warps.get(name);
    }

    public void set(String name, Home home) {
        warps.put(name, home);
        saveAsync();
    }

    public boolean remove(String name) {
        if (warps.remove(name) == null) return false;
        saveAsync();
        return true;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String name : yml.getKeys(false)) {
            ConfigurationSection h = yml.getConfigurationSection(name);
            if (h == null || h.getString("world") == null) continue;
            warps.put(name, new Home(h.getString("world"), h.getDouble("x"), h.getDouble("y"),
                    h.getDouble("z"), (float) h.getDouble("yaw"), (float) h.getDouble("pitch"), h.getString("icon", "ENDER_PEARL")));
        }
    }

    private String serialize() {
        YamlConfiguration yml = new YamlConfiguration();
        for (var e : warps.entrySet()) {
            String base = e.getKey() + ".";
            Home v = e.getValue();
            yml.set(base + "world", v.world());
            yml.set(base + "x", v.x());
            yml.set(base + "y", v.y());
            yml.set(base + "z", v.z());
            yml.set(base + "yaw", v.yaw());
            yml.set(base + "pitch", v.pitch());
            yml.set(base + "icon", v.icon());
        }
        return yml.saveToString();
    }

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
                Path tmp = new File(file.getParentFile(), "warps.yml.tmp").toPath();
                Files.writeString(tmp, data, StandardCharsets.UTF_8);
                Files.move(tmp, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().severe("No se pudo guardar warps.yml: " + ex.getMessage());
            }
        }
    }
}
