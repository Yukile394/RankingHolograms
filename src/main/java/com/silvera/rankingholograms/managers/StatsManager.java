package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.PlayerStats;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the authoritative in-memory copy of online players' stats and
 * synchronizes them with the database. Kills/deaths mutate the cached
 * object directly and are flushed asynchronously so the main thread is
 * never blocked on a database write.
 */
public class StatsManager {

    private final RankingHologramsPlugin plugin;
    private final Map<UUID, PlayerStats> online = new ConcurrentHashMap<>();

    public StatsManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleJoin(UUID uuid, String name) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerStats stats = plugin.databaseManager().loadPlayer(uuid, name);
            stats.setName(name);
            stats.setLastLogin(System.currentTimeMillis());
            online.put(uuid, stats);
            plugin.databaseManager().savePlayer(stats);
        });
    }

    public void handleQuit(UUID uuid) {
        PlayerStats stats = online.remove(uuid);
        if (stats == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long sessionSeconds = Math.max(0, (now - stats.getLastLogin()) / 1000L);
        stats.setTotalOnlineSeconds(stats.getTotalOnlineSeconds() + sessionSeconds);
        stats.setLastLogout(now);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.databaseManager().savePlayer(stats));
    }

    public PlayerStats get(UUID uuid) {
        return online.get(uuid);
    }

    public void registerKill(UUID killerUuid) {
        PlayerStats stats = online.get(killerUuid);
        if (stats == null) {
            return;
        }
        stats.addKill();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.databaseManager().savePlayer(stats));
    }

    public void registerDeath(UUID victimUuid) {
        PlayerStats stats = online.get(victimUuid);
        if (stats == null) {
            return;
        }
        stats.addDeath();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.databaseManager().savePlayer(stats));
    }

    /**
     * Flushes live session time for every currently online player without
     * ending their session; used before server shutdown.
     */
    public void flushAllOnline() {
        long now = System.currentTimeMillis();
        for (PlayerStats stats : online.values()) {
            long sessionSeconds = Math.max(0, (now - stats.getLastLogin()) / 1000L);
            stats.setTotalOnlineSeconds(stats.getTotalOnlineSeconds() + sessionSeconds);
            stats.setLastLogin(now);
            plugin.databaseManager().savePlayer(stats);
        }
    }
}
