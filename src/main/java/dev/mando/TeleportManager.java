package dev.mando;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.*;

public final class TeleportManager implements Listener {
  private final MandoPlugin plugin;
  private final Map<UUID,Pending> pending=new HashMap<>();
  private final Map<UUID,Long> cooldown=new HashMap<>();
  private record Pending(Location from,Location to,long token){}
  public TeleportManager(MandoPlugin plugin){this.plugin=plugin;}
  public void teleport(Player p,Location to,String permissionBypass,Runnable success){
    if(to==null||to.getWorld()==null){p.sendMessage("§cDestino no disponible.");return;}
    int cd=plugin.getConfig().getInt("teleport.cooldown",5);
    long now=System.currentTimeMillis(),until=cooldown.getOrDefault(p.getUniqueId(),0L);
    if(cd>0&&until>now&&!p.hasPermission(permissionBypass)){p.sendMessage("§cEspera "+Math.max(1,(until-now+999)/1000)+"s para volver a teletransportarte.");return;}
    int warm=Math.max(0,plugin.getConfig().getInt("teleport.warmup",3));
    if(warm==0||p.hasPermission("mando.teleport.bypass-warmup")){execute(p,to,cd,success);return;}
    long token=System.nanoTime();pending.put(p.getUniqueId(),new Pending(p.getLocation().clone(),to.clone(),token));
    p.sendMessage("§6Mando §8» §fTeletransporte en §e"+warm+"s§f. No te muevas ni recibas daño.");
    Bukkit.getScheduler().runTaskLater(plugin,()->{Pending q=pending.get(p.getUniqueId());if(q!=null&&q.token()==token){pending.remove(p.getUniqueId());execute(p,q.to(),cd,success);}},warm*20L);
  }
  private void execute(Player p,Location to,int cd,Runnable success){if(!p.isOnline()||to.getWorld()==null)return;Location checked=safeStored(to);if(checked==null){p.sendMessage("§cEl destino ya no es seguro.");return;}p.teleportAsync(checked).thenAccept(ok->{if(ok){if(cd>0)cooldown.put(p.getUniqueId(),System.currentTimeMillis()+cd*1000L);if(success!=null)Bukkit.getScheduler().runTask(plugin,success);}else p.sendMessage("§cNo se pudo teletransportar.");});}
  private Location safeStored(Location to){World w=to.getWorld();if(w==null||to.getY()<=w.getMinHeight()+1||to.getY()>=w.getMaxHeight()-2||!w.getWorldBorder().isInside(to))return null;if(to.getBlock().isPassable()&&to.clone().add(0,1,0).getBlock().isPassable())return to;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-2;dy<=3;dy++){Location n=to.clone().add(dx,dy,dz);if(n.getY()<=w.getMinHeight()+1||n.getY()>=w.getMaxHeight()-2)continue;if(n.getBlock().isPassable()&&n.clone().add(0,1,0).getBlock().isPassable()&&n.clone().add(0,-1,0).getBlock().getType().isSolid())return n;}return null;}
  private void cancel(Player p,String why){if(pending.remove(p.getUniqueId())!=null)p.sendMessage("§cTeletransporte cancelado: "+why+".");}
  @EventHandler(ignoreCancelled=true) public void move(PlayerMoveEvent e){if(!plugin.getConfig().getBoolean("teleport.cancel-on-move",true))return;Pending q=pending.get(e.getPlayer().getUniqueId());if(q==null||e.getTo()==null)return;if(q.from().distanceSquared(e.getTo())>0.04)cancel(e.getPlayer(),"te moviste");}
  @EventHandler(ignoreCancelled=true) public void damage(EntityDamageEvent e){if(plugin.getConfig().getBoolean("teleport.cancel-on-damage",true)&&e.getEntity() instanceof Player p)cancel(p,"recibiste daño");}
  public void cancel(UUID id){pending.remove(id);}
}
