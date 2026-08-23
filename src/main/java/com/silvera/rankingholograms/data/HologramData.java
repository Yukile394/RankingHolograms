package com.silvera.rankingholograms.data;

import java.util.UUID;

/**
 * A persisted hologram: its category, world/coordinates, and the entity IDs
 * currently rendering it (populated at runtime, not persisted).
 */
public class HologramData {

    private final UUID id;
    private final LeaderboardType type;
    private final String world;
    private final double x;
    private final double y;
    private final double z;

    public HologramData(UUID id, LeaderboardType type, String world, double x, double y, double z) {
        this.id = id;
        this.type = type;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getId() {
        return id;
    }

    public LeaderboardType getType() {
        return type;
    }

    public String getWorld() {
        return world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
