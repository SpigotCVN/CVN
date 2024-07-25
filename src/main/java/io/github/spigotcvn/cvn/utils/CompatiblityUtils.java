package io.github.spigotcvn.cvn.utils;

import io.github.spigotcvn.cvn.CVN;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.stianloader.picoresolve.version.MavenVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompatiblityUtils {
    /**
     * Check if a server is based on paper.
     * @return true if the server is based on paper
     */
    public static boolean isPaperBased() {
        try {
            // Any other works, just the shortest I could find.
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static String minecraftVersion;
    /**
     * Returns the actual running Minecraft version, e.g. 1.20 or 1.16.5
     *
     * @return Minecraft version
     */
    public static String getMinecraftVersion() {
        if (minecraftVersion != null) {
            return minecraftVersion;
        } else {
            String bukkitGetVersionOutput = Bukkit.getVersion();
            Matcher matcher = Pattern.compile("\\(MC: (?<version>[\\d]+\\.[\\d]+(\\.[\\d]+)?)\\)").matcher(bukkitGetVersionOutput);
            if (matcher.find()) {
                return minecraftVersion = matcher.group("version");
            } else {
                throw new RuntimeException("Could not determine Minecraft version from Bukkit.getVersion(): " + bukkitGetVersionOutput);
            }
        }
    }

    /**
     * Returns the old CraftBukkit location, no longer here after <b>Paper</b> 1.20.5
     * @return Something like v1_20_R3 or null if it is not relocated
     */
    public static @Nullable String getCBOldNotation(CVN plugin) {
        if(isNewCBPackages(plugin)) return null;
        String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
        try {
            return craftBukkitPackage.substring(craftBukkitPackage.lastIndexOf('.') + 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Check if the server use the new Paper 1.20.5 craftbukkit package location.
     */
    public static boolean isNewCBPackages(CVN plugin) {
        MavenVersion cbRelocated = MavenVersion.parse("1.20.5");
        return plugin.getActualVersion().isNewerThan(cbRelocated) && isPaperBased();
    }

    /**
     * Check if the server is after 1.16.5 for nms package location.
     */
    public static boolean isNewNMSPackages(CVN plugin) {
        MavenVersion nmsRelocated = MavenVersion.parse("1.16.5");
        return plugin.getActualVersion().isNewerThan(nmsRelocated);
    }
}
