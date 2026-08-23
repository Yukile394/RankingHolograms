package com.silvera.rankingholograms.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SetupTabCompleter implements TabCompleter {

    private static final List<String> ROOT = List.of("oyuncu", "haftalik", "zaman", "klan", "yonetim", "reload");
    private static final List<String> CREATE_TYPES = List.of("kill", "death", "weeklykill", "weeklydeath", "clankill", "clandeath", "time");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(ROOT);
            options.add("create");
            options.add("remove");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            options.addAll(CREATE_TYPES);
        }

        List<String> result = new ArrayList<>();
        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
        for (String option : options) {
            if (option.toLowerCase().startsWith(current)) {
                result.add(option);
            }
        }
        return result;
    }
}
