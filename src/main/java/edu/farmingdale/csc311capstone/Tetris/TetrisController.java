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

import java.util.List;

public class TetrisController {
    private AnimationTimer at;
    private ImageView[] currentBlock;
    public static boolean[][] grid;
    private int blockNumber;
    private static int[] pieceCords;
    private double snapInterval =1.0;
    private double fastSnapInterval;
    private double baseSnapInterval;
    private TetrisMechanics tm;


    @FXML
    private HBox root;

    @FXML
    private GridPane gamespace;

    @FXML
    private void initialize(){
        tm= new TetrisMechanics();
        root.setFocusTraversable(true);
        gamespace.setTranslateY(-80.0);
        gamespace.setPickOnBounds(true);
        grid= new boolean[10][24];
        int[] hz_spinSetUpL= {4,23,6,22};
        int[] hs_spinSetUpL= {6,23,4,22};
        int[] wallkick_s_setUp= {6,23,7,23,7,22,7,21,4,21,4,22};
        int[] wallkick_z_setUp= {4,23,6,22,3,23,3,22,3,21,6,21};
        int[] triple_s_setUp= {4,23,5,21,3,23,3,22,3,21,6,23,6,22,6,21,6,20};
        int[] triple_z_setUp= {5,23,4,21,6,23,6,22,6,21,3,23,3,22,3,21,3,20};

        int[] tspinSetUp={4,19,4,18,4,17,4,16,4,15,5,15,6,19,6,17,7,19,7,18,7,17,8,19,8,18,8,17,8,16,8,15,8,14,5,14};
        demoHelper(triple_z_setUp);
        newBlockEnters();

        baseSnapInterval=snapInterval;
        at= new AnimationTimer() {
            private long lastUpdate = 0;
            private double accumulatedTime = 0.0; // Tracks time between snaps
            private double lockDelayAccumulator = 0.0;
            private final double lockDelayTime = 1.0;
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

                    fastSnapInterval = snapInterval / 4.0;

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
                        List<Integer> linesCleared= TetrisMechanics.linesCleared(grid);
                        if(!linesCleared.isEmpty()){
                            grid= TetrisMechanics.fixGrid(grid, linesCleared);
                            gamespace= TetrisMechanics.fixBoard(gamespace, linesCleared);
                        }
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
        if(key.getCode()==KeyCode.L){
            at.stop();
            pieceCords=TetrisPieces.helpRotateL(pieceCords, grid, blockNumber);
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
        blockNumber= tm.getBlock();
        currentBlock=TetrisMechanics.createBlock(blockNumber);
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

    public void demoHelper(int[] setUp){
        for(int i=0; i<setUp.length; i=i+2){
            Image image = new Image(
                    TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/Test-block.png")
            );
            ImageView demoBlocks= new ImageView(image);
            demoBlocks.setFitWidth(20.0);
            demoBlocks.setFitHeight(20.0);
            gamespace.add(demoBlocks, setUp[i], setUp[i+1]);
            grid[setUp[i]][setUp[i+1]]=true;
        }
    }
}
