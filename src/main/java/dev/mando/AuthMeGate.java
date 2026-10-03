package dev.mando;
import org.bukkit.event.*;import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.*;
public final class AuthMeGate implements Listener {
 private final IntegrationManager integrations;
 private static final Set<String> COMMANDS=Set.of("god","heal","rtp","sethome","home","homes","delhome","edithome","restorehome","fly","back","spawn","setspawn","warp","warps","setwarp","addwarp","delwarp","removewarp","tools","ender","profile","mail","tpa","tpaccept","tpdeny","tpatoggle","profiletp","changelog","serverconfig","scfg","purpurgui","mando");
 public AuthMeGate(IntegrationManager i){integrations=i;}
 @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=true) public void command(PlayerCommandPreprocessEvent e){String raw=e.getMessage();String cmd=raw.substring(1).split(" ",2)[0].toLowerCase(Locale.ROOT);if(COMMANDS.contains(cmd)&&!integrations.authenticated(e.getPlayer()))e.setCancelled(true);}
}
