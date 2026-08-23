package com.silvera.rankingholograms.data;

/**
 * Aggregated kill/death totals for a SimpleClans clan. clanId corresponds to
 * SimpleClans' internal clan tag, which is unique and stable.
 */
public class ClanStats {

    private final String clanId;
    private String clanName;
    private long totalKills;
    private long weeklyKills;
    private long totalDeaths;
    private long weeklyDeaths;

    public ClanStats(String clanId, String clanName) {
        this.clanId = clanId;
        this.clanName = clanName;
    }

    public String getClanId() {
        return clanId;
    }

    public String getClanName() {
        return clanName;
    }

    public void setClanName(String clanName) {
        this.clanName = clanName;
    }

    public long getTotalKills() {
        return totalKills;
    }

    public void setTotalKills(long totalKills) {
        this.totalKills = totalKills;
    }

    public long getWeeklyKills() {
        return weeklyKills;
    }

    public void setWeeklyKills(long weeklyKills) {
        this.weeklyKills = weeklyKills;
    }

    public long getTotalDeaths() {
        return totalDeaths;
    }

    public void setTotalDeaths(long totalDeaths) {
        this.totalDeaths = totalDeaths;
    }

    public long getWeeklyDeaths() {
        return weeklyDeaths;
    }

    public void setWeeklyDeaths(long weeklyDeaths) {
        this.weeklyDeaths = weeklyDeaths;
    }

    public void addKill() {
        totalKills++;
        weeklyKills++;
    }

    public void addDeath() {
        totalDeaths++;
        weeklyDeaths++;
    }
}
