package com.kartersanamo.passwordManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PasswordDatabase {
    private static final String DB_URL = "jdbc:sqlite:passwords.db";
    private Connection connection;

    public PasswordDatabase() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Create connection
            connection = DriverManager.getConnection(DB_URL);

            // Create a table if it doesn't exist
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS passwords (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    service TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    notes TEXT
                )
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
            }

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> passwords = new ArrayList<>();
        String sql = "SELECT * FROM passwords ORDER BY service";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String service = rs.getString("service");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String notes = rs.getString("notes");

                passwords.add(new PasswordEntry(id, service, username, password, notes));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving passwords: " + e.getMessage());
        }

        return passwords;
    }

    public boolean addPassword(String service, String username, String password, String notes) {
        String sql = "INSERT INTO passwords (service, username, password, notes) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, notes);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error adding password: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(int id, String service, String username, String password, String notes) {
        String sql = "UPDATE passwords SET service = ?, username = ?, password = ?, notes = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, notes);
            pstmt.setInt(5, id);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePassword(int id) {
        String sql = "DELETE FROM passwords WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error deleting password: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    public static class PasswordEntry {
        private final int id;
        private String service;
        private String username;
        private String password;
        private String notes;

        public PasswordEntry(int id, String service, String username, String password, String notes) {
            this.id = id;
            this.service = service;
            this.username = username;
            this.password = password;
            this.notes = notes;
        }

        public int getId() { return id; }

        public String getService() { return service; }
        public void setService(String service) { this.service = service; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}

