package com.silvera.rankingholograms.config;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final RankingHologramsPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public long updateIntervalTicks() {
        return Math.max(1, cfg.getInt("hologram.update-interval", 5)) * 20L;
    }

    public double baseLineHeight() {
        return cfg.getDouble("hologram.base-line-height", 0.26);
    }

    public String databaseFile() {
        return cfg.getString("database.file", "data.db");
    }

    public String colorName() {
        return cfg.getString("colors.name", "#A8F0C4");
    }

    public String colorDescription() {
        return cfg.getString("colors.description", "#D3DCE0");
    }

    public String nameLineTemplate() {
        return cfg.getString("messages.name-line", "{name} (#{rank}");
    }

    /** Description line for a given category (KILL/DEATH/TIME/CLAN) and rank (1-3). */
    public String descriptionLine(String categoryKey, int rank) {
        String path = "messages.description." + categoryKey + "." + rank;
        return cfg.getString(path, categoryKey + " #" + rank);
    }

    public String message(String key) {
        return cfg.getString("messages." + key, key);
    }
}
