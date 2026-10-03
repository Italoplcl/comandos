package dev.mando;
import org.bukkit.command.*;
import java.util.*;
public final class MandoAdminCommand implements CommandExecutor,TabCompleter {
 private final Mando plugin; private final PlayerStorage storage; private final PlayerIdentityService identities;
 public MandoAdminCommand(Mando p,PlayerStorage s,PlayerIdentityService i){plugin=p;storage=s;identities=i;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!s.hasPermission("mando.admin")){s.sendMessage(plugin.msg("no-permission"));return true;}
  if(a.length==1&&a[0].equalsIgnoreCase("backup")){storage.backupAll("manual");s.sendMessage("Mando: backup de players completado.");return true;}
  if(a.length==4&&a[0].equalsIgnoreCase("identity")&&a[1].equalsIgnoreCase("bind")){try{UUID id=UUID.fromString(a[3]);identities.bindManual(a[2],id);s.sendMessage("Mando: asociación guardada.");}catch(Exception e){s.sendMessage("Uso: /mando identity bind <nombre> <UUID>");}return true;}
  if(a.length==3&&a[0].equalsIgnoreCase("identity")&&a[1].equalsIgnoreCase("resolve")){var r=identities.resolve(a[2]);s.sendMessage(r==null?"Mando: identidad no resuelta.":"Mando: "+r.name()+" -> "+r.uuid());return true;}
  s.sendMessage("Uso: /mando backup | /mando identity resolve <nombre|UUID> | /mando identity bind <nombre> <UUID>");return true;
 }
 public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("backup","identity");if(a.length==2&&a[0].equalsIgnoreCase("identity"))return List.of("resolve","bind");return List.of();}
}