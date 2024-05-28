package io.github.spigotcvn.cvn.loader;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.remapper.Remapper;
import io.github.spigotcvn.cvn.utils.FileUtils;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginLoader {
    private final CVN cvn;
    private final File plugin;
    private @Nullable File remappedPlugin;

    public PluginLoader(CVN cvn, File plugin) {
        this.cvn = cvn;
        this.plugin = plugin;
    }

    public PluginType getPluginType() {

        try (JarFile jar = new JarFile(plugin)) {
            JarEntry cvnPluginYaml = jar.getJarEntry("cvn-plugin.yml");
            JarEntry pluginYaml = jar.getJarEntry("plugin.yml");

            // Is a good CVN plugin
            if(cvnPluginYaml != null && pluginYaml != null) {
                return PluginType.CVN;
            }
            // Is a basic plugin
            else if (cvnPluginYaml == null && pluginYaml != null) {
                return PluginType.SPIGOT;
            }
            // Is a CVN plugin
            else if (cvnPluginYaml != null) {
                return PluginType.CVN;
            }
        } catch (IOException | YAMLException ex) {
            cvn.getLogger().severe("Cannot read type for " + plugin.getName() + " !");
        }

        // There is not a plugin.yml, neither a cvn-plugin.yml
        return PluginType.NONE;
    }

    public void remapPlugin(Remapper remapper) throws IOException, URISyntaxException {
        // TODO : fix being trying editing remapped file when it's already existing, so error

        // Remap from intermediary to server obfuscate and put the file as remappedPlugin
        File remappedPlugin = new File(plugin.getAbsolutePath().replace(".jar", "-remapped.jar"));

        File classpathJar = new File(FileUtils.formatToGoodJarFilePath(cvn.getServer().getClass()));

        remapper.remapJarFromIntermediary(
                classpathJar.toPath(),
                plugin,
                remappedPlugin
        );

        this.remappedPlugin = FileUtils.jarToCVNJar(cvn, remappedPlugin);
    }

    public void loadPlugin() throws InvalidPluginException, InvalidDescriptionException {
        if(remappedPlugin == null) throw new InvalidPluginException("You can't load the plugin if it wasn't remapped!");

        Plugin loadedPlugin = cvn.getServer().getPluginManager().loadPlugin(remappedPlugin);
        if(loadedPlugin == null) throw new InvalidPluginException("The remapped plugin can't be loaded!");

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
     * Get the remapped plugin created by {@link #remapPlugin(Remapper)}
     * @return The remapped plugin file, or null if not remapped yet
     */
    public @Nullable File getRemappedPlugin() {
        return remappedPlugin;
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
