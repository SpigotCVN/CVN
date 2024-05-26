package io.github.cvn.cvn.remapper;

import io.github.cvn.cvn.CVN;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.*;
import java.nio.file.Path;

public class Remapper {
    private final CVN plugin;

    public Remapper(CVN plugin) {
        this.plugin = plugin;
    }

    /**
     * Takes in an intermediary mapped jar and remaps it to an official mapped jar (an obfuscated one).
     * @param jarFile       The jar file to remap
     * @param resultJarFile The file to save the remapped jar to
     */
    public void remapJarFromIntermediary(Path classpath, File jarFile, File resultJarFile) {
        File mappingFile = plugin.getMappingFile();

        if(mappingFile == null) throw new IllegalStateException("Could not find mapping file !");

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(
                        TinyUtils.createTinyMappingProvider(
                                mappingFile.toPath(),
                                Namespace.INTERMEDIARY.getNamespaceName(),
                                Namespace.OBFUSCATED.getNamespaceName()
                        )
                ).build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(resultJarFile.toPath()).build()) {
            outputConsumer.addNonClassFiles(jarFile.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            remapper.readInputs(jarFile.toPath());
            remapper.readClassPath(classpath);

            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
        }
    }

    /**
     * Takes in an officially mapped jar (an obfuscated one) and remaps it to the intermediary mappings.
     * @param jarFile       The jar file to remap
     * @param resultJarFile The file to save the remapped jar to
     */
    public void remapJarFromIntermediary2(Path classpath, File jarFile, File resultJarFile) {
        System.out.println("Remapping jar to obfuscated mappings...");

        File mappingFile = plugin.getMappingFile();

        if(mappingFile == null) throw new IllegalStateException("Could not find mapping file !");

        System.out.println("Loaded mappings from: " + mappingFile.getAbsolutePath());

        TinyRemapper remapper = TinyRemapper.newRemapper()
                .withMappings(
                        TinyUtils.createTinyMappingProvider(
                                mappingFile.toPath(),
                                Namespace.INTERMEDIARY.getNamespaceName(),
                                Namespace.OBFUSCATED.getNamespaceName()
                        )
                )
                .build();

        try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(resultJarFile.toPath()).build()) {
            outputConsumer.addNonClassFiles(jarFile.toPath(), NonClassCopyMode.FIX_META_INF, remapper);

            System.out.println("Reading inputs from " + jarFile.getAbsolutePath() + "...");
            remapper.readInputs(jarFile.toPath());
            System.out.println("Reading classpath from " + classpath.toAbsolutePath() + "...");
            remapper.readClassPath(classpath);

            System.out.println("Remapping jar...");
            remapper.apply(outputConsumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            remapper.finish();
            System.out.println("Finished remapping jar to intermediary mappings to: " + resultJarFile.getAbsolutePath());
        }
    }

    private enum Namespace {
        INTERMEDIARY("intermediary"),
        OBFUSCATED("official");

        final String namespaceName;

        Namespace(String name) {
            this.namespaceName = name;
        }

        public String getNamespaceName() {
            return namespaceName;
        }
    }
}