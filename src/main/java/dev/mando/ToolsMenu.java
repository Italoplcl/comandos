package dev.mando;

import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Menu /tools: mesas de trabajo que funcionan sin necesitar el bloque fisico. */
public final class ToolsMenu implements InventoryHolder {

    enum Tool {
        CRAFTING(Material.CRAFTING_TABLE, "tool-crafting"),
        ANVIL(Material.ANVIL, "tool-anvil"),
        SMITHING(Material.SMITHING_TABLE, "tool-smithing"),
        STONECUTTER(Material.STONECUTTER, "tool-stonecutter"),
        LOOM(Material.LOOM, "tool-loom"),
        CARTOGRAPHY(Material.CARTOGRAPHY_TABLE, "tool-cartography"),
        GRINDSTONE(Material.GRINDSTONE, "tool-grindstone"),
        ENCHANTING(Material.ENCHANTING_TABLE, "tool-enchanting"),
        ENDER(Material.ENDER_CHEST, "tool-ender");

        final Material icon;
        final String nameKey;

        Tool(Material icon, String nameKey) {
            this.icon = icon;
            this.nameKey = nameKey;
        }
    }

    private final Inventory inventory;

    private ToolsMenu(MandoPlugin plugin) {
        this.inventory = Bukkit.createInventory(this, 9, plugin.text("tools-title"));
        for (Tool t : Tool.values()) {
            ItemStack item = new ItemStack(t.icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(plugin.text(t.nameKey).decoration(TextDecoration.ITALIC, false));
            meta.lore(java.util.List.of(plugin.text("tool-lore").decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
            inventory.setItem(t.ordinal(), item);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public static void open(MandoPlugin plugin, Player p) {
        p.openInventory(new ToolsMenu(plugin).inventory);
    }

    private static void openTool(MandoPlugin plugin, Player p, Tool t) {
        switch (t) {
            case CRAFTING -> p.openWorkbench(null, true);
            case ANVIL -> p.openAnvil(null, true);
            case SMITHING -> p.openSmithingTable(null, true);
            case STONECUTTER -> p.openStonecutter(null, true);
            case LOOM -> p.openLoom(null, true);
            case CARTOGRAPHY -> p.openCartographyTable(null, true);
            case GRINDSTONE -> p.openGrindstone(null, true);
            // Solo cuenta los libreros que haya alrededor de tu posicion.
            case ENCHANTING -> p.openEnchanting(p.getLocation(), true);
            case ENDER -> {
                if (p.hasPermission("mando.ender")) p.openInventory(p.getEnderChest());
                else p.sendMessage(plugin.msg("no-permission"));
            }
        }
    }

    public static final class Events implements Listener {
        private final MandoPlugin plugin;

        public Events(MandoPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (!(e.getInventory().getHolder(false) instanceof ToolsMenu)) return;
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            if (!e.isLeftClick()) return;

            int slot = e.getRawSlot();
            if (slot < 0 || slot >= Tool.values().length) return;
            Tool tool = Tool.values()[slot];

            // Se abre en el tick siguiente para no manipular inventarios dentro del evento de clic.
            Bukkit.getScheduler().runTask(plugin, () -> openTool(plugin, p, tool));
        }

        @EventHandler
        public void onDrag(InventoryDragEvent e) {
            if (e.getInventory().getHolder(false) instanceof ToolsMenu) e.setCancelled(true);
        }
    }
}
