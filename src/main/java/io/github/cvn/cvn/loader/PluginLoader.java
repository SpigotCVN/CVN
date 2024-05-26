package io.github.cvn.cvn.loader;

import io.github.cvn.cvn.CVN;
import io.github.cvn.cvn.remapper.Remapper;
import net.lingala.zip4j.ZipFile;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private final CVN cvn;
    private File plugin;

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
        // TODO : fix being trying editing remapped file when it's already existing, so error

        // Remap from intermediary to server obfuscate and put the file as remappedPlugin
        File remappedPlugin = new File(plugin.getAbsolutePath().replace(".jar", "-remapped.jar"));

        File classpathJar = new File(cvn.getServer().getClass().getProtectionDomain().getCodeSource().getLocation().getPath().replaceFirst("/", ""));

        remapper.remapJarFromIntermediary(
                classpathJar.toPath(),
                plugin,
                remappedPlugin
        );

        // Edit from cvn-plugin.yml to plugin.yml
        ZipFile zipFile = new ZipFile(remappedPlugin);
        zipFile.renameFile("cvn-plugin.yml", "plugin.yml");

        this.plugin = remappedPlugin;
    }

    public void loadPlugin() throws InvalidPluginException, InvalidDescriptionException {
        Plugin loadedPlugin = cvn.getServer().getPluginManager().loadPlugin(plugin);
        cvn.getServer().getPluginManager().enablePlugin(loadedPlugin);
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
