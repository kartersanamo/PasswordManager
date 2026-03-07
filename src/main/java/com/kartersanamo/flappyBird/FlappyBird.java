package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;

public class FlappyBird extends JPanel {
    private final int screenWidth;
    private final int screenHeight;
    private final Image backgroundImage;
    private final Image backgroundNightImage;
    private final Image baseImage;
    private final Image gameOverImage;
    private final Image[] numberImages;

    private Bird bird;
    private int score;
    private boolean gameStarted;
    private boolean gameOver;
    private boolean nightMode;

    private Pipe bottomPipe1;
    private Pipe bottomPipe2;
    private Pipe topPipe1;
    private Pipe topPipe2;

    public FlappyBird(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Load daytime background
        java.net.URL bgURL = getClass().getResource(Main.SPRITES_PATH + "/background-day.png");
        if (bgURL != null) {
            this.backgroundImage = new ImageIcon(bgURL).getImage();
        } else {
            this.backgroundImage = null;
        }

        // Load nighttime background
        java.net.URL bgNightURL = getClass().getResource(Main.SPRITES_PATH + "/background-night.png");
        if (bgNightURL != null) {
            this.backgroundNightImage = new ImageIcon(bgNightURL).getImage();
        } else {
            this.backgroundNightImage = null;
        }

        // Load base image
        java.net.URL baseURL = getClass().getResource(Main.SPRITES_PATH + "/base.png");
        if (baseURL != null) {
            this.baseImage = new ImageIcon(baseURL).getImage();
        } else {
            this.baseImage = null;
        }

        // Load game over image
        java.net.URL gameOverURL = getClass().getResource(Main.SPRITES_PATH + "/gameover.png");
        if (gameOverURL != null) {
            this.gameOverImage = new ImageIcon(gameOverURL).getImage();
        } else {
            this.gameOverImage = null;
        }

        // Load number images (0-9)
        this.numberImages = new Image[10];
        for (int i = 0; i < 10; i++) {
            java.net.URL numURL = getClass().getResource(Main.SPRITES_PATH + "/" + i + ".png");
            if (numURL != null) {
                numberImages[i] = new ImageIcon(numURL).getImage();
            }
        }

        setOpaque(true);
        setPreferredSize(new Dimension(screenWidth, screenHeight));
    }

    public void setBottomPipes(Pipe bottomPipe1, Pipe bottomPipe2) {
        this.bottomPipe1 = bottomPipe1;
        this.bottomPipe2 = bottomPipe2;
    }

    public void setTopPipes(Pipe topPipe1, Pipe topPipe2) {
        this.topPipe1 = topPipe1;
        this.topPipe2 = topPipe2;
    }

    public void setBird(Bird bird) {
        this.bird = bird;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(screenWidth, screenHeight);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw selected background scaled to screen
        Image selectedBackground = nightMode ? backgroundNightImage : backgroundImage;
        if (selectedBackground != null && selectedBackground.getWidth(null) > 0) {
            g.drawImage(selectedBackground, 0, 0, screenWidth, screenHeight, null);
        } else {
            g.setColor(new Color(114, 197, 255));
            g.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Draw pipes
        drawPipe(g, bottomPipe1, false);
        drawPipe(g, bottomPipe2, false);
        drawPipe(g, topPipe1, true);
        drawPipe(g, topPipe2, true);

        // Draw base at bottom (tile it across screen width)
        if (baseImage != null) {
            int baseWidth = baseImage.getWidth(null);
            int baseY = getBaseY();

            // Tile the base image across the screen width
            for (int x = 0; x < screenWidth; x += baseWidth) {
                g.drawImage(baseImage, x, baseY, null);
            }
        }

        // Draw bird
        if (bird != null && bird.getImage() != null) {
            g.drawImage(bird.getImage(), bird.getX(), bird.getY(), bird.getWidth(), bird.getHeight(), null);
        }

        // Draw score using number sprites
        if (gameStarted) {
            drawScore(g, score, screenWidth / 2, 80);
        }

        // Draw game over message
        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 128));
            g.fillRect(0, 0, screenWidth, screenHeight);

            // Draw game over sprite
            if (gameOverImage != null) {
                int goWidth = gameOverImage.getWidth(null);
                g.drawImage(gameOverImage, (screenWidth - goWidth) / 2, screenHeight / 2 - 100, null);
            }

            // Draw score label with text and score with number sprites
            g.setColor(Color.WHITE);
            g.setFont(new Font("Times New Roman", Font.PLAIN, 24));
            String scoreLabel = "Score:";
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(scoreLabel);
            g.drawString(scoreLabel, (screenWidth - labelWidth) / 2, screenHeight / 2);

            drawScore(g, score, screenWidth / 2, screenHeight / 2 + 40);

            String restartText = "Press SPACE to restart";
            fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(restartText);
            g.drawString(restartText, (screenWidth - textWidth) / 2, (screenHeight / 2 + 100) + 40);
        }

        // Draw start message
        if (!gameStarted && !gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Times New Roman", Font.BOLD, 32));
            String startText = "Press SPACE to start";
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(startText);
            g.drawString(startText, (screenWidth - textWidth) / 2, (screenHeight / 2) - 25);
        }
    }

    private void drawPipe(Graphics g, Pipe pipe, boolean flipped) {
        if (pipe == null || pipe.getImage() == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        if (flipped) {
            // Draw flipped (top pipe) - scaled to pipe dimensions
            g2d.drawImage(
                    pipe.getImage(),
                    pipe.getX(),
                    pipe.getY() + pipe.getHeight(),
                    pipe.getX() + pipe.getWidth(),
                    pipe.getY(),
                    0,
                    0,
                    pipe.getImage().getWidth(null),
                    pipe.getImage().getHeight(null),
                    null
            );
        } else {
            // Draw normal (bottom pipe) - scaled to pipe dimensions
            g2d.drawImage(
                    pipe.getImage(),
                    pipe.getX(),
                    pipe.getY(),
                    pipe.getWidth(),
                    pipe.getHeight(),
                    null
            );
        }
        g2d.dispose();
    }

    private void drawScore(Graphics g, int score, int centerX, int y) {
        String scoreStr = String.valueOf(score);

        // Calculate total width of all number sprites
        int totalWidth = 0;
        int spacing = 2; // pixels between numbers
        for (int i = 0; i < scoreStr.length(); i++) {
            int digit = scoreStr.charAt(i) - '0';
            if (numberImages[digit] != null) {
                totalWidth += numberImages[digit].getWidth(null);
                if (i < scoreStr.length() - 1) {
                    totalWidth += spacing;
                }
            }
        }

        // Draw each digit centered
        int x = centerX - totalWidth / 2;
        for (int i = 0; i < scoreStr.length(); i++) {
            int digit = scoreStr.charAt(i) - '0';
            if (numberImages[digit] != null) {
                Image numImg = numberImages[digit];
                g.drawImage(numImg, x, y, null);
                x += numImg.getWidth(null) + spacing;
            }
        }
    }

    public int getBaseY() {
        return screenHeight - baseImage.getHeight(null);
    }
}