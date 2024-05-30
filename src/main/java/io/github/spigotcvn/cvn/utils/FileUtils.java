package io.github.spigotcvn.cvn.utils;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.asm.AsmWriter;
import io.github.spigotcvn.cvn.asm.CustomRemapper;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    public static Set<File> listJarFilesNotRemapped(File directory) {
        if(!directory.isDirectory()) return null;

        File[] files = directory.listFiles();

        if(files == null) return null;

        return Stream.of(files)
                .filter(FileUtils::isJar)
                .filter(FileUtils::isNotRemapped)
                .collect(Collectors.toSet());
    }

    public static List<File> getClassFiles(File directory) {
        List<File> foundFiles = new ArrayList<>();

        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if(fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    foundFiles.add(file);
                } else if (file.isDirectory()) {
                    foundFiles.addAll(getClassFiles(file));
                }
            }
        }

        return foundFiles.stream()
                .filter(FileUtils::isClass)
                .collect(Collectors.toList());
    }

    public static boolean isJar(File file) {
        return file.isFile() && FilenameUtils.getExtension(file.getName()).equals("jar");
    }

    public static boolean isNotRemapped(File file) {
        return file.isFile() && !file.getName().contains("- remapped");
    }

    public static boolean isClass(File file) {
        return file.isFile() && FilenameUtils.getExtension(file.getName()).equals("class");
    }

    public static String formatToGoodJarFilePath(Class<?> clasz) throws URISyntaxException {
        // Get the location of the class file of the main class
        String path = clasz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

        // Convert the path to a file to get a normalized version
        File jarFile = new File(path);

        // Get the absolute path of the JAR file
        return jarFile.getAbsolutePath();
    }

    public static File jarToCVNJar(CVN plugin, File remappedPlugin) throws IOException {
        UUID pluginUuid = UUID.randomUUID();

        File cvnJarFolder = new File(plugin.getTempFolder() + "/cvnjar");

        Files.createDirectories(Paths.get(cvnJarFolder.getAbsolutePath()));

        // Edit from cvn-plugin.yml to plugin.yml
        ZipFile zipFile = new ZipFile(remappedPlugin);
        // Remove the base fake
        zipFile.removeFile("plugin.yml");

        // Create temp manager
        File tempFolder = new File(cvnJarFolder.getAbsolutePath() + "/cvn-" + pluginUuid);

        // Extract cvn-plugin.yml to temp folder
        zipFile.extractFile("cvn-plugin.yml", tempFolder.getAbsolutePath());

        // Get the extracted cvn-plugin.yml
        File tempPlugin = new File(tempFolder.getAbsolutePath() + "/cvn-plugin.yml");

        // Edit name to a "CVN implementation"
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(tempPlugin);
        yamlConfiguration.set("name", yamlConfiguration.get("name") + "-CVN");

        // Add CVN as dependency
        List<String> depends = yamlConfiguration.getStringList("depend");

        if(!depends.contains("CVN")) depends.add("CVN");
        yamlConfiguration.set("depend", depends);

        // Save temp cvn-plugin.yml
        yamlConfiguration.save(tempPlugin);

        // Add the cvn-plugin.yml to the final jar as plugin.yml
        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip("plugin.yml");

        zipFile.addFile(tempPlugin, parameters);

        // Delete temp folder
        tempFolder.getParentFile().delete();

        // Return the remapped jar
        return remappedPlugin;
    }

    public static Map<File, String> remapCraftBukkitImports(CVN plugin, File zipFile) throws IOException {
        plugin.getLogger().info("Mapping CraftBukkit imports...");

        ZipFile zip = new ZipFile(zipFile);

        // Get the extract folder
        File extractFolder = new File(plugin.getTempFolder() + "/asm-remap");

        // Create it if it doesn't exist
        Files.createDirectories(Paths.get(extractFolder.getAbsolutePath()));

        // Extract the zip file
        zip.extractAll(extractFolder.getAbsolutePath());

        Map<File, String> fileStringMap = new HashMap<>();

        // Get all the class files
        List<File> files = FileUtils.getClassFiles(extractFolder);

        for(File file : files) {
            CustomRemapper remapper = new CustomRemapper(plugin);
            fileStringMap.put(file, AsmWriter.writeAsm(file, remapper));
        }

        return fileStringMap;
    }
}
