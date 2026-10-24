package edu.farmingdale.csc311capstone.SpaceInvaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Game rules and state for classic Space Invaders. Kept free of JavaFX so it can be unit tested.
 *
 * Step 1 (graphics): the starting layout of the cannon, the alien formation and the shields.
 * Step 2 (movement): the cannon slides left and right and stops at the edges.
 * Step 3 (shooting): one shot on screen at a time. It destroys the first alien it touches
 * (adding that alien's points) or knocks a block out of a shield.
 */
public final class SpaceInvadersMechanics {
    public static final double PLAYFIELD_WIDTH = 560;
    public static final double PLAYFIELD_HEIGHT = 560;

    // Player cannon: moves left and right along the bottom
    public static final double PLAYER_WIDTH = 36;
    public static final double PLAYER_HEIGHT = 16;
    public static final double PLAYER_Y = PLAYFIELD_HEIGHT - 30;   // center of the cannon
    public static final double PLAYER_SPEED = 220;                 // pixels per second
    public static final double PLAYER_SHOT_SPEED = 500;            // only one shot on screen at a time
    public static final int STARTING_LIVES = 3;

    // Alien formation
    public static final int ALIEN_ROWS = 5;
    public static final int ALIEN_COLUMNS = 11;
    public static final double ALIEN_SPACING_X = 40;
    public static final double ALIEN_SPACING_Y = 32;
    public static final double FORMATION_START_X = 80;             // center of the top-left alien
    public static final double FORMATION_START_Y = 90;

    // Shields
    public static final int SHIELD_COUNT = 4;
    public static final double SHIELD_TOP = PLAYER_Y - 80;

    private double playerX = PLAYFIELD_WIDTH / 2;
    private Bullet playerShot = null;
    private int lives = STARTING_LIVES;
    private int score = 0;
    private int wave = 1;

    private final List<Alien> aliens = new ArrayList<>();
    private final List<Shield> shields = new ArrayList<>();

    public SpaceInvadersMechanics() {
        startWave();
    }

    /**
     * Advances the game by one frame. dx is -1 (left), 0 or 1 (right) from the arrow keys,
     * and fireHeld is true while the fire key is down.
     */
    public void update(double deltaSeconds, int dx, boolean fireHeld) {
        movePlayer(deltaSeconds, dx);
        if (fireHeld && playerShot == null) {
            playerShot = new Bullet(playerX, PLAYER_Y - PLAYER_HEIGHT / 2, -PLAYER_SHOT_SPEED);
        }
        updateShot(deltaSeconds);
    }

    private void movePlayer(double deltaSeconds, int dx) {
        playerX += dx * PLAYER_SPEED * deltaSeconds;
        playerX = clamp(playerX, PLAYER_WIDTH / 2, PLAYFIELD_WIDTH - PLAYER_WIDTH / 2);
    }

    private void updateShot(double deltaSeconds) {
        if (playerShot == null) {
            return;
        }
        playerShot.update(deltaSeconds);
        if (playerShot.isOffScreen()) {
            playerShot = null;
            return;
        }
        // Shields stop the shot and lose a block
        for (Shield shield : shields) {
            if (shield.hit(playerShot.getLeft(), playerShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT)) {
                playerShot = null;
                return;
            }
        }
        // The first alien touched is destroyed and the shot is used up
        for (Alien alien : aliens) {
            if (rectsOverlap(playerShot.getLeft(), playerShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT,
                    alien.getLeft(), alien.getTop(), Alien.WIDTH, Alien.HEIGHT)) {
                aliens.remove(alien);
                score += alien.getType().getPoints();
                playerShot = null;
                return;
            }
        }
    }

    static boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Builds a fresh formation and fresh shields. */
    private void startWave() {
        aliens.clear();
        for (int row = 0; row < ALIEN_ROWS; row++) {
            Alien.Type type = row == 0 ? Alien.Type.SQUID : row <= 2 ? Alien.Type.CRAB : Alien.Type.OCTOPUS;
            for (int col = 0; col < ALIEN_COLUMNS; col++) {
                aliens.add(new Alien(FORMATION_START_X + col * ALIEN_SPACING_X,
                        FORMATION_START_Y + row * ALIEN_SPACING_Y, type, col));
            }
        }
        shields.clear();
        double gap = (PLAYFIELD_WIDTH - SHIELD_COUNT * Shield.WIDTH) / (SHIELD_COUNT + 1);
        for (int i = 0; i < SHIELD_COUNT; i++) {
            shields.add(new Shield(gap + i * (Shield.WIDTH + gap), SHIELD_TOP));
        }
    }

    public double getPlayerX() {
        return playerX;
    }

    public Bullet getPlayerShot() {
        return playerShot;
    }

    public int getLives() {
        return lives;
    }

    public int getScore() {
        return score;
    }

    public int getWave() {
        return wave;
    }

    public List<Alien> getAliens() {
        return aliens;
    }

    public List<Shield> getShields() {
        return shields;
    }

    // ---- Game pieces (kept inside this class to match the other games' two-class layout) ----

    /**
     * One alien in the formation. The row it starts in decides its type and points.
     */
    public static class Alien {
        public static final double WIDTH = 28;
        public static final double HEIGHT = 20;

        /** The three classic alien types, from the top row down. */
        public enum Type {
            SQUID(30), CRAB(20), OCTOPUS(10);

            private final int points;

            Type(int points) {
                this.points = points;
            }

            public int getPoints() {
                return points;
            }
        }

        private double x;
        private double y;
        private final Type type;
        private final int column;

        public Alien(double x, double y, Type type, int column) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.column = column;
        }

        public double getLeft() {
            return x - WIDTH / 2;
        }

        public double getRight() {
            return x + WIDTH / 2;
        }

        public double getTop() {
            return y - HEIGHT / 2;
        }

        public double getBottom() {
            return y + HEIGHT / 2;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public Type getType() {
            return type;
        }

        public int getColumn() {
            return column;
        }
    }

    /**
     * A shot moving straight up (the player's). Alien shots will reuse this in a later step.
     */
    public static class Bullet {
        public static final double WIDTH = 3;
        public static final double HEIGHT = 12;

        private final double x;
        private double y;
        private final double velocityY;

        public Bullet(double x, double y, double velocityY) {
            this.x = x;
            this.y = y;
            this.velocityY = velocityY;
        }

        public void update(double deltaSeconds) {
            y += velocityY * deltaSeconds;
        }

        /** True once the bullet has left the playfield. */
        public boolean isOffScreen() {
            return y + HEIGHT / 2 < 0 || y - HEIGHT / 2 > PLAYFIELD_HEIGHT;
        }

        public double getLeft() {
            return x - WIDTH / 2;
        }

        public double getTop() {
            return y - HEIGHT / 2;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    /**
     * A bunker the player hides behind, built from a grid of small blocks
     * that get knocked out one at a time.
     */
    public static class Shield {
        public static final int COLUMNS = 11;
        public static final int ROWS = 8;
        public static final double BLOCK_SIZE = 5;
        public static final double WIDTH = COLUMNS * BLOCK_SIZE;
        public static final double HEIGHT = ROWS * BLOCK_SIZE;

        private final double left;
        private final double top;
        private final boolean[][] blocks = new boolean[ROWS][COLUMNS];

        public Shield(double left, double top) {
            this.left = left;
            this.top = top;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    boolean cornerCut = row == 0 && (col == 0 || col == COLUMNS - 1);
                    boolean archCut = row >= ROWS - 3 && col >= 3 && col <= COLUMNS - 4;
                    blocks[row][col] = !cornerCut && !archCut;
                }
            }
        }

        /**
         * Knocks out the first block the rectangle overlaps, if any. Returns true if a block was hit,
         * so a bullet knows to disappear.
         */
        public boolean hit(double rectLeft, double rectTop, double rectWidth, double rectHeight) {
            int firstCol = (int) Math.floor((rectLeft - left) / BLOCK_SIZE);
            int lastCol = (int) Math.floor((rectLeft + rectWidth - left) / BLOCK_SIZE);
            int firstRow = (int) Math.floor((rectTop - top) / BLOCK_SIZE);
            int lastRow = (int) Math.floor((rectTop + rectHeight - top) / BLOCK_SIZE);
            // Check from the bottom up so the player's shot chips the shield from underneath
            for (int row = Math.min(ROWS - 1, lastRow); row >= Math.max(0, firstRow); row--) {
                for (int col = Math.max(0, firstCol); col <= Math.min(COLUMNS - 1, lastCol); col++) {
                    if (blocks[row][col]) {
                        blocks[row][col] = false;
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isBlock(int row, int col) {
            return blocks[row][col];
        }

        public int countBlocks() {
            int count = 0;
            for (boolean[] row : blocks) {
                for (boolean block : row) {
                    if (block) count++;
                }
            }
            return count;
        }

        public double getLeft() {
            return left;
        }

        public double getTop() {
            return top;
        }
    }
}
