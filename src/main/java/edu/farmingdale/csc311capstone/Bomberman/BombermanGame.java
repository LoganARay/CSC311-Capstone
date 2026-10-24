package edu.farmingdale.csc311capstone.Bomberman;

import java.util.ArrayList;
import java.util.List;

public class BombermanGame {
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
    }

    public void startGame() {
        // TODO: Start gameplay when the mechanics are implemented.
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
