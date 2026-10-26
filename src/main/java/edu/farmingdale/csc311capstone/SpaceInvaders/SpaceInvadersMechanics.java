package edu.farmingdale.csc311capstone.SpaceInvaders;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Game rules and state for classic Space Invaders. Kept free of JavaFX so it can be unit tested.
 *
 * Step 1 (graphics): the starting layout of the cannon, the alien formation and the shields.
 * Step 2 (movement): the cannon slides left and right and stops at the edges.
 * Step 3 (shooting): one shot on screen at a time. It destroys the first alien it touches
 * (adding that alien's points) or knocks a block out of a shield.
 * Step 4 (marching): the formation jumps sideways, drops and turns around at the edges, and speeds up
 * as aliens die. Aliens chew through shields they march into, and reaching the cannon's row ends the game.
 * Step 5 (alien fire and lives): the lowest alien in a random column fires down. Alien shots chip shields
 * and cost a life when they hit the cannon. The game pauses briefly while the cannon respawns, a bonus life
 * comes at 1500 points, and losing the last life ends the game.
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
    public static final int EXTRA_LIFE_SCORE = 1500;               // one bonus life, like the arcade
    public static final double RESPAWN_TIME = 1.5;                 // the game pauses while the cannon explodes

    // Alien formation
    public static final int ALIEN_ROWS = 5;
    public static final int ALIEN_COLUMNS = 11;
    public static final double ALIEN_SPACING_X = 40;
    public static final double ALIEN_SPACING_Y = 32;
    public static final double FORMATION_START_X = 80;             // center of the top-left alien
    public static final double FORMATION_START_Y = 90;
    public static final double ALIEN_STEP_X = 10;                  // the formation moves in little jumps
    public static final double ALIEN_STEP_DOWN = 16;               // and drops when it reaches an edge
    public static final double SLOWEST_STEP_TIME = 0.6;            // seconds between jumps with all 55 aliens
    public static final double FASTEST_STEP_TIME = 0.03;           // the last alien races
    public static final double ALIEN_SHOT_SPEED = 200;
    public static final double ALIEN_FIRE_INTERVAL = 0.9;          // average seconds between alien shots
    public static final int MAX_ALIEN_SHOTS = 3;

    // Shields
    public static final int SHIELD_COUNT = 4;
    public static final double SHIELD_TOP = PLAYER_Y - 80;

    private final Random random;

    private double playerX = PLAYFIELD_WIDTH / 2;
    private Bullet playerShot = null;
    private int lives = STARTING_LIVES;
    private boolean extraLifeGiven = false;
    private double respawnTimeLeft = 0;
    private int score = 0;
    private int wave = 1;

    private final List<Alien> aliens = new ArrayList<>();
    private int marchDirection = 1;                                 // 1 = right, -1 = left
    private double stepTimer = 0;
    private boolean aliensLanded = false;
    private final List<Bullet> alienShots = new ArrayList<>();
    private double alienFireTimer = ALIEN_FIRE_INTERVAL;
    private final List<Shield> shields = new ArrayList<>();

    public SpaceInvadersMechanics() {
        this(new Random());
    }

    /** Tests pass a seeded Random so alien fire is predictable. */
    SpaceInvadersMechanics(Random random) {
        this.random = random;
        startWave();
    }

    /**
     * Advances the game by one frame. dx is -1 (left), 0 or 1 (right) from the arrow keys,
     * and fireHeld is true while the fire key is down.
     */
    public void update(double deltaSeconds, int dx, boolean fireHeld) {
        if (isGameOver()) {
            return;
        }
        // While the cannon is exploding everything waits, like the arcade
        if (respawnTimeLeft > 0) {
            respawnTimeLeft = Math.max(0, respawnTimeLeft - deltaSeconds);
            return;
        }
        movePlayer(deltaSeconds, dx);
        if (fireHeld && playerShot == null) {
            playerShot = new Bullet(playerX, PLAYER_Y - PLAYER_HEIGHT / 2, -PLAYER_SHOT_SPEED);
        }
        marchAliens(deltaSeconds);
        updateAlienFire(deltaSeconds);
        updateShot(deltaSeconds);
        updateAlienShots(deltaSeconds);
    }

    /** Time between formation jumps: slow with a full formation, faster as aliens die. */
    public double getStepTime() {
        double fraction = aliens.size() / (double) (ALIEN_ROWS * ALIEN_COLUMNS);
        return FASTEST_STEP_TIME + (SLOWEST_STEP_TIME - FASTEST_STEP_TIME) * fraction;
    }

    private void marchAliens(double deltaSeconds) {
        stepTimer += deltaSeconds;
        while (stepTimer >= getStepTime() && !aliens.isEmpty()) {
            stepTimer -= getStepTime();
            stepFormation();
        }
    }

    /** One jump sideways, or a drop and turn around if the next jump would leave the playfield. */
    private void stepFormation() {
        boolean hitsEdge = false;
        for (Alien alien : aliens) {
            double nextLeft = alien.getLeft() + marchDirection * ALIEN_STEP_X;
            double nextRight = alien.getRight() + marchDirection * ALIEN_STEP_X;
            if (nextLeft < 0 || nextRight > PLAYFIELD_WIDTH) {
                hitsEdge = true;
                break;
            }
        }
        for (Alien alien : aliens) {
            if (hitsEdge) {
                alien.move(0, ALIEN_STEP_DOWN);
            } else {
                alien.move(marchDirection * ALIEN_STEP_X, 0);
            }
            // Aliens chew through shields they march into
            for (Shield shield : shields) {
                shield.erase(alien.getLeft(), alien.getTop(), Alien.WIDTH, Alien.HEIGHT);
            }
            // Reaching the cannon's row ends the game
            if (alien.getBottom() >= PLAYER_Y - PLAYER_HEIGHT / 2) {
                aliensLanded = true;
            }
        }
        if (hitsEdge) {
            marchDirection = -marchDirection;
        }
    }

    private void movePlayer(double deltaSeconds, int dx) {
        playerX += dx * PLAYER_SPEED * deltaSeconds;
        playerX = clamp(playerX, PLAYER_WIDTH / 2, PLAYFIELD_WIDTH - PLAYER_WIDTH / 2);
    }

    private void updateAlienFire(double deltaSeconds) {
        alienFireTimer -= deltaSeconds;
        if (alienFireTimer > 0) {
            return;
        }
        alienFireTimer += ALIEN_FIRE_INTERVAL * (0.5 + random.nextDouble());
        if (alienShots.size() >= MAX_ALIEN_SHOTS || aliens.isEmpty()) {
            return;
        }
        // A random column fires, from its lowest alien so shots never pass through other aliens
        Alien shooter = aliens.get(random.nextInt(aliens.size()));
        for (Alien alien : aliens) {
            if (alien.getColumn() == shooter.getColumn() && alien.getY() > shooter.getY()) {
                shooter = alien;
            }
        }
        alienShots.add(new Bullet(shooter.getX(), shooter.getBottom(), ALIEN_SHOT_SPEED));
    }

    private void updateAlienShots(double deltaSeconds) {
        for (Bullet shot : alienShots) {
            shot.update(deltaSeconds);
        }
        alienShots.removeIf(Bullet::isOffScreen);
        alienShots.removeIf(this::hitsShield);
        for (Bullet shot : alienShots) {
            if (rectsOverlap(shot.getLeft(), shot.getTop(), Bullet.WIDTH, Bullet.HEIGHT,
                    playerX - PLAYER_WIDTH / 2, PLAYER_Y - PLAYER_HEIGHT / 2, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                loseLife();
                return;
            }
        }
    }

    private boolean hitsShield(Bullet bullet) {
        for (Shield shield : shields) {
            if (shield.hit(bullet.getLeft(), bullet.getTop(), Bullet.WIDTH, Bullet.HEIGHT)) {
                return true;
            }
        }
        return false;
    }

    private void loseLife() {
        lives--;
        respawnTimeLeft = RESPAWN_TIME;
        alienShots.clear();
        playerShot = null;
        playerX = PLAYFIELD_WIDTH / 2;
    }

    private void addScore(int points) {
        score += points;
        if (!extraLifeGiven && score >= EXTRA_LIFE_SCORE) {
            extraLifeGiven = true;
            lives++;
        }
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
        if (hitsShield(playerShot)) {
            playerShot = null;
            return;
        }
        // Meeting an alien shot head on destroys both
        for (Bullet alienShot : alienShots) {
            if (rectsOverlap(playerShot.getLeft(), playerShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT,
                    alienShot.getLeft(), alienShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT)) {
                alienShots.remove(alienShot);
                playerShot = null;
                return;
            }
        }
        // The first alien touched is destroyed and the shot is used up
        for (Alien alien : aliens) {
            if (rectsOverlap(playerShot.getLeft(), playerShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT,
                    alien.getLeft(), alien.getTop(), Alien.WIDTH, Alien.HEIGHT)) {
                aliens.remove(alien);
                addScore(alien.getType().getPoints());
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

    /** Puts an alien shot on screen. Used by tests. */
    void addAlienShot(Bullet shot) {
        alienShots.add(shot);
    }

    /** Stops aliens from firing on their own. Used by tests. */
    void disableAlienFire() {
        alienFireTimer = Double.POSITIVE_INFINITY;
    }

    /** Adds points as if something was shot. Used by tests. */
    void addScoreForTest(int points) {
        addScore(points);
    }

    /** True once the aliens reach the cannon or the last life is lost. */
    public boolean isGameOver() {
        return lives <= 0 || aliensLanded;
    }

    public boolean isRespawning() {
        return respawnTimeLeft > 0;
    }

    public List<Bullet> getAlienShots() {
        return alienShots;
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

        public void move(double dx, double dy) {
            x += dx;
            y += dy;
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
     * A shot moving straight up (the player's) or straight down (an alien's).
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

        /** Knocks out every block the rectangle overlaps (used when aliens march through). */
        public void erase(double rectLeft, double rectTop, double rectWidth, double rectHeight) {
            while (hit(rectLeft, rectTop, rectWidth, rectHeight)) {
                // keep clearing until nothing under the rectangle is left
            }
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
