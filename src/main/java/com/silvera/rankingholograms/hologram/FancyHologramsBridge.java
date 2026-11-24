package com.silvera.rankingholograms.hologram;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Single point of contact with FancyHolograms. The API is resolved at
 * runtime so the plugin does not need a compile-time dependency and works
 * with both the v2 API (de.oliver.fancyholograms) and the v3 API
 * (com.fancyinnovations.fancyholograms). All calls must run on the main thread.
 */
public class FancyHologramsBridge {

    private enum Mode { V2, V3 }

    private final RankingHologramsPlugin plugin;
    private Mode mode;
    private Object source;          // v2: HologramManager, v3: HologramRegistry
    private boolean warned;

    public FancyHologramsBridge(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        Plugin fancy = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        if (fancy == null) {
            return false;
        }
        ClassLoader loader = fancy.getClass().getClassLoader();

        try {
            Class<?> api = Class.forName("com.fancyinnovations.fancyholograms.api.FancyHolograms", true, loader);
            Object instance = call(api, "get");
            this.source = call(instance, "getRegistry");
            this.mode = Mode.V3;
            plugin.getLogger().info("FancyHolograms v3 API algilandi.");
            return source != null;
        } catch (Throwable ignored) {
            // not v3, try v2
        }

        try {
            Class<?> api = Class.forName("de.oliver.fancyholograms.api.FancyHologramsPlugin", true, loader);
            Object instance = call(api, "get");
            this.source = call(instance, "getHologramManager");
            this.mode = Mode.V2;
            plugin.getLogger().info("FancyHolograms v2 API algilandi.");
            return source != null;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "FancyHolograms API'si cozumlenemedi: " + t, t);
            return false;
        }
    }

    /** Every hologram currently known to FancyHolograms. */
    public List<Object> holograms() {
        if (source == null) {
            return List.of();
        }
        try {
            Object result = call(source, mode == Mode.V3 ? "getAll" : "getHolograms");
            if (result instanceof Collection<?> c) {
                return new ArrayList<>(c);
            }
        } catch (Throwable t) {
            warnOnce("Hologram listesi alinamadi", t);
        }
        return List.of();
    }

    public String name(Object hologram) {
        try {
            Object data = call(hologram, "getData");
            Object name = call(data, "getName");
            return name == null ? null : name.toString();
        } catch (Throwable t) {
            warnOnce("Hologram adi okunamadi", t);
            return null;
        }
    }

    /** Text lines of a text hologram, or null for block/item holograms. */
    @SuppressWarnings("unchecked")
    public List<String> lines(Object hologram) {
        try {
            Object data = call(hologram, "getData");
            if (data == null || !has(data, "getText") || !has(data, "setText")) {
                return null;
            }
            Object text = call(data, "getText");
            if (text instanceof List<?> list) {
                return new ArrayList<>((List<String>) list);
            }
        } catch (Throwable t) {
            warnOnce("Hologram satirlari okunamadi", t);
        }
        return null;
    }

    public void setLines(Object hologram, List<String> lines) {
        try {
            Object data = call(hologram, "getData");
            call(data, "setText", new ArrayList<>(lines));
        } catch (Throwable t) {
            warnOnce("Hologram satirlari yazilamadi", t);
        }
    }

    /** Pushes the changed text to everybody who currently sees the hologram. */
    public void refresh(Object hologram) {
        try {
            if (mode == Mode.V2) {
                callIfPresent(hologram, "forceUpdate");
                callIfPresent(hologram, "refreshForViewersInWorld");
            } else {
                Object viewers = callIfPresent(hologram, "getViewers");
                if (viewers instanceof Collection<?> c && has(hologram, "updateFor")) {
                    for (Object o : c) {
                        Player p = o instanceof UUID id ? Bukkit.getPlayer(id) : null;
                        if (p != null) {
                            call(hologram, "updateFor", p);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            warnOnce("Hologram yenilenemedi", t);
        }
    }

    // ---- reflection helpers ----

    private static boolean has(Object target, String name) {
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static Object callIfPresent(Object target, String name, Object... args) throws Exception {
        return has(target, name) ? call(target, name, args) : null;
    }

    private static final java.util.Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private static boolean accepts(Method m, Object[] args) {
        Class<?>[] types = m.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            if (args[i] != null && !types[i].isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Object call(Object target, String name, Object... args) throws Exception {
        Class<?> type = target instanceof Class<?> c ? c : target.getClass();
        boolean isStatic = target instanceof Class<?>;
        String cacheKey = type.getName() + "#" + name + "#" + args.length;
        Method cached = METHOD_CACHE.get(cacheKey);
        if (cached != null && accepts(cached, args)) {
            return cached.invoke(isStatic ? null : target, args);
        }
        for (Method m : type.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) {
                continue;
            }
            boolean ok = true;
            Class<?>[] types = m.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                if (args[i] != null && !types[i].isInstance(args[i])) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                continue;
            }
            m.setAccessible(true);
            METHOD_CACHE.put(cacheKey, m);
            return m.invoke(isStatic ? null : target, args);
        }
        throw new NoSuchMethodException(type.getName() + "#" + name);
    }

    private void warnOnce(String message, Throwable t) {
        if (!warned) {
            warned = true;
            plugin.getLogger().log(Level.WARNING, message + ": " + t, t);
        }
    }
}
