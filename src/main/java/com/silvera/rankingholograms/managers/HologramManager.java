package com.silvera.rankingholograms.managers;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.HologramData;
import com.silvera.rankingholograms.data.LeaderboardType;
import com.silvera.rankingholograms.data.PlayerStats;
import com.silvera.rankingholograms.util.MessageUtil;
import com.silvera.rankingholograms.util.TimeFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns every live hologram entity. Every leaderboard category is rendered
 * through renderLines(), which guarantees identical alignment, spacing and
 * styling rules across categories (requirement: symmetric design).
 */
public class HologramManager {

    private final RankingHologramsPlugin plugin;
    private final Map<UUID, HologramData> holograms = new HashMap<>();
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private final Map<UUID, Interaction> interactions = new HashMap<>();
    /** Toggle state per hologram id: true means showing the weekly variant for KILL/DEATH holograms. */
    private final Map<UUID, Boolean> weeklyToggle = new HashMap<>();

    public HologramManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        for (HologramData data : plugin.databaseManager().loadHolograms()) {
            World world = plugin.getServer().getWorld(data.getWorld());
            if (world == null) {
                plugin.getLogger().warning("Gecersiz dunya, hologram atlandi: " + data.getWorld());
                continue;
            }
            holograms.put(data.getId(), data);
            spawn(data);
        }
    }

    public UUID create(LeaderboardType type, Location location) {
        UUID id = UUID.randomUUID();
        HologramData data = new HologramData(id, type, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ());
        holograms.put(id, data);
        plugin.databaseManager().saveHologram(data);
        spawn(data);
        return id;
    }

    public boolean remove(UUID id) {
        HologramData data = holograms.remove(id);
        if (data == null) {
            return false;
        }
        despawn(id);
        plugin.databaseManager().deleteHologram(id);
        weeklyToggle.remove(id);
        return true;
    }

    public Map<UUID, HologramData> getAll() {
        return holograms;
    }

    public void despawnAll() {
        for (UUID id : new ArrayList<>(displays.keySet())) {
            despawn(id);
        }
    }

    private void despawn(UUID id) {
        TextDisplay display = displays.remove(id);
        if (display != null && !display.isDead()) {
            display.remove();
        }
        Interaction interaction = interactions.remove(id);
        if (interaction != null && !interaction.isDead()) {
            interaction.remove();
        }
    }

    private void spawn(HologramData data) {
        World world = plugin.getServer().getWorld(data.getWorld());
        if (world == null) {
            return;
        }
        Location loc = new Location(world, data.getX(), data.getY(), data.getZ());

        TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
            td.setBillboard(parseBillboard());
            td.setShadowed(plugin.configManager().shadow());
            td.setSeeThrough(false);
            td.setDefaultBackground(plugin.configManager().background());
            td.setLineWidth(500);
            td.setViewRange(plugin.configManager().viewDistance() / 16f);
            td.setPersistent(true);
            Transformation t = td.getTransformation();
            t.getTranslation().set(new Vector3f(0f, 0f, 0f));
            td.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            ));
        });
        displays.put(data.getId(), display);

        // Interaction entity slightly in front, used to catch right-click toggles for weekly leaderboards.
        if (!data.getType().isClanBased() && data.getType() != LeaderboardType.TIME) {
            Interaction interaction = world.spawn(loc, Interaction.class, i -> {
                i.setInteractionWidth(1.4f);
                i.setInteractionHeight(1.4f);
                i.setPersistent(true);
                i.setResponsive(true);
            });
            interactions.put(data.getId(), interaction);
            weeklyToggle.putIfAbsent(data.getId(), false);
        }

        refresh(data.getId());
    }

    private Display.Billboard parseBillboard() {
        try {
            return Display.Billboard.valueOf(plugin.configManager().billboard().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Display.Billboard.CENTER;
        }
    }

    public UUID hologramIdForInteraction(UUID interactionEntityUuid) {
        for (Map.Entry<UUID, Interaction> entry : interactions.entrySet()) {
            if (entry.getValue().getUniqueId().equals(interactionEntityUuid)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void toggleWeekly(UUID hologramId) {
        HologramData data = holograms.get(hologramId);
        if (data == null || data.getType().isClanBased() || data.getType() == LeaderboardType.TIME) {
            return;
        }
        boolean current = weeklyToggle.getOrDefault(hologramId, false);
        weeklyToggle.put(hologramId, !current);
        refresh(hologramId);
    }

    /** Called periodically by HologramUpdateTask; recomputes and repaints text without recreating entities. */
    public void refreshAll() {
        for (UUID id : new ArrayList<>(holograms.keySet())) {
            refresh(id);
        }
    }

    public void refresh(UUID id) {
        HologramData data = holograms.get(id);
        TextDisplay display = displays.get(id);
        if (data == null || display == null || display.isDead()) {
            return;
        }

        LeaderboardType effectiveType = data.getType();
        boolean showingWeekly = weeklyToggle.getOrDefault(id, false);
        if (showingWeekly) {
            if (effectiveType == LeaderboardType.KILL) effectiveType = LeaderboardType.WEEKLY_KILL;
            else if (effectiveType == LeaderboardType.DEATH) effectiveType = LeaderboardType.WEEKLY_DEATH;
        }

        Component content = renderLines(effectiveType);
        display.text(content);
    }

    /**
     * Renders one leaderboard category into a single centered multi-line
     * component. This is the single shared renderer every category funnels
     * through, guaranteeing identical spacing/alignment.
     */
    private Component renderLines(LeaderboardType type) {
        int size = plugin.configManager().leaderboardSize();
        String titleColor = plugin.configManager().colorTitle();
        String titleText = plugin.configManager().hologramTitle(type.name());

        Component result = MessageUtil.parse(titleColor, titleText).appendNewline().appendNewline();

        List<String> primaryLines = new ArrayList<>();
        List<String> valueLines = new ArrayList<>();
        List<Boolean> unused = new ArrayList<>();

        boolean showSeconds = plugin.configManager().showSeconds();

        switch (type) {
            case KILL -> fillPlayers(primaryLines, valueLines, "total_kills", size, plugin.configManager().killSuffix(), false, showSeconds);
            case DEATH -> fillPlayers(primaryLines, valueLines, "total_deaths", size, plugin.configManager().deathSuffix(), false, showSeconds);
            case WEEKLY_KILL -> fillPlayers(primaryLines, valueLines, "weekly_kills", size, plugin.configManager().killSuffix(), false, showSeconds);
            case WEEKLY_DEATH -> fillPlayers(primaryLines, valueLines, "weekly_deaths", size, plugin.configManager().deathSuffix(), false, showSeconds);
            case TIME -> fillPlayers(primaryLines, valueLines, "total_online_seconds", size, null, true, showSeconds);
            case CLAN_KILL -> fillClans(primaryLines, valueLines, "total_kills", size, plugin.configManager().killSuffix());
            case CLAN_DEATH -> fillClans(primaryLines, valueLines, "total_deaths", size, plugin.configManager().deathSuffix());
        }

        if (primaryLines.isEmpty()) {
            return result.append(MessageUtil.parse(plugin.configManager().colorSubtitle(), plugin.configManager().message("empty-leaderboard")));
        }

        for (int i = 0; i < primaryLines.size(); i++) {
            String rankColor = rankColor(i);
            String rankLine = (i + 1) + ". " + primaryLines.get(i);
            Component line = MessageUtil.parse(rankColor, rankLine).appendNewline()
                    .append(MessageUtil.parse(plugin.configManager().colorValue(), "   " + valueLines.get(i)));
            result = result.appendNewline().append(line);
            if (i < primaryLines.size() - 1) {
                result = result.appendNewline();
            }
        }

        return result;
    }

    private String rankColor(int index) {
        return switch (index) {
            case 0 -> plugin.configManager().colorFirst();
            case 1 -> plugin.configManager().colorSecond();
            case 2 -> plugin.configManager().colorThird();
            default -> plugin.configManager().colorNormal();
        };
    }

    private void fillPlayers(List<String> names, List<String> values, String column, int limit,
                              String suffix, boolean isTime, boolean showSeconds) {
        List<PlayerStats> top = plugin.databaseManager().topPlayers(column, limit);
        for (PlayerStats stats : top) {
            names.add(stats.getName());
            if (isTime) {
                values.add(TimeFormatter.format(stats.getTotalOnlineSeconds(), showSeconds));
            } else {
                long value = switch (column) {
                    case "total_kills" -> stats.getTotalKills();
                    case "weekly_kills" -> stats.getWeeklyKills();
                    case "total_deaths" -> stats.getTotalDeaths();
                    case "weekly_deaths" -> stats.getWeeklyDeaths();
                    default -> 0L;
                };
                values.add(value + " " + suffix);
            }
        }
    }

    private void fillClans(List<String> names, List<String> values, String column, int limit, String suffix) {
        List<ClanStats> top = plugin.databaseManager().topClans(column, limit);
        for (ClanStats stats : top) {
            names.add(stats.getClanName());
            long value = column.equals("total_kills") ? stats.getTotalKills() : stats.getTotalDeaths();
            values.add(value + " " + suffix);
        }
    }
}
