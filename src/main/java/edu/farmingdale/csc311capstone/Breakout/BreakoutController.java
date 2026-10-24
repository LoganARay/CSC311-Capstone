package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.HelloApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class BreakoutController {
    private BreakoutGame game;

    @FXML
    private Pane gameArea;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        game = new BreakoutGame();
        Paddle paddle = game.getPaddle();
        Rectangle paddleShape = new Rectangle(paddle.getX(), paddle.getY(),
                paddle.getWidth(), paddle.getHeight());
        paddleShape.setFill(Color.DARKSLATEBLUE);
        Ball ball = game.getBall();
        Circle ballShape = new Circle(ball.getX(), ball.getY(), ball.getRadius(), Color.DARKORANGE);
        gameArea.getChildren().addAll(paddleShape, ballShape);
        for (Brick brick : game.getBricks()) {
            Rectangle brickShape = new Rectangle(brick.getX(), brick.getY(),
                    brick.getWidth(), brick.getHeight());
            brickShape.setFill(Color.STEELBLUE);
            gameArea.getChildren().add(brickShape);
        }
    }

    @FXML
    private void handleStart() {
        game.startGame();
        statusLabel.setText("Breakout skeleton only - gameplay coming later.");
    }

    @FXML
    private void handleBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        gameArea.getScene().setRoot(loader.load());
    }
}
