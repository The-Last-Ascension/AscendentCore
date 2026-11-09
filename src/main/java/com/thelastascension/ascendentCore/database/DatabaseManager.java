package com.thelastascension.ascendentCore.database;

import com.thelastascension.ascendentCore.AscendentCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Gestor de Base de Datos de alto rendimiento (SQLite en modo WAL por defecto + soporte MariaDB/MySQL).
 * Diseñado para migrar de Windows a Linux sin requerir configuración externa.
 */
public class DatabaseManager {

    private final AscendentCore core;
    private HikariDataSource dataSource;
    private final ExecutorService dbExecutor;
    private boolean mysqlMode = false;

    public DatabaseManager(AscendentCore core) {
        this.core = core;
        this.dbExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void initialize() {
        String dbType = core.getConfig().getString("database.type", "SQLITE").toUpperCase();
        HikariConfig config = new HikariConfig();
        config.setPoolName("AscendentCore-DBPool");

        if ("MYSQL".equals(dbType) || "MARIADB".equals(dbType)) {
            this.mysqlMode = true;
            String host = core.getConfig().getString("database.mysql.host", "127.0.0.1");
            int port = core.getConfig().getInt("database.mysql.port", 3306);
            String database = core.getConfig().getString("database.mysql.database", "the_last_ascension");
            String user = core.getConfig().getString("database.mysql.username", "ascendent");
            String pass = core.getConfig().getString("database.mysql.password", "");
            int poolSize = core.getConfig().getInt("database.mysql.pool-size", 10);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(poolSize);
        } else {
            this.mysqlMode = false;
            String relativePath = core.getConfig().getString("database.sqlite-file", "data/ascendent.db");
            File dbFile = new File(core.getDataFolder(), relativePath);
            if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setMaximumPoolSize(1); // SQLite en modo WAL rinde al máximo con 1 conexión serializada sin bloqueos de archivo
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA foreign_keys=ON;");
        }

        config.setConnectionTimeout(10000);
        this.dataSource = new HikariDataSource(config);
        createCoreTables();
    }

    private void createCoreTables() {
        String createProfilesSql = """
                CREATE TABLE IF NOT EXISTS ascendent_profiles (
                    uuid VARCHAR(36) PRIMARY KEY,
                    player_name VARCHAR(32) NOT NULL,
                    language VARCHAR(10) NOT NULL DEFAULT 'es_es',
                    global_level INT NOT NULL DEFAULT 1,
                    global_exp DOUBLE NOT NULL DEFAULT 0.0,
                    free_stat_points INT NOT NULL DEFAULT 0,
                    coins BIGINT NOT NULL DEFAULT 100,
                    unlocked_talisman_slots INT NOT NULL DEFAULT 1,
                    active_class_id VARCHAR(64) NOT NULL DEFAULT 'none',
                    invested_stats_json TEXT NOT NULL DEFAULT '{}'
                );
                """;
        executeSchema(createProfilesSql);
    }

    /**
     * Permite que cualquier plugin modular (AscendentCombat, AscendentClasses, AscendentHousing, etc.)
     * cree sus propias tablas en la base de datos unificada al iniciar.
     */
    public void executeSchema(String sql) {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            core.getLogger().log(Level.SEVERE, "Error al inicializar tabla SQL en AscendentCore", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task, dbExecutor);
    }

    public boolean isMysqlMode() {
        return mysqlMode;
    }

    public void shutdown() {
        dbExecutor.shutdown();
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
