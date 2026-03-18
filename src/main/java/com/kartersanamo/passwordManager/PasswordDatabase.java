package com.kartersanamo.passwordManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary data store — MySQL (private.passwords), either direct or via Cloudflare TCP proxy.
 *
 * Prerequisites before launching the app:
 *   cloudflared access tcp --hostname <cloudflare-tcp-hostname> --url localhost:13306
 *
 * Required encryption key source (64 hex-char / 32-byte AES-256 key):
 *   ENCRYPTION_KEY env var, JVM property, encryption.key file, or user-home key file.
 *
 * Column mapping  (MySQL → internal API):
 *   service_name       → service
 *   encrypted_password → password  (decrypted transparently on read)
 *   notes_encrypted    → notes     (decrypted transparently on read; NULL-safe)
 */
public class PasswordDatabase {

    // ── MySQL connection defaults (overridable via JVM props/env vars) ───────
    private static final String DEFAULT_DB_HOST = "sql.kartersanamo.com";
    private static final String DEFAULT_DB_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "private";
    private static final String DEFAULT_DB_USER = "sqladmin";
    private static final String DEFAULT_DB_PASSWORD = null;
    private static final String DEFAULT_DB_MODE = "direct";
    private static final int CONNECT_MAX_ATTEMPTS = 20;
    private static final int CONNECT_RETRY_SLEEP_MS = 500;

    private final String mysqlUrl;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final boolean shouldAutoStartCloudflareProxy;
    private final String connectionMode;

    private Connection connection;
    private String lastConnectionError;
    private String lastProxyStartupStatus = "Not attempted.";
    private boolean encryptionKeyLoaded;
    private String encryptionKeyStatusMessage;

    public PasswordDatabase() {
        DbConnectionConfig config = resolveDbConnectionConfig();
        this.mysqlUrl = config.url();
        this.mysqlUser = config.user();
        this.mysqlPassword = config.password();
        this.shouldAutoStartCloudflareProxy = config.autoStartCloudflareProxy();
        this.connectionMode = config.mode();

        loadEncryptionKey();
        initializeDatabase();
    }

    // ── Key loading ───────────────────────────────────────────────────────────

    private void loadEncryptionKey() {
        EncryptionKeyResolver.KeyResolutionResult result = EncryptionKeyResolver.resolve();
        if (!result.found()) {
            encryptionKeyLoaded = false;
            encryptionKeyStatusMessage = result.message();
            System.err.println(
                "============================================================\n" +
                " ERROR: No usable encryption key was found.\n" +
                " The app cannot decrypt passwords without the external AES key.\n\n" +
                " Supported sources:\n" +
                "   - ENCRYPTION_KEY environment variable\n" +
                "   - -DENCRYPTION_KEY=<64-hex-chars> JVM property\n" +
                "   - encryption.key file next to the jar\n" +
                "   - ~/.password-manager/encryption.key\n\n" +
                " Details: " + result.message() + "\n" +
                "============================================================");
            return;
        }

        try {
            PasswordHasher.setRawKey(result.rawKey());
            encryptionKeyLoaded = true;
            encryptionKeyStatusMessage = "Loaded from " + result.sourceDescription();
        } catch (Exception e) {
            encryptionKeyLoaded = false;
            encryptionKeyStatusMessage = "Invalid encryption key: " + e.getMessage();
            System.err.println("ERROR: " + encryptionKeyStatusMessage);
        }
    }

    // ── Connection & schema ───────────────────────────────────────────────────

