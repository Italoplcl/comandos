package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerCommandPreprocessEvent;import java.lang.reflect.*;
public final class AuthGuardListener implements Listener{
 private final MandoPlugin plugin;public AuthGuardListener(MandoPlugin p){plugin=p;}
 public boolean authenticated(Player p){if(Bukkit.getPluginManager().getPlugin("AuthMe")==null)return true;try{Class<?> c=Class.forName("fr.xephi.authme.api.v3.AuthMeApi");Object api=c.getMethod("getInstance").invoke(null);return Boolean.TRUE.equals(c.getMethod("isAuthenticated",Player.class).invoke(api,p));}catch(Exception e){return false;}}
 @EventHandler(priority=EventPriority.LOWEST) public void command(PlayerCommandPreprocessEvent e){if(authenticated(e.getPlayer()))return;String root=e.getMessage().substring(1).split(" ")[0].toLowerCase();if(root.equals("login")||root.equals("l")||root.equals("register")||root.equals("reg"))return;if(plugin.getCommand(root)!=null){e.setCancelled(true);e.getPlayer().sendMessage("§cAutentícate antes de usar Mando.");}}
}