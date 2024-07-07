package io.github.spigotcvn.cvn.loader;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.gson.JarHashModel;
import io.github.spigotcvn.cvn.remapper.Mappings;
import io.github.spigotcvn.cvn.utils.FileUtils;
import io.github.spigotcvn.cvn.utils.JarUtil;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.util.Objects;
import java.util.UUID;
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
            throw new RuntimeException("Unable to get type for plugin " + plugin.getName(), ex);
        }

        // There is no plugin.yml, neither a cvn-plugin.yml
        return PluginType.NONE;
    }

    public boolean hasPluginChanged() {
        File remappedPluginHash = new File(cvn.getRemappedFolder(), plugin.getName() + ".json");
        if(!remappedPluginHash.exists()) return true;

        JarHashModel jarHashModel;
        try {
            jarHashModel = CVN.GSON.fromJson(new FileReader(remappedPluginHash), JarHashModel.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if(jarHashModel == null) return true;
        return !Objects.equals(jarHashModel.getHash(), FileUtils.getHash(plugin));
    }

    public void remapPlugin(Mappings mappings) throws IOException {
        // Before remapping the jar, you download the intermediary mappings,
        // generate combined spigot mappings and after that you run the merger,
        // save the new mapping file and use it to remap from intermediary to spigot instead of to official

        // Remap from intermediary to server obfuscate and put the file as remappedPlugin
        this.remappedPlugin = new File(cvn.getRemappedFolder(), plugin.getName());
        File oldPluginHash = new File(cvn.getRemappedFolder(), plugin.getName() + ".json");

        if(!mappings.getClasspathJars().isClasspathJarRemapped()) {
            mappings.getClasspathJars().remapClasspathJar();
        }
        File classpathJar = mappings.getClasspathJars().getRemappedClasspathJar();

        File asmDirName = new File(cvn.getCacheFolder(), "asm-remap-" + UUID.randomUUID());

        mappings.remapJar(
                classpathJar.toPath(),
                plugin,
                remappedPlugin,
                Mappings.Namespace.INTERMEDIARY,
                Mappings.Namespace.SPIGOT
        );

        FileUtils.remapCraftBukkitImports(cvn, remappedPlugin, asmDirName);
        FileUtils.unCVNifyPlugin(asmDirName);

        try {
            JarUtil.repackJar(remappedPlugin, asmDirName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        JarHashModel jarHashModel = new JarHashModel(remappedPlugin, FileUtils.getHash(plugin));
        try(FileWriter writer = new FileWriter(oldPluginHash)) {
            String json = CVN.GSON.toJson(jarHashModel);
            writer.write(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        FileUtils.iterateOverFiles(
                File::delete,
                asmDirName
        );
        asmDirName.delete();
    }

    public void loadRemappedPlugin() {
        this.remappedPlugin = new File(cvn.getRemappedFolder(), plugin.getName());
        if(!remappedPlugin.exists()) throw new IllegalStateException("Plugin hasn't been remapped yet!");
    }

    public void loadPluginToSpigot() throws InvalidPluginException, InvalidDescriptionException {
        if(remappedPlugin == null) throw new InvalidPluginException("You can't load the plugin if it wasn't remapped!");

        Plugin loadedPlugin = cvn.getServer().getPluginManager().loadPlugin(remappedPlugin);
        if(loadedPlugin == null) throw new InvalidPluginException("The remapped plugin can't be loaded!");

//        cvn.getServer().getPluginManager().enablePlugin(loadedPlugin);
    }

    /**
     * Get the plugin involved in this loader
     * @return the plugin file (.jar)
     */
    public File getPlugin() {
        return plugin;
    }

    /**
     * Get the remapped plugin created by {@link #remapPlugin(Mappings)}
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
