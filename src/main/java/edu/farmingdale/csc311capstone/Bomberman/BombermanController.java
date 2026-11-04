package edu.farmingdale.csc311capstone.Bomberman;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class BombermanController {
    private BombermanGame game;
    private AnimationTimer gameLoop;
    private long lastFrame;
    private KeyCode moveKey;
    private double movementCooldown;
    private Label bombCountdownLabel;
    private Circle bombShape;
    private Circle playerShape;
    private TranslateTransition playerMovement;

    @FXML
    private GridPane gameArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label scoreLabel;

    @FXML
    private void initialize() {
        game = new BombermanGame();
        gameArea.setFocusTraversable(true);
        playerShape = new Circle(11, Color.ROYALBLUE);
        playerMovement = new TranslateTransition(Duration.seconds(0.12), playerShape);
        gameArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case UP, DOWN, LEFT, RIGHT -> {
                    if (moveKey != event.getCode()) {
                        moveKey = event.getCode();
                        if (movementCooldown <= 0) {
                            movePlayer();
                        }
                    }
                }
                case SPACE -> {
                    game.placeBomb();
                    refreshView();
                }
                default -> {
                    return;
                }
            }
            event.consume();
        });
        gameArea.setOnKeyReleased(event -> {
            if (event.getCode() == moveKey) {
                moveKey = null;
                event.consume();
            }
        });
        gameArea.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                moveKey = null;
            }
        });
        gameArea.setOnMouseClicked(event -> gameArea.requestFocus());

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
                movementCooldown = Math.max(0, movementCooldown - elapsedSeconds);
                if (moveKey != null && movementCooldown <= 0 && game.isRunning()) {
                    movePlayer();
                }
                // Redraw when a bomb explodes or the flames disappear.
                if (bombCount != game.getBombs().size()
                        || explosionCount != game.getExplosionCells().size()) {
                    refreshView();
                }
                updateBombCountdown();
                if (!game.isRunning()) {
                    stop();
                    moveKey = null;
                    stopPlayerMovement();
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
        moveKey = null;
        movementCooldown = 0;
        stopPlayerMovement();
        game.resetGame();
        handleStart();
    }

    @FXML
    private void handleBack() {
        gameLoop.stop();
        moveKey = null;
        stopPlayerMovement();
        SceneSwitcher.showMenu(gameArea);
    }

    private void movePlayer() {
        if (!game.isRunning() || moveKey == null) {
            return;
        }
        int oldRow = game.getPlayer().getRow();
        int oldColumn = game.getPlayer().getColumn();
        switch (moveKey) {
            case UP -> game.movePlayer(-1, 0);
            case DOWN -> game.movePlayer(1, 0);
            case LEFT -> game.movePlayer(0, -1);
            case RIGHT -> game.movePlayer(0, 1);
            default -> { return; }
        }
        movementCooldown = 0.15;
        refreshView();
        int row = game.getPlayer().getRow();
        int column = game.getPlayer().getColumn();
        if (game.isRunning() && (row != oldRow || column != oldColumn)) {
            playerMovement.stop();
            playerMovement.setFromX((oldColumn - column) * 34);
            playerMovement.setFromY((oldRow - row) * 34);
            playerMovement.setToX(0);
            playerMovement.setToY(0);
            playerMovement.playFromStart();
        }
        if (!game.isRunning()) {
            gameLoop.stop();
            moveKey = null;
            stopPlayerMovement();
        }
    }

    private void stopPlayerMovement() {
        playerMovement.stop();
        playerShape.setTranslateX(0);
        playerShape.setTranslateY(0);
    }

    private void refreshView() {
        scoreLabel.setText("Score: " + game.getScore());
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
        bombCountdownLabel = null;
        bombShape = null;
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
            bombShape = addCircle(bomb.getRow(), bomb.getColumn(), 12, Color.BLACK);
            bombCountdownLabel = new Label();
            bombCountdownLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            bombCountdownLabel.setMouseTransparent(true);
            GridPane.setHalignment(bombCountdownLabel, HPos.CENTER);
            GridPane.setValignment(bombCountdownLabel, VPos.CENTER);
            gameArea.add(bombCountdownLabel, bomb.getColumn(), bomb.getRow());
        }
        updateBombCountdown();
        Player player = game.getPlayer();
        playerShape.setFill(game.isGameOver() ? Color.CRIMSON : Color.ROYALBLUE);
        GridPane.setHalignment(playerShape, HPos.CENTER);
        GridPane.setValignment(playerShape, VPos.CENTER);
        gameArea.add(playerShape, player.getColumn(), player.getRow());
    }

    private void updateBombCountdown() {
        if (bombCountdownLabel == null || game.getBombs().isEmpty()) {
            return;
        }
        double remaining = game.getBombs().get(0).getSecondsRemaining();
        bombCountdownLabel.setText(Integer.toString((int) Math.ceil(remaining)));
        bombShape.setFill(remaining <= 1 && (int) (remaining * 8) % 2 == 0
                ? Color.CRIMSON : Color.BLACK);
    }

    private Circle addCircle(int row, int column, double radius, Color color) {
        Circle circle = new Circle(radius, color);
        GridPane.setHalignment(circle, HPos.CENTER);
        GridPane.setValignment(circle, VPos.CENTER);
        gameArea.add(circle, column, row);
        return circle;
    }
}
