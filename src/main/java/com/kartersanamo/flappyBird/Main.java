package com.kartersanamo.flappyBird;

import javax.swing.*;
import java.awt.*;

public class Main {

    private static final int SCREEN_WIDTH = 288 * 2;
    private static final int SCREEN_HEIGHT = 512 * 2;
    private static final int PIPE_GAP = SCREEN_HEIGHT / 5;
    private static final int PIPE_WIDTH = SCREEN_WIDTH / 8;
    private static final int PIPE_HEIGHT = PIPE_WIDTH * 4;
    private static final int UPDATE_INTERVAL = 25; // in ms
    private static final int PIPE_MOVE_SPEED = 5; // in pixels/update
    private static final int SCREEN_DELAY = 300;

    private static final String TITLE = "Flappy Bird";
    public static final String SPRITES_PATH = "resources/sprites";

    private static final Main game = new Main();

    private final JFrame frame = new JFrame(TITLE);
    private JButton startButton;
    private JPanel topPanel;

    private FlappyBird screen;
    private boolean loop = true;

    static void main() {
        SwingUtilities.invokeLater(() -> {
            game.buildFrame();

            Thread thread = new Thread(() -> {
                game.gameScreen(true);
            });
            thread.start();
        });
    }

    private void gameScreen(boolean isSplash) {
        Pipe bottomPipe1 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe bottomPipe2 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe topPipe1 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);
        Pipe topPipe2 = new Pipe(PIPE_WIDTH, PIPE_HEIGHT);

        //variables to track x and y image locations for the bottom pipe
        int xLoc1 = SCREEN_WIDTH+SCREEN_DELAY, xLoc2 = (int) (3.0 /2.0*SCREEN_WIDTH+PIPE_WIDTH/2.0) + SCREEN_DELAY;
        int yLoc1 = randomLocation(), yLoc2 = randomLocation();

        //variable to hold the loop start time
        long startTime = System.currentTimeMillis();

        while(loop) {
            if((System.currentTimeMillis() - startTime) > UPDATE_INTERVAL) {
                //check if a set of pipes has left the screen
                //if so, reset the pipe's X location and assign a new Y location
                if(xLoc1 < (-PIPE_WIDTH)) {
                    xLoc1 = SCREEN_WIDTH;
                    yLoc1 = randomLocation();
                }
                else if(xLoc2 < (-PIPE_WIDTH)) {
                    xLoc2 = SCREEN_WIDTH;
                    yLoc2 = randomLocation();
                }

                //decrement the pipe locations by the predetermined amount
                xLoc1 -= PIPE_MOVE_SPEED;
                xLoc2 -= PIPE_MOVE_SPEED;

                //update the BottomPipe and TopPipe locations
                bottomPipe1.setX(xLoc1);
                bottomPipe1.setY(yLoc1);
                bottomPipe2.setX(xLoc2);
                bottomPipe2.setY(yLoc2);
                topPipe1.setX(xLoc1);
                topPipe1.setY(yLoc1-PIPE_GAP-PIPE_HEIGHT); //ensure tp1 placed in proper location
                topPipe2.setX(xLoc2);
                topPipe2.setY(yLoc2-PIPE_GAP-PIPE_HEIGHT); //ensure tp2 placed in proper location

                //set the BottomPipe and TopPipe local variables in PlayGameScreen by parsing the local variables
                screen.setBottomPipes(bottomPipe1, bottomPipe2);
                screen.setTopPipes(topPipe1, topPipe2);

                //update pgs's JPanel
                topPanel.revalidate();
                topPanel.repaint();

                //update the time-tracking variable after all operations completed
                startTime = System.currentTimeMillis();
            }
        }
    }

    private void buildFrame() {
        // Set the icon
        ImageIcon icon = new ImageIcon(SPRITES_PATH + "/favicon.ico");
        frame.setIconImage(icon.getImage());

        // Window/operation settings
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        frame.setResizable(false);
        frame.setAlwaysOnTop(false);
        frame.setVisible(true);
    }

    private JPanel createContentPane() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        LayoutManager layout = new OverlayLayout(panel);
        panel.setLayout(layout);

        JButton startGame = new JButton("Start Game");
        startGame.setBackground(Color.BLACK);
        startGame.setForeground(Color.WHITE);
        startGame.setFocusable(false);
        startGame.setFont(new Font("Arial", Font.BOLD, 42));
        startGame.setAlignmentX(Component.CENTER_ALIGNMENT);
        startGame.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(startGame);

        FlappyBird gameScreen = new FlappyBird(SCREEN_WIDTH, SCREEN_HEIGHT, true);
        panel.add(gameScreen);

        startGame.addActionListener(e -> {
           // do sum
        });

        return panel;
    }

    private int randomLocation() {
        int temp = 0;
        while(temp <= PIPE_GAP+50 || temp >= SCREEN_HEIGHT-PIPE_GAP) {
            temp = (int) (Math.random() *((double)SCREEN_HEIGHT));
        }
        return temp;
    }
}
