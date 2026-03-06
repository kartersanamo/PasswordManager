package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;

public class Pipe {

    private Image pipeImage;
    private Rectangle pipeRect;
    private int pipeX;
    private int pipeY;

    public Pipe(int pipeWidth, int pipeHeight) {
        this.pipeImage = new ImageIcon(Main.SPRITES_PATH + "/pipe-green.png").getImage().getScaledInstance(pipeWidth, pipeHeight, Image.SCALE_DEFAULT);
        this.pipeX = 1024;
        this.pipeY = 0;
        this.pipeRect = new Rectangle(pipeX, pipeY, pipeImage.getWidth(null), pipeImage.getHeight(null));
    }

    public Image getImage() {
        return pipeImage;
    }

    public int getWidth() {
        return pipeImage.getWidth(null);
    }

    public int getHeight() {
        return pipeImage.getHeight(null);
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
