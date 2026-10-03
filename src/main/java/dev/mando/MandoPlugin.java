package dev.mando;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class MandoPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private HomeManager homes;
    private GodListener god;
    private WarpManager warps;
    private FlyListener fly;
    private BackListener back;
    private ModuleManager modules;
    private PlatformDetector platform;
    private LocationMarkerManager markers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        modules = new ModuleManager(this);
        platform = new PlatformDetector();
        boolean firstSetup=!getConfig().getBoolean("setup.completed",false);
        getLogger().info("Mando "+getPluginMeta().getVersion()+" iniciando en "+platform.platform());
        getLogger().info("Minecraft "+getServer().getMinecraftVersion()+" | Java "+System.getProperty("java.version"));
        if(firstSetup)getLogger().warning("Primera configuración pendiente. Entra como OP para abrir el asistente de Mando.");
        else getLogger().info("Configuración inicial: completada.");
        MandoCommand mando = new MandoCommand(this);
        register("mando", mando, mando);
        getServer().getPluginManager().registerEvents(new MandoJoinListener(this, mando), this);
        markers = new LocationMarkerManager(this);
        getServer().getPluginManager().registerEvents(markers, this);

        homes = new HomeManager(this);
        god = new GodListener();
        Rtp rtp = new Rtp(this);
        Commands commands = new Commands(this, homes, god, rtp, markers);

        for (String name : List.of("god", "heal", "rtp")) register(name, commands, commands);
        if (modules.enabled("homes")) for (String name : List.of("sethome", "home", "homes", "delhome")) register(name, commands, commands);

        warps = new WarpManager(this);
        fly = new FlyListener();
        back = new BackListener();
        WarpColorMenu warpColors = new WarpColorMenu(this, warps, markers);
        getServer().getPluginManager().registerEvents(warpColors, this);
        ExtraCommands extra = new ExtraCommands(this, fly, back, warps, warpColors, markers);
        for (String name : List.of("fly", "back", "tools", "ender")) register(name, extra, extra);
        if (modules.enabled("spawn")) for (String name : List.of("spawn", "setspawn")) register(name, extra, extra);
        if (modules.enabled("warps")) for (String name : List.of("warp", "setwarp", "delwarp")) register(name, extra, extra);

        getServer().getPluginManager().registerEvents(god, this);
        getServer().getPluginManager().registerEvents(fly, this);
        getServer().getPluginManager().registerEvents(back, this);
        getServer().getPluginManager().registerEvents(new ToolsMenu.Events(this), this);
        getServer().getPluginManager().registerEvents(new HomesMenu.Events(this, homes), this);

        if (modules.enabled("changelog")) {
            PluginCommand changelog = getCommand("changelog");
            if (changelog != null) changelog.setExecutor(new ChangelogCommand(this));
        }

        if (modules.enabled("server-config")) {
            ServerConfigMenu serverConfig = new ServerConfigMenu(this, modules, platform);
            register("serverconfig", serverConfig, serverConfig);
            getServer().getPluginManager().registerEvents(serverConfig, this);
        }
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) { cmd.setExecutor(executor); cmd.setTabCompleter(completer); }
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
