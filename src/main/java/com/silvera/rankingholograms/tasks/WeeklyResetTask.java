package com.silvera.rankingholograms.tasks;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

/**
 * Runs once a minute and performs the weekly reset exactly once per
 * configured reset window. The last reset timestamp is persisted so a
 * server restart never triggers a duplicate reset for the same week.
 */
public class WeeklyResetTask extends BukkitRunnable {

    private static final String META_KEY = "last_weekly_reset";

    private final RankingHologramsPlugin plugin;

    public WeeklyResetTask(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.configManager().weeklyEnabled()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now();
        DayOfWeek targetDay = plugin.configManager().resetDay();
        int targetHour = plugin.configManager().resetHour();
        int targetMinute = plugin.configManager().resetMinute();

        boolean withinResetWindow = now.getDayOfWeek() == targetDay
                && now.getHour() == targetHour
                && now.getMinute() == targetMinute;

        if (!withinResetWindow) {
            return;
        }

        String lastResetKey = weekKeyFor(now);
        String storedKey = plugin.databaseManager().getMeta(META_KEY);
        if (lastResetKey.equals(storedKey)) {
            return; // already reset for this window
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.statsManager().resetWeekly();
            plugin.databaseManager().resetWeeklyClans();
            plugin.databaseManager().setMeta(META_KEY, lastResetKey);
            plugin.getLogger().info("Haftalik siralama sifirlandi.");
        });
    }

    private String weekKeyFor(ZonedDateTime time) {
        return time.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
                + "-W" + time.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }
}
