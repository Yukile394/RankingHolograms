package com.silvera.rankingholograms.managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.entity.Player;

/**
 * Thin helper around Citizens' own player-to-NPC selection (the same
 * selection driven by "/npc select"). We don't keep our own map: Citizens
 * already tracks per-player selection, we just read it back.
 */
public class NpcSelectionManager {

    /** Returns the NPC currently selected by this player via Citizens, or null. */
    public NPC getSelected(Player player) {
        if (!CitizensAPI.hasImplementation()) {
            return null;
        }
        return CitizensAPI.getDefaultNPCSelector().getSelected(player);
    }
}
