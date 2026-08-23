package com.silvera.rankingholograms.db;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.HologramData;
import com.silvera.rankingholograms.data.LeaderboardType;
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
import java.util.List;
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
                    weekly_kills INTEGER NOT NULL DEFAULT 0,
                    total_deaths INTEGER NOT NULL DEFAULT 0,
                    weekly_deaths INTEGER NOT NULL DEFAULT 0,
                    total_online_seconds INTEGER NOT NULL DEFAULT 0,
                    last_login INTEGER NOT NULL DEFAULT 0,
                    last_logout INTEGER NOT NULL DEFAULT 0
                );
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_stats (
                    clan_id TEXT PRIMARY KEY,
                    clan_name TEXT NOT NULL,
                    total_kills INTEGER NOT NULL DEFAULT 0,
                    weekly_kills INTEGER NOT NULL DEFAULT 0,
                    total_deaths INTEGER NOT NULL DEFAULT 0,
                    weekly_deaths INTEGER NOT NULL DEFAULT 0
                );
            """);

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS holograms (
                    id TEXT PRIMARY KEY,
                    type TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL
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
                    stats.setWeeklyKills(rs.getLong("weekly_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setWeeklyDeaths(rs.getLong("weekly_deaths"));
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
            INSERT INTO player_stats (uuid, name, total_kills, weekly_kills, total_deaths, weekly_deaths,
                total_online_seconds, last_login, last_logout)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                total_kills = excluded.total_kills,
                weekly_kills = excluded.weekly_kills,
                total_deaths = excluded.total_deaths,
                weekly_deaths = excluded.weekly_deaths,
                total_online_seconds = excluded.total_online_seconds,
                last_login = excluded.last_login,
                last_logout = excluded.last_logout
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stats.getUuid().toString());
            ps.setString(2, stats.getName());
            ps.setLong(3, stats.getTotalKills());
            ps.setLong(4, stats.getWeeklyKills());
            ps.setLong(5, stats.getTotalDeaths());
            ps.setLong(6, stats.getWeeklyDeaths());
            ps.setLong(7, stats.getTotalOnlineSeconds());
            ps.setLong(8, stats.getLastLogin());
            ps.setLong(9, stats.getLastLogout());
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
                    stats.setWeeklyKills(rs.getLong("weekly_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setWeeklyDeaths(rs.getLong("weekly_deaths"));
                    stats.setTotalOnlineSeconds(rs.getLong("total_online_seconds"));
                    list.add(stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Siralama sorgusu basarisiz: " + e.getMessage(), e);
        }
        return list;
    }

    public void resetWeeklyPlayers() {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE player_stats SET weekly_kills = 0, weekly_deaths = 0");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Haftalik oyuncu resetleme basarisiz: " + e.getMessage(), e);
        }
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
                    stats.setWeeklyKills(rs.getLong("weekly_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setWeeklyDeaths(rs.getLong("weekly_deaths"));
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
            INSERT INTO clan_stats (clan_id, clan_name, total_kills, weekly_kills, total_deaths, weekly_deaths)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(clan_id) DO UPDATE SET
                clan_name = excluded.clan_name,
                total_kills = excluded.total_kills,
                weekly_kills = excluded.weekly_kills,
                total_deaths = excluded.total_deaths,
                weekly_deaths = excluded.weekly_deaths
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, stats.getClanId());
            ps.setString(2, stats.getClanName());
            ps.setLong(3, stats.getTotalKills());
            ps.setLong(4, stats.getWeeklyKills());
            ps.setLong(5, stats.getTotalDeaths());
            ps.setLong(6, stats.getWeeklyDeaths());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan verisi kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public List<ClanStats> topClans(String column, int limit) {
        String sql = "SELECT * FROM clan_stats ORDER BY " + column + " DESC, clan_name ASC LIMIT ?";
        List<ClanStats> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClanStats stats = new ClanStats(rs.getString("clan_id"), rs.getString("clan_name"));
                    stats.setTotalKills(rs.getLong("total_kills"));
                    stats.setWeeklyKills(rs.getLong("weekly_kills"));
                    stats.setTotalDeaths(rs.getLong("total_deaths"));
                    stats.setWeeklyDeaths(rs.getLong("weekly_deaths"));
                    list.add(stats);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Klan siralama sorgusu basarisiz: " + e.getMessage(), e);
        }
        return list;
    }

    public void resetWeeklyClans() {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE clan_stats SET weekly_kills = 0, weekly_deaths = 0");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Haftalik klan resetleme basarisiz: " + e.getMessage(), e);
        }
    }

    // ---------- holograms ----------

    public void saveHologram(HologramData data) {
        String sql = """
            INSERT INTO holograms (id, type, world, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                type = excluded.type, world = excluded.world,
                x = excluded.x, y = excluded.y, z = excluded.z
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, data.getId().toString());
            ps.setString(2, data.getType().name());
            ps.setString(3, data.getWorld());
            ps.setDouble(4, data.getX());
            ps.setDouble(5, data.getY());
            ps.setDouble(6, data.getZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public void deleteHologram(UUID id) {
        String sql = "DELETE FROM holograms WHERE id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram silinemedi: " + e.getMessage(), e);
        }
    }

    public List<HologramData> loadHolograms() {
        List<HologramData> list = new ArrayList<>();
        String sql = "SELECT * FROM holograms";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString("id"));
                    LeaderboardType type = LeaderboardType.valueOf(rs.getString("type"));
                    String world = rs.getString("world");
                    double x = rs.getDouble("x");
                    double y = rs.getDouble("y");
                    double z = rs.getDouble("z");
                    list.add(new HologramData(id, type, world, x, y, z));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Bozuk hologram kaydi atlandi: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologramlar yuklenemedi: " + e.getMessage(), e);
        }
        return list;
    }

    // ---------- meta (weekly reset tracking) ----------

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
