package com.silvera.rankingholograms;

import com.silvera.rankingholograms.commands.CreateHologramCommand;
import com.silvera.rankingholograms.commands.HologramSetupCommand;
import com.silvera.rankingholograms.commands.SetupTabCompleter;
import com.silvera.rankingholograms.config.ConfigManager;
import com.silvera.rankingholograms.data.LeaderboardType;
import com.silvera.rankingholograms.db.DatabaseManager;
import com.silvera.rankingholograms.listeners.HologramInteractListener;
import com.silvera.rankingholograms.listeners.PlayerConnectionListener;
import com.silvera.rankingholograms.listeners.PlayerDeathListener;
import com.silvera.rankingholograms.managers.ClanIntegration;
import com.silvera.rankingholograms.managers.HologramManager;
import com.silvera.rankingholograms.managers.StatsManager;
import com.silvera.rankingholograms.tasks.HologramUpdateTask;
import com.silvera.rankingholograms.tasks.WeeklyResetTask;
import org.bukkit.plugin.java.JavaPlugin;

public class RankingHologramsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private ClanIntegration clanIntegration;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

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
        this.hologramManager = new HologramManager(this);

        registerCommands();
        registerListeners();

        hologramManager.loadAll();

        new HologramUpdateTask(this).runTaskTimer(this, configManager.updateIntervalTicks(), configManager.updateIntervalTicks());
        new WeeklyResetTask(this).runTaskTimer(this, 20L * 5, 20L * 60);

        getLogger().info("RankingHolograms etkinlestirildi.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.flushAllOnline();
        }
        if (hologramManager != null) {
            hologramManager.despawnAll();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("RankingHolograms devre disi birakildi.");
    }

    private void registerCommands() {
        getCommand("killsiralama").setExecutor(new CreateHologramCommand(this, LeaderboardType.KILL));
        getCommand("olumsiralama").setExecutor(new CreateHologramCommand(this, LeaderboardType.DEATH));
        getCommand("zamansiralama").setExecutor(new CreateHologramCommand(this, LeaderboardType.TIME));
        getCommand("klansiralama").setExecutor(new CreateHologramCommand(this, LeaderboardType.CLAN_KILL));

        HologramSetupCommand setupCommand = new HologramSetupCommand(this);
        getCommand("siralama").setExecutor(setupCommand);
        getCommand("siralama").setTabCompleter(new SetupTabCompleter());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new HologramInteractListener(this), this);
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

    public HologramManager hologramManager() {
        return hologramManager;
    }
}
