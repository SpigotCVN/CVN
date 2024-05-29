package io.github.spigotcvn.cvn;

import io.github.spigotcvn.cvn.loader.PluginLoader;
import io.github.spigotcvn.cvn.remapper.Remapper;
import io.github.spigotcvn.cvn.utils.FileUtils;
import io.github.spigotcvn.mappingsdownloader.MappingsDownloader;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public final class CVN extends JavaPlugin {
    private String tempFolder;
    private @Nullable File mappingFile;

    @Override
    public void onEnable() {
        getLogger().info("Enabled!");

        saveDefaultConfig();

        tempFolder = getDataFolder().getAbsolutePath() + "/temp";

        MappingsDownloader mappingsDownloader = new MappingsDownloader(this, getConfig());
        mappingFile = mappingsDownloader.tryDownload();

        getLogger().info("Remapping plugins...");

        Remapper remapper = new Remapper(this);

        File pluginsFolder = new File(getDataFolder().getParent());

        for(File file : FileUtils.listJarFilesNotRemapped(pluginsFolder)) {
            PluginLoader loader = new PluginLoader(this, file);

            // If the plugin isn't a CVN plugin, skip it
            if(loader.getPluginType() != PluginLoader.PluginType.CVN) continue;

            try {
                loader.remapPlugin(remapper);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            // Should I really explain what is this?
            // Hello rad also
            getLogger().info("Successfully remapped " + file.getName() + " !");

            try {
                loader.loadPlugin();
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                throw new RuntimeException(e);
            }
        }

        getLogger().info("Finished remapping plugins !");
    }

    public @Nullable File getMappingFile() {
        return mappingFile;
    }

    public String getTempFolder() {
        return tempFolder;
    }
}