package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

public class BreakoutController {
    private static final double FIELD_WIDTH = 550;
    private static final double FIELD_HEIGHT = 330;
    private static final double PADDLE_SPEED = 360;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean running;
    private long lastFrame;
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
    private Label scoreLabel;
    @FXML
    private Label livesLabel;
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
        gameArea.setFocusTraversable(true);

        gameArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                leftPressed = true;
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                rightPressed = true;
                event.consume();
            }
        });
        gameArea.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                leftPressed = false;
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                rightPressed = false;
                event.consume();
            }
        });
        gameArea.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                leftPressed = false;
                rightPressed = false;
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
                // Limit catch-up after a pause and use small steps to avoid skipped collisions.
                double elapsed = Math.min((now - lastFrame) / 1_000_000_000.0, 0.05);
                lastFrame = now;
                int steps = Math.max(1, (int) Math.ceil(elapsed / (1.0 / 120)));
                for (int i = 0; i < steps && running; i++) {
                    updateMovement(elapsed / steps);
                }
                updatePaddlePosition();
                updateBallPosition();
            }
        };
    }

    private void updateMovement(double elapsedSeconds) {
        Paddle paddle = game.getPaddle();
        int direction = (rightPressed ? 1 : 0) - (leftPressed ? 1 : 0);
        paddle.move(direction * PADDLE_SPEED * elapsedSeconds, FIELD_WIDTH);

        Ball ball = game.getBall();
        ball.move(elapsedSeconds);
        if (ball.getVelocityY() > 0
                && ball.getY() + ball.getRadius() >= paddle.getY()
                && ball.getY() - ball.getRadius() < paddle.getY() + paddle.getHeight()
                && ball.getX() + ball.getRadius() >= paddle.getX()
                && ball.getX() - ball.getRadius() <= paddle.getX() + paddle.getWidth()) {
            ball.setY(paddle.getY() - ball.getRadius());
            ball.reverseY();
        }

        if (ball.getX() + ball.getRadius() >= FIELD_WIDTH) {
            ball.setX(FIELD_WIDTH - ball.getRadius());
            ball.reverseX();
        } else if (ball.getX() - ball.getRadius() <= 0) {
            ball.setX(ball.getRadius());
            ball.reverseX();
        }
        if (ball.getY() - ball.getRadius() <= 0) {
            ball.setY(ball.getRadius());
            ball.reverseY();
        }
        if (ball.getY() - ball.getRadius() >= FIELD_HEIGHT) {
            running = false;
            gameLoop.stop();
            leftPressed = false;
            rightPressed = false;
            lastFrame = 0;
            game.loseLife();
            drawGame();
            if (game.getLives() > 0) {
                statusLabel.setText("Ball missed! Press Start to use your next life.");
            } else {
                statusLabel.setText("Game over! Press Restart to play again.");
            }
            return;
        }
        checkBrickCollisions();
    }


    @FXML
    private void handleStart() {
        if (game.getLives() == 0) {
            statusLabel.setText("Game over! Press Restart to play again.");
            return;
        }
        if (!running) {
            game.startGame();
            lastFrame = 0;
            running = true;
            gameLoop.start();
        }
        statusLabel.setText("Game started!");
        gameArea.requestFocus();

    }

    @FXML
    private void handleBack() {
        gameLoop.stop();
        running = false;
        leftPressed = false;
        rightPressed = false;
        SceneSwitcher.showMenu(gameArea);
    }

    @FXML
    private void handleRestart() {
        gameLoop.stop();
        running = false;
        leftPressed = false;
        rightPressed = false;
        game.resetGame();
        drawGame();
        handleStart();
    }

    private void drawGame() {
        gameArea.getChildren().clear();
        bricks.clear();
        scoreLabel.setText("Score: " + game.getScore());
        livesLabel.setText("Lives: " + game.getLives());
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

