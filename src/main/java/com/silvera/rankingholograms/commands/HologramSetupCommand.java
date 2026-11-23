package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.managers.LeaderboardManager;
import com.silvera.rankingholograms.managers.LeaderboardManager.Entry;
import com.silvera.rankingholograms.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * /siralama (aka /sıralamahologramayarla):
 *   reload           - config'i yeniden yukle ve hologramlari yenile
 *   liste <kategori> - ilk 10'u sohbette goster (killed, zaman, olum, klan)
 */
public class HologramSetupCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;

    public HologramSetupCommand(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rankhologram.admin")) {
            MessageUtil.send(sender, plugin.configManager().message("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("rankhologram.reload")) {
                    MessageUtil.send(sender, plugin.configManager().message("no-permission"));
                    return true;
                }
                plugin.configManager().reload();
                plugin.startUpdateTask();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                        () -> plugin.leaderboardManager().refreshAndRender());
                MessageUtil.send(sender, plugin.configManager().message("reload-success"));
            }
            case "liste" -> {
                LeaderboardCategory category = args.length > 1 ? LeaderboardCategory.fromKey(args[1]) : null;
                if (category == null) {
                    MessageUtil.send(sender, plugin.configManager().message("unknown-category"));
                    return true;
                }
                sendList(sender, category);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendList(CommandSender sender, LeaderboardCategory category) {
        List<Entry> top = plugin.leaderboardManager().top(category);
        if (top.isEmpty()) {
            MessageUtil.send(sender, plugin.configManager().message("empty-leaderboard"));
            return;
        }
        MessageUtil.send(sender, "<gold>Top " + LeaderboardManager.TOP_SIZE + " <gray>(" + category.placeholderKey() + ")");
        for (int i = 0; i < top.size(); i++) {
            Entry e = top.get(i);
            MessageUtil.send(sender, "<gray>#" + (i + 1) + " <white>" + escape(e.name())
                    + " <gray>- <white>" + LeaderboardManager.formatValue(category, e.value()));
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("<", "\\<");
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.send(sender, "<gray>/siralama liste <killed|zaman|olum|klan> <white>- ilk 10'u goster");
        MessageUtil.send(sender, "<gray>/siralama reload <white>- config'i yeniden yukle");
        MessageUtil.send(sender, "<gray>Hologram satirina yaz: <white>/hologram edit <isim> setline 1 &a#1 %killed1%");
        MessageUtil.send(sender, "<gray>Placeholder: <white>%killed1-10% %zaman1-10% %olum1-10% %klan1-10%");
        MessageUtil.send(sender, "<gray>Deger icin: <white>%killed1_deger% (zaman, olum, klan icin ayni)");
    }
}
