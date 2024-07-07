package io.github.spigotcvn.cvn.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarUtil {
    public static void repackJar(File resultJarFile, File unarchiveDir) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(resultJarFile);
             JarOutputStream jos = new JarOutputStream(fos)) {

            // Iterate over all files in the unarchive directory and add them to the JAR
            FileUtils.iterateOverFiles(file -> {
                try {
                    String entryName = unarchiveDir.toPath()
                            .relativize(file.toPath())
                            .toString()
                            .replace(File.separator, "/");
                    System.out.println(entryName);
                    JarEntry entry = new JarEntry(entryName);
                    jos.putNextEntry(entry);
                    Files.copy(file.toPath(), jos);
                    jos.closeEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, unarchiveDir);
        }

        FileUtils.iterateOverFiles(File::delete, unarchiveDir);
        unarchiveDir.delete();
    }
}