package io.github.spigotcvn.cvn.version;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {
    private final int major, minor, patch;

    /**
     * Get the actual server version using bukkit
     */
    public Version() {
        String version = Bukkit.getVersion();
        String mcVersion;

        Matcher matcher = Pattern.compile("\\(MC: (?<version>[\\d]+\\.[\\d]+(\\.[\\d]+)?)\\)").matcher(version);

        if (matcher.find()) {
            mcVersion = matcher.group("version");
        } else {
            throw new RuntimeException("Could not determine Minecraft version from Bukkit.getVersion(): " + version);
        }

        String[] mcParts = mcVersion.split("\\.");

        this.major = Integer.parseInt(mcParts[0]);
        this.minor = Integer.parseInt(mcParts[1]);
        this.patch = Integer.parseInt(mcParts[2]);
    }

    /**
     * Get a version instance based on the given arguments
     */
    public Version(int major, int minor, int patch){
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Get a version instance based on a string like "1.20.4"
     */
    public Version(@NotNull String version) {
        String[] split = version.split("\\.");
        if (split.length < 3) {
            throw new IllegalArgumentException("Invalid version string " + version);
        }

        try {
            this.major = Integer.parseInt(split[0]);
            this.minor = Integer.parseInt(split[1]);
            this.patch = Integer.parseInt(split[2]);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid version string " + version);
        }
    }

    /**
     * 1.19.4 -> 1
     *
     * @return the major
     */
    public int getMajor() {
        return major;
    }

    /**
     * 1.19.4 -> 19
     *
     * @return the minor
     */
    public int getMinor() {
        return minor;
    }

    /**
     * 1.19.4 -> 4
     *
     * @return the patch
     */
    public int getPatch() {
        return patch;
    }

    @Override
    public String toString() {
        return getMajor() + "." + getMinor() + "." + getPatch();
    }
}
