package io.github.cvn.cvn;

import io.github.cvn.cvn.loader.PluginLoader;
import io.github.cvn.cvn.remapper.Remapper;
import io.github.cvn.cvn.utils.FileUtils;
import io.github.cvn.mappingsdownloader.MappingsDownloader;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class CVN extends JavaPlugin {
    private @Nullable File mappingFile;

    @Override
    public void onEnable() {
        getLogger().info("Enabled!");
        saveDefaultConfig();

        MappingsDownloader mappingsDownloader = new MappingsDownloader(this, getConfig());
        mappingFile = mappingsDownloader.tryDownload();

        getLogger().info("Remapping plugins...");

        Remapper remapper = new Remapper(this);

        File pluginsFolder = new File(getDataFolder().getParent());

        for(File file : FileUtils.listFiles(pluginsFolder)) {
            PluginLoader loader = new PluginLoader(this, file);

            // If the plugin isn't a CVN plugin, skip it
            if(loader.getPluginType() != PluginLoader.PluginType.CVN) continue;

            try {
                loader.remapPlugin(remapper);
            } catch (IOException e) {
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
}