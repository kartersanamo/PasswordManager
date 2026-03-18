package com.kartersanamo.passwordManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the Cloudflare TCP proxy for MySQL when needed.
 */
public final class CloudflareTcpProxyManager {

    private static final String LOCAL_HOST = "127.0.0.1";
    private static final int LOCAL_PORT = 13306;
    private static final int STARTUP_CHECK_ATTEMPTS = 120;
    private static final int STARTUP_CHECK_SLEEP_MS = 250;
    private static final String DEFAULT_HOSTNAME = "sql.kartersanamo.com";
    private static Process appOwnedProxyProcess;
    private static volatile String lastRuntimeDiagnostic = "Not available.";

    private CloudflareTcpProxyManager() {
    }

    public static synchronized ProxyStartResult ensureProxyRunning() {
        return ensureProxyRunning(false);
    }

    public static synchronized ProxyStartResult ensureProxyRunning(boolean forceRestart) {
        if (forceRestart && appOwnedProxyProcess != null) {
            stopAppOwnedProxy();
        }

        if (isLocalPortOpen()) {
            if (forceRestart) {
                lastRuntimeDiagnostic = "Local port 13306 is already in use by another process.";
                return new ProxyStartResult(false,
                    "Local port 13306 is already in use by another process; cannot force-restart proxy safely.");
            }
            lastRuntimeDiagnostic = "Local port 13306 is already open.";
            return new ProxyStartResult(true, "Cloudflare proxy already running.");
        }

        String executable = resolveCloudflaredExecutable();
        if (executable == null) {
            lastRuntimeDiagnostic = "Could not find cloudflared executable.";
            return new ProxyStartResult(false,
                "Could not find cloudflared executable. Set CLOUDFLARED_PATH or install cloudflared to /opt/homebrew/bin/cloudflared.");
        }

        String hostname = getEffectiveHostname();
        List<String> command = buildStartCommand(executable, hostname);
        System.out.println("Cloudflare TCP hostname: " + hostname);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            // Packaged macOS apps often have a minimal PATH.
            String path = processBuilder.environment().getOrDefault("PATH", "");
            String extraPath = "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin";
            processBuilder.environment().put("PATH",
                (path == null || path.isBlank()) ? extraPath : (path + ":" + extraPath));

            Process proxyProcess = processBuilder.start();
            appOwnedProxyProcess = proxyProcess;
            startLogPump(proxyProcess);

            if (waitForLocalPortReady(proxyProcess)) {
                lastRuntimeDiagnostic = "Cloudflare listener opened on localhost:13306.";
                return new ProxyStartResult(true, "Cloudflare proxy started by application.");
            }

            if (!proxyProcess.isAlive()) {
                lastRuntimeDiagnostic = "cloudflared exited before local listener was ready.";
                return new ProxyStartResult(false,
                    "cloudflared exited before opening local port 13306. Command: " + String.join(" ", command));
            }

            lastRuntimeDiagnostic = "Proxy process started but localhost:13306 was not ready in time.";
            return new ProxyStartResult(false,
                "Proxy process started, but local port 13306 did not become ready in time. " +
                    "Cloudflare Access login may still be required. Command: " + String.join(" ", command));
        } catch (IOException e) {
            lastRuntimeDiagnostic = "Failed to start cloudflared: " + e.getMessage();
            return new ProxyStartResult(false,
                "Failed to start cloudflared with executable '" + executable + "': " + e.getMessage());
        }
    }

    public static String getLastRuntimeDiagnostic() {
        return lastRuntimeDiagnostic;
    }

    private static void stopAppOwnedProxy() {
        Process process = appOwnedProxyProcess;
        appOwnedProxyProcess = null;
        if (process == null) {
            return;
        }
        if (!process.isAlive()) {
            return;
        }

        process.destroy();
        for (int i = 0; i < 10; i++) {
            if (!process.isAlive()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static List<String> buildStartCommand(String executable, String hostname) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("access");
        command.add("tcp");
        command.add("--hostname");
        command.add(hostname);
        command.add("--url");
        command.add("localhost:" + LOCAL_PORT);
        return command;
    }

    public static String getEffectiveHostname() {
        String jvmOverride = sanitizeHostname(System.getProperty("passwordManager.cloudflareHostname"));
        if (jvmOverride != null) {
            return jvmOverride;
        }

        String envOverride = sanitizeHostname(System.getenv("CLOUDFLARE_TCP_HOSTNAME"));
        if (envOverride != null) {
            return envOverride;
        }

        return DEFAULT_HOSTNAME;
    }

    private static String sanitizeHostname(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String candidate = trimmed;
        try {
            URI uri = trimmed.contains("://") ? URI.create(trimmed) : URI.create("https://" + trimmed);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                candidate = uri.getHost();
            }
        } catch (IllegalArgumentException ignored) {
            // Fall back to manual normalization below.
        }

        int slashIdx = candidate.indexOf('/');
        if (slashIdx >= 0) {
            candidate = candidate.substring(0, slashIdx);
        }
        while (candidate.endsWith(".")) {
            candidate = candidate.substring(0, candidate.length() - 1);
        }

        candidate = candidate.trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private static String resolveCloudflaredExecutable() {
        String envPath = System.getenv("CLOUDFLARED_PATH");
        if (isExecutable(envPath)) {
            return envPath;
        }

        for (Path candidate : candidateExecutablePaths()) {
            if (isExecutable(candidate.toString())) {
                return candidate.toString();
            }
        }

        // Last resort: rely on PATH lookup.
        return "cloudflared";
    }

    private static List<Path> candidateExecutablePaths() {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get("/opt/homebrew/bin/cloudflared"));
        candidates.add(Paths.get("/usr/local/bin/cloudflared"));
        candidates.add(Paths.get("/usr/bin/cloudflared"));

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            candidates.add(Paths.get(userHome, "bin", "cloudflared"));
        }

        Path appDir = getApplicationDirectory();
        if (appDir != null) {
            candidates.add(appDir.resolve("cloudflared"));
            candidates.add(appDir.resolve("bin").resolve("cloudflared"));

            Path macOsDir = findAncestorByName(appDir, "MacOS");
            if (macOsDir != null) {
                candidates.add(macOsDir.resolve("cloudflared"));
            }

            Path resourcesDir = findAncestorByName(appDir, "Resources");
            if (resourcesDir != null) {
                candidates.add(resourcesDir.resolve("cloudflared"));
                candidates.add(resourcesDir.resolve("bin").resolve("cloudflared"));
            }
        }
        return candidates;
    }

    private static Path findAncestorByName(Path start, String name) {
        Path current = start;
        while (current != null) {
            Path fileName = current.getFileName();
            if (fileName != null && name.equals(fileName.toString())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean isExecutable(String pathString) {
        if (pathString == null || pathString.isBlank()) {
            return false;
        }
        try {
            Path path = Paths.get(pathString).toAbsolutePath().normalize();
            return Files.isRegularFile(path) && Files.isExecutable(path);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Path getApplicationDirectory() {
        try {
            Path codeSource = Paths.get(CloudflareTcpProxyManager.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            return Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean waitForLocalPortReady(Process process) {
        for (int i = 0; i < STARTUP_CHECK_ATTEMPTS; i++) {
            if (isLocalPortOpen()) {
                return true;
            }
            if (process != null && !process.isAlive()) {
                return false;
            }
            try {
                Thread.sleep(STARTUP_CHECK_SLEEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static boolean isLocalPortOpen() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOCAL_HOST, LOCAL_PORT), 300);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void startLogPump(Process process) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lastRuntimeDiagnostic = line;
                    System.out.println("[cloudflared] " + line);
                }
            } catch (IOException ignored) {
                // Ignore logging failures; they should not block app startup.
            }
        }, "cloudflared-log");
        logThread.setDaemon(true);
        logThread.start();
    }

    public record ProxyStartResult(boolean ready, String message) {
    }
}

