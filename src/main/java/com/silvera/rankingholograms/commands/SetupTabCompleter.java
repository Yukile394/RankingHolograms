package com.silvera.rankingholograms.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SetupTabCompleter implements TabCompleter {

    private static final List<String> ROOT = List.of("remove", "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length != 1) {
            return result;
        }
        String current = args[0].toLowerCase();
        for (String option : ROOT) {
            if (option.startsWith(current)) {
                result.add(option);
            }
        }
        return result;
    }
}
