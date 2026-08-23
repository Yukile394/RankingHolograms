package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardType;
import com.silvera.rankingholograms.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shared executor for /killsiralama, /olumsiralama, /zamansiralama and
 * /klansiralama: each spawns exactly one leaderboard hologram of the given
 * type at the sender's current location.
 */
public class CreateHologramCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;
    private final LeaderboardType type;

    public CreateHologramCommand(RankingHologramsPlugin plugin, LeaderboardType type) {
        this.plugin = plugin;
        this.type = type;
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

        plugin.hologramManager().create(type, player.getLocation());
        MessageUtil.send(player, plugin.configManager().message("hologram-created"));
        return true;
    }
}
