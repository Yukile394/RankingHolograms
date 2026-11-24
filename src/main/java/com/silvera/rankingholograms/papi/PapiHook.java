package com.silvera.rankingholograms.papi;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import org.bukkit.Bukkit;

/** Keeps every PlaceholderAPI class reference out of the main class, so the plugin also runs without PAPI. */
public final class PapiHook {

    private static PapiExpansion expansion;

    private PapiHook() {}

    public static boolean register(RankingHologramsPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI yok, %rh_...% (kisisel) placeholder'lar kapali.");
            return false;
        }
        expansion = new PapiExpansion(plugin);
        boolean ok = expansion.register();
        if (ok) {
            plugin.leaderboardManager().enablePersonal();
            plugin.getLogger().info("PlaceholderAPI genisletmesi 'rh' kaydedildi.");
        }
        return ok;
    }

    public static void unregister() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }
}
