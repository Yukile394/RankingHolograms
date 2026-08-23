package com.silvera.rankingholograms.data;

/**
 * The four leaderboard categories. Each category exposes exactly 3 ranks
 * (top-3), and each rank can be bound to one Citizens NPC's hologram.
 */
public enum LeaderboardCategory {
    KILL,
    DEATH,
    TIME,
    CLAN;

    public boolean isClanBased() {
        return this == CLAN;
    }
}
