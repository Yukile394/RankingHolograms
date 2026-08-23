package com.silvera.rankingholograms.listeners;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.entity.Interaction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

public class HologramInteractListener implements Listener {

    private final RankingHologramsPlugin plugin;

    public HologramInteractListener(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Interaction)) {
            return;
        }
        UUID hologramId = plugin.hologramManager().hologramIdForInteraction(event.getRightClicked().getUniqueId());
        if (hologramId == null) {
            return;
        }
        event.setCancelled(true);
        plugin.hologramManager().toggleWeekly(hologramId);
    }
}
