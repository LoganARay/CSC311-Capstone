package edu.farmingdale.csc311capstone.Pong;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class PongController {

    @FXML
    private Pane gamePane;

    @FXML
    private Rectangle leftPaddle;

    @FXML
    private Rectangle rightPaddle;

    @FXML
    private Circle ball;

    @FXML
    private Label player1Score;

    @FXML
    private Label player2Score;

    @FXML
    private Label statusLabel;

    @FXML
    private Button pauseButton;

    private final PongMechanics mechanics = new PongMechanics();

    private boolean wPressed;
    private boolean sPressed;
    private boolean upPressed;
    private boolean downPressed;
    private boolean paused = false;

    private AnimationTimer gameLoop;
    private long lastFrameTime = 0;

    @FXML
    private void initialize() {

        gamePane.setFocusTraversable(true);

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {

                if (lastFrameTime == 0) {
                    lastFrameTime = now;
                    return;
                }

                double deltaTime = Math.min(
                        (now - lastFrameTime) / 1_000_000_000.0,
                        0.05
                );

                lastFrameTime = now;

                if (paused || mechanics.isGameOver()) {
                    return;
                }

                updatePaddles(deltaTime);
                updateBall(deltaTime);
            }
        };

        gamePane.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene == null) {
                        gameLoop.stop();
                        resetKeys();
                        return;
                    }

                    newScene.addEventFilter(
                            KeyEvent.KEY_PRESSED,
                            this::handleKeyPressed
                    );

                    newScene.addEventFilter(
                            KeyEvent.KEY_RELEASED,
                            this::handleKeyReleased
                    );

                    newScene.windowProperty().addListener(
                            (obs, oldWindow, newWindow) -> {

                                if (newWindow == null) {
                                    gameLoop.stop();
                                    resetKeys();
                                } else {
                                    lastFrameTime = 0;
                                    gameLoop.start();
                                    gamePane.requestFocus();
                                }
                            }
                    );

                    if (newScene.getWindow() != null) {
                        lastFrameTime = 0;
                        gameLoop.start();
                        gamePane.requestFocus();
                    }
                }
        );

        updateScores();
    }

    private void handleKeyPressed(KeyEvent event) {

        switch (event.getCode()) {
            case SPACE -> {
                togglePause();
                event.consume();
            }

            case W -> {
                if (!paused && !mechanics.isGameOver()) {
                    wPressed = true;
                }
                event.consume();
            }

            case S -> {
                if (!paused && !mechanics.isGameOver()) {
                    sPressed = true;
                }
                event.consume();
            }

            case UP -> {
                if (!paused && !mechanics.isGameOver()) {
                    upPressed = true;
                }
                event.consume();
            }

            case DOWN -> {
                if (!paused && !mechanics.isGameOver()) {
                    downPressed = true;
                }
                event.consume();
            }

            default -> {
            }
        }
    }

    private void handleKeyReleased(KeyEvent event) {

        switch (event.getCode()) {
            case W -> {
                wPressed = false;
                event.consume();
            }
            case S -> {
                sPressed = false;
                event.consume();
            }
            case UP -> {
                upPressed = false;
                event.consume();
            }
            case DOWN -> {
                downPressed = false;
                event.consume();
            }
            default -> {
            }
        }
    }

    private void updatePaddles(double deltaTime) {

        int leftDirection =
                (sPressed ? 1 : 0) - (wPressed ? 1 : 0);

        int rightDirection =
                (downPressed ? 1 : 0) - (upPressed ? 1 : 0);

        double gameHeight = gamePane.getPrefHeight();

        leftPaddle.setY(
                PongMechanics.movePaddle(
                        leftPaddle.getY(),
                        leftPaddle.getHeight(),
                        gameHeight,
                        leftDirection,
                        deltaTime
                )
        );

        rightPaddle.setY(
                PongMechanics.movePaddle(
                        rightPaddle.getY(),
                        rightPaddle.getHeight(),
                        gameHeight,
                        rightDirection,
                        deltaTime
                )
        );
    }

    private void updateBall(double deltaTime) {

        mechanics.moveBall(deltaTime);

        mechanics.checkWallCollision(
                gamePane.getPrefHeight(),
                ball.getRadius()
        );

        mechanics.checkPaddleCollision(
                leftPaddle.getX(),
                leftPaddle.getY(),
                leftPaddle.getWidth(),
                leftPaddle.getHeight(),
                ball.getRadius(),
                true
        );

        mechanics.checkPaddleCollision(
                rightPaddle.getX(),
                rightPaddle.getY(),
                rightPaddle.getWidth(),
                rightPaddle.getHeight(),
                ball.getRadius(),
                false
        );

        boolean scored = mechanics.checkScore(
                gamePane.getPrefWidth(),
                gamePane.getPrefHeight(),
                ball.getRadius()
        );

        ball.setCenterX(mechanics.getBallX());
        ball.setCenterY(mechanics.getBallY());

        if (scored) {
            updateScores();

            if (mechanics.isGameOver()) {
                statusLabel.setText(
                        "Player " + mechanics.getWinner() + " wins!"
                );

                pauseButton.setDisable(true);
                gameLoop.stop();
                resetKeys();
            }
        }
    }

    private void updateScores() {
        player1Score.setText(
                String.valueOf(mechanics.getPlayer1Score())
        );

        player2Score.setText(
                String.valueOf(mechanics.getPlayer2Score())
        );
    }

    @FXML
    private void togglePause() {

        if (mechanics.isGameOver()) {
            return;
        }

        paused = !paused;
        resetKeys();

        if (paused) {
            gameLoop.stop();
            statusLabel.setText("Paused");
            pauseButton.setText("Resume");
        } else {
            lastFrameTime = 0;
            gameLoop.start();
            statusLabel.setText("First to 7");
            pauseButton.setText("Pause");
        }

        gamePane.requestFocus();
    }

    @FXML
    private void restartGame() {

        mechanics.resetGame();

        leftPaddle.setY(170);
        rightPaddle.setY(170);

        ball.setCenterX(mechanics.getBallX());
        ball.setCenterY(mechanics.getBallY());

        paused = false;
        lastFrameTime = 0;
        resetKeys();

        statusLabel.setText("First to 7");
        pauseButton.setText("Pause");
        pauseButton.setDisable(false);

        updateScores();

        gameLoop.start();
        gamePane.requestFocus();
    }

    @FXML
    private void backToArcade() {
        gameLoop.stop();
        resetKeys();

        SceneSwitcher.showMenu(gamePane);
    }

    private void resetKeys() {
        wPressed = false;
        sPressed = false;
        upPressed = false;
        downPressed = false;
    }
}
