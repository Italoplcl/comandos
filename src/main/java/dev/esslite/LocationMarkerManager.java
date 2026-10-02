package dev.esslite;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

/** Marcadores cosméticos. Nunca son la fuente de verdad de homes/warps/spawn. */
public final class LocationMarkerManager implements Listener {
    private final EssLite plugin;
    private final NamespacedKey markerKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey nameKey;

    public LocationMarkerManager(EssLite plugin) {
        this.plugin = plugin;
        markerKey = new NamespacedKey(plugin, "location_marker");
        typeKey = new NamespacedKey(plugin, "marker_type");
        nameKey = new NamespacedKey(plugin, "marker_name");
        if (plugin.getConfig().getBoolean("markers.spawn.particles.enabled", true)) {
            long interval = Math.max(10L, plugin.getConfig().getLong("markers.spawn.particles.interval-ticks", 20L));
            Bukkit.getScheduler().runTaskTimer(plugin, this::spawnParticles, interval, interval);
        }
    }

    public boolean createHomeSign(Player p, String homeName) {
        if (!plugin.getConfig().getBoolean("markers.homes.enabled", true)) return true;
        int distance = plugin.getConfig().getInt("markers.homes.max-distance", 5);
        Block target = p.getTargetBlockExact(distance);
        BlockFace face = p.getTargetBlockFace(distance);
        if (target == null || face == null) return false;
        Block place = target.getRelative(face);
        if (!place.isEmpty()) return false;
        Material mat = face == BlockFace.DOWN ? Material.OAK_HANGING_SIGN : (face == BlockFace.UP ? Material.OAK_SIGN : Material.OAK_WALL_SIGN);
        BlockData candidate = mat.createBlockData();
        if (candidate instanceof Directional d && face != BlockFace.UP && face != BlockFace.DOWN) {
            d.setFacing(face);
        } else if (candidate instanceof Rotatable r) {
            r.setRotation(cardinal(p.getLocation().getYaw()));
        }
        // Pregunta a la propia API si el cartel sobreviviría en esta posición.
        // Evita carteles flotando sobre nieve, alfombras y geometrías parciales sin mantener listas manuales.
        if (!candidate.isSupported(place)) return false;
        place.setBlockData(candidate, false);
        if (!(place.getState() instanceof Sign sign)) { place.setType(Material.AIR, false); return false; }
        String deco = plugin.getConfig().getString("markers.homes.decoration", "--------");
        for (Side signFace : Side.values()) {
            SignSide side = sign.getSide(signFace);
            side.line(0, net.kyori.adventure.text.Component.text(deco));
            side.line(1, net.kyori.adventure.text.Component.text(homeName));
            side.line(2, net.kyori.adventure.text.Component.text(p.getName()));
            side.line(3, net.kyori.adventure.text.Component.text(deco));
            side.setGlowingText(plugin.getConfig().getBoolean("markers.homes.glowing", true));
        }
        mark(sign, "home", p.getUniqueId() + ":" + homeName);
        sign.update(true, false);
        return true;
    }

    public boolean createWarpBanner(Player p, String warpName, DyeColor color) {
        if (!plugin.getConfig().getBoolean("markers.warps.enabled", true)) return true;
        Block feet = p.getLocation().getBlock();
        if (!feet.isEmpty() || !feet.getRelative(BlockFace.DOWN).getType().isSolid()) return false;
        BlockData candidate = bannerMaterial(color).createBlockData();
        if (candidate instanceof Rotatable rotatable) rotatable.setRotation(cardinal(p.getLocation().getYaw()));
        if (!candidate.isSupported(feet)) return false;
        feet.setBlockData(candidate, false);
        if (!(feet.getState() instanceof Banner banner)) { feet.setType(Material.AIR, false); return false; }
        mark(banner, "warp", warpName);
        banner.update(true, false);
        return true;
    }

    public boolean createSpawnBanner(Player p) {
        if (!plugin.getConfig().getBoolean("markers.spawn.enabled", true)) return true;
        Block feet = p.getLocation().getBlock();
        if (!feet.isEmpty() || !feet.getRelative(BlockFace.DOWN).getType().isSolid()) return false;
        BlockData candidate = Material.LIME_BANNER.createBlockData();
        if (candidate instanceof Rotatable rotatable) rotatable.setRotation(cardinal(p.getLocation().getYaw()));
        if (!candidate.isSupported(feet)) return false;
        feet.setBlockData(candidate, false);
        if (!(feet.getState() instanceof Banner banner)) { feet.setType(Material.AIR, false); return false; }
        mark(banner, "spawn", p.getWorld().getName());
        banner.update(true, false);
        plugin.getConfig().set("markers.spawn.location.world", feet.getWorld().getName());
        plugin.getConfig().set("markers.spawn.location.x", feet.getX());
        plugin.getConfig().set("markers.spawn.location.y", feet.getY());
        plugin.getConfig().set("markers.spawn.location.z", feet.getZ());
        plugin.saveConfig();
        return true;
    }

    private void mark(TileState state, String type, String name) {
        PersistentDataContainer pdc = state.getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte)1);
        pdc.set(typeKey, PersistentDataType.STRING, type);
        pdc.set(nameKey, PersistentDataType.STRING, name);
    }

    @EventHandler public void onBreak(BlockBreakEvent e) {
        if (!(e.getBlock().getState() instanceof TileState state)) return;
        if (!state.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) return;
        // El marcador es cosmético: romperlo jamás elimina la ubicación real.
        e.setDropItems(false);
    }

    private void spawnParticles() {
        String worldName = plugin.getConfig().getString("markers.spawn.location.world", "");
        if (worldName == null || worldName.isBlank()) return;
        World w = Bukkit.getWorld(worldName); if (w == null) return;
        int x=plugin.getConfig().getInt("markers.spawn.location.x"), y=plugin.getConfig().getInt("markers.spawn.location.y"), z=plugin.getConfig().getInt("markers.spawn.location.z");
        Location loc = new Location(w, x + .5, y + 1.2, z + .5);
        double view = plugin.getConfig().getDouble("markers.spawn.particles.view-distance", 16.0);
        boolean nearby = w.getPlayers().stream().anyMatch(p -> p.getLocation().distanceSquared(loc) <= view * view);
        if (!nearby) return;
        w.spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, .65, .45, .65, 0.0);
    }

    private static BlockFace cardinal(float yaw) {
        int i = Math.floorMod(Math.round(yaw / 45f), 8);
        return new BlockFace[]{BlockFace.SOUTH,BlockFace.SOUTH_WEST,BlockFace.WEST,BlockFace.NORTH_WEST,BlockFace.NORTH,BlockFace.NORTH_EAST,BlockFace.EAST,BlockFace.SOUTH_EAST}[i];
    }
    private static Material bannerMaterial(DyeColor c) { return Material.valueOf(c.name().toUpperCase(Locale.ROOT) + "_BANNER"); }
}
