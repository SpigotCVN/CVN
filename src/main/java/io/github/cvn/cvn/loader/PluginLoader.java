package io.github.cvn.cvn.loader;

import io.github.cvn.cvn.CVN;
import io.github.cvn.cvn.remapper.Remapper;
import io.github.cvn.cvn.utils.FileUtils;
import net.fabricmc.tinyremapper.extension.mixin.common.data.Pair;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private final CVN cvn;
    private final File plugin;

    public PluginLoader(CVN cvn, File plugin) {
        this.cvn = cvn;
        this.plugin = plugin;
    }

    public PluginType getPluginType() {

        try (JarFile jar = new JarFile(plugin)) {
            JarEntry cvnPluginYaml = jar.getJarEntry("cvn-plugin.yml");
            JarEntry pluginYaml = jar.getJarEntry("plugin.yml");

            // Is a basic plugin
            if (cvnPluginYaml == null && pluginYaml != null) {
                return PluginType.SPIGOT;
            }
            // Is a CVN plugin
            else if (cvnPluginYaml != null && pluginYaml == null) {
                return PluginType.CVN;
            }
        } catch (IOException | YAMLException ex) {
            cvn.getLogger().severe("Cannot read type for " + plugin.getName() + " !");
        }

        // There is not a plugin.yml, neither a cvn-plugin.yml
        return PluginType.NONE;
    }

    public void remapPlugin(Remapper remapper) throws IOException {
        // Remap from intermediary to server obfuscate and put the file as remappedPlugin
        File remappedPlugin = FileUtils.insertInFileName(plugin, " - remapped");

        remapper.remapJarFromIntermediary(
                Paths.get(cvn.getServer().getClass().getProtectionDomain().getCodeSource().getLocation().getPath()),
                plugin,
                remappedPlugin
        );

        // Edit from cvn-plugin.yml to plugin.yml
        try (JarFile jar = new JarFile(plugin)) {
            InputStream stream = jar.getInputStream(jar.getJarEntry("cvn-plugin.yml"));

            ZipFile zipFile = new ZipFile(plugin);
            zipFile.removeFile("cvn-plugin.yml");

            ZipParameters parameters = new ZipParameters();
            parameters.setFileNameInZip("plugin.yml");

            zipFile.addStream(stream, parameters);
        } catch (IOException | YAMLException ex) {
            cvn.getLogger().severe("Cannot manipulate jar for " + plugin.getName() + " !");
        }
    }

    public void loadPlugin() throws InvalidPluginException, InvalidDescriptionException {
        cvn.getServer().getPluginManager().loadPlugin(plugin);
    }

    /**
     * Get the plugin involved in this loader
     * @return the plugin file (.jar)
     */
    public File getPlugin() {
        return plugin;
    }

    /**
     * The possible types of .jar<br>
     * {@link #CVN} A CVN plugin<br>
     * {@link #SPIGOT} A Spigot plugin<br>
     * {@link #NONE} A basic .jar, nothing more
     */
    public enum PluginType {
        CVN,
        SPIGOT,
        NONE;
    }
}
