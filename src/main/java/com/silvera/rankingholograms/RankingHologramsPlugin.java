package com.silvera.rankingholograms;

import com.silvera.rankingholograms.commands.HologramSetupCommand;
import com.silvera.rankingholograms.commands.SetupTabCompleter;
import com.silvera.rankingholograms.config.ConfigManager;
import com.silvera.rankingholograms.db.DatabaseManager;
import com.silvera.rankingholograms.hologram.HologramManager;
import com.silvera.rankingholograms.listeners.PlayerConnectionListener;
import com.silvera.rankingholograms.listeners.PlayerDeathListener;
import com.silvera.rankingholograms.managers.ClanIntegration;
import com.silvera.rankingholograms.managers.LeaderboardManager;
import com.silvera.rankingholograms.managers.StatsManager;
import com.silvera.rankingholograms.tasks.HologramUpdateTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class RankingHologramsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private ClanIntegration clanIntegration;
    private LeaderboardManager leaderboardManager;
    private HologramManager hologramManager;
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") == null) {
            getLogger().severe(configManager.message("fancyholograms-missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.clanIntegration = new ClanIntegration(this);
        if (!clanIntegration.setup()) {
            getLogger().severe(configManager.message("simpleclans-missing"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Database baglantisi kurulamadi, plugin devre disi birakiliyor.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.statsManager = new StatsManager(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.hologramManager = new HologramManager(this);
        if (!hologramManager.setup()) {
            getLogger().severe(configManager.message("fancyholograms-api-error"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("siralama").setExecutor(new HologramSetupCommand(this));
        getCommand("siralama").setTabCompleter(new SetupTabCompleter());

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        startUpdateTask();

        getLogger().info("RankingHolograms etkinlestirildi.");
    }

    @Override
    public void onDisable() {
        stopUpdateTask();
        if (hologramManager != null) {
            hologramManager.restoreTemplates();
        }
        if (statsManager != null) {
            statsManager.flushAllOnline();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("RankingHolograms devre disi birakildi.");
    }

    /** (Re)starts the periodic refresh, e.g. after the update interval changed in config.yml. */
    public void startUpdateTask() {
        stopUpdateTask();
        long interval = configManager.updateIntervalTicks();
        this.updateTask = new HologramUpdateTask(this).runTaskTimerAsynchronously(this, 40L, interval);
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public ConfigManager configManager() {
        return configManager;
    }

    public DatabaseManager databaseManager() {
        return databaseManager;
    }

    public StatsManager statsManager() {
        return statsManager;
    }

    public ClanIntegration clanIntegration() {
        return clanIntegration;
    }

    public LeaderboardManager leaderboardManager() {
        return leaderboardManager;
    }

    public HologramManager hologramManager() {
        return hologramManager;
    }
}
