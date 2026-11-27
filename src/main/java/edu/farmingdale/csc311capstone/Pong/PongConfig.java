package edu.farmingdale.csc311capstone.Pong;

import java.util.Objects;

/** All match rules and AI tuning live here; no UI or database dependencies. */
public record PongConfig(Mode mode, Opponent opponent, Difficulty difficulty) {
    public enum Mode {
        PLAYER_VS_AI,
        LOCAL_MULTIPLAYER,
        BLITZ
    }

    public enum Opponent {
        HUMAN,
        CPU
    }

    public enum Difficulty {
        EASY(215, .34, 75, 0, 0),
        MEDIUM(300, .19, 34, 0, 0),
        HARD(385, .10, 15, .12, 72);
        public final double speed, reactionSeconds, error, misreadChance, misreadOffset;

        Difficulty(
                double speed,
                double reactionSeconds,
                double error,
                double misreadChance,
                double misreadOffset) {
            this.speed = speed;
            this.reactionSeconds = reactionSeconds;
            this.error = error;
            this.misreadChance = misreadChance;
            this.misreadOffset = misreadOffset;
        }
    }

    public static final double WIDTH = 880,
            HEIGHT = 480,
            PADDLE_HEIGHT = 88,
            PADDLE_WIDTH = 14,
            LEFT_X = 30,
            RIGHT_X = WIDTH - 44,
            BALL_RADIUS = 8,
            PADDLE_SPEED = 400,
            INITIAL_SPEED = 345,
            MAX_SPEED = 780,
            MIN_HORIZONTAL_SPEED = 175,
            BLITZ_SECONDS = 60,
            READY_SECONDS = 3,
            SERVE_SECONDS = 1;
    public static final int WINNING_SCORE = 7;

    public PongConfig {
        Objects.requireNonNull(mode);
        Objects.requireNonNull(opponent);
        Objects.requireNonNull(difficulty);
        if (mode == Mode.PLAYER_VS_AI) opponent = Opponent.CPU;
        if (mode == Mode.LOCAL_MULTIPLAYER) opponent = Opponent.HUMAN;
    }

    public static PongConfig local() {
        return new PongConfig(Mode.LOCAL_MULTIPLAYER, Opponent.HUMAN, Difficulty.MEDIUM);
    }

    public String title() {
        return switch (mode) {
            case PLAYER_VS_AI -> "PLAYER VS AI";
            case LOCAL_MULTIPLAYER -> "LOCAL MULTIPLAYER";
            case BLITZ -> "60 SECOND BLITZ";
        };
    }

    public String opponentLabel() {
        return opponent == Opponent.CPU ? "CPU · " + difficulty : "PLAYER TWO";
    }
}
