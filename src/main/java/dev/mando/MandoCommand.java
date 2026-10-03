package dev.mando;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;import org.bukkit.command.*;import org.bukkit.entity.Player;import java.util.List;
public final class MandoCommand implements CommandExecutor,TabCompleter{
 private final MandoPlugin plugin;public MandoCommand(MandoPlugin p){plugin=p;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!(s instanceof Player p)){s.sendMessage("Mando: usa los comandos administrativos dentro del juego.");return true;}
  if(a.length>0){
   if(a[0].equalsIgnoreCase("config")){p.performCommand("serverconfig");return true;}
   if(a[0].equalsIgnoreCase("changelog")){p.performCommand("changelog");return true;}
   if(a[0].equalsIgnoreCase("setup")){showSetup(p,false);return true;}
   if(a[0].equalsIgnoreCase("status")){showStatus(p);return true;}
   if(a[0].equalsIgnoreCase("reload")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}plugin.reloadConfig();plugin.integrations().refresh();p.sendMessage("§6Mando §8» §aConfiguración recargada. Los cambios que requieren reinicio siguen pendientes.");return true;}
   if(a[0].equalsIgnoreCase("stop")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}if(a.length>1&&a[1].equalsIgnoreCase("cancel")){plugin.shutdown().cancel(true);return true;}if(a.length<2){p.sendMessage("§c/mando stop <minutos|cancel>");return true;}try{int min=Integer.parseInt(a[1]);if(!plugin.shutdown().scheduleMinutes(min))p.sendMessage("§cMinutos inválidos.");}catch(NumberFormatException ex){p.sendMessage("§c/mando stop <minutos|cancel>");}return true;}
   if(a[0].equalsIgnoreCase("cleanup")&&a.length>1&&a[1].equalsIgnoreCase("markers")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}int n=plugin.markers().cleanupRegistered();p.sendMessage("§aRegistro de marcadores limpiado: "+n+" entradas inválidas.");return true;}
   if(a[0].equalsIgnoreCase("diagnose")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}String d=plugin.diagnostics().create();p.sendMessage(d==null?"§cNo se pudo crear diagnóstico.":"§aDiagnóstico creado: "+d);return true;}
   if(a[0].equalsIgnoreCase("reset")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}ActionButton yes=button("Restablecer setup","Marca el asistente como pendiente",pl->{plugin.getConfig().set("setup.completed",false);plugin.saveConfig();pl.sendMessage("§6Mando §8» §eEl asistente volverá a mostrarse en la próxima sesión.");});ActionButton no=ActionButton.builder(Component.text("Cancelar")).build();p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Restablecer Mando")).body(List.of(DialogBody.plainMessage(Component.text("Esto no borra Homes, correo ni datos. Solo vuelve a dejar pendiente el asistente de configuración.")))).build()).type(DialogType.confirmation(yes,no))));return true;}
   if(a[0].equalsIgnoreCase("backup")){if(!p.isOp()){p.sendMessage(plugin.msg("no-permission"));return true;}String name=plugin.backups().createManual();p.sendMessage(Component.text(name==null?"No se pudo crear el backup.":"Backup creado: "+name));return true;}
  }showMain(p);return true;
 }
 private void showStatus(Player p){String body="Mando "+plugin.getPluginMeta().getVersion()+"
Plataforma: "+plugin.platform().platform()+"
Minecraft: "+Bukkit.getMinecraftVersion()+"
Storage: YAML
Setup: "+(plugin.getConfig().getBoolean("setup.completed",false)?"completado":"pendiente")+"
Homes: "+state("homes")+" · Warps: "+state("warps")+" · Spawn: "+state("spawn")+"
Último backup: "+plugin.backups().lastBackup()+"
Reinicio programado: "+(plugin.shutdown().active()?plugin.shutdown().secondsLeft()+"s":"no")+"

Integraciones / proveedores:
"+plugin.integrations().summary();p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Mando · Estado")).body(List.of(DialogBody.plainMessage(Component.text(body)))).build()).type(DialogType.notice())));}
 private String state(String m){return plugin.getConfig().getBoolean("modules."+m+".enabled",true)?"activo":"desactivado";}
 public void showMain(Player p){ActionButton config=button("Configurar servidor","Abrir ServerConfig",pl->pl.performCommand("serverconfig"));ActionButton status=button("Estado","Ver estado de Mando",pl->pl.performCommand("mando status"));String body="Servidor: "+Bukkit.getName()+" "+Bukkit.getMinecraftVersion()+"

Mando centraliza configuración, ubicaciones, comunicación e integraciones.

Build de prueba "+plugin.getPluginMeta().getVersion()+".";p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Mando · Administración")).body(List.of(DialogBody.plainMessage(Component.text(body)))).build()).type(DialogType.confirmation(config,status))));}
 public void showSetup(Player p,boolean first){plugin.setupProtection().begin(p);ActionButton now=button("Aplicar y configurar","Guardar base recomendada y abrir ServerConfig",pl->{plugin.getConfig().set("setup.completed",true);plugin.saveConfig();plugin.setupProtection().end(pl);pl.performCommand("serverconfig");});ActionButton later=button("Más tarde","Cerrar por ahora",pl->plugin.setupProtection().end(pl));String intro=(first?"Primera configuración de Mando.":"Asistente de Mando.")+"

Detectado: "+Bukkit.getName()+" "+Bukkit.getMinecraftVersion()+"

La configuración recomendada mantiene Homes, Warps, Spawn, Mail y TPA activos y permite ajustar Purpur desde ServerConfig.";p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Bienvenido a Mando")).body(List.of(DialogBody.plainMessage(Component.text(intro)))).build()).type(DialogType.confirmation(now,later))));}
 private ActionButton button(String t,String tip,java.util.function.Consumer<Player> action){return ActionButton.builder(Component.text(t)).tooltip(Component.text(tip)).action(DialogAction.customClick((r,a)->{if(a instanceof Player p)action.accept(p);},ClickCallback.Options.builder().uses(1).build())).build();}
 public List<String> onTabComplete(CommandSender s,Command c,String a,String[] x){return x.length==1?List.of("setup","config","status","reload","stop","backup","cleanup","diagnose","changelog"):List.of();}
}