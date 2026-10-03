package dev.mando;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import java.lang.reflect.Method;
import java.util.*;

public final class IntegrationManager implements Listener {
 public enum AfkProvider { AUTO, PURPUR, ESSENTIALSX, CMI, MANDO, NONE }
 private final Mando plugin; private final Set<UUID> mandoAfk=new HashSet<>(); private final Map<UUID,Long> activity=new HashMap<>();
 private AfkProvider selected;
 public IntegrationManager(Mando p){plugin=p;selected=parse(p.getConfig().getString("integrations.afk.provider","AUTO"));resolve();}
 public boolean has(String name){return Bukkit.getPluginManager().getPlugin(name)!=null;}
 public AfkProvider afkProvider(){return selected;}
 public void select(AfkProvider p){selected=p;plugin.getConfig().set("integrations.afk.provider",p.name());plugin.saveConfig();resolve();}
 public String summary(){return "AFK="+selected+" | AuthMe="+yn(has("AuthMe"))+" | Vault="+yn(has("Vault"))+" | TAB="+yn(has("TAB"));}
 public boolean authenticated(Player p){
  if(!has("AuthMe"))return true;
  try{Class<?> api=Class.forName("fr.xephi.authme.api.v3.AuthMeApi");Object inst=api.getMethod("getInstance").invoke(null);return (boolean)api.getMethod("isAuthenticated",Player.class).invoke(inst,p);}
  catch(Throwable ignored){return false;}
 }
 public boolean isAfk(Player p){
  return switch(selected){
   case NONE->false; case MANDO->mandoAfk.contains(p.getUniqueId());
   case PURPUR->reflectBool(p,"isAfk");
   case ESSENTIALSX->essAfk(p);
   case CMI->cmiAfk(p);
   case AUTO->false;
  };
 }
 public Double vaultBalance(OfflinePlayer p){
  if(!has("Vault"))return null;
  try{Class<?> eco=Class.forName("net.milkbowl.vault.economy.Economy");Object rsp=Bukkit.getServicesManager().getRegistration(eco).getProvider();return ((Number)eco.getMethod("getBalance",OfflinePlayer.class).invoke(rsp,p)).doubleValue();}
  catch(Throwable ignored){return null;}
 }
 @EventHandler public void join(PlayerJoinEvent e){activity.put(e.getPlayer().getUniqueId(),System.currentTimeMillis());}
 @EventHandler public void quit(PlayerQuitEvent e){activity.remove(e.getPlayer().getUniqueId());mandoAfk.remove(e.getPlayer().getUniqueId());}
 @EventHandler public void move(PlayerMoveEvent e){if(e.getTo()!=null&&(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getY()!=e.getTo().getY()||e.getFrom().getZ()!=e.getTo().getZ()))active(e.getPlayer());}
 @EventHandler public void chat(AsyncPlayerChatEvent e){active(e.getPlayer());}
 @EventHandler public void command(PlayerCommandPreprocessEvent e){active(e.getPlayer());}
 public void tick(){if(selected!=AfkProvider.MANDO)return;long now=System.currentTimeMillis(),ms=Math.max(30,plugin.getConfig().getLong("integrations.afk.mando.timeout-seconds",300))*1000L;for(Player p:Bukkit.getOnlinePlayers()){long last=activity.getOrDefault(p.getUniqueId(),now);if(now-last>=ms)mandoAfk.add(p.getUniqueId());}}
 private void active(Player p){activity.put(p.getUniqueId(),System.currentTimeMillis());mandoAfk.remove(p.getUniqueId());}
 private void resolve(){if(selected!=AfkProvider.AUTO)return;if(has("CMI"))selected=AfkProvider.CMI;else if(has("Essentials"))selected=AfkProvider.ESSENTIALSX;else if(isPurpur())selected=AfkProvider.PURPUR;else selected=AfkProvider.MANDO;plugin.getLogger().info("Proveedor AFK automático: "+selected);}
 private boolean isPurpur(){try{Class.forName("org.purpurmc.purpur.PurpurConfig");return true;}catch(Throwable ignored){return false;}}
 private boolean essAfk(Player p){try{Object pl=Bukkit.getPluginManager().getPlugin("Essentials");Method getUser=pl.getClass().getMethod("getUser",UUID.class);Object u=getUser.invoke(pl,p.getUniqueId());return (boolean)u.getClass().getMethod("isAfk").invoke(u);}catch(Throwable ignored){return false;}}
 private boolean cmiAfk(Player p){try{Class<?> c=Class.forName("com.Zrips.CMI.CMI");Object inst=c.getMethod("getInstance").invoke(null);Object pm=inst.getClass().getMethod("getPlayerManager").invoke(inst);Object u=pm.getClass().getMethod("getUser",UUID.class).invoke(pm,p.getUniqueId());return (boolean)u.getClass().getMethod("isAfk").invoke(u);}catch(Throwable ignored){return false;}}
 private boolean reflectBool(Object o,String m){try{return (boolean)o.getClass().getMethod(m).invoke(o);}catch(Throwable ignored){return false;}}
 private AfkProvider parse(String s){try{return AfkProvider.valueOf(s.toUpperCase(Locale.ROOT));}catch(Exception e){return AfkProvider.AUTO;}}
 private String yn(boolean b){return b?"sí":"no";}
}
