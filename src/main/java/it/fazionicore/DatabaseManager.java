package it.fazionicore;

import java.io.File;
import java.sql.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final FazioniCore plugin;
    private Connection connection;
    private final boolean mariadbEnabled;

    public DatabaseManager(FazioniCore plugin) {
        this.plugin = plugin;
        this.mariadbEnabled = plugin.getConfigManager().isDatabaseEnabled();
    }

    public void initialize() {
        if (!mariadbEnabled) {
            ensureDataDirs();
            return;
        }
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            ConfigManager cfg = plugin.getConfigManager();
            String url = "jdbc:mariadb://" + cfg.getDbHost() + ":" + cfg.getDbPort()
                    + "/" + cfg.getDbName()
                    + "?useSSL=false&autoReconnect=true&characterEncoding=UTF-8";
            connection = DriverManager.getConnection(url, cfg.getDbUser(), cfg.getDbPass());
            createTables();
            plugin.getLogger().info("Connessione MariaDB stabilita.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Errore connessione MariaDB, uso YAML.", e);
            connection = null;
        }
        ensureDataDirs();
    }

    private void createTables() {
        if (connection == null) return;
        String[] tables = {
            """
            CREATE TABLE IF NOT EXISTS raid_logs (
                id INT AUTO_INCREMENT PRIMARY KEY,
                faction_name VARCHAR(64) NOT NULL,
                player_name VARCHAR(64) NOT NULL,
                player_uuid VARCHAR(36) NOT NULL,
                block_type VARCHAR(128) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x INT NOT NULL, y INT NOT NULL, z INT NOT NULL,
                action VARCHAR(32) NOT NULL,
                timestamp BIGINT NOT NULL,
                INDEX idx_faction (faction_name),
                INDEX idx_timestamp (timestamp)
            )
            """
        };
        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Errore creazione tabelle MariaDB.", e);
        }
    }

    public void saveRaidLog(it.fazionicore.model.RaidLog log) {
        if (connection == null) {
            saveRaidLogYaml(log);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO raid_logs (faction_name,player_name,player_uuid,block_type,world,x,y,z,action,timestamp) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, log.getFactionName());
                ps.setString(2, log.getPlayerName());
                ps.setString(3, log.getPlayerUUID());
                ps.setString(4, log.getBlockType());
                ps.setString(5, log.getWorld());
                ps.setInt(6, log.getX());
                ps.setInt(7, log.getY());
                ps.setInt(8, log.getZ());
                ps.setString(9, log.getAction());
                ps.setLong(10, log.getTimestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Errore salvataggio raid log MariaDB.", e);
            }
        });
    }

    private void saveRaidLogYaml(it.fazionicore.model.RaidLog log) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File logFile = new File(plugin.getDataFolder(), "data/raidlogs/" + log.getFactionName() + ".log");
                logFile.getParentFile().mkdirs();
                try (java.io.FileWriter fw = new java.io.FileWriter(logFile, true)) {
                    fw.write(log.toLogLine() + "\n");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Errore salvataggio raid log YAML.", e);
            }
        });
    }

    private void ensureDataDirs() {
        new File(plugin.getDataFolder(), "data/factions").mkdirs();
        new File(plugin.getDataFolder(), "data/claims").mkdirs();
        new File(plugin.getDataFolder(), "data/wars").mkdirs();
        new File(plugin.getDataFolder(), "data/raidlogs").mkdirs();
    }

    public boolean isMariaDB() { return connection != null; }

    public void close() {
        if (connection != null) {
            try { connection.close(); } catch (SQLException ignored) {}
        }
    }
}
