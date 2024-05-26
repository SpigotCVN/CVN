package io.github.cvn.cvn;

import org.bukkit.plugin.java.JavaPlugin;

public class DummyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().disablePlugin(this);
    }
}
