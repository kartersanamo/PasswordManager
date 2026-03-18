package com.kartersanamo.passwordManager;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

public class PasswordHasher {

    // Using Argon2id variant which is resistant to both side-channel and GPU attacks
    private static final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    // Recommended parameters for Argon2:
    private static final int ITERATIONS = 3;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 4;

    // AES-GCM parameters
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";

    private static SecretKey encryptionKey;
    private static boolean externalEncryptionKeyActive;

    /**
     * Sets the master password used for encryption key derivation.
     * This should be called with the secret code when the password manager is unlocked.
     *
     * @param masterPassword the master password (secret code from Flappy Bird)
     */
    public static void setMasterPassword(String masterPassword) {
        if (masterPassword == null || masterPassword.isEmpty()) {
            throw new IllegalArgumentException("Master password cannot be null or empty");
        }
        if (externalEncryptionKeyActive) {
            System.out.println("External encryption key is active; secret code will not replace it.");
            return;
        }
        encryptionKey = deriveEncryptionKey(masterPassword);
        System.out.println("Encryption key initialized with master password using PBKDF2");
    }

    /**
     * Sets the encryption key directly from raw bytes (e.g., from ENCRYPTION_KEY env var).
     * Use this when the key is managed externally rather than derived from a master password.
     *
     * @param rawKey 32-byte AES-256 key
     */
    public static void setRawKey(byte[] rawKey) {
        if (rawKey == null || rawKey.length != 32) {
            throw new IllegalArgumentException("Raw key must be exactly 32 bytes for AES-256");
        }
        encryptionKey = new SecretKeySpec(rawKey, "AES");
        externalEncryptionKeyActive = true;
        System.out.println("Encryption key loaded from raw ENCRYPTION_KEY");
    }

    /**
     * Gets the current encryption key.
     *
     * @return the encryption key
     */
    private static SecretKey getEncryptionKey() {
        if (encryptionKey == null) {
            throw new IllegalStateException(
                "No encryption key is loaded. Configure ENCRYPTION_KEY, a supported key file, or JVM property."
            );
        }
        return encryptionKey;
    }

    /**
     * Hashes a password using Argon2id algorithm (one-way hash for authentication).
     *
     * @param password the plain text password to hash
     * @return the hashed password as a string
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            char[] passwordChars = password.toCharArray();
            String hash = argon2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordChars);

            // Clear the password from memory
            java.util.Arrays.fill(passwordChars, '\0');

            return hash;
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verifies a password against a hash.
     *
     * @param hash the stored hash to verify against
     * @param password the plain text password to verify
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean verifyPassword(String hash, String password) {
        if (hash == null || password == null) {
            return false;
        }

        try {
            char[] passwordChars = password.toCharArray();
            boolean matches = argon2.verify(hash, passwordChars);

            // Clear the password from memory
            java.util.Arrays.fill(passwordChars, '\0');

            return matches;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Derives a 256-bit encryption key from a master password using PBKDF2.
     * Uses a fixed salt to ensure the same master password always produces the same key.
     * <p>
     * Note: While Argon2 is better for password hashing (authentication), PBKDF2 is 
     * perfectly suitable for key derivation and is deterministic with a fixed salt.
     *
     * @param masterPassword the master password
     * @return a SecretKey suitable for AES-256 encryption
     */
    private static SecretKey deriveEncryptionKey(String masterPassword) {
        try {
            // Use a FIXED salt so we get the same key every time from the same password
            // In production, this salt should be unique per user and stored securely
            byte[] salt = "PASSWORD_MANAGER_SALT_V1_2026".getBytes(StandardCharsets.UTF_8);
            
            // PBKDF2 parameters - 100,000 iterations are recommended for 2026
            int iterations = 100000;
            int keyLength = 256; // 256 bits for AES-256
            
            // Create PBKDF2 key spec
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), salt, iterations, keyLength);
            
            // Get SecretKeyFactory for PBKDF2WithHmacSHA256
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            
            // Generate the key
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Error deriving encryption key", e);
        }
    }

    /**
     * Encrypts a password using AES-GCM with a key derived from Argon2.
     * The result includes the IV and is Base64 encoded.
     *
     * @param plaintext the password to encrypt
     * @return the encrypted password (Base64 encoded, includes IV)
     */
    public static String encryptPassword(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }

        try {
            // Generate a random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(), spec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }

    /**
     * Decrypts a password that was encrypted with encryptPassword.
     *
     * @param encrypted the encrypted password (Base64 encoded, includes IV)
     * @return the decrypted password
     */
    public static String decryptPassword(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }

        try {
            // Decode Base64
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), spec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password", e);
        }
    }

    /**
     * Checks if a given string is already encrypted (Base64 encoded).
     * This is a simple heuristic check.
     *
     * @param text the text to check
     * @return true if the text appears to be encrypted
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Check if it's valid Base64 and has reasonable length
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            // Encrypted passwords should be at least IV length + some ciphertext
            return decoded.length > GCM_IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

