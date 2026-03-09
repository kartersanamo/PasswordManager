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

            // Create passwords table if it doesn't exist
            String createPasswordsTableSQL = """
                CREATE TABLE IF NOT EXISTS passwords (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    service TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    notes TEXT
                )
                """;

            // Create settings table to store secret code hash
            String createSettingsTableSQL = """
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                )
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createPasswordsTableSQL);
                stmt.execute(createSettingsTableSQL);
            }

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
    
    /**
     * Gets the stored secret code hash from the database.
     * @return the secret code hash, or null if not set
     */
    public String getSecretCodeHash() {
        String sql = "SELECT value FROM settings WHERE key = 'secret_code_hash'";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving secret code hash: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Stores the secret code hash in the database.
     *
     * @param hash the Argon2 hash of the secret code
     */
    public void setSecretCodeHash(String hash) {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES ('secret_code_hash', ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error storing secret code hash: " + e.getMessage());
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
                String encryptedPassword = rs.getString("password");
                String notes = rs.getString("notes");

                // Decrypt the password before adding to the list
                String decryptedPassword;
                try {
                    if (PasswordHasher.isEncrypted(encryptedPassword)) {
                        decryptedPassword = PasswordHasher.decryptPassword(encryptedPassword);
                    } else {
                        // Handle legacy plain-text passwords - encrypt them
                        decryptedPassword = encryptedPassword;
                        // Migrate to encrypted format
                        updatePassword(id, service, username, decryptedPassword, notes);
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting password for service: " + service);
                    System.err.println("Exception details: " + e.getClass().getName() + ": " + e.getMessage());
                    
                    // Try to handle as plain text (legacy format)
                    System.err.println("Attempting to treat as plain text password...");
                    decryptedPassword = encryptedPassword;
                    
                    // Try to re-encrypt it properly
                    try {
                        System.err.println("Attempting to migrate password to encrypted format...");
                        updatePassword(id, service, username, decryptedPassword, notes);
                        System.err.println("Migration successful for service: " + service);
                    } catch (Exception migrationError) {
                        System.err.println("Failed to migrate password: " + migrationError.getMessage());
                        decryptedPassword = "[DECRYPTION ERROR - Check console for details]";
                    }
                }

                passwords.add(new PasswordEntry(id, service, username, decryptedPassword, notes));
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
            // Encrypt the password before storing (using Argon2-derived key)
            String encryptedPassword = PasswordHasher.encryptPassword(password);
            pstmt.setString(3, encryptedPassword);
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
            // Encrypt the password before storing (only if it's not already encrypted)
            String passwordToStore = PasswordHasher.isEncrypted(password) ? password : PasswordHasher.encryptPassword(password);
            pstmt.setString(3, passwordToStore);
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

    public List<String> getUniqueUsernames() {
        List<String> usernames = new ArrayList<>();
        String sql = "SELECT DISTINCT username FROM passwords ORDER BY username";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String username = rs.getString("username");
                if (username != null && !username.trim().isEmpty()) {
                    usernames.add(username);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving usernames: " + e.getMessage());
        }

        return usernames;
    }

    /**
     * Scans stored password rows and identifies entries that look encrypted but
     * fail decryption with the current key.
     *
     * @param removeCorrupted if true, delete corrupted entries from the table
     * @return number of corrupted entries found
     */
    public int checkForCorruptedPasswords(boolean removeCorrupted) {
        String selectSql = "SELECT id, service, password FROM passwords";
        List<Integer> corruptedIds = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String service = rs.getString("service");
                String storedPassword = rs.getString("password");

                // Plain-text legacy values are not considered corrupted here.
                if (!PasswordHasher.isEncrypted(storedPassword)) {
                    continue;
                }

                try {
                    PasswordHasher.decryptPassword(storedPassword);
                } catch (Exception e) {
                    System.err.println("Corrupted password detected for service: " + service + " (id=" + id + ")");
                    corruptedIds.add(id);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error checking corrupted passwords: " + e.getMessage());
            return 0;
        }

        if (removeCorrupted && !corruptedIds.isEmpty()) {
            String deleteSql = "DELETE FROM passwords WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
                for (Integer id : corruptedIds) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Error removing corrupted passwords: " + e.getMessage());
            }
        }

        return corruptedIds.size();
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
    
    /**
     * Re-encrypts all passwords with a new secret code.
     * This is called when the user changes their secret code.
     * 
     * @param oldSecretCode the old secret code (for decryption)
     * @param newSecretCode the new secret code (for re-encryption)
     * @return true if successful, false otherwise
     */
    public boolean reEncryptAllPasswords(String oldSecretCode, String newSecretCode) {
        List<PasswordEntry> allPasswords = new ArrayList<>();
        String selectSql = "SELECT id, service, username, password, notes FROM passwords";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {
            
            // First, decrypt all passwords with old secret code
            PasswordHasher.setMasterPassword(oldSecretCode);
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String service = rs.getString("service");
                String username = rs.getString("username");
                String encryptedPassword = rs.getString("password");
                String notes = rs.getString("notes");
                
                try {
                    String decryptedPassword;
                    if (PasswordHasher.isEncrypted(encryptedPassword)) {
                        decryptedPassword = PasswordHasher.decryptPassword(encryptedPassword);
                    } else {
                        // Plain text password
                        decryptedPassword = encryptedPassword;
                    }
                    
                    allPasswords.add(new PasswordEntry(id, service, username, decryptedPassword, notes));
                } catch (Exception e) {
                    System.err.println("Failed to decrypt password for service: " + service);
                    return false;
                }
            }
            
            // Now re-encrypt with new secret code
            PasswordHasher.setMasterPassword(newSecretCode);
            
            String updateSql = "UPDATE passwords SET password = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                for (PasswordEntry entry : allPasswords) {
                    String reEncrypted = PasswordHasher.encryptPassword(entry.password());
                    pstmt.setString(1, reEncrypted);
                    pstmt.setInt(2, entry.id());
                    pstmt.executeUpdate();
                }
            }
            
            // Update the stored secret code hash
            String newHash = PasswordHasher.hashPassword(newSecretCode);
            setSecretCodeHash(newHash);
            
            System.out.println("Successfully re-encrypted " + allPasswords.size() + " passwords with new secret code");
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error re-encrypting passwords: " + e.getMessage());
            return false;
        }
    }

    public record PasswordEntry(int id, String service, String username, String password, String notes) {
    }
}
