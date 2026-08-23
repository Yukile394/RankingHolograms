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

    /**
     * Desired packet tracking distance for hologram-bound NPCs, in blocks.
     * Applied via NPC.Metadata.TRACKING_RANGE so the NPC (and therefore its
     * HologramTrait) keeps being sent to the client at long range instead of
     * despawning at the server/Paper default entity-tracking-range.
     */
    public int viewDistance() {
        return Math.max(16, cfg.getInt("hologram.view-distance", 128));
    }

    /**
     * When true, hologram-bound NPCs are also exempt from Citizens'
     * distance-based despawn/unspawn behaviour (activation range), on top of
     * the raised packet tracking range. This is the closest approximation to
     * "always visible" that Citizens' API exposes; it cannot override the
     * Minecraft client's own render/fog distance.
     */
    public boolean alwaysVisible() {
        return cfg.getBoolean("hologram.always-visible", true);
    }

    /** Minimum milliseconds between two content repaints of the same hologram, to avoid flicker/packet spam. */
    public long minRepaintIntervalMillis() {
        return Math.max(0, cfg.getInt("hologram.min-repaint-interval-ms", 250));
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

    /** Line template for a single row inside a top-3 board (e.g. "{rank}. {name} - {value}"). */
    public String top3RowTemplate() {
        return cfg.getString("messages.top3-row", "<gray>#{rank} <white>{name}");
    }

    /** Final "total" line template shown under the three top3 rows. */
    public String top3TotalTemplate(String categoryKey) {
        String path = "messages.top3-total." + categoryKey;
        return cfg.getString(path, "<yellow>Toplam {value} " + categoryKey);
    }

    public String message(String key) {
        return cfg.getString("messages." + key, key);
    }
}
