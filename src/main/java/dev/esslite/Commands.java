package dev.esslite;

import dev.esslite.HomeManager.Home;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Commands implements CommandExecutor, TabCompleter {

    private static final int MENU_CAP = 54;

    private final EssLite plugin;
    private final HomeManager homes;
    private final GodListener god;
    private final Rtp rtp;
    private final LocationMarkerManager markers;

    public Commands(EssLite plugin, HomeManager homes, GodListener god, Rtp rtp, LocationMarkerManager markers) {
        this.plugin = plugin;
        this.homes = homes;
        this.god = god;
        this.rtp = rtp;
        this.markers = markers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.msg("only-players"));
            return true;
        }
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "god" -> p.sendMessage(plugin.msg(god.toggle(p) ? "god-on" : "god-off"));
            case "heal" -> heal(p);
            case "sethome" -> setHome(p, args);
            case "home" -> home(p, args);
            case "homes" -> HomesMenu.open(plugin, homes, p);
            case "delhome" -> delHome(p, args);
            case "edithome" -> editHome(p,args);
            case "restorehome" -> restoreHome(p,args);
            case "rtp" -> rtp.start(p);
            default -> { }
        }
        return true;
    }

    private void heal(Player p) {
        GodListener.fill(p);
        if (plugin.getConfig().getBoolean("heal.restore-health", false)) {
            AttributeInstance max = p.getAttribute(Attribute.MAX_HEALTH);
            if (max != null) p.setHealth(max.getValue());
        }
        p.sendMessage(plugin.msg("heal-done"));
    }

    private void setHome(Player p, String[] args) {
        String name = args.length == 0
                ? plugin.getConfig().getString("homes.default-name", "home")
                : HomeManager.normalize(args[0]);
        if (name == null) {
            p.sendMessage(plugin.msg("invalid-name"));
            return;
        }

        UUID id = p.getUniqueId();
        Map<String, Home> all = homes.all(id);
        int cap = p.hasPermission("esslite.homes.unlimited")
                ? MENU_CAP
                : Math.min(plugin.getConfig().getInt("homes.max", 3), MENU_CAP);
        if (!all.containsKey(name) && all.size() >= cap) {
            p.sendMessage(plugin.msg("homes-limit", Placeholder.unparsed("max", String.valueOf(cap))));
            return;
        }

        homes.set(id, name, Home.of(p.getLocation()));
        boolean markerOk = markers.createHomeSign(p, name);
        p.sendMessage(plugin.msg("home-set", Placeholder.unparsed("name", name)));
        if (!markerOk) p.sendMessage(plugin.msg("home-marker-failed"));
    }

    private void home(Player p, String[] args) {
        Map<String, Home> all = homes.all(p.getUniqueId());
        if (all.isEmpty()) {
            p.sendMessage(plugin.msg("no-homes"));
            return;
        }

        String name;
        if (args.length == 0) {
            String def = plugin.getConfig().getString("homes.default-name", "home");
            if (all.containsKey(def)) {
                name = def;
            } else if (all.size() == 1) {
                name = all.keySet().iterator().next();
            } else {
                p.sendMessage(plugin.msg("home-choose"));
                return;
            }
        } else {
            name = HomeManager.normalize(args[0]);
        }

        Home h = name == null ? null : all.get(name);
        if (h == null) {
            p.sendMessage(plugin.msg("home-not-found",
                    Placeholder.unparsed("name", args.length > 0 ? args[0] : String.valueOf(name))));
            return;
        }
        homes.teleport(p, name, h);
    }

    private void delHome(Player p, String[] args) {
        if (args.length == 0) {
            p.sendMessage(plugin.msg("home-choose"));
            return;
        }
        String name = HomeManager.normalize(args[0]);
        if (name != null && homes.remove(p.getUniqueId(), name)) {
            p.sendMessage(plugin.msg("home-deleted", Placeholder.unparsed("name", name)));
        } else {
            p.sendMessage(plugin.msg("home-not-found", Placeholder.unparsed("name", args[0])));
        }
    }

    private void editHome(Player p,String[] args) {
        if(args.length<2){p.sendMessage(plugin.msg("home-edit-usage"));return;}
        String name=HomeManager.normalize(args[0]); Home h=name==null?null:homes.all(p.getUniqueId()).get(name);
        if(h==null){p.sendMessage(plugin.msg("home-not-found",Placeholder.unparsed("name",args[0])));return;}
        if(!args[1].equalsIgnoreCase("icon")||args.length<3){p.sendMessage(plugin.msg("home-edit-usage"));return;}
        org.bukkit.Material mat=org.bukkit.Material.matchMaterial(args[2]);
        if(mat==null||!mat.isItem()){p.sendMessage(plugin.msg("home-icon-invalid"));return;}
        homes.set(p.getUniqueId(),name,h.withIcon(mat.name()));
        p.sendMessage(plugin.msg("home-icon-set",Placeholder.unparsed("name",name),Placeholder.unparsed("icon",mat.name())));
    }

    private void restoreHome(Player p,String[] args){
        if(args.length<2){p.sendMessage(plugin.msg("home-restore-usage"));return;}
        String name=HomeManager.normalize(args[0]); if(name==null){p.sendMessage(plugin.msg("invalid-name"));return;}
        boolean ok=args[1].equalsIgnoreCase("deleted")?homes.restoreDeleted(p.getUniqueId(),name):
                args[1].equalsIgnoreCase("previous")&&homes.restorePrevious(p.getUniqueId(),name);
        p.sendMessage(plugin.msg(ok?"home-restored":"home-restore-none",Placeholder.unparsed("name",name)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p) || args.length != 1) return List.of();
        String n = cmd.getName().toLowerCase(Locale.ROOT);
        if (!n.equals("home") && !n.equals("sethome") && !n.equals("delhome") && !n.equals("edithome") && !n.equals("restorehome")) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return homes.all(p.getUniqueId()).keySet().stream().filter(k -> k.startsWith(prefix)).toList();
    }
}
