package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class BreakoutController {
    private static final double FIELD_WIDTH = 550;
    private static final double FIELD_HEIGHT = 330;
    private BreakoutGame game;
    private Stage gameStage;
    private boolean wasFullScreen;
    private Rectangle paddleShape;
    private Circle ballShape;
    private AnimationTimer gameLoop;
    private Map<Brick, Rectangle> bricks = new HashMap<>();

    @FXML
    private Pane gameArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private StackPane gameContainer;


    @FXML
    private void initialize() {
        game = new BreakoutGame();
        drawGame();

        // Scale the view while keeping gameplay positions in the same coordinate system.
        gameArea.scaleXProperty().bind(Bindings.max(0.1, Bindings.min(
                gameContainer.widthProperty().subtract(24).divide(FIELD_WIDTH),
                gameContainer.heightProperty().subtract(24).divide(FIELD_HEIGHT))));
        gameArea.scaleYProperty().bind(gameArea.scaleXProperty());
        gameArea.setClip(new Rectangle(FIELD_WIDTH, FIELD_HEIGHT));
        Platform.runLater(() -> {
            if (gameArea.getScene() != null
                    && gameArea.getScene().getWindow() instanceof Stage stage) {
                gameStage = stage;
                wasFullScreen = stage.isFullScreen();
                stage.setFullScreen(true);
            }
        });

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
        game.startGame();
        gameLoop.start();
        statusLabel.setText("Game started!");
        gameArea.requestFocus();

    }

    @FXML
    private void handleBack() {
        gameLoop.stop();
        if (gameStage != null) {
            gameStage.setFullScreen(wasFullScreen);
        }
        SceneSwitcher.showMenu(gameArea);
    }

    @FXML
    private void handleRestart() {
        gameLoop.stop();
        game.resetGame();
        drawGame();
        handleStart();
    }

    private void drawGame() {
        gameArea.getChildren().clear();
        bricks.clear();
        scoreLabel.setText("Score: " + game.getScore());
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
                game.removeBrick(brick);
                scoreLabel.setText("Score: " + game.getScore());

                Rectangle brickShape = bricks.remove(brick);
                gameArea.getChildren().remove(brickShape);

                ball.reverseY();
                break;
            }
        }
    }
}

