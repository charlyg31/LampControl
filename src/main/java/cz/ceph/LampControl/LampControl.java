/*
	Code has been adapted from jacklink01.
	Code is modified by Ceph.
	GNU General Public License version 3 (GPLv3)
*/
package cz.ceph.LampControl;

import cz.ceph.LampControl.listeners.InteractListener;
import cz.ceph.LampControl.listeners.PhysicsListener;
import cz.ceph.LampControl.utils.BlockSwitcher;
import cz.ceph.LampControl.utils.Commands;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class LampControl extends JavaPlugin {
    public static boolean opUsesHand = true, toggleLamps = true, takeItemOnUse = false, usePermissions = false,
            controlRails = true, controlShelves = true;
    public static PluginDescriptionFile pluginInfo;
    public static YamlConfiguration messagesConfig;
    private static final int CONFIG_VERSION = 7;

    public Material toolMaterial = Material.FLINT_AND_STEEL;
    private BlockSwitcher switcher;

    @Override
    public void onEnable() {
        pluginInfo = getDescription();

        saveDefaultConfig();
        if (getConfig().getInt("config-version", 0) < CONFIG_VERSION) {
            saveResource("config.yml", true);
            getLogger().info("Older config version found, created new one.");
        }
        loadConfig();
        loadMessages();

        switcher = new BlockSwitcher(this);

        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PhysicsListener(this), this);

        Commands commandExecutor = new Commands(this);
        getCommand("lamp").setExecutor(commandExecutor);
        getCommand("/lamp").setExecutor(commandExecutor);

        getLogger().info(pluginInfo.getName() + " v" + pluginInfo.getVersion() + " was enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(pluginInfo.getName() + " v" + pluginInfo.getVersion() + " was disabled!");
    }

    private void loadConfig() {
        reloadConfig();
        String itemName = getConfig().getString("lampControlItem", "FLINT_AND_STEEL");
        Material item = Material.matchMaterial(itemName);
        if (item == null || !item.isItem()) {
            getLogger().warning("Unknown lampControlItem '" + itemName + "', using FLINT_AND_STEEL.");
            item = Material.FLINT_AND_STEEL;
        }
        toolMaterial = item;
        usePermissions = getConfig().getBoolean("usePermissions", true);
        opUsesHand = getConfig().getBoolean("opUsesHand", true);
        takeItemOnUse = getConfig().getBoolean("takeItemOnUse", false);
        toggleLamps = getConfig().getBoolean("toggleLamps", true);
        controlRails = getConfig().getBoolean("controlRails", true);
        controlShelves = getConfig().getBoolean("controlShelves", true);
    }

    private void loadMessages() {
        File messages = new File(getDataFolder(), "messages.yml");
        if (!messages.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messages);
    }

    public BlockSwitcher getSwitcher() {
        return switcher;
    }
}
