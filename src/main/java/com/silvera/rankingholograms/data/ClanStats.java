package com.silvera.rankingholograms.data;

/**
 * Aggregated kill totals for a SimpleClans clan. clanId corresponds to
 * SimpleClans' internal clan tag, which is unique and stable.
 */
public class ClanStats {

    private final String clanId;
    private String clanName;
    private long totalKills;

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

    public void addKill() {
        totalKills++;
    }
}
