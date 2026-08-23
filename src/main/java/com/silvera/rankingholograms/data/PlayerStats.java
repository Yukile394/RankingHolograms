package com.silvera.rankingholograms.data;

import java.util.UUID;

/**
 * Persistent per-player statistics. UUID is always the stable identity key,
 * the display name is only used for rendering and is refreshed on login.
 */
public class PlayerStats {

    private final UUID uuid;
    private String name;
    private long totalKills;
    private long totalDeaths;
    private long totalOnlineSeconds;
    private long lastLogin;
    private long lastLogout;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(long totalKills) {
        this.totalKills = totalKills;
    }

    public long getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(long totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public long getTotalOnlineSeconds() {
        return totalOnlineSeconds;
    }

    public void setTotalOnlineSeconds(long totalOnlineSeconds) {
        this.totalOnlineSeconds = totalOnlineSeconds;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getLastLogout() {
        return lastLogout;
    }

    public void setLastLogout(long lastLogout) {
        this.lastLogout = lastLogout;
    }

    public void addKill() {
        totalKills++;
    }

    public void addDeath() {
        totalDeaths++;
    }
}
