package com.silvera.rankingholograms;

import com.silvera.rankingholograms.commands.HologramSetupCommand;
import com.silvera.rankingholograms.commands.NpcSelectCommand;
import com.silvera.rankingholograms.commands.RankHologramCommand;
import com.silvera.rankingholograms.commands.SetupTabCompleter;
import com.silvera.rankingholograms.config.ConfigManager;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.db.DatabaseManager;
import com.silvera.rankingholograms.listeners.NpcRemoveListener;
import com.silvera.rankingholograms.listeners.PlayerConnectionListener;
import com.silvera.rankingholograms.listeners.PlayerDeathListener;
import com.silvera.rankingholograms.managers.ClanIntegration;
import com.silvera.rankingholograms.managers.HologramManager;
import com.silvera.rankingholograms.managers.NpcSelectionManager;
import com.silvera.rankingholograms.managers.StatsManager;
import com.silvera.rankingholograms.tasks.HologramUpdateTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RankingHologramsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private StatsManager statsManager;
    private ClanIntegration clanIntegration;
    private HologramManager hologramManager;
    private NpcSelectionManager npcSelectionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
            getLogger().severe(configManager.message("citizens-missing"));
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
        this.hologramManager = new HologramManager(this);
        this.npcSelectionManager = new NpcSelectionManager();

        registerCommands();
        registerListeners();

        hologramManager.loadAll();

        new HologramUpdateTask(this).runTaskTimer(this, configManager.updateIntervalTicks(), configManager.updateIntervalTicks());

        getLogger().info("RankingHolograms etkinlestirildi.");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) {
            statsManager.flushAllOnline();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("RankingHolograms devre disi birakildi.");
    }

    private void registerCommands() {
        getCommand("npcselect").setExecutor(new NpcSelectCommand(this));

        registerRankCommand("killsiralama1", LeaderboardCategory.KILL, 1);
        registerRankCommand("killsiralama2", LeaderboardCategory.KILL, 2);
        registerRankCommand("killsiralama3", LeaderboardCategory.KILL, 3);

        registerRankCommand("zamansiralama1", LeaderboardCategory.TIME, 1);
        registerRankCommand("zamansiralama2", LeaderboardCategory.TIME, 2);
        registerRankCommand("zamansiralama3", LeaderboardCategory.TIME, 3);

        registerRankCommand("olumsiralama1", LeaderboardCategory.DEATH, 1);
        registerRankCommand("olumsiralama2", LeaderboardCategory.DEATH, 2);
        registerRankCommand("olumsiralama3", LeaderboardCategory.DEATH, 3);

        registerRankCommand("klansiralama1", LeaderboardCategory.CLAN, 1);
        registerRankCommand("klansiralama2", LeaderboardCategory.CLAN, 2);
        registerRankCommand("klansiralama3", LeaderboardCategory.CLAN, 3);

        HologramSetupCommand setupCommand = new HologramSetupCommand(this);
        getCommand("siralama").setExecutor(setupCommand);
        getCommand("siralama").setTabCompleter(new SetupTabCompleter());
    }

    private void registerRankCommand(String name, LeaderboardCategory category, int rank) {
        getCommand(name).setExecutor(new RankHologramCommand(this, category, rank));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new NpcRemoveListener(this), this);
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

    public NpcSelectionManager npcSelectionManager() {
        return npcSelectionManager;
    }
}
