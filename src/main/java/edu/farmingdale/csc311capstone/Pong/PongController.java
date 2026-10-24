package edu.farmingdale.csc311capstone.Pong;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
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

    private boolean wPressed;
    private boolean sPressed;
    private boolean upPressed;
    private boolean downPressed;

    private AnimationTimer gameLoop;
    private long lastFrameTime = 0;

    @FXML
    private void initialize() {

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
                updatePaddles(deltaTime);
            }
        };

        gamePane.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (newScene == null) {
                        return;
                    }

                    newScene.setOnKeyPressed(this::handleKeyPressed);
                    newScene.setOnKeyReleased(this::handleKeyReleased);

                    newScene.windowProperty().addListener(
                            (obs, oldWindow, newWindow) -> {

                                if (newWindow == null) {
                                    gameLoop.stop();
                                    resetKeys();
                                } else {
                                    lastFrameTime = 0;
                                    gameLoop.start();
                                }
                            }
                    );
                }
        );
    }

    private void handleKeyPressed(KeyEvent event) {

        switch (event.getCode()) {
            case W -> wPressed = true;
            case S -> sPressed = true;
            case UP -> upPressed = true;
            case DOWN -> downPressed = true;
            default -> {
            }
        }
    }

    private void handleKeyReleased(KeyEvent event) {

        switch (event.getCode()) {
            case W -> wPressed = false;
            case S -> sPressed = false;
            case UP -> upPressed = false;
            case DOWN -> downPressed = false;
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

    private void resetKeys() {
        wPressed = false;
        sPressed = false;
        upPressed = false;
        downPressed = false;
    }
}
