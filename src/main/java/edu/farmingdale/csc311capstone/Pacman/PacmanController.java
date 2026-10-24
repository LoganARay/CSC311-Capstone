package edu.farmingdale.csc311capstone.Pacman;

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class PacmanController {

    private static final double TILE_SIZE = 28;
    private static final long MOVE_INTERVAL = 140_000_000L;

    @FXML
    private Pane mazePane;

    @FXML
    private Label scoreLabel;

    @FXML
    private Label pelletsLabel;

    @FXML
    private Label statusLabel;

    private final PacmanMechanics mechanics = new PacmanMechanics();

    private Circle pacman;
    private Circle[][] pelletNodes;
    private AnimationTimer gameLoop;

    private int rowDirection = 0;
    private int colDirection = 0;

    private long lastMoveTime = 0;

    private final EventHandler<KeyEvent> keyPressedHandler =
            this::handleKeyPressed;

    private final EventHandler<KeyEvent> keyReleasedHandler =
            this::handleKeyReleased;

    @FXML
    private void initialize() {

        mazePane.setFocusTraversable(true);

        renderMaze();
        createPacman();
        updateLabels();

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (mechanics.isGameWon()) {
                    return;
                }

                if (rowDirection == 0 && colDirection == 0) {
                    return;
                }

                if (now - lastMoveTime < MOVE_INTERVAL) {
                    return;
                }

                lastMoveTime = now;
                movePacman();
            }
        };

        mazePane.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (oldScene != null) {
                        oldScene.removeEventFilter(
                                KeyEvent.KEY_PRESSED,
                                keyPressedHandler
                        );

                        oldScene.removeEventFilter(
                                KeyEvent.KEY_RELEASED,
                                keyReleasedHandler
                        );

                        gameLoop.stop();
                        resetDirection();
                    }

                    if (newScene == null) {
                        return;
                    }

                    newScene.addEventFilter(
                            KeyEvent.KEY_PRESSED,
                            keyPressedHandler
                    );

                    newScene.addEventFilter(
                            KeyEvent.KEY_RELEASED,
                            keyReleasedHandler
                    );

                    newScene.windowProperty().addListener(
                            (obs, oldWindow, newWindow) -> {

                                if (newWindow == null) {
                                    gameLoop.stop();
                                    resetDirection();
                                } else {
                                    lastMoveTime = 0;

                                    if (!mechanics.isGameWon()) {
                                        gameLoop.start();
                                    }

                                    mazePane.requestFocus();
                                }
                            }
                    );

                    if (newScene.getWindow() != null) {
                        lastMoveTime = 0;
                        gameLoop.start();
                        mazePane.requestFocus();
                    }
                }
        );
    }

    private void renderMaze() {

        mazePane.getChildren().clear();

        mazePane.setPrefSize(
                mechanics.getColumns() * TILE_SIZE,
                mechanics.getRows() * TILE_SIZE
        );

        pelletNodes = new Circle[
                mechanics.getRows()
                ][
                mechanics.getColumns()
                ];

        for (int row = 0; row < mechanics.getRows(); row++) {

            for (int col = 0; col < mechanics.getColumns(); col++) {

                char tile = mechanics.getTile(row, col);

                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;

                if (tile == PacmanMechanics.WALL) {

                    Rectangle wall = new Rectangle(
                            x, y, TILE_SIZE, TILE_SIZE
                    );

                    wall.setFill(Color.DARKBLUE);
                    wall.setStroke(Color.DODGERBLUE);
                    wall.setStrokeWidth(1);

                    mazePane.getChildren().add(wall);

                } else if (tile == PacmanMechanics.PELLET) {

                    Circle pellet = new Circle(
                            x + TILE_SIZE / 2,
                            y + TILE_SIZE / 2,
                            3
                    );

                    pellet.setFill(Color.LIGHTYELLOW);

                    pelletNodes[row][col] = pellet;
                    mazePane.getChildren().add(pellet);
                }
            }
        }
    }

    private void createPacman() {

        pacman = new Circle(10, Color.YELLOW);

        updatePacmanPosition();

        mazePane.getChildren().add(pacman);
    }

    private void updatePacmanPosition() {

        pacman.setCenterX(
                mechanics.getPlayerCol() * TILE_SIZE + TILE_SIZE / 2
        );

        pacman.setCenterY(
                mechanics.getPlayerRow() * TILE_SIZE + TILE_SIZE / 2
        );
    }

    private void handleKeyPressed(KeyEvent event) {

        if (mechanics.isGameWon()) {
            return;
        }

        switch (event.getCode()) {
            case UP -> {
                rowDirection = -1;
                colDirection = 0;
            }

            case DOWN -> {
                rowDirection = 1;
                colDirection = 0;
            }

            case LEFT -> {
                rowDirection = 0;
                colDirection = -1;
            }

            case RIGHT -> {
                rowDirection = 0;
                colDirection = 1;
            }

            default -> {
                return;
            }
        }

        event.consume();
    }

    private void handleKeyReleased(KeyEvent event) {

        switch (event.getCode()) {
            case UP -> {
                if (rowDirection == -1) {
                    resetDirection();
                }
            }

            case DOWN -> {
                if (rowDirection == 1) {
                    resetDirection();
                }
            }

            case LEFT -> {
                if (colDirection == -1) {
                    resetDirection();
                }
            }

            case RIGHT -> {
                if (colDirection == 1) {
                    resetDirection();
                }
            }

            default -> {
                return;
            }
        }

        event.consume();
    }

    private void movePacman() {

        boolean moved = mechanics.movePlayer(
                rowDirection,
                colDirection
        );

        if (!moved) {
            return;
        }

        int row = mechanics.getPlayerRow();
        int col = mechanics.getPlayerCol();

        Circle pellet = pelletNodes[row][col];

        if (pellet != null &&
                mechanics.getTile(row, col) == PacmanMechanics.EMPTY) {

            mazePane.getChildren().remove(pellet);
            pelletNodes[row][col] = null;
        }

        updatePacmanPosition();
        updateLabels();

        if (mechanics.isGameWon()) {
            gameLoop.stop();
            resetDirection();

            statusLabel.setText(
                    "Maze cleared! Final score: " + mechanics.getScore()
            );
        }
    }

    private void updateLabels() {

        scoreLabel.setText(
                "Score: " + mechanics.getScore()
        );

        pelletsLabel.setText(
                "Pellets Left: " + mechanics.getRemainingPellets()
        );
    }

    @FXML
    private void restartGame() {

        gameLoop.stop();

        mechanics.resetGame();
        resetDirection();
        lastMoveTime = 0;

        renderMaze();
        createPacman();
        updateLabels();

        statusLabel.setText("Use arrow keys to collect all pellets");

        gameLoop.start();
        mazePane.requestFocus();
    }

    private void resetDirection() {
        rowDirection = 0;
        colDirection = 0;
    }
}
