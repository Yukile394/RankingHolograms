package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds an in-memory snapshot of the top 10 of every category. The snapshot
 * is rebuilt off the main thread from the existing database tables, so
 * placeholder rendering never touches the database.
 */
public class LeaderboardManager {

    public static final int TOP_SIZE = 10;

    public record Entry(String name, long value) {}

    private final RankingHologramsPlugin plugin;
    private final Map<LeaderboardCategory, List<Entry>> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);
    private volatile long lastTimeFlush = System.currentTimeMillis();

    // personal placeholders (%rh_...%): rank of every player / clan, key = uuid or clan id
    private volatile boolean personalEnabled;
    private volatile Map<LeaderboardCategory, Map<String, Integer>> personalRanks = Map.of();
    private volatile Map<String, Long> clanKills = Map.of();

    public LeaderboardManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Rank is 1-based. Returns null when that position is empty. */
    public Entry entry(LeaderboardCategory category, int rank) {
        List<Entry> list = cache.get(category);
        if (list == null || rank < 1 || rank > list.size()) {
            return null;
        }
        return list.get(rank - 1);
    }

    public List<Entry> top(LeaderboardCategory category) {
        return cache.getOrDefault(category, List.of());
    }

    /** Rebuilds all four top-10 lists. Must be called off the main thread. */
    public void refreshNow() {
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (now - lastTimeFlush >= plugin.configManager().timeFlushMillis()) {
                lastTimeFlush = now;
                plugin.statsManager().flushOnlineTime();
            }
            for (LeaderboardCategory category : LeaderboardCategory.values()) {
                cache.put(category, load(category));
            }
            if (personalEnabled) {
                loadPersonal();
            }
        } finally {
            refreshing.set(false);
        }
    }

    public void enablePersonal() {
        this.personalEnabled = true;
    }

    /** 1-based global rank of a player (uuid) or clan (clan id), or null when unknown. */
    public Integer rankOf(LeaderboardCategory category, String key) {
        Map<String, Integer> ranks = personalRanks.get(category);
        return ranks == null || key == null ? null : ranks.get(key);
    }

    public Long clanKillsOf(String clanId) {
        return clanId == null ? null : clanKills.get(clanId);
    }

    private void loadPersonal() {
        Map<LeaderboardCategory, Map<String, Integer>> ranks = new java.util.EnumMap<>(LeaderboardCategory.class);
        for (LeaderboardCategory category : LeaderboardCategory.values()) {
            if (category == LeaderboardCategory.CLAN) {
                continue;
            }
            Map<String, Integer> map = new HashMap<>();
            int position = 1;
            for (String id : plugin.databaseManager().orderedPlayerIds(column(category))) {
                map.put(id, position++);
            }
            ranks.put(category, map);
        }
        Map<String, Integer> clanRanks = new HashMap<>();
        Map<String, Long> kills = new HashMap<>();
        int position = 1;
        for (ClanStats clan : plugin.databaseManager().allClans()) {
            clanRanks.put(clan.getClanId(), position++);
            kills.put(clan.getClanId(), clan.getTotalKills());
        }
        ranks.put(LeaderboardCategory.CLAN, clanRanks);
        this.personalRanks = ranks;
        this.clanKills = kills;
    }

    /** Async-safe: rebuilds the snapshot, then repaints holograms on the main thread. */
    public void refreshAndRender() {
        refreshNow();
        if (plugin.isEnabled()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.hologramManager().renderAll());
        }
    }

    private List<Entry> load(LeaderboardCategory category) {
        List<Entry> result = new ArrayList<>(TOP_SIZE);
        if (category == LeaderboardCategory.CLAN) {
            for (ClanStats clan : plugin.databaseManager().topClans(TOP_SIZE)) {
                result.add(new Entry(clan.getClanName(), clan.getTotalKills()));
            }
        } else {
            for (PlayerStats stats : plugin.databaseManager().topPlayers(column(category), TOP_SIZE)) {
                result.add(new Entry(stats.getName(), value(category, stats)));
            }
        }
        return List.copyOf(result);
    }

    private static String column(LeaderboardCategory category) {
        return switch (category) {
            case DEATH -> "total_deaths";
            case TIME -> "total_online_seconds";
            default -> "total_kills";
        };
    }

    private static long value(LeaderboardCategory category, PlayerStats stats) {
        return switch (category) {
            case DEATH -> stats.getTotalDeaths();
            case TIME -> stats.getTotalOnlineSeconds();
            default -> stats.getTotalKills();
        };
    }

    /** Seconds become "Xs Ydk", everything else stays a plain count. */
    public static String formatValue(LeaderboardCategory category, long value) {
        if (category != LeaderboardCategory.TIME) {
            return String.valueOf(value);
        }
        long hours = value / 3600;
        long minutes = (value % 3600) / 60;
        return hours + "s " + minutes + "dk";
    }
}
