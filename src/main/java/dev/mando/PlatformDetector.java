package dev.mando;

public final class PlatformDetector {
    public enum Platform { PAPER, PURPUR, UNKNOWN }
    private final Platform platform;
    public PlatformDetector() {
        Platform p;
        try { Class.forName("org.purpurmc.purpur.PurpurConfig"); p = Platform.PURPUR; }
        catch (Throwable ignored) {
            try { Class.forName("io.papermc.paper.configuration.GlobalConfiguration"); p = Platform.PAPER; }
            catch (Throwable ignored2) { p = Platform.UNKNOWN; }
        }
        this.platform = p;
    }
    public Platform platform() { return platform; }
    public boolean isPurpur() { return platform == Platform.PURPUR; }
    public boolean isPaperFamily() { return platform == Platform.PAPER || platform == Platform.PURPUR; }
}
