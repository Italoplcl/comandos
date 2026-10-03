package dev.mando;

/** Centraliza el encendido/apagado real de módulos. */
public final class ModuleManager {
    private final Mando plugin;
    public ModuleManager(Mando plugin) { this.plugin = plugin; }
    public boolean enabled(String module) { return plugin.getConfig().getBoolean("modules." + module + ".enabled", true); }
    public boolean purpurEnabled() { return enabled("server-config") && plugin.getConfig().getBoolean("modules.server-config.purpur.enabled", true); }
    private boolean purpurModule(String name, boolean def) { return purpurEnabled() && plugin.getConfig().getBoolean("modules.server-config.purpur." + name + ".enabled", def); }
    public boolean mountsEnabled() { return purpurModule("mounts", false); }
    public boolean mobsEnabled() { return purpurModule("mobs", true); }
    public boolean gameplayEnabled() { return purpurModule("gameplay", true); }
    public boolean breedingEnabled() { return purpurModule("breeding", true); }
    public boolean raidsEnabled() { return purpurModule("raids", true); }
}
