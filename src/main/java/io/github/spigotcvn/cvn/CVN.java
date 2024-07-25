package io.github.spigotcvn.cvn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.spigotcvn.cvn.gson.FileSerializer;
import io.github.spigotcvn.cvn.loader.PluginLoader;
import io.github.spigotcvn.cvn.remapper.Mappings;
import io.github.spigotcvn.cvn.utils.CompatiblityUtils;
import io.github.spigotcvn.cvn.utils.FileUtils;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.stianloader.picoresolve.version.MavenVersion;

import java.io.File;
import java.io.IOException;

public final class CVN extends JavaPlugin {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(File.class, new FileSerializer())
            .setPrettyPrinting()
            .create();

    private File cacheFolder;
    private File remappedFolder;
    private MavenVersion actualVersion;
    private String cbLocation;

    @Override
    public void onEnable() {
        getLogger().info("Enabled!");

        actualVersion = MavenVersion.parse(CompatiblityUtils.getMinecraftVersion());
        cbLocation = CompatiblityUtils.getCBOldNotation(this);

        saveDefaultConfig();

        cacheFolder = new File(getDataFolder(), "cache");
        remappedFolder = new File(getDataFolder(), "remapped");
        if(!cacheFolder.exists()) cacheFolder.mkdirs();
        if(!remappedFolder.exists()) remappedFolder.mkdirs();

        getLogger().info("Remapping plugins...");

        Mappings mappings = new Mappings(this);
        File pluginsFolder = new File(getDataFolder().getParent());
        for(File plugin : pluginsFolder.listFiles()) {
            if(plugin.isDirectory()) continue;
            if(!FileUtils.isJar(plugin)) continue;

            PluginLoader pluginLoader = new PluginLoader(this, plugin);
            if(pluginLoader.getPluginType() != PluginLoader.PluginType.CVN) continue;

            if(pluginLoader.hasPluginChanged()) {
                getLogger().info("Remapping " + plugin.getName() + "...");
                try {
                    pluginLoader.remapPlugin(mappings);
                } catch (IOException e) {
                    getLogger().severe("Could not remap plugin " + plugin.getName() + "!");
                    getLogger().severe("This plugin will be skipped");
                    getLogger().severe("THIS IS LIKELY A CVN ISSUE!");
                    e.printStackTrace();
                }
            } else {
                getLogger().info("Plugin " + plugin.getName() + " has not changed, loading remapped plugin...");
                pluginLoader.loadRemappedPlugin();
            }

            try {
                getLogger().info("Loading plugin " + plugin.getName() + "...");
                pluginLoader.loadPluginToSpigot();
            } catch (InvalidDescriptionException | InvalidPluginException e) {
                getLogger().severe("Could not load plugin " + plugin.getName() + "!");
                getLogger().severe("This plugin will be skipped.");
                getLogger().severe("THIS IS LIKELY NOT A CVN ISSUE!");
                e.printStackTrace();
            }
        }

        getLogger().info("Finished remapping plugins!");
    }

    public MavenVersion getActualVersion() {
        return actualVersion;
    }

    public String getCbLocation() {
        return cbLocation;
    }

    public File getCacheFolder() {
        return cacheFolder;
    }

    public File getRemappedFolder() {
        return remappedFolder;
    }
}