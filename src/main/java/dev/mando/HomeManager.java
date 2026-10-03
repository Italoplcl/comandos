package dev.mando;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;import org.bukkit.configuration.ConfigurationSection;import org.bukkit.entity.Player;
import java.util.*;import java.util.regex.Pattern;
public final class HomeManager {
 public record Home(String world,double x,double y,double z,float yaw,float pitch,String icon){
  Location toLocation(){World w=Bukkit.getWorld(world);return w==null?null:new Location(w,x,y,z,yaw,pitch);}
  static Home of(Location l){return new Home(l.getWorld().getName(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch(),autoIcon(l));}
  public Home withIcon(String m){return new Home(world,x,y,z,yaw,pitch,m);}
  private static String autoIcon(Location l){return switch(l.getWorld().getEnvironment()){case NETHER->"NETHERRACK";case THE_END->"END_STONE";default->"GRASS_BLOCK";};}
 }
 private static final Pattern VALID=Pattern.compile("[a-z0-9_-]{1,16}");
 private final Mando plugin;private final PlayerStorage storage;private TeleportService teleports;
 public HomeManager(Mando p,PlayerStorage s){plugin=p;storage=s;}
 public void teleports(TeleportService t){teleports=t;}
 public static String normalize(String raw){if(raw==null)return null;String n=raw.toLowerCase(Locale.ROOT);return VALID.matcher(n).matches()?n:null;}
 public Map<String,Home> all(UUID id){return readMap(id,"homes.current");}
 public Home deleted(UUID id,String n){return readMap(id,"homes.deleted").get(n);}
 public Home previous(UUID id,String n){return readMap(id,"homes.previous").get(n);}
 public void set(UUID id,String name,Home h){storage.update(id,y->{String b="homes.current."+name;Home old=read(y,b);if(old!=null)write(y,"homes.previous."+name,old);write(y,b,h);});}
 public boolean remove(UUID id,String name){if(!all(id).containsKey(name))return false;storage.update(id,y->{Home h=read(y,"homes.current."+name);if(h!=null)write(y,"homes.deleted."+name,h);y.set("homes.current."+name,null);});return true;}
 public boolean restoreDeleted(UUID id,String name){Home h=deleted(id,name);if(h==null)return false;storage.update(id,y->{write(y,"homes.current."+name,h);y.set("homes.deleted."+name,null);});return true;}
 public boolean restorePrevious(UUID id,String name){Home h=previous(id,name);if(h==null)return false;storage.update(id,y->{write(y,"homes.current."+name,h);y.set("homes.previous."+name,null);});return true;}
 public void teleport(Player p,String n,Home h){if(h.toLocation()==null){p.sendMessage(plugin.msg("home-world-missing"));return;}if(teleports==null){p.sendMessage(plugin.msg("teleport-failed"));return;}teleports.teleport(p,h::toLocation,TeleportService.Kind.STORED,"home",()->p.sendMessage(plugin.msg("home-teleported",Placeholder.unparsed("name",n))));}
 public void saveNow(){storage.flushAll();}
 private Map<String,Home> readMap(UUID id,String path){return storage.query(id,y->{ConfigurationSection s=y.getConfigurationSection(path);if(s==null)return Map.of();Map<String,Home> m=new TreeMap<>();for(String n:s.getKeys(false)){Home h=read(y,path+"."+n);if(h!=null)m.put(n,h);}return Collections.unmodifiableMap(m);});}
 private static Home read(org.bukkit.configuration.file.YamlConfiguration y,String b){String w=y.getString(b+".world");return w==null?null:new Home(w,y.getDouble(b+".x"),y.getDouble(b+".y"),y.getDouble(b+".z"),(float)y.getDouble(b+".yaw"),(float)y.getDouble(b+".pitch"),y.getString(b+".icon","GRASS_BLOCK"));}
 private static void write(org.bukkit.configuration.file.YamlConfiguration y,String b,Home h){y.set(b+".world",h.world());y.set(b+".x",h.x());y.set(b+".y",h.y());y.set(b+".z",h.z());y.set(b+".yaw",h.yaw());y.set(b+".pitch",h.pitch());y.set(b+".icon",h.icon());}
}
