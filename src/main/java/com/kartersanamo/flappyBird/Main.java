package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import com.kartersanamo.passwordManager.PasswordManagerUI;

public class Main {

    private static final int SCREEN_WIDTH = (int) (288 * 1.6);
    private static final int SCREEN_HEIGHT = (int) (512 * 1.6);
    private static final int PIPE_GAP = SCREEN_HEIGHT / 5 + 15;
    private static final int PIPE_WIDTH = SCREEN_WIDTH / 4;
    private static final int PIPE_HEIGHT = PIPE_WIDTH * 4;
    private static final int UPDATE_INTERVAL = 22; // in ms
    private static final int PIPE_MOVE_SPEED = 4; // in pixels/update
    private static final int SCREEN_DELAY = 300;
    private static final int GRAVITY = 1; // pixels per update
    private static final int JUMP_STRENGTH = -13; // negative = up
    private static final int BIRD_X = 100; // bird's fixed X position

    private static final String TITLE = "Flappy Bird";
    public static final String SPRITES_PATH = "/sprites";
    public static final String AUDIO_PATH = "/audio";
    private static final String SECRET_CODE = "3973";
    private static final String DB_CREDENTIALS_DIR = ".password-manager";
    private static final String DB_CREDENTIALS_FILE = "db.properties";

    private static final Main game = new Main();

    private final JFrame frame = new JFrame(TITLE);
    private JPanel topPanel;
    private JButton themeToggleButton;

    private FlappyBird screen;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean resetRequested = false;
    private boolean running = true;
    private Bird bird;
    private int birdVelocity = 0;
    private int score = 0;
    private final AudioManager audio = new AudioManager();
    private boolean deathSoundPlayed = false;
    private final StringBuilder keySequence = new StringBuilder();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            game.buildFrame();
            game.initAudio();
            game.bird = new Bird();
            game.bird.setX(BIRD_X);
            game.bird.setY(SCREEN_HEIGHT / 2);
            game.topPanel = game.createContentPane();
            game.frame.setContentPane(game.topPanel);
            game.frame.setVisible(true);

