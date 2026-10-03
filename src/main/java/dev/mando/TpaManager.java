package dev.mando;
import org.bukkit.entity.Player;import java.util.*;
public final class TpaManager {
 private record Request(UUID from,long expires){}
 private final Mando plugin;private final PlayerStorage storage;private final Map<UUID,Request> requests=new HashMap<>();
 public TpaManager(Mando p,PlayerStorage s){plugin=p;storage=s;}
 public boolean enabled(UUID id){return storage.query(id,y->y.getBoolean("preferences.tpa.enabled",true));}
 public boolean toggle(UUID id){boolean v=!enabled(id);storage.update(id,y->y.set("preferences.tpa.enabled",v));if(!v)requests.remove(id);return v;}
 public String request(Player from,Player to){if(!enabled(to.getUniqueId()))return "disabled";requests.put(to.getUniqueId(),new Request(from.getUniqueId(),System.currentTimeMillis()+plugin.getConfig().getLong("tpa.timeout-seconds",60)*1000));return "ok";}
 public Player accept(Player to){Request r=requests.remove(to.getUniqueId());if(r==null||r.expires()<System.currentTimeMillis())return null;return plugin.getServer().getPlayer(r.from());}
 public boolean deny(Player to){return requests.remove(to.getUniqueId())!=null;}
}
