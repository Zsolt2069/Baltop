package hu.msizsolt.baltop.gui;

import hu.msizsolt.baltop.model.BalanceEntry;
import hu.msizsolt.baltop.service.BalanceTopService;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class BaltopGuiManager {
    private static final int TOP_GUI_SIZE = 54;
    private static final int PLAYER_GUI_SIZE = 27;
    private static final int[] PLAYER_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PAGE_CONTENT_SIZE = PLAYER_SLOTS.length;
    private static final int PREVIOUS_SLOT = 48;
    private static final int NEXT_SLOT = 50;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

    private final BalanceTopService balanceTopService;

    public BaltopGuiManager(BalanceTopService balanceTopService) {
        this.balanceTopService = balanceTopService;
    }

    public void openTopGui(Player player, int requestedPage) {
        balanceTopService.refreshNow();

        int maxPage = balanceTopService.getMaxPage(PAGE_CONTENT_SIZE);
        int page = Math.max(0, Math.min(requestedPage, maxPage));
        TopGuiHolder holder = new TopGuiHolder(page);

        Inventory inventory = Bukkit.createInventory(holder, TOP_GUI_SIZE, Component.text("Baltop", NamedTextColor.WHITE));
        holder.setInventory(inventory);

        fillTopGuiFrame(inventory);

        List<BalanceEntry> pageEntries = balanceTopService.getPage(page, PAGE_CONTENT_SIZE);
        for (int index = 0; index < pageEntries.size() && index < PLAYER_SLOTS.length; index++) {
            inventory.setItem(PLAYER_SLOTS[index], createHead(pageEntries.get(index)));
        }

        inventory.setItem(PREVIOUS_SLOT, createNavigationItem("Previous Page", page > 0));
        inventory.setItem(NEXT_SLOT, createNavigationItem("Next Page", page < maxPage));

        player.openInventory(inventory);
    }

    public void openPlayerBalanceGui(Player viewer, BalanceEntry entry) {
        balanceTopService.refreshNow();
        BalanceEntry refreshedEntry = balanceTopService.findEntryByName(entry.name());
        if (refreshedEntry != null) {
            entry = refreshedEntry;
        }

        PlayerGuiHolder holder = new PlayerGuiHolder(entry);
        Inventory inventory = Bukkit.createInventory(holder, PLAYER_GUI_SIZE,
                Component.text(entry.name() + "'s balance", NamedTextColor.WHITE));
        holder.setInventory(inventory);

        fillWithBackground(inventory);
        inventory.setItem(13, createCenteredPlayerHead(entry));
        inventory.setItem(22, createBalanceItem(entry));
        viewer.openInventory(inventory);
    }

    public void handleClick(Player player, InventoryHolder holder, int slot) {
        if (holder instanceof TopGuiHolder topGuiHolder) {
            if (slot == PREVIOUS_SLOT && topGuiHolder.page() > 0) {
                openTopGui(player, topGuiHolder.page() - 1);
                return;
            }

            if (slot == NEXT_SLOT && topGuiHolder.page() < balanceTopService.getMaxPage(PAGE_CONTENT_SIZE)) {
                openTopGui(player, topGuiHolder.page() + 1);
                return;
            }

            int playerIndex = getPlayerSlotIndex(slot);
            if (playerIndex >= 0) {
                List<BalanceEntry> pageEntries = balanceTopService.getPage(topGuiHolder.page(), PAGE_CONTENT_SIZE);
                if (playerIndex < pageEntries.size()) {
                    openPlayerBalanceGui(player, pageEntries.get(playerIndex));
                }
            }
        }
    }

    public boolean isBaltopInventory(InventoryHolder holder) {
        return holder instanceof TopGuiHolder || holder instanceof PlayerGuiHolder;
    }

    private void fillWithBackground(Inventory inventory) {
        ItemStack filler = createFiller();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void fillTopGuiFrame(Inventory inventory) {
        ItemStack filler = createFiller();

        for (int slot = 0; slot <= 8; slot++) {
            inventory.setItem(slot, filler);
        }

        for (int slot = 45; slot <= 53; slot++) {
            inventory.setItem(slot, filler);
        }

        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, filler);
            inventory.setItem((row * 9) + 8, filler);
        }
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(" ", NamedTextColor.WHITE));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(String text, boolean enabled) {
        ItemStack item = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(text, enabled ? NamedTextColor.GOLD : NamedTextColor.GRAY));
        if (!enabled) {
            meta.lore(List.of(text("No page available", NamedTextColor.DARK_GRAY)));
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHead(BalanceEntry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid());
        meta.setOwningPlayer(offlinePlayer);
        meta.displayName(text("#" + entry.rank() + " " + entry.name(), NamedTextColor.LIGHT_PURPLE));

        List<Component> lore = new ArrayList<>();
        lore.add(text("Value: " + formatBalance(entry.balance()), NamedTextColor.GOLD));
        lore.add(text("Rank: #" + entry.rank(), NamedTextColor.YELLOW));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCenteredPlayerHead(BalanceEntry entry) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.uuid()));
        meta.displayName(text(entry.name(), NamedTextColor.WHITE));

        List<Component> lore = new ArrayList<>();
        lore.add(text("Rank: #" + entry.rank(), NamedTextColor.YELLOW));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBalanceItem(BalanceEntry entry) {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text(formatBalance(entry.balance()), NamedTextColor.GREEN));
        meta.lore(List.of(text("Current balance", NamedTextColor.DARK_GREEN)));
        item.setItemMeta(meta);
        return item;
    }

    private String formatBalance(double value) {
        double absolute = Math.abs(value);
        if (absolute >= 1_000_000_000_000.0D) {
            return DECIMAL_FORMAT.format(value / 1_000_000_000_000.0D) + "T";
        }
        if (absolute >= 1_000_000_000.0D) {
            return DECIMAL_FORMAT.format(value / 1_000_000_000.0D) + "B";
        }
        if (absolute >= 1_000_000.0D) {
            return DECIMAL_FORMAT.format(value / 1_000_000.0D) + "M";
        }
        if (absolute >= 1_000.0D) {
            return DECIMAL_FORMAT.format(value / 1_000.0D) + "K";
        }
        return DECIMAL_FORMAT.format(value);
    }

    private int getPlayerSlotIndex(int clickedSlot) {
        for (int index = 0; index < PLAYER_SLOTS.length; index++) {
            if (PLAYER_SLOTS[index] == clickedSlot) {
                return index;
            }
        }
        return -1;
    }

    private Component text(String content, NamedTextColor color) {
        return Component.text(content, color).decoration(TextDecoration.ITALIC, false);
    }

    private static final class TopGuiHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private TopGuiHolder(int page) {
            this.page = page;
        }

        public int page() {
            return page;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class PlayerGuiHolder implements InventoryHolder {
        private Inventory inventory;

        private PlayerGuiHolder(BalanceEntry entry) {
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
