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
        // Leaderboard values are recomputed and repainted onto each bound
        // NPC's HologramTrait; no entity churn happens here.
        plugin.hologramManager().refreshAll();
    }
}
