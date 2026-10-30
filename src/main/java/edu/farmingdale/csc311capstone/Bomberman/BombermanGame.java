package edu.farmingdale.csc311capstone.Bomberman;

import java.util.ArrayList;
import java.util.List;

public class BombermanGame {
    private boolean running;
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
    }

    public void startGame() {
        running = true;
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

        player.setRow(row);
        player.setColumn(column);
    }

    public boolean isRunning() {
        return running;
    }

    public void updateGame() {
        // TODO: Add player movement and map interactions.
    }

    public void placeBomb() {
        // TODO: Add bomb placement, timers, and explosions.
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
