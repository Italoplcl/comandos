package dev.esslite;

/** Centraliza el encendido/apagado real de módulos. */
public final class ModuleManager {
    private final EssLite plugin;
    public ModuleManager(EssLite plugin) { this.plugin = plugin; }
    public boolean enabled(String module) { return plugin.getConfig().getBoolean("modules." + module + ".enabled", true); }
    public boolean purpurEnabled() { return enabled("server-config") && plugin.getConfig().getBoolean("modules.server-config.purpur.enabled", true); }
    public boolean mountsEnabled() { return purpurEnabled() && plugin.getConfig().getBoolean("modules.server-config.purpur.mounts.enabled", false); }
}
