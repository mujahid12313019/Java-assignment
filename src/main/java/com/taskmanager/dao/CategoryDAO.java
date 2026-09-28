package com.taskmanager.dao;

import com.taskmanager.database.DatabaseConnection;
import com.taskmanager.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CategoryDAO
 * Data Access Object for category lookups in SQLite.
 */
public class CategoryDAO {

    private static final Logger LOGGER = Logger.getLogger(CategoryDAO.class.getName());
    private final DatabaseConnection databaseConnection;

    public CategoryDAO(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public CategoryDAO() {
        this(new DatabaseConnection());
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY id ASC;";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying categories", e);
        }
        return list;
    }

    public Optional<Category> getCategoryById(int id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?;";
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Category(rs.getInt("id"), rs.getString("name")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying category by id " + id, e);
        }
        return Optional.empty();
    }
}
