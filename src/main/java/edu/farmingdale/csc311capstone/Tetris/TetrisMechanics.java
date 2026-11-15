package edu.farmingdale.csc311capstone.Tetris;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TetrisMechanics {
    public static boolean checkCollisionDown(int[] block){
        if(block[1]==19 || block[3]==19 || block[5]==19 || block[7]==19){
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
        //changes to right directions, if not then chnage and bound are already set for left direction
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
        Image currentGraphic = new Image(TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/o-block.png"));
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
//        return IntStream.range(0, placedBlocks.length)
//        .filter(l->{
//            for (boolean value : placedBlocks[l]) {
//                if (!value) {return false;}
//            }
//            return true;
//        }).boxed().collect(Collectors.toList());
        List<Integer> fullLines= new ArrayList<Integer>();
        int rowCount=0;
        for(int r=0; r<=19; r++){
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
                    GridPane.setRowIndex(node, r + 1);
                }
            }
        }
        return currentGame;
    }
}
