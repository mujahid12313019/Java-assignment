package com.taskmanager.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseConnection
 * Manages SQLite JDBC database connections.
 *
 * Demonstrates:
 * - JDBC Driver registration & connection management
 * - Enabling SQLite Foreign Key constraints (PRAGMA foreign_keys = ON)
 * - Safe directory creation and exception handling
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String DEFAULT_DB_PATH = "data/taskmanager.db";
    private final String dbUrl;

    public DatabaseConnection(String dbPath) {
        ensureDatabaseDirectory(dbPath);
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    public DatabaseConnection() {
        this(DEFAULT_DB_PATH);
    }

    private void ensureDatabaseDirectory(String dbPath) {
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                LOGGER.info("Created database storage directory: " + parentDir.getAbsolutePath());
            }
        }
    }

    /**
     * Obtains a new SQLite connection with foreign keys enabled.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        // SQLite foreign keys are disabled by default; strictly enforce them
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
