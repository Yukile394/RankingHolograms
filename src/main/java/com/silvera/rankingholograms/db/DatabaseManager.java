package com.silvera.rankingholograms.db;

import com.silvera.rankingholograms.RankingHologramsPlugin;
import com.silvera.rankingholograms.data.ClanStats;
import com.silvera.rankingholograms.data.LeaderboardCategory;
import com.silvera.rankingholograms.data.PlayerStats;
import com.silvera.rankingholograms.data.RankBinding;
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

            // One row per NPC that currently has a leaderboard hologram bound to it.
            // npc_id is the Citizens NPC's stable integer id.
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rank_bindings (
                    npc_id INTEGER PRIMARY KEY,
                    category TEXT NOT NULL,
                    rank INTEGER NOT NULL
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

    // ---------- rank bindings (NPC <-> leaderboard rank) ----------

    public void saveBinding(RankBinding binding) {
        String sql = """
            INSERT INTO rank_bindings (npc_id, category, rank)
            VALUES (?, ?, ?)
            ON CONFLICT(npc_id) DO UPDATE SET category = excluded.category, rank = excluded.rank
        """;
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, binding.getNpcId());
            ps.setString(2, binding.getCategory().name());
            ps.setInt(3, binding.getRank());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram baglantisi kaydedilemedi: " + e.getMessage(), e);
        }
    }

    public void deleteBinding(int npcId) {
        String sql = "DELETE FROM rank_bindings WHERE npc_id = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, npcId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram baglantisi silinemedi: " + e.getMessage(), e);
        }
    }

    public List<RankBinding> loadBindings() {
        List<RankBinding> list = new ArrayList<>();
        String sql = "SELECT * FROM rank_bindings";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    int npcId = rs.getInt("npc_id");
                    LeaderboardCategory category = LeaderboardCategory.valueOf(rs.getString("category"));
                    int rank = rs.getInt("rank");
                    list.add(new RankBinding(npcId, category, rank));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Bozuk hologram baglantisi atlandi: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Hologram baglantilari yuklenemedi: " + e.getMessage(), e);
        }
        return list;
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
