package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;
import com.silvera.rankingholograms.data.RankBinding;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.trait.HologramTrait;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns every NPC <-> leaderboard-rank binding. Rendering is delegated to
 * Citizens' own HologramTrait, which is attached to the NPC itself: it
 * already tracks the NPC's position and (via alwaysUseNameHologram) its
 * scale, so the hologram sits exactly where the NPC's nametag would and
 * grows/shrinks together with "/npc attribute scale".
 */
public class HologramManager {

    private final RankingHologramsPlugin plugin;
    /** npcId -> binding, kept in sync with the rank_bindings table. */
    private final Map<Integer, RankBinding> bindings = new HashMap<>();

    public HologramManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        for (RankBinding binding : plugin.databaseManager().loadBindings()) {
            bindings.put(binding.getNpcId(), binding);
        }
        refreshAll();
    }

    /** Binds (or replaces) the given category+rank hologram onto the given NPC. */
    public void bind(NPC npc, LeaderboardCategory category, int rank) {
        RankBinding binding = new RankBinding(npc.getId(), category, rank);
        bindings.put(npc.getId(), binding);
        plugin.databaseManager().saveBinding(binding);
        refresh(npc);
    }

    /** Removes any leaderboard hologram bound to the given NPC. Returns true if one existed. */
    public boolean unbind(NPC npc) {
        RankBinding removed = bindings.remove(npc.getId());
        if (removed == null) {
            return false;
        }
        plugin.databaseManager().deleteBinding(npc.getId());
        clearHologram(npc);
        return true;
    }

    public RankBinding getBinding(int npcId) {
        return bindings.get(npcId);
    }

    /** Called periodically; recomputes leaderboard values and repaints every bound NPC's hologram. */
    public void refreshAll() {
        if (!CitizensAPI.hasImplementation()) {
            return;
        }
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        for (RankBinding binding : new ArrayList<>(bindings.values())) {
            NPC npc = registry.getById(binding.getNpcId());
            if (npc == null) {
                // NPC was deleted through Citizens directly; drop the stale binding.
                bindings.remove(binding.getNpcId());
                plugin.databaseManager().deleteBinding(binding.getNpcId());
                continue;
            }
            refresh(npc);
        }
    }

    private void refresh(NPC npc) {
        RankBinding binding = bindings.get(npc.getId());
        if (binding == null) {
            return;
        }
        RankLine line = computeLine(binding.getCategory(), binding.getRank());
        renderHologram(npc, line);
    }

    private void clearHologram(NPC npc) {
        if (!npc.hasTrait(HologramTrait.class)) {
            return;
        }
        HologramTrait trait = npc.getOrAddTrait(HologramTrait.class);
        trait.clear();
        npc.setAlwaysUseNameHologram(false);
    }

    /**
     * Pushes the two-line, centered, colored hologram onto the NPC via
     * HologramTrait. HologramTrait renders every line centered by default
     * and stacks lines above the NPC's head using the given line height,
     * which is exactly the "symmetric / same center" requirement, and it
     * automatically follows the NPC's entity scale (e.g. after
     * "/npc attribute scale 2") since the renderer positions relative to
     * the NPC's live bounding box.
     */
    private void renderHologram(NPC npc, RankLine line) {
        npc.setAlwaysUseNameHologram(true);
        HologramTrait trait = npc.getOrAddTrait(HologramTrait.class);
        trait.clear();
        trait.setLineHeight(plugin.configManager().baseLineHeight());
        trait.addLine(nameLineMiniMessage(line));
        trait.addLine(descriptionLineMiniMessage(line));
    }

    private String nameLineMiniMessage(RankLine line) {
        String color = plugin.configManager().colorName();
        return "<" + color + ">" + line.nameLine();
    }

    private String descriptionLineMiniMessage(RankLine line) {
        String color = plugin.configManager().colorDescription();
        return "<" + color + ">" + line.descriptionLine();
    }

    /** Resolves the actual name + description text for a category/rank pair. */
    private RankLine computeLine(LeaderboardCategory category, int rank) {
        int index = rank - 1;
        String subject;
        boolean hasData;

        if (category == LeaderboardCategory.CLAN) {
            List<ClanStats> top = plugin.databaseManager().topClans(3);
            hasData = index < top.size();
            subject = hasData ? top.get(index).getClanName() : null;
        } else {
            String column = switch (category) {
                case KILL -> "total_kills";
                case DEATH -> "total_deaths";
                case TIME -> "total_online_seconds";
                default -> "total_kills";
            };
            List<PlayerStats> top = plugin.databaseManager().topPlayers(column, 3);
            hasData = index < top.size();
            subject = hasData ? top.get(index).getName() : null;
        }

        String categoryKey = category.name();
        String descriptionTemplate = plugin.configManager().descriptionLine(categoryKey, rank);

        if (!hasData) {
            return new RankLine(plugin.configManager().message("empty-leaderboard"), "");
        }

        String nameLine = plugin.configManager().nameLineTemplate()
                .replace("{name}", subject)
                .replace("{rank}", String.valueOf(rank));

        return new RankLine(nameLine, descriptionTemplate);
    }

    private record RankLine(String nameLine, String descriptionLine) {}
}
