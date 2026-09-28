package com.taskmanager.dao;

import com.taskmanager.database.DatabaseConnection;
import com.taskmanager.model.Task;
import com.taskmanager.model.TaskPriority;
import com.taskmanager.model.TaskStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TaskDAO
 * Data Access Object executing complete CRUD operations against SQLite using JDBC PreparedStatements.
 *
 * Demonstrates:
 * - CREATE: INSERT INTO tasks (...) VALUES (?, ?, ...)
 * - READ: SELECT ... FROM tasks JOIN users ... JOIN categories ...
 * - UPDATE: UPDATE tasks SET ... WHERE id = ?
 * - DELETE: DELETE FROM tasks WHERE id = ?
 * - Multi-table SQL JOIN queries demonstrating relational database relationships
 * - PreparedStatement parameterization to prevent SQL injection vulnerabilities
 */
public class TaskDAO {

    private static final Logger LOGGER = Logger.getLogger(TaskDAO.class.getName());
    private final DatabaseConnection databaseConnection;

    public TaskDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public TaskDAO() {
        this(new DatabaseConnection());
    }

    // ================= 1. CREATE =================
    /**
     * Inserts a new task into the SQLite database.
     * Uses PreparedStatement with RETURN_GENERATED_KEYS to obtain the database primary key.
     */
    public Task createTask(Task task, int userId, int categoryId) throws SQLException {
        String sql = """
            INSERT INTO tasks (user_id, category_id, title, description, priority, status, created_at, deadline, completed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, userId);
            if (categoryId > 0) {
                pstmt.setInt(2, categoryId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setString(3, task.getTitle());
            pstmt.setString(4, task.getDescription() != null ? task.getDescription() : "");
            pstmt.setString(5, task.getPriority().name());
            pstmt.setString(6, task.getStatus().name());
            pstmt.setString(7, task.getCreatedDateTime() != null ? task.getCreatedDateTime().toString() : LocalDateTime.now().toString());
            pstmt.setString(8, task.getDeadline() != null ? task.getDeadline().toString() : null);
            pstmt.setInt(9, task.isCompleted() ? 1 : 0);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating task failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    task.setId(String.valueOf(generatedId));
                }
            }

            task.setUserId(userId);
            task.setCategoryId(categoryId);
            LOGGER.fine("SQL CREATE: Inserted task with database ID " + task.getId());
            return task;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during createTask", e);
            throw e;
        }
    }

    public Task createTask(Task task) throws SQLException {
        int uId = task.getUserId() > 0 ? task.getUserId() : 1;
        int cId = (task.getCategoryId() != null && task.getCategoryId() > 0) ? task.getCategoryId() : 1;
        return createTask(task, uId, cId);
    }

    // ================= 2. READ =================
    /**
     * Retrieves all tasks using a multi-table SQL JOIN:
     * tasks JOIN users ON tasks.user_id = users.id
     * LEFT JOIN categories ON tasks.category_id = categories.id
     */
    public List<Task> getAllTasks() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = """
            SELECT 
                t.id, t.user_id, t.category_id, t.title, t.description,
                t.priority, t.status, t.created_at, t.deadline, t.completed,
                u.name AS user_name, u.email AS user_email,
                c.name AS category_name
            FROM tasks t
            JOIN users u ON t.user_id = u.id
            LEFT JOIN categories c ON t.category_id = c.id
            ORDER BY t.id DESC;
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Task task = mapResultSetToTask(rs);
                tasks.add(task);
            }
            LOGGER.fine("SQL READ: Retrieved " + tasks.size() + " tasks from SQLite.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during getAllTasks", e);
            throw e;
        }
        return tasks;
    }

    /**
     * Reads a single task by ID.
     */
    public Optional<Task> getTaskById(int id) throws SQLException {
        String sql = """
            SELECT 
                t.id, t.user_id, t.category_id, t.title, t.description,
                t.priority, t.status, t.created_at, t.deadline, t.completed,
                u.name AS user_name, u.email AS user_email,
                c.name AS category_name
            FROM tasks t
            JOIN users u ON t.user_id = u.id
            LEFT JOIN categories c ON t.category_id = c.id
            WHERE t.id = ?;
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during getTaskById " + id, e);
            throw e;
        }
        return Optional.empty();
    }

    // ================= 3. UPDATE =================
    /**
     * Updates an existing task record in SQLite.
     */
    public boolean updateTask(Task task) throws SQLException {
        String sql = """
            UPDATE tasks
            SET title = ?, description = ?, priority = ?, status = ?, deadline = ?, completed = ?, category_id = ?
            WHERE id = ?;
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription() != null ? task.getDescription() : "");
            pstmt.setString(3, task.getPriority().name());
            pstmt.setString(4, task.getStatus().name());
            pstmt.setString(5, task.getDeadline() != null ? task.getDeadline().toString() : null);
            pstmt.setInt(6, task.isCompleted() ? 1 : 0);
            if (task.getCategoryId() != null && task.getCategoryId() > 0) {
                pstmt.setInt(7, task.getCategoryId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setInt(8, task.getIntId());

            int updated = pstmt.executeUpdate();
            LOGGER.fine("SQL UPDATE: Updated task id=" + task.getIntId() + " (rows affected: " + updated + ")");
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during updateTask for id " + task.getId(), e);
            throw e;
        }
    }

    /**
     * Shortcut method to mark a task as completed directly in the database.
     */
    public boolean markTaskCompleted(int id) throws SQLException {
        String sql = "UPDATE tasks SET completed = 1, status = 'COMPLETED' WHERE id = ?;";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int updated = pstmt.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during markTaskCompleted for id " + id, e);
            throw e;
        }
    }

    // ================= 4. DELETE =================
    /**
     * Deletes a task record from SQLite by ID.
     */
    public boolean deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?;";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int deleted = pstmt.executeUpdate();
            LOGGER.fine("SQL DELETE: Deleted task id=" + id + " (rows affected: " + deleted + ")");
            return deleted > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SQL Error during deleteTask for id " + id, e);
            throw e;
        }
    }

    // ================= Helper Mapper =================
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        TaskPriority priority = TaskPriority.fromString(rs.getString("priority"));
        TaskStatus status = TaskStatus.fromString(rs.getString("status"));

        String createdAtStr = rs.getString("created_at");
        LocalDateTime createdAt = null;
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                createdAt = LocalDateTime.parse(createdAtStr);
            } catch (Exception ignored) {
                createdAt = LocalDateTime.now();
            }
        }

        String deadlineStr = rs.getString("deadline");
        LocalDate deadline = null;
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                deadline = LocalDate.parse(deadlineStr);
            } catch (Exception ignored) {}
        }

        boolean completed = rs.getInt("completed") == 1;

        Task task = new Task(String.valueOf(id), title, description, priority, status, createdAt, deadline, completed);
        task.setUserId(rs.getInt("user_id"));
        task.setCategoryId(rs.getInt("category_id"));
        task.setUserName(rs.getString("user_name"));
        task.setCategoryName(rs.getString("category_name"));
        return task;
    }
}
