package io.github.spigotcvn.cvn.asm;

import org.bukkit.Bukkit;
import org.objectweb.asm.commons.Remapper;

public class CustomRemapper extends Remapper {
    private final String CRAFTBUKKIT_PACKAGE = Bukkit.getServer().getClass().getPackage().getName();
    private final String CRAFTBUKKIT_VERSION = CRAFTBUKKIT_PACKAGE.substring(CRAFTBUKKIT_PACKAGE.lastIndexOf('.') + 1);

    public CustomRemapper() {

    }

    @Override
    public String map(String internalName) {
        if (internalName.matches("org\\/bukkit\\/craftbukkit\\/(.*?)\\/")) {
            //return internalName.replaceFirst("org\\/bukkit\\/craftbukkit\\/(.*?)\\/", newVersion);
        }
        return internalName;
    }
}
