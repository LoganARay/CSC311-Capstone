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
    private int blockNumber;
    private static int[] pieceCords;
    private double snapInterval =1.0;
    private double fastSnapInterval;
    private double baseSnapInterval;


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
        baseSnapInterval=snapInterval;
        ImageView[] newBlockImg= new ImageView[7];
        Image image = new Image(
                TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/o-block.png")
        );
        ImageView demo = new ImageView(image); ImageView demo1 = new ImageView(image); ImageView demo2 = new ImageView(image); ImageView demo3 = new ImageView(image);
        ImageView demo4 = new ImageView(image); ImageView demo5 = new ImageView(image); ImageView demo6 = new ImageView(image);
        newBlockImg[0]=demo; newBlockImg[1]=demo1; newBlockImg[2]=demo2; newBlockImg[3]=demo3; newBlockImg[4]=demo4; newBlockImg[5]=demo5; newBlockImg[6]=demo6;
        for(int i=0; i<7; i++){
            newBlockImg[i].setFitWidth(20.0);
            newBlockImg[i].setFitHeight(20.0);
        }
        gamespace.add(newBlockImg[0], 4, 19);
        gamespace.add(newBlockImg[1], 5, 19);
        gamespace.add(newBlockImg[2], 4, 17);
        gamespace.add(newBlockImg[3], 5, 17);
        gamespace.add(newBlockImg[4], 7, 17);
        gamespace.add(newBlockImg[5], 7, 18);
        gamespace.add(newBlockImg[6], 7, 19);
        gamespace.add(newBlockImg[7], 7, 17);
        gamespace.add(newBlockImg[8], 7, 18);
        gamespace.add(newBlockImg[9], 7, 19);
        grid[4][19]=true; grid[5][19]=true; grid[4][17]=true; grid[5][17]=true;
         grid[7][18]=true; grid[7][17]=true;
        at= new AnimationTimer() {
            private long lastUpdate = 0;
            private double accumulatedTime = 0.0; // Tracks time between snaps
            private double lockDelayAccumulator = 0.0;
            private final double lockDelayTime = 2.0;
            private final double gridStep = 20.0;    // Number of pixels to snap down each time

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                if(!TetrisMechanics.checkCollisionDown(pieceCords)) {
                    lockDelayAccumulator = 0.0;

                    fastSnapInterval = snapInterval / 2.0;

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
                    lockDelayAccumulator += elapsedSeconds;
                    if (lockDelayAccumulator >= lockDelayTime) {
                        stop();
                        TetrisMechanics.setCollision(pieceCords);
                        newBlockEnters();
                        snapInterval = 1.0;
                        at.start();
                    }
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
                    newScene.addEventFilter(
                            KeyEvent.KEY_RELEASED,
                            this::keyReleased
                    );
                }
        );
    }

    private void keyPressed(KeyEvent key){
        if(key.getCode()== KeyCode.RIGHT){
            if(!TetrisMechanics.checkCollisionHorizontal(pieceCords, true)) {
                at.stop();
                removeGridChildren();
                pieceCords[0]++;
                pieceCords[2]++;
                pieceCords[4]++;
                pieceCords[6]++;
                addGridChildren();
                at.start();
            }
        }
        if(key.getCode()== KeyCode.LEFT){
            if(!TetrisMechanics.checkCollisionHorizontal(pieceCords, false)) {
                at.stop();
                removeGridChildren();
                pieceCords[0]--;
                pieceCords[2]--;
                pieceCords[4]--;
                pieceCords[6]--;
                addGridChildren();
                at.start();
            }
        }
        if(key.getCode()==KeyCode.UP){
            at.stop();
            while(!TetrisMechanics.checkCollisionDown(pieceCords)){
                pieceCords[1]++; pieceCords[3]++; pieceCords[5]++; pieceCords[7]++;
            }
            removeGridChildren();
            addGridChildren();
            at.start();
        }
        if(key.getCode()==KeyCode.DOWN){
            at.stop();
            snapInterval=fastSnapInterval;
            at.start();
        }
        if(key.getCode()==KeyCode.R){
            at.stop();
            pieceCords=TetrisPieces.helpRotateR(pieceCords, grid, blockNumber);
            removeGridChildren();
            addGridChildren();
            at.start();
        }
    }

    private void keyReleased(KeyEvent key){
        if(key.getCode()==KeyCode.DOWN){
            at.stop();
            snapInterval=baseSnapInterval;
            at.start();
        }
    }

    public void newBlockEnters(){
        currentBlock=TetrisMechanics.createBlock();
        blockNumber=(int) (6);
        pieceCords=TetrisPieces.setStartPiece(blockNumber);
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
