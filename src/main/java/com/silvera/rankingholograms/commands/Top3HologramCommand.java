package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shared executor for /killsiralamatop3, /zamansiralamatop3,
 * /olumsiralamatop3 and /klansiralamatop3. Binds a combined "top 3 + total"
 * board (3 rank rows plus a total line) onto whichever Citizens NPC the
 * sender currently has selected, instead of a single rank like
 * RankHologramCommand does.
 */
public class Top3HologramCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;
    private final LeaderboardCategory category;

    public Top3HologramCommand(RankingHologramsPlugin plugin, LeaderboardCategory category) {
        this.plugin = plugin;
        this.category = category;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Bu komut sadece oyuncular tarafindan kullanilabilir.");
            return true;
        }
        if (!player.hasPermission("rankhologram.create")) {
            MessageUtil.send(player, plugin.configManager().message("no-permission"));
            return true;
        }

        NPC selected = plugin.npcSelectionManager().getSelected(player);
        if (selected == null) {
            MessageUtil.send(player, plugin.configManager().message("npc-not-selected"));
            return true;
        }

        plugin.hologramManager().bindTop3(selected, category);
        MessageUtil.send(player, plugin.configManager().message("hologram-linked"));
        return true;
    }
}
