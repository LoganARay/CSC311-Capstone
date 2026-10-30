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

    public boolean isInside(int row, int column) {
        return row >= 0 && row < grid.length
                && column >= 0 && column < grid[row].length;
    }

    public int countBlocks() {
        int count = 0;
        for (int[] row : grid) {
            for (int tile : row) {
                if (tile == BLOCK) {
                    count++;
                }
            }
        }
        return count;
    }

    public int[][] getGrid() {
        return grid;
    }
}
