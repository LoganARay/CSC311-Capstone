package edu.farmingdale.csc311capstone.Bomberman;

public class GameMap {
    public static final int EMPTY = 0;
    public static final int WALL = 1;
    public static final int BLOCK = 2;

    private final int[][] grid = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 2, 0, 2, 0, 2, 0, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
            {1, 2, 0, 2, 0, 0, 2, 0, 2, 0, 1},
            {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 2, 0, 2, 0, 0, 2, 0, 2, 1},
            {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1},
            {1, 0, 0, 2, 0, 2, 0, 0, 2, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    public int[][] getGrid() {
        return grid;
    }
}
