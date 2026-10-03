package dev.mando;
import org.bukkit.command.*;
import java.util.*;
public final class MandoAdminCommand implements CommandExecutor,TabCompleter {
 private final Mando plugin; private final PlayerStorage storage; private final PlayerIdentityService identities; private final IntegrationManager integrations;
 public MandoAdminCommand(Mando p,PlayerStorage s,PlayerIdentityService i,IntegrationManager x){plugin=p;storage=s;identities=i;integrations=x;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!s.hasPermission("mando.admin")){s.sendMessage(plugin.msg("no-permission"));return true;}
  if(a.length==1&&a[0].equalsIgnoreCase("integrations")){showIntegrations(s);return true;}
  if(a.length==3&&a[0].equalsIgnoreCase("integrations")&&a[1].equalsIgnoreCase("afk")){try{integrations.select(IntegrationManager.AfkProvider.valueOf(a[2].toUpperCase(Locale.ROOT)));s.sendMessage("Mando: proveedor AFK -> "+integrations.afkProvider());}catch(Exception e){s.sendMessage("Proveedor inválido.");}return true;}
  if(a.length==3&&a[0].equalsIgnoreCase("integrations")&&a[1].equalsIgnoreCase("balance")){var id=identities.resolve(a[2]);if(id==null){s.sendMessage("Mando: jugador no encontrado.");return true;}Double v=integrations.vaultBalance(org.bukkit.Bukkit.getOfflinePlayer(id.uuid()));s.sendMessage(v==null?"Mando: Vault/economía no disponible.":"Mando: saldo leído por Vault = "+v);return true;}
  if(a.length==1&&a[0].equalsIgnoreCase("backup")){storage.backupAll("manual");s.sendMessage("Mando: backup de players completado.");return true;}
  if(a.length==4&&a[0].equalsIgnoreCase("identity")&&a[1].equalsIgnoreCase("bind")){try{UUID id=UUID.fromString(a[3]);identities.bindManual(a[2],id);s.sendMessage("Mando: asociación guardada.");}catch(Exception e){s.sendMessage("Uso: /mando identity bind <nombre> <UUID>");}return true;}
  if(a.length==3&&a[0].equalsIgnoreCase("identity")&&a[1].equalsIgnoreCase("resolve")){var r=identities.resolve(a[2]);s.sendMessage(r==null?"Mando: identidad no resuelta.":"Mando: "+r.name()+" -> "+r.uuid());return true;}
  s.sendMessage("Uso: /mando integrations | /mando backup | /mando identity resolve <nombre|UUID> | /mando identity bind <nombre> <UUID>");return true;
 }
 private void showIntegrations(CommandSender s){if(!(s instanceof org.bukkit.entity.Player p)){s.sendMessage("Mando: "+integrations.summary());return;}net.kyori.adventure.text.Component built=net.kyori.adventure.text.Component.text(integrations.summary()+"\\n\\nProveedor AFK: ");for(var v:IntegrationManager.AfkProvider.values())built=built.append(net.kyori.adventure.text.Component.text("["+v+"] ").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/mando integrations afk "+v.name())));final net.kyori.adventure.text.Component body=built;io.papermc.paper.dialog.Dialog d=io.papermc.paper.dialog.Dialog.create(b->b.empty().base(io.papermc.paper.registry.data.dialog.DialogBase.builder(net.kyori.adventure.text.Component.text("Mando · Integraciones")).body(java.util.List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(body))).build()).type(io.papermc.paper.registry.data.dialog.type.DialogType.notice()));p.showDialog(d);}
 public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("integrations","backup","identity");if(a.length==2&&a[0].equalsIgnoreCase("identity"))return List.of("resolve","bind");return List.of();}
}