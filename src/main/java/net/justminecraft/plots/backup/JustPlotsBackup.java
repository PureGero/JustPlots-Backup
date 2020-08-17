package net.justminecraft.plots.backup;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class JustPlotsBackup extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }
}
