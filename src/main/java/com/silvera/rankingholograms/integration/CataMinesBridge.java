package com.silvera.rankingholograms.integration;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;

/**
 * Read-only access to CataMines (verified against CataMines 2.5.11). The classes are
 * resolved at runtime, so this plugin needs no CataMines jar to compile and keeps
 * working when CataMines is missing. Every value comes straight from CataMines:
 * MineManager.getInstance().getMines() and AbstractCataMine#getCountdown().
 * Main thread only.
 */
public class CataMinesBridge {

    /** Snapshot of one mine at the moment of the lookup. */
    public record MineInfo(String name, int countdownSeconds, boolean timed) {}

    private static final String MANAGER = "de.c4t4lysm.catamines.schedulers.MineManager";
    private static final String MINE = "de.c4t4lysm.catamines.utils.mine.AbstractCataMine";

    private final RankingHologramsPlugin plugin;
    private boolean attempted;
    private boolean available;
    private Method getInstance;
    private Method getMines;
    private Method getName;
    private Method getCountdown;
    private Method isRunnable;
    private Method getResetMode;

    public CataMinesBridge(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean ensure() {
        if (attempted) {
            return available;
        }
        Plugin cata = Bukkit.getPluginManager().getPlugin("CataMines");
        if (cata == null || !cata.isEnabled()) {
            return false; // try again later, CataMines may still be loading
        }
        attempted = true;
        try {
            ClassLoader loader = cata.getClass().getClassLoader();
            Class<?> manager = Class.forName(MANAGER, true, loader);
            Class<?> mine = Class.forName(MINE, true, loader);
            getInstance = manager.getMethod("getInstance");
            getMines = manager.getMethod("getMines");
            getName = mine.getMethod("getName");
            getCountdown = mine.getMethod("getCountdown");
            isRunnable = mine.getMethod("isRunnable");
            getResetMode = mine.getMethod("getResetMode");
            available = true;
            plugin.getLogger().info("CataMines entegrasyonu etkin (%maden_sure% placeholder'lari).");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING,
                    "CataMines API'si cozumlenemedi, maden placeholder'lari kapali: " + t, t);
        }
        return available;
    }

    /** Finds a mine by its real CataMines name (case-insensitive), or null when there is none. */
    public MineInfo find(String name) {
        if (name == null || !ensure()) {
            return null;
        }
        try {
            Object manager = getInstance.invoke(null);
            Object list = getMines.invoke(manager);
            if (!(list instanceof List<?> mines)) {
                return null;
            }
            for (Object mine : mines) {
                String mineName = (String) getName.invoke(mine);
                if (mineName != null && mineName.equalsIgnoreCase(name)) {
                    boolean runnable = (Boolean) isRunnable.invoke(mine);
                    Object mode = getResetMode.invoke(mine);
                    boolean timed = runnable && mode != null && !"PERCENTAGE".equals(mode.toString());
                    int countdown = (Integer) getCountdown.invoke(mine);
                    return new MineInfo(mineName, Math.max(0, countdown), timed);
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "CataMines okunamadi: " + t, t);
            available = false;
            attempted = false;
        }
        return null;
    }
}
