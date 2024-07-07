package io.github.spigotcvn.cvn.gson;

import java.io.File;

public class JarHashModel {
    private final File jarFile;
    private final String hash;

    public JarHashModel(File jarFile, String hash) {
        this.jarFile = jarFile;
        this.hash = hash;
    }

    public File getJarFile() {
        return jarFile;
    }

    public String getHash() {
        return hash;
    }
}
