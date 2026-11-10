package edu.farmingdale.csc311capstone.Bomberman;

import java.util.ArrayList;
import java.util.List;

public class BombermanGame {
    private static final int ENEMY_COLUMN = 9;
    private static final int PATROL_START_ROW = 1;
    private static final int PATROL_END_ROW = 4;
    private static final double PATROL_STEP_SECONDS = 0.5;

    private int enemyRow;
    private int enemyDirection;
    private double enemyStepRemaining;
    private boolean enemyAlive;
    private boolean running;
    private boolean gameOver;
    private boolean won;
    private int score;
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
        enemyRow = PATROL_END_ROW;
        enemyDirection = -1;
        enemyStepRemaining = PATROL_STEP_SECONDS;
        enemyAlive = true;
        map = new GameMap();
        bombs.clear();
        running = false;
        gameOver = false;
        won = false;
        score = 0;
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
        if (hasBombAt(row, column)) {
            return;
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
        if (running && enemyAlive) {
            updateEnemy(elapsedSeconds);
        }
    }

    private void updateEnemy(double elapsedSeconds) {
        enemyStepRemaining -= elapsedSeconds;
        if (enemyStepRemaining > 0) {
            return;
        }
        enemyStepRemaining = PATROL_STEP_SECONDS;
        int nextRow = enemyRow + enemyDirection;
        if (!canPatrolTo(nextRow)) {
            enemyDirection = -enemyDirection;
            nextRow = enemyRow + enemyDirection;
        }
        if (canPatrolTo(nextRow)) {
            enemyRow = nextRow;
            checkEnemyHit();
            checkPlayerHit();
        }
    }

    private boolean canPatrolTo(int row) {
        return row >= PATROL_START_ROW && row <= PATROL_END_ROW
                && map.getGrid()[row][ENEMY_COLUMN] == GameMap.EMPTY
                && !hasBombAt(row, ENEMY_COLUMN);
    }

    private boolean hasBombAt(int row, int column) {
        for (Bomb bomb : bombs) {
            if (bomb.getRow() == row && bomb.getColumn() == column) {
                return true;
            }
        }
        return false;
    }

    public int getEnemyRow() {
        return enemyRow;
    }

    public int getEnemyColumn() {
        return ENEMY_COLUMN;
    }

    public boolean isEnemyAlive() {
        return enemyAlive;
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
                    score += 50;
                    break;
                }
            }
        }

        explosionSecondsRemaining = 0.4;
        checkEnemyHit();
        checkPlayerHit();
    }

    private void checkEnemyHit() {
        if (enemyAlive && isInExplosion(enemyRow, ENEMY_COLUMN)) {
            enemyAlive = false;
        }
    }

    private void checkPlayerHit() {
        int row = player.getRow();
        int column = player.getColumn();
        if (isInExplosion(row, column)
                || (enemyAlive && row == enemyRow && column == ENEMY_COLUMN)) {
            gameOver = true;
            running = false;
        }
    }

    private boolean isInExplosion(int row, int column) {
        for (int[] cell : explosionCells) {
            if (row == cell[0] && column == cell[1]) {
                return true;
            }
        }
        return false;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getScore() {
        return score;
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
