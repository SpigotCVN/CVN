package io.github.spigotcvn.cvn.remapper;

import io.github.spigotcvn.cvn.CVN;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.*;
import java.nio.file.Path;

public class Mappings {
    private final CVN plugin;
    private final ClasspathJars classpath;
    private final MappingFiles mappingFiles;

    public Mappings(CVN plugin) {
        this.plugin = plugin;
        this.classpath = new ClasspathJars(plugin, this);
        this.mappingFiles = new MappingFiles(plugin, this);
    }

    /**
     * Takes in a jar in mapping <b>from</b> and remaps it to <b>to</b>
     * @param classpath     The classpath to remap the jar with
     * @param jarFile       The jar file to remap
     * @param resultJarFile The file to save the remapped jar to
     * @param from          The namespace to remap from
     * @param to            The namespace to remap to
     */
    public void remapJar(Path classpath, File jarFile, File resultJarFile, Namespace from, Namespace to) {
        if(!mappingFiles.isFinalMappingFilePresent()) {
            mappingFiles.generateFinalMappingFile();
        }
        File mappingFile = mappingFiles.getFinalMappingFile();

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(
                        TinyUtils.createTinyMappingProvider(
                                mappingFile.toPath(),
                                from.getNamespaceName(),
                                to.getNamespaceName()
                        )
                )
                .ignoreConflicts(true)
                .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(resultJarFile.toPath().toAbsolutePath()).build()) {
            outputConsumer.addNonClassFiles(jarFile.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            remapper.readInputs(jarFile.toPath());
            if(classpath != null) {
                remapper.readClassPath(classpath);
            }

            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            remapper.finish();
        }
    }

    public ClasspathJars getClasspathJars() {
        return classpath;
    }

    public MappingFiles getMappingFiles() {
        return mappingFiles;
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