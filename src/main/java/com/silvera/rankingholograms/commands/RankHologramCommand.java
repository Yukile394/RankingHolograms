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
 * Shared executor for /killsiralama1..3, /zamansiralama1..3,
 * /olumsiralama1..3 and /klansiralama1..3. Binds the given category+rank
 * hologram onto whichever Citizens NPC the sender currently has selected
 * (via Citizens' own "/npc select" or our "/npcselect").
 */
public class RankHologramCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;
    private final LeaderboardCategory category;
    private final int rank;

    public RankHologramCommand(RankingHologramsPlugin plugin, LeaderboardCategory category, int rank) {
        this.plugin = plugin;
        this.category = category;
        this.rank = rank;
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

        plugin.hologramManager().bind(selected, category, rank);
        MessageUtil.send(player, plugin.configManager().message("hologram-linked"));
        return true;
    }
}
