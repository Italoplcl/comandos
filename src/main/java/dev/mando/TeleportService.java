package dev.mando;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.function.Supplier;

public final class TeleportService implements Listener {
 public enum Kind { STORED, PLAYER, RTP, ADMIN }
 private static final Set<Material> HAZARDS=EnumSet.of(Material.LAVA,Material.WATER,Material.CACTUS,Material.MAGMA_BLOCK,Material.FIRE,Material.SOUL_FIRE,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.SWEET_BERRY_BUSH,Material.POWDER_SNOW,Material.COBWEB,Material.POINTED_DRIPSTONE,Material.WITHER_ROSE);
 private record Pending(Location origin,BukkitTask task){}
 private final Mando plugin;private final BackListener back;private final Map<UUID,Pending> pending=new HashMap<>();private final Map<UUID,Map<String,Long>> cooldowns=new HashMap<>();
 public TeleportService(Mando p,BackListener b){plugin=p;back=b;}
 public void teleport(Player p,Supplier<Location> target,Kind kind,String channel,Runnable success){
  cancel(p,false);long left=cooldownLeft(p,channel);if(left>0){p.sendMessage(plugin.msg("teleport-cooldown",Placeholder.unparsed("seconds",String.valueOf(left))));return;}
  int warm=p.hasPermission("mando.teleport.bypass-warmup")?0:plugin.getConfig().getInt("teleport."+channel+".warmup-seconds",plugin.getConfig().getInt("teleport.default-warmup-seconds",0));
  Location origin=p.getLocation().clone();Runnable run=()->execute(p,origin,target,kind,channel,success);
  if(warm<=0){run.run();return;}p.sendMessage(plugin.msg("teleport-warmup",Placeholder.unparsed("seconds",String.valueOf(warm))));
  BukkitTask task=Bukkit.getScheduler().runTaskLater(plugin,run,warm*20L);pending.put(p.getUniqueId(),new Pending(origin,task));
 }
 private void execute(Player p,Location origin,Supplier<Location> supplier,Kind kind,String channel,Runnable success){
  pending.remove(p.getUniqueId());if(!p.isOnline())return;Location raw=supplier.get();if(raw==null||raw.getWorld()==null){p.sendMessage(plugin.msg("teleport-invalid"));return;}
  Location dest=switch(kind){case STORED->safeStored(raw,3);case PLAYER->strict(raw);case RTP,ADMIN->strict(raw);};if(dest==null){p.sendMessage(plugin.msg("teleport-unsafe"));return;}
  Location actualOrigin=p.getLocation().clone();p.teleportAsync(dest).thenAccept(ok->{if(ok){back.set(p.getUniqueId(),actualOrigin);cooldowns.computeIfAbsent(p.getUniqueId(),k->new HashMap<>()).put(channel,System.currentTimeMillis());if(success!=null)success.run();}else p.sendMessage(plugin.msg("teleport-failed"));});
 }
 private Location safeStored(Location center,int radius){Location exact=strict(center);if(exact!=null)return exact;World w=center.getWorld();for(int r=1;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;Location c=new Location(w,center.getBlockX()+dx+.5,center.getY(),center.getBlockZ()+dz+.5,center.getYaw(),center.getPitch());for(int dy=-r;dy<=r;dy++){c.setY(center.getBlockY()+dy);Location s=strict(c);if(s!=null)return s;}}return null;}
 public Location strict(Location l){World w=l.getWorld();if(w==null)return null;Block feet=w.getBlockAt(l.getBlockX(),l.getBlockY(),l.getBlockZ()),head=feet.getRelative(BlockFace.UP),ground=feet.getRelative(BlockFace.DOWN);if(!ground.getType().isSolid()||HAZARDS.contains(ground.getType())||!free(feet)||!free(head))return null;return new Location(w,l.getBlockX()+.5,l.getY(),l.getBlockZ()+.5,l.getYaw(),l.getPitch());}
 private boolean free(Block b){return b.isPassable()&&!b.isLiquid()&&!HAZARDS.contains(b.getType());}
 private long cooldownLeft(Player p,String channel){if(p.hasPermission("mando.teleport.bypass-cooldown"))return 0;int sec=plugin.getConfig().getInt("teleport."+channel+".cooldown-seconds",plugin.getConfig().getInt("teleport.default-cooldown-seconds",0));Long last=cooldowns.getOrDefault(p.getUniqueId(),Map.of()).get(channel);if(last==null)return 0;return Math.max(0,sec-(System.currentTimeMillis()-last)/1000);}
 private void cancel(Player p,boolean tell){Pending x=pending.remove(p.getUniqueId());if(x!=null){x.task().cancel();if(tell)p.sendMessage(plugin.msg("teleport-cancelled"));}}
 @EventHandler(ignoreCancelled=true) public void move(PlayerMoveEvent e){Pending x=pending.get(e.getPlayer().getUniqueId());if(x==null||e.getTo()==null)return;double max=plugin.getConfig().getDouble("teleport.cancel-move-distance",0.15);if(!e.getFrom().getWorld().equals(e.getTo().getWorld())||e.getFrom().distanceSquared(e.getTo())>max*max)cancel(e.getPlayer(),true);}
 @EventHandler(ignoreCancelled=true) public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&plugin.getConfig().getBoolean("teleport.cancel-on-damage",true))cancel(p,true);}
}
