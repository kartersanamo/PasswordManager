package com.kartersanamo.passwordManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the external AES encryption key used for MySQL password data.
 */
public final class EncryptionKeyResolver {

    private static final Pattern RUN_CONFIG_KEY_PATTERN = Pattern.compile(
        "<env\\s+name=\"ENCRYPTION_KEY\"\\s+value=\"([0-9a-fA-F]{64})\""
    );

    private EncryptionKeyResolver() {
    }

    public static KeyResolutionResult resolve() {
        List<String> attemptedSources = new ArrayList<>();

        KeyResolutionResult result = tryStringValue(System.getenv("ENCRYPTION_KEY"), "environment variable ENCRYPTION_KEY");
        if (result.found()) {
            return result;
        }
        attemptedSources.add(result.message());

        result = tryStringValue(System.getProperty("ENCRYPTION_KEY"), "JVM property ENCRYPTION_KEY");
        if (result.found()) {
            return result;
        }
        attemptedSources.add(result.message());

        result = tryStringValue(System.getProperty("passwordManager.encryptionKey"),
            "JVM property passwordManager.encryptionKey");
        if (result.found()) {
            return result;
        }
        attemptedSources.add(result.message());

        for (Path candidate : collectKeyFileCandidates()) {
            result = tryFile(candidate);
            if (result.found()) {
                return result;
            }
            attemptedSources.add(result.message());
        }

        for (Path runConfig : collectRunConfigCandidates()) {
            result = tryIntelliJRunConfiguration(runConfig);
            if (result.found()) {
                return result;
            }
            attemptedSources.add(result.message());
        }

        return new KeyResolutionResult(
            null,
            null,
            "No usable encryption key found. Checked: " + String.join("; ", attemptedSources)
        );
    }

    private static KeyResolutionResult tryStringValue(String value, String sourceDescription) {
        if (value == null || value.isBlank()) {
            return new KeyResolutionResult(null, null, sourceDescription + " not set");
        }

        try {
            byte[] rawKey = HexFormat.of().parseHex(value.strip());
            if (rawKey.length != 32) {
                return new KeyResolutionResult(null, null,
                    sourceDescription + " must be exactly 64 hex characters");
            }
            return new KeyResolutionResult(rawKey, sourceDescription,
                "Loaded encryption key from " + sourceDescription);
        } catch (IllegalArgumentException e) {
            return new KeyResolutionResult(null, null,
                sourceDescription + " contains invalid hex");
        }
    }

    private static KeyResolutionResult tryFile(Path candidate) {
        if (candidate == null) {
            return new KeyResolutionResult(null, null, "key file candidate path unavailable");
        }
        if (!Files.isRegularFile(candidate)) {
            return new KeyResolutionResult(null, null, "key file not found: " + candidate);
        }

        try {
            String content = Files.readString(candidate, StandardCharsets.UTF_8).trim();
            KeyResolutionResult result = tryStringValue(content, "file " + candidate);
            if (result.found()) {
                return result;
            }
            return new KeyResolutionResult(null, null, "invalid key file: " + candidate + " (" + result.message() + ")");
        } catch (IOException e) {
            return new KeyResolutionResult(null, null, "unable to read key file: " + candidate + " (" + e.getMessage() + ")");
        }
    }

    private static KeyResolutionResult tryIntelliJRunConfiguration(Path runConfigPath) {
        if (runConfigPath == null || !Files.isRegularFile(runConfigPath)) {
            return new KeyResolutionResult(null, null, "IntelliJ run configuration not found: " + runConfigPath);
        }

        try {
            String xml = Files.readString(runConfigPath, StandardCharsets.UTF_8);
            Matcher matcher = RUN_CONFIG_KEY_PATTERN.matcher(xml);
            if (!matcher.find()) {
                return new KeyResolutionResult(null, null,
                    "ENCRYPTION_KEY not present in IntelliJ run configuration: " + runConfigPath);
            }
            return tryStringValue(matcher.group(1), "IntelliJ run configuration " + runConfigPath);
        } catch (IOException e) {
            return new KeyResolutionResult(null, null,
                "unable to read IntelliJ run configuration: " + runConfigPath + " (" + e.getMessage() + ")");
        }
    }

    private static List<Path> collectKeyFileCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();

        Path appDir = getApplicationDirectory();
        Path workingDir = Paths.get("").toAbsolutePath().normalize();
        Path userHome = getUserHomeDirectory();

        addKeyFileLocations(candidates, appDir);
        addKeyFileLocations(candidates, workingDir);
        if (userHome != null) {
            candidates.add(userHome.resolve(".password-manager").resolve("encryption.key"));
        }

        return new ArrayList<>(candidates);
    }

    private static void addKeyFileLocations(Set<Path> candidates, Path baseDir) {
        if (baseDir == null) {
            return;
        }
        candidates.add(baseDir.resolve("encryption.key"));
        candidates.add(baseDir.resolve(".encryption.key"));
        candidates.add(baseDir.resolve("config").resolve("encryption.key"));
    }

    private static List<Path> collectRunConfigCandidates() {
        Set<Path> candidates = new LinkedHashSet<>();
        addAncestorRunConfigs(candidates, getApplicationDirectory());
        addAncestorRunConfigs(candidates, Paths.get("").toAbsolutePath().normalize());
        return new ArrayList<>(candidates);
    }

    private static void addAncestorRunConfigs(Set<Path> candidates, Path start) {
        Path current = start;
        while (current != null) {
            candidates.add(current.resolve(".idea").resolve("runConfigurations").resolve("Main.xml"));
            current = current.getParent();
        }
    }

    private static Path getApplicationDirectory() {
        try {
            Path codeSource = Paths.get(EncryptionKeyResolver.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            return Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private static Path getUserHomeDirectory() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            return null;
        }
        return Paths.get(userHome).toAbsolutePath().normalize();
    }

    public record KeyResolutionResult(byte[] rawKey, String sourceDescription, String message) {
        public boolean found() {
            return rawKey != null;
        }
    }
}

