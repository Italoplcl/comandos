package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.entity.EntityDamageEvent;import org.bukkit.event.player.PlayerQuitEvent;import java.util.*;
public final class SetupProtectionListener implements Listener{
 private final MandoPlugin plugin;private final Set<UUID> protectedPlayers=new HashSet<>();
 public SetupProtectionListener(MandoPlugin p){plugin=p;}
 public void begin(Player p){protectedPlayers.add(p.getUniqueId());Bukkit.getScheduler().runTaskLater(plugin,()->protectedPlayers.remove(p.getUniqueId()),20L*300);}
 public void end(Player p){protectedPlayers.remove(p.getUniqueId());}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&protectedPlayers.contains(p.getUniqueId()))e.setCancelled(true);}
 @EventHandler public void quit(PlayerQuitEvent e){protectedPlayers.remove(e.getPlayer().getUniqueId());}
}
