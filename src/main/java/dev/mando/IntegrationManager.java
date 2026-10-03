package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.plugin.Plugin;import java.util.*;
public final class IntegrationManager{
 public enum State{ACTIVE,STANDBY,CONFLICT,UNAVAILABLE}
 private final MandoPlugin plugin;private final Map<String,State> states=new LinkedHashMap<>();
 public IntegrationManager(MandoPlugin p){plugin=p;refresh();}
 public void refresh(){states.clear();for(String n:List.of("Essentials","CMI","HuskHomes","BetterRTP","TAB","ChatControl","PlaceholderAPI","LuckPerms","Vault","AuthMe"))states.put(n,present(n)?State.STANDBY:State.UNAVAILABLE);select("homes",List.of("HuskHomes","Essentials","CMI"));select("rtp",List.of("BetterRTP","HuskHomes","Essentials","CMI"));if(present("Vault"))states.put("Vault",State.ACTIVE);if(present("AuthMe"))states.put("AuthMe",State.ACTIVE);if(present("PlaceholderAPI"))states.put("PlaceholderAPI",State.ACTIVE);}
 private boolean present(String n){Plugin p=Bukkit.getPluginManager().getPlugin(n);return p!=null&&p.isEnabled();}
 private void select(String cap,List<String> c){String chosen=plugin.getConfig().getString("providers."+cap,"Mando");if(!chosen.equalsIgnoreCase("Mando")&&present(chosen))states.put(chosen,State.ACTIVE);}
 public State state(String n){return states.getOrDefault(n,State.UNAVAILABLE);}public Map<String,State> states(){return Collections.unmodifiableMap(states);}
 public String summary(){StringBuilder s=new StringBuilder();states.forEach((k,v)->{if(v!=State.UNAVAILABLE)s.append(k).append(": ").append(v).append("\n");});return s.length()==0?"Sin integraciones externas activas":s.toString().trim();}
}