package edu.farmingdale.csc311capstone.Pacman;

/** Frozen once per run, ready for a future shared history/leaderboard service. */
public record PacmanResult(String gameName, PacmanMode mode, int score, int levelsCompleted,
                           int levelReached, int ghostsCaptured, int pelletsCollected,
                           int powerPelletsCollected, int livesRemaining, double durationSeconds,
                           Outcome outcome, int captureTarget, double secondsRemaining) {
    public enum Outcome { GAME_OVER, TIME_EXPIRED, CHALLENGE_COMPLETED, CHALLENGE_FAILED }
}
