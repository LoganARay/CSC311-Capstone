package edu.farmingdale.csc311capstone.Tetris;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TetrisMechanics {
    private List<Integer> blockBag;

    public TetrisMechanics(){
        blockBag= new ArrayList<>();
    }

    public int getBlock(){
        if(blockBag.size()==0){
            blockBag= IntStream.rangeClosed(0, 6).boxed().collect(Collectors.toCollection(ArrayList::new));
        }
        if(blockBag.size()==7){
            for(int i=0; i<=21; i++){ //swaps blocks around randomly 21 times.
                int pos1= (int) (Math.random()*7);
                int pos2= (int) (Math.random()*7);
                int swapBlock= blockBag.get(pos2);
                blockBag.set(pos2, blockBag.get(pos1));
                blockBag.set(pos1, swapBlock);
            }
        }
        return blockBag.removeFirst();
    }
    public static boolean checkCollisionDown(int[] block){
        if(block[1]==23 || block[3]==23 || block[5]==23 || block[7]==23){
            return true;
        }
        for(int i=0; i<8; i=i+2){
            if(TetrisController.grid[block[i]][block[i+1]+1]){
                return true;
            }
        }
        return false;
    }

    //for direction; true=right; false=left
    public static boolean checkCollisionHorizontal(int[] block, boolean direction){
        int change=-1;
        int bound=0;
        //changes to right directions, if not then change and bound are already set for left direction
        if(direction){
            change=1;
            bound=9;
        }

        if(block[0]==bound || block[2]==bound || block[4]==bound || block[6]==bound){
            return true;
        }
        for(int i=0; i<8; i=i+2) {
            if (TetrisController.grid[block[i]+change][block[i+1]]) {
                return true;
            }
        }
        return false;
    }

    public static void setCollision(int[] block){
        TetrisController.grid[block[0]][block[1]]=true;
        TetrisController.grid[block[2]][block[3]]=true;
        TetrisController.grid[block[4]][block[5]]=true;
        TetrisController.grid[block[6]][block[7]]=true;

    }

    public static ImageView[] createBlock(int block){
        ImageView[] newBlockImg= new ImageView[4];
        Image image = setGraphic(block);
        ImageView demo = new ImageView(image); ImageView demo1 = new ImageView(image); ImageView demo2 = new ImageView(image); ImageView demo3 = new ImageView(image);
        newBlockImg[0]=demo; newBlockImg[1]=demo1; newBlockImg[2]=demo2; newBlockImg[3]=demo3;
        for(int i=0; i<4; i++){
            newBlockImg[i].setFitWidth(20.0);
            newBlockImg[i].setFitHeight(20.0);
        }
        return newBlockImg;
    }

    public static Image setGraphic(int block){
        Image currentGraphic = new Image(TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/Test-block.png"));
        switch(block){
            case 0->{//o-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/o-block.png")
                );
            }
            case 1->{//l-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/l-block.png")
                );
            }
            case 2->{//j-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/j-block.png")
                );
            }
            case 3->{//s-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/s-block.png")
                );
            }
            case 4->{//z-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/z-block.png")
                );
            }
            case 5->{//t-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/t-block.png")
                );
            }
            case 6->{//i-block
                currentGraphic= new Image(
                        TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/BlockGraphics/i-block.png")
                );
            }
            default->{}
        }
        return currentGraphic;
    }


    public static List<Integer> linesCleared(boolean[][] placedBlocks){
        List<Integer> fullLines= new ArrayList<Integer>();
        int rowCount=0;
        for(int r=0; r<=23; r++){
            boolean cleared=true;
            for(int i=0; i<10; i++){
                if(!placedBlocks[i][r]){
                    cleared=false;
                    break;
                }

            }
            if(cleared){ fullLines.add(r);}
        }
        return fullLines;
    }

    public static boolean[][] fixGrid(boolean[][] currentGrid, List<Integer> fullLines){
        for(int r:fullLines) {
            for(int p=0; p<10; p++){
                for(int i=r; i>0; i--){
                    currentGrid[p][i]=currentGrid[p][i-1];} }
                }
        return currentGrid;
    }

    public static GridPane fixBoard(GridPane currentGame, List<Integer> fullLines){
        for(int l: fullLines) {
            currentGame.getChildren().removeIf(node -> {
                Integer row = GridPane.getRowIndex(node);
                int r = (row == null) ? 0 : row;
                return r == l;
            });

            for (Node node : currentGame.getChildren()) {
                Integer row = GridPane.getRowIndex(node);
                int r = (row == null) ? 0 : row;
                if (r < l) {
                    GridPane.setRowIndex(node, r +1);
                }
            }
            System.out.println(currentGame.getRowCount());
        }
//        currentGame.getChildren().stream().map(node ->{
//            node.setStyle(
//                "-fx-background-color: #181825;" +
//                "-fx-border-color: #313244;" +
//                "-fx-opacity: 0.8;" +
//                "-fx-border-width: 0 0 1 0;"
//            );
//        }).collect(Collectors.toCollection(Node));
        Pane firstRowPane = new Pane();
        firstRowPane.setStyle("-fx-background-color: #181825;" +
                "-fx-border-color: #313244;" +
                "-fx-opacity: 0.8;" +
                "-fx-border-width: 0 0 1 0;");
        currentGame.add(firstRowPane,0,4,currentGame.getColumnCount(), 1);
        return currentGame;
    }
}
