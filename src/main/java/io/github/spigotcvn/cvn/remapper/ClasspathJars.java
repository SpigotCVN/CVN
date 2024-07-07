package io.github.spigotcvn.cvn.remapper;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.gson.JarHashModel;
import io.github.spigotcvn.cvn.utils.FileUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Objects;

public class ClasspathJars {
    private final CVN plugin;
    private final Mappings mappings;
    public ClasspathJars(CVN plugin, Mappings mappings) {
        this.plugin = plugin;
        this.mappings = mappings;
    }

    public File remapClasspathJar() {
        File classpathJar = getServerJar();
        File remappedClasspathJar = new File(plugin.getCacheFolder(), "remapped-classpath.jar");

        if(!mappings.getMappingFiles().isFinalMappingFilePresent()) {
            mappings.getMappingFiles().generateFinalMappingFile();
        }

        mappings.remapJar(
                null,
                classpathJar, remappedClasspathJar,
                Mappings.Namespace.SPIGOT, Mappings.Namespace.INTERMEDIARY
        );

        File remappedClasspathJson = new File(plugin.getCacheFolder(), "remapped-classpath.json");
        JarHashModel jarHashModel = new JarHashModel(remappedClasspathJar, FileUtils.getHash(classpathJar));
        try (FileWriter writer = new FileWriter(remappedClasspathJson)) {
            CVN.GSON.toJson(jarHashModel, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return remappedClasspathJar;
    }

    public File getRemappedClasspathJar() {
        File remappedClasspathJar = new File(plugin.getCacheFolder(), "remapped-classpath.jar");
        if(remappedClasspathJar.exists()) {
            return remappedClasspathJar;
        }
        return null;
    }

    public boolean isClasspathJarRemapped() {
        if(didServerJarChange()) return false;

        File remappedClasspathJar = new File(plugin.getCacheFolder(), "remapped-classpath.jar");
        File remappedClasspathJson = new File(plugin.getCacheFolder(), "remapped-classpath.json");

        if(!remappedClasspathJar.exists() || !remappedClasspathJson.exists()) return false;
        String newHash = FileUtils.getHash(remappedClasspathJar);
        String oldHash;
        try (FileReader reader = new FileReader(remappedClasspathJson)) {
            JarHashModel jarHashModel = CVN.GSON.fromJson(reader, JarHashModel.class);
            oldHash = jarHashModel.getHash();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Objects.equals(newHash, oldHash);
    }

    public boolean didServerJarChange() {
        String newHash = FileUtils.getHash(getServerJar());
        String oldHash;
        try(FileReader reader = new FileReader(new File(plugin.getCacheFolder(), "server.json"))) {
            JarHashModel jarHashModel = CVN.GSON.fromJson(reader, JarHashModel.class);
            oldHash = jarHashModel.getHash();
        } catch (IOException e) {
            return true;
        }

        return !Objects.equals(newHash, oldHash);
    }

    public File getServerJar() {
        try {
            Method serverHandle = plugin.getServer().getClass().getDeclaredMethod("getServer");
            Object nmsServer = serverHandle.invoke(plugin.getServer());
            String server = FileUtils.formatToGoodJarFilePath(nmsServer.getClass());
            return new File(server);
        } catch (URISyntaxException | NoSuchMethodException |
                 IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
