package edu.farmingdale.csc311capstone.Breakout;

import java.util.ArrayList;
import java.util.List;

public class BreakoutGame {
    private int score;
    private Paddle paddle;
    private Ball ball;
    private final List<Brick> bricks = new ArrayList<>();

    public BreakoutGame() {
        initializeGame();
    }

    public void initializeGame() {
        paddle = new Paddle(225, 300, 100, 12);
        ball = new Ball(275, 280, 8);
        score = 0;
        bricks.clear();
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 8; column++) {
                bricks.add(new Brick(17.5 + column * 65, 30 + row * 24, 60, 18));
            }
        }
    }

    public void startGame() {
        // TODO: Start gameplay when the mechanics are implemented.
    }

    public void updateGame() {
        // TODO: Add paddle movement, ball physics, and brick collisions.
    }

    public void resetGame() {
        initializeGame();
    }

    public void removeBrick(Brick brick) {
        if (bricks.remove(brick)) {
            score += 50;
        }
    }

    public int getScore() {
        return score;
    }

    public Paddle getPaddle() {
        return paddle;
    }

    public Ball getBall() {
        return ball;
    }

    public List<Brick> getBricks() {
        return bricks;
    }
}
