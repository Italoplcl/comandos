package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.command.*;import org.bukkit.entity.Player;import java.lang.reflect.Method;import java.util.*;
public final class AfkManager implements CommandExecutor{
 private final MandoPlugin plugin;private final Set<UUID> fallback=new HashSet<>();
 public AfkManager(MandoPlugin p){plugin=p;}
 public boolean isAfk(Player p){try{Method m=p.getClass().getMethod("isAfk");return Boolean.TRUE.equals(m.invoke(p));}catch(Exception ignored){return fallback.contains(p.getUniqueId());}}
 private void set(Player p,boolean v){try{Method m=p.getClass().getMethod("setAfk",boolean.class);m.invoke(p,v);return;}catch(Exception ignored){}if(v)fallback.add(p.getUniqueId());else fallback.remove(p.getUniqueId());}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p))return true;boolean v=!isAfk(p);set(p,v);p.sendMessage("§6Mando §8» "+(v?"§eAhora estás AFK.":"§aYa no estás AFK."));return true;}
}