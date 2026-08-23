package com.silvera.rankingholograms.tasks;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HologramUpdateTask extends BukkitRunnable {

    private final RankingHologramsPlugin plugin;

    public HologramUpdateTask(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Leaderboard values are cached inside HologramManager.refreshAll() calls,
        // which only touch existing TextDisplay text components (no entity churn).
        plugin.hologramManager().refreshAll();
    }
}