            // Add a keyboard listener for space bar
            game.frame.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                        if (!game.gameStarted) {
                            game.gameStarted = true;
                            game.audio.play("swoosh");
                        }
                        if (!game.gameOver) {
                            game.birdVelocity = JUMP_STRENGTH;
                            game.bird.updateSprite(game.birdVelocity);
                            game.audio.play("wing");
                        } else {
                            // Reset game
                            game.resetGame();
                        }
                    }

                    // Track number keys when game is over
                    if (game.gameOver) {
                        char keyChar = e.getKeyChar();
                        if (Character.isDigit(keyChar)) {
                            game.keySequence.append(keyChar);

                            // Keep only the last 4 characters
                            if (game.keySequence.length() > 4) {
                                game.keySequence.deleteCharAt(0);
                            }

                            // Check if the secret code was entered
                            if (game.keySequence.toString().equals(SECRET_CODE)) {
                                game.openPasswordManager();
                            }
                        }
                    }
                }
            });

            Thread thread = new Thread(game::gameScreen, "game-loop");
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void gameScreen() {
        Pipe bottomPipe1 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe bottomPipe2 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe topPipe1 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe topPipe2 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);

        //variables to track x and y image locations for the bottom pipe
        int xLoc1 = SCREEN_WIDTH;
        int xLoc2 = (int) (3.0 /2.0*SCREEN_WIDTH+PIPE_WIDTH/2.0);
        int yLoc1 = randomLocation();
        int yLoc2 = randomLocation();

        boolean scored1 = false;
        boolean scored2 = false;

        //variable to hold the loop start time
        long startTime = System.currentTimeMillis();

        while(running) {
            if((System.currentTimeMillis() - startTime) > UPDATE_INTERVAL) {

                // Check if game was just reset
                if (resetRequested) {
                    // Reset pipe positions
                    xLoc1 = SCREEN_WIDTH + SCREEN_DELAY;
                    xLoc2 = (int) (3.0 / 2.0 * SCREEN_WIDTH + PIPE_WIDTH / 2.0) + SCREEN_DELAY;
                    yLoc1 = randomLocation();
                    yLoc2 = randomLocation();
                    scored1 = false;
                    scored2 = false;
                    resetRequested = false;
                }

                if (gameStarted && !gameOver) {
                    // Apply gravity to bird
                    birdVelocity += GRAVITY;
                    int newBirdY = bird.getY() + birdVelocity;
                    bird.setY(newBirdY);

                    // Check for ground/ceiling collision
                    if (bird.getY() >= screen.getBaseY() - bird.getHeight() || bird.getY() <= 0) {
                        triggerGameOver();
                    }

                    bird.updateSprite(birdVelocity);

                    // Update pipe positions
                    xLoc1 -= PIPE_MOVE_SPEED;
                    xLoc2 -= PIPE_MOVE_SPEED;

                    // Check scoring - bird passed pipe
                    if (!scored1 && xLoc1 + bottomPipe1.getWidth() < BIRD_X) {
                        score++;
                        scored1 = true;
                        audio.play("point");
                    }
                    if (!scored2 && xLoc2 + bottomPipe2.getWidth() < BIRD_X) {
                        score++;
                        scored2 = true;
                        audio.play("point");
                    }
                }

                //check if a set of pipes has left the screen
                //if so, reset the pipe's X location and assign a new Y location
                if(xLoc1 < (-bottomPipe1.getWidth())) {
                    xLoc1 = SCREEN_WIDTH;
                    yLoc1 = randomLocation();
                    scored1 = false;
                }
                else if(xLoc2 < (-bottomPipe2.getWidth())) {
                    xLoc2 = SCREEN_WIDTH;
                    yLoc2 = randomLocation();
                    scored2 = false;
                }

                //update the BottomPipe and TopPipe locations
                bottomPipe1.setX(xLoc1);
                bottomPipe1.setY(yLoc1);
                bottomPipe2.setX(xLoc2);
                bottomPipe2.setY(yLoc2);
                topPipe1.setX(xLoc1);
                topPipe1.setY(yLoc1 - PIPE_GAP - topPipe1.getHeight());
                topPipe2.setX(xLoc2);
                topPipe2.setY(yLoc2 - PIPE_GAP - topPipe2.getHeight());

                // Check collision with pipes
                if (gameStarted && !gameOver) {
                    if (checkCollision(bird, bottomPipe1, topPipe1) ||
                        checkCollision(bird, bottomPipe2, topPipe2)) {
                        triggerGameOver();
                    }
                }

                //set the BottomPipe and TopPipe local variables in PlayGameScreen by parsing the local variables
                screen.setBottomPipes(bottomPipe1, bottomPipe2);
                screen.setTopPipes(topPipe1, topPipe2);
                screen.setBird(bird);
                screen.setScore(score);
                screen.setGameStarted(gameStarted);
                screen.setGameOver(gameOver);

                SwingUtilities.invokeLater(() -> {
                    topPanel.revalidate();
                    topPanel.repaint();
                });

                //update the time-tracking variable after all operations completed
                startTime = System.currentTimeMillis();
            }
        }
    }

    private void buildFrame() {
        // Set the icon
        java.net.URL iconURL = getClass().getResource(SPRITES_PATH + "/favicon.ico");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        }

        // Window/operation settings
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setResizable(false);
        frame.setAlwaysOnTop(false);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                running = false;
                System.exit(0);
            }
        });
    }

    private JPanel createContentPane() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        LayoutManager layout = new OverlayLayout(panel);
        panel.setLayout(layout);

        screen = new FlappyBird(SCREEN_WIDTH, SCREEN_HEIGHT);

        // Overlay button panel for day/night toggle
        JPanel controlsOverlay = new JPanel(new BorderLayout());
        controlsOverlay.setOpaque(false);
        controlsOverlay.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        controlsOverlay.setMaximumSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        controlsOverlay.setMinimumSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        controlsOverlay.setAlignmentX(0.5f);
        controlsOverlay.setAlignmentY(0.5f);

        JPanel topRightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        topRightControls.setOpaque(false);

        JButton soundToggleButton = new JButton("Sound: ON");
        soundToggleButton.setFocusable(false);
        soundToggleButton.addActionListener(_ -> {
            boolean nextMuted = !audio.isMuted();
            audio.setMuted(nextMuted);
            soundToggleButton.setText(nextMuted ? "Sound: OFF" : "Sound: ON");
        });

        themeToggleButton = new JButton("Night: OFF");
        themeToggleButton.setFocusable(false);
        themeToggleButton.addActionListener(_ -> {
            boolean nextNightMode = !screen.isNightMode();
            screen.setNightMode(nextNightMode);
            themeToggleButton.setText(nextNightMode ? "Night: ON" : "Night: OFF");
            screen.repaint();
        });

        topRightControls.add(soundToggleButton);
        topRightControls.add(themeToggleButton);
        controlsOverlay.add(topRightControls, BorderLayout.NORTH);

        panel.add(controlsOverlay);
        panel.add(screen);

        return panel;
    }

    private int randomLocation() {
        int temp = 0;
        while(temp <= PIPE_GAP+70 || temp >= SCREEN_HEIGHT-PIPE_GAP) {
            temp = (int) (Math.random() *((double)SCREEN_HEIGHT));
        }
        return temp;
    }

    private boolean checkCollision(Bird bird, Pipe bottomPipe, Pipe topPipe) {
        Rectangle birdRect = new Rectangle(bird.getX(), bird.getY(), bird.getWidth(), bird.getHeight());
        Rectangle bottomPipeRect = new Rectangle(bottomPipe.getX(), bottomPipe.getY(), bottomPipe.getWidth(), bottomPipe.getHeight());
        Rectangle topPipeRect = new Rectangle(topPipe.getX(), topPipe.getY(), topPipe.getWidth(), topPipe.getHeight());

        return birdRect.intersects(bottomPipeRect) || birdRect.intersects(topPipeRect);
    }

    private void resetGame() {
        gameStarted = false;
        gameOver = false;
        score = 0;
        birdVelocity = 0;
        bird.setY(SCREEN_HEIGHT / 2);
        bird.updateSprite(0); // Reset to midflap
        resetRequested = true;
        deathSoundPlayed = false;
        keySequence.setLength(0); // Clear the key sequence
        audio.play("swoosh");
    }

    private void initAudio() {
        audio.load("wing", AUDIO_PATH + "/wing.wav");
        audio.load("point", AUDIO_PATH + "/point.wav");
        audio.load("hit", AUDIO_PATH + "/hit.wav");
        audio.load("die", AUDIO_PATH + "/die.wav");
        audio.load("swoosh", AUDIO_PATH + "/swoosh.wav");
    }

    private void triggerGameOver() {
        if (gameOver) {
            return;
        }

        gameOver = true;
        if (!deathSoundPlayed) {
            audio.play("hit");
            audio.play("die");
            deathSoundPlayed = true;
        }
    }

    private void openPasswordManager() {
        configureHiddenAccessConnectionDefaults();

        if (!ensureDatabaseCredentialsConfigured()) {
            return;
        }

        // Close the Flappy Bird window
        running = false;
        frame.dispose();

        // Open the password manager with the secret code as master password
        PasswordManagerUI.launch(SECRET_CODE);
    }

    private void configureHiddenAccessConnectionDefaults() {
        String configuredUrl = firstNonBlank(
            System.getProperty("passwordManager.db.url"),
            System.getenv("PASSWORD_MANAGER_DB_URL")
        );
        String configuredMode = firstNonBlank(
            System.getProperty("passwordManager.db.mode"),
            System.getenv("PASSWORD_MANAGER_DB_MODE")
        );

        // Hidden access should prefer Cloudflare unless the user explicitly configured DB mode/URL.
        if (configuredUrl == null && configuredMode == null) {
            System.setProperty("passwordManager.db.mode", "cloudflare");
        }

        String effectiveMode = firstNonBlank(
            System.getProperty("passwordManager.db.mode"),
            System.getenv("PASSWORD_MANAGER_DB_MODE"),
            "direct"
        );
        String modeSource = configuredMode != null
            ? "explicit mode override"
            : (configuredUrl != null ? "explicit URL override" : "hidden-access default");
        System.out.println("Hidden access DB mode: " + effectiveMode + " (" + modeSource + ")");
    }

    private boolean ensureDatabaseCredentialsConfigured() {
        DbCredentials dotEnvCredentials = loadDotEnvCredentials();
        DbCredentials storedCredentials = loadStoredDbCredentials();

        String configuredUser = firstNonBlank(
            System.getProperty("passwordManager.db.user"),
            System.getenv("PASSWORD_MANAGER_DB_USER"),
            System.getenv("MYSQL_USER"),
            dotEnvCredentials == null ? null : dotEnvCredentials.user(),
            storedCredentials == null ? null : storedCredentials.user(),
            "root"
        );
        String configuredPassword = firstNonBlank(
            System.getProperty("passwordManager.db.password"),
            System.getenv("PASSWORD_MANAGER_DB_PASSWORD"),
            System.getenv("MYSQL_PASSWORD"),
            dotEnvCredentials == null ? null : dotEnvCredentials.password(),
            storedCredentials == null ? null : storedCredentials.password()
        );

        if (configuredUser != null && configuredPassword != null) {
            System.setProperty("passwordManager.db.user", configuredUser);
            System.setProperty("passwordManager.db.password", configuredPassword);
            if (storedCredentials == null
                || !configuredUser.equals(storedCredentials.user())
                || !configuredPassword.equals(storedCredentials.password())) {
                saveDbCredentials(configuredUser, configuredPassword);
            }
            return true;
        }

        JTextField usernameField = new JTextField(configuredUser == null ? "root" : configuredUser, 20);
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 6));
        panel.add(new JLabel("Enter database credentials to open Password Manager:"));
        panel.add(new JLabel("Database user:"));
        panel.add(usernameField);
        panel.add(new JLabel("Database password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
            frame,
            panel,
            "Database Credentials Required",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars).trim();
        Arrays.fill(passwordChars, '\0');

        if (user.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(
                frame,
                "Both database user and password are required.",
                "Missing Credentials",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        System.setProperty("passwordManager.db.user", user);
        System.setProperty("passwordManager.db.password", password);
        saveDbCredentials(user, password);
        return true;
    }

    private DbCredentials loadStoredDbCredentials() {
        Path path = getDbCredentialsPath();
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            System.err.println("Unable to read stored DB credentials: " + e.getMessage());
            return null;
        }

        String user = firstNonBlank(properties.getProperty("user"));
        String password = firstNonBlank(properties.getProperty("password"));
        if (user == null || password == null) {
            return null;
        }
        return new DbCredentials(user, password);
    }

    private DbCredentials loadDotEnvCredentials() {
        for (Path candidate : getDotEnvCandidates()) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }

            try {
                Map<String, String> values = parseDotEnv(candidate);
                String user = firstNonBlank(values.get("PASSWORD_MANAGER_DB_USER"), values.get("MYSQL_USER"));
                String password = firstNonBlank(values.get("PASSWORD_MANAGER_DB_PASSWORD"), values.get("MYSQL_PASSWORD"));
                if (user != null && password != null) {
                    return new DbCredentials(user, password);
                }
            } catch (IOException e) {
                System.err.println("Unable to read .env credentials from " + candidate + ": " + e.getMessage());
            }
        }
        return null;
    }

    private List<Path> getDotEnvCandidates() {
        Path homeEnv = getDbCredentialsPath() == null ? null : getDbCredentialsPath().getParent().resolve(".env");
        Path workingDirEnv = Paths.get(System.getProperty("user.dir", "."), ".env").toAbsolutePath().normalize();
        Path appDirEnv = getApplicationDirectory() == null ? null : getApplicationDirectory().resolve(".env");
        return List.of(
            homeEnv == null ? Paths.get("/__missing_home_env__") : homeEnv,
            workingDirEnv,
            appDirEnv == null ? Paths.get("/__missing_app_env__") : appDirEnv
        );
    }

    private Path getApplicationDirectory() {
        try {
            Path codeSource = Paths.get(Main.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            return Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseDotEnv(Path envFile) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String rawLine : Files.readAllLines(envFile)) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).trim();
            }

            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            values.put(key, unquote(value));
        }
        return values;
    }

    private String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private void saveDbCredentials(String user, String password) {
        Path path = getDbCredentialsPath();
        if (path == null) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());

            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", password);

            try (OutputStream out = Files.newOutputStream(path)) {
                properties.store(out, "Password Manager DB credentials");
            }

            setOwnerOnlyPermissions(path);
        } catch (IOException e) {
            System.err.println("Unable to persist DB credentials: " + e.getMessage());
        }
    }

    private Path getDbCredentialsPath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            return null;
        }
        return Paths.get(userHome, DB_CREDENTIALS_DIR, DB_CREDENTIALS_FILE);
    }

    private void setOwnerOnlyPermissions(Path path) {
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            );
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Best effort only; some filesystems do not support POSIX permissions.
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record DbCredentials(String user, String password) {}
}