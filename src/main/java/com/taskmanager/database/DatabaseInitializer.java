package com.taskmanager.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DatabaseInitializer
 * Responsible for creating schema tables and seeding initial demonstration data.
 *
 * Demonstrates:
 * - Relational DDL (Data Definition Language) execution
 * - Primary Key and Foreign Key constraint setup
 * - Seeding relational records (Users -> Tasks -> Categories)
 */
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    private final DatabaseConnection databaseConnection;

    public DatabaseInitializer(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public DatabaseInitializer() {
        this(new DatabaseConnection());
    }

    /**
     * Initializes all relational tables and default seed rows.
     */
    public void initialize() {
        try (Connection conn = databaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL
                );
            """);

            // 2. Create Categories Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);

            // 3. Create Tasks Table with Foreign Keys
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER,
                    title TEXT NOT NULL,
                    description TEXT,
                    priority TEXT NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    deadline TEXT,
                    completed INTEGER DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
                );
            """);

            LOGGER.info("SQLite schema initialized successfully (users, categories, tasks).");

            // 4. Seed Default User if not exists
            stmt.execute("""
                INSERT OR IGNORE INTO users (id, name, email)
                VALUES (1, 'Default Student', 'student@university.edu');
            """);

            // 5. Seed Default Categories if not exist
            stmt.execute("""
                INSERT OR IGNORE INTO categories (id, name) VALUES
                (1, 'Academic / Coursework'),
                (2, 'Lab & Programming'),
                (3, 'Personal / Admin');
            """);

            // 6. Check if tasks are empty, seed initial demonstration tasks
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM tasks;")) {
                if (rs.next() && rs.getInt("total") == 0) {
                    seedInitialTasks(conn);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SQLite database tables", e);
            throw new RuntimeException("Database initialization failure", e);
        }
    }

    private void seedInitialTasks(Connection conn) throws SQLException {
        LOGGER.info("Seeding initial relational tasks into SQLite database...");
        try (Statement stmt = conn.createStatement()) {
            String today = LocalDate.now().toString();
            String tomorrow = LocalDate.now().plusDays(2).toString();
            String yesterday = LocalDate.now().minusDays(2).toString();
            String nextWeek = LocalDate.now().plusDays(7).toString();
            String now = LocalDateTime.now().toString();

            stmt.execute(String.format("""
                INSERT INTO tasks (user_id, category_id, title, description, priority, status, created_at, deadline, completed)
                VALUES 
                (1, 1, 'Prepare Java Concurrency Slides', 'Create lecture slides explaining race conditions, synchronization, and ScheduledExecutorService.', 'HIGH', 'IN_PROGRESS', '%s', '%s', 0),
                (1, 2, 'Implement SQLite JDBC Layer', 'Build DatabaseConnection, TaskDAO, and write prepared statements with foreign keys.', 'HIGH', 'COMPLETED', '%s', '%s', 1),
                (1, 1, 'Review Operating Systems Chapter 4', 'Read sections on process scheduling, context switching, and POSIX threads.', 'MEDIUM', 'PENDING', '%s', '%s', 0),
                (1, 2, 'Fix Memory Leak in C Daemon', 'Use Valgrind to trace unclosed file descriptors in socket listener.', 'LOW', 'PENDING', '%s', '%s', 0),
                (1, 3, 'Set Up Git Branch Protection', 'Configure branch protection rules requiring pull request reviews before merge.', 'MEDIUM', 'COMPLETED', '%s', '%s', 1);
            """, now, tomorrow, now, yesterday, now, nextWeek, now, yesterday, now, yesterday));

            LOGGER.info("Initial tasks successfully seeded into SQLite.");
        }
    }
}
