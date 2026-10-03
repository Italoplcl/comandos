package dev.mando;

import dev.mando.HomeManager.Home;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.Locale;

/** /fly, /back, /spawn, /setspawn, /warp, /setwarp, /delwarp, /tools, /ender */
public final class ExtraCommands implements CommandExecutor, TabCompleter {

    private final MandoPlugin plugin;
    private final FlyListener fly;
    private final BackListener back;
    private final WarpManager warps;
    private final WarpColorMenu warpColors;
    private final LocationMarkerManager markers;

    public ExtraCommands(MandoPlugin plugin, FlyListener fly, BackListener back, WarpManager warps, WarpColorMenu warpColors, LocationMarkerManager markers) {
        this.plugin = plugin;
        this.fly = fly;
        this.back = back;
        this.warps = warps;
        this.warpColors = warpColors;
        this.markers = markers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.msg("only-players"));
            return true;
        }
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "fly" -> fly(p);
            case "back" -> back(p);
            case "spawn" -> spawn(p);
            case "setspawn" -> setSpawn(p);
            case "warp" -> warp(p, args);
            case "setwarp" -> setWarp(p, args);
            case "delwarp" -> delWarp(p, args);
            case "tools" -> ToolsMenu.open(plugin, p);
            case "ender" -> p.openInventory(p.getEnderChest());
            default -> { }
        }
        return true;
    }

    // ---------- helpers ----------

    private void tp(Player p, Location loc, String okKey, TagResolver... resolvers) {
        plugin.teleports().teleport(p, loc, "mando.teleport.bypass-warmup", () -> p.sendMessage(plugin.msg(okKey, resolvers)));
    }

    private World spawnWorld() {
        String name = plugin.getConfig().getString("spawn.world", "");
        World w = (name == null || name.isBlank()) ? null : Bukkit.getWorld(name);
        return w != null ? w : Bukkit.getWorlds().get(0);
    }

    // ---------- comandos ----------

    private void fly(Player p) {
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
            p.sendMessage(plugin.msg("fly-creative"));
            return;
        }
        p.sendMessage(plugin.msg(fly.toggle(p) ? "fly-on" : "fly-off"));
    }

    private void back(Player p) {if(!plugin.integrations().isMandoProvider("back")){p.sendMessage(Component.text("Mando » Back pertenece al proveedor configurado."));return;}
        Location target = back.get(p.getUniqueId());
        if (target == null || target.getWorld() == null) {
            p.sendMessage(plugin.msg("back-none"));
            return;
        }
        long cooldown=back.cooldown(p);if(cooldown>0){p.sendMessage(Component.text("Mando » Espera "+cooldown+"s para volver a usar /back."));return;}
        target=back.safe(target);if(target==null){p.sendMessage(Component.text("Mando » El destino de /back no es seguro."));return;}
        Location finalTarget=target;plugin.teleports().teleport(p,finalTarget,"mando.teleport.bypass-warmup",()->{back.markUsed(p.getUniqueId());p.sendMessage(plugin.msg("back-teleported"));});
    }

    private void spawn(Player p) {if(!plugin.integrations().isMandoProvider("spawn-command")){p.sendMessage(Component.text("Mando » Spawn pertenece al proveedor configurado."));return;}
        Location dest = plugin.spawn().effective(p);
        tp(p, dest, "spawn-teleported");
    }

    private void setSpawn(Player p) {if(!plugin.integrations().isMandoProvider("global-spawn")){p.sendMessage(Component.text("Mando » Spawn pertenece al proveedor configurado."));return;}
        Location here=p.getLocation().clone();
        if(!plugin.spawn().set(here)){p.sendMessage(plugin.msg("teleport-failed"));return;}
        boolean markerOk = markers.createSpawnBanner(p);
        p.sendMessage(plugin.msg("spawn-set"));
        if (!markerOk) p.sendMessage(plugin.msg("marker-no-space"));
    }

    private void warp(Player p, String[] args) {if(!plugin.integrations().isMandoProvider("warps")){p.sendMessage(Component.text("Mando » Warps pertenece al proveedor configurado."));return;}
        if(args.length==0){WarpBrowser.open(plugin,warps,p,0,"");return;}
        String name=HomeManager.normalize(args[0]);Home h=name==null?null:warps.get(name);if(h==null){p.sendMessage(plugin.msg("warp-not-found",Placeholder.unparsed("name",args[0])));return;}Location loc=h.toLocation();if(loc==null){p.sendMessage(plugin.msg("home-world-missing"));return;}tp(p,loc,"warp-teleported",Placeholder.unparsed("name",name));
    }

    private void setWarp(Player p, String[] args) {if(!plugin.integrations().isMandoProvider("warps")){p.sendMessage(Component.text("Mando » Warps pertenece al proveedor configurado."));return;}
        if (args.length == 0) {
            p.sendMessage(plugin.msg("warp-name-required"));
            return;
        }
        String name = HomeManager.normalize(args[0]);
        if (name == null) {
            p.sendMessage(plugin.msg("invalid-name"));
            return;
        }
        if (plugin.getConfig().getBoolean("markers.warps.enabled", true) && plugin.getConfig().getBoolean("markers.warps.color-selector", true)) {
            warpColors.open(p, name);
            return;
        }
        warps.set(name, Home.of(p.getLocation()));
        p.sendMessage(plugin.msg("warp-set", Placeholder.unparsed("name", name)));
    }

    private void delWarp(Player p, String[] args) {if(!plugin.integrations().isMandoProvider("warps")){p.sendMessage(Component.text("Mando » Warps pertenece al proveedor configurado."));return;}
        if (args.length == 0) {
            p.sendMessage(plugin.msg("warp-name-required"));
            return;
        }
        String name = HomeManager.normalize(args[0]);
        if (name != null && warps.get(name)!=null && warps.remove(name)) {
            if(!markers.removeWarpMarker(name)) markers.forgetWarpMarker(name);
            p.sendMessage(plugin.msg("warp-deleted", Placeholder.unparsed("name", name)));
        } else {
            p.sendMessage(plugin.msg("warp-not-found", Placeholder.unparsed("name", args[0])));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) return List.of();
        String n = cmd.getName().toLowerCase(Locale.ROOT);
        if (!n.equals("warp") && !n.equals("setwarp") && !n.equals("delwarp")) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return warps.all().keySet().stream().filter(k -> k.startsWith(prefix)).toList();
    }
}
