package io.github.spigotcvn.cvn.asm;

import io.github.spigotcvn.cvn.utils.CompatiblityUtils;
import org.objectweb.asm.commons.Remapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomRemapper extends Remapper {
    @Override
    public String map(String internalName) {
        Pattern pattern = Pattern.compile("org/bukkit/craftbukkit/([^/]+)/.*");
        Matcher matcher = pattern.matcher(internalName);

        if (matcher.matches()) {
            String cbLocation = CompatiblityUtils.getCBOldNotation();
            if(cbLocation == null) return internalName;

            return internalName.replaceFirst(matcher.group(1), cbLocation);
        }
        return internalName;
    }
}
