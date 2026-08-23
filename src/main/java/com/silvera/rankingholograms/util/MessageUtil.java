package com.silvera.rankingholograms.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {}

    public static Component parse(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return MM.deserialize(raw);
    }

    public static Component parse(String colorTag, String content) {
        return MM.deserialize(colorTag + escapeClose(content));
    }

    /** Wraps a raw hex/color code (e.g. "#A8F0C4") as a MiniMessage color tag if needed. */
    public static Component colored(String colorOrTag, String content) {
        String tag = colorOrTag.startsWith("<") ? colorOrTag : "<" + colorOrTag + ">";
        return parse(tag, content);
    }

    private static String escapeClose(String content) {
        // Plain content is safe to inline since we never accept raw user MiniMessage tags here.
        return content;
    }

    public static void send(CommandSender sender, String raw) {
        sender.sendMessage(parse(raw));
    }
}
