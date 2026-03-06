package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;

public class FlappyBird extends JPanel {
    private int screenWidth;
    private int screenHeight;
    private boolean isSplash = true;

    private Pipe bottomPipe1;
    private Pipe bottomPipe2;
    private Pipe topPipe1;
    private Pipe topPipe2;

    public FlappyBird(int screenWidth, int screenHeight, boolean isSplash) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.isSplash = isSplash;
    }

    public void setBottomPipes(Pipe bottomPipe1, Pipe bottomPipe2) {
        this.bottomPipe1 = bottomPipe1;
        this.bottomPipe2 = bottomPipe2;
    }

    public void setTopPipes(Pipe topPipe1, Pipe topPipe2) {
        this.topPipe1 = topPipe1;
        this.topPipe2 = topPipe2;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}
