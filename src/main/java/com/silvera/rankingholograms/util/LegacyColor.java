package com.silvera.rankingholograms.util;

/**
 * Converts legacy colour codes (&a, &c, &6, &l, &r, &#RRGGBB, also with the
 * section sign) into MiniMessage tags, which is what FancyHolograms renders.
 * Like legacy codes, a colour code resets earlier formatting.
 */
public final class LegacyColor {

    private static final String[] COLORS = {
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
            "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"
    };

    private LegacyColor() {}

    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len) {
                char n = Character.toLowerCase(input.charAt(i + 1));

                if (n == '#' && i + 7 < len && isHex(input, i + 2, 6)) {
                    out.append("<reset><#").append(input, i + 2, i + 8).append('>');
                    i += 7;
                    continue;
                }
                int idx = Character.digit(n, 16);
                if (idx >= 0) {
                    out.append("<reset><").append(COLORS[idx]).append('>');
                    i++;
                    continue;
                }
                String tag = switch (n) {
                    case 'l' -> "<bold>";
                    case 'o' -> "<italic>";
                    case 'n' -> "<underlined>";
                    case 'm' -> "<strikethrough>";
                    case 'k' -> "<obfuscated>";
                    case 'r' -> "<reset>";
                    default -> null;
                };
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean isHex(String s, int from, int count) {
        for (int i = from; i < from + count; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
