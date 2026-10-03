package dev.mando;
import org.bukkit.*;import org.bukkit.configuration.file.YamlConfiguration;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.entity.PlayerDeathEvent;import java.io.*;import java.time.Instant;import java.util.*;
public final class DeathManager implements Listener{
 public record Death(String world,double x,double y,double z,float yaw,float pitch,String cause,String at){}
 private final MandoPlugin plugin;private final File file;private final YamlConfiguration y;
 public DeathManager(MandoPlugin p){plugin=p;file=new File(p.getDataFolder(),"deaths.yml");y=YamlConfiguration.loadConfiguration(file);}
 @EventHandler public void death(PlayerDeathEvent e){Player p=e.getPlayer();Location l=p.getLocation();String base=p.getUniqueId()+".entries";List<Map<?,?>> old=y.getMapList(base);List<Map<String,Object>> list=new ArrayList<>();Map<String,Object> m=new LinkedHashMap<>();m.put("world",l.getWorld().getName());m.put("x",l.getX());m.put("y",l.getY());m.put("z",l.getZ());m.put("yaw",l.getYaw());m.put("pitch",l.getPitch());m.put("cause",p.getLastDamageCause()==null?"UNKNOWN":p.getLastDamageCause().getCause().name());m.put("at",Instant.now().toString());list.add(m);for(Map<?,?> x:old){if(list.size()>=plugin.getConfig().getInt("deaths.history-size",5))break;Map<String,Object> z=new LinkedHashMap<>();x.forEach((k,v)->z.put(String.valueOf(k),v));list.add(z);}y.set(base,list);save();}
 public List<Death> list(UUID id){List<Death> out=new ArrayList<>();for(Map<?,?> m:y.getMapList(id+".entries"))try{out.add(new Death(String.valueOf(m.get("world")),num(m.get("x")),num(m.get("y")),num(m.get("z")),((Number)m.get("yaw")).floatValue(),((Number)m.get("pitch")).floatValue(),String.valueOf(m.get("cause")),String.valueOf(m.get("at"))));}catch(Exception ignored){}return out;}
 public Location location(Death d){World w=Bukkit.getWorld(d.world());return w==null?null:new Location(w,d.x(),d.y(),d.z(),d.yaw(),d.pitch());}
 private double num(Object o){return ((Number)o).doubleValue();}
 public synchronized void save(){try{file.getParentFile().mkdirs();y.save(file);}catch(IOException e){plugin.getLogger().severe("Deaths save: "+e.getMessage());}}
}