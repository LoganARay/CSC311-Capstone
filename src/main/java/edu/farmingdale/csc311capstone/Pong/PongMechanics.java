package edu.farmingdale.csc311capstone.Pong;

public class PongMechanics {

    private static final double PADDLE_SPEED = 320.0;

    private double ballX = 400;
    private double ballY = 210;

    private double ballVelocityX = 250;
    private double ballVelocityY = 150;

    public static double movePaddle(double currentY,
                                    double paddleHeight,
                                    double gameHeight,
                                    int direction,
                                    double deltaTime) {

        double newY = currentY + direction * PADDLE_SPEED * deltaTime;

        return Math.max(0, Math.min(newY, gameHeight - paddleHeight));
    }

    public void moveBall(double deltaTime) {
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

    public double getBallX() {
        return ballX;
    }

    public double getBallY() {
        return ballY;
    }
}
