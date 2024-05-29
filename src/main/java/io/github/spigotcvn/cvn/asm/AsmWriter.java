package io.github.spigotcvn.cvn.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class AsmWriter {
    public static String writeAsm(File clazz, Remapper remapper) throws IOException {
        ClassReader reader = new ClassReader(Files.newInputStream(clazz.toPath()));
        String basePath = reader.getClassName();
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, remapper), 0);
        byte[] outputData = writer.toByteArray();

        try (FileOutputStream outputStream = new FileOutputStream(clazz)) {
            outputStream.write(outputData);
        }

        return basePath;
    }
}
