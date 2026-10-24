package edu.farmingdale.csc311capstone.Pong;

public class PongMechanics {

    private static final double PADDLE_SPEED = 320.0;

    public static double movePaddle(double currentY,
                                    double paddleHeight,
                                    double gameHeight,
                                    int direction,
                                    double deltaTime) {

        double newY = currentY + direction * PADDLE_SPEED * deltaTime;

        return Math.max(0, Math.min(newY, gameHeight - paddleHeight));
    }
}
