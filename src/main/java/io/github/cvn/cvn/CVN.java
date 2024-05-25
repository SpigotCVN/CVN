package io.github.cvn.cvn;

import io.github.cvn.cvn.loader.PluginLoader;
import io.github.cvn.cvn.remapper.Remapper;
import io.github.cvn.cvn.utils.FileUtils;
import io.github.cvn.mappingsdownloader.MappingsDownloader;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;

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

            // Final remapped file
            File remappedPlugin = FileUtils.insertInFileName(file, " - remapped");

            // Remap from intermediary to server obfuscate and put the file as remappedPlugin
            remapper.remapJarFromIntermediary(
                    Paths.get(getServer().getClass().getProtectionDomain().getCodeSource().getLocation().getPath()),
                    file,
                    remappedPlugin
            );

            // Should I really explain what is this?
            getLogger().info("Successfully remapped " + file.getName() + " !");
        }

        getLogger().info("Finished remapping plugins !");
    }

    public @Nullable File getMappingFile() {
        return mappingFile;
    }
}