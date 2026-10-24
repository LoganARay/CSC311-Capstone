package edu.farmingdale.csc311capstone.Tetris;

public class TetrisPieces {
    public static int[] setStartPiece(int num){
        int[] temp= new int[8];
        switch(num){
            case 0 -> { //o-block
                temp[0]=4; temp[1]=0;
                temp[2]=5; temp[3]=0;
                temp[4]=4; temp[5]=1;
                temp[6]=5; temp[7]=1;
            }
            case 1 -> { //l-block
                temp[0]=4; temp[1]=0;
                temp[2]=4; temp[3]=1;
                temp[4]=4; temp[5]=2;
                temp[6]=5; temp[7]=2;
            }
            case 2 -> { //rl-block
                temp[0]=5; temp[1]=0;
                temp[2]=5; temp[3]=1;
                temp[4]=5; temp[5]=2;
                temp[6]=4; temp[7]=2;
            }
            case 3 -> { //s-block
                temp[0]=4; temp[1]=0;
                temp[2]=4; temp[3]=1;
                temp[4]=5; temp[5]=1;
                temp[6]=5; temp[7]=2;
            }
            case 4 -> { //z-block
                temp[0]=5; temp[1]=0;
                temp[2]=5; temp[3]=1;
                temp[4]=4; temp[5]=1;
                temp[6]=4; temp[7]=2;
            }
            case 5 -> { //t-block
                temp[0]=4; temp[1]=0;
                temp[2]=5; temp[3]=0;
                temp[4]=6; temp[5]=0;
                temp[6]=5; temp[7]=1;
            }
            case 6 -> { //i-block
                temp[0]=4; temp[1]=0;
                temp[2]=4; temp[3]=1;
                temp[4]=4; temp[5]=2;
                temp[6]=4; temp[7]=3;
            }

            default ->{ }
        }

        return temp;
    }
}
