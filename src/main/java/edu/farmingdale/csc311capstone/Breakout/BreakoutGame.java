package edu.farmingdale.csc311capstone.Breakout;

import java.util.ArrayList;
import java.util.List;

public class BreakoutGame {
    public enum Mode {
        NORMAL("Normal"),
        TARGET_HUNT("Target Hunt");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final Mode mode;
    private int score;
    private int lives;
    private Paddle paddle;
    private Ball ball;
    private final List<Brick> bricks = new ArrayList<>();

    public BreakoutGame() {
        this(Mode.NORMAL);
    }

    public BreakoutGame(Mode mode) {
        this.mode = mode;
        initializeGame();
    }

    public void initializeGame() {
        resetPositions();
        score = 0;
        lives = 3;
        bricks.clear();
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 8; column++) {
                boolean target = mode == Mode.TARGET_HUNT && column == 1 + row * 2;
                bricks.add(new Brick(17.5 + column * 65, 30 + row * 24, 60, 18, target));
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

    private void resetPositions() {
        paddle = new Paddle(225, 300, 100, 12);
        ball = new Ball(275, 280, 8);
    }

    public void loseLife() {
        if (lives > 0) {
            lives--;
            if (lives > 0) {
                resetPositions();
            }
        }
    }

    public int getLives() {
        return lives;
    }

    public void removeBrick(Brick brick) {
        if (bricks.remove(brick)) {
            score += 50;
        }
    }

    public int getTargetsRemaining() {
        return (int) bricks.stream().filter(Brick::isTarget).count();
    }

    public boolean hasWon() {
        return mode == Mode.TARGET_HUNT ? getTargetsRemaining() == 0 : bricks.isEmpty();
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
