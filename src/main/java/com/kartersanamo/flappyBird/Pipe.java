package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;

public class Pipe {

    private Image pipeImage;
    private int pipeX;
    private int pipeY;
    private final int pipeWidth;
    private final int pipeHeight;

    public Pipe(int pipeWidth, int pipeHeight) {
        java.net.URL pipeURL = getClass().getResource(Main.SPRITES_PATH + "/pipe-green.png");
        if (pipeURL != null) {
            ImageIcon icon = new ImageIcon(pipeURL);
            this.pipeImage = icon.getImage();
        }
        this.pipeWidth = pipeWidth;
        this.pipeHeight = pipeHeight;
        this.pipeX = 1024;
        this.pipeY = 0;
    }

    public Image getImage() {
        return pipeImage;
    }

    public int getWidth() {
        return pipeWidth;
    }

    public int getHeight() {
        return pipeHeight;
    }

    public int getX() {
        return pipeX;
    }

    public void setX(int x) {
        this.pipeX = x;
    }

    public int getY() {
        return pipeY;
    }

    public void setY(int y) {
        this.pipeY = y;
    }
}
