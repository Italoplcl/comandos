package dev.mando;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/** Primera integración visual con los Dialogs nativos modernos de Paper. */
public final class ChangelogCommand implements CommandExecutor {
    private final Mando plugin;
    public ChangelogCommand(Mando plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(plugin.msg("only-players")); return true; }
        String version = plugin.getConfig().getString("changelog.version", plugin.getPluginMeta().getVersion());
        List<String> lines = plugin.getConfig().getStringList("changelog.lines");
        Component assembledBody = Component.empty();
        for (String line : lines) {
            assembledBody = assembledBody.append(Component.text("• " + line + "\n"));
        }
        final Component body = assembledBody;
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Novedades del servidor · " + version))
                        .body(List.of(DialogBody.plainMessage(body)))
                        .build())
                .type(DialogType.notice()));
        p.showDialog(dialog);
        return true;
    }
}
