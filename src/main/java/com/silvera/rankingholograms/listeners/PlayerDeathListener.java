package com.silvera.rankingholograms.listeners;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts only genuine player-vs-player kills. Mob kills, environmental
 * deaths and self kills are never counted, and each death event instance
 * is processed at most once.
 */
public class PlayerDeathListener implements Listener {

    private final RankingHologramsPlugin plugin;
    private final Set<UUID> processedDeathEvents = ConcurrentHashMap.newKeySet();

    public PlayerDeathListener(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID marker = victim.getUniqueId();

        // Guard against any plugin re-firing the same death (e.g. via custom respawn logic).
        if (!processedDeathEvents.add(marker)) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> processedDeathEvents.remove(marker), 5L);

        plugin.statsManager().registerDeath(victim.getUniqueId());

        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return; // no valid PvP killer: mob death, environmental death, or self kill
        }

        plugin.statsManager().registerKill(killer.getUniqueId());
        plugin.clanIntegration().registerClanKill(killer.getUniqueId());
    }
}
