package com.silvera.rankingholograms.config;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.util.LegacyColor;
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

    /** Seconds between two leaderboard refreshes + hologram repaints. */
    public long updateIntervalTicks() {
        return Math.max(1, cfg.getInt("hologram.update-interval", 5)) * 20L;
    }

    /** How often hologram lines are re-read and repainted (maden geri sayimi icin saniye alti hassasiyet). */
    public long liveIntervalTicks() {
        return Math.max(5, cfg.getInt("hologram.live-interval-ticks", 10));
    }

    /** Shown by %madenismi_sure% when the mine has no running timer (e.g. percentage-only mode). */
    public String mineInactiveText() {
        return LegacyColor.toMiniMessage(cfg.getString("mines.inactive-text", "-"));
    }

    /** How often live session time of online players is written to the database. */
    public long timeFlushMillis() {
        return Math.max(5, cfg.getInt("hologram.time-flush-seconds", 60)) * 1000L;
    }

    public String databaseFile() {
        return cfg.getString("database.file", "data.db");
    }

    /** Shown by %killed5% etc. when that position is still empty. */
    public String emptyName() {
        return LegacyColor.toMiniMessage(cfg.getString("placeholders.empty-name", "-"));
    }

    /** Shown by %killed5_deger% etc. when that position is still empty. */
    public String emptyValue() {
        return LegacyColor.toMiniMessage(cfg.getString("placeholders.empty-value", "0"));
    }

    public String message(String key) {
        return cfg.getString("messages." + key, key);
    }
}
