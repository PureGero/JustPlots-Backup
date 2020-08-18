package net.justminecraft.plots.backup;

import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import net.justminecraft.plots.events.PlotClearEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class JustPlotsBackup extends JavaPlugin implements Listener {

    public static int MINUTES_BETWEEN_MANUAL_BACKUPS;
    private static boolean AUTOMATIC_BACKUP_ON_PLOT_CLEAR;
    private static int MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS;
    private static final int DELAY_BETWEEN_CLEARS = 2 * 60 * 1000;

    private BackupManager backupManager;

    @Override
    public void onEnable() {
        backupManager = new BackupManager(this);

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        MINUTES_BETWEEN_MANUAL_BACKUPS = getConfig().getInt("minutes-between-manual-backups", 10);
        AUTOMATIC_BACKUP_ON_PLOT_CLEAR = getConfig().getBoolean("automatic-backup-on-plot-clear", true);
        MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS = getConfig().getInt("maximum-plot-size-for-automatic-backups", 200);

        getServer().getPluginManager().registerEvents(this, this);

        JustPlots.getCommandExecuter().addCommand(new BackupCommand(this));
        JustPlots.getCommandExecuter().addCommand(new RestoreCommand(this));
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }

    private HashMap<Plot, Long> lastClears = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlotClearCheck(PlotClearEvent event) {
        if (AUTOMATIC_BACKUP_ON_PLOT_CLEAR
                && event.getPlot().getBottom().distanceSquared(event.getPlot().getTop()) > 2*MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS*MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS + 256*256
                && lastClears.getOrDefault(event.getPlot(), 0L) < System.currentTimeMillis() - DELAY_BETWEEN_CLEARS) {
            lastClears.put(event.getPlot(), System.currentTimeMillis());

            if (event.getPlayer() != null) {
                event.getPlayer().sendMessage(ChatColor.RED + "Caution: This plot is too big to be automatically backed up!");
                event.getPlayer().sendMessage(ChatColor.RED + "Please run the command again if you are sure about deleting this plot.");
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlotClear(PlotClearEvent event) {
        if (AUTOMATIC_BACKUP_ON_PLOT_CLEAR
                && event.getPlot().getBottom().distanceSquared(event.getPlot().getTop()) <= 2*MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS*MAXIMUM_PLOT_SIZE_FOR_AUTOMATIC_BACKUPS + 256*256
                && getBackupManager().backupIfNotDuplicate(event.getPlot())
                && event.getPlayer() != null) {
            event.getPlayer().sendMessage(ChatColor.GREEN + "Plot " + event.getPlot() + " has automatically been backed up.");
        }
    }
}
