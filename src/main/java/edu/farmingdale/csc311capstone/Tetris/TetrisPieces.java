package edu.farmingdale.csc311capstone.Tetris;

public class TetrisPieces {
    public static int[] setStartPiece(int num){
        int[] temp= new int[9];
        switch(num){
            case 0 -> { //o-block
                temp[0]=4; temp[1]=0;// ⬛⬜⬜⬛
                temp[2]=5; temp[3]=0;// ⬛⬜⬜⬛
                temp[4]=4; temp[5]=1;// ⬛⬛⬛⬛
                temp[6]=5; temp[7]=1;// ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 1 -> { //l-block
                temp[0]=3; temp[1]=0;// ⬜⬜⬜⬛
                temp[2]=4; temp[3]=0;// ⬜⬛⬛⬛
                temp[4]=5; temp[5]=0;// ⬛⬛⬛⬛
                temp[6]=3; temp[7]=1;// ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 2 -> { //j-block
                temp[0]=5; temp[1]=1;// ⬜⬜⬜⬛
                temp[2]=5; temp[3]=0;// ⬛⬛⬜⬛
                temp[4]=4; temp[5]=0;// ⬛⬛⬛⬛
                temp[6]=3; temp[7]=0;// ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 3 -> { //s-block
                temp[0]=5; temp[1]=0;//  ⬛⬜⬜⬛
                temp[2]=4; temp[3]=0;//  ⬜⬜⬛⬛
                temp[4]=4; temp[5]=1;//  ⬛⬛⬛⬛
                temp[6]=3; temp[7]=1;//  ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 4 -> { //z-block
                temp[0]=3; temp[1]=0;// ⬜⬜⬛⬛
                temp[2]=4; temp[3]=0;// ⬛⬜⬜⬛
                temp[4]=4; temp[5]=1;// ⬛⬛⬛⬛
                temp[6]=5; temp[7]=1;// ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 5 -> { //t-block
                temp[0]=3; temp[1]=0;// ⬜⬜⬜⬛
                temp[2]=4; temp[3]=0;// ⬛⬜⬛⬛
                temp[4]=5; temp[5]=0;// ⬛⬛⬛⬛
                temp[6]=4; temp[7]=1;// ⬛⬛⬛⬛
                temp[8]=0;
            }
            case 6 -> { //i-block
                temp[0]=3; temp[1]=0;// ⬜⬜⬜⬜
                temp[2]=4; temp[3]=0;// ⬛⬛⬛⬛
                temp[4]=5; temp[5]=0;// ⬛⬛⬛⬛
                temp[6]=6; temp[7]=0;// ⬛⬛⬛⬛
                temp[8]=0;
            }

            default ->{ }
        }

        return temp;
    }

