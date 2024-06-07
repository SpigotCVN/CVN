package io.github.spigotcvn.cvn.remapper;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.smdownloader.SpigotMappingsDownloader;
import io.github.spigotcvn.smdownloader.mappings.MappingFile;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.*;
import java.nio.file.Path;

public class Mappings {
    private final CVN plugin;

    public Mappings(CVN plugin) {
        this.plugin = plugin;
    }

    /**
     * Takes in an intermediary mapped jar and remaps it to an official mapped jar (an obfuscated one).
     * @param jarFile       The jar file to remap
     * @param resultJarFile The file to save the remapped jar to
     */
    public void remapJarFromIntermediary(Path classpath, File jarFile, File resultJarFile, File mappingFile, Namespace from, Namespace to) {
        plugin.getLogger().info("Remapping jar to obfuscated mappings...");

        if(mappingFile == null) throw new IllegalStateException("Could not find mapping file !");

        plugin.getLogger().info("Loaded mappings from: " + mappingFile.getAbsolutePath());

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(
                        TinyUtils.createTinyMappingProvider(
                                mappingFile.toPath(),
                                from.getNamespaceName(),
                                to.getNamespaceName()
                        )
                )
                .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(resultJarFile.toPath()).build()) {
            outputConsumer.addNonClassFiles(jarFile.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            plugin.getLogger().info("Reading inputs from " + jarFile.getAbsolutePath() + "...");
            remapper.readInputs(jarFile.toPath());
            plugin.getLogger().info("Reading classpath from " + classpath.toAbsolutePath() + "...");
            remapper.readClassPath(classpath);

            plugin.getLogger().info("Remapping jar...");
            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
            plugin.getLogger().info("Finished remapping jar from intermediary mappings to: " + resultJarFile.getAbsolutePath());
        }
    }

    public void doMappings() {
        /*
        OK basically, in your plugins data folder you will have two folders, one for remapped jars (rjf for short) and one for the builddata repo (bdrf for short)
        OK Basically, you download spigot mappings, download mojmaps, then combine them and then you use the combined mapping for everything else
        (iirc the combined mapping generate method should take care of all of everything that hasn't been done before)

        If the version changed, run the generate mappings method with argument true, that will ensure the mappings are redownloaded and regenerated
        The method will give you a MappingFile, hold onto it until you have downloaded intermediary mappings. Then use the mappings merger (please without renaming the main class and just use the provided api) and combine the tiny and spigot mappings, as the namespace set the spigot
        Now you have the tiny mappings, remap the jars using that from intermediary to spigot and store the remapped jars in the rjf. With them also store one text file that has an identical name, just with .txt added to the back (don't even need to remove .jar) which will simply hold the hash of the original (unremapped) file. Every reboot it will go through all jars and if it's a cvn plugin, it should check whether its hash exists and whether it has changed, if any of the conditions don't meet it will remap the jar and regenerate the hash.
        Then you just call load on the remapped jars and that's it
         */

        String version = plugin.getActualVersion().getOriginText();

        SpigotMappingsDownloader mappinger = new SpigotMappingsDownloader(new File(plugin.getBuildDataFolder()), version);

        // Spigot mappings download
        plugin.getLogger().info("Downloading Spigot mappings for version " + version);

        if(!mappinger.isVersionValid()) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }

        mappinger.downloadMappings(false);

        // Mojmaps download
        plugin.getLogger().info("Downloading Mojang mappings for version " + version);

        if(!mappinger.hasMojangMappings()) {
            throw new IllegalArgumentException("Mojang mappings are not available for version: " + version);
        }

        mappinger.downloadMojangMappings(false);

        // Generate combined
        plugin.getLogger().info("Generating combined mappings for version " + version);
        MappingFile combined = mappinger.generateCombinedMappings(false);

        plugin.setCombinedMappingFile(combined.getFile());
    }

    public enum Namespace {
        INTERMEDIARY("intermediary"),
        OBFUSCATED("official"),
        SPIGOT("spigot");

        final String namespaceName;

        Namespace(String name) {
            this.namespaceName = name;
        }

        public String getNamespaceName() {
            return namespaceName;
        }
    }
}