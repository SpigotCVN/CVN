package io.github.spigotcvn.cvn.utils;

import io.github.spigotcvn.cvn.CVN;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    public static Set<File> listFiles(File directory) {
        if(!directory.isDirectory()) return null;

        File[] files = directory.listFiles();

        if(files == null) return null;

        return Stream.of(files)
                .filter(FileUtils::isJar)
                .collect(Collectors.toSet());
    }

    public static boolean isJar(File file) {
        return file.isFile() && FilenameUtils.getExtension(file.getName()).equals("jar");
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

        String folderPath = plugin.getDataFolder().getAbsolutePath() + "/temp";
        Files.createDirectories(Paths.get(folderPath));

        // Edit from cvn-plugin.yml to plugin.yml
        ZipFile zipFile = new ZipFile(remappedPlugin);
        // Remove the base fake
        zipFile.removeFile("plugin.yml");
        //zipFile.renameFile("cvn-plugin.yml", "plugin.yml");


        File tempFolder = new File(folderPath + "/cvn-" + pluginUuid);

        zipFile.extractFile("cvn-plugin.yml", tempFolder.getAbsolutePath());

        File tempPlugin = new File(tempFolder.getAbsolutePath() + "/cvn-plugin.yml");

        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(tempPlugin);

        yamlConfiguration.set("name", "CVN-" + pluginUuid);

        List<String> depends = yamlConfiguration.getStringList("depend");

        if(!depends.contains("CVN")) depends.add("CVN");

        yamlConfiguration.set("depend", depends);

        yamlConfiguration.save(tempPlugin);

        ZipParameters parameters = new ZipParameters();
        parameters.setFileNameInZip("plugin.yml");

        zipFile.addFile(tempPlugin, parameters);

        return remappedPlugin;
    }
}
