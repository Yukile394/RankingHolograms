package com.silvera.rankingholograms.config;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.DayOfWeek;

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

    public int leaderboardSize() {
        return Math.max(1, cfg.getInt("settings.leaderboard-size", 10));
    }

    public long updateIntervalTicks() {
        return Math.max(1, cfg.getInt("hologram.update-interval", 5)) * 20L;
    }

    public int viewDistance() {
        return cfg.getInt("hologram.view-distance", 32);
    }

    public boolean shadow() {
        return cfg.getBoolean("hologram.shadow", false);
    }

    public boolean background() {
        return cfg.getBoolean("hologram.background", false);
    }

    public double lineSpacing() {
        return cfg.getDouble("hologram.line-spacing", 0.28);
    }

    public String billboard() {
        return cfg.getString("hologram.billboard", "CENTER");
    }

    public boolean weeklyEnabled() {
        return cfg.getBoolean("weekly.enabled", true);
    }

    public DayOfWeek resetDay() {
        try {
            return DayOfWeek.valueOf(cfg.getString("weekly.reset-day", "MONDAY").toUpperCase());
        } catch (IllegalArgumentException e) {
            return DayOfWeek.MONDAY;
        }
    }

    public int resetHour() {
        return cfg.getInt("weekly.reset-hour", 0);
    }

    public int resetMinute() {
        return cfg.getInt("weekly.reset-minute", 0);
    }

    public boolean showSeconds() {
        return cfg.getBoolean("time.show-seconds", false);
    }

    public String databaseFile() {
        return cfg.getString("database.file", "data.db");
    }

    public String colorTitle() {
        return cfg.getString("colors.title", "<gradient:#00C6FF:#0072FF>");
    }

    public String colorFirst() {
        return cfg.getString("colors.first", "<gold>");
    }

    public String colorSecond() {
        return cfg.getString("colors.second", "<gray>");
    }

    public String colorThird() {
        return cfg.getString("colors.third", "<#CD7F32>");
    }

    public String colorNormal() {
        return cfg.getString("colors.normal", "<white>");
    }

    public String colorValue() {
        return cfg.getString("colors.value", "<yellow>");
    }

    public String colorSubtitle() {
        return cfg.getString("colors.subtitle", "<#7F8C9A>");
    }

    public String hologramTitle(String key) {
        String path = "messages.hologram-titles." + key;
        return cfg.getString(path, key);
    }

    public String killSuffix() {
        return cfg.getString("messages.kill-suffix", "Kill");
    }

    public String deathSuffix() {
        return cfg.getString("messages.death-suffix", "Olum");
    }

    public String message(String key) {
        return cfg.getString("messages." + key, key);
    }
}
