package dev.mando;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Recuerda la ultima ubicacion (antes de teletransportarse o al morir). Solo en memoria. */
public final class BackListener implements Listener {

    private final Map<UUID, Location> backs = new HashMap<>();

    public Location get(UUID id) {
        Location l = backs.get(id);
        return l == null ? null : l.clone();
    }

    public void set(UUID id, Location loc) {
        backs.put(id, loc.clone());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        boolean sameWorld = from.getWorld().equals(to.getWorld());
        if (sameWorld && from.distanceSquared(to) < 1) return;
        set(e.getPlayer().getUniqueId(), from);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        set(e.getPlayer().getUniqueId(), e.getPlayer().getLocation());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        backs.remove(e.getPlayer().getUniqueId());
    }
}
