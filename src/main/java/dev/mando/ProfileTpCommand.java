package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.Location;import org.bukkit.command.*;import org.bukkit.entity.Player;import java.util.*;
public final class ProfileTpCommand implements CommandExecutor{
 private final Mando plugin;private final HomeManager homes;private final DeathHistory deaths;private final TeleportService teleports;
 public ProfileTpCommand(Mando p,HomeManager h,DeathHistory d,TeleportService t){plugin=p;homes=h;deaths=d;teleports=t;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!(s instanceof Player p)||!p.hasPermission("mando.profile.admin"))return true;if(a.length<3)return true;try{UUID id=UUID.fromString(a[0]);Location loc=null;if(a[1].equalsIgnoreCase("home")){var h=homes.all(id).get(HomeManager.normalize(a[2]));if(h!=null)loc=h.toLocation();}else if(a[1].equalsIgnoreCase("death")){int i=Integer.parseInt(a[2])-1;var ds=deaths.all(id);if(i>=0&&i<ds.size())loc=deaths.location(ds.get(i));}if(loc!=null){Location dest=loc.clone();teleports.teleport(p,()->dest,TeleportService.Kind.ADMIN,"admin",()->p.sendMessage(plugin.msg("admin-tp-done")));}}catch(Exception ignored){}return true;}
}
