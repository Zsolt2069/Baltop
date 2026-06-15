package hu.msizsolt.baltop.command;

import hu.msizsolt.baltop.gui.BaltopGuiManager;
import hu.msizsolt.baltop.model.BalanceEntry;
import hu.msizsolt.baltop.service.BalanceTopService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BaltopCommand implements CommandExecutor, TabCompleter {
    private final BaltopGuiManager guiManager;
    private final BalanceTopService balanceTopService;

    public BaltopCommand(BaltopGuiManager guiManager, BalanceTopService balanceTopService) {
        this.guiManager = guiManager;
        this.balanceTopService = balanceTopService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        balanceTopService.refreshNow();

        if (args.length == 0) {
            guiManager.openTopGui(player, 0);
            return true;
        }

        if (args.length == 1) {
            BalanceEntry entry = balanceTopService.findEntryByName(args[0]);
            if (entry == null) {
                player.sendMessage(Component.text("That player was not found in the cached balance list.", NamedTextColor.RED));
                return true;
            }

            guiManager.openPlayerBalanceGui(player, entry);
            return true;
        }

        player.sendMessage(Component.text("Usage: /" + label + " [player]", NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (BalanceEntry entry : balanceTopService.getCachedEntries()) {
            if (entry.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(entry.name());
                if (matches.size() >= 20) {
                    break;
                }
            }
        }
        return matches;
    }
}
