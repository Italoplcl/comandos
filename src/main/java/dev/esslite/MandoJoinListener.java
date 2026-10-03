package dev.esslite;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerJoinEvent;import java.util.*;
public final class MandoJoinListener implements Listener{
 private final EssLite plugin;private final MandoCommand mando;private final Set<UUID> shown=new HashSet<>();
 public MandoJoinListener(EssLite p,MandoCommand m){plugin=p;mando=m;}
 @EventHandler public void onJoin(PlayerJoinEvent e){Player p=e.getPlayer();if(!p.isOp()||!plugin.getConfig().getBoolean("setup.show-to-op",true)||plugin.getConfig().getBoolean("setup.completed",false)||!shown.add(p.getUniqueId()))return;Bukkit.getScheduler().runTaskLater(plugin,()->{if(p.isOnline())mando.showSetup(p,true);},40L);}
}
