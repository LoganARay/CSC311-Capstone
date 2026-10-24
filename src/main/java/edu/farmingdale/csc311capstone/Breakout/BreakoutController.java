package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class BreakoutController {
    private BreakoutGame game;
    private Rectangle paddleShape;

    @FXML
    private Pane gameArea;
    @FXML
    private Label statusLabel;



    @FXML
    private void initialize() {
        game = new BreakoutGame();
        Paddle paddle = game.getPaddle();
        paddleShape = new Rectangle(paddle.getX(), paddle.getY(),
                paddle.getWidth(), paddle.getHeight());

        paddleShape.setFill(Color.DARKSLATEBLUE);

        Ball ball = game.getBall();
        Circle ballShape = new Circle(ball.getX(), ball.getY(), ball.getRadius(), Color.DARKORANGE);
        gameArea.getChildren().addAll(paddleShape, ballShape);
        for (Brick brick : game.getBricks()) {
            Rectangle brickShape = new Rectangle(brick.getX(), brick.getY(),
                    brick.getWidth(), brick.getHeight());

            gameArea.setFocusTraversable(true);

            gameArea.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.LEFT) {
                    game.getPaddle().move(-10, gameArea.getWidth());
                    updatePaddlePosition();
                    event.consume();
                }
                else if (event.getCode() == KeyCode.RIGHT) {
                    game.getPaddle().move(10 ,gameArea.getWidth());
                    updatePaddlePosition();
                    event.consume();
                }
            });

            brickShape.setFill(Color.STEELBLUE);
            gameArea.getChildren().add(brickShape);
        }
    }


    @FXML
    private void handleStart() {
        System.out.println("Start button clicked!");
        game.startGame();
        statusLabel.setText("Game stared!");
        gameArea.requestFocus();
    }

    @FXML
    private void handleBack() {
        SceneSwitcher.showMenu(gameArea);
    }

    private void updatePaddlePosition() {
        paddleShape.setX(game.getPaddle().getX());

    }
}

