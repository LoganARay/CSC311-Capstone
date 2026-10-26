package edu.farmingdale.csc311capstone.Pacman;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class PacmanMechanics {

    public static final char WALL = '#';
    public static final char PELLET = '.';
    public static final char EMPTY = ' ';

    private static final int POINTS_PER_PELLET = 10;

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private static final int[][] GHOST_STARTS = {
            {11, 15}, {5, 13}
    };

    private static final String[] MAZE_LAYOUT = {
            "#################",
            "#P..............#",
            "#.###.###.###.#.#",
            "#...............#",
            "###.###.#.###.###",
            "#.....#...#.....#",
            "#.###.#.#.#.###.#",
            "#.....#...#.....#",
            "###.###.#.###.###",
            "#...............#",
            "#.###.###.###.#.#",
            "#...............#",
            "#################"
    };

    private final char[][] maze;
    private final int[][] ghosts = new int[GHOST_STARTS.length][2];
    private final Random random = new Random();

    private final int startRow;
    private final int startCol;

    private int playerRow;
    private int playerCol;

    private int score;
    private int remainingPellets;
    private int lives;

    private boolean gameWon;
    private boolean gameOver;

    public PacmanMechanics() {

        maze = new char[MAZE_LAYOUT.length][MAZE_LAYOUT[0].length()];

        int foundRow = -1;
        int foundCol = -1;

        for (int row = 0; row < MAZE_LAYOUT.length; row++) {

            if (MAZE_LAYOUT[row].length() != MAZE_LAYOUT[0].length()) {
                throw new IllegalArgumentException("Invalid maze width");
            }

            for (int col = 0; col < MAZE_LAYOUT[row].length(); col++) {

                if (MAZE_LAYOUT[row].charAt(col) == 'P') {
                    foundRow = row;
                    foundCol = col;
                }
            }
        }

        if (foundRow == -1) {
            throw new IllegalStateException("Pac-Man start not found");
        }

        startRow = foundRow;
        startCol = foundCol;

        resetGame();

        for (int[] spawn : GHOST_STARTS) {
            if (isWall(spawn[0], spawn[1])) {
                throw new IllegalStateException(
                        "Ghost spawn is inside a wall"
                );
            }
        }
    }

    public boolean movePlayer(int rowChange, int colChange) {

        if (isFinished() ||
                Math.abs(rowChange) + Math.abs(colChange) != 1) {
            return false;
        }

        int newRow = playerRow + rowChange;
        int newCol = playerCol + colChange;

        if (isWall(newRow, newCol)) {
            return false;
        }

        playerRow = newRow;
        playerCol = newCol;

        if (maze[playerRow][playerCol] == PELLET) {

            maze[playerRow][playerCol] = EMPTY;
            score += POINTS_PER_PELLET;
            remainingPellets--;

            if (remainingPellets == 0) {
                gameWon = true;
            }
        }

        return true;
    }

    public void moveGhosts() {

        if (isFinished()) {
            return;
        }

        int[][] distances = distancesToPlayer();

        for (int i = 0; i < ghosts.length; i++) {

            List<int[]> options = new ArrayList<>();

            for (int[] direction : DIRECTIONS) {

                int row = ghosts[i][0] + direction[0];
                int col = ghosts[i][1] + direction[1];

                if (!isWall(row, col)) {
                    options.add(new int[]{row, col});
                }
            }

            if (options.isEmpty()) {
                continue;
            }

            Collections.shuffle(options, random);

            int[] next = options.get(0);

            boolean chase = i == 0 || random.nextDouble() < 0.65;

            if (chase) {

                for (int[] option : options) {

                    if (distances[option[0]][option[1]] <
                            distances[next[0]][next[1]]) {
                        next = option;
                    }
                }
            }

            ghosts[i][0] = next[0];
            ghosts[i][1] = next[1];
        }
    }

    private int[][] distancesToPlayer() {

        int[][] distances = new int[getRows()][getColumns()];

        for (int[] row : distances) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }

        Queue<int[]> queue = new ArrayDeque<>();

        distances[playerRow][playerCol] = 0;
        queue.add(new int[]{playerRow, playerCol});

        while (!queue.isEmpty()) {

            int[] current = queue.remove();

            for (int[] direction : DIRECTIONS) {

                int row = current[0] + direction[0];
                int col = current[1] + direction[1];

                if (!isWall(row, col) &&
                        distances[row][col] == Integer.MAX_VALUE) {

                    distances[row][col] =
                            distances[current[0]][current[1]] + 1;

                    queue.add(new int[]{row, col});
                }
            }
        }

        return distances;
    }

    public boolean checkGhostCollision() {

        if (isFinished()) {
            return false;
        }

        for (int[] ghost : ghosts) {

            if (ghost[0] == playerRow &&
                    ghost[1] == playerCol) {

                lives--;

                if (lives == 0) {
                    gameOver = true;
                } else {
                    resetPositions();
                }

                return true;
            }
        }

        return false;
    }

    private void resetPositions() {

        playerRow = startRow;
        playerCol = startCol;

        for (int i = 0; i < ghosts.length; i++) {

            ghosts[i][0] = GHOST_STARTS[i][0];
            ghosts[i][1] = GHOST_STARTS[i][1];
        }
    }

    public void resetGame() {

        remainingPellets = 0;

        for (int row = 0; row < MAZE_LAYOUT.length; row++) {

            for (int col = 0; col < MAZE_LAYOUT[row].length(); col++) {

                char tile = MAZE_LAYOUT[row].charAt(col);

                maze[row][col] = tile == 'P' ? EMPTY : tile;

                if (tile == PELLET) {
                    remainingPellets++;
                }
            }
        }

        score = 0;
        lives = 3;

        gameWon = false;
        gameOver = false;

        resetPositions();
    }

    public boolean isWall(int row, int col) {

        return row < 0 || row >= getRows() ||
                col < 0 || col >= getColumns() ||
                maze[row][col] == WALL;
    }

    public int getRows() {
        return maze.length;
    }

    public int getColumns() {
        return maze[0].length;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }

    public int getPlayerRow() {
        return playerRow;
    }

    public int getPlayerCol() {
        return playerCol;
    }

    public int getGhostCount() {
        return ghosts.length;
    }

    public int getGhostRow(int index) {
        return ghosts[index][0];
    }

    public int getGhostCol(int index) {
        return ghosts[index][1];
    }

    public char getTile(int row, int col) {
        return maze[row][col];
    }

    public int getScore() {
        return score;
    }

    public int getRemainingPellets() {
        return remainingPellets;
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameWon() {
        return gameWon;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isFinished() {
        return gameWon || gameOver;
    }
}
