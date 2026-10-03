package dev.mando;
import org.bukkit.Bukkit;import org.bukkit.OfflinePlayer;import java.lang.reflect.*;
public final class EconomyBridge{
 public String balance(OfflinePlayer p){try{Class<?> eco=Class.forName("net.milkbowl.vault.economy.Economy");Object reg=Bukkit.getServicesManager().getRegistration(eco);if(reg==null)return null;Object provider=reg.getClass().getMethod("getProvider").invoke(reg);Object v=provider.getClass().getMethod("getBalance",OfflinePlayer.class).invoke(provider,p);return String.format(java.util.Locale.ROOT,"%.2f",((Number)v).doubleValue());}catch(Exception e){return null;}}
}