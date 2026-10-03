package dev.mando;
import net.kyori.adventure.text.Component;import net.kyori.adventure.text.event.ClickEvent;import net.kyori.adventure.text.event.HoverEvent;import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerQuitEvent;import java.util.*;
public final class TpaManager implements Listener{
 public record Req(UUID from,UUID to,boolean here,long expires){}
 private final MandoPlugin plugin;private final PlayerDataManager players;private final TeleportManager teleports;private final Map<UUID,Req> in=new HashMap<>(),out=new HashMap<>();private final Map<UUID,Long> lastRequest=new HashMap<>();
 public TpaManager(MandoPlugin p,PlayerDataManager d,TeleportManager t){plugin=p;players=d;teleports=t;}
 public boolean disabled(Player p){return !players.tpaEnabled(p.getUniqueId());}
 public boolean toggle(Player p){boolean v=!players.tpaEnabled(p.getUniqueId());players.setTpaEnabled(p.getUniqueId(),v);return v;}
 public String request(Player f,Player t,boolean here){
  if(f.equals(t))return "No puedes enviarte TPA a ti mismo.";if(disabled(t))return "Ese jugador no acepta TPA.";if(plugin.afk().isAfk(t))return "Ese jugador está AFK y no recibe solicitudes TPA.";
  long now=System.currentTimeMillis(),spam=Math.max(0,plugin.getConfig().getLong("tpa.anti-spam-seconds",3))*1000L;if(now-lastRequest.getOrDefault(f.getUniqueId(),0L)<spam)return "Espera antes de enviar otra solicitud.";if(out.containsKey(f.getUniqueId()))return "Ya tienes una solicitud saliente.";if(in.containsKey(t.getUniqueId()))return "Ese jugador ya tiene una solicitud pendiente.";
  int sec=plugin.getConfig().getInt("tpa.expire-seconds",30);Req r=new Req(f.getUniqueId(),t.getUniqueId(),here,now+sec*1000L);in.put(r.to(),r);out.put(r.from(),r);lastRequest.put(f.getUniqueId(),now);Bukkit.getScheduler().runTaskLater(plugin,()->expire(r),sec*20L);
  Component accept=Component.text("[ACEPTAR]",NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/tpaccept")).hoverEvent(HoverEvent.showText(Component.text("Aceptar solicitud")));
  Component deny=Component.text("[RECHAZAR]",NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/tpdeny")).hoverEvent(HoverEvent.showText(Component.text("Rechazar solicitud")));
  t.sendMessage(Component.text("Mando » "+f.getName()+(here?" quiere que vayas hacia él. ":" quiere ir hacia ti. "),NamedTextColor.WHITE).append(accept).append(Component.space()).append(deny));return "Solicitud enviada.";
 }
 public String accept(Player t){Req r=in.remove(t.getUniqueId());if(r==null||r.expires()<System.currentTimeMillis())return "No tienes TPA pendiente.";out.remove(r.from());Player f=Bukkit.getPlayer(r.from());if(f==null)return "El jugador se desconectó.";Player mover=r.here()?t:f,target=r.here()?f:t;if(!target.isOnline())return "El destino ya no está conectado.";if(plugin.afk().isAfk(target))return "El destino está AFK; la solicitud fue cancelada.";teleports.teleportStrict(mover,target.getLocation(),"mando.tpa.bypass",()->{mover.sendMessage("§6Mando §8» §aTPA completado.");target.sendMessage("§6Mando §8» §aTPA completado.");});return "TPA aceptado. Preparando teletransporte.";}
 public String deny(Player t){Req r=in.remove(t.getUniqueId());if(r==null)return "No tienes TPA pendiente.";out.remove(r.from());Player f=Bukkit.getPlayer(r.from());if(f!=null)f.sendMessage("§6Mando §8» §cSolicitud rechazada.");return "TPA rechazado.";}
 public String cancel(Player f){Req r=out.remove(f.getUniqueId());if(r==null)return "No tienes TPA saliente.";in.remove(r.to());Player t=Bukkit.getPlayer(r.to());if(t!=null)t.sendMessage("§6Mando §8» §7La solicitud fue cancelada.");return "TPA cancelado.";}
 private void expire(Req r){if(Objects.equals(in.get(r.to()),r)){in.remove(r.to());out.remove(r.from());Player f=Bukkit.getPlayer(r.from());if(f!=null)f.sendMessage("§6Mando §8» §7La solicitud TPA expiró.");Player t=Bukkit.getPlayer(r.to());if(t!=null)t.sendMessage("§6Mando §8» §7La solicitud TPA expiró.");}}
 @EventHandler public void quit(PlayerQuitEvent e){teleports.cancel(e.getPlayer().getUniqueId());Req a=in.remove(e.getPlayer().getUniqueId());if(a!=null)out.remove(a.from());Req z=out.remove(e.getPlayer().getUniqueId());if(z!=null)in.remove(z.to());}
}