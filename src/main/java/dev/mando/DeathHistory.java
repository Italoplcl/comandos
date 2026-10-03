package dev.mando;
import org.bukkit.*;import org.bukkit.event.*;import org.bukkit.event.entity.PlayerDeathEvent;import java.util.*;
public final class DeathHistory implements Listener {
 public record Death(long time,String world,double x,double y,double z){}
 private final Mando plugin;private final PlayerStorage storage;
 public DeathHistory(Mando p,PlayerStorage s){plugin=p;storage=s;}
 @EventHandler public void death(PlayerDeathEvent e){Location l=e.getPlayer().getLocation();UUID id=e.getPlayer().getUniqueId();storage.update(id,y->{List<Map<?,?>> old=new ArrayList<>(y.getMapList("history.deaths"));List<Map<String,Object>> out=new ArrayList<>();out.add(Map.of("time",System.currentTimeMillis(),"world",l.getWorld().getName(),"x",l.getX(),"y",l.getY(),"z",l.getZ()));for(Map<?,?>m:old)if(out.size()<plugin.getConfig().getInt("profile.deaths-kept",10))out.add(copy(m));y.set("history.deaths",out);});}
 public List<Death> all(UUID id){return storage.query(id,y->{List<Death> l=new ArrayList<>();for(Map<?,?>m:y.getMapList("history.deaths"))try{l.add(new Death(((Number)m.get("time")).longValue(),String.valueOf(m.get("world")),((Number)m.get("x")).doubleValue(),((Number)m.get("y")).doubleValue(),((Number)m.get("z")).doubleValue()));}catch(Exception ignored){}return List.copyOf(l);});}
 public Location location(Death d){World w=Bukkit.getWorld(d.world());return w==null?null:new Location(w,d.x(),d.y(),d.z());}
 public void save(){storage.flushAll();}
 private static Map<String,Object> copy(Map<?,?>m){Map<String,Object>o=new HashMap<>();for(var e:m.entrySet())o.put(String.valueOf(e.getKey()),e.getValue());return o;}
}
