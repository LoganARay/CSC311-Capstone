package edu.farmingdale.csc311capstone.Pong;

public class PongMechanics {

    private static final double PADDLE_SPEED = 320.0;
    private static final double BALL_SPEED_X = 250.0;
    private static final double BALL_SPEED_Y = 150.0;
    private static final int WINNING_SCORE = 7;

    private double ballX = 400;
    private double ballY = 210;

    private double ballVelocityX = BALL_SPEED_X;
    private double ballVelocityY = BALL_SPEED_Y;

    private int player1Score = 0;
    private int player2Score = 0;
    private boolean gameOver = false;

    public static double movePaddle(double currentY,
                                    double paddleHeight,
                                    double gameHeight,
                                    int direction,
                                    double deltaTime) {

        double newY = currentY + direction * PADDLE_SPEED * deltaTime;

        return Math.max(0, Math.min(newY, gameHeight - paddleHeight));
    }

    public void moveBall(double deltaTime) {
        if (gameOver) {
            return;
        }

        ballX += ballVelocityX * deltaTime;
        ballY += ballVelocityY * deltaTime;
    }

    public void checkWallCollision(double gameHeight, double ballRadius) {

        if (ballY - ballRadius <= 0 && ballVelocityY < 0) {
            ballY = ballRadius;
            ballVelocityY = -ballVelocityY;
        }

        if (ballY + ballRadius >= gameHeight && ballVelocityY > 0) {
            ballY = gameHeight - ballRadius;
            ballVelocityY = -ballVelocityY;
        }
    }

    public void checkPaddleCollision(double paddleX,
                                     double paddleY,
                                     double paddleWidth,
                                     double paddleHeight,
                                     double ballRadius,
                                     boolean leftPaddle) {

        boolean horizontalCollision =
                ballX + ballRadius >= paddleX &&
                        ballX - ballRadius <= paddleX + paddleWidth;

        boolean verticalCollision =
                ballY + ballRadius >= paddleY &&
                        ballY - ballRadius <= paddleY + paddleHeight;

        if (!horizontalCollision || !verticalCollision) {
            return;
        }

        if (leftPaddle && ballVelocityX < 0) {
            ballX = paddleX + paddleWidth + ballRadius;
            ballVelocityX = -ballVelocityX;
        }

        if (!leftPaddle && ballVelocityX > 0) {
            ballX = paddleX - ballRadius;
            ballVelocityX = -ballVelocityX;
        }
    }

    public boolean checkScore(double gameWidth,
                              double gameHeight,
                              double ballRadius) {

        if (gameOver) {
            return false;
        }

        if (ballX + ballRadius < 0) {
            player2Score++;
            resetBall(gameWidth, gameHeight, false);

        } else if (ballX - ballRadius > gameWidth) {
            player1Score++;
            resetBall(gameWidth, gameHeight, true);

        } else {
            return false;
        }

        if (player1Score >= WINNING_SCORE ||
                player2Score >= WINNING_SCORE) {
            gameOver = true;
        }

        return true;
    }

    private void resetBall(double gameWidth,
                           double gameHeight,
                           boolean moveRight) {

        ballX = gameWidth / 2;
        ballY = gameHeight / 2;

        ballVelocityX = moveRight ? BALL_SPEED_X : -BALL_SPEED_X;

        ballVelocityY = ballVelocityY >= 0
                ? -BALL_SPEED_Y
                : BALL_SPEED_Y;
    }

    public void resetGame() {
        ballX = 400;
        ballY = 210;

        ballVelocityX = BALL_SPEED_X;
        ballVelocityY = BALL_SPEED_Y;

        player1Score = 0;
        player2Score = 0;
        gameOver = false;
    }

    public double getBallX() {
        return ballX;
    }

    public double getBallY() {
        return ballY;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getWinner() {
        if (!gameOver) {
            return 0;
        }

        return player1Score >= WINNING_SCORE ? 1 : 2;
    }
}
