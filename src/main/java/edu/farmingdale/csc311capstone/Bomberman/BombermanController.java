package edu.farmingdale.csc311capstone.Bomberman;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class BombermanController {
    private BombermanGame game;
    private AnimationTimer gameLoop;
    private long lastFrame;

    @FXML
    private GridPane gameArea;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        game = new BombermanGame();
        gameArea.setFocusTraversable(true);
        gameArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP -> game.movePlayer(-1, 0);
                case DOWN -> game.movePlayer(1, 0);
                case LEFT -> game.movePlayer(0, -1);
                case RIGHT -> game.movePlayer(0, 1);
                case SPACE -> game.placeBomb();
                default -> {
                    return;
                }
            }
            refreshView();
            if (!game.isRunning()) {
                gameLoop.stop();
            }
            event.consume();
        });

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrame == 0) {
                    lastFrame = now;
                    return;
                }
                double elapsedSeconds = (now - lastFrame) / 1_000_000_000.0;
                lastFrame = now;
                int bombCount = game.getBombs().size();
                int explosionCount = game.getExplosionCells().size();
                game.updateGame(elapsedSeconds);
                // Redraw when a bomb explodes or the flames disappear.
                if (bombCount != game.getBombs().size()
                        || explosionCount != game.getExplosionCells().size()) {
                    refreshView();
                }
                if (!game.isRunning()) {
                    stop();
                }
            }
        };
        refreshView();
    }

    @FXML
    private void handleStart() {
        if (!game.isRunning()) {
            game.startGame();
            if (game.isRunning()) {
                lastFrame = 0;
                gameLoop.start();
            }
        }
        refreshView();
        gameArea.requestFocus();
    }

    @FXML
    private void handleRestart() {
        gameLoop.stop();
        game.resetGame();
        handleStart();
    }

    @FXML
    private void handleBack() {
        gameLoop.stop();
        SceneSwitcher.showMenu(gameArea);
    }

    private void refreshView() {
        drawGame();
        if (game.isGameOver()) {
            statusLabel.setText("Game over! Press Restart to try again.");
        } else if (game.hasWon()) {
            statusLabel.setText("You win! All blocks cleared. Press Restart to play again.");
        } else if (game.isRunning()) {
            statusLabel.setText("Blocks remaining: " + game.getMap().countBlocks());
        } else {
            statusLabel.setText("Press Start to clear all the brown blocks.");
        }
    }

    private void drawGame() {
        gameArea.getChildren().clear();
        int[][] grid = game.getMap().getGrid();
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                Rectangle tile = new Rectangle(34, 34);
                if (grid[row][column] == GameMap.WALL) {
                    tile.setFill(Color.SLATEGRAY);
                } else if (grid[row][column] == GameMap.BLOCK) {
                    tile.setFill(Color.SANDYBROWN);
                } else {
                    tile.setFill(Color.WHITESMOKE);
                }
                tile.setStroke(Color.LIGHTGRAY);
                gameArea.add(tile, column, row);
            }
        }
        for (int[] cell : game.getExplosionCells()) {
            Rectangle flame = new Rectangle(34, 34, Color.ORANGE);
            gameArea.add(flame, cell[1], cell[0]);
        }
        for (Bomb bomb : game.getBombs()) {
            addCircle(bomb.getRow(), bomb.getColumn(), 9, Color.BLACK);
        }
        Player player = game.getPlayer();
        addCircle(player.getRow(), player.getColumn(), 11,
                game.isGameOver() ? Color.CRIMSON : Color.ROYALBLUE);
    }

    private void addCircle(int row, int column, double radius, Color color) {
        Circle circle = new Circle(radius, color);
        GridPane.setHalignment(circle, HPos.CENTER);
        GridPane.setValignment(circle, VPos.CENTER);
        gameArea.add(circle, column, row);
    }
}
