package dev.mando;

import org.bukkit.entity.Player;
import java.util.*;

public final class TpaManager {
 private record Request(UUID from,long expires){}
 private final Mando plugin;private final Map<UUID,Request> requests=new HashMap<>();private final Set<UUID> disabled=new HashSet<>();
 public TpaManager(Mando p){plugin=p;}
 public boolean enabled(UUID id){return !disabled.contains(id);}
 public boolean toggle(UUID id){if(disabled.remove(id))return true;disabled.add(id);requests.remove(id);return false;}
 public String request(Player from,Player to){if(!enabled(to.getUniqueId()))return "disabled";requests.put(to.getUniqueId(),new Request(from.getUniqueId(),System.currentTimeMillis()+plugin.getConfig().getLong("tpa.timeout-seconds",60)*1000));return "ok";}
 public Player accept(Player to){Request r=requests.remove(to.getUniqueId());if(r==null||r.expires()<System.currentTimeMillis())return null;return plugin.getServer().getPlayer(r.from());}
 public boolean deny(Player to){return requests.remove(to.getUniqueId())!=null;}
}
