package dev.esslite;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlyListener implements Listener {

    private final Set<UUID> flyers = new HashSet<>();

    /** @return true si quedo activado. */
    public boolean toggle(Player p) {
        UUID id = p.getUniqueId();
        if (flyers.remove(id)) {
            p.setFlying(false);
            p.setAllowFlight(false);
            return false;
        }
        flyers.add(id);
        p.setAllowFlight(true);
        return true;
    }

    public void disableAll() {
        for (UUID id : flyers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        flyers.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (flyers.remove(p.getUniqueId())) {
            p.setFlying(false);
            p.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent e) {
        // El servidor gestiona el vuelo segun el modo de juego: dejamos de controlarlo.
        flyers.remove(e.getPlayer().getUniqueId());
    }
}
