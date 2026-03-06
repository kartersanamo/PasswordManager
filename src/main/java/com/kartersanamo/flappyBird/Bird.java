package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Bird {
    private Image birdImage;
    private BufferedImage birdBI;
    private Rectangle birdRect;
    private int birdX;
    private int birdY;

    public Bird() {
        this.birdImage = new ImageIcon(Main.SPRITES_PATH + "/bird.png").getImage();
        this.birdRect = new Rectangle(birdX, birdY, birdImage.getWidth(null), birdImage.getHeight(null));
        this.birdX = 100;
        this.birdY = 100;
        BufferedImage bi = new BufferedImage(birdImage.getWidth(null), birdImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        g.drawImage(birdImage, 0, 0, null);
        g.dispose();
        this.birdBI = bi;
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

    public BufferedImage getBI() {
        return this.birdBI;
    }
}