    public static int[] helpRotateR(int[] blockCords, boolean[][] grid, int block){
        int[] temp= new int[9];
        switch(block){
            case 0->{ //o-block
                //You are too stupid to rotate idiot!
            }
            case 1->{ //l-block
                    switch (blockCords[8]) {
                        //⬛⬛⬛⬛
                        //⬜⬜⬜⬛
                        //⬜⬛⬛⬛
                        //⬛⬛⬛⬛
                        case 0 -> {
                            temp[0]=blockCords[0]+2; temp[1]=blockCords[1]-1;   //⬛⬜⬜⬛
                            temp[2]=blockCords[2]+1; temp[3]=blockCords[3];     //⬛⬛⬜⬛
                            temp[4]=blockCords[4]; temp[5]=blockCords[5]+1;     //⬛⬛⬜⬛
                            temp[6]=blockCords[6]+1; temp[7]=blockCords[7]-2;   //⬛⬛⬛⬛
                            temp[8]=1;
                            if(isGridOccupied(grid, temp)){
                                temp[1]--; temp[3]--; temp[5]--; temp[7]--;
                                if(isGridOccupied(grid, temp)){
                                    return blockCords;
                                }
                            }
                        }
                        case 1->{
                            temp[0]=blockCords[0]; temp[1]=blockCords[1]+2;     //⬛⬛⬛⬛
                            temp[2]=blockCords[2]-1; temp[3]=blockCords[3]+1;   //⬛⬛⬜⬛
                            temp[4]=blockCords[4]-2; temp[5]=blockCords[5];     //⬜⬜⬜⬛
                            temp[6]=blockCords[6]+1; temp[7]=blockCords[7]+1;   //⬛⬛⬛⬛
                            temp[8]=2;
                            temp=checkBoundRight(temp);

                        }
                        case 2->{
                            temp[0]=blockCords[0]-2; temp[1]=blockCords[1];     //⬜⬛⬛⬛
                            temp[2]=blockCords[2]-1; temp[3]=blockCords[3]-1;   //⬜⬛⬛⬛
                            temp[4]=blockCords[4]; temp[5]=blockCords[5]-2;     //⬜⬜⬛⬛
                            temp[6]=blockCords[6]-1; temp[7]=blockCords[7]+1;   //⬛⬛⬛⬛
                            temp[8]=3;
                            if(isGridOccupied(grid, temp)){
                                return blockCords;
                            }
                        }
                        case 3->{
                            temp[0]=blockCords[0]; temp[1]=blockCords[1]-1;     //⬛⬛⬛⬛
                            temp[2]=blockCords[2]+1; temp[3]=blockCords[3];     //⬜⬜⬜⬛
                            temp[4]=blockCords[4]+2; temp[5]=blockCords[5]+1;   //⬜⬛⬛⬛
                            temp[6]=blockCords[6]-1; temp[7]=blockCords[7];     //⬛⬛⬛⬛
                            temp[8]=0;
                            temp=checkBoundRight(temp);
                            while(isGridOccupied(grid, temp)){
                                temp[1]--; temp[3]--; temp[5]--; temp[7]--;
                            }
                        }
                        default ->{}
                    }

            }
            //⬛⬛⬛⬛
            //⬜⬜⬜⬛
            //⬛⬛⬜⬛
            //⬛⬛⬛⬛
            case 2->{//j-block
                switch(blockCords[8]){
                    case 0 ->{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1];   //⬛⬛⬜⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3]+1;   //⬛⬛⬜⬛
                        temp[4]=blockCords[4]+1; temp[5]=blockCords[5];   //⬛⬜⬜⬛
                        temp[6]=blockCords[6]+2; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=1;
                    }
                    case 1 ->{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1]-1; //⬛⬛⬛⬛
                        temp[2]=blockCords[2]-2; temp[3]=blockCords[3];   //⬜⬛⬛⬛
                        temp[4]=blockCords[4]-1; temp[5]=blockCords[5]+1; //⬜⬜⬜⬛
                        temp[6]=blockCords[6]; temp[7]=blockCords[7]+2;   //⬛⬛⬛⬛
                        temp[8]=2;
                        temp=checkBoundLeft(temp);
                    }
                    case 2 ->{
                        temp[0]=blockCords[0]+2; temp[1]=blockCords[1]-1; //⬛⬜⬜⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]-2; //⬛⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5]-1;   //⬛⬜⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7];   //⬛⬛⬛⬛
                        temp[8]=3;
                    }
                    case 3 ->{
                        temp[0]=blockCords[0]; temp[1]=blockCords[1]+2;   //⬛⬛⬛⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]+1; //⬜⬜⬜⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬛⬜⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=0;
                        temp=checkBoundLeft(temp);
                    }
                    default ->{}
                }

            }
            case 3->{//s-block
                switch(blockCords[8]){
                    //⬛⬜⬜⬛
                    //⬜⬜⬛⬛
                    //⬛⬛⬛⬛
                    //⬛⬛⬛⬛
                    case 0 ->{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1];   //⬛⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3]+1;   //⬛⬜⬜⬛
                        temp[4]=blockCords[4]+1; temp[5]=blockCords[5];   //⬛⬛⬜⬛
                        temp[6]=blockCords[6]+2; temp[7]=blockCords[7]+1; //⬛⬛⬛⬛
                        temp[8]=1;
                    }
                    case 1 ->{
                        temp[0]=blockCords[0]+1; temp[1]=blockCords[1]+1; //⬛⬛⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬛⬜⬜⬛
                        temp[4]=blockCords[4]-1; temp[5]=blockCords[5]+1; //⬜⬜⬛⬛
                        temp[6]=blockCords[6]-2; temp[7]=blockCords[7];   //⬛⬛⬛⬛
                        temp[8]=2;
                        temp=checkBoundLeft(temp);
                    }
                    case 2 ->{
                        temp[0]=blockCords[0]-2; temp[1]=blockCords[1]-1; //⬜⬛⬛⬛
                        temp[2]=blockCords[2]-1; temp[3]=blockCords[3];   //⬜⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5]-1;   //⬛⬜⬛⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7];   //⬛⬛⬛⬛
                        temp[8]=3;
                    }
                    case 3 ->{
                        temp[0]=blockCords[0]+2; temp[1]=blockCords[1];   //⬛⬜⬜⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]-1; //⬜⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬛⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=0;
                        temp=checkBoundRight(temp);
                    }
                    default ->{}
                }
            }
            case 4->{//z-block
                switch(blockCords[8]){
                    //⬛⬛⬛⬛
                    //⬜⬜⬛⬛
                    //⬛⬜⬜⬛
                    //⬛⬛⬛⬛
                    case 0 ->{
                        temp[0]=blockCords[0]+1; temp[1]=blockCords[1]-1; //⬛⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬜⬜⬛⬛
                        temp[4]=blockCords[4]-1; temp[5]=blockCords[5]-1; //⬜⬛⬛⬛
                        temp[6]=blockCords[6]-2; temp[7]=blockCords[7];   //⬛⬛⬛⬛
                        temp[8]=1;
                    }
                    case 1 ->{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1];   //⬜⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3]-1;   //⬛⬜⬜⬛
                        temp[4]=blockCords[4]+1; temp[5]=blockCords[5];   //⬛⬛⬛⬛
                        temp[6]=blockCords[6]+2; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=2;
                        temp=checkBoundRight(temp);
                    }
                    case 2 ->{
                        temp[0]=blockCords[0]+2; temp[1]=blockCords[1];   //⬛⬛⬜⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]+1; //⬛⬜⬜⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬜⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]+1; //⬛⬛⬛⬛
                        temp[8]=3;
                    }
                    case 3 ->{
                        temp[0]=blockCords[0]-2; temp[1]=blockCords[1]+1; //⬛⬛⬛⬛
                        temp[2]=blockCords[2]-1; temp[3]=blockCords[3];   //⬜⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5]+1;   //⬛⬜⬜⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7];   //⬛⬛⬛⬛
                        temp[8]=0;
                        temp=checkBoundLeft(temp);
                    }
                    default ->{}
                }

            }
            case 5->{//t-block
                switch(blockCords[8]){
                    //⬛⬛⬛⬛⬛⬛⬜⬜⬜⬛⬛⬜
                    //⬜⬜⬜⬛⬛⬛⬜⬜⬛⬛⬛⬜
                    //⬛⬜⬛⬛⬛⬛⬜⬜⬛⬜⬜⬜
                    //⬛⬛⬛⬛⬛⬛⬜⬜⬛⬛⬜⬜
                    case 0 ->{ //⬛⬜⬜⬛⬜⬜⬜
                        temp[0]=blockCords[0]+1; temp[1]=blockCords[1]-1; //⬛⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬜⬜⬛⬛
                        temp[4]=blockCords[4]-1; temp[5]=blockCords[5]+1; //⬛⬜⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=1;
                    }
                    case 1 ->{
                        temp[0]=blockCords[0]+1; temp[1]=blockCords[1]+1; //⬛⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬜⬜⬜⬛
                        temp[4]=blockCords[4]-1; temp[5]=blockCords[5]-1; //⬛⬛⬛⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛
                        temp[8]=2;
                        temp=checkBoundRight(temp);
                        temp=checkBoundLeft(temp);
                        temp=levelOneTSpin(temp, grid);
                    }
                    case 2 ->{
                        if(grid[blockCords[0]+1][blockCords[1]] && grid[blockCords[0]+1][blockCords[1]-1]
                                && grid[blockCords[0]][blockCords[1]+1] && grid[blockCords[2]][blockCords[3]+1]
                                 && grid[blockCords[4]][blockCords[5]-1] && !grid[blockCords[4]][blockCords[5]+1]
                             && !grid[blockCords[4]][blockCords[5]+2] && !grid[blockCords[4]][blockCords[5]+3]
                                && !grid[blockCords[4]+1][temp[5]+2]){
                            temp[0]=blockCords[0]-2; temp[1]=blockCords[1]+3;
                            temp[2]=blockCords[2]-1; temp[3]=blockCords[3] +2;
                            temp[4]=blockCords[4]; temp[5]=blockCords[5]+1;
                            temp[6]=blockCords[6]; temp[7]=blockCords[7]+3;
                            temp[8]=3;

                        }else{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1]+1; //⬛⬜⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬛⬜⬜⬛
                        temp[4]=blockCords[4]+1; temp[5]=blockCords[5]-1; //⬛⬜⬛⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7]+1; //⬛⬛⬛⬛
                        temp[8]=3;
                        }
                    }
                    case 3 ->{
                        temp[0]=blockCords[0]-1; temp[1]=blockCords[1]-1; //⬛⬛⬛⬛
                        temp[2]=blockCords[2]; temp[3]=blockCords[3];     //⬜⬜⬜⬛
                        temp[4]=blockCords[4]+1; temp[5]=blockCords[5]+1; //⬛⬜⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]+1; //⬛⬛⬛⬛
                        temp[8]=0;
                        temp=checkBoundLeft(temp);
                    }
                    default ->{}
                }

            }
            case 6->{//i-block
                switch(blockCords[8]){
                    //⬛⬛⬛⬛⬛
                    //⬛⬛⬛⬛⬛
                    //⬜⬜⬜⬜⬛
                    //⬛⬛⬛⬛⬛
                    //⬛⬛⬛⬛⬛
                    case 0 ->{
                        temp[0]=blockCords[0]+2; temp[1]=blockCords[1]-2; //⬛⬛⬜⬛⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]-1; //⬛⬛⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬛⬜⬛⬛
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]+1; //⬛⬛⬜⬛⬛
                        temp[8]=1;                                        //⬛⬛⬛⬛⬛
                    }
                    case 1 ->{
                        temp[0]=blockCords[0]+2; temp[1]=blockCords[1]+2; //⬛⬛⬛⬛⬛
                        temp[2]=blockCords[2]+1; temp[3]=blockCords[3]+1; //⬛⬛⬛⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬜⬜⬜⬜
                        temp[6]=blockCords[6]-1; temp[7]=blockCords[7]-1; //⬛⬛⬛⬛⬛
                        temp[8]=2;                                        //⬛⬛⬛⬛⬛
                        temp=checkBoundRight(temp);
                        temp=checkBoundRight(temp);
                        temp=checkBoundLeft(temp);
                    }
                    case 2 ->{
                        temp[0]=blockCords[0]-2; temp[1]=blockCords[1]+2; //⬛⬛⬛⬛⬛
                        temp[2]=blockCords[2]-1; temp[3]=blockCords[3]+1; //⬛⬛⬜⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬛⬛⬜⬛⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7]-1; //⬛⬛⬜⬛⬛
                        temp[8]=3;                                        //⬛⬛⬜⬛⬛
                    }
                    case 3 ->{
                        temp[0]=blockCords[0]-2; temp[1]=blockCords[1]-2; //⬛⬛⬛⬛⬛
                        temp[2]=blockCords[2]-1; temp[3]=blockCords[3]-1; //⬛⬛⬛⬛⬛
                        temp[4]=blockCords[4]; temp[5]=blockCords[5];     //⬜⬜⬜⬜⬛
                        temp[6]=blockCords[6]+1; temp[7]=blockCords[7]+1; //⬛⬛⬛⬛⬛
                        temp[8]=0;                                        //⬛⬛⬛⬛⬛
                        temp=checkBoundLeft(temp);
                        temp=checkBoundLeft(temp);
                        temp=checkBoundRight(temp);
                    }
                    default ->{}
                }
            }
        }
        return rotationSafety(grid, temp, blockCords);
    }

    public static boolean isGridOccupied(boolean[][] grid, int[] temp){
        for(int i=0; i<8; i=i+2){
            if(grid[temp[i]][temp[i+1]]){
                return true;
            }
        }
        return false;
    }

    public static int[] checkBoundRight(int[] temp) {
        for (int i = 0; i < 8; i = i + 2) {
            if (temp[i] > 9) {
                temp[0]--;
                temp[2]--;
                temp[4]--;
                temp[6]--;
            }
        }
        return temp;
    }
    public static int[] checkBoundLeft(int[] temp) {
        for (int i = 0; i < 8; i = i + 2) {
            if (temp[i] < 0) {
                temp[0]++;
                temp[2]++;
                temp[4]++;
                temp[6]++;
            }
        }
        return temp;
    }

    public static int[] rotationSafety(boolean[][] grid, int[] temp, int[] og){
        int[] temp2=temp;
        if(isGridOccupied(grid, temp2)){
            for(int i=0; i<3; i++){
                temp2[1]--; temp2[3]--; temp2[5]--; temp2[7]--;
                if(!isGridOccupied(grid, temp2)){
                    return temp;
                }
            }
            temp2=temp;
            temp2[0]--; temp2[2]--; temp2[4]--; temp2[6]--;
            if(!isGridOccupied(grid, temp2)){
                return temp;
            }
            temp2=temp;
            temp2[0]++; temp2[2]++; temp2[4]++; temp2[6]++;
            if(!isGridOccupied(grid, temp2)){
                return temp;
            }
        }
        return temp2;
    }

    public static int[] safetyBound(int[] temp2, int axis, int inc){
        temp2[axis]=temp2[axis]+inc; temp2[axis+2]=temp2[axis+2]+inc; temp2[axis+4]=temp2[axis+4]+inc; temp2[axis+6]=temp2[axis+6]+inc;
        return temp2;
    }

    //
    public static int[] levelOneTSpin(int[] temp, boolean[][] grid){
        if( ( ( (grid[temp[2]][temp[3]+2] && grid[temp[4]][temp[5]+2]) || (temp[1]+1)==19)) && grid[temp[4]-1][temp[5]] && ((temp[0]+1)==10) || (( grid[temp[0]][temp[1]]) && grid[temp[0]][temp[1]+1])){
            for(int i=2; i<8; i=i+2){
                if(grid[temp[i]-1][temp[i+1]+1]){
                    return temp;
                }
            }
            temp[0]--; temp[1]++; temp[2]--; temp[3]++; temp[4]--; temp[5]++; temp[6]--; temp[7]++;
        }
        return temp;
    }
}

