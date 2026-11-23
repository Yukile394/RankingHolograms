package com.silvera.rankingholograms.commands;

import com.silvera.rankingholograms.data.LeaderboardCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SetupTabCompleter implements TabCompleter {

    private static final List<String> ROOT = List.of("liste", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(ROOT);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("liste")) {
            for (LeaderboardCategory c : LeaderboardCategory.values()) {
                options.add(c.placeholderKey());
            }
        }
        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        options.removeIf(o -> !o.startsWith(current));
        return options;
    }
}
