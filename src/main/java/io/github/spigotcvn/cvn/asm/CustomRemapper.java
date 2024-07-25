package io.github.spigotcvn.cvn.asm;

import io.github.spigotcvn.cvn.CVN;
import io.github.spigotcvn.cvn.utils.CompatiblityUtils;
import org.objectweb.asm.commons.Remapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomRemapper extends Remapper {
    private final CVN plugin;

    public CustomRemapper(CVN plugin) {
        this.plugin = plugin;
    }

    @Override
    public String map(String internalName) {
        // TODO : This may not works if the plugin is made with a org.bukkit.craftbukkit.x or net.minecraft.server.x version

        Pattern cbPattern = Pattern.compile("org/bukkit/craftbukkit/([^/]+)/.*");
        Matcher cbMatcher = cbPattern.matcher(internalName);

        Pattern nmsPattern = Pattern.compile("net/minecraft/server/([^/]+)/.*");
        Matcher nmsMatcher = nmsPattern.matcher(internalName);

        if (cbMatcher.matches()) {

            // This is CB
            // If the cb naming couldn't be found, relocate to new naming
            if(plugin.getCbLocation() == null) return relocate(internalName);

            // Replace plugin's cb naming by server's cb naming
            return internalName.replaceFirst(cbMatcher.group(1), plugin.getCbLocation());

        } else if (nmsMatcher.matches()) {

            // This is NMS

            // If the server is running on 1.17+, or the cb naming couldn't be found
            if(CompatiblityUtils.isNewNMSPackages(plugin) || plugin.getCbLocation() == null) return relocate(internalName);

            // Replace plugin's nms naming by server's nms naming
            return internalName.replaceFirst(nmsMatcher.group(1), plugin.getCbLocation());

        }

        return internalName;

    }

    private String relocate(String internalName) {
        // TODO : Make it more precise
        return internalName
                .replaceAll("/v[^/]+/", "/"); // Remove vX_XX_X notation
    }
}
