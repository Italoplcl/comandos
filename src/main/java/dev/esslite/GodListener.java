package dev.esslite;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GodListener implements Listener {

    private final Set<UUID> gods = new HashSet<>();

    /** @return true si quedo activado. */
    public boolean toggle(Player p) {
        UUID id = p.getUniqueId();
        if (gods.remove(id)) {
            p.setInvulnerable(false);
            return false;
        }
        gods.add(id);
        p.setInvulnerable(true);
        fill(p);
        return true;
    }

    public static void fill(Player p) {
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setExhaustion(0f);
    }

    public void disableAll() {
        for (UUID id : gods) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            if (p != null) p.setInvulnerable(false);
        }
        gods.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && gods.contains(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && gods.contains(p.getUniqueId())) {
            e.setCancelled(true);
            fill(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (gods.remove(p.getUniqueId())) {
            // setInvulnerable se guarda en los datos del jugador: hay que limpiarlo al salir.
            p.setInvulnerable(false);
        }
    }
}
