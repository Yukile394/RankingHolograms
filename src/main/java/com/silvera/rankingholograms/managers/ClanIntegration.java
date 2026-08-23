package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import net.sacredlabyrinth.phaed.simpleclans.Clan;
import net.sacredlabyrinth.phaed.simpleclans.SimpleClans;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Thin wrapper over the real SimpleClans API. If SimpleClans is not present
 * the plugin fails to enable (declared as a hard depend in plugin.yml), so
 * by the time this class is used the plugin is guaranteed to be loaded.
 */
public class ClanIntegration {

    private final RankingHologramsPlugin plugin;
    private SimpleClans simpleClans;

    public ClanIntegration(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("SimpleClans") == null) {
            plugin.getLogger().severe("SimpleClans bulunamadi. Plugin devre disi birakiliyor.");
            return false;
        }
        try {
            this.simpleClans = SimpleClans.getInstance();
            return simpleClans != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "SimpleClans baglantisi kurulamadi: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Returns the clan tag (used as a stable clan identity) for a player,
     * or null if the player is not in a clan.
     */
    public Clan getClan(UUID playerUuid) {
        if (simpleClans == null) {
            return null;
        }
        return simpleClans.getClanManager().getClanByPlayerUniqueId(playerUuid);
    }

    public void registerClanKill(UUID killerUuid) {
        Clan clan = getClan(killerUuid);
        if (clan == null) {
            return;
        }
        String tag = clan.getTag();
        String name = clan.getName();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            ClanStats stats = plugin.databaseManager().loadClan(tag, name);
            stats.setClanName(name);
            stats.addKill();
            plugin.databaseManager().saveClan(stats);
        });
    }
}
