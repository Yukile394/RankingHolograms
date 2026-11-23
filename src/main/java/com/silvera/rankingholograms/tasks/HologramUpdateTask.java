package com.silvera.rankingholograms.tasks;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Runs asynchronously: rebuilds the top 10 snapshots from the database,
 * then hands the repaint of all placeholder holograms to the main thread.
 */
public class HologramUpdateTask extends BukkitRunnable {

    private final RankingHologramsPlugin plugin;

    public HologramUpdateTask(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.leaderboardManager().refreshAndRender();
    }
}
