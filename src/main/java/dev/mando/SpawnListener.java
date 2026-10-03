package dev.mando;
import org.bukkit.Location;import org.bukkit.event.*;import org.bukkit.event.player.PlayerRespawnEvent;
public final class SpawnListener implements Listener{
 private final MandoPlugin plugin;public SpawnListener(MandoPlugin p){plugin=p;}
 @EventHandler(priority=EventPriority.HIGH) public void respawn(PlayerRespawnEvent e){if(!plugin.getConfig().getBoolean("spawn.use-for-respawn",true))return;if(e.isBedSpawn()||e.isAnchorSpawn())return;Location l=plugin.spawn().get();if(l!=null)e.setRespawnLocation(l);}
}