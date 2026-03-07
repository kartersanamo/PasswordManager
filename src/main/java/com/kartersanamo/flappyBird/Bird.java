package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Bird {
    private Image birdImage;
    private Image upflapImage;
    private Image midflapImage;
    private Image downflapImage;
    private final Rectangle birdRect;
    private int birdX;
    private int birdY;
    private static final double SCALE = 1.5;

    public Bird() {
        // Load all three bird sprites
        java.net.URL upflapURL = getClass().getResource(Main.SPRITES_PATH + "/yellowbird-upflap.png");
        if (upflapURL != null) {
            this.upflapImage = new ImageIcon(upflapURL).getImage();
        }

        java.net.URL midflapURL = getClass().getResource(Main.SPRITES_PATH + "/yellowbird-midflap.png");
        if (midflapURL != null) {
            this.midflapImage = new ImageIcon(midflapURL).getImage();
        }

        java.net.URL downflapURL = getClass().getResource(Main.SPRITES_PATH + "/yellowbird-downflap.png");
        if (downflapURL != null) {
            this.downflapImage = new ImageIcon(downflapURL).getImage();
        }

        // Start with midflap
        this.birdImage = midflapImage;

        this.birdX = 100;
        this.birdY = 100;
        int width = birdImage != null ? birdImage.getWidth(null) : 34;
        int height = birdImage != null ? birdImage.getHeight(null) : 24;
        this.birdRect = new Rectangle(birdX, birdY, (int) (width * SCALE), (int) (height * SCALE));
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        if (birdImage != null) {
            g.drawImage(birdImage, 0, 0, null);
        }
        g.dispose();
    }

    public Image getImage() {
        return birdImage;
    }

    public int getWidth() {
        return birdRect.width;
    }

    public int getHeight() {
        return birdRect.height;
    }

    public int getX() {
        return birdX;
    }

    public void setX(int x) {
        this.birdX = x;
    }

    public int getY() {
        return birdY;
    }

    public void setY(int y) {
        this.birdY = y;
    }

    public void updateSprite(int velocity) {
        // Change sprite based on velocity
        // velocity > 5 means falling fast (downflap)
        // velocity < -5 means rising fast (upflap)
        // otherwise midflap
        if (velocity > 5) {
            this.birdImage = downflapImage;
        } else if (velocity < -3) {
            this.birdImage = upflapImage;
        } else {
            this.birdImage = midflapImage;
        }
    }
}