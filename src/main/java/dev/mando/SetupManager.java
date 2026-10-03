package dev.mando;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.*;

public final class SetupManager implements Listener {
 private final Mando plugin; private final PlatformDetector platform; private final IntegrationManager integrations;
 public SetupManager(Mando p,PlatformDetector pf,IntegrationManager i){plugin=p;platform=pf;integrations=i;}
 public boolean complete(){return plugin.getConfig().getBoolean("setup.completed",false);}
 public void complete(boolean v){plugin.getConfig().set("setup.completed",v);plugin.saveConfig();}
 public void defaults(){
  set("language","es");set("homes.max",3);set("markers.homes.enabled",true);set("markers.warps.enabled",true);set("markers.spawn.enabled",true);
  set("teleport.default-warmup-seconds",3);set("teleport.default-cooldown-seconds",5);set("teleport.cancel-on-damage",true);set("teleport.cancel-move-distance",0.15);
  set("back.enabled",true);set("teleport.safety.enabled",true);set("rtp.min-radius",500);set("rtp.max-radius",5000);set("modules.mail.enabled",true);
  set("integrations.afk.provider","AUTO");set("integrations.vault.read-only",true);set("modules.server-config.purpur.enabled",true);plugin.saveConfig();
 }
 private void set(String p,Object v){if(!plugin.getConfig().contains(p))plugin.getConfig().set(p,v);}
 public void show(Player p){
  Component body=Component.text("Estado: "+(complete()?"configurado":"primera configuración")+
   "\nPlataforma: "+platform.platform()+"\n"+integrations.summary()+"\n\n")
   .append(Component.text("[Aplicar base recomendada] ").clickEvent(ClickEvent.runCommand("/serverconfig setup defaults")))
   .append(Component.text("[Comandos] ").clickEvent(ClickEvent.runCommand("/serverconfig commands")))
   .append(Component.text("[Integraciones]").clickEvent(ClickEvent.runCommand("/mando integrations")));
  Dialog d=Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Mando · Setup")).body(List.of(DialogBody.plainMessage(body))).build()).type(DialogType.notice()));p.showDialog(d);
 }
 public void ecosystemChanged(String oldSig,String now){plugin.getConfig().set("setup.ecosystem-signature",now);plugin.saveConfig();if(oldSig==null||oldSig.equals(now))return;plugin.getLogger().warning("Cambió el ecosistema de plugins/plataforma: "+oldSig+" -> "+now);for(Player p:Bukkit.getOnlinePlayers())if(p.hasPermission("mando.serverconfig"))showChange(p,oldSig,now);}
 public String signature(){return platform.platform()+"|"+integrations.summary();}
 private void showChange(Player p,String oldSig,String now){Component body=Component.text("Mando detectó un cambio de ecosistema.\nAntes: "+oldSig+"\nAhora: "+now+"\nRevisa proveedores y configuración.").append(Component.text("\n[Abrir configuración]").clickEvent(ClickEvent.runCommand("/serverconfig")));Dialog d=Dialog.create(b->b.empty().base(DialogBase.builder(Component.text("Mando · Ecosistema actualizado")).body(List.of(DialogBody.plainMessage(body))).build()).type(DialogType.notice()));p.showDialog(d);}
 @EventHandler public void join(PlayerJoinEvent e){if(!complete()&&e.getPlayer().hasPermission("mando.serverconfig"))Bukkit.getScheduler().runTaskLater(plugin,()->show(e.getPlayer()),40L);}
}
