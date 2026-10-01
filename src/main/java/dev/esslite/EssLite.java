package dev.esslite;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class EssLite extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private HomeManager homes;
    private GodListener god;
    private WarpManager warps;
    private FlyListener fly;
    private BackListener back;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        homes = new HomeManager(this);
        god = new GodListener();
        Rtp rtp = new Rtp(this);
        Commands commands = new Commands(this, homes, god, rtp);

        for (String name : List.of("god", "heal", "sethome", "home", "homes", "delhome", "rtp")) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(commands);
                cmd.setTabCompleter(commands);
            }
        }

        warps = new WarpManager(this);
        fly = new FlyListener();
        back = new BackListener();
        ExtraCommands extra = new ExtraCommands(this, fly, back, warps);
        for (String name : List.of("fly", "back", "spawn", "setspawn", "warp", "setwarp", "delwarp", "tools", "ender")) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(extra);
                cmd.setTabCompleter(extra);
            }
        }

        getServer().getPluginManager().registerEvents(god, this);
        getServer().getPluginManager().registerEvents(fly, this);
        getServer().getPluginManager().registerEvents(back, this);
        getServer().getPluginManager().registerEvents(new ToolsMenu.Events(this), this);
        getServer().getPluginManager().registerEvents(new HomesMenu.Events(this, homes), this);

        ServerConfigMenu serverConfig = new ServerConfigMenu(this);
        PluginCommand serverConfigCommand = getCommand("serverconfig");
        if (serverConfigCommand != null) {
            serverConfigCommand.setExecutor(serverConfig);
            serverConfigCommand.setTabCompleter(serverConfig);
        }
        getServer().getPluginManager().registerEvents(serverConfig, this);
    }

    @Override
    public void onDisable() {
        if (god != null) god.disableAll();
        if (fly != null) fly.disableAll();
        if (homes != null) homes.saveNow();
        if (warps != null) warps.saveNow();
    }

    /** Mensaje sin prefijo. */
    public Component text(String key, TagResolver... resolvers) {
        String raw = getConfig().getString("messages." + key, "<red>Falta el mensaje: " + key);
        return MM.deserialize(raw, resolvers);
    }

    /** Mensaje con prefijo. */
    public Component msg(String key, TagResolver... resolvers) {
        Component prefix = MM.deserialize(getConfig().getString("messages.prefix", ""));
        return prefix.append(text(key, resolvers));
    }

    public List<Component> list(String key, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        for (String line : getConfig().getStringList("messages." + key)) {
            out.add(MM.deserialize(line, resolvers));
        }
        return out;
    }
}
