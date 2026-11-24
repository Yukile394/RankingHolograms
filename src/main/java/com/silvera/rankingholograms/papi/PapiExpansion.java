package com.silvera.rankingholograms.papi;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;
import com.silvera.rankingholograms.managers.LeaderboardManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import org.bukkit.OfflinePlayer;

/**
 * Per-viewer placeholders, resolved by FancyHolograms through PlaceholderAPI:
 *   %rh_killed%  %rh_zaman%  %rh_olum%  %rh_klan%            - viewer's own value
 *   %rh_killed_sira% %rh_zaman_sira% %rh_olum_sira% %rh_klan_sira% - viewer's own rank
 * "klan" means the viewer's clan (its total kills and its clan rank).
 */
public class PapiExpansion extends PlaceholderExpansion {

    private final RankingHologramsPlugin plugin;

    public PapiExpansion(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rh";
    }

    @Override
    public String getAuthor() {
        return "YukileX";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }
        String[] parts = params.toLowerCase(java.util.Locale.ROOT).split("_");
        LeaderboardCategory category = LeaderboardCategory.fromKey(parts[0]);
        if (category == null || parts.length > 2 || (parts.length == 2 && !parts[1].equals("sira"))) {
            return null;
        }
        boolean wantRank = parts.length == 2;
        String emptyName = plugin.configManager().emptyName();
        String emptyValue = plugin.configManager().emptyValue();
        if (player == null) {
            return wantRank ? emptyName : emptyValue;
        }

        if (category == LeaderboardCategory.CLAN) {
            Clan clan = plugin.clanIntegration().getClan(player.getUniqueId());
            if (clan == null) {
                return wantRank ? emptyName : emptyValue;
            }
            if (wantRank) {
                Integer rank = plugin.leaderboardManager().rankOf(category, clan.getTag());
                return rank == null ? emptyName : String.valueOf(rank);
            }
            Long kills = plugin.leaderboardManager().clanKillsOf(clan.getTag());
            return String.valueOf(kills == null ? 0 : kills);
        }

        PlayerStats stats = plugin.statsManager().get(player.getUniqueId());
        if (wantRank) {
            Integer rank = plugin.leaderboardManager().rankOf(category, player.getUniqueId().toString());
            return rank == null ? emptyName : String.valueOf(rank);
        }
        if (stats == null) {
            return emptyValue;
        }
        long value = switch (category) {
            case DEATH -> stats.getTotalDeaths();
            case TIME -> stats.getTotalOnlineSeconds()
                    + Math.max(0, (System.currentTimeMillis() - stats.getLastLogin()) / 1000L);
            default -> stats.getTotalKills();
        };
        return LeaderboardManager.formatValue(category, value);
    }
}
