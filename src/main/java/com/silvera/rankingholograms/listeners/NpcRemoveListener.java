package com.silvera.rankingholograms.listeners;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Keeps rank_bindings clean when an admin deletes a Citizens NPC directly
 * (e.g. "/npc remove") instead of going through our own unbind flow.
 */
public class NpcRemoveListener implements Listener {

    private final RankingHologramsPlugin plugin;

    public NpcRemoveListener(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNpcRemove(NPCRemoveEvent event) {
        plugin.hologramManager().unbind(event.getNPC());
    }
}
