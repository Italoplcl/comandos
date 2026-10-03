package dev.mando;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HomesMenu implements InventoryHolder {

    private final Inventory inventory;
    private final List<String> names = new ArrayList<>();

    private HomesMenu(Mando plugin, Map<String, HomeManager.Home> homes) {
        int rows = Math.max(1, Math.min(6, (homes.size() + 8) / 9));
        int size = rows * 9;
        this.inventory = Bukkit.createInventory(this, size, plugin.text("menu-title"));

        for (var entry : homes.entrySet()) {
            if (names.size() >= size) break;
            HomeManager.Home h = entry.getValue();

            TagResolver[] placeholders = {
                    Placeholder.unparsed("name", entry.getKey()),
                    Placeholder.unparsed("world", h.world()),
                    Placeholder.unparsed("x", String.valueOf((int) Math.floor(h.x()))),
                    Placeholder.unparsed("y", String.valueOf((int) Math.floor(h.y()))),
                    Placeholder.unparsed("z", String.valueOf((int) Math.floor(h.z())))
            };

            ItemStack item = new ItemStack(icon(h));
            ItemMeta meta = item.getItemMeta();
            meta.displayName(plugin.text("menu-item-name", placeholders).decoration(TextDecoration.ITALIC, false));
            meta.lore(plugin.list("menu-lore", placeholders).stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
            item.setItemMeta(meta);

            inventory.setItem(names.size(), item);
            names.add(entry.getKey());
        }
    }

    private static Material icon(HomeManager.Home h) {
        Material configured = Material.matchMaterial(h.icon());
        if (configured != null && configured.isItem()) return configured;
        World w = Bukkit.getWorld(h.world());
        if (w == null) return Material.BARRIER;
        return switch (w.getEnvironment()) { case NETHER -> Material.NETHERRACK; case THE_END -> Material.END_STONE; default -> Material.GRASS_BLOCK; };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(Mando plugin, HomeManager homes, Player p) {
        Map<String, HomeManager.Home> all = homes.all(p.getUniqueId());
        if (all.isEmpty()) {
            p.sendMessage(plugin.msg("no-homes"));
            return;
        }
        p.openInventory(new HomesMenu(plugin, all).inventory);
    }

    public static final class Events implements Listener {
        private final Mando plugin;
        private final HomeManager homes;

        public Events(Mando plugin, HomeManager homes) {
            this.plugin = plugin;
            this.homes = homes;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder(false) instanceof HomesMenu menu)) return;
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;

            int slot = e.getRawSlot();
            if (slot < 0 || slot >= menu.names.size()) return;
            String name = menu.names.get(slot);

            if (e.isShiftClick() && e.isRightClick()) {
                UUID id = p.getUniqueId();
                if (homes.remove(id, name)) {
                    p.sendMessage(plugin.msg("home-deleted", Placeholder.unparsed("name", name)));
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (homes.all(id).isEmpty()) p.closeInventory();
                    else open(plugin, homes, p);
                });
            } else if (e.isLeftClick()) {
                HomeManager.Home home = homes.all(p.getUniqueId()).get(name);
                p.closeInventory();
                if (home != null) homes.teleport(p, name, home);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder(false) instanceof HomesMenu) e.setCancelled(true);
        }
    }
}
