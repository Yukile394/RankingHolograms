package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * "/npchologramdurdur": toggles a freeze on the currently selected Citizens
 * NPC's rotation (and therefore its hologram, since the hologram renders on
 * top of the NPC entity). First use locks the NPC facing the direction the
 * sender is currently looking, and that same locked position/rotation is
 * then shown identically to every player - it is not a per-viewer effect.
 * Second use (same command) unfreezes it and restores normal look-close
 * rotation if the NPC had it before.
 */
public class NpcHologramFreezeCommand implements CommandExecutor {

    private final RankingHologramsPlugin plugin;

    public NpcHologramFreezeCommand(RankingHologramsPlugin plugin) {
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

        NPC selected = plugin.npcSelectionManager().getSelected(player);
        if (selected == null) {
            MessageUtil.send(player, plugin.configManager().message("hologram-freeze-not-npc"));
            return true;
        }

        boolean nowFrozen = plugin.hologramManager().toggleFreeze(selected, player.getLocation().getDirection());
        MessageUtil.send(player, plugin.configManager().message(
                nowFrozen ? "hologram-frozen" : "hologram-unfrozen"));
        return true;
    }
}
