package edu.farmingdale.csc311capstone.Pacman;

/** Centralized rules shared by the mechanics and mode-selection screen. */
public enum PacmanMode {
    CLASSIC("Classic", "Three lives. Clear mazes, collect fruit and reach the next level.", 3, 0, 1, 8),
    TIME_ATTACK("Time Attack", "60 seconds to score. Clear a maze to keep going; three lives.", 3, 60, 1, 8),
    SPEED_UP("Speed-Up", "Every 20 pellets increases the pace, up to 1.55×. Three lives.", 3, 0, 1, 8),
    HARD("Hard Mode", "Two lives, faster pursuit and four-second power pellets.", 2, 0, 1.22, 4),
    GHOST_CHALLENGE("Ghost Challenge", "Capture six ghosts in 90 seconds. Power pellets recharge every 12s.", 3, 90, .95, 10);
    public final String title, description;
    public final int startingLives;
    public final double duration, ghostSpeedFactor, frightenedSeconds;
    PacmanMode(String title,String description,int lives,double duration,double ghostSpeedFactor,double frightenedSeconds) {
        this.title=title;this.description=description;this.startingLives=lives;this.duration=duration;
        this.ghostSpeedFactor=ghostSpeedFactor;this.frightenedSeconds=frightenedSeconds;
    }
    public static final int CAPTURE_TARGET=6, PELLET_POINTS=10, POWER_POINTS=50, FRUIT_POINTS=300;
    public static final double PLAYER_SPEED=5.2, GHOST_SPEED=3.35, MAX_PLAYER_SPEED=8.1, MAX_GHOST_SPEED=6.6,
            FRUIT_SECONDS=9, READY_SECONDS=2, CHALLENGE_RECHARGE_SECONDS=12;
}
