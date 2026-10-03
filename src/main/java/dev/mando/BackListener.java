package dev.mando;
import org.bukkit.*;import org.bukkit.configuration.file.YamlConfiguration;import org.bukkit.event.*;import org.bukkit.event.entity.PlayerDeathEvent;import org.bukkit.event.player.PlayerTeleportEvent;import java.io.*;import java.util.*;
public final class BackListener implements Listener{
 private final MandoPlugin plugin;private final File file;private final YamlConfiguration y;private final Map<UUID,Location> backs=new HashMap<>();
 public BackListener(MandoPlugin p){plugin=p;file=new File(p.getDataFolder(),"back.yml");y=YamlConfiguration.loadConfiguration(file);load();}
 public Location get(UUID id){Location l=backs.get(id);return l==null?null:l.clone();}
 public void set(UUID id,Location l){backs.put(id,l.clone());save(id,l);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onTeleport(PlayerTeleportEvent e){Location from=e.getFrom(),to=e.getTo();if(to==null)return;if(from.getWorld().equals(to.getWorld())&&from.distanceSquared(to)<1)return;set(e.getPlayer().getUniqueId(),from);}
 @EventHandler(priority=EventPriority.MONITOR) public void onDeath(PlayerDeathEvent e){set(e.getPlayer().getUniqueId(),e.getPlayer().getLocation());}
 private void load(){for(String s:y.getKeys(false))try{UUID id=UUID.fromString(s);World w=Bukkit.getWorld(y.getString(s+".world",""));if(w!=null)backs.put(id,new Location(w,y.getDouble(s+".x"),y.getDouble(s+".y"),y.getDouble(s+".z"),(float)y.getDouble(s+".yaw"),(float)y.getDouble(s+".pitch")));}catch(Exception ignored){}}
 private synchronized void save(UUID id,Location l){String b=id+".";y.set(b+"world",l.getWorld().getName());y.set(b+"x",l.getX());y.set(b+"y",l.getY());y.set(b+"z",l.getZ());y.set(b+"yaw",l.getYaw());y.set(b+"pitch",l.getPitch());try{file.getParentFile().mkdirs();y.save(file);}catch(IOException e){plugin.getLogger().warning("Back save: "+e.getMessage());}}
}