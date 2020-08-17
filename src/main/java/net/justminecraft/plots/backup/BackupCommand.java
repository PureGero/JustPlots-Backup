package net.justminecraft.plots.backup;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public class BackupCommand extends SubCommand {
    private final JustPlotsBackup plugin;

    // Used to prevent players abusing the backup command
    private HashMap<String, Long> lastBackups = new HashMap<>();

    public BackupCommand(JustPlotsBackup plugin) {
        super("/p backup", "Make a backup of this plot", "backup");

        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return false;
        }

        Plot plot = JustPlots.getPlotAt((Player) sender);

        if (plot == null) {
            sender.sendMessage(ChatColor.RED + "You are not standing on a plot");
            return false;
        }

        if (!plot.isOwner((Player) sender) && !sender.hasPermission("justplots.add.other")) {
            sender.sendMessage(ChatColor.RED + JustPlots.getUsername(plot.getOwner()) + " owns that plot");
            return false;
        }

        long lastBackup = lastBackups.getOrDefault(getKey((Player) sender, plot), 0L);

        if (lastBackup > System.currentTimeMillis() - JustPlotsBackup.MINUTES_BETWEEN_MANUAL_BACKUPS * 60 * 1000) {
            sender.sendMessage(ChatColor.RED + "You can only make a backup of your plot every " + JustPlotsBackup.MINUTES_BETWEEN_MANUAL_BACKUPS + " minutes");
            return false;
        }

        plugin.getBackupManager().backup(plot);

        lastBackups.put(getKey((Player) sender, plot), System.currentTimeMillis());

        sender.sendMessage(ChatColor.GREEN + "Succesfully backed up plot " + plot);

        return true;
    }

    private String getKey(Player player, Plot plot) {
        return player.getUniqueId() + ";" + plot;
    }

    public void onTabComplete(CommandSender sender, String[] args, List<String> tabCompletion) {

    }
}
