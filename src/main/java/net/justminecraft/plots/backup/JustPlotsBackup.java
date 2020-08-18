package net.justminecraft.plots.backup;

import net.justminecraft.plots.JustPlots;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class JustPlotsBackup extends JavaPlugin implements Listener {

    public static int MINUTES_BETWEEN_MANUAL_BACKUPS;

    private BackupManager backupManager;

    @Override
    public void onEnable() {
        backupManager = new BackupManager(this);

        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        MINUTES_BETWEEN_MANUAL_BACKUPS = getConfig().getInt("minutes-between-manual-backups");

        getServer().getPluginManager().registerEvents(this, this);

        JustPlots.getCommandExecuter().addCommand(new BackupCommand(this));
        JustPlots.getCommandExecuter().addCommand(new RestoreCommand(this));
    }

    public BackupManager getBackupManager() {
        return backupManager;
    }
}
