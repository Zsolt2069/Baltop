package hu.msizsolt.baltop.service;

import hu.msizsolt.baltop.BaltopPlugin;
import hu.msizsolt.baltop.model.BalanceEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitTask;

public final class BalanceTopService {
    private static final long REFRESH_INTERVAL_TICKS = 20L * 60L * 5L;

    private final BaltopPlugin plugin;
    private final Economy economy;
    private final AtomicBoolean refreshRunning = new AtomicBoolean(false);

    private volatile List<BalanceEntry> cachedEntries = List.of();
    private volatile BukkitTask refreshTask;

    public BalanceTopService(BaltopPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void startAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }

        refreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshNow,
                REFRESH_INTERVAL_TICKS,
                REFRESH_INTERVAL_TICKS
        );
    }

    public void stopAutoRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    public void refreshNow() {
        if (!refreshRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            List<BalanceEntry> refreshed = new ArrayList<>(offlinePlayers.length);

            for (OfflinePlayer offlinePlayer : offlinePlayers) {
                String name = offlinePlayer.getName();
                if (name == null || name.isBlank()) {
                    continue;
                }

                double balance = economy.getBalance(offlinePlayer);
                refreshed.add(new BalanceEntry(offlinePlayer.getUniqueId(), name, balance, 0));
            }

            List<BalanceEntry> sortedEntries = refreshed.stream()
                    .sorted(Comparator.comparingDouble(BalanceEntry::balance).reversed()
                            .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toCollection(ArrayList::new));

            List<BalanceEntry> rankedEntries = new ArrayList<>(sortedEntries.size());
            for (int index = 0; index < sortedEntries.size(); index++) {
                BalanceEntry entry = sortedEntries.get(index);
                rankedEntries.add(new BalanceEntry(entry.uuid(), entry.name(), entry.balance(), index + 1));
            }

            this.cachedEntries = List.copyOf(rankedEntries);
        } finally {
            refreshRunning.set(false);
        }
    }

    public List<BalanceEntry> getCachedEntries() {
        return cachedEntries;
    }

    public List<BalanceEntry> getPage(int page, int pageSize) {
        List<BalanceEntry> entries = cachedEntries;
        int fromIndex = page * pageSize;
        if (fromIndex >= entries.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + pageSize, entries.size());
        return entries.subList(fromIndex, toIndex);
    }

    public int getMaxPage(int pageSize) {
        List<BalanceEntry> entries = cachedEntries;
        if (entries.isEmpty()) {
            return 0;
        }
        return (entries.size() - 1) / pageSize;
    }

    public BalanceEntry findEntryByName(String name) {
        for (BalanceEntry entry : cachedEntries) {
            if (entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }
}
