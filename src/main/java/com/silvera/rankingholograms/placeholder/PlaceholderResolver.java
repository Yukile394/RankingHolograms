package com.silvera.rankingholograms.placeholder;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.managers.LeaderboardManager;
import com.silvera.rankingholograms.managers.LeaderboardManager.Entry;
import com.silvera.rankingholograms.util.LegacyColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves %killed1%..%killed10%, %zaman1%..%zaman10%, %olum1%..%olum10% and
 * %klan1%..%klan10% (name) plus the same tokens with a "_deger" suffix
 * (the stat value, e.g. %killed1_deger%). Each token reads only its own
 * category. Empty positions render as the configured fallback.
 */
public class PlaceholderResolver {

    private static final Pattern TOKEN =
            Pattern.compile("%(killed|zaman|olum|klan)(\\d{1,2})(_deger)?%", Pattern.CASE_INSENSITIVE);
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final RankingHologramsPlugin plugin;

    public PlaceholderResolver(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean validRank(int rank) {
        return rank >= 1 && rank <= LeaderboardManager.TOP_SIZE;
    }

    /** True when the text holds at least one supported token. */
    public boolean containsPlaceholder(String text) {
        if (text == null || text.indexOf('%') < 0) {
            return false;
        }
        Matcher m = TOKEN.matcher(text);
        while (m.find()) {
            if (validRank(Integer.parseInt(m.group(2)))) {
                return true;
            }
        }
        return false;
    }

    /** Colours the template (legacy &-codes -> MiniMessage) and substitutes every token. */
    public String render(String template) {
        String colored = LegacyColor.toMiniMessage(template);
        Matcher m = TOKEN.matcher(colored);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            int rank = Integer.parseInt(m.group(2));
            String replacement;
            if (!validRank(rank)) {
                replacement = m.group();
            } else {
                replacement = value(LeaderboardCategory.fromKey(m.group(1)), rank, m.group(3) != null);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * True when {@code text} has the shape of a rendered {@code template}
     * (literal parts identical, tokens matched by anything). Used to
     * recognise our own earlier output after a restart or hologram reload.
     */
    public boolean matchesShape(String template, String text) {
        String colored = LegacyColor.toMiniMessage(template);
        Matcher m = TOKEN.matcher(colored);
        StringBuilder regex = new StringBuilder("^");
        int last = 0;
        while (m.find()) {
            if (!validRank(Integer.parseInt(m.group(2)))) {
                continue;
            }
            if (m.start() > last) {
                regex.append(Pattern.quote(colored.substring(last, m.start())));
            }
            regex.append(".*?");
            last = m.end();
        }
        if (last < colored.length()) {
            regex.append(Pattern.quote(colored.substring(last)));
        }
        regex.append('$');
        return Pattern.compile(regex.toString(), Pattern.DOTALL).matcher(text).matches();
    }

    private String value(LeaderboardCategory category, int rank, boolean wantValue) {
        Entry entry = plugin.leaderboardManager().entry(category, rank);
        if (entry == null) {
            return wantValue ? plugin.configManager().emptyValue() : plugin.configManager().emptyName();
        }
        String raw = wantValue ? LeaderboardManager.formatValue(category, entry.value()) : entry.name();
        return MM.escapeTags(raw == null ? "" : raw);
    }
}
