package com.silvera.rankingholograms.data;

/**
 * A persisted link between a Citizens NPC (identified by its Citizens
 * integer id, stable across restarts) and a leaderboard category + rank
 * (1, 2 or 3). At most one binding exists per NPC.
 */
public class RankBinding {

    private final int npcId;
    private final LeaderboardCategory category;
    private final int rank; // 1, 2 or 3

    public RankBinding(int npcId, LeaderboardCategory category, int rank) {
        this.npcId = npcId;
        this.category = category;
        this.rank = rank;
    }

    public int getNpcId() {
        return npcId;
    }

    public LeaderboardCategory getCategory() {
        return category;
    }

    public int getRank() {
        return rank;
    }
}
