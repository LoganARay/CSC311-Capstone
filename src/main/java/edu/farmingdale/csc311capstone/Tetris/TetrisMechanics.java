package edu.farmingdale.csc311capstone.Tetris;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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

    public static ImageView[] createBlock(){
        ImageView[] newBlockImg= new ImageView[4];
        Image image = new Image(
                TetrisMechanics.class.getResourceAsStream("/edu/farmingdale/csc311capstone/TetrisGraphics/o-block.png")
        );
        ImageView demo = new ImageView(image); ImageView demo1 = new ImageView(image); ImageView demo2 = new ImageView(image); ImageView demo3 = new ImageView(image);
        newBlockImg[0]=demo; newBlockImg[1]=demo1; newBlockImg[2]=demo2; newBlockImg[3]=demo3;
        for(int i=0; i<4; i++){
            newBlockImg[i].setFitWidth(20.0);
            newBlockImg[i].setFitHeight(20.0);
        }
        return newBlockImg;
    }
}
