package io.github.cvn.cvn.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
    /**
     * Insert a string at the end of the filename.
     * @param file The file to manipulate
     * @param insertion The string to insert
     * @return The new File with the insertion.
     */
    public static File insertInFileName(File file, String insertion) {
        String fileExtension = FilenameUtils.getExtension(file.getName());
        return new File(replaceLast(file.getAbsolutePath(), fileExtension, insertion + fileExtension));
    }

    public static String replaceLast(String string, String substring, String replacement)
    {
        int index = string.lastIndexOf(substring);
        if (index == -1)
            return string;
        return string.substring(0, index) + replacement
                + string.substring(index+substring.length());
    }

    public static Set<File> listFiles(File directory) {
        if(!directory.isDirectory()) return null;

        File[] files = directory.listFiles();

        if(files == null) return null;

        return Stream.of(files)
                .filter(FileUtils::isJar)
                .collect(Collectors.toSet());
    }

    public static boolean isJar(File file) {
        return file.isFile() && FilenameUtils.getExtension(file.getName()).equals(".jar");
    }
}