    private void initializeDatabase() {
        try {
            // Explicitly register the MySQL driver — required when the driver jar
            // is on the classpath but the ServiceLoader hasn't picked it up yet
            // (e.g. first run after adding the dependency in pom.xml).
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println(
                "============================================================\n" +
                " ERROR: MySQL JDBC driver not found on classpath.\n\n" +
                " Fix: In IntelliJ go to:\n" +
                "   View → Tool Windows → Maven → (Reload All Maven Projects)\n" +
                " Then rebuild: Build → Rebuild Project\n" +
                "============================================================");
            return;
        }

        if (mysqlUser == null || mysqlUser.isBlank() || mysqlPassword == null || mysqlPassword.isBlank()) {
            lastConnectionError = "Database credentials are missing. Set PASSWORD_MANAGER_DB_USER and PASSWORD_MANAGER_DB_PASSWORD " +
                "(or -DpasswordManager.db.user / -DpasswordManager.db.password).";
            System.err.println("ERROR: " + lastConnectionError);
            return;
        }

        // Register cleanup on app shutdown (SSH tunnel + database)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing database and SSH tunnel...");
            close();
            SSHTunnelManager.stopTunnel();
        }));

        // Auto-start SSH tunnel for direct mode targeting localhost:13306
        // (typical SSH port forwarding setup)
        if ("direct".equals(connectionMode)) {
            // Extract host/port from the URL to check if we need SSH tunnel
            String jdbcHost = mysqlUrl.replaceAll(".*://([^:]+).*", "$1");
            String jdbcPort = mysqlUrl.replaceAll(".*:([0-9]+)/.*", "$1");
            
            if ("127.0.0.1".equals(jdbcHost) && "13306".equals(jdbcPort)) {
                int tunnelPort = SSHTunnelManager.startTunnel("HomeServer", "127.0.0.1", 3306, 13306);
                if (tunnelPort < 0) {
                    lastConnectionError = "Failed to establish SSH tunnel. Ensure ~/.ssh/config has 'Host HomeServer' entry.";
                    System.err.println("ERROR: " + lastConnectionError);
                    return;
                }
                System.out.println("SSH tunnel established on localhost:" + tunnelPort);
            }
        }

        // Keep Cloudflare auto-start only for proxy/local-tunnel style connections.
        if (shouldAutoStartCloudflareProxy) {
            CloudflareTcpProxyManager.ProxyStartResult proxyResult =
                CloudflareTcpProxyManager.ensureProxyRunning();
            lastProxyStartupStatus = proxyResult.message();
            if (!proxyResult.ready()) {
                System.err.println("Cloudflare proxy auto-start issue (PasswordDatabase): " + proxyResult.message());
            }
        } else {
            lastProxyStartupStatus = "Not used in direct mode.";
        }

        try {
            connection = connectWithRetry();
            lastConnectionError = null;
            System.out.println("Connected to MySQL using URL: " + mysqlUrl);

            // Ensure passwords table exists (matches migration schema)
            String createPasswordsSQL = """
                CREATE TABLE IF NOT EXISTS passwords (
                    id                 BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
                    service_name       VARCHAR(255)      NOT NULL,
                    username           VARCHAR(255)      NOT NULL,
                    encrypted_password TEXT              NOT NULL,
                    url                VARCHAR(1024)     NULL,
                    notes_encrypted    TEXT              NULL,
                    created_at         TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at         TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                                  ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uq_service_user (service_name, username)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

            // Settings table for secret code hash
            String createSettingsSQL = """
                CREATE TABLE IF NOT EXISTS settings (
                    `key`   VARCHAR(255) NOT NULL PRIMARY KEY,
                    value   TEXT         NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createPasswordsSQL);
                stmt.execute(createSettingsSQL);
            }

        } catch (SQLException e) {
            lastConnectionError = formatSqlException(e);
            String authHint = isAuthenticationFailure(e)
                ? "\n Credentials rejected. Configure one of:\n" +
                  "   -DpasswordManager.db.user / -DpasswordManager.db.password\n" +
                  "   or env PASSWORD_MANAGER_DB_USER / PASSWORD_MANAGER_DB_PASSWORD\n"
                : "";
            String modeHint;
            if (shouldAutoStartCloudflareProxy) {
                String cloudflareHostname = CloudflareTcpProxyManager.getEffectiveHostname();
                String proxyDiagnostic = CloudflareTcpProxyManager.getLastRuntimeDiagnostic();
                modeHint =
                    " Make sure the Cloudflare proxy is running:\n" +
                    "   cloudflared access tcp --hostname " + cloudflareHostname + " --url localhost:13306\n" +
                    " Latest cloudflared output: " +
                    ((proxyDiagnostic == null || proxyDiagnostic.isBlank()) ? "(none captured)" : proxyDiagnostic) + "\n";
            } else {
                modeHint =
                    " Verify remote MySQL access to " + mysqlUrl + " from this laptop and allow your public IP on the server firewall.\n";
            }
            System.err.println(
                "============================================================\n" +
                " ERROR: Cannot connect to MySQL: " + lastConnectionError + "\n\n" +
                authHint +
                modeHint +
                "============================================================");
        }
    }

    private DbConnectionConfig resolveDbConnectionConfig() {
        String explicitUrl = firstNonBlank(
            System.getProperty("passwordManager.db.url"),
            System.getenv("PASSWORD_MANAGER_DB_URL")
        );

        String configuredMode = firstNonBlank(
            System.getProperty("passwordManager.db.mode"),
            System.getenv("PASSWORD_MANAGER_DB_MODE"),
            DEFAULT_DB_MODE
        );
        String normalizedMode = configuredMode == null ? DEFAULT_DB_MODE : configuredMode.trim().toLowerCase();
        boolean cloudflareMode = normalizedMode.equals("cloudflare") || normalizedMode.equals("proxy") || normalizedMode.equals("tunnel");

        String defaultHost = cloudflareMode ? "127.0.0.1" : DEFAULT_DB_HOST;
        String defaultPort = cloudflareMode ? "13306" : DEFAULT_DB_PORT;

        String host = firstNonBlank(
            System.getProperty("passwordManager.db.host"),
            System.getenv("PASSWORD_MANAGER_DB_HOST"),
            defaultHost
        );
        String port = firstNonBlank(
            System.getProperty("passwordManager.db.port"),
            System.getenv("PASSWORD_MANAGER_DB_PORT"),
            defaultPort
        );
        String dbName = firstNonBlank(
            System.getProperty("passwordManager.db.name"),
            System.getenv("PASSWORD_MANAGER_DB_NAME"),
            DEFAULT_DB_NAME
        );
        String user = firstNonBlank(
            System.getProperty("passwordManager.db.user"),
            System.getenv("PASSWORD_MANAGER_DB_USER"),
            System.getenv("MYSQL_USER"),
            DEFAULT_DB_USER
        );
        String password = firstNonBlank(
            System.getProperty("passwordManager.db.password"),
            System.getenv("PASSWORD_MANAGER_DB_PASSWORD"),
            System.getenv("MYSQL_PASSWORD"),
            DEFAULT_DB_PASSWORD
        );

        boolean explicitLocalProxyUrl = explicitUrl != null &&
            (explicitUrl.contains("localhost:13306") || explicitUrl.contains("127.0.0.1:13306"));
        boolean autoStartCloudflareProxy = cloudflareMode || explicitLocalProxyUrl || ("13306".equals(port) && isLoopbackHost(host));

        String sslMode = firstNonBlank(
            System.getProperty("passwordManager.db.sslMode"),
            System.getenv("PASSWORD_MANAGER_DB_SSL_MODE"),
            autoStartCloudflareProxy ? "DISABLED" : "PREFERRED"
        );
        String allowPublicKeyRetrieval = firstNonBlank(
            System.getProperty("passwordManager.db.allowPublicKeyRetrieval"),
            System.getenv("PASSWORD_MANAGER_DB_ALLOW_PUBLIC_KEY_RETRIEVAL"),
            autoStartCloudflareProxy ? "true" : "false"
        );

        String url = explicitUrl != null ? explicitUrl :
            "jdbc:mysql://" + host + ":" + port + "/" + dbName +
            "?sslMode=" + sslMode +
            "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval +
            "&serverTimezone=UTC" +
            "&characterEncoding=UTF-8&connectTimeout=3000&socketTimeout=10000";

        return new DbConnectionConfig(url, user, password, autoStartCloudflareProxy, normalizedMode);
    }

    private boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.trim().toLowerCase();
        return "127.0.0.1".equals(normalized) || "localhost".equals(normalized) || "::1".equals(normalized);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private Connection connectWithRetry() throws SQLException {
        SQLException last = null;
        boolean restartedProxyAfterCommFailure = false;

        for (int attempt = 1; attempt <= CONNECT_MAX_ATTEMPTS; attempt++) {
            try {
                return DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
            } catch (SQLException e) {
                last = e;

                if (shouldAutoStartCloudflareProxy && !restartedProxyAfterCommFailure && isCommunicationLinkFailure(e)) {
                    CloudflareTcpProxyManager.ProxyStartResult restartResult =
                        CloudflareTcpProxyManager.ensureProxyRunning(true);
                    lastProxyStartupStatus = restartResult.message();
                    System.err.println("MySQL communication failure detected; proxy restart attempt: " + restartResult.message());

                    if (!restartResult.ready()) {
                        SQLException failFast = new SQLException(
                            "Cloudflare proxy restart was not ready: " + restartResult.message(),
                            "08001"
                        );
                        failFast.addSuppressed(e);
                        throw failFast;
                    }

                    restartedProxyAfterCommFailure = true;
                }

                if (shouldAutoStartCloudflareProxy && isCommunicationLinkFailure(e)) {
                    String proxyDiagnostic = CloudflareTcpProxyManager.getLastRuntimeDiagnostic();
                    if (isActionableProxyDiagnostic(proxyDiagnostic)) {
                        SQLException failFast = new SQLException(
                            "Cloudflare proxy diagnostic: " + proxyDiagnostic,
                            "08001"
                        );
                        failFast.addSuppressed(e);
                        throw failFast;
                    }
                }

                if (attempt == CONNECT_MAX_ATTEMPTS) {
                    break;
                }

                try {
                    Thread.sleep(CONNECT_RETRY_SLEEP_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    SQLException wrapped = new SQLException("Interrupted while waiting to retry MySQL connection", interrupted);
                    wrapped.addSuppressed(e);
                    throw wrapped;
                }
            }
        }

        throw last == null ? new SQLException("Unknown MySQL connection error") : last;
    }

    private boolean isCommunicationLinkFailure(SQLException e) {
        if (e == null) {
            return false;
        }

        String sqlState = e.getSQLState();
        if (sqlState != null && sqlState.startsWith("08")) {
            return true;
        }

        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("communications link failure")
            || lower.contains("connection refused")
            || lower.contains("connect timed out")
            || lower.contains("connection reset");
    }

    private boolean isActionableProxyDiagnostic(String proxyDiagnostic) {
        if (proxyDiagnostic == null || proxyDiagnostic.isBlank()) {
            return false;
        }
        String lower = proxyDiagnostic.toLowerCase();
        return lower.contains("failed to connect to origin")
            || lower.contains("no route to host")
            || lower.contains("bad handshake")
            || lower.contains("access login")
            || lower.contains("token");
    }

    private String formatSqlException(SQLException e) {
        if (e == null) {
            return "Unknown SQL error";
        }
        String sqlState = e.getSQLState() == null ? "n/a" : e.getSQLState();
        return e.getMessage() + " (SQLState=" + sqlState + ", ErrorCode=" + e.getErrorCode() + ")";
    }

    private boolean isAuthenticationFailure(SQLException e) {
        if (e == null) {
            return false;
        }
        String sqlState = e.getSQLState();
        return "28000".equals(sqlState) || e.getErrorCode() == 1045;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public String getLastConnectionError() {
        return lastConnectionError;
    }

    public String getLastProxyStartupStatus() {
        return lastProxyStartupStatus;
    }

    public boolean isCloudflareProxyMode() {
        return shouldAutoStartCloudflareProxy;
    }

    public String getConnectionMode() {
        return connectionMode;
    }

    public boolean isEncryptionKeyLoaded() {
        return encryptionKeyLoaded;
    }

    public String getEncryptionKeyStatusMessage() {
        return encryptionKeyStatusMessage;
    }

    // ── Settings (secret code hash) ───────────────────────────────────────────

    public String getSecretCodeHash() {
        if (connection == null) return null;
        String sql = "SELECT value FROM settings WHERE `key` = 'secret_code_hash'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            System.err.println("Error retrieving secret code hash: " + e.getMessage());
        }
        return null;
    }

    public void setSecretCodeHash(String hash) {
        if (connection == null) return;
        String sql = "INSERT INTO settings (`key`, value) VALUES ('secret_code_hash', ?) " +
                     "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error storing secret code hash: " + e.getMessage());
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<PasswordEntry> getAllPasswords() {
        List<PasswordEntry> passwords = new ArrayList<>();
        if (connection == null) return passwords;

        // Order by service_name to match original SQLite behaviour
        String sql = "SELECT id, service_name, username, encrypted_password, url, notes_encrypted " +
                     "FROM passwords ORDER BY service_name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int    id      = rs.getInt("id");
                String service = rs.getString("service_name");
                String user    = rs.getString("username");
                String encPw   = rs.getString("encrypted_password");
                String url     = rs.getString("url");
                String encNote = rs.getString("notes_encrypted");

                if (!encryptionKeyLoaded) {
                    passwords.add(new PasswordEntry(
                        id,
                        service,
                        user,
                        "[ENCRYPTION KEY REQUIRED]",
                        url,
                        encNote == null || encNote.isBlank() ? null : "[ENCRYPTION KEY REQUIRED]"
                    ));
                    continue;
                }

                // Decrypt password
                String plainPw;
                try {
                    plainPw = PasswordHasher.isEncrypted(encPw)
                            ? PasswordHasher.decryptPassword(encPw)
                            : encPw;   // legacy plain-text guard
                } catch (Exception e) {
                    System.err.println("Decryption error for service '" + service + "': " + e.getMessage());
                    plainPw = "[DECRYPTION ERROR]";
                }

                // Decrypt notes (nullable)
                String plainNote = null;
                if (encNote != null && !encNote.isBlank()) {
                    try {
                        plainNote = PasswordHasher.isEncrypted(encNote)
                                ? PasswordHasher.decryptPassword(encNote)
                                : encNote;
                    } catch (Exception e) {
                        System.err.println("Note decryption error for service '" + service + "': " + e.getMessage());
                        plainNote = "[NOTE DECRYPTION ERROR]";
                    }
                }

                passwords.add(new PasswordEntry(id, service, user, plainPw, url, plainNote));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving passwords: " + e.getMessage());
        }
        return passwords;
    }

    public boolean addPassword(String service, String username, String password, String url, String notes) {
        if (connection == null) return false;
        if (!encryptionKeyLoaded) return false;
        String sql = "INSERT INTO passwords (service_name, username, encrypted_password, url, notes_encrypted) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service);
            pstmt.setString(2, username);
            pstmt.setString(3, PasswordHasher.encryptPassword(password));
            pstmt.setString(4, (url != null && !url.isBlank()) ? url : null);
            pstmt.setString(5, (notes != null && !notes.isBlank())
                    ? PasswordHasher.encryptPassword(notes) : null);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding password: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePassword(int id, String service, String username, String password, String url, String notes) {
        if (connection == null) return false;
        if (!encryptionKeyLoaded) return false;
        String sql = "UPDATE passwords SET service_name = ?, username = ?, " +
                     "encrypted_password = ?, url = ?, notes_encrypted = ? " +
                     "WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, service);
            pstmt.setString(2, username);
            // Always encrypt user input from the edit form; heuristic detection can misclassify Base64-looking plaintext.
            pstmt.setString(3, PasswordHasher.encryptPassword(password));
            pstmt.setString(4, (url != null && !url.isBlank()) ? url : null);
            pstmt.setString(5, (notes != null && !notes.isBlank())
                    ? PasswordHasher.encryptPassword(notes) : null);
            pstmt.setInt(6, id);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePassword(int id) {
        if (connection == null) return false;
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
        if (connection == null) return usernames;
        String sql = "SELECT DISTINCT username FROM passwords ORDER BY username";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String u = rs.getString("username");
                if (u != null && !u.isBlank()) usernames.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving usernames: " + e.getMessage());
        }
        return usernames;
    }

    // ── Integrity check ───────────────────────────────────────────────────────

    public int checkForCorruptedPasswords(boolean removeCorrupted) {
        if (connection == null) return 0;
        if (!encryptionKeyLoaded) {
            System.err.println("Skipping corrupted password check because the encryption key is unavailable.");
            return 0;
        }
        String selectSql = "SELECT id, service_name, encrypted_password FROM passwords";
        List<Integer> corruptedIds = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {
            while (rs.next()) {
                int    id      = rs.getInt("id");
                String service = rs.getString("service_name");
                String encPw   = rs.getString("encrypted_password");
                if (!PasswordHasher.isEncrypted(encPw)) continue;
                try {
                    PasswordHasher.decryptPassword(encPw);
                } catch (Exception e) {
                    System.err.println("Corrupted entry: service='" + service + "' id=" + id);
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
                for (int id : corruptedIds) {
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Error removing corrupted passwords: " + e.getMessage());
            }
        }
        return corruptedIds.size();
    }

    // ── Re-encryption (secret code change) ───────────────────────────────────

    public boolean reEncryptAllPasswords(String oldSecretCode, String newSecretCode) {
        if (connection == null) return false;

        // With MySQL the encryption key is the external ENCRYPTION_KEY, not
        // derived from the secret code — so re-encryption is a no-op here.
        // We only need to update the stored hash.
        String newHash = PasswordHasher.hashPassword(newSecretCode);
        setSecretCodeHash(newHash);
        System.out.println("Secret code hash updated (MySQL mode: encryption key unchanged).");
        return true;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    // ── Record ────────────────────────────────────────────────────────────────

    public record PasswordEntry(int id, String service, String username, String password, String url, String notes) {}

    private record DbConnectionConfig(String url, String user, String password,
                                      boolean autoStartCloudflareProxy, String mode) {}
}
