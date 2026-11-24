package com.silvera.rankingholograms.db;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite backed persistence layer. All schema access happens through this
 * class so a MySQL/MariaDB implementation can be swapped in later by
 * providing an alternate DataSource and dialect-specific DDL.
 */
public class DatabaseManager {

    private final RankingHologramsPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(RankingHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), plugin.configManager().databaseFile());
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setMaximumPoolSize(1); // SQLite only supports a single writer safely
            hikariConfig.setPoolName("RankingHolograms-SQLite");
            this.dataSource = new HikariDataSource(hikariConfig);

            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database baglantisi kurulamadi: " + e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTables() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_stats (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    total_kills INTEGER NOT NULL DEFAULT 0,
                    total_deaths INTEGER NOT NULL DEFAULT 0,
                    total_online_seconds INTEGER NOT NULL DEFAULT 0,
                    last_login INTEGER NOT NULL DEFAULT 0,
                    last_logout INTEGER NOT NULL DEFAULT 0
                );
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_stats (
                    clan_id TEXT PRIMARY KEY,
                    clan_name TEXT NOT NULL,
                    total_kills INTEGER NOT NULL DEFAULT 0
                );
            """);

            // Hologram satirlarindaki placeholder sablonlari (FancyHolograms).
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hologram_templates (
                    holo TEXT NOT NULL,
                    line INTEGER NOT NULL,
                    template TEXT NOT NULL,
                    PRIMARY KEY (holo, line)
                );
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS plugin_meta (
                    meta_key TEXT PRIMARY KEY,
                    meta_value TEXT NOT NULL
                );
            """);
        }
    }

    // ---------- player stats ----------

    public PlayerStats loadPlayer(UUID uuid, String fallbackName) {
        String sql = "SELECT * FROM player_stats WHERE uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerStats stats = new PlayerStats(uuid, rs.getString("name"));
                    stats.setTotalKills(rs.getLong("total_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setTotalOnlineSeconds(rs.getLong("total_online_seconds"));
                    stats.setLastLogin(rs.getLong("last_login"));
                    stats.setLastLogout(rs.getLong("last_logout"));
                    return stats;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Oyuncu verisi yuklenemedi: " + e.getMessage(), e);
        }
        return new PlayerStats(uuid, fallbackName);
    }

    public void savePlayer(PlayerStats stats) {
        String sql = """
            INSERT INTO player_stats (uuid, name, total_kills, total_deaths, total_online_seconds, last_login, last_logout)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                total_kills = excluded.total_kills,
                total_deaths = excluded.total_deaths,
                total_online_seconds = excluded.total_online_seconds,
                last_login = excluded.last_login,
                last_logout = excluded.last_logout
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stats.getUuid().toString());
            ps.setString(2, stats.getName());
            ps.setLong(3, stats.getTotalKills());
            ps.setLong(4, stats.getTotalDeaths());
            ps.setLong(5, stats.getTotalOnlineSeconds());
            ps.setLong(6, stats.getLastLogin());
            ps.setLong(7, stats.getLastLogout());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Oyuncu verisi kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public List<PlayerStats> topPlayers(String column, int limit) {
        String sql = "SELECT * FROM player_stats ORDER BY " + column + " DESC, name ASC LIMIT ?";
        List<PlayerStats> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PlayerStats stats = new PlayerStats(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                    stats.setTotalKills(rs.getLong("total_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setTotalOnlineSeconds(rs.getLong("total_online_seconds"));
                    list.add(stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Siralama sorgusu basarisiz: " + e.getMessage(), e);
        }
        return list;
    }

    // ---------- clan stats ----------

    public ClanStats loadClan(String clanId, String fallbackName) {
        String sql = "SELECT * FROM clan_stats WHERE clan_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClanStats stats = new ClanStats(clanId, rs.getString("clan_name"));
                    stats.setTotalKills(rs.getLong("total_kills"));
                    return stats;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan verisi yuklenemedi: " + e.getMessage(), e);
        }
        return new ClanStats(clanId, fallbackName);
    }

    public void saveClan(ClanStats stats) {
        String sql = """
            INSERT INTO clan_stats (clan_id, clan_name, total_kills)
            VALUES (?, ?, ?)
            ON CONFLICT(clan_id) DO UPDATE SET
                clan_name = excluded.clan_name,
                total_kills = excluded.total_kills
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stats.getClanId());
            ps.setString(2, stats.getClanName());
            ps.setLong(3, stats.getTotalKills());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan verisi kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public List<ClanStats> topClans(int limit) {
        String sql = "SELECT * FROM clan_stats ORDER BY total_kills DESC, clan_name ASC LIMIT ?";
        List<ClanStats> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClanStats stats = new ClanStats(rs.getString("clan_id"), rs.getString("clan_name"));
                    stats.setTotalKills(rs.getLong("total_kills"));
                    list.add(stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan siralama sorgusu basarisiz: " + e.getMessage(), e);
        }
        return list;
    }

    /** Every player uuid ordered exactly like the top lists (used for personal ranks). */
    public List<String> orderedPlayerIds(String column) {
        String sql = "SELECT uuid FROM player_stats ORDER BY " + column + " DESC, name ASC";
        List<String> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Kisisel siralama sorgusu basarisiz: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ClanStats> allClans() {
        String sql = "SELECT * FROM clan_stats ORDER BY total_kills DESC, clan_name ASC";
        List<ClanStats> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ClanStats stats = new ClanStats(rs.getString("clan_id"), rs.getString("clan_name"));
                stats.setTotalKills(rs.getLong("total_kills"));
                list.add(stats);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan listesi alinamadi: " + e.getMessage(), e);
        }
        return list;
    }

    // ---------- hologram line templates ----------

    public void saveTemplate(String holo, int line, String template) {
        String sql = """
            INSERT INTO hologram_templates (holo, line, template) VALUES (?, ?, ?)
            ON CONFLICT(holo, line) DO UPDATE SET template = excluded.template
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, holo);
            ps.setInt(2, line);
            ps.setString(3, template);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram sablonu kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public void deleteTemplate(String holo, int line) {
        String sql = "DELETE FROM hologram_templates WHERE holo = ? AND line = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, holo);
            ps.setInt(2, line);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram sablonu silinemedi: " + e.getMessage(), e);
        }
    }

    public Map<String, Map<Integer, String>> loadTemplates() {
        Map<String, Map<Integer, String>> result = new HashMap<>();
        String sql = "SELECT holo, line, template FROM hologram_templates";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.computeIfAbsent(rs.getString("holo"), k -> new HashMap<>())
                        .put(rs.getInt("line"), rs.getString("template"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram sablonlari yuklenemedi: " + e.getMessage(), e);
        }
        return result;
    }

    // ---------- meta ----------

    public String getMeta(String key) {
        String sql = "SELECT meta_value FROM plugin_meta WHERE meta_key = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("meta_value");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Meta veri okunamadi: " + e.getMessage(), e);
        }
        return null;
    }

    public void setMeta(String key, String value) {
        String sql = """
            INSERT INTO plugin_meta (meta_key, meta_value) VALUES (?, ?)
            ON CONFLICT(meta_key) DO UPDATE SET meta_value = excluded.meta_value
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Meta veri yazilamadi: " + e.getMessage(), e);
        }
    }
}
