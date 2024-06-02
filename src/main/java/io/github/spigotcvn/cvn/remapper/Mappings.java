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
    public void remapJarFromIntermediary(Path classpath, File jarFile, File resultJarFile, Namespace from, Namespace to) {
        plugin.getLogger().info("Remapping jar to obfuscated mappings...");

        File mappingFile = plugin.getMappingFile();

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

    public void downloadSpigotMappings() {
        SpigotMappingsDownloader mappinger = new SpigotMappingsDownloader(new File(dir), version);
        plugin.getLogger().info("Downloading Spigot mappings for version " + version);

        if(!mappinger.isVersionValid()) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }

        for (MappingFile file : mappinger.downloadMappings(false)) {
            if (!file.getFile().exists()) continue;
            System.out.println(file.getFileType());
            System.out.println(file.getType());
            System.out.println(file.getFile().getName());
            System.out.println();
        }
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