package com.silvera.rankingholograms.data;

/**
 * The four leaderboard categories. Each category exposes a top 10 and is
 * addressed from hologram lines through its placeholder key, e.g.
 * %killed1% ... %killed10%.
 */
public enum LeaderboardCategory {
    KILL("killed"),
    TIME("zaman"),
    DEATH("olum"),
    CLAN("klan");

    private final String placeholderKey;

    LeaderboardCategory(String placeholderKey) {
        this.placeholderKey = placeholderKey;
    }

    public String placeholderKey() {
        return placeholderKey;
    }

    public boolean isClanBased() {
        return this == CLAN;
    }

    public static LeaderboardCategory fromKey(String key) {
        if (key == null) {
            return null;
        }
        String k = key.toLowerCase(java.util.Locale.ROOT);
        for (LeaderboardCategory c : values()) {
            if (c.placeholderKey.equals(k)) {
                return c;
            }
        }
        return null;
    }
}
