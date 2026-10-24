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

    public double getBallX() {
        return ballX;
    }

    public double getBallY() {
        return ballY;
    }
}
