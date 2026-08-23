package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.util.MessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * "/npcselect": selects the nearest Citizens NPC for the sender, exactly as
 * Citizens' own "/npc select" would when standing next to an NPC and
 * right-clicking it. Provided as a convenience so a rank command can always
 * be preceded by "walk up to the NPC, /npcselect" without needing to
 * right-click.
 */
public class NpcSelectCommand implements CommandExecutor {

    private static final double MAX_DISTANCE = 6.0;

    private final RankingHologramsPlugin plugin;

    public NpcSelectCommand(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
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

        NPC nearest = null;
        double nearestDistance = MAX_DISTANCE;
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (!npc.isSpawned()) {
                continue;
            }
            if (!npc.getEntity().getWorld().equals(player.getWorld())) {
                continue;
            }
            double distance = npc.getEntity().getLocation().distance(player.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = npc;
            }
        }

        if (nearest == null) {
            MessageUtil.send(player, plugin.configManager().message("no-nearby-npc"));
            return true;
        }

        CitizensAPI.getDefaultNPCSelector().select(player, nearest);
        MessageUtil.send(player, plugin.configManager().message("npc-selected")
                .replace("{name}", nearest.getName()));
        return true;
    }
}
