package edu.farmingdale.csc311capstone.SpaceInvaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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
    public static final double WAVE_START_DROP = 16;               // each new wave starts this much lower
    public static final double MAX_WAVE_START_DROP = 96;
    public static final double ALIEN_STEP_X = 10;                  // the formation moves in little jumps
    public static final double ALIEN_STEP_DOWN = 16;               // and drops when it reaches an edge
    public static final double SLOWEST_STEP_TIME = 0.6;            // seconds between jumps with all 55 aliens
    public static final double FASTEST_STEP_TIME = 0.03;           // the last alien races
    public static final double ALIEN_SHOT_SPEED = 200;
    public static final double ALIEN_FIRE_INTERVAL = 0.9;          // average seconds between alien shots
    public static final int MAX_ALIEN_SHOTS = 3;

    // Mystery ship that sometimes crosses the top
    public static final double MYSTERY_Y = 50;
    public static final double MYSTERY_WIDTH = 40;
    public static final double MYSTERY_HEIGHT = 16;
    public static final double MYSTERY_SPEED = 120;
    public static final double MYSTERY_MIN_WAIT = 20;
    public static final double MYSTERY_MAX_WAIT = 30;
    public static final int[] MYSTERY_POINTS = {50, 100, 150, 300};

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

    private boolean mysteryActive = false;
    private double mysteryX;
    private int mysteryDirection;
    private double mysteryTimer;
    private int lastMysteryPoints = 0;

    public SpaceInvadersMechanics() {
        this(new Random());
    }

    /** Tests pass a seeded Random so alien fire is predictable. */
    SpaceInvadersMechanics(Random random) {
        this.random = random;
        mysteryTimer = nextMysteryWait();
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
        updateMystery(deltaSeconds);
        updateShot(deltaSeconds);
        updateAlienShots(deltaSeconds);
        if (aliens.isEmpty()) {
            wave++;
            startWave();
        }
    }

    private void updateMystery(double deltaSeconds) {
        if (mysteryActive) {
            mysteryX += mysteryDirection * MYSTERY_SPEED * deltaSeconds;
            if (mysteryX < -MYSTERY_WIDTH || mysteryX > PLAYFIELD_WIDTH + MYSTERY_WIDTH) {
                mysteryActive = false;
                mysteryTimer = nextMysteryWait();
            }
            return;
        }
        mysteryTimer -= deltaSeconds;
        if (mysteryTimer <= 0) {
            startMystery(random.nextBoolean() ? 1 : -1);
        }
    }

    /** Sends the mystery ship across from the left (direction 1) or the right (direction -1). */
    private void startMystery(int direction) {
        mysteryActive = true;
        mysteryDirection = direction;
        mysteryX = direction > 0 ? -MYSTERY_WIDTH / 2 : PLAYFIELD_WIDTH + MYSTERY_WIDTH / 2;
    }

    private double nextMysteryWait() {
        return MYSTERY_MIN_WAIT + random.nextDouble() * (MYSTERY_MAX_WAIT - MYSTERY_MIN_WAIT);
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
        if (mysteryActive && rectsOverlap(playerShot.getLeft(), playerShot.getTop(), Bullet.WIDTH, Bullet.HEIGHT,
                mysteryX - MYSTERY_WIDTH / 2, MYSTERY_Y - MYSTERY_HEIGHT / 2, MYSTERY_WIDTH, MYSTERY_HEIGHT)) {
            lastMysteryPoints = MYSTERY_POINTS[random.nextInt(MYSTERY_POINTS.length)];
            addScore(lastMysteryPoints);
            mysteryActive = false;
            mysteryTimer = nextMysteryWait();
            playerShot = null;
        }
    }

    static boolean rectsOverlap(double ax, double ay, double aw, double ah,
                                double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Builds a fresh formation (a little lower each wave) and fresh shields. */
    private void startWave() {
        aliens.clear();
        alienShots.clear();
        playerShot = null;
        marchDirection = 1;
        stepTimer = 0;
        double drop = Math.min(MAX_WAVE_START_DROP, (wave - 1) * WAVE_START_DROP);
        for (int row = 0; row < ALIEN_ROWS; row++) {
            Alien.Type type = row == 0 ? Alien.Type.SQUID : row <= 2 ? Alien.Type.CRAB : Alien.Type.OCTOPUS;
            for (int col = 0; col < ALIEN_COLUMNS; col++) {
                aliens.add(new Alien(FORMATION_START_X + col * ALIEN_SPACING_X,
                        FORMATION_START_Y + drop + row * ALIEN_SPACING_Y, type, col));
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

    /** Starts the mystery ship right away at x. Used by tests. */
    void startMysteryForTest(int direction, double x) {
        startMystery(direction);
        mysteryX = x;
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

    public boolean isMysteryActive() {
        return mysteryActive;
    }

    public double getMysteryX() {
        return mysteryX;
    }

    /** Points from the last mystery ship hit, 0 if none has been hit yet. */
    public int getLastMysteryPoints() {
        return lastMysteryPoints;
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

    // ---- Extermination: the bullet-hell game mode ----

    /**
     * Game rules and state for Extermination, the bullet-hell game mode. Kept inside this class so the
     * game stays in its two files.
     *
     * The player ship flies freely in 8 directions, slows down while focused, and holds fire for a
     * steady stream of shots. In the sandbox, enemies (the Space Invaders aliens) are placed by hand to
     * lay out a level. Each one has:
     * - a flight path: waypoints it flies through in order at its own speed, once or on a loop. Any
     *   point (the start too) can be a stop, where the enemy holds still for a set time before going on
     * - a spawn time: how many seconds into the level it shows up
     * - a count: how many enemies fly that path, one behind the other
     * There are two ways to spawn. An enemy that starts off-screen (placed at the edge) flies in from
     * the side; one that starts on screen appears out of nowhere, fading in before it moves.
     * Each group also has a bullet pattern (aimed, fan, ring, spiral or random), which starts out as the
     * alien type's own shot and can be changed. Enemy bullets that touch the ship's tiny hitbox count as
     * a hit. A shot destroys an enemy, and a reset starts the level over.
     *
     * Enemy groups are organised into waves. The first wave starts with the level; each later wave
     * starts once the wave before it is cleared (every enemy spawned and then shot or flown off) or
     * once it has started, and then waits a few seconds. A group's spawn time counts from the start
     * of its own wave, so waves can be timed without knowing how long the player takes.
     *
     * Levels are saved as text (see {@link #saveLevel()} and {@link #loadLevel(String)}) and kept in
     * {@link #STAGES}, so a level designed in the sandbox can be copied and pasted in here.
     */
    public static final class Extermination {
        /**
         * The Extermination stages, as level text. # starts a comment. To save a level, press Copy level
         * in the sandbox and paste it between a stage's quotes. The text is a list of waves, each
         * followed by its enemy groups:
         * WAVE number after=cleared/started wait=seconds
         * TYPE start=x,y speed= count= spawn= loop=yes/no shot=PATTERN bullets= spread= bulletSpeed= every= first= spin= fire=always/stopped path=x,y x,y ...
         * A point written x,y@seconds is a stop: the enemy waits there that long, then flies on (start=x,y@2 too).
         * fire=stopped means it only shoots while it's holding still. first= is how many seconds after it
         * comes on screen it fires its first shot (left out: half of every=).
         * spawn is seconds after the wave starts. A start or path point past the edge of the 560x560
         * playfield is off-screen.
         */
        public static final String[] STAGES = {
                // Stage 1 (an example to start from)
                """
                # Wave 1: a line of squids sweeps in from the left and flies off to the right
                WAVE 1 after=cleared wait=1.0
                SQUID start=-28,90 speed=160 count=5 spawn=0.0 loop=no shot=AIMED bullets=1 spread=0 bulletSpeed=200 every=1.5 spin=15 path=200,90 200,230 588,230
                # Wave 2: 2 seconds after wave 1 is gone, the same from the right
                WAVE 2 after=cleared wait=2.0
                SQUID start=588,130 speed=160 count=5 spawn=0.0 loop=no shot=AIMED bullets=1 spread=0 bulletSpeed=200 every=1.5 spin=15 path=360,130 360,270 -28,270
                # Wave 3: a crab that has to be shot, then two octopuses a second later
                WAVE 3 after=cleared wait=2.0
                CRAB start=280,160 speed=60 count=1 spawn=0.0 loop=yes shot=FAN bullets=5 spread=60 bulletSpeed=160 every=1.5 spin=15 path=200,160 360,160
                OCTOPUS start=140,110 speed=120 count=1 spawn=1.0 loop=no shot=RING bullets=12 spread=0 bulletSpeed=120 every=2.0 spin=15 path=
                OCTOPUS start=420,110 speed=120 count=1 spawn=1.0 loop=no shot=SPIRAL bullets=4 spread=0 bulletSpeed=130 every=0.3 spin=12 path=
                """,
                // Stage 2
                "",
                // Stage 3
                "",
                // Stage 4
                ""
        };

        // Player ship
        public static final double PLAYER_SPEED = 260;             // pixels per second
        public static final double FOCUS_SPEED = 110;              // slower while focused, for dodging
        public static final double PLAYER_WIDTH = 24;              // size of the ship drawing
        public static final double PLAYER_HEIGHT = 28;
        public static final double HITBOX_RADIUS = 3;              // only this small dot can be hit
        public static final double PLAYER_START_Y = PLAYFIELD_HEIGHT - 60;

        // Player shots
        public static final double SHOT_SPEED = 700;
        public static final double SHOT_INTERVAL = 0.08;           // seconds between shots while fire is held
        public static final double SHOT_WIDTH = 4;
        public static final double SHOT_HEIGHT = 14;

        // Enemies
        public static final double PICK_RADIUS = 20;               // how close a click must be to pick an enemy
        public static final double ENEMY_SPEED = 120;              // default flight speed, pixels per second
        public static final double MIN_ENEMY_SPEED = 20;
        public static final double MAX_ENEMY_SPEED = 400;
        public static final double EDGE_SNAP = 25;                 // placed this close to an edge, it starts off-screen
        public static final double FOLLOW_SPACING = 40;            // pixels between enemies sharing one path
        public static final int MAX_COUNT = 20;
        public static final double MAX_SPAWN_TIME = 120;           // seconds after the enemy's wave starts
        public static final double MAX_FIRST_SHOT_TIME = 60;       // seconds on screen before the first shot, at most
        public static final double MAX_STOP_TIME = 30;             // seconds an enemy can hold still at a stop
        public static final double MAX_WAVE_WAIT = 30;             // seconds a wave can wait before starting
        public static final double APPEAR_TIME = 0.4;              // seconds an on-screen spawn takes to fade in

        // Enemy bullets
        public static final double BULLET_RADIUS = 4;
        public static final double HIT_INVINCIBLE_TIME = 1;        // after a hit, the ship can't be hit again for this long
        public static final int MAX_BULLETS_PER_SHOT = 200;
        public static final double MIN_BULLET_SPEED = 40;
        public static final double MAX_BULLET_SPEED = 600;
        public static final double MIN_FIRE_INTERVAL = 0.05;      // seconds between shots
        /** Most enemy bullets on screen at once; past this new shots are skipped, so a huge pattern can't freeze the game. */
        public static final int MAX_ENEMY_BULLETS = 4000;
        public static final double MAX_FIRE_INTERVAL = 5;

        /** The shapes an enemy's shot can take. */
        public enum Pattern {
            NONE,    // doesn't shoot
            AIMED,   // straight at the player; more bullets make a quick line of them
            FAN,     // bullets spread evenly over an angle, centred on the player
            RING,    // a full circle of bullets
            SPIRAL,  // a circle that turns a little after every shot
            RANDOM   // bullets sprayed at random angles within the spread, toward the player
        }

        private double playerX = PLAYFIELD_WIDTH / 2;
        private double playerY = PLAYER_START_Y;
        private boolean focused = false;
        private double shotCooldown = 0;
        /** Player shots, moving straight up. Each one is its x and y. */
        private final List<double[]> shots = new ArrayList<>();
        /** The enemies as they were designed: the level layout. */
        private final List<EnemyDesign> layout = new ArrayList<>();
        /** The enemies flying right now. */
        private final List<Enemy> enemies = new ArrayList<>();
        /** Seconds since the level (re)started. */
        private double levelTime = 0;
        /** The waves' settings, in order. There's always at least one. */
        private final List<Wave> waves = new ArrayList<>(List.of(new Wave()));
        /** The wave this run started from (0 unless playing from a later wave). */
        private int firstWave = 0;
        /** When each wave started and was cleared this run, in level time; -1 for not yet. */
        private final List<Double> waveStartTimes = new ArrayList<>();
        private final List<Double> waveClearTimes = new ArrayList<>();
        /** How many of each design's enemies have spawned so far this run. */
        private final Map<EnemyDesign, Integer> spawned = new HashMap<>();
        /** Enemy bullets. Each one is {x, y, x speed, y speed}. */
        private final List<double[]> enemyBullets = new ArrayList<>();
        /** How many times the ship has been hit this run. */
        private int hits = 0;
        /** Seconds left before the ship can be hit again. */
        private double invincibleTimeLeft = 0;
        private final Random random = new Random();

        /**
         * Advances the game by one frame. dx and dy are -1, 0 or 1 from the arrow keys, focusHeld slows
         * the ship down, and fireHeld fires a stream of shots.
         */
        public void update(double deltaSeconds, int dx, int dy, boolean focusHeld, boolean fireHeld) {
            levelTime += deltaSeconds;
            focused = focusHeld;
            movePlayer(deltaSeconds, dx, dy);

            shotCooldown = Math.max(0, shotCooldown - deltaSeconds);
            if (fireHeld && shotCooldown == 0) {
                shots.add(new double[]{playerX, playerY - PLAYER_HEIGHT / 2});
                shotCooldown = SHOT_INTERVAL;
            }
            for (double[] shot : shots) {
                shot[1] -= SHOT_SPEED * deltaSeconds;
            }
            shots.removeIf(shot -> shot[1] + SHOT_HEIGHT / 2 < 0);

            updateWaves();
            spawnDueEnemies();
            for (Enemy enemy : enemies) {
                enemy.fly(deltaSeconds);
                if (enemy.readyToFire(deltaSeconds)) {
                    fire(enemy);
                }
            }
            enemies.removeIf(Enemy::hasLeftScreen);
            shots.removeIf(this::hitEnemy);
            moveEnemyBullets(deltaSeconds);
        }

        /** Fires one shot of the enemy's pattern from where it is. */
        private void fire(Enemy enemy) {
            EnemyDesign design = enemy.getDesign();
            int count = design.getBulletCount();
            double speed = design.getBulletSpeed();
            double spread = Math.toRadians(design.getSpread());
            double aim = Math.atan2(playerY - enemy.getY(), playerX - enemy.getX());
            switch (design.getPattern()) {
                case AIMED -> {
                    // A line of bullets: same direction, each a little faster than the one before
                    for (int i = 0; i < count; i++) {
                        addEnemyBullet(enemy, aim, speed * (1 + 0.15 * i));
                    }
                }
                case FAN -> {
                    for (int i = 0; i < count; i++) {
                        double offset = count == 1 ? 0 : -spread / 2 + spread * i / (count - 1);
                        addEnemyBullet(enemy, aim + offset, speed);
                    }
                }
                case RING, SPIRAL -> {
                    // Starts pointing straight down; a spiral's start turns a bit more after every shot
                    double start = Math.PI / 2 + Math.toRadians(enemy.getSpinAngle());
                    for (int i = 0; i < count; i++) {
                        addEnemyBullet(enemy, start + 2 * Math.PI * i / count, speed);
                    }
                    if (design.getPattern() == Pattern.SPIRAL) {
                        enemy.addSpin(design.getSpin());
                    }
                }
                case RANDOM -> {
                    for (int i = 0; i < count; i++) {
                        double offset = (random.nextDouble() - 0.5) * spread;
                        addEnemyBullet(enemy, aim + offset, speed * (0.7 + random.nextDouble() * 0.6));
                    }
                }
                case NONE -> { }
            }
        }

        private void addEnemyBullet(Enemy enemy, double angle, double speed) {
            if (enemyBullets.size() >= MAX_ENEMY_BULLETS) {
                return;
            }
            enemyBullets.add(new double[]{enemy.getX(), enemy.getY(), Math.cos(angle) * speed, Math.sin(angle) * speed});
        }

        /** Moves the enemy bullets, drops the ones that left the screen, and checks them against the hitbox. */
        private void moveEnemyBullets(double deltaSeconds) {
            invincibleTimeLeft = Math.max(0, invincibleTimeLeft - deltaSeconds);
            for (double[] bullet : enemyBullets) {
                bullet[0] += bullet[2] * deltaSeconds;
                bullet[1] += bullet[3] * deltaSeconds;
            }
            enemyBullets.removeIf(b -> b[0] < -BULLET_RADIUS || b[0] > PLAYFIELD_WIDTH + BULLET_RADIUS
                    || b[1] < -BULLET_RADIUS || b[1] > PLAYFIELD_HEIGHT + BULLET_RADIUS);
            if (invincibleTimeLeft > 0) {
                return;
            }
            for (double[] bullet : enemyBullets) {
                // Only the tiny hitbox counts, so bullets can graze the ship drawing
                if (Math.hypot(bullet[0] - playerX, bullet[1] - playerY) < BULLET_RADIUS + HITBOX_RADIUS) {
                    hits++;
                    invincibleTimeLeft = HIT_INVINCIBLE_TIME;
                    enemyBullets.remove(bullet);
                    return;
                }
            }
        }

        /**
         * Marks waves that have been cleared, and starts the next wave once its rule allows: the first
         * wave of the run after its wait, and every other wave after the previous one is cleared (or
         * has started) plus its wait. Waves start in order.
         */
        private void updateWaves() {
            while (waveStartTimes.size() < waves.size()) {
                waveStartTimes.add(-1.0);
                waveClearTimes.add(-1.0);
            }
            for (int k = firstWave; k < waves.size(); k++) {
                if (isWaveStarted(k) && waveClearTimes.get(k) < 0 && isWaveDone(k)) {
                    waveClearTimes.set(k, levelTime);
                }
            }
            for (int k = firstWave; k < waves.size(); k++) {
                if (isWaveStarted(k)) {
                    continue;
                }
                Wave wave = waves.get(k);
                double readyAt;
                if (k == firstWave) {
                    readyAt = k == 0 ? wave.getWait() : 0; // playing from a later wave starts it right away
                } else if (wave.isAfterCleared()) {
                    readyAt = waveClearTimes.get(k - 1) < 0 ? -1 : waveClearTimes.get(k - 1) + wave.getWait();
                } else {
                    readyAt = waveStartTimes.get(k - 1) < 0 ? -1 : waveStartTimes.get(k - 1) + wave.getWait();
                }
                if (readyAt >= 0 && levelTime >= readyAt) {
                    waveStartTimes.set(k, levelTime);
                } else {
                    break; // later waves wait for this one
                }
            }
        }

        /** True when every enemy of the wave has spawned and none of them is still flying. */
        private boolean isWaveDone(int wave) {
            for (EnemyDesign design : layout) {
                if (design.getWave() == wave && spawned.getOrDefault(design, 0) < design.getCount()) {
                    return false;
                }
            }
            for (Enemy enemy : enemies) {
                if (enemy.getDesign().getWave() == wave) {
                    return false;
                }
            }
            return true;
        }

        /** Spawns every enemy whose time has come, counting from the start of its wave. */
        private void spawnDueEnemies() {
            for (EnemyDesign design : layout) {
                int wave = design.getWave();
                if (!isWaveStarted(wave)) {
                    continue;
                }
                double timeInWave = levelTime - waveStartTimes.get(wave);
                int count = spawned.getOrDefault(design, 0);
                while (count < design.getCount() && timeInWave >= design.spawnTimeOf(count)) {
                    enemies.add(new Enemy(design));
                    count++;
                }
                spawned.put(design, count);
            }
        }

        public boolean isWaveStarted(int wave) {
            return wave < waveStartTimes.size() && waveStartTimes.get(wave) >= 0;
        }

        public boolean isWaveCleared(int wave) {
            return wave < waveClearTimes.size() && waveClearTimes.get(wave) >= 0;
        }

        /** The latest wave that has started this run, or -1 before the first one starts. */
        public int getRunningWave() {
            int running = -1;
            for (int k = firstWave; k < waves.size(); k++) {
                if (isWaveStarted(k)) running = k;
            }
            return running;
        }

        /** Seconds since the given wave started, or 0 if it hasn't. */
        public double getTimeInWave(int wave) {
            return isWaveStarted(wave) ? levelTime - waveStartTimes.get(wave) : 0;
        }

        /** True once every wave of the run has been cleared. */
        public boolean isLevelClear() {
            for (int k = firstWave; k < waves.size(); k++) {
                if (!isWaveCleared(k)) return false;
            }
            return true;
        }

        public List<Wave> getWaves() {
            return waves;
        }

        /** Adds an empty wave at the end and returns its index. */
        public int addWave() {
            waves.add(new Wave());
            return waves.size() - 1;
        }

        private void movePlayer(double deltaSeconds, int dx, int dy) {
            double speed = focused ? FOCUS_SPEED : PLAYER_SPEED;
            // Diagonals move at the same speed as straight lines
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length > 0) {
                playerX += dx / length * speed * deltaSeconds;
                playerY += dy / length * speed * deltaSeconds;
            }
            playerX = Math.max(PLAYER_WIDTH / 2, Math.min(PLAYFIELD_WIDTH - PLAYER_WIDTH / 2, playerX));
            playerY = Math.max(PLAYER_HEIGHT / 2, Math.min(PLAYFIELD_HEIGHT - PLAYER_HEIGHT / 2, playerY));
        }

        /** Destroys the first enemy the shot touches. Returns true when it hit one, so the shot goes away. */
        private boolean hitEnemy(double[] shot) {
            for (Enemy enemy : enemies) {
                if (!enemy.isAppearing()
                        && Math.abs(shot[0] - enemy.getX()) < (Alien.WIDTH + SHOT_WIDTH) / 2
                        && Math.abs(shot[1] - enemy.getY()) < (Alien.HEIGHT + SHOT_HEIGHT) / 2) {
                    enemies.remove(enemy);
                    return true;
                }
            }
            return false;
        }

        /**
         * Adds an enemy to the level layout at (x, y). A spot within EDGE_SNAP of an edge is moved just
         * off that edge, so the enemy flies in from the side. It joins the given wave and spawns on
         * schedule once that wave has started.
         */
        public EnemyDesign placeEnemy(Alien.Type type, double x, double y, int wave) {
            EnemyDesign design = new EnemyDesign(type, snapX(x), snapY(y));
            design.setWave(wave);
            layout.add(design);
            return design;
        }

        /** Moves an x within EDGE_SNAP of the left or right edge to just off that edge. */
        static double snapX(double x) {
            if (x < EDGE_SNAP) return -Alien.WIDTH;
            if (x > PLAYFIELD_WIDTH - EDGE_SNAP) return PLAYFIELD_WIDTH + Alien.WIDTH;
            return x;
        }

        /** Moves a y within EDGE_SNAP of the top or bottom edge to just off that edge. */
        static double snapY(double y) {
            if (y < EDGE_SNAP) return -Alien.HEIGHT;
            if (y > PLAYFIELD_HEIGHT - EDGE_SNAP) return PLAYFIELD_HEIGHT + Alien.HEIGHT;
            return y;
        }

        /**
         * The placed enemy of the given wave whose start marker is nearest to (x, y), or null if none is
         * within PICK_RADIUS.
         */
        public EnemyDesign findEnemyNear(double x, double y, int wave) {
            EnemyDesign nearest = null;
            double best = PICK_RADIUS;
            for (EnemyDesign design : layout) {
                if (design.getWave() != wave) {
                    continue;
                }
                double distance = Math.hypot(design.getMarkerX() - x, design.getMarkerY() - y);
                if (distance <= best) {
                    best = distance;
                    nearest = design;
                }
            }
            return nearest;
        }

        /** Removes the wave's placed enemy nearest to (x, y). Returns the one removed, or null if none was close enough. */
        public EnemyDesign removeEnemyNear(double x, double y, int wave) {
            EnemyDesign removed = findEnemyNear(x, y, wave);
            if (removed != null) {
                removeEnemy(removed);
            }
            return removed;
        }

        /** Removes one placed enemy from the layout, along with any of its enemies flying right now. */
        public void removeEnemy(EnemyDesign design) {
            layout.remove(design);
            spawned.remove(design);
            enemies.removeIf(enemy -> enemy.getDesign() == design);
        }

        /**
         * The level layout as text, one line per enemy group, in the format described at {@link #STAGES}.
         * Paste it into STAGES to keep it.
         */
        public String saveLevel() {
            StringBuilder text = new StringBuilder();
            for (int k = 0; k < waves.size(); k++) {
                Wave wave = waves.get(k);
                text.append(String.format(Locale.ROOT, "WAVE %d after=%s wait=%.1f%n", k + 1,
                        wave.isAfterCleared() ? "cleared" : "started", wave.getWait()));
                for (EnemyDesign d : layout) {
                    if (d.getWave() == k) {
                        appendEnemyLine(text, d);
                    }
                }
            }
            return text.toString();
        }

        private static void appendEnemyLine(StringBuilder text, EnemyDesign d) {
                text.append(String.format(Locale.ROOT,
                        "%s start=%s speed=%d count=%d spawn=%.1f loop=%s shot=%s bullets=%d spread=%d bulletSpeed=%d every=%.2f first=%.1f spin=%d fire=%s path=",
                        d.getType(), pointText(d.getStartX(), d.getStartY(), d.getStopTime(0)), Math.round(d.getSpeed()),
                        d.getCount(), d.getSpawnTime(), d.isLoop() ? "yes" : "no", d.getPattern(), d.getBulletCount(),
                        Math.round(d.getSpread()), Math.round(d.getBulletSpeed()), d.getFireInterval(), d.getFirstShotDelay(),
                        Math.round(d.getSpin()), d.isFireOnlyWhenStopped() ? "stopped" : "always"));
                List<String> points = new ArrayList<>();
                for (int i = 1; i <= d.getWaypoints().size(); i++) {
                    double[] point = d.getWaypoints().get(i - 1);
                    points.add(pointText(point[0], point[1], d.getStopTime(i)));
                }
                text.append(String.join(" ", points)).append("\n");
        }

        /** A point as level text: "x,y", or "x,y@seconds" when it's a stop. */
        private static String pointText(double x, double y, double stopTime) {
            String point = Math.round(x) + "," + Math.round(y);
            return stopTime > 0 ? point + String.format(Locale.ROOT, "@%.1f", stopTime) : point;
        }

        /**
         * Replaces the layout with the level in the text (the format described at {@link #STAGES}) and
         * starts it from time 0. Enemy lines belong to the WAVE line above them (wave 1 if there is
         * none). Settings left out of a line keep the alien type's or wave's defaults.
         * Throws IllegalArgumentException, naming the line, if the text can't be read; the layout is
         * left as it was.
         */
        public void loadLevel(String text) {
            List<EnemyDesign> loaded = new ArrayList<>();
            List<Wave> loadedWaves = new ArrayList<>(List.of(new Wave()));
            int currentWave = 0;
            String[] lines = text.split("\\R");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                try {
                    if (line.toUpperCase().startsWith("WAVE")) {
                        currentWave = parseWaveLine(line, loadedWaves);
                    } else {
                        EnemyDesign design = parseEnemyLine(line);
                        design.setWave(currentWave);
                        loaded.add(design);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": expected a number but found "
                            + e.getMessage().replace("For input string: ", ""), e);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
                }
            }
            layout.clear();
            layout.addAll(loaded);
            waves.clear();
            waves.addAll(loadedWaves);
            resetEnemies();
        }

        /**
         * Reads a "WAVE number after=cleared/started wait=seconds" line, adding waves up to that number,
         * and returns the wave's index.
         */
        private static int parseWaveLine(String line, List<Wave> waves) {
            String[] words = line.split("\\s+");
            if (words.length < 2) {
                throw new IllegalArgumentException("WAVE needs a number, like WAVE 2");
            }
            int number = Integer.parseInt(words[1]);
            if (number < 1) {
                throw new IllegalArgumentException("wave numbers start at 1");
            }
            while (waves.size() < number) {
                waves.add(new Wave());
            }
            Wave wave = waves.get(number - 1);
            for (int i = 2; i < words.length; i++) {
                String[] keyValue = words[i].split("=", 2);
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("expected key=value but found \"" + words[i] + "\"");
                }
                switch (keyValue[0]) {
                    case "after" -> wave.setAfterCleared(!keyValue[1].equalsIgnoreCase("started"));
                    case "wait" -> wave.setWait(Double.parseDouble(keyValue[1]));
                    default -> throw new IllegalArgumentException("unknown wave setting \"" + keyValue[0] + "\"");
                }
            }
            return number - 1;
        }

        /** Reads one line of level text into an enemy design. */
        private static EnemyDesign parseEnemyLine(String line) {
            // Everything after "path=" is the list of points; the rest is the type and key=value settings
            String path = "";
            int pathStart = line.indexOf("path=");
            if (pathStart >= 0) {
                path = line.substring(pathStart + "path=".length()).strip();
                line = line.substring(0, pathStart).strip();
            }
            String[] words = line.split("\\s+");
            Alien.Type type = Alien.Type.valueOf(words[0].toUpperCase());
            Map<String, String> settings = new HashMap<>();
            for (int i = 1; i < words.length; i++) {
                String[] keyValue = words[i].split("=", 2);
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("expected key=value but found \"" + words[i] + "\"");
                }
                settings.put(keyValue[0], keyValue[1]);
            }
            if (!settings.containsKey("start")) {
                throw new IllegalArgumentException("missing start=x,y");
            }
            double[] start = parsePoint(settings.get("start"));
            EnemyDesign design = new EnemyDesign(type, start[0], start[1]);
            design.setStopTime(0, start[2]);
            if (settings.containsKey("speed")) design.setSpeed(Double.parseDouble(settings.get("speed")));
            if (settings.containsKey("count")) design.setCount(Integer.parseInt(settings.get("count")));
            if (settings.containsKey("spawn")) design.setSpawnTime(Double.parseDouble(settings.get("spawn")));
            if (settings.containsKey("loop")) design.setLoop(settings.get("loop").equalsIgnoreCase("yes"));
            if (settings.containsKey("shot")) design.setPattern(Pattern.valueOf(settings.get("shot").toUpperCase()));
            if (settings.containsKey("bullets")) design.setBulletCount(Integer.parseInt(settings.get("bullets")));
            if (settings.containsKey("spread")) design.setSpread(Double.parseDouble(settings.get("spread")));
            if (settings.containsKey("bulletSpeed")) design.setBulletSpeed(Double.parseDouble(settings.get("bulletSpeed")));
            if (settings.containsKey("every")) design.setFireInterval(Double.parseDouble(settings.get("every")));
            // Levels saved before first= existed fire their first shot after half an interval, as they did then
            design.setFirstShotDelay(settings.containsKey("first")
                    ? Double.parseDouble(settings.get("first")) : design.getFireInterval() / 2);
            if (settings.containsKey("spin")) design.setSpin(Double.parseDouble(settings.get("spin")));
            if (settings.containsKey("fire")) design.setFireOnlyWhenStopped(settings.get("fire").equalsIgnoreCase("stopped"));
            if (!path.isEmpty()) {
                for (String point : path.split("\\s+")) {
                    double[] xy = parsePoint(point);
                    design.startStroke();
                    design.addWaypoint(xy[0], xy[1]);
                    design.setStopTime(design.getWaypoints().size(), xy[2]);
                }
            }
            return design;
        }

        /** Reads "x,y" or "x,y@seconds" into {x, y, seconds}; seconds is 0 when there's no stop. */
        private static double[] parsePoint(String text) {
            String[] pointAndStop = text.split("@", 2);
            String[] parts = pointAndStop[0].split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("expected a point like 120,80 or 120,80@2 but found \"" + text + "\"");
            }
            double stop = pointAndStop.length == 2 ? Double.parseDouble(pointAndStop[1]) : 0;
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), stop};
        }

        /** Removes every placed enemy and wave, leaving an empty level with one wave. */
        public void clearLayout() {
            layout.clear();
            waves.clear();
            waves.add(new Wave());
            resetEnemies();
        }

        /** Starts the level over from time 0: the enemies spawn again on schedule and the shots are cleared. */
        public void resetEnemies() {
            playFromWave(0);
        }

        /**
         * Starts the run over from the given wave, which begins straight away; the waves before it are
         * skipped. Handy for testing one wave without playing the whole level.
         */
        public void playFromWave(int wave) {
            firstWave = Math.max(0, Math.min(wave, waves.size() - 1));
            waveStartTimes.clear();
            waveClearTimes.clear();
            levelTime = 0;
            enemies.clear();
            spawned.clear();
            shots.clear();
            enemyBullets.clear();
            hits = 0;
            invincibleTimeLeft = 0;
        }

        public double getPlayerX() {
            return playerX;
        }

        public double getPlayerY() {
            return playerY;
        }

        public boolean isFocused() {
            return focused;
        }

        /** Each shot is its center as {x, y}. */
        public List<double[]> getShots() {
            return shots;
        }

        public List<Enemy> getEnemies() {
            return enemies;
        }

        public List<EnemyDesign> getLayout() {
            return layout;
        }

        public double getLevelTime() {
            return levelTime;
        }

        /** Each enemy bullet is {x, y, x speed, y speed}. */
        public List<double[]> getEnemyBullets() {
            return enemyBullets;
        }

        public int getHits() {
            return hits;
        }

        /** True for a moment after the ship is hit, while it can't be hit again. */
        public boolean isInvincible() {
            return invincibleTimeLeft > 0;
        }

        /**
         * When a wave starts: after the previous wave is cleared (or after it starts), plus a wait.
         * The first wave waits from the start of the level.
         */
        public static final class Wave {
            private boolean afterCleared = true;
            private double wait = 2;

            public boolean isAfterCleared() {
                return afterCleared;
            }

            public void setAfterCleared(boolean afterCleared) {
                this.afterCleared = afterCleared;
            }

            public double getWait() {
                return wait;
            }

            /** Sets the wait in seconds, kept between 0 and MAX_WAVE_WAIT. */
            public void setWait(double seconds) {
                wait = Math.max(0, Math.min(MAX_WAVE_WAIT, seconds));
            }
        }

        /**
         * How one group of enemies is designed: the alien type, where they start, their flight path,
         * which wave it's in, when the first one spawns (seconds after the wave starts) and how many
         * follow it. The path is a list of waypoints flown in
         * order at the design's speed. Without a loop an enemy stops at the last waypoint (or leaves, if
         * that's off-screen); with a loop it flies back to the start and goes around again. Each enemy
         * in the group fires the design's bullet pattern every fire interval while it's on screen.
         */
        public static final class EnemyDesign {
            private final Alien.Type type;
            private final double startX;
            private final double startY;
            private final List<double[]> waypoints = new ArrayList<>();
            /** How many waypoints each stroke added (a click is a stroke of one), so undo can take back a whole drag. */
            private final List<Integer> strokes = new ArrayList<>();
            private double speed = ENEMY_SPEED;
            private boolean loop = false;
            private int count = 1;
            /** Which wave the group belongs to (0 is the first). */
            private int wave = 0;
            private double spawnTime = 0;

            // Bullet pattern
            private Pattern pattern;
            private int bulletCount;
            private double spread;          // degrees, for FAN and RANDOM
            private double bulletSpeed;
            private double fireInterval;    // seconds between shots
            private double firstShotDelay;  // seconds from coming on screen to the first shot
            private double spin = 15;       // degrees the SPIRAL turns per shot
            private boolean fireOnlyWhenStopped = false;
            /** Seconds to hold still at the start before moving (waypoints keep their own in waypoint[2]). */
            private double startStopTime = 0;

            public EnemyDesign(Alien.Type type, double startX, double startY) {
                this.type = type;
                this.startX = startX;
                this.startY = startY;
                // Each alien type comes with its own shot, which can then be changed
                switch (type) {
                    case SQUID -> setPatternValues(Pattern.AIMED, 3, 0, 220, 1.2);
                    case CRAB -> setPatternValues(Pattern.FAN, 5, 60, 160, 1.5);
                    case OCTOPUS -> setPatternValues(Pattern.RING, 12, 0, 120, 2.0);
                }
            }

            private void setPatternValues(Pattern pattern, int count, double spread, double speed, double interval) {
                this.pattern = pattern;
                this.bulletCount = count;
                this.spread = spread;
                this.bulletSpeed = speed;
                this.fireInterval = interval;
                this.firstShotDelay = interval / 2;
            }

            /**
             * When the given enemy of the group spawns (0 is the first). The rest follow FOLLOW_SPACING
             * pixels apart, so the gap stays the same at any speed.
             */
            public double spawnTimeOf(int index) {
                return spawnTime + index * FOLLOW_SPACING / speed;
            }

            /** Begins a new stroke: a click, or a drag that adds waypoints as the mouse moves. */
            public void startStroke() {
                strokes.add(0);
            }

            /**
             * Adds a waypoint to the current stroke. Like a start, one near an edge moves just off it, so
             * enemies can fly out.
             */
            public void addWaypoint(double x, double y) {
                if (strokes.isEmpty()) {
                    startStroke();
                }
                waypoints.add(new double[]{snapX(x), snapY(y), 0});
                strokes.set(strokes.size() - 1, strokes.get(strokes.size() - 1) + 1);
            }

            /** Takes back the last stroke: the last clicked point, or every point of the last drag. */
            public void removeLastStroke() {
                if (strokes.isEmpty()) {
                    return;
                }
                int points = strokes.remove(strokes.size() - 1);
                for (int i = 0; i < points && !waypoints.isEmpty(); i++) {
                    waypoints.remove(waypoints.size() - 1);
                }
            }

            /** The last waypoint, or the start when there are none. */
            public double[] getPathEnd() {
                return waypoints.isEmpty() ? new double[]{startX, startY} : waypoints.get(waypoints.size() - 1);
            }

            /**
             * How long the enemy holds still at a point of its path. Point 0 is the start, point 1 the
             * first waypoint, and so on.
             */
            public double getStopTime(int point) {
                if (point == 0) {
                    return startStopTime;
                }
                return point <= waypoints.size() ? waypoints.get(point - 1)[2] : 0;
            }

            /** Makes a point a stop (0 seconds = no stop), kept between 0 and MAX_STOP_TIME. */
            public void setStopTime(int point, double seconds) {
                double time = Math.max(0, Math.min(MAX_STOP_TIME, seconds));
                if (point == 0) {
                    startStopTime = time;
                } else if (point <= waypoints.size()) {
                    waypoints.get(point - 1)[2] = time;
                }
            }

            /** The point (1 = first waypoint) within the given distance of x, y, or -1 if there's none. */
            public int findWaypointNear(double x, double y, double radius) {
                int nearest = -1;
                double best = radius;
                for (int i = 0; i < waypoints.size(); i++) {
                    double distance = Math.hypot(waypoints.get(i)[0] - x, waypoints.get(i)[1] - y);
                    if (distance <= best) {
                        best = distance;
                        nearest = i + 1;
                    }
                }
                return nearest;
            }

            public boolean isFireOnlyWhenStopped() {
                return fireOnlyWhenStopped;
            }

            public void setFireOnlyWhenStopped(boolean onlyWhenStopped) {
                fireOnlyWhenStopped = onlyWhenStopped;
            }

            /** Removes every waypoint, so the enemy stays where it spawns. */
            public void clearPath() {
                waypoints.clear();
                strokes.clear();
            }

            /** Sets the speed, kept between MIN_ENEMY_SPEED and MAX_ENEMY_SPEED. */
            public void setSpeed(double speed) {
                this.speed = Math.max(MIN_ENEMY_SPEED, Math.min(MAX_ENEMY_SPEED, speed));
            }

            public void changeSpeed(double amount) {
                setSpeed(speed + amount);
            }

            /** Sets how many enemies fly this path, kept between 1 and MAX_COUNT. */
            public void setCount(int count) {
                this.count = Math.max(1, Math.min(MAX_COUNT, count));
            }

            public void changeCount(int amount) {
                setCount(count + amount);
            }

            /** Sets the spawn time in seconds, kept between 0 and MAX_SPAWN_TIME. */
            public void setSpawnTime(double seconds) {
                spawnTime = Math.max(0, Math.min(MAX_SPAWN_TIME, seconds));
            }

            public void changeSpawnTime(double seconds) {
                setSpawnTime(spawnTime + seconds);
            }

            public void setLoop(boolean loop) {
                this.loop = loop;
            }

            public int getWave() {
                return wave;
            }

            public void setWave(int wave) {
                this.wave = wave;
            }

            public void setPattern(Pattern pattern) {
                this.pattern = pattern;
            }

            /** Switches to the next pattern in the list, wrapping around. */
            public void nextPattern() {
                Pattern[] all = Pattern.values();
                pattern = all[(pattern.ordinal() + 1) % all.length];
            }

            /** Sets how many bullets one shot fires, kept between 1 and MAX_BULLETS_PER_SHOT. */
            public void setBulletCount(int count) {
                bulletCount = Math.max(1, Math.min(MAX_BULLETS_PER_SHOT, count));
            }

            /** Sets the spread in degrees, kept between 0 and 360. */
            public void setSpread(double degrees) {
                spread = Math.max(0, Math.min(360, degrees));
            }

            public void setBulletSpeed(double speed) {
                bulletSpeed = Math.max(MIN_BULLET_SPEED, Math.min(MAX_BULLET_SPEED, speed));
            }

            public void setFireInterval(double seconds) {
                fireInterval = Math.max(MIN_FIRE_INTERVAL, Math.min(MAX_FIRE_INTERVAL, seconds));
            }

            /** Sets how far a spiral turns per shot, in degrees, kept between -90 and 90. */
            public void setSpin(double degrees) {
                spin = Math.max(-90, Math.min(90, degrees));
            }

            public Pattern getPattern() {
                return pattern;
            }

            public int getBulletCount() {
                return bulletCount;
            }

            public double getSpread() {
                return spread;
            }

            public double getBulletSpeed() {
                return bulletSpeed;
            }

            /** Sets how long after coming on screen it fires the first time (0 = straight away), kept between 0 and MAX_FIRST_SHOT_TIME. */
            public void setFirstShotDelay(double seconds) {
                firstShotDelay = Math.max(0, Math.min(MAX_FIRST_SHOT_TIME, seconds));
            }

            public double getFirstShotDelay() {
                return firstShotDelay;
            }

            public double getFireInterval() {
                return fireInterval;
            }

            public double getSpin() {
                return spin;
            }

            public void toggleLoop() {
                loop = !loop;
            }

            /** True when the enemy starts off-screen and flies in, false when it appears on screen. */
            public boolean entersFromEdge() {
                return startX < 0 || startX > PLAYFIELD_WIDTH || startY < 0 || startY > PLAYFIELD_HEIGHT;
            }

            /** Where the start is shown in the editor: the start itself, or the nearest point on the screen edge. */
            public double getMarkerX() {
                return Math.max(0, Math.min(PLAYFIELD_WIDTH, startX));
            }

            public double getMarkerY() {
                return Math.max(0, Math.min(PLAYFIELD_HEIGHT, startY));
            }

            public Alien.Type getType() {
                return type;
            }

            public double getStartX() {
                return startX;
            }

            public double getStartY() {
                return startY;
            }

            /** Each waypoint is {x, y}. */
            public List<double[]> getWaypoints() {
                return waypoints;
            }

            public double getSpeed() {
                return speed;
            }

            public boolean isLoop() {
                return loop;
            }

            public int getCount() {
                return count;
            }

            public double getSpawnTime() {
                return spawnTime;
            }
        }

        /** One enemy in a run, flying along its design's path. Path edits show up right away. */
        public static final class Enemy {
            private final EnemyDesign design;
            private double x;
            private double y;
            /** Index of the waypoint being flown to. Equal to the number of waypoints when looping back to the start. */
            private int target = 0;
            /** Seconds left of the fade-in for an enemy that appears on screen. */
            private double appearTimeLeft;
            private boolean pathFinished = false;
            /** Seconds until the next shot. Only counts down on screen, so the first comes the design's first-shot delay after it shows up. */
            private double fireTimeLeft;
            /** How far a spiral has turned so far, in degrees. */
            private double spinAngle = 0;
            /** Seconds left holding still at a stop. */
            private double stopTimeLeft;

            public Enemy(EnemyDesign design) {
                this.design = design;
                this.x = design.getStartX();
                this.y = design.getStartY();
                this.appearTimeLeft = design.entersFromEdge() ? 0 : APPEAR_TIME;
                this.fireTimeLeft = design.getFirstShotDelay();
                this.stopTimeLeft = design.getStopTime(0);
            }

            /** True while it holds still: at a stop, or at the end of its path. */
            public boolean isStopped() {
                return stopTimeLeft > 0 || pathFinished;
            }

            /** Counts down to the next shot. True when it's time to fire (only on screen, never while fading in). */
            boolean readyToFire(double deltaSeconds) {
                boolean onScreen = x >= 0 && x <= PLAYFIELD_WIDTH && y >= 0 && y <= PLAYFIELD_HEIGHT;
                boolean allowedToFire = !design.isFireOnlyWhenStopped() || isStopped();
                if (design.getPattern() == Pattern.NONE || isAppearing() || !onScreen || !allowedToFire) {
                    return false;
                }
                fireTimeLeft -= deltaSeconds;
                if (fireTimeLeft > 0) {
                    return false;
                }
                fireTimeLeft = design.getFireInterval();
                return true;
            }

            double getSpinAngle() {
                return spinAngle;
            }

            void addSpin(double degrees) {
                spinAngle = (spinAngle + degrees) % 360;
            }

            /** Moves along the path by speed * deltaSeconds, passing through as many waypoints as that covers. */
            void fly(double deltaSeconds) {
                if (appearTimeLeft > 0) {
                    appearTimeLeft = Math.max(0, appearTimeLeft - deltaSeconds);
                    return; // holds still while it fades in
                }
                if (stopTimeLeft > 0) {
                    stopTimeLeft = Math.max(0, stopTimeLeft - deltaSeconds);
                    return; // holding still at a stop (it still shoots)
                }
                List<double[]> path = design.getWaypoints();
                double distanceLeft = design.getSpeed() * deltaSeconds;
                pathFinished = false;
                // Bounded so a path with every point in the same spot can't spin forever
                for (int step = 0; step <= path.size() + 1 && distanceLeft > 0; step++) {
                    double[] point;
                    if (target < path.size()) {
                        point = path.get(target);
                    } else if (design.isLoop() && !path.isEmpty()) {
                        point = new double[]{design.getStartX(), design.getStartY()};
                    } else {
                        pathFinished = true; // end of the path: stay put
                        return;
                    }
                    double dx = point[0] - x;
                    double dy = point[1] - y;
                    double distance = Math.hypot(dx, dy);
                    if (distance <= distanceLeft) {
                        x = point[0];
                        y = point[1];
                        distanceLeft -= distance;
                        // Point numbers: 0 is the start, 1 the first waypoint. The next target is the waypoint
                        // after the one reached, which has the same number as the reached point.
                        int reached = target < path.size() ? target + 1 : 0;
                        target = reached;
                        stopTimeLeft = design.getStopTime(reached);
                        if (stopTimeLeft > 0) {
                            return;
                        }
                    } else {
                        x += dx / distance * distanceLeft;
                        y += dy / distance * distanceLeft;
                        distanceLeft = 0;
                    }
                }
            }

            /** True once the enemy has flown its whole path and ended up off-screen, so it can be removed. */
            boolean hasLeftScreen() {
                return pathFinished && (x < -Alien.WIDTH / 2 || x > PLAYFIELD_WIDTH + Alien.WIDTH / 2
                        || y < -Alien.HEIGHT / 2 || y > PLAYFIELD_HEIGHT + Alien.HEIGHT / 2);
            }

            public boolean isAppearing() {
                return appearTimeLeft > 0;
            }

            /** 0 when an appearing enemy has just spawned, 1 once it has fully faded in. */
            public double getAppearProgress() {
                return 1 - appearTimeLeft / APPEAR_TIME;
            }

            public EnemyDesign getDesign() {
                return design;
            }

            public Alien.Type getType() {
                return design.getType();
            }

            public double getX() {
                return x;
            }

            public double getY() {
                return y;
            }
        }
    }
}
