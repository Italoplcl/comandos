package dev.esslite;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Rtp {

    private static final Set<Material> UNSAFE = EnumSet.of(
            Material.LAVA, Material.WATER, Material.CACTUS, Material.MAGMA_BLOCK,
            Material.FIRE, Material.SOUL_FIRE, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.COBWEB,
            Material.POINTED_DRIPSTONE, Material.WITHER_ROSE,
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE);

    private final EssLite plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> searching = new HashSet<>();
    private TeleportService teleports;

    public Rtp(EssLite plugin) {
        this.plugin = plugin;
    }

    public void teleports(TeleportService value) { this.teleports = value; }

    public void start(Player p) {
        FileConfiguration cfg = plugin.getConfig();
        World world = p.getWorld();
        UUID id = p.getUniqueId();

        if (!cfg.getStringList("rtp.worlds").contains(world.getName())
                || world.getEnvironment() != World.Environment.NORMAL) {
            p.sendMessage(plugin.msg("rtp-world"));
            return;
        }
        if (searching.contains(id)) {
            p.sendMessage(plugin.msg("rtp-busy"));
            return;
        }

        int cd = cfg.getInt("rtp.cooldown-seconds", 30);
        Long last = cooldowns.get(id);
        if (cd > 0 && last != null && !p.hasPermission("esslite.rtp.bypass")) {
            long left = cd - (System.currentTimeMillis() - last) / 1000L;
            if (left > 0) {
                p.sendMessage(plugin.msg("rtp-cooldown", Placeholder.unparsed("seconds", String.valueOf(left))));
                return;
            }
        }

        searching.add(id);
        p.sendMessage(plugin.msg("rtp-searching"));
        attempt(p, world, Math.max(1, cfg.getInt("rtp.max-attempts", 15)));
    }

    private void attempt(Player p, World world, int left) {
        UUID id = p.getUniqueId();
        if (!p.isOnline() || !p.getWorld().equals(world)) {
            searching.remove(id);
            return;
        }
        if (left <= 0) {
            searching.remove(id);
            p.sendMessage(plugin.msg("rtp-fail"));
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double borderHalf = world.getWorldBorder().getSize() / 2.0;
        double max = Math.min(cfg.getDouble("rtp.max-radius", 5000), borderHalf);
        double min = Math.min(cfg.getDouble("rtp.min-radius", 500), Math.max(0, max - 1));

        Location center = cfg.getBoolean("rtp.center-on-player", false) ? p.getLocation() : world.getSpawnLocation();
        double angle = rnd.nextDouble() * Math.PI * 2;
        // sqrt => distribucion uniforme por area (no se amontona en el centro)
        double dist = Math.sqrt(min * min + rnd.nextDouble() * (max * max - min * min));
        int x = (int) Math.floor(center.getX() + Math.cos(angle) * dist);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * dist);

        if (!world.getWorldBorder().isInside(new Location(world, x + 0.5, 64, z + 0.5))) {
            attempt(p, world, left - 1);
            return;
        }

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Location dest = findSafe(world, x, z, p.getLocation());
            if (dest == null) {
                attempt(p, world, left - 1);
                return;
            }
            searching.remove(id);
            if (teleports == null) { p.sendMessage(plugin.msg("teleport-failed")); return; }
            Location chosen = dest.clone();
            teleports.teleport(p, () -> chosen, TeleportService.Kind.RTP, "rtp", () -> {
                cooldowns.put(id, System.currentTimeMillis());
                p.sendMessage(plugin.msg("rtp-success",
                        Placeholder.unparsed("x", String.valueOf(chosen.getBlockX())),
                        Placeholder.unparsed("y", String.valueOf(chosen.getBlockY())),
                        Placeholder.unparsed("z", String.valueOf(chosen.getBlockZ()))));
            });
        }).exceptionally(ex -> {
            searching.remove(id);
            p.sendMessage(plugin.msg("rtp-fail"));
            return null;
        });
    }

    private static Location findSafe(World world, int x, int z, Location look) {
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (y <= world.getMinHeight() || y + 2 >= world.getMaxHeight()) return null;

        Block ground = world.getBlockAt(x, y, z);
        Material gm = ground.getType();
        if (!gm.isSolid() || UNSAFE.contains(gm)) return null;

        Block feet = ground.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (!isFree(feet) || !isFree(head)) return null;

        return new Location(world, x + 0.5, y + 1, z + 0.5, look.getYaw(), look.getPitch());
    }

    private static boolean isFree(Block b) {
        return b.isPassable() && !b.isLiquid() && !UNSAFE.contains(b.getType());
    }
}
