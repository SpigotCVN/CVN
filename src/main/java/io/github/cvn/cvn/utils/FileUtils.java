package io.github.cvn.cvn.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Set;
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
}
