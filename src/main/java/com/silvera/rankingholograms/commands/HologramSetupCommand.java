package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.HologramData;
import com.silvera.rankingholograms.data.LeaderboardType;
import com.silvera.rankingholograms.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Handles /siralama (aka /sıralamahologramayarla). With no arguments it
 * shows the 5 main categories described in the spec; sub-arguments drill
 * into leaderboard type selection or hologram management.
 */
public class HologramSetupCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;

    public HologramSetupCommand(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafindan kullanilabilir.");
            return true;
        }
        if (!player.hasPermission("rankhologram.admin")) {
            MessageUtil.send(player, plugin.configManager().message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "oyuncu" -> sendPlayerMenu(player);
            case "haftalik" -> sendWeeklyMenu(player);
            case "zaman" -> plugin.hologramManager().create(LeaderboardType.TIME, player.getLocation());
            case "klan" -> sendClanMenu(player);
            case "yonetim" -> sendManagementMenu(player);
            case "create" -> {
                if (args.length < 2) return true;
                createByKeyword(player, args[1]);
            }
            case "list" -> sendManagementMenu(player);
            case "remove" -> {
                if (args.length < 2) {
                    MessageUtil.send(player, plugin.configManager().message("hologram-not-found"));
                    return true;
                }
                removeById(player, args[1]);
            }
            case "reload" -> {
                if (!player.hasPermission("rankhologram.reload")) {
                    MessageUtil.send(player, plugin.configManager().message("no-permission"));
                    return true;
                }
                plugin.configManager().reload();
                MessageUtil.send(player, plugin.configManager().message("reload-success"));
            }
            default -> sendMainMenu(player);
        }
        return true;
    }

    private void sendMainMenu(Player player) {
        String title = plugin.configManager().colorTitle();
        player.sendMessage(MessageUtil.parse(title, "== Siralama Hologram Ayarlari =="));
        player.sendMessage(clickable("1. Oyuncu Siralamalari", "/siralama oyuncu"));
        player.sendMessage(clickable("2. Haftalik Siralamalar", "/siralama haftalik"));
        player.sendMessage(clickable("3. Zaman Siralamasi", "/siralama zaman"));
        player.sendMessage(clickable("4. Klan Siralamalari", "/siralama klan"));
        player.sendMessage(clickable("5. Hologram Yonetimi", "/siralama yonetim"));
    }

    private void sendPlayerMenu(Player player) {
        player.sendMessage(clickable("Kill Siralamasi", "/siralama create kill"));
        player.sendMessage(clickable("Olum Siralamasi", "/siralama create death"));
    }

    private void sendWeeklyMenu(Player player) {
        player.sendMessage(clickable("Haftalik Kill", "/siralama create weeklykill"));
        player.sendMessage(clickable("Haftalik Olum", "/siralama create weeklydeath"));
    }

    private void sendClanMenu(Player player) {
        player.sendMessage(clickable("Klan Kill", "/siralama create clankill"));
        player.sendMessage(clickable("Klan Olum", "/siralama create clandeath"));
    }

    private void sendManagementMenu(Player player) {
        Map<UUID, HologramData> all = plugin.hologramManager().getAll();
        if (all.isEmpty()) {
            player.sendMessage(MessageUtil.parse(plugin.configManager().colorSubtitle(), "Kayitli hologram yok."));
            return;
        }
        for (HologramData data : all.values()) {
            String line = data.getType().name() + " @ " + data.getWorld()
                    + " (" + Math.round(data.getX()) + ", " + Math.round(data.getY()) + ", " + Math.round(data.getZ()) + ")";
            Component comp = MessageUtil.parse(plugin.configManager().colorNormal(), line + "  ")
                    .append(clickable("[Sil]", "/siralama remove " + data.getId()));
            player.sendMessage(comp);
        }
    }

    private void createByKeyword(Player player, String keyword) {
        LeaderboardType type = switch (keyword.toLowerCase()) {
            case "kill" -> LeaderboardType.KILL;
            case "death" -> LeaderboardType.DEATH;
            case "weeklykill" -> LeaderboardType.WEEKLY_KILL;
            case "weeklydeath" -> LeaderboardType.WEEKLY_DEATH;
            case "clankill" -> LeaderboardType.CLAN_KILL;
            case "clandeath" -> LeaderboardType.CLAN_DEATH;
            case "time" -> LeaderboardType.TIME;
            default -> null;
        };
        if (type == null) {
            return;
        }
        plugin.hologramManager().create(type, player.getLocation());
        MessageUtil.send(player, plugin.configManager().message("hologram-created"));
    }

    private void removeById(Player player, String rawId) {
        try {
            UUID id = UUID.fromString(rawId);
            if (plugin.hologramManager().remove(id)) {
                MessageUtil.send(player, plugin.configManager().message("hologram-removed"));
            } else {
                MessageUtil.send(player, plugin.configManager().message("hologram-not-found"));
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.send(player, plugin.configManager().message("hologram-not-found"));
        }
    }

    private Component clickable(String text, String cmd) {
        return MessageUtil.parse(plugin.configManager().colorValue(), text)
                .clickEvent(ClickEvent.runCommand(cmd));
    }
}
