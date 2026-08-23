package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.util.MessageUtil;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /siralama (aka /sıralamahologramayarla). With no arguments it
 * shows the current admin actions; "remove" unbinds the selected NPC's
 * hologram, "reload" reloads the config.
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
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "remove" -> removeSelected(player);
            case "reload" -> {
                if (!player.hasPermission("rankhologram.reload")) {
                    MessageUtil.send(player, plugin.configManager().message("no-permission"));
                    return true;
                }
                plugin.configManager().reload();
                plugin.hologramManager().refreshAll();
                MessageUtil.send(player, plugin.configManager().message("reload-success"));
            }
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MessageUtil.parse("<gray>/siralama remove <white>- secili NPC'nin hologram baglantisini kaldir"));
        player.sendMessage(MessageUtil.parse("<gray>/siralama reload <white>- config'i yeniden yukle"));
    }

    private void removeSelected(Player player) {
        NPC selected = plugin.npcSelectionManager().getSelected(player);
        if (selected == null) {
            MessageUtil.send(player, plugin.configManager().message("npc-not-selected"));
            return;
        }
        if (plugin.hologramManager().unbind(selected)) {
            MessageUtil.send(player, plugin.configManager().message("hologram-unlinked"));
        } else {
            MessageUtil.send(player, plugin.configManager().message("hologram-not-found"));
        }
    }
}
