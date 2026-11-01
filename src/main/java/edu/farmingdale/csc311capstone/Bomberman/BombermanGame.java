package edu.farmingdale.csc311capstone.Bomberman;

import java.util.ArrayList;
import java.util.List;

public class BombermanGame {
    private boolean running;
    private boolean gameOver;
    private boolean won;
    // Each entry stores an explosion square as {row, column}.
    private final List<int[]> explosionCells = new ArrayList<>();
    private double explosionSecondsRemaining;
    private Player player;
    private GameMap map;
    private final List<Bomb> bombs = new ArrayList<>();

    public BombermanGame() {
        initializeGame();
    }

    public void initializeGame() {
        player = new Player(1, 1);
        map = new GameMap();
        bombs.clear();
        running = false;
        gameOver = false;
        won = false;
        explosionCells.clear();
        explosionSecondsRemaining = 0;
    }

    public void startGame() {
        if (!gameOver && !won) {
            running = true;
        }
    }

    public void movePlayer(int rowChange, int columnChange) {
        if (!running || Math.abs(rowChange) + Math.abs(columnChange) != 1) {
            return;
        }

        int row = player.getRow() + rowChange;
        int column = player.getColumn() + columnChange;
        if (!map.isInside(row, column)
                || map.getGrid()[row][column] != GameMap.EMPTY) {
            return;
        }

        // The player can leave a newly placed bomb, but cannot walk back onto it.
        for (Bomb bomb : bombs) {
            if (bomb.getRow() == row && bomb.getColumn() == column) {
                return;
            }
        }

        player.setRow(row);
        player.setColumn(column);
        checkPlayerHit();
    }

    public boolean isRunning() {
        return running;
    }

    public void updateGame(double elapsedSeconds) {
        if (!running || !Double.isFinite(elapsedSeconds) || elapsedSeconds <= 0) {
            return;
        }

        if (!explosionCells.isEmpty()) {
            explosionSecondsRemaining -= elapsedSeconds;
            if (explosionSecondsRemaining <= 0) {
                explosionCells.clear();
                if (map.countBlocks() == 0) {
                    won = true;
                    running = false;
                }
            }
        } else if (!bombs.isEmpty()) {
            Bomb bomb = bombs.get(0);
            bomb.update(elapsedSeconds);
            if (bomb.isReadyToExplode()) {
                bombs.clear();
                explodeBomb(bomb);
            }
        }
    }

    public void placeBomb() {
        if (running && bombs.isEmpty() && explosionCells.isEmpty()) {
            bombs.add(new Bomb(player.getRow(), player.getColumn()));
        }
    }

    private void explodeBomb(Bomb bomb) {
        explosionCells.clear();
        explosionCells.add(new int[]{bomb.getRow(), bomb.getColumn()});
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] direction : directions) {
            for (int distance = 1; distance <= 2; distance++) {
                int row = bomb.getRow() + direction[0] * distance;
                int column = bomb.getColumn() + direction[1] * distance;
                if (!map.isInside(row, column)
                        || map.getGrid()[row][column] == GameMap.WALL) {
                    break;
                }

                explosionCells.add(new int[]{row, column});
                if (map.getGrid()[row][column] == GameMap.BLOCK) {
                    map.getGrid()[row][column] = GameMap.EMPTY;
                    break;
                }
            }
        }

        explosionSecondsRemaining = 0.4;
        checkPlayerHit();
    }

    private void checkPlayerHit() {
        for (int[] cell : explosionCells) {
            if (player.getRow() == cell[0] && player.getColumn() == cell[1]) {
                gameOver = true;
                running = false;
                return;
            }
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean hasWon() {
        return won;
    }

    public List<int[]> getExplosionCells() {
        return explosionCells;
    }

    public void resetGame() {
        initializeGame();
    }

    public Player getPlayer() {
        return player;
    }

    public GameMap getMap() {
        return map;
    }

    public List<Bomb> getBombs() {
        return bombs;
    }
}
