package dev.mando;

import dev.mando.MailManager.Mail;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.text.SimpleDateFormat;
import java.util.*;

public final class ProfileCommand implements CommandExecutor,TabCompleter {
 private final Mando plugin;private final HomeManager homes;private final MailManager mail;private final TpaManager tpa;private final DeathHistory deaths;private final TeleportService teleports;private final PlayerIdentityService identities;
 public ProfileCommand(Mando p,HomeManager h,MailManager m,TpaManager t,DeathHistory d,TeleportService ts,PlayerIdentityService i){plugin=p;homes=h;mail=m;tpa=t;deaths=d;teleports=ts;identities=i;}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!(s instanceof Player p)){s.sendMessage(plugin.msg("only-players"));return true;}
  switch(c.getName().toLowerCase(Locale.ROOT)){
   case "profile"->profile(p,a);
   case "mail"->mail(p,a);
   case "tpa"->request(p,a);
   case "tpaccept"->{Player from=tpa.accept(p);if(from==null){p.sendMessage(plugin.msg("tpa-none"));return true;}UUID target=p.getUniqueId();teleports.teleport(from,()->{Player live=Bukkit.getPlayer(target);return live==null?null:live.getLocation();},TeleportService.Kind.PLAYER,"tpa",()->{p.sendMessage(plugin.msg("tpa-accepted"));from.sendMessage(plugin.msg("tpa-accepted"));});}
   case "tpdeny"->{p.sendMessage(plugin.msg(tpa.deny(p)?"tpa-denied":"tpa-none"));}
   case "tpatoggle"->{p.sendMessage(plugin.msg(tpa.toggle(p.getUniqueId())?"tpa-on":"tpa-off"));}
  } return true;
 }
 private void profile(Player p,String[] a){
  if(a.length>0&&p.hasPermission("mando.profile.admin")){var id=identities.resolve(a[0]);if(id==null){p.sendMessage(plugin.msg("player-not-found"));return;}OfflinePlayer target=Bukkit.getOfflinePlayer(id.uuid());showAdmin(p,target);return;}
  Component body=Component.text("Homes: "+homes.all(p.getUniqueId()).size()+"\nMail sin leer: "+mail.unread(p.getUniqueId())+"\nTPA: "+(tpa.enabled(p.getUniqueId())?"ON":"OFF")+"\n\n")
   .append(Component.text("[Homes]").clickEvent(ClickEvent.runCommand("/homes"))).append(Component.text("  "))
   .append(Component.text("[Mail]").clickEvent(ClickEvent.runCommand("/mail"))).append(Component.text("  "))
   .append(Component.text("[TPA toggle]").clickEvent(ClickEvent.runCommand("/tpatoggle")));
  show(p,"Perfil · "+p.getName(),body);
 }
 private void showAdmin(Player admin,OfflinePlayer target){
  UUID id=target.getUniqueId();Component b=Component.text("Jugador: "+target.getName()+"\nUUID: "+id+"\nOnline: "+target.isOnline()+"\nHomes: "+homes.all(id).size()+"\nMuertes guardadas: "+deaths.all(id).size()+"\nMail: "+mail.inbox(id).size()+"\n\n");
  int i=0;for(var e:homes.all(id).entrySet()){var loc=e.getValue().toLocation();if(loc!=null)b=b.append(Component.text("[TP home "+e.getKey()+"]").clickEvent(ClickEvent.runCommand("/profiletp "+id+" home "+e.getKey()))).append(Component.newline());if(++i>=10)break;}
  i=0;for(var d:deaths.all(id)){b=b.append(Component.text("[TP muerte "+(++i)+"]").clickEvent(ClickEvent.runCommand("/profiletp "+id+" death "+i))).append(Component.newline());if(i>=10)break;}
  show(admin,"Administrar perfil · "+target.getName(),b);
 }
 private void mail(Player p,String[] a){
  if(a.length==0){Component b=Component.text("Sin leer: "+mail.unread(p.getUniqueId())+"\n\n");List<Mail> ms=mail.inbox(p.getUniqueId());for(int i=ms.size()-1;i>=0&&i>=ms.size()-20;i--){Mail m=ms.get(i);b=b.append(Component.text((m.read()?"":"● ")+m.fromName()+" · "+new SimpleDateFormat("dd/MM HH:mm").format(new Date(m.sentAt()))).clickEvent(ClickEvent.runCommand("/mail read "+m.id()))).append(Component.newline());}show(p,"Mailbox",b);return;}
  if(a[0].equalsIgnoreCase("send")&&a.length>=3){var rid=identities.resolve(a[1]);if(rid==null){p.sendMessage(plugin.msg("player-not-found"));return;}OfflinePlayer to=Bukkit.getOfflinePlayer(rid.uuid());String body=String.join(" ",Arrays.copyOfRange(a,2,a.length));String r=mail.send(p.getUniqueId(),p.getName(),to.getUniqueId(),body);p.sendMessage(plugin.msg(r.equals("blocked")?"mail-blocked":"mail-sent"));return;}
  if(a[0].equalsIgnoreCase("read")&&a.length>=2){Mail m=mail.get(p.getUniqueId(),a[1]);if(m==null){p.sendMessage(plugin.msg("mail-none"));return;}mail.markRead(p.getUniqueId(),m.id());Component b=Component.text(m.body()+"\n\n").append(Component.text("[Responder]").clickEvent(ClickEvent.suggestCommand("/mail send "+m.fromName()+" "))).append(Component.text("  [Borrar]").clickEvent(ClickEvent.runCommand("/mail delete "+m.id()))).append(Component.text("  [Bloquear]").clickEvent(ClickEvent.runCommand("/mail block "+m.fromName())));show(p,"Mail de "+m.fromName(),b);return;}
  if(a[0].equalsIgnoreCase("delete")&&a.length>=2){p.sendMessage(plugin.msg(mail.delete(p.getUniqueId(),a[1])?"mail-deleted":"mail-none"));return;}
  if(a[0].equalsIgnoreCase("block")&&a.length>=2){var rid=identities.resolve(a[1]);if(rid==null){p.sendMessage(plugin.msg("player-not-found"));return;}OfflinePlayer t=Bukkit.getOfflinePlayer(rid.uuid());boolean on=mail.toggleBlock(p.getUniqueId(),t.getUniqueId());p.sendMessage(plugin.msg(on?"mail-block-on":"mail-block-off"));return;}
  p.sendMessage(plugin.msg("mail-usage"));
 }
 private void request(Player p,String[] a){if(a.length==0){p.sendMessage(plugin.msg("tpa-usage"));return;}Player to=Bukkit.getPlayerExact(a[0]);if(to==null||to.equals(p)){p.sendMessage(plugin.msg("player-not-found"));return;}String r=tpa.request(p,to);if(r.equals("disabled")){p.sendMessage(plugin.msg("tpa-disabled-target"));return;}p.sendMessage(plugin.msg("tpa-sent"));to.sendMessage(plugin.msg("tpa-received")); }
 private void show(Player p,String title,Component body){Dialog d=Dialog.create(b->b.empty().base(DialogBase.builder(Component.text(title)).body(List.of(DialogBody.plainMessage(body))).build()).type(DialogType.notice()));p.showDialog(d);}
 @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length!=1)return List.of();String n=c.getName().toLowerCase(Locale.ROOT);if(n.equals("tpa"))return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(x->x.toLowerCase().startsWith(a[0].toLowerCase())).toList();if(n.equals("mail"))return List.of("send","read","delete","block").stream().filter(x->x.startsWith(a[0].toLowerCase())).toList();return List.of();}
}
