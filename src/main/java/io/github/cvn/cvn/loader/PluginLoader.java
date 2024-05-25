package io.github.cvn.cvn.loader;

import io.github.cvn.cvn.CVN;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
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
            cvn.getLogger().severe("Cannot read plugin type for " + plugin.getName() + " !");
        }

        // There is not a plugin.yml, neither a cvn-plugin.yml
        return PluginType.NONE;
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
