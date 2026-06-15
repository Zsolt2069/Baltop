package hu.msizsolt.baltop;

import hu.msizsolt.baltop.command.BaltopCommand;
import hu.msizsolt.baltop.gui.BaltopGuiListener;
import hu.msizsolt.baltop.gui.BaltopGuiManager;
import hu.msizsolt.baltop.service.BalanceTopService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class BaltopPlugin extends JavaPlugin {
    private Economy economy;
    private BalanceTopService balanceTopService;
    private BaltopGuiManager guiManager;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vault economy provider not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.balanceTopService = new BalanceTopService(this, economy);
        this.guiManager = new BaltopGuiManager(balanceTopService);

        PluginCommand baltopCommand = getCommand("baltop");
        if (baltopCommand == null) {
            getLogger().severe("Command /baltop is missing from plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        BaltopCommand command = new BaltopCommand(guiManager, balanceTopService);
        baltopCommand.setExecutor(command);
        baltopCommand.setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(new BaltopGuiListener(guiManager), this);

        balanceTopService.refreshNow();
        balanceTopService.startAutoRefresh();
    }

    @Override
    public void onDisable() {
        if (balanceTopService != null) {
            balanceTopService.stopAutoRefresh();
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> registration =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            return false;
        }

        this.economy = registration.getProvider();
        return this.economy != null;
    }
}
