package edu.farmingdale.csc311capstone.Pacman;

public class PacmanMechanics {

    public static final char WALL = '#';
    public static final char PELLET = '.';
    public static final char EMPTY = ' ';

    private static final int POINTS_PER_PELLET = 10;

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

    private final int startRow;
    private final int startCol;

    private int playerRow;
    private int playerCol;

    private int score;
    private int remainingPellets;
    private boolean gameWon;

    public PacmanMechanics() {

        maze = new char[MAZE_LAYOUT.length][MAZE_LAYOUT[0].length()];

        int foundRow = -1;
        int foundCol = -1;

        for (int row = 0; row < MAZE_LAYOUT.length; row++) {

            if (MAZE_LAYOUT[row].length() != MAZE_LAYOUT[0].length()) {
                throw new IllegalArgumentException("Invalid maze width");
            }

            for (int col = 0; col < MAZE_LAYOUT[row].length(); col++) {

                char tile = MAZE_LAYOUT[row].charAt(col);

                if (tile == 'P') {
                    foundRow = row;
                    foundCol = col;
                    maze[row][col] = EMPTY;
                } else {
                    maze[row][col] = tile;
                }

                if (tile == PELLET) {
                    remainingPellets++;
                }
            }
        }

        if (foundRow == -1) {
            throw new IllegalStateException("Pac-Man start not found");
        }

        startRow = foundRow;
        startCol = foundCol;

        playerRow = startRow;
        playerCol = startCol;
    }

    public boolean movePlayer(int rowChange, int colChange) {

        if (gameWon) {
            return false;
        }

        if (Math.abs(rowChange) + Math.abs(colChange) != 1) {
            return false;
        }

        int newRow = playerRow + rowChange;
        int newCol = playerCol + colChange;

        if (isWall(newRow, newCol)) {
            return false;
        }

        playerRow = newRow;
        playerCol = newCol;

        collectPellet();

        return true;
    }

    private void collectPellet() {

        if (maze[playerRow][playerCol] != PELLET) {
            return;
        }

        maze[playerRow][playerCol] = EMPTY;

        score += POINTS_PER_PELLET;
        remainingPellets--;

        if (remainingPellets == 0) {
            gameWon = true;
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

        playerRow = startRow;
        playerCol = startCol;

        score = 0;
        gameWon = false;
    }

    public boolean isWall(int row, int col) {

        if (row < 0 || row >= getRows() ||
                col < 0 || col >= getColumns()) {
            return true;
        }

        return maze[row][col] == WALL;
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

    public char getTile(int row, int col) {
        return maze[row][col];
    }

    public int getScore() {
        return score;
    }

    public int getRemainingPellets() {
        return remainingPellets;
    }

    public boolean isGameWon() {
        return gameWon;
    }
}
