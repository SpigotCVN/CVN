package io.github.spigotcvn.cvn;

import io.github.spigotcvn.cvn.loader.PluginLoader;
import io.github.spigotcvn.cvn.remapper.Mappings;
import io.github.spigotcvn.cvn.utils.CompatiblityUtils;
import io.github.spigotcvn.cvn.utils.FileUtils;
import io.github.spigotcvn.mappingsdownloader.MappingsDownloader;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.stianloader.picoresolve.version.MavenVersion;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

public final class CVN extends JavaPlugin {
    private String tempFolder;
    private String remappedJarsFolder;
    private String buildDataFolder;
    private String mappingsFolder;

    private @Nullable File mappingFile;
    private @Nullable File combinedMappingFile;
    private MavenVersion actualVersion;

    @Override
    public void onEnable() {
        getLogger().info("Enabled!");

        tempFolder = getDataFolder().getAbsolutePath() + "/temp";
        remappedJarsFolder = getDataFolder().getAbsolutePath() + "/rjf";
        buildDataFolder = getDataFolder().getAbsolutePath() + "/bdrf";
        mappingsFolder = getDataFolder().getAbsolutePath() + "\\mappings";

        saveDefaultConfig();

        actualVersion = MavenVersion.parse(CompatiblityUtils.getMinecraftVersion());

        MappingsDownloader mappingsDownloader = new MappingsDownloader(this, getMappingsFolder(), getActualVersion().getOriginText());
        mappingFile = mappingsDownloader.tryDownload();

        getLogger().info("Remapping plugins...");

        Mappings mappings = new Mappings(this);

        File pluginsFolder = new File(getDataFolder().getParent());

        mappings.doMappings();

        Set<File> files = FileUtils.listJarFilesNotRemapped(pluginsFolder);

        if(files == null) throw new RuntimeException("No jar found !");

        for(File file : files) {
            PluginLoader loader = new PluginLoader(this, file);

            // If the plugin isn't a CVN plugin, skip it
            if(loader.getPluginType() != PluginLoader.PluginType.CVN) continue;

            try {
                loader.remapPlugin(mappings);
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

    public MavenVersion getActualVersion() {
        return actualVersion;
    }

    public @Nullable File getMappingFile() {
        return mappingFile;
    }

    public void setCombinedMappingFile(@Nullable File combinedMappingFile) {
        this.combinedMappingFile = combinedMappingFile;
    }

    public @Nullable File getCombinedMappingFile() {
        return combinedMappingFile;
    }

    public String getTempFolder() {
        return tempFolder;
    }

    public String getRemappedJarsFolder() {
        return remappedJarsFolder;
    }

    public String getBuildDataFolder() {
        return buildDataFolder;
    }

    public String getMappingsFolder() {
        return mappingsFolder;
    }
}