package io.github.spigotcvn.cvn.utils;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.asm.AsmWriter;
import io.github.spigotcvn.cvn.asm.CustomRemapper;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileUtils {
    /**
     * List all jar files that are not remapped in a directory
     * @param directory The directory to search in
     * @return A set of jar files that are not remapped
     */
    public static Set<File> listJarFiles(File directory) {
        if(!directory.isDirectory()) return new HashSet<>();

        File[] files = directory.listFiles();

        if(files == null) return new HashSet<>();

        return Stream.of(files)
                .filter(FileUtils::isJar)
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

    public static Map<File, String> remapCraftBukkitImports(CVN plugin, File zipFile, File asmDir) throws IOException {
        Map<File, String> fileStringMap = new HashMap<>();

        try(ZipFile zip = new JarFile(zipFile)) {
            // Create it if it doesn't exist
            Files.createDirectories(Paths.get(asmDir.getAbsolutePath()));

            // Extract the zip file
            FileUtils.extractAll(zip, asmDir);

            // Get all the class files
            List<File> files = FileUtils.getClassFiles(asmDir);

            for (File file : files) {
                CustomRemapper remapper = new CustomRemapper(plugin);
                fileStringMap.put(file, AsmWriter.writeAsm(file, remapper));
            }
        }

        return fileStringMap;
    }

    public static List<File> extractAll(ZipFile zip, File directory) {
        List<File> extractedFiles = new ArrayList<>();
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File entryDestination = new File(directory, entry.getName());
            if(entryDestination.getParentFile() != null) entryDestination.getParentFile().mkdirs();
            if (entry.isDirectory()) {
                entryDestination.mkdirs();
            } else {
                try (InputStream in = zip.getInputStream(entry);
                     OutputStream out = new BufferedOutputStream(Files.newOutputStream(entryDestination.toPath(), StandardOpenOption.CREATE))) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                extractedFiles.add(entryDestination);
            }
        }
        return extractedFiles;
    }

    public static void unCVNifyPlugin(File unpackedDir) {
        File pluginYml = new File(unpackedDir, "plugin.yml");
        if(pluginYml.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(pluginYml);
            File mainClassPath = new File(config.getString("main").replace(".", File.separator) + ".class");
            if(mainClassPath.exists()) {
                mainClassPath.delete();
            }
            pluginYml.delete();
        }
        File cvnPluginYml = new File(unpackedDir, "cvn-plugin.yml");
        if(!cvnPluginYml.exists()) throw new IllegalStateException("The plugin is not a CVN plugin!");

        cvnPluginYml.renameTo(pluginYml);
    }

    public static String getHash(File file) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            try (InputStream is = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        MessageDigest md;
        try {
             md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        byte[] hash = md.digest(os.toByteArray());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void iterateOverFiles(Consumer<File> consumer, File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                iterateOverFiles(consumer, file);
            } else {
                consumer.accept(file);
            }
        }
    }

    /**
     * Get the java plugin description in plugin.yml from a File instance.<br>
     * Picked up from {@link JavaPluginLoader#getPluginDescription(File)}
     */
    public static PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        Validate.notNull(file, "File cannot be null");

        JarFile jar = null;
        InputStream stream = null;

        try {
            jar = new JarFile(file);
            JarEntry entry = jar.getJarEntry("plugin.yml");

            if (entry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("Jar does not contain plugin.yml"));
            }

            stream = jar.getInputStream(entry);

            return new PluginDescriptionFile(stream);

        } catch (IOException | YAMLException ex) {
            throw new InvalidDescriptionException(ex);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException ignored) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
