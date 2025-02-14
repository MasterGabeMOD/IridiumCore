package com.iridium.iridiumcore;

import com.iridium.iridiumcore.gui.GUI;
import com.iridium.iridiumcore.multiversion.MultiVersion;
import com.iridium.iridiumcore.nms.NMS;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * The main class of this plugin which handles initialization
 * and shutdown of the plugin.
 */
@Getter
@NoArgsConstructor
public class IridiumCore extends JavaPlugin {

    private Persist persist;
    private NMS nms;
    private MultiVersion multiVersion;
    private boolean isTesting = false;
    private BukkitTask saveTask;

    private static IridiumCore instance;

    /**
     * Constructor used for UnitTests
     */
    public IridiumCore(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        this.isTesting = true;
        // Disable logging
        getLogger().setFilter(record -> false);
    }

    /**
     * Code that should be executed before this plugin gets enabled.
     * Initializes the configurations and sets the default world generator.
     */
    @Override
    public void onLoad() {
        // Create the data folder in order to make Jackson work
        getDataFolder().mkdir();

        // Initialize the configs
        this.persist = new Persist(Persist.PersistType.YAML, this);
        loadConfigs();
        saveConfigs();
    }

    /**
     * Plugin startup logic.
     */
    @Override
    public void onEnable() {
        instance = this;
        
        if (!PaperLib.isSpigot() && !isTesting) {
            getLogger().warning("CraftBukkit isn't supported, please use Spigot or one of its forks");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        setupMultiVersion();

        registerListeners();

        if (isTesting) {
            return;
        }

        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveData, 20L * 60 * 5, 20L * 60 * 5);


        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();
            if (inventoryHolder instanceof GUI) {
                ((GUI) inventoryHolder).addContent(player.getOpenInventory().getTopInventory());
            }
        }), 20L, 20L); // Changed from 0, 1 to 20, 20 to reduce the frequency
    }

    /**
     * Plugin shutdown logic.
     */
    @Override
    public void onDisable() {
        if (isTesting) return;
        if (saveTask != null) saveTask.cancel();
        saveData();
        Bukkit.getOnlinePlayers().forEach(HumanEntity::closeInventory);
        getLogger().info("-------------------------------");
        getLogger().info("");
        getLogger().info(getDescription().getName() + " Disabled!");
        getLogger().info("");
        getLogger().info("-------------------------------");
    }

    /**
     * Registers the plugin's listeners.
     */
    public void registerListeners() {
    }

    /**
     * Saves islands, users and other data to the database.
     */
    public void saveData() {
    }

    /**
     * Automatically gets the correct {@link MultiVersion} and {@link NMS} support from the Minecraft server version.
     */
    private void setupMultiVersion() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            MinecraftVersion minecraftVersion = MinecraftVersion.byName(version);

            this.nms = minecraftVersion.getNms();
            this.multiVersion = minecraftVersion.getMultiVersion(this);
        } catch (Exception exception) {
            this.nms = MinecraftVersion.DEFAULT.getNms();
            this.multiVersion = MinecraftVersion.DEFAULT.getMultiVersion(this);
        }
    }

    /**
     * Loads the configurations required for this plugin.
     *
     * @see Persist
     */
    public void loadConfigs() {
    }


    /**
     * Saves changes to the configuration files.
     *
     * @see Persist
     */
    public void saveConfigs() {
    }

    public static IridiumCore getInstance() {
        return instance;
    }

}
