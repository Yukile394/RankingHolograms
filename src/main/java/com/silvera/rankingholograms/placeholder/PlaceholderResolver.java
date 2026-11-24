package com.silvera.rankingholograms.placeholder;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.integration.CataMinesBridge.MineInfo;
import com.silvera.rankingholograms.managers.LeaderboardManager;
import com.silvera.rankingholograms.managers.LeaderboardManager.Entry;
import com.silvera.rankingholograms.util.LegacyColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the placeholders written into hologram lines:
 * <ul>
 *   <li>%killed1%..%killed10%, %zaman1%..%zaman10%, %olum1%..%olum10%, %klan1%..%klan10%
 *       (name) and the same with a "_deger" suffix (value). Each token reads only its own category.</li>
 *   <li>%madenismi_sure% - time left until that CataMines mine resets. The name must be the
 *       real name of an existing mine; every mine works automatically.</li>
 * </ul>
 * Main thread only.
 */
public class PlaceholderResolver {

    private static final Pattern RANK =
            Pattern.compile("%(killed|zaman|olum|klan)(\\d{1,2})(_deger)?%", Pattern.CASE_INSENSITIVE);
    private static final Pattern MINE =
            Pattern.compile("%([A-Za-z0-9_\\-]+)_sure%", Pattern.CASE_INSENSITIVE);
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private record Span(int start, int end, String text, LeaderboardCategory category,
                        int rank, boolean wantValue, String mineName) {}

    private final RankingHologramsPlugin plugin;

    public PlaceholderResolver(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    private static boolean validRank(int rank) {
        return rank >= 1 && rank <= LeaderboardManager.TOP_SIZE;
    }

    /**
     * All tokens in the text that are ours. In shape mode a mine token counts even when the
     * mine does not exist right now (used to recognise our own earlier output).
     */
    private List<Span> spans(String text, boolean shapeMode) {
        List<Span> list = new ArrayList<>();
        if (text == null || text.indexOf('%') < 0) {
            return list;
        }
        Matcher r = RANK.matcher(text);
        while (r.find()) {
            int rank = Integer.parseInt(r.group(2));
            if (validRank(rank)) {
                list.add(new Span(r.start(), r.end(), r.group(), LeaderboardCategory.fromKey(r.group(1)),
                        rank, r.group(3) != null, null));
            }
        }
        Matcher m = MINE.matcher(text);
        while (m.find()) {
            if (shapeMode || plugin.cataMinesBridge().find(m.group(1)) != null) {
                list.add(new Span(m.start(), m.end(), m.group(), null, 0, false, m.group(1)));
            }
        }
        list.sort(Comparator.comparingInt(Span::start));
        List<Span> clean = new ArrayList<>();
        int last = 0;
        for (Span s : list) {
            if (s.start() >= last) {
                clean.add(s);
                last = s.end();
            }
        }
        return clean;
    }

    /** True when the text holds at least one supported token. */
    public boolean containsPlaceholder(String text) {
        return !spans(text, false).isEmpty();
    }

    /** Colours the template (legacy &-codes -> MiniMessage) and substitutes every token. */
    public String render(String template) {
        String colored = LegacyColor.toMiniMessage(template);
        StringBuilder out = new StringBuilder();
        int last = 0;
        for (Span s : spans(colored, false)) {
            out.append(colored, last, s.start()).append(value(s));
            last = s.end();
        }
        out.append(colored.substring(last));
        return out.toString();
    }

    /**
     * True when {@code text} has the shape of a rendered {@code template}
     * (literal parts identical, tokens matched by anything). Used to
     * recognise our own earlier output after a restart or hologram reload.
     */
    public boolean matchesShape(String template, String text) {
        String colored = LegacyColor.toMiniMessage(template);
        StringBuilder regex = new StringBuilder("^");
        int last = 0;
        for (Span s : spans(colored, true)) {
            if (s.start() > last) {
                regex.append(Pattern.quote(colored.substring(last, s.start())));
            }
            regex.append(".*?");
            last = s.end();
        }
        if (last < colored.length()) {
            regex.append(Pattern.quote(colored.substring(last)));
        }
        regex.append('$');
        return Pattern.compile(regex.toString(), Pattern.DOTALL).matcher(text).matches();
    }

    private String value(Span s) {
        if (s.mineName() != null) {
            return mineValue(s);
        }
        Entry entry = plugin.leaderboardManager().entry(s.category(), s.rank());
        if (entry == null) {
            return s.wantValue() ? plugin.configManager().emptyValue() : plugin.configManager().emptyName();
        }
        String raw = s.wantValue() ? LeaderboardManager.formatValue(s.category(), entry.value()) : entry.name();
        return MM.escapeTags(raw == null ? "" : raw);
    }

    private String mineValue(Span s) {
        MineInfo mine = plugin.cataMinesBridge().find(s.mineName());
        if (mine == null) {
            return s.text();
        }
        if (!mine.timed()) {
            return plugin.configManager().mineInactiveText();
        }
        return formatDuration(mine.countdownSeconds());
    }

    /** 3725 -> "1sa 2dk 5sn", 125 -> "2dk 5sn", 45 -> "45sn". */
    public static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "sa " + minutes + "dk " + seconds + "sn";
        }
        if (minutes > 0) {
            return minutes + "dk " + seconds + "sn";
        }
        return seconds + "sn";
    }
}
