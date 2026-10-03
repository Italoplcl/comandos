package dev.mando;
import org.bukkit.*;import org.bukkit.boss.*;import org.bukkit.scheduler.BukkitTask;import java.time.*;
public final class ShutdownManager{
 private final MandoPlugin plugin;private BukkitTask task;private BossBar bar;private long end;
 public ShutdownManager(MandoPlugin p){plugin=p;}
 public boolean active(){return task!=null;}
 public long secondsLeft(){return active()?Math.max(0,(end-System.currentTimeMillis()+999)/1000):0;}
 public synchronized boolean scheduleMinutes(int minutes){if(minutes<1)return false;cancel(false);long total=minutes*60L;end=System.currentTimeMillis()+total*1000L;bar=Bukkit.createBossBar("Mando · apagado programado",BarColor.RED,BarStyle.SOLID);bar.setVisible(true);Bukkit.getOnlinePlayers().forEach(bar::addPlayer);task=Bukkit.getScheduler().runTaskTimer(plugin,()->tick(total),0L,20L);Bukkit.broadcastMessage("§6Mando §8» §eServidor programado para apagarse en "+minutes+" min.");return true;}
 private void tick(long total){long left=secondsLeft();for(var p:Bukkit.getOnlinePlayers())if(!bar.getPlayers().contains(p))bar.addPlayer(p);bar.setProgress(Math.max(0.0,Math.min(1.0,left/(double)total)));bar.setTitle("Mando · apagado en "+format(left));if(left<=0){finish();return;}if(left==1800||left==600||left==300||left==60||left==30||left==10)Bukkit.broadcastMessage("§6Mando §8» §eApagado en "+format(left)+".");}
 private String format(long s){return s>=60?(s/60)+"m "+(s%60)+"s":s+"s";}
 public synchronized void cancel(boolean announce){if(task!=null){task.cancel();task=null;}if(bar!=null){bar.removeAll();bar.setVisible(false);bar=null;}end=0;if(announce)Bukkit.broadcastMessage("§6Mando §8» §aApagado programado cancelado.");}
 private synchronized void finish(){if(task!=null){task.cancel();task=null;}if(bar!=null){bar.removeAll();bar=null;}plugin.backups().createCritical();Bukkit.shutdown();}
}