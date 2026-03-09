package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;
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

    static void main() {
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
        int xLoc1 = SCREEN_WIDTH+SCREEN_DELAY, xLoc2 = (int) (3.0 /2.0*SCREEN_WIDTH+PIPE_WIDTH/2.0) + SCREEN_DELAY;
        int yLoc1 = randomLocation(), yLoc2 = randomLocation();

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
        // Close the Flappy Bird window
        running = false;
        frame.dispose();

        // Open the password manager with the secret code as master password
        PasswordManagerUI.launch(SECRET_CODE);
    }
}