package dev.mando;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.*;
import java.time.Instant;
import java.util.*;

public final class PlayerDataManager {
  private final MandoPlugin plugin;
  private final File dir;
  public PlayerDataManager(MandoPlugin plugin){this.plugin=plugin;this.dir=new File(plugin.getDataFolder(),"players");dir.mkdirs();}
  private File file(UUID id){return new File(dir,id+".yml");}
  private YamlConfiguration load(UUID id){return YamlConfiguration.loadConfiguration(file(id));}
  public synchronized void touch(Player p){YamlConfiguration y=load(p.getUniqueId());y.set("identity.uuid",p.getUniqueId().toString());y.set("identity.last-name",p.getName());y.set("identity.last-seen",Instant.now().toString());save(p.getUniqueId(),y);}
  public synchronized boolean tpaEnabled(UUID id){return load(id).getBoolean("preferences.tpa-enabled",true);}
  public synchronized void setTpaEnabled(UUID id,boolean v){YamlConfiguration y=load(id);y.set("preferences.tpa-enabled",v);save(id,y);}
  public synchronized boolean mailBlocked(UUID owner,UUID sender){return load(owner).getStringList("mail.blocked").contains(sender.toString());}
  public synchronized void setMailBlocked(UUID owner,UUID other,boolean blocked){YamlConfiguration y=load(owner);List<String> l=new ArrayList<>(y.getStringList("mail.blocked"));l.remove(other.toString());if(blocked)l.add(other.toString());y.set("mail.blocked",l);save(owner,y);}
  public synchronized void setLastReadVersion(UUID id,String version){YamlConfiguration y=load(id);y.set("changelog.last-read-version",version);save(id,y);}
  public synchronized String lastReadVersion(UUID id){return load(id).getString("changelog.last-read-version","");}
  public synchronized Set<UUID> knownPlayers(){Set<UUID> out=new HashSet<>();File[] fs=dir.listFiles((d,n)->n.endsWith(".yml"));if(fs!=null)for(File f:fs)try{out.add(UUID.fromString(f.getName().substring(0,f.getName().length()-4)));}catch(Exception ignored){}return out;}
  public synchronized String lastName(UUID id){return load(id).getString("identity.last-name",id.toString());}
  private void save(UUID id,YamlConfiguration y){try{dir.mkdirs();y.save(file(id));}catch(IOException e){plugin.getLogger().severe("Player data: "+e.getMessage());}}
}
