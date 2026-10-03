package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerJoinEvent;import java.util.*;
public final class MandoJoinListener implements Listener{
 private final MandoPlugin plugin;private final MandoCommand mando;private final Set<UUID> shown=new HashSet<>();
 public MandoJoinListener(MandoPlugin p,MandoCommand m){plugin=p;mando=m;}
 @EventHandler public void onJoin(PlayerJoinEvent e){
  Player p=e.getPlayer();if(!p.isOp()||!plugin.getConfig().getBoolean("setup.show-to-op",true)||shown.contains(p.getUniqueId()))return;
  waitAuthenticated(p,0);
 }
 private void waitAuthenticated(Player p,int attempt){
  Bukkit.getScheduler().runTaskLater(plugin,()->{
   if(!p.isOnline())return;
   if(plugin.authGuard().authenticated(p)){if(plugin.integrations().changedSinceLastStart())p.sendMessage("§6Mando §8» §eCambió el ecosistema de plugins desde el último inicio. Revisa §f/mando status §eantes de cambiar proveedores.");if(!plugin.getConfig().getBoolean("setup.completed",false)&&shown.add(p.getUniqueId()))mando.showSetup(p,true);return;}
   if(attempt<30)waitAuthenticated(p,attempt+1);
  },attempt==0?40L:20L);
 }
}