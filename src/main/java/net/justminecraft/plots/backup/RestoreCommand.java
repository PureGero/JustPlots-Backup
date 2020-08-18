package net.justminecraft.plots.backup;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.commands.SubCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class RestoreCommand extends SubCommand {
    private final JustPlotsBackup plugin;

    public RestoreCommand(JustPlotsBackup plugin) {
        super("/p restore", "Restore a backup of this plot", "restore");

        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command");
            return false;
        }

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run that command");
            return false;
        }

        Plot plot = JustPlots.getPlotAt((Player) sender);

        if (plot == null) {
            sender.sendMessage(ChatColor.RED + "You are not standing on a plot");
            return false;
        }

        if (!plot.isOwner((Player) sender) && !sender.hasPermission("justplots.restore.other")) {
            sender.sendMessage(ChatColor.RED + JustPlots.getUsername(plot.getOwner()) + " owns that plot");
            return false;
        }

        if (args.length < 1) {
            long since = sender.hasPermission("justplots.restore.other") ? 0 : plot.getCreation();

            sender.sendMessage(ChatColor.AQUA + "--------- " + ChatColor.WHITE + "Backups for plot " + plot + ChatColor.AQUA + " -------------------");

            plugin.getBackupManager().listBackups(plot, since).forEach(backup -> {
                ChatColor color = ChatColor.AQUA;

                if (plugin.getBackupManager().parseTime(backup) < plot.getCreation()) {
                    color = ChatColor.GRAY;
                }

                String cmd = "/p restore " + backup;
                sender.spigot().sendMessage(new ComponentBuilder(" Restore " + backup).color(color)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(cmd).create()))
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd)).create());
            });

            return false;
        }

        if (plugin.getBackupManager().restore(plot, args[0])) {
            sender.sendMessage(ChatColor.GREEN + "Succesfully restored plot " + plot + " to " + args[0]);
        } else {
            sender.sendMessage(ChatColor.RED + "Could not find backup " + args[0] + " for plot " + plot);
        }

        return true;
    }

    public void onTabComplete(CommandSender sender, String[] args, List<String> tabCompletion) {
        if (args.length == 1 && sender instanceof Player) {
            Plot plot = JustPlots.getPlotAt((Player) sender);

            if (plot == null || (!plot.isOwner((Player) sender) && !sender.hasPermission("justplots.restore.other"))) {
                return;
            }

            long since = sender.hasPermission("justplots.restore.other") ? 0 : plot.getCreation();

            plugin.getBackupManager().listBackups(plot, since).forEach(backup -> {
                if (backup.toLowerCase().startsWith(args[0].toLowerCase())) {
                    tabCompletion.add(backup);
                }
            });
        }
    }

    public String getPermission() {
        return "justplots.restore";
    }
}
