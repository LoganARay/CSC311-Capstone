package edu.farmingdale.csc311capstone.Tetris;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class TetrisController {
    private AnimationTimer at;
    private ImageView[] currentBlock;
    public static boolean[][] grid;
    private static int[] pieceCords;


    @FXML
    private HBox root;

    @FXML
    private GridPane gamespace;

    @FXML
    private void initialize(){
        root.setFocusTraversable(true);
        gamespace.setPickOnBounds(true);
        grid= new boolean[10][20];
        newBlockEnters();

        at= new AnimationTimer() {
            private long lastUpdate = 0;
            private double accumulatedTime = 0.0; // Tracks time between snaps

            private final double snapInterval = 0.1; // Time in seconds (every 2 seconds)
            private final double gridStep = 20.0;    // Number of pixels to snap down each time

            @Override
            public void handle(long now) {
                if(!TetrisMechanics.checkCollisionDown(pieceCords)) {
                    if (lastUpdate == 0) {
                        lastUpdate = now;
                        return;
                    }
                    // Calculate time elapsed in seconds since last frame
                    double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                    lastUpdate = now;

                    // Add to our running timer accumulator
                    accumulatedTime += elapsedSeconds;

                    // Check if 2 seconds have passed
                    if (accumulatedTime >= snapInterval) {
                        gamespace.getChildren().remove(currentBlock[0]);
                        gamespace.getChildren().remove(currentBlock[1]);
                        gamespace.getChildren().remove(currentBlock[2]);
                        gamespace.getChildren().remove(currentBlock[3]);
                        pieceCords[1]++; pieceCords[3]++; pieceCords[5]++; pieceCords[7]++;
                        gamespace.add(currentBlock[0], pieceCords[0], pieceCords[1]);
                        gamespace.add(currentBlock[1], pieceCords[2], pieceCords[3]);
                        gamespace.add(currentBlock[2], pieceCords[4], pieceCords[5]);
                        gamespace.add(currentBlock[3], pieceCords[6], pieceCords[7]);

                        // Subtract the interval so we keep the remainder for precise timing
                        accumulatedTime -= snapInterval;
                    }
                }
                else{
                    stop();
                    TetrisMechanics.setCollision(pieceCords);
                    newBlockEnters();
                    at.start();
                }
            }

            @Override
            public void stop() {
                super.stop();
                lastUpdate = 0; // Reset so next start doesn't cause a massive jump
            }
        };
        at.start();

        root.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {
                    newScene.addEventFilter(
                            KeyEvent.KEY_PRESSED,
                            this::keyPressed
                    );
                }
        );
    }

    private void keyPressed(KeyEvent key){
        at.stop();
        if(key.getCode()== KeyCode.RIGHT){
            if(!TetrisMechanics.checkCollisionHorizontal(pieceCords, true)) {
                removeGridChildren();
                pieceCords[0]++;
                pieceCords[2]++;
                pieceCords[4]++;
                pieceCords[6]++;
                addGridChildren();
            }
        }
        if(key.getCode()== KeyCode.LEFT){
            if(!TetrisMechanics.checkCollisionHorizontal(pieceCords, false)) {
                removeGridChildren();
                pieceCords[0]--;
                pieceCords[2]--;
                pieceCords[4]--;
                pieceCords[6]--;
                addGridChildren();
            }
        }
        at.start();
    }

    public void newBlockEnters(){
        currentBlock=TetrisMechanics.createBlock();
        pieceCords=TetrisPieces.setStartPiece((int) (Math.random() * 7));
        addGridChildren();
    }

    public void removeGridChildren(){
        gamespace.getChildren().remove(currentBlock[0]);
        gamespace.getChildren().remove(currentBlock[1]);
        gamespace.getChildren().remove(currentBlock[2]);
        gamespace.getChildren().remove(currentBlock[3]);
    }

    public void addGridChildren(){
        gamespace.add(currentBlock[0], pieceCords[0], pieceCords[1]);
        gamespace.add(currentBlock[1], pieceCords[2], pieceCords[3]);
        gamespace.add(currentBlock[2], pieceCords[4], pieceCords[5]);
        gamespace.add(currentBlock[3], pieceCords[6], pieceCords[7]);
    }
}
