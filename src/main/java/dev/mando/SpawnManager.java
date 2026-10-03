package dev.mando;
import org.bukkit.*;import org.bukkit.configuration.file.YamlConfiguration;import org.bukkit.entity.Player;import java.io.*;
public final class SpawnManager{
 private final MandoPlugin plugin;private final File file;private final YamlConfiguration y;
 public SpawnManager(MandoPlugin p){plugin=p;file=new File(p.getDataFolder(),"spawn.yml");y=YamlConfiguration.loadConfiguration(file);}
 public Location get(){String w=y.getString("spawn.world");World world=w==null?null:Bukkit.getWorld(w);if(world==null)return null;return new Location(world,y.getDouble("spawn.x"),y.getDouble("spawn.y"),y.getDouble("spawn.z"),(float)y.getDouble("spawn.yaw"),(float)y.getDouble("spawn.pitch"));}
 public synchronized boolean set(Location l){if(l==null||l.getWorld()==null)return false;y.set("spawn.world",l.getWorld().getName());y.set("spawn.x",l.getX());y.set("spawn.y",l.getY());y.set("spawn.z",l.getZ());y.set("spawn.yaw",l.getYaw());y.set("spawn.pitch",l.getPitch());try{file.getParentFile().mkdirs();y.save(file);return true;}catch(IOException e){plugin.getLogger().severe("Spawn save: "+e.getMessage());return false;}}
 public Location effective(Player p){Location own=get();return own!=null?own:Bukkit.getWorlds().get(0).getSpawnLocation();}
}