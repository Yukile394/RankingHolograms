package com.silvera.rankingholograms.data;

/**
 * All leaderboard categories supported by the plugin.
 * Every category is rendered through the same symmetric hologram renderer.
 */
public enum LeaderboardType {
    KILL(false),
    DEATH(false),
    TIME(false),
    CLAN_KILL(true),
    CLAN_DEATH(true),
    WEEKLY_KILL(false),
    WEEKLY_DEATH(false);

    private final boolean clanBased;

    LeaderboardType(boolean clanBased) {
        this.clanBased = clanBased;
    }

    public boolean isClanBased() {
        return clanBased;
    }

    public boolean isWeekly() {
        return this == WEEKLY_KILL || this == WEEKLY_DEATH;
    }
}
