package dev.esslite;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class DeathHistory implements Listener {
 public record Death(long time,String world,double x,double y,double z){}
 private final EssLite plugin;private final File file;private final Map<UUID,List<Death>> data=new HashMap<>();
 public DeathHistory(EssLite p){plugin=p;file=new File(p.getDataFolder(),"deaths.yml");load();}
 @EventHandler public void death(PlayerDeathEvent e){Location l=e.getPlayer().getLocation();List<Death>d=data.computeIfAbsent(e.getPlayer().getUniqueId(),k->new ArrayList<>());d.add(0,new Death(System.currentTimeMillis(),l.getWorld().getName(),l.getX(),l.getY(),l.getZ()));while(d.size()>plugin.getConfig().getInt("profile.deaths-kept",10))d.remove(d.size()-1);save();}
 public List<Death> all(UUID id){return List.copyOf(data.getOrDefault(id,List.of()));}
 public Location location(Death d){var w=Bukkit.getWorld(d.world());return w==null?null:new Location(w,d.x(),d.y(),d.z());}
 private void load(){YamlConfiguration y=YamlConfiguration.loadConfiguration(file);for(String u:y.getKeys(false))try{UUID id=UUID.fromString(u);for(Map<?,?>m:y.getMapList(u)){data.computeIfAbsent(id,k->new ArrayList<>()).add(new Death(((Number)m.get("time")).longValue(),String.valueOf(m.get("world")),((Number)m.get("x")).doubleValue(),((Number)m.get("y")).doubleValue(),((Number)m.get("z")).doubleValue()));}}catch(Exception ignored){}}
 public void save(){YamlConfiguration y=new YamlConfiguration();for(var e:data.entrySet()){List<Map<String,Object>>l=new ArrayList<>();for(Death d:e.getValue())l.add(Map.of("time",d.time(),"world",d.world(),"x",d.x(),"y",d.y(),"z",d.z()));y.set(e.getKey().toString(),l);}try{y.save(file);}catch(IOException ex){plugin.getLogger().severe("No se pudo guardar deaths.yml");}}
}
