package dev.esslite;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.List;
public final class MandoCommand implements CommandExecutor, TabCompleter {
 private final EssLite plugin; public MandoCommand(EssLite plugin){this.plugin=plugin;}
 @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!(s instanceof Player p)){s.sendMessage("Mando: abre /serverconfig dentro del juego.");return true;}
  if(a.length>0){if(a[0].equalsIgnoreCase("config")){p.performCommand("serverconfig");return true;}if(a[0].equalsIgnoreCase("changelog")){p.performCommand("changelog");return true;}if(a[0].equalsIgnoreCase("setup")){showSetup(p,false);return true;}}
  showMain(p);return true;
 }
 public void showMain(Player p){
  ActionButton config=button("Configurar servidor","Abrir ServerConfig",pl->pl.performCommand("serverconfig"));
  ActionButton news=button("Novedades","Ver esta build",pl->pl.performCommand("changelog"));
  p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Mando · Administración del servidor")).body(List.of(DialogBody.plainMessage(Component.text("Servidor: "+Bukkit.getName()+" "+Bukkit.getMinecraftVersion()+"\n\nMando centraliza configuración y herramientas administrativas. En Purpur, prioriza las funciones nativas en vez de recrearlas.\n\nBuild de prueba 1.4.0-test.")))).build()).type(DialogType.confirmation(config,news))));
 }
 public void showSetup(Player p,boolean first){
  ActionButton now=button("Configurar ahora","Abrir configuración inicial",pl->{plugin.getConfig().set("setup.completed",true);plugin.saveConfig();pl.performCommand("serverconfig");});
  ActionButton later=button("Más tarde","Cerrar por ahora",pl->{});
  String intro=first?"Mando está instalado. Antes de usarlo, revisemos la configuración principal.":"Asistente inicial de Mando.";
  p.showDialog(Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Bienvenido a Mando")).body(List.of(DialogBody.plainMessage(Component.text(intro+"\n\nDetectado: "+Bukkit.getName()+" "+Bukkit.getMinecraftVersion()+"\n\nServerConfig separa las funciones de Mando de las opciones nativas de Purpur. Abrir este asistente no cambia opciones por sí solo.")))).build()).type(DialogType.confirmation(now,later))));
 }
 private ActionButton button(String t,String tip,java.util.function.Consumer<Player> action){return ActionButton.builder(Component.text(t)).tooltip(Component.text(tip)).action(DialogAction.customClick((r,a)->{if(a instanceof Player p)action.accept(p);},ClickCallback.Options.builder().uses(1).build())).build();}
 @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] x){return x.length==1?List.of("setup","config","changelog"):List.of();}
}
