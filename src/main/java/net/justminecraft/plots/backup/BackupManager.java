package net.justminecraft.plots.backup;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.justminecraft.plots.Plot;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackupManager {

    private JustPlotsBackup plugin;
    private File backupsDir;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public BackupManager(@NotNull JustPlotsBackup plugin) {
        this.plugin = plugin;
        this.backupsDir = new File(plugin.getDataFolder(), "backups");

        if (!backupsDir.isDirectory() && !backupsDir.mkdirs()) {
            plugin.getLogger().severe("Could not create directory " + backupsDir.getPath());
        }
    }

    public void backup(@NotNull Plot plot) {
        BlockArrayClipboard clipboard = copy(plot);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> save(plot, clipboard));
    }

    @NotNull
    private BlockArrayClipboard copy(@NotNull Plot plot) {
        World world = Bukkit.getWorld(plot.getWorldName());

        if (world == null) {
            throw new IllegalStateException("World " + plot.getWorldName() + " is not loaded");
        }

        EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(BukkitAdapter.adapt(world), 0);
        Region region = new CuboidRegion(BukkitAdapter.asBlockVector(plot.getBottom()), BukkitAdapter.asBlockVector(plot.getTop()));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BukkitAdapter.asBlockVector(plot.getBottom()));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setCopyingEntities(true);
        copy.setCopyingBiomes(true);

        try {
            Operations.completeLegacy(copy);
        } catch (MaxChangedBlocksException e) {
            throw new RuntimeException(e);
        }

        return clipboard;
    }

    private void save(@NotNull Plot plot, @NotNull BlockArrayClipboard clipboard) {
        ClipboardFormat format = ClipboardFormats.findByAlias("schem");

        if (format == null) {
            throw new IllegalStateException("Format 'schem' not found");
        }

        try (ClipboardWriter writer = format.getWriter(new BufferedOutputStream(new FileOutputStream(getNewBackupFile(plot))))) {
            writer.write(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getNewBackupFile(Plot plot) {
        return new File(backupsDir, getFilePrefix(plot) + dateFormat.format(new Date()) + ".schem");
    }

    @NotNull
    private String getFilePrefix(@NotNull Plot plot) {
        return plot.getWorldName() + "-" + plot.getId().getX() + "-" + plot.getId().getZ() + "_";
    }
}
