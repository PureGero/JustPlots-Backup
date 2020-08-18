package net.justminecraft.plots.backup;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import net.justminecraft.plots.JustPlots;
import net.justminecraft.plots.Plot;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.text.ParseException;
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

        EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));
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

    @NotNull
    private File save(@NotNull Plot plot, @NotNull BlockArrayClipboard clipboard) {
        ClipboardFormat format = ClipboardFormats.findByAlias("schem");

        if (format == null) {
            throw new IllegalStateException("Format 'schem' not found");
        }

        File file = getNewBackupFile(plot);

        try (ClipboardWriter writer = format.getWriter(new BufferedOutputStream(new FileOutputStream(file)))) {
            writer.write(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    private void backupIfNotDuplicate(@NotNull Plot plot) {
        List<String> backups = listBackups(plot, 0);

        File newBackup = save(plot, copy(plot));

        try {
            long length = newBackup.length();
            byte[] bytes = Files.readAllBytes(newBackup.toPath());

            for (String backup : backups) {
                File file = new File(backupsDir, getFilePrefix(plot) + backup + ".schem");

                if (file.length() == length && byteArrayEqual(bytes, Files.readAllBytes(file.toPath()))) {
                    // Duplicate backup, delete it
                    if (newBackup.delete()) {
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean byteArrayEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }

    public boolean restore(@NotNull Plot plot, @NotNull String backup) {
        File file = new File(backupsDir, getFilePrefix(plot) + backup + ".schem");

        if (!file.isFile()) {
            return false;
        }

        ClipboardFormat format = ClipboardFormats.findByAlias("schem");

        if (format == null) {
            throw new IllegalStateException("Format 'schem' not found");
        }

        try (ClipboardReader reader = format.getReader(new BufferedInputStream(new FileInputStream(file)))) {
            Clipboard clipboard = reader.read();

            World world = Bukkit.getWorld(plot.getWorldName());

            if (world == null) {
                throw new IllegalStateException("World " + plot.getWorldName() + " is not loaded");
            }

            backupIfNotDuplicate(plot);

            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Player) && JustPlots.getPlotAt(entity) == plot) {
                    entity.remove();
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));
                BlockVector3 to = clipboard.getOrigin();
                Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(to).ignoreAirBlocks(false).copyBiomes(true).copyEntities(true).build();

                try {
                    Operations.completeLegacy(operation);
                    Operations.completeLegacy(editSession.commit());
                } catch (MaxChangedBlocksException e) {
                    e.printStackTrace();
                }
            });

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private File getNewBackupFile(Plot plot) {
        return new File(backupsDir, getFilePrefix(plot) + dateFormat.format(new Date()) + ".schem");
    }

    @NotNull
    public List<String> listBackups(@NotNull Plot plot, long since) {
        List<String> backups = new ArrayList<>();

        String prefix = getFilePrefix(plot);

        File[] files = backupsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(prefix)) {
                    String name = file.getName().substring(prefix.length(), file.getName().indexOf(".schem"));
                    if (parseTime(name) >= since) {
                        backups.add(name);
                    }
                }
            }
        }

        return backups;
    }

    @NotNull
    private String getFilePrefix(@NotNull Plot plot) {
        return plot.getWorldName() + "-" + plot.getId().getX() + "-" + plot.getId().getZ() + "_";
    }

    public long parseTime(String backupName) {
        try {
            return dateFormat.parse(backupName).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
