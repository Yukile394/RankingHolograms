package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;
import com.silvera.rankingholograms.data.RankBinding;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPC.Metadata;
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
 *
 * Distance/visibility handling: every hologram type in this plugin (kill,
 * death, time, clan, top-3 boards) goes through this exact same NPC +
 * HologramTrait pipeline, so the fixes below apply uniformly to all of them.
 * The hologram itself never "moves" and is never re-created; only its text
 * content is repainted. What was actually causing it to disappear at range
 * is that Citizens' NPC entity stops being sent to a player once the player
 * leaves the NPC's packet tracking range (server/Paper default, usually
 * ~48-64 blocks) - the hologram is rendered as part of that same entity, so
 * it disappeared together with the NPC. applyVisibilitySettings() below
 * raises NPC.Metadata.TRACKING_RANGE (the official Citizens API for this)
 * so the NPC keeps being tracked out to the configured view-distance.
 */
public class HologramManager {

    private final RankingHologramsPlugin plugin;
    /** npcId -> binding, kept in sync with the rank_bindings table. */
    private final Map<Integer, RankBinding> bindings = new HashMap<>();
    /** npcId -> last rendered content, so unchanged holograms are never repainted (avoids flicker/packet spam). */
    private final Map<Integer, RankLine> lastRendered = new HashMap<>();
    /** npcId -> last repaint timestamp (ms), used to rate-limit repaints. */
    private final Map<Integer, Long> lastRepaintAt = new HashMap<>();
    /** npcIds whose hologram is currently frozen via /npchologramdurdur. */
    private final java.util.Set<Integer> frozenNpcIds = new java.util.HashSet<>();
    /** npcId -> whether its LookClose trait was actually enabled right before freezing (so unfreeze only re-enables when it should). */
    private final java.util.Set<Integer> hadLookCloseBeforeFreeze = new java.util.HashSet<>();

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
        applyVisibilitySettings(npc);
        lastRendered.remove(npc.getId());
        refresh(npc, true);
    }

    /**
     * Binds a "top 3" board (3 rank rows + a total line) for the given
     * category onto the given NPC. Uses rank=0 internally to distinguish a
     * top3 board binding from a single-rank binding in storage, without
     * requiring any database schema change.
     */
    public void bindTop3(NPC npc, LeaderboardCategory category) {
        RankBinding binding = new RankBinding(npc.getId(), category, 0);
        bindings.put(npc.getId(), binding);
        plugin.databaseManager().saveBinding(binding);
        applyVisibilitySettings(npc);
        lastRendered.remove(npc.getId());
        refresh(npc, true);
    }

    /** Removes any leaderboard hologram bound to the given NPC. Returns true if one existed. */
    public boolean unbind(NPC npc) {
        RankBinding removed = bindings.remove(npc.getId());
        lastRendered.remove(npc.getId());
        lastRepaintAt.remove(npc.getId());
        frozenNpcIds.remove(npc.getId());
        hadLookCloseBeforeFreeze.remove(npc.getId());
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

    /**
     * Freezes the given NPC (and its hologram, since the hologram is
     * rendered on top of the NPC) facing its current yaw/pitch, and stops
     * Citizens from turning it to look at nearby players. Because the NPC
     * entity itself stops rotating server-side, every player who sees it
     * sees the exact same locked position/direction - it's not a per-player
     * client-side effect. Returns true if the NPC is now frozen, false if
     * it was unfrozen (this method toggles).
     *
     * Citizens' LookClose trait is only disabled (via lookClose(false)),
     * never removed, while frozen: removing/re-adding the trait would reset
     * any custom settings the admin configured on it (range, realistic
     * looking, random-look, etc). Unfreezing re-enables it only if it was
     * actually enabled right before freezing, so a NPC whose LookClose was
     * already off keeps being off afterwards.
     *
     * @param npc the NPC to freeze/unfreeze
     * @param facing if freezing, the direction (yaw AND pitch) to lock the
     *               NPC to - normally the admin's own look direction at the
     *               time the command was used. Ignored when unfreezing.
     */
    public boolean toggleFreeze(NPC npc, org.bukkit.util.Vector facing) {
        int id = npc.getId();
        if (frozenNpcIds.contains(id)) {
            frozenNpcIds.remove(id);
            if (hadLookCloseBeforeFreeze.remove(id) && npc.hasTrait(net.citizensnpcs.trait.LookClose.class)) {
                npc.getOrAddTrait(net.citizensnpcs.trait.LookClose.class).lookClose(true);
            }
            return false;
        } else {
            frozenNpcIds.add(id);
            if (npc.hasTrait(net.citizensnpcs.trait.LookClose.class)) {
                net.citizensnpcs.trait.LookClose lookClose = npc.getOrAddTrait(net.citizensnpcs.trait.LookClose.class);
                if (lookClose.isEnabled()) {
                    hadLookCloseBeforeFreeze.add(id);
                    lookClose.lookClose(false);
                }
            }
            // Lock the NPC facing the given direction (the admin's look
            // direction, yaw AND pitch, at the moment the command was run).
            // Since this rotates the actual NPC entity server-side, every
            // player sees the same locked orientation - not a per-player
            // client effect.
            if (npc.isSpawned() && facing != null) {
                org.bukkit.Location loc = npc.getEntity().getLocation();
                loc.setDirection(facing);
                npc.teleport(loc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
            return true;
        }
    }

    public boolean isFrozen(int npcId) {
        return frozenNpcIds.contains(npcId);
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
                lastRendered.remove(binding.getNpcId());
                lastRepaintAt.remove(binding.getNpcId());
                frozenNpcIds.remove(binding.getNpcId());
                hadLookCloseBeforeFreeze.remove(binding.getNpcId());
                plugin.databaseManager().deleteBinding(binding.getNpcId());
                continue;
            }
            refresh(npc, false);
        }
    }

    private void refresh(NPC npc, boolean force) {
        RankBinding binding = bindings.get(npc.getId());
        if (binding == null) {
            return;
        }
        // Metadata.TRACKING_RANGE is marked persistent so it should survive a
        // restart on its own, but re-applying it here on every refresh cycle
        // is cheap (a metadata set, no packets) and guarantees a bound NPC's
        // visibility settings can never silently drift back to the server
        // default, e.g. after Citizens reloads NPC data.
        applyVisibilitySettings(npc);
        RankLine line = binding.getRank() == 0
                ? computeTop3Line(binding.getCategory())
                : computeLine(binding.getCategory(), binding.getRank());
        renderHologram(npc, line, force);
    }

    /**
     * Applies the configured packet-tracking distance (and, if
     * always-visible is on, disables Citizens' distance-based despawn) to a
     * hologram-bound NPC. This is what keeps the hologram visible far away
     * instead of only re-appearing on approach; it does not move the
     * hologram's coordinates in any way.
     *
     * Two things are set:
     * 1) NPC.Metadata.TRACKING_RANGE on the NPC itself, and
     * 2) HologramTrait#setViewRange(int) on its hologram trait directly -
     *    each hologram line is actually a separate helper entity spawned by
     *    Citizens, and HologramTrait's own spawnHologram() only copies the
     *    NPC's TRACKING_RANGE onto a new line's entity if the trait's own
     *    viewRange field is unset (-1); calling setViewRange() here removes
     *    that dependency and guarantees every line entity gets the same
     *    tracking distance as the NPC, for every hologram type in this
     *    plugin (kill/death/time/clan single-rank and top3 boards alike).
     *
     * TRACKING_RANGE only controls how far the *server* will keep sending
     * entity packets for this NPC (and now its hologram lines). The
     * Minecraft client's own entity render distance / fog distance
     * (controlled by the player's video settings and the server's
     * view-distance in server.properties) is a separate, client-side limit
     * that no plugin can override - once a player is that far away the
     * server has already stopped loading/rendering that world area for
     * them regardless of tracking range.
     */
    private void applyVisibilitySettings(NPC npc) {
        int viewDistance = plugin.configManager().viewDistance();
        npc.data().setPersistent(Metadata.TRACKING_RANGE, viewDistance);
        npc.getOrAddTrait(HologramTrait.class).setViewRange(viewDistance);
        if (plugin.configManager().alwaysVisible()) {
            // -1 tells Citizens to skip its own distance-based activation
            // range check for this NPC, so it doesn't unspawn/deactivate
            // client-side entities while a player is still within
            // TRACKING_RANGE but outside the server's normal activation range.
            npc.data().setPersistent(Metadata.ACTIVATION_RANGE, -1);
            // Force-loads the NPC's chunk regardless of any player's
            // distance, so the NPC (and hologram lines) never disappear
            // because their chunk got unloaded server-side.
            npc.data().setPersistent(Metadata.KEEP_CHUNK_LOADED, true);
        }
    }


    private void clearHologram(NPC npc) {
        // Give the NPC its plain nameplate back, regardless of whether a
        // HologramTrait line is still attached.
        npc.data().setPersistent(Metadata.NAMEPLATE_VISIBLE, true);
        npc.setAlwaysUseNameHologram(false);
        if (!npc.hasTrait(HologramTrait.class)) {
            return;
        }
        npc.getOrAddTrait(HologramTrait.class).clear();
    }

    /**
     * Pushes the hologram lines onto the NPC via HologramTrait.
     * HologramTrait renders every line centered by default and stacks lines
     * above the NPC's head using the given line height, which is exactly
     * the "symmetric / same center" requirement, and it automatically
     * follows the NPC's entity scale (e.g. after "/npc attribute scale 2")
     * since the renderer positions relative to the NPC's live bounding box.
     *
     * The NPC's own nameplate (whatever "/npc rename" set it to) is hidden
     * here, so a bound NPC never needs to be renamed by hand and its plain
     * name never renders underneath/behind our lines.
     *
     * Repaint throttling: the hologram is only actually cleared+rewritten
     * when either the text content changed since the last repaint, or the
     * configured min-repaint-interval has elapsed and content differs. If
     * nothing changed, this method returns immediately without touching the
     * trait at all - this removes the "blinks out for 1ms and comes back"
     * flicker that happened when every periodic refresh cleared and
     * rewrote identical text every single tick.
     */
    private void renderHologram(NPC npc, RankLine line, boolean force) {
        int id = npc.getId();
        RankLine previous = lastRendered.get(id);
        if (!force && line.equals(previous)) {
            return;
        }
        if (!force) {
            long now = System.currentTimeMillis();
            long last = lastRepaintAt.getOrDefault(id, 0L);
            if (now - last < plugin.configManager().minRepaintIntervalMillis()) {
                return;
            }
            lastRepaintAt.put(id, now);
        }

        npc.data().setPersistent(Metadata.NAMEPLATE_VISIBLE, false);
        npc.setAlwaysUseNameHologram(true);
        HologramTrait trait = npc.getOrAddTrait(HologramTrait.class);
        trait.clear();
        trait.setLineHeight(plugin.configManager().baseLineHeight());
        for (int i = line.lines().size() - 1; i >= 0; i--) {
            // Added in reverse so the first line in the list ends up on top,
            // matching how the previous 2-line layout stacked description
            // under the name line.
            trait.addLine(line.lines().get(i));
        }
        lastRendered.put(id, line);
    }

    /** Resolves the actual name + description text for a single category/rank pair (existing single-rank holograms). */
    private RankLine computeLine(LeaderboardCategory category, int rank) {
        int index = rank - 1;
        String subject;
        boolean hasData;
        long value = 0;

        if (category == LeaderboardCategory.CLAN) {
            List<ClanStats> top = plugin.databaseManager().topClans(3);
            hasData = index < top.size();
            subject = hasData ? top.get(index).getClanName() : null;
            value = hasData ? top.get(index).getTotalKills() : 0;
        } else {
            String column = statsColumn(category);
            List<PlayerStats> top = plugin.databaseManager().topPlayers(column, 3);
            hasData = index < top.size();
            if (hasData) {
                PlayerStats stats = top.get(index);
                subject = stats.getName();
                value = statValue(category, stats);
            } else {
                subject = null;
            }
        }

        String categoryKey = category.name();
        String descriptionTemplate = plugin.configManager().descriptionLine(categoryKey, rank);

        if (!hasData) {
            return new RankLine(List.of(colorize(plugin.configManager().message("empty-leaderboard"), null)));
        }

        String nameLine = plugin.configManager().nameLineTemplate()
                .replace("{name}", subject)
                .replace("{rank}", String.valueOf(rank));

        // 2. satir: aciklayici etiket ("En Cok Kill Alan Kisi" vb.), sayi icermez.
        String descriptionLine = descriptionTemplate
                .replace("{value}", formatValue(category, value))
                .replace("{rank}", String.valueOf(rank));

        // 3. satir: bu kisinin/klanin gercek sayisi, kucuk/soluk renkte
        // (top3-total'daki gibi sabit sari renk degil, colors.value ile kontrol edilir).
        String valueLine = plugin.configManager().totalValueTemplate(categoryKey)
                .replace("{value}", formatValue(category, value));

        // Name line first so it ends up on top after renderHologram()'s reverse-add, matching the previous layout.
        return new RankLine(List.of(
                colorize(nameLine, plugin.configManager().colorName()),
                colorize(descriptionLine, plugin.configManager().colorDescription()),
                colorize(valueLine, plugin.configManager().colorValue())
        ));
    }

    /**
     * Resolves a 4-line "top 3" board for the given category: one row per
     * rank (1-3) using messages.top3-row, followed by a total line (sum of
     * the top 3 values) using messages.top3-total.<category>.
     */
    private RankLine computeTop3Line(LeaderboardCategory category) {
        List<String> rows = new ArrayList<>();
        long total = 0;
        boolean anyData = false;

        if (category == LeaderboardCategory.CLAN) {
            List<ClanStats> top = plugin.databaseManager().topClans(3);
            for (int i = 0; i < 3; i++) {
                if (i < top.size()) {
                    anyData = true;
                    long v = top.get(i).getTotalKills();
                    total += v;
                    rows.add(top3Row(i + 1, top.get(i).getClanName(), category, v));
                } else {
                    rows.add(top3EmptyRow(i + 1));
                }
            }
        } else {
            List<PlayerStats> top = plugin.databaseManager().topPlayers(statsColumn(category), 3);
            for (int i = 0; i < 3; i++) {
                if (i < top.size()) {
                    anyData = true;
                    long v = statValue(category, top.get(i));
                    total += v;
                    rows.add(top3Row(i + 1, top.get(i).getName(), category, v));
                } else {
                    rows.add(top3EmptyRow(i + 1));
                }
            }
        }

        if (!anyData) {
            return new RankLine(List.of(colorize(plugin.configManager().message("empty-leaderboard"), null)));
        }

        String totalLine = plugin.configManager().top3TotalTemplate(category.name())
                .replace("{value}", formatValue(category, total));

        List<String> lines = new ArrayList<>(rows);
        lines.add(totalLine); // total line is the extra 4th line, shown below the 3 rank rows
        return new RankLine(lines);
    }

    private String top3Row(int rank, String name, LeaderboardCategory category, long value) {
        return plugin.configManager().top3RowTemplate()
                .replace("{rank}", String.valueOf(rank))
                .replace("{name}", name)
                .replace("{value}", formatValue(category, value));
    }

    private String top3EmptyRow(int rank) {
        return plugin.configManager().top3RowTemplate()
                .replace("{rank}", String.valueOf(rank))
                .replace("{name}", "-")
                .replace("{value}", "0");
    }

    private String statsColumn(LeaderboardCategory category) {
        return switch (category) {
            case KILL -> "total_kills";
            case DEATH -> "total_deaths";
            case TIME -> "total_online_seconds";
            default -> "total_kills";
        };
    }

    private long statValue(LeaderboardCategory category, PlayerStats stats) {
        return switch (category) {
            case DEATH -> stats.getTotalDeaths();
            case TIME -> stats.getTotalOnlineSeconds();
            default -> stats.getTotalKills();
        };
    }

    private String colorize(String text, String hexColor) {
        return hexColor == null ? text : "<" + hexColor + ">" + text;
    }

    /** Formats a raw stat value for display: seconds become "Xs Ydk", everything else stays a plain count. */
    private String formatValue(LeaderboardCategory category, long value) {
        if (category != LeaderboardCategory.TIME) {
            return String.valueOf(value);
        }
        long hours = value / 3600;
        long minutes = (value % 3600) / 60;
        return hours + "s " + minutes + "dk";
    }

    /** lines() are ordered top-to-bottom as displayed; renderHologram() adds them in reverse to HologramTrait. */
    private record RankLine(List<String> lines) {}
}
