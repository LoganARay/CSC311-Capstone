package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

public class BreakoutController {
    private BreakoutGame game;
    private Rectangle paddleShape;
    private Circle ballShape;
    private AnimationTimer gameLoop;
    private Map<Brick, Rectangle> bricks = new HashMap<>();

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
        ballShape = new Circle(ball.getX(), ball.getY(), ball.getRadius(), Color.DARKORANGE);
        gameArea.getChildren().addAll(paddleShape, ballShape);

        for (Brick brick : game.getBricks()) {
            Rectangle brickShape = new Rectangle(brick.getX(), brick.getY(),
                    brick.getWidth(), brick.getHeight());


            brickShape.setFill(Color.STEELBLUE);
            gameArea.getChildren().add(brickShape);

            bricks.put(brick, brickShape);
        }

        gameArea.setFocusTraversable(true);

        gameArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                game.getPaddle().move(-30, gameArea.getWidth());
                updatePaddlePosition();
                event.consume();
            }
            else if (event.getCode() == KeyCode.RIGHT) {
                game.getPaddle().move(30,gameArea.getWidth());
                updatePaddlePosition();
                event.consume();
            }
        });

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Ball ball = game.getBall();
                Paddle paddle = game.getPaddle();

                if (ball.getY() + ball.getRadius() >= paddle.getY()
                        && ball.getY() - ball.getRadius() < paddle.getY() + paddle.getHeight()
                        && ball.getX() + ball.getRadius() >= paddle.getX()
                        && ball.getX() - ball.getRadius() <= paddle.getX() + paddle.getWidth()) {

                    ball.setY(paddle.getY() - ball.getRadius());
                    ball.reverseY();
                }

                ball.move();

                if(ball.getX() + ball.getRadius() >= gameArea.getWidth()) {
                    ball.setX(gameArea.getWidth() - ball.getRadius());
                    ball.reverseX();
                } else if (ball.getX()-ball.getRadius() <= 0) {
                    ball.setX(ball.getRadius());
                    ball.reverseX();
                }

                if (ball.getY() - ball.getRadius() <= 0) {
                    ball.setY(ball.getRadius());
                    ball.reverseY();
                }
                if (ball.getY() - ball.getRadius() >= gameArea.getHeight()) {
                    gameLoop.stop();
                    statusLabel.setText("Game over! You missed the ball.");
                    return;
                }

                checkBrickCollisions();
                updateBallPosition();
            }
        };
    }


    @FXML
    private void handleStart() {
        System.out.println("Start button clicked!");
        game.startGame();
        gameLoop.start();
        statusLabel.setText("Game stared!");
        gameArea.requestFocus();

    }

    @FXML
    private void handleBack() {
        gameLoop.stop();
        SceneSwitcher.showMenu(gameArea);
    }

    private void updatePaddlePosition() {

        paddleShape.setX(game.getPaddle().getX());

    }
    private void updateBallPosition() {
        Ball ball = game.getBall();
        ballShape.setCenterX(ball.getX());
        ballShape.setCenterY(ball.getY());
    }
    private void checkBrickCollisions() {
        Ball ball = game.getBall();

        for (int i = 0; i < game.getBricks().size(); i++) {
            Brick brick = game.getBricks().get(i);

            boolean collision =
                    ball.getX() + ball.getRadius() >= brick.getX()
                            && ball.getX() - ball.getRadius() <= brick.getX() + brick.getWidth()
                            && ball.getY() + ball.getRadius() >= brick.getY()
                            && ball.getY() - ball.getRadius() <= brick.getY() + brick.getHeight();

            if (collision) {
                game.getBricks().remove(i);

                Rectangle brickShape = bricks.remove(brick);
                gameArea.getChildren().remove(brickShape);

                ball.reverseY();
                break;
            }
        }
    }
}

