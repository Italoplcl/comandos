package dev.mando;
import org.bukkit.Location;import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerRespawnEvent;import org.bukkit.event.player.PlayerJoinEvent;
public final class SpawnListener implements Listener{
 private final MandoPlugin plugin;public SpawnListener(MandoPlugin p){plugin=p;}
 @EventHandler public void join(PlayerJoinEvent e){Player p=e.getPlayer();if(p.hasPlayedBefore()||!plugin.getConfig().getBoolean("spawn.first-join",true)||!plugin.integrations().isMandoProvider("first-join-spawn"))return;Location l=plugin.spawn().get();if(l!=null)afterAuth(p,l,0);}
 private void afterAuth(Player p,Location l,int tries){Bukkit.getScheduler().runTaskLater(plugin,()->{if(!p.isOnline())return;if(plugin.authGuard().authenticated(p)){p.teleportAsync(l);return;}if(tries<120)afterAuth(p,l,tries+1);},10L);}
 @EventHandler(priority=EventPriority.HIGH) public void respawn(PlayerRespawnEvent e){if(!plugin.getConfig().getBoolean("spawn.use-for-respawn",true)||!plugin.integrations().isMandoProvider("respawn"))return;if(e.isBedSpawn()||e.isAnchorSpawn())return;Location l=plugin.spawn().get();if(l!=null)e.setRespawnLocation(l);}
}