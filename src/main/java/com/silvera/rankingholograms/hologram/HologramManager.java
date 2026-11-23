package com.silvera.rankingholograms.hologram;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.placeholder.PlaceholderResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Scans every FancyHolograms text hologram and renders our placeholders into
 * its lines. The line as typed by the admin (the template) is remembered, so
 * the placeholder keeps updating after the line has been replaced by its
 * rendered text. Editing a line to something without placeholders releases it.
 * Main thread only.
 */
public class HologramManager {

    private record LineState(String template, String rendered) {}

    private final RankingHologramsPlugin plugin;
    private final FancyHologramsBridge bridge;
    private final PlaceholderResolver resolver;

    /** hologram (lower case) -> line index -> live state. */
    private final Map<String, Map<Integer, LineState>> states = new HashMap<>();
    /** Templates persisted in the database, used to recover after a crash or /fholograms reload. */
    private Map<String, Map<Integer, String>> saved = new HashMap<>();

    public HologramManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
        this.bridge = new FancyHologramsBridge(plugin);
        this.resolver = new PlaceholderResolver(plugin);
    }

    public boolean setup() {
        if (!bridge.setup()) {
            return false;
        }
        this.saved = plugin.databaseManager().loadTemplates();
        return true;
    }

    public PlaceholderResolver resolver() {
        return resolver;
    }

    /** Repaints every hologram that carries placeholders. */
    public void renderAll() {
        Set<String> seen = new HashSet<>();
        for (Object hologram : bridge.holograms()) {
            String name = bridge.name(hologram);
            if (name == null) {
                continue;
            }
            List<String> lines = bridge.lines(hologram);
            if (lines == null) {
                continue;
            }
            String key = name.toLowerCase(Locale.ROOT);
            seen.add(key);
            renderOne(hologram, key, lines);
        }
        states.keySet().retainAll(seen);
    }

    private void renderOne(Object hologram, String key, List<String> lines) {
        Map<Integer, LineState> state = states.computeIfAbsent(key, k -> new HashMap<>());
        Map<Integer, String> persisted = saved.get(key);
        List<String> out = new ArrayList<>(lines);
        boolean changed = false;

        for (int i = 0; i < lines.size(); i++) {
            String current = lines.get(i) == null ? "" : lines.get(i);
            LineState previous = state.get(i);
            String template = null;

            if (previous != null && current.equals(previous.rendered())) {
                template = previous.template();
            } else if (resolver.containsPlaceholder(current)) {
                template = current;
            } else if (previous == null && persisted != null && persisted.get(i) != null
                    && resolver.matchesShape(persisted.get(i), current)) {
                template = persisted.get(i);
            }

            if (template == null) {
                forget(key, i, state, persisted);
                continue;
            }

            String rendered = resolver.render(template);
            out.set(i, rendered);
            if (!rendered.equals(current)) {
                changed = true;
            }
            if (persisted == null || !template.equals(persisted.get(i))) {
                remember(key, i, template);
                persisted = saved.get(key);
            }
            state.put(i, new LineState(template, rendered));
        }

        // lines that no longer exist
        for (Integer index : new ArrayList<>(state.keySet())) {
            if (index >= lines.size()) {
                forget(key, index, state, persisted);
            }
        }

        if (changed) {
            bridge.setLines(hologram, out);
            bridge.refresh(hologram);
        }
    }

    private void remember(String key, int line, String template) {
        saved.computeIfAbsent(key, k -> new HashMap<>()).put(line, template);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.databaseManager().saveTemplate(key, line, template));
    }

    private void forget(String key, int line, Map<Integer, LineState> state, Map<Integer, String> persisted) {
        state.remove(line);
        if (persisted != null && persisted.remove(line) != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.databaseManager().deleteTemplate(key, line));
        }
    }

    /**
     * Writes the original (placeholder) text back into every hologram we
     * rendered, so FancyHolograms saves the templates and not a stale snapshot.
     */
    public void restoreTemplates() {
        for (Object hologram : bridge.holograms()) {
            String name = bridge.name(hologram);
            if (name == null) {
                continue;
            }
            Map<Integer, LineState> state = states.get(name.toLowerCase(Locale.ROOT));
            if (state == null || state.isEmpty()) {
                continue;
            }
            List<String> lines = bridge.lines(hologram);
            if (lines == null) {
                continue;
            }
            boolean changed = false;
            for (Map.Entry<Integer, LineState> e : state.entrySet()) {
                int i = e.getKey();
                if (i < lines.size() && e.getValue().rendered().equals(lines.get(i))) {
                    lines.set(i, e.getValue().template());
                    changed = true;
                }
            }
            if (changed) {
                bridge.setLines(hologram, lines);
            }
        }
    }
}
