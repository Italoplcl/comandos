package dev.mando;
import org.bukkit.Location;import org.bukkit.Bukkit;import org.bukkit.event.*;import org.bukkit.event.player.PlayerRespawnEvent;import org.bukkit.event.player.PlayerJoinEvent;
public final class SpawnListener implements Listener{
 private final MandoPlugin plugin;public SpawnListener(MandoPlugin p){plugin=p;}
 @EventHandler public void join(PlayerJoinEvent e){if(e.getPlayer().hasPlayedBefore()||!plugin.getConfig().getBoolean("spawn.first-join",true))return;Location l=plugin.spawn().get();if(l!=null)Bukkit.getScheduler().runTask(plugin,()->{if(e.getPlayer().isOnline())e.getPlayer().teleportAsync(l);});}
 @EventHandler(priority=EventPriority.HIGH) public void respawn(PlayerRespawnEvent e){if(!plugin.getConfig().getBoolean("spawn.use-for-respawn",true))return;if(e.isBedSpawn()||e.isAnchorSpawn())return;Location l=plugin.spawn().get();if(l!=null)e.setRespawnLocation(l);}
}