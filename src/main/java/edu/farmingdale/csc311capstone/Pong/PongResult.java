package edu.farmingdale.csc311capstone.Pong;

/** Immutable integration boundary for a future shared result service. Player is the left side. */
public record PongResult(String gameName, PongConfig.Mode mode, String difficulty,
                         PongConfig.Opponent opponentType, int playerScore, int opponentScore,
                         Outcome outcome, double durationSeconds, int paddleHits, int longestRally) {
    public enum Outcome { WIN, LOSS, DRAW }
}
