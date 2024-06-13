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
        Pattern pattern = Pattern.compile("org/bukkit/craftbukkit/([^/]+)/.*");
        Matcher matcher = pattern.matcher(internalName);

        if (matcher.matches()) {
            String cbLocation = CompatiblityUtils.getCBOldNotation(plugin);

            // TODO : This is not working, rewrite it
            if(cbLocation == null) return internalName // For 1.20.5+ :
                    .replaceFirst(String.valueOf(
                                    internalName.charAt(internalName.indexOf(matcher.group(1))+1)
                    ), "") // Remove point after CB package notation
                    .replaceFirst(matcher.group(1), ""); // Remove cb notation

            return internalName.replaceFirst(matcher.group(1), cbLocation);
        }
        return internalName;
    }
}
