package com.kartersanamo.passwordManager;

import com.jcraft.jsch.*;

/**
 * Manages automatic SSH tunneling to reach remote MySQL when direct TCP is blocked.
 * Handles tunneling via user's SSH config (e.g., HomeServer alias from ~/.ssh/config).
 */
public final class SSHTunnelManager {

    private static Session sshSession;
    private static int forwardedLocalPort = -1;
    private static final Object lock = new Object();

    private SSHTunnelManager() {
    }

    /**
     * Establishes an SSH tunnel to reach remote MySQL via port forwarding.
     * Reads SSH config from ~/.ssh/config (e.g., Host HomeServer).
     *
     * @param sshHostAlias    SSH config host alias (e.g., "HomeServer")
     * @param remoteBindAddr  MySQL address on remote server (usually "127.0.0.1")
     * @param remotePort      MySQL port on remote server (usually 3306)
     * @param localPort       Local port to bind tunnel to (e.g., 13306)
     * @return local port where tunnel is listening, or -1 if failed
     */
    public static int startTunnel(String sshHostAlias, String remoteBindAddr, int remotePort, int localPort) {
        synchronized (lock) {
            if (sshSession != null && sshSession.isConnected()) {
                System.out.println("SSH tunnel already running on localhost:" + forwardedLocalPort);
                return forwardedLocalPort;
            }

            try {
                JSch jsch = new JSch();
                String userHome = System.getProperty("user.home");

                // Load SSH config from ~/.ssh/config
                String sshConfigPath = userHome + "/.ssh/config";
                java.nio.file.Path configPath = java.nio.file.Paths.get(sshConfigPath);
                if (!java.nio.file.Files.exists(configPath)) {
                    System.err.println("SSH config not found at " + sshConfigPath);
                    return -1;
                }

                // Parse SSH config manually (simple key=value parsing)
                java.util.Map<String, String> config = parseSshConfig(sshConfigPath, sshHostAlias);
                if (config.isEmpty()) {
                    System.err.println("SSH host '" + sshHostAlias + "' not found in ~/.ssh/config");
                    return -1;
                }

                String hostname = config.getOrDefault("HostName", null);
                String user = config.getOrDefault("User", "sanamo");
                String portStr = config.getOrDefault("Port", "22");
                int port = Integer.parseInt(portStr);
                String identityFile = config.getOrDefault("IdentityFile", userHome + "/.ssh/id_rsa");

                System.out.println("SSH tunnel: " + user + "@" + hostname + ":" + port + 
                                 " -> " + remoteBindAddr + ":" + remotePort + " <- localhost:" + localPort);

                // Load identity file
                if (java.nio.file.Files.exists(java.nio.file.Paths.get(identityFile))) {
                    jsch.addIdentity(identityFile);
                    System.out.println("Loaded SSH key: " + identityFile);
                }

                // Create session
                sshSession = jsch.getSession(user, hostname, port);
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");

                // Connect
                sshSession.connect(10000);
                System.out.println("SSH connection established to " + hostname);

                // Set up port forwarding
                int assignedPort = sshSession.setPortForwardingL(localPort, remoteBindAddr, remotePort);
                forwardedLocalPort = assignedPort;

                System.out.println("SSH tunnel ready: localhost:" + assignedPort + " -> " + 
                                 remoteBindAddr + ":" + remotePort + " on " + hostname);

                return assignedPort;

            } catch (JSchException e) {
                System.err.println("SSH tunnel error: " + e.getMessage());
                e.printStackTrace();
                return -1;
            } catch (java.io.IOException e) {
                System.err.println("SSH config read error: " + e.getMessage());
                e.printStackTrace();
                return -1;
            }
        }
    }

    /**
     * Parse SSH config file and extract settings for a given host.
     */
    private static java.util.Map<String, String> parseSshConfig(String configPath, String targetHost) 
            throws java.io.IOException {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Paths.get(configPath));
        
        boolean inTarget = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            if (line.toLowerCase().startsWith("host ")) {
                String[] parts = line.split("\\s+", 2);
                inTarget = parts.length > 1 && parts[1].equalsIgnoreCase(targetHost);
            } else if (inTarget && line.contains(" ")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                }
            }
        }
        
        return result;
    }

    /**
     * Closes the SSH tunnel and cleans up resources.
     */
    public static void stopTunnel() {
        synchronized (lock) {
            if (sshSession != null && sshSession.isConnected()) {
                try {
                    sshSession.delPortForwardingL(forwardedLocalPort);
                    sshSession.disconnect();
                    System.out.println("SSH tunnel closed");
                } catch (JSchException e) {
                    System.err.println("Error closing SSH tunnel: " + e.getMessage());
                }
                sshSession = null;
                forwardedLocalPort = -1;
            }
        }
    }

    public static boolean isTunnelOpen() {
        synchronized (lock) {
            return sshSession != null && sshSession.isConnected();
        }
    }

    public static int getTunnelLocalPort() {
        return forwardedLocalPort;
    }
}

