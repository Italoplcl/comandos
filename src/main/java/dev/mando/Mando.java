package dev.mando;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Mando extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private HomeManager homes;
    private GodListener god;
    private WarpManager warps;
    private FlyListener fly;
    private BackListener back;
    private ModuleManager modules;
    private PlatformDetector platform;
    private LocationMarkerManager markers;
    private MailManager mail;
    private DeathHistory deaths;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        modules = new ModuleManager(this);
        platform = new PlatformDetector();
        getLogger().info("Plataforma detectada: " + platform.platform());
        markers = new LocationMarkerManager(this);
        getServer().getPluginManager().registerEvents(markers, this);

        homes = new HomeManager(this);
        mail = new MailManager(this);
        deaths = new DeathHistory(this);
        TpaManager tpa = new TpaManager(this);
        getServer().getPluginManager().registerEvents(deaths, this);
        god = new GodListener();
        Rtp rtp = new Rtp(this);
        Commands commands = new Commands(this, homes, god, rtp, markers);

        for (String name : List.of("god", "heal", "rtp")) register(name, commands, commands);
        if (modules.enabled("homes")) for (String name : List.of("sethome", "home", "homes", "delhome", "edithome", "restorehome")) register(name, commands, commands);

        warps = new WarpManager(this);
        fly = new FlyListener();
        back = new BackListener();
        TeleportService teleports = new TeleportService(this, back);
        homes.teleports(teleports);
        rtp.teleports(teleports);
        getServer().getPluginManager().registerEvents(teleports, this);
        WarpColorMenu warpColors = new WarpColorMenu(this, warps, markers);
        getServer().getPluginManager().registerEvents(warpColors, this);
        ExtraCommands extra = new ExtraCommands(this, fly, back, warps, warpColors, markers, teleports);
        for (String name : List.of("fly", "back", "tools", "ender")) register(name, extra, extra);
        if (modules.enabled("spawn")) for (String name : List.of("spawn", "setspawn")) register(name, extra, extra);
        if (modules.enabled("warps")) for (String name : List.of("warp", "warps", "setwarp", "delwarp")) register(name, extra, extra);

        getServer().getPluginManager().registerEvents(god, this);
        getServer().getPluginManager().registerEvents(fly, this);
        getServer().getPluginManager().registerEvents(back, this);
        getServer().getPluginManager().registerEvents(new ToolsMenu.Events(this), this);
        getServer().getPluginManager().registerEvents(new HomesMenu.Events(this, homes), this);
        ProfileCommand profile = new ProfileCommand(this, homes, mail, tpa, deaths, teleports);
        if (modules.enabled("profile")) register("profile", profile, profile);
        if (modules.enabled("mail")) register("mail", profile, profile);
        if (modules.enabled("tpa")) for (String name : List.of("tpa","tpaccept","tpdeny","tpatoggle")) register(name, profile, profile);
        PluginCommand profileTp = getCommand("profiletp"); if (profileTp != null) profileTp.setExecutor(new ProfileTpCommand(this, homes, deaths, teleports));

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
        if (mail != null) mail.save();
        if (deaths != null) deaths.save();
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
