package dev.mando;

import dev.mando.HomeManager.Home;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class WarpColorMenu implements Listener {
    private static final String TITLE = "§8Mando • Color del Warp";
    private static final DyeColor[] COLORS = {DyeColor.RED,DyeColor.ORANGE,DyeColor.YELLOW,DyeColor.LIME,DyeColor.GREEN,DyeColor.LIGHT_BLUE,DyeColor.BLUE,DyeColor.PURPLE,DyeColor.WHITE};
    private final Mando plugin; private final WarpManager warps; private final LocationMarkerManager markers;
    private record Pending(String name, Location location) {}
    private final Map<UUID,Pending> pending = new HashMap<>();
    public WarpColorMenu(Mando plugin, WarpManager warps, LocationMarkerManager markers){this.plugin=plugin;this.warps=warps;this.markers=markers;}
    public void open(Player p,String name){pending.put(p.getUniqueId(),new Pending(name,p.getLocation().clone()));Inventory inv=Bukkit.createInventory(null,9,TITLE);for(int i=0;i<COLORS.length;i++){Material m=Material.valueOf(COLORS[i].name()+"_BANNER");ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName("§f"+pretty(COLORS[i]));it.setItemMeta(meta);inv.setItem(i,it);}p.openInventory(inv);}
    @EventHandler public void click(InventoryClickEvent e){if(!TITLE.equals(e.getView().getTitle()))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p))return;int s=e.getRawSlot();if(s<0||s>=COLORS.length)return;Pending choice=pending.remove(p.getUniqueId());if(choice==null)return;String name=choice.name();Location loc=choice.location();warps.set(name,Home.of(loc));boolean marker=markers.createWarpBanner(loc,name,COLORS[s]);p.closeInventory();p.sendMessage(plugin.msg("warp-set",net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name",name)));if(!marker)p.sendMessage(plugin.msg("marker-no-space"));}
    private String pretty(DyeColor c){return switch(c){case LIGHT_BLUE->"Celeste";case LIME->"Lima";case PURPLE->"Morado";case WHITE->"Blanco";case RED->"Rojo";case ORANGE->"Naranja";case YELLOW->"Amarillo";case GREEN->"Verde";case BLUE->"Azul";default->c.name();};}
}
