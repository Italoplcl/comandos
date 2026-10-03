package dev.mando;
import net.kyori.adventure.bossbar.BossBar;import net.kyori.adventure.text.Component;import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.player.PlayerJoinEvent;import org.bukkit.scheduler.BukkitTask;
import java.util.*;
public final class AnnouncementManager implements Listener {
 private static final MiniMessage MM=MiniMessage.miniMessage();private final Mando plugin;private int index;private BukkitTask rotation;
 public AnnouncementManager(Mando p){plugin=p;start();}
 private void start(){if(!plugin.getConfig().getBoolean("announcements.enabled",false))return;long sec=Math.max(30,plugin.getConfig().getLong("announcements.interval-seconds",300));rotation=Bukkit.getScheduler().runTaskTimer(plugin,this::next,sec*20L,sec*20L);}
 private void next(){List<Map<?,?>> list=plugin.getConfig().getMapList("announcements.items");if(list.isEmpty())return;show(list.get(index++%list.size()));}
 @EventHandler public void join(PlayerJoinEvent e){if(!plugin.getConfig().getBoolean("announcements.enabled",false)||!plugin.getConfig().getBoolean("announcements.join.enabled",false))return;long d=Math.max(0,plugin.getConfig().getLong("announcements.join.delay-seconds",3))*20L;Bukkit.getScheduler().runTaskLater(plugin,()->{String m=plugin.getConfig().getString("announcements.join.message","");if(!m.isBlank())e.getPlayer().sendMessage(MM.deserialize(m));},d);}
 private void show(Map<?,?> m){Object mv=m.get("message");String text=mv==null?"":String.valueOf(mv);List<String> channels=strings(m.get("channels"));Component c=MM.deserialize(text);int duration=num(m.get("duration-seconds"),5);String command=m.get("command")==null?"":String.valueOf(m.get("command"));
  for(Player p:Bukkit.getOnlinePlayers()){if(channels.contains("CHAT"))p.sendMessage(c);if(channels.contains("ACTIONBAR"))p.sendActionBar(c);if(channels.contains("TITLE"))p.showTitle(net.kyori.adventure.title.Title.title(c,Component.empty()));}
  if(channels.contains("BOSSBAR"))boss(c,duration,command);else if(!command.isBlank())Bukkit.getScheduler().runTaskLater(plugin,()->Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command),duration*20L);
 }
 private void boss(Component text,int seconds,String command){BossBar bar=BossBar.bossBar(text,1f,BossBar.Color.BLUE,BossBar.Overlay.PROGRESS);for(Player p:Bukkit.getOnlinePlayers())p.showBossBar(bar);final int total=Math.max(1,seconds);final int[] left={total};BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(plugin,()->{left[0]--;bar.progress(Math.max(0f,(float)left[0]/total));if(left[0]<=0){for(Player p:Bukkit.getOnlinePlayers())p.hideBossBar(bar);task[0].cancel();if(!command.isBlank())Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);}},20L,20L);}
 private List<String> strings(Object o){if(!(o instanceof List<?> l))return List.of("CHAT");return l.stream().map(String::valueOf).map(String::toUpperCase).toList();}private int num(Object o,int d){return o instanceof Number n?n.intValue():d;}
}
