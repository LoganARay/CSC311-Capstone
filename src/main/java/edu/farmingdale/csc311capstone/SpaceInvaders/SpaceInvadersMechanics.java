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
         * WAVE number after=cleared/started wait=seconds bomb=yes (bomb=yes: clearing the wave drops a bomb from the top)
         * TYPE start=x,y speed= count= hp= spawn= loop=yes/no shot=PATTERN bullets= spread= bulletSpeed= every= first= spin= fire=always/stopped path=x,y x,y ...
         * repeats= sends the whole group again that many more times, gap= seconds apart (from one
         * batch's start to the next), for a steady stream down the same path (left out: 0, no repeats).
         * hp= is how many shots each enemy of the group takes to destroy (left out: 1). Over 1 it shows a health bar.
         * formation= is how the group's count= enemies fly: LINE (one after another along the path, the
         * default), or any other Formation shape (all at once, keeping that shape as they follow the
         * path). spacing= is the gap between them in px (left out: 40). face=yes turns the shape to
         * face the way it flies (an arrow points where it's going); left out, it never turns.
         * A point written x,y@seconds is a stop: the enemy waits there that long, then flies on (start=x,y@2 too).
         * fire=stopped means it only shoots while it's holding still. first= is how many seconds after it
         * comes on screen it fires its first shot (left out: half of every=).
         * Bullet movement: accel= (px/s per second, below 0 slows down) and curve= (degrees per second) start
         * with every bullet; steps= lists timed changes, each at/speed/accel/curve/turn/aim (aim or no),
         * separated by ;. For example steps=0.6/0/0/0/0/no;1.2/180/0/0/0/aim stops a bullet after 0.6 s,
         * then at 1.2 s aims it at the player and sends it off at 180.
         * follows=N makes a group wait for group N of the same wave (counting its enemy lines from 1) to
         * be cleared; its spawn= then counts from that moment instead of from the wave's start. With
         * when=spawned it counts from when group N's first enemy spawns instead (when=cleared is the default).
         * Bullet look and extras: size= (radius in px), color=, pic= (a bullet picture from BULLET_PICTURES,
         * like pic=rice_red; size= still sets the hitbox and the picture is scaled to fit it, picsize= (percent, left out:
         * 100) makes just the picture bigger or smaller, color= only matters for lasers), bounces= (times a bullet bounces off the
         * sides and top). A step can have a 7th part, split: the bullet bursts into that many
         * bullets in a ring (0 = no split). shot=LASER fires beams: a warning line for LASER_WARN_TIME,
         * then a beam for beam= seconds, size= sets its width. shot=WALL fires a straight line of bullets
         * side by side, width= px wide, all flying the same way.
         * Where it shoots: aim=PLAYER (at the player every shot), aim=ONCE (at the player on the phase's first
         * shot, then keeps that direction) or aim=FIXED (always direction= degrees: 0 right, 90 down,
         * 180 left, 270 up). sweep= turns that direction by degrees per second while the phase runs, and
         * swing= (degrees, 0 = keeps turning) makes it sweep back and forth across that many degrees.
         * A laser's beam follows the sweep, so it drags across the screen. Left out, aim is FIXED down
         * for RING and SPIRAL and PLAYER for the rest, like before these settings existed.
         * Shot phases: the settings on the enemy line are phase 1; lasts= is how long it runs (0 = for
         * good). Each PHASE line under an enemy adds the next phase, with the same shot settings. After
         * the last phase the enemy starts again at phase 1.
         * Layers: together=yes on a PHASE line makes it fire at the same time as the phase before it,
         * so one attack can mix several patterns (a ring plus aimed shots plus a sweeping wall...). The
         * attack's time and boss settings come from its first pattern. delay= is how many seconds into
         * the attack a pattern starts firing (left out: 0).
         * Bosses: boss=yes on an enemy line makes it a boss (count 1, drawn twice the size). It flies its
         * path, and at the end of it starts its attacks: phase 1 (the enemy line) is attack 1, each PHASE
         * line the next. A boss attack has its own health, cardhp= (shots to beat it), and lasts= is its
         * time limit (0 = none). name= makes it a spell card, shown on screen (_ stands for a space); left
         * out it's a plain attack. move=STAY holds still, move=WANDER hops to a spot near its own every
         * hop= seconds. move=PATH flies its own path for that attack: movepath= lists the points as
         * x,y@stop;x,y@stop (stop = seconds it holds still there), movespeed= is how fast it flies
         * between them, and moveloop=yes goes back to the first point after the last, round and round,
         * Each pattern of a boss attack can fire at only some spots: atpoint=N fires only while it's
         * stopped at point N of the attack's path (give that point a stop), atpoint=-1 only while it's
         * moving, and a list like atpoint=1,3 at each of them. volleys= is how many shots it fires there each time (0 = keeps firing while it's
         * there); with no atpoint=, volleys= is how many shots it fires in the whole attack.
         * moveloop=bounce goes back and forth (1, 2, 3, 2, 1, 2...), and moveloop=no stays at the last point. When an attack is beaten or its time runs out, the bullets on screen vanish and,
         * after a short break, the next attack starts. Beating the last one destroys the boss; if the
         * last one's time runs out it flies away instead.
         * spawn is seconds after the wave starts. A start or path point past the edge of the 560x560
         * playfield is off-screen.
         */
        public static final String[] STAGES = {
                // Stage 1 (an example to start from)
                """
WAVE 1 after=cleared wait=2.0
OCTOPUS start=588,84 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.5 loop=no first=1.0 fire=always follows=3 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=588,87 588,87 588,87 588,87 588,87 588,87 534,87 522,85 510,84 498,83 486,83 474,83 462,83 450,84 438,86 426,86 414,86 402,88 389,89 377,92 364,96 352,100 340,105 329,110 318,115 307,122 297,130 287,138 278,146 269,154 261,164 253,174 247,186 241,197 238,209 234,221 232,234 230,246 230,258 235,269 241,280 248,291 256,301 265,310 277,316 289,321 301,321 313,318 322,310 331,301 331,289 329,276 326,263 321,252 310,245 298,241 286,238 273,233 261,231 249,230 237,232 225,234 213,237 200,241 189,246 178,252 167,258 155,264 144,272 134,279 124,288 114,297 104,305 92,314 79,320 67,327 54,333 43,338 32,342 -28,348 -28,349 -28,350 -28,351 -28,351 -28,353 -28,354 -28,354 -28,354 -28,354 -28,354 -28,355 -28,355 -28,355 -28,355 -28,355 -28,355 -28,355
OCTOPUS start=588,84 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=2.5 loop=no first=1.0 fire=always follows=3 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=588,87 588,87 588,87 588,87 588,87 588,87 534,87 522,85 510,84 498,83 486,83 474,83 462,83 450,84 438,86 426,86 414,86 402,88 389,89 377,92 364,96 352,100 340,105 329,110 318,115 307,122 297,130 287,138 278,146 269,154 261,164 253,174 247,186 241,197 238,209 234,221 232,234 230,246 230,258 235,269 241,280 248,291 256,301 265,310 277,316 289,321 301,321 313,318 322,310 331,301 331,289 329,276 326,263 321,252 310,245 298,241 286,238 273,233 261,231 249,230 237,232 225,234 213,237 200,241 189,246 178,252 167,258 155,264 144,272 134,279 124,288 114,297 104,305 92,314 79,320 67,327 54,333 43,338 32,342 -28,348 -28,349 -28,350 -28,351 -28,351 -28,353 -28,354 -28,354 -28,354 -28,354 -28,354 -28,355 -28,355 -28,355 -28,355 -28,355 -28,355 -28,355
OCTOPUS start=-28,104 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=35,106 47,110 57,119 70,125 81,131 92,138 102,145 112,154 121,165 128,174 136,184 145,194 152,204 158,216 164,228 170,239 177,250 184,262 190,273 198,282 206,293 215,303 225,312 234,319 246,325 258,328 270,330 282,331 294,331 306,330 320,326 332,322 344,315 356,306 364,297 371,287 372,275 369,262 362,251 354,242 345,234 333,228 321,223 309,222 297,223 286,230 282,242 282,254 282,268 286,280 290,291 298,302 306,312 317,320 327,326 339,333 352,338 364,342 378,346 390,348 402,351 416,353 429,356 441,356 455,358 467,363 479,369 491,376 500,386 507,395 516,406 525,415 588,423 588,425 588,426 588,427 588,429 588,430 588,432 588,433 588,434 588,436 588,436 588,437 588,438 588,438 588,438 588,438 588,439 588,439 588,439 588,439 588,439
OCTOPUS start=-28,104 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=2.0 loop=no first=1.0 fire=always follows=5 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=35,106 47,110 57,119 70,125 81,131 92,138 102,145 112,154 121,165 128,174 136,184 145,194 152,204 158,216 164,228 170,239 177,250 184,262 190,273 198,282 206,293 215,303 225,312 234,319 246,325 258,328 270,330 282,331 294,331 306,330 320,326 332,322 344,315 356,306 364,297 371,287 372,275 369,262 362,251 354,242 345,234 333,228 321,223 309,222 297,223 286,230 282,242 282,254 282,268 286,280 290,291 298,302 306,312 317,320 327,326 339,333 352,338 364,342 378,346 390,348 402,351 416,353 429,356 441,356 455,358 467,363 479,369 491,376 500,386 507,395 516,406 525,415 588,423 588,425 588,426 588,427 588,429 588,430 588,432 588,433 588,434 588,436 588,436 588,437 588,438 588,438 588,438 588,438 588,439 588,439 588,439 588,439 588,439
OCTOPUS start=424,-20 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.5 loop=no first=1.0 fire=always follows=7 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=425,50 425,50 425,62 426,74 426,86 427,98 429,110 430,122 432,134 434,146 438,158 441,170 445,182 450,194 454,206 458,218 463,229 470,239 478,249 487,258 498,266 508,275 518,284 527,292 588,300 588,301 588,302 588,302 588,302 588,303 588,305 588,306 588,306 588,307 588,308 588,308 588,309 588,309 588,310 588,310 588,311 588,311 588,311 588,312 588,313 588,313 588,314 588,314 588,314 588,314 588,315 588,316 588,317
OCTOPUS start=424,-20 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=2.5 loop=no first=1.0 fire=always follows=7 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=425,50 425,50 425,62 426,74 426,86 427,98 429,110 430,122 432,134 434,146 438,158 441,170 445,182 450,194 454,206 458,218 463,229 470,239 478,249 487,258 498,266 508,275 518,284 527,292 588,300 588,301 588,302 588,302 588,302 588,303 588,305 588,306 588,306 588,307 588,308 588,308 588,309 588,309 588,310 588,310 588,311 588,311 588,311 588,312 588,313 588,313 588,314 588,314 588,314 588,314 588,315 588,316 588,317
OCTOPUS start=86,-20 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.5 loop=no first=1.0 fire=always follows=1 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=90,46 90,58 90,70 94,83 98,95 100,107 102,119 105,131 106,143 109,155 111,167 112,180 112,192 112,204 112,216 110,228 106,241 102,253 97,264 89,274 78,282 69,290 58,296 47,302 37,308 -28,313 -28,314 -28,314 -28,317 -28,318 -28,319 -28,319 -28,320 -28,321 -28,322 -28,322 -28,323 -28,324 -28,325 -28,325 -28,325 -28,326 -28,326 -28,326 -28,326 -28,326
OCTOPUS start=86,-20 speed=185 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=2.5 loop=no first=1.0 fire=always follows=1 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=90,46 90,58 90,70 94,83 98,95 100,107 102,119 105,131 106,143 109,155 111,167 112,180 112,192 112,204 112,216 110,228 106,241 102,253 97,264 89,274 78,282 69,290 58,296 47,302 37,308 -28,313 -28,314 -28,314 -28,317 -28,318 -28,319 -28,319 -28,320 -28,321 -28,322 -28,322 -28,323 -28,324 -28,325 -28,325 -28,325 -28,326 -28,326 -28,326 -28,326 -28,326
OCTOPUS start=206,-20 speed=175 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always follows=1 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=206,-20 206,26 208,38 208,50 208,62 208,74 206,86 206,98 206,110 205,122 202,135 200,147 198,160 194,173 191,185 189,197 186,209 182,221 182,233 182,246 184,258 187,270 188,283 189,295 189,308 189,320 189,333 189,345 184,356 179,369 174,380 167,393 158,405 150,416 142,426 132,434 120,438 108,444 98,452 91,462 83,471 76,482 70,493 66,505 66,517 63,530 60,580 60,580 59,580 59,580 58,580 58,580 58,580 58,580 57,580 57,580 57,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580 56,580
OCTOPUS start=354,-20 speed=175 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always follows=3 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=-15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=354,-20 354,26 352,38 352,50 352,62 352,74 354,86 354,98 354,110 355,122 358,135 360,147 362,160 366,173 369,185 371,197 374,209 378,221 378,233 378,246 376,258 373,270 372,283 371,295 371,308 371,320 371,333 371,345 376,356 381,369 386,380 393,393 402,405 410,416 418,426 428,434 440,438 452,444 462,452 469,462 477,471 484,482 490,493 494,505 494,517 497,530 500,580 500,580 501,580 501,580 502,580 502,580 502,580 502,580 503,580 503,580 503,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580
OCTOPUS start=354,-20 speed=175 count=6 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=2.0 loop=no first=1.0 fire=always follows=7 when=cleared lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=-15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=354,-20 354,26 352,38 352,50 352,62 352,74 354,86 354,98 354,110 355,122 358,135 360,147 362,160 366,173 369,185 371,197 374,209 378,221 378,233 378,246 376,258 373,270 372,283 371,295 371,308 371,320 371,333 371,345 376,356 381,369 386,380 393,393 402,405 410,416 418,426 428,434 440,438 452,444 462,452 469,462 477,471 484,482 490,493 494,505 494,517 497,530 500,580 500,580 501,580 501,580 502,580 502,580 502,580 502,580 503,580 503,580 503,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580 504,580
OCTOPUS start=279,-20 speed=175 count=5 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=14.3 loop=no first=1.0 fire=always lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=281,-20 281,26 281,38 281,50 281,62 280,75 279,87 278,100 274,112 270,126 268,138 265,150 262,162 258,174 255,186 253,198 250,211 249,224 246,237 244,249 243,262 242,274 242,286 240,298 237,310 230,321 223,332 214,340 203,346 192,351 181,356 169,362 158,369 147,376 137,382 126,388 114,394 103,399 92,404 78,409 67,414 54,418 42,422 29,426 -28,431 -28,431 -28,432 -28,433 -28,434 -28,434 -28,434 -28,435 -28,435 -28,436 -28,436 -28,437 -28,438 -28,438 -28,438 -28,439 -28,439 -28,440 -28,440 -28,440 -28,440 -28,441
WAVE 2 after=cleared wait=2.0
OCTOPUS start=406,-20 speed=140 count=10 repeats=2 gap=6.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=404,-20 404,-20 404,-20 404,-20 404,26 396,35 386,43 376,50 366,57 358,66 351,77 345,87 342,99 342,111 343,124 348,136 356,146 368,152 379,158 391,163 403,167 415,171 427,176 438,182 448,193 457,202 463,214 470,226 472,238 476,250 477,262 475,275 468,285 458,293 450,302 441,311 430,318 419,322 406,326 395,332 384,337 374,343 363,350 354,357 344,365 336,374 327,385 321,395 315,407 316,420 322,430 330,440 339,448 351,452 363,456 376,457 388,460 400,462 412,466 424,470 435,475 447,482 457,490 464,500 471,510 478,521 479,534 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 481,580 480,580
OCTOPUS start=154,-20 speed=140 count=10 repeats=2 gap=6.0 hp=1 formation=LINE spacing=40 face=no spawn=3.0 loop=no first=1.0 fire=always follows=1 when=spawned lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=-15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=156,-20 156,-20 156,-20 156,-20 156,26 164,35 174,43 184,50 194,57 202,66 209,77 215,87 218,99 218,111 217,124 212,136 204,146 192,152 181,158 169,163 157,167 145,171 133,176 122,182 112,193 103,202 97,214 90,226 88,238 84,250 83,262 85,275 92,285 102,293 110,302 119,311 130,318 141,322 154,326 165,332 176,337 186,343 197,350 206,357 216,365 224,374 233,385 239,395 245,407 244,420 238,430 230,440 221,448 209,452 197,456 184,457 172,460 160,462 148,466 136,470 125,475 113,482 103,490 96,500 89,510 82,521 81,534 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 79,580 80,580
OCTOPUS start=449,-21 speed=140 count=10 repeats=2 gap=6.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=447,-20 447,-20 447,-20 447,-20 447,25 439,34 429,42 419,49 409,56 401,65 394,76 388,86 385,98 385,110 386,123 391,135 399,145 411,151 422,157 434,162 446,166 458,170 470,175 481,181 491,192 500,201 506,213 513,225 515,237 519,249 520,261 518,274 511,284 501,292 493,301 484,310 473,317 462,321 449,325 438,331 427,336 417,342 406,349 397,356 387,364 379,373 370,384 364,394 358,406 359,419 365,429 373,439 382,447 394,451 406,455 419,456 431,459 443,461 455,465 467,469 478,474 490,481 500,489 507,499 514,509 521,520 522,533 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 524,580 523,580
OCTOPUS start=111,-21 speed=140 count=10 repeats=2 gap=6.0 hp=1 formation=LINE spacing=40 face=no spawn=3.0 loop=no first=1.0 fire=always follows=3 when=spawned lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=-15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=113,-20 113,-20 113,-20 113,-20 113,25 121,34 131,42 141,49 151,56 159,65 166,76 172,86 175,98 175,110 174,123 169,135 161,145 149,151 138,157 126,162 114,166 102,170 90,175 79,181 69,192 60,201 54,213 47,225 45,237 41,249 40,261 42,274 49,284 59,292 67,301 76,310 87,317 98,321 111,325 122,331 133,336 143,342 154,349 163,356 173,364 181,373 190,384 196,394 202,406 201,419 195,429 187,439 178,447 166,451 154,455 141,456 129,459 117,461 105,465 93,469 82,474 70,481 60,489 53,499 46,509 39,520 38,533 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 36,580 37,580
WAVE 3 after=cleared wait=2.0 bomb=yes
OCTOPUS start=273,92 speed=120 count=1 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always boss=yes lasts=60.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= cardhp=150 move=PATH hop=2.0 movespeed=140 moveloop=bounce movepath=272,91@2.0;56,141@2.0;272,78@2.0;485,125@2.0 name=Blessed_rain path=
  PHASE lasts=0.0 shot=RANDOM bullets=5 spread=150 bulletSpeed=120 every=0.15 spin=15 size=3 color=BLUE bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=rice_blue picsize=60 together=yes atpoint=1 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RANDOM bullets=5 spread=150 bulletSpeed=120 every=0.15 spin=15 size=3 color=BLUE bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=rice_blue picsize=60 together=yes atpoint=3 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RING bullets=30 spread=0 bulletSpeed=150 every=1.50 spin=15 size=8 color=WHITE bounces=1 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=orb_white together=yes atpoint=2 volleys=2 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RING bullets=30 spread=0 bulletSpeed=150 every=1.50 spin=15 size=8 color=WHITE bounces=1 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=orb_white together=yes atpoint=4 volleys=2 cardhp=150 move=STAY hop=2.0
WAVE 4 after=cleared wait=2.0
OCTOPUS start=64,-20 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=stopped lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=70,63@2.0 -28,118
OCTOPUS start=172,-20 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=4 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=170,54@2.0 -28,174
OCTOPUS start=294,-20 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=9 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=290,62@2.0 315,-20
OCTOPUS start=432,-20 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=1 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=432,60@2.0 588,181
OCTOPUS start=528,-20 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=always follows=2 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=525,58@2.0 588,128
OCTOPUS start=-28,93 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=7 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=71,149@2.0 -28,174
OCTOPUS start=588,102 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=8 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=502,154@2.0 588,166
OCTOPUS start=-28,235 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=5 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=100,273@2.0 -28,316
OCTOPUS start=588,261 speed=120 count=1 repeats=3 gap=9.0 hp=1 formation=LINE spacing=40 face=no spawn=1.0 loop=no first=1.0 fire=stopped follows=6 when=spawned lasts=0.0 shot=RING bullets=22 spread=0 bulletSpeed=200 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_red path=492,303@2.0 588,336
WAVE 5 after=cleared wait=2.5
CRAB start=95,114 speed=120 count=1 repeats=2 gap=3.0 hp=15 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=0.8 fire=always next=gone lasts=0.0 shot=AIMED bullets=3 spread=0 bulletSpeed=160 every=2.00 spin=15 size=8 color=RED bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=arrowhead_red picsize=60 path=
  PHASE lasts=0.0 shot=RING bullets=16 spread=0 bulletSpeed=100 every=1.50 spin=11 size=4 color=BLUE bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_cyan picsize=70 together=yes
CRAB start=465,114 speed=120 count=1 repeats=2 gap=8.5 hp=15 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=0.8 fire=always follows=1 when=spawned next=gone lasts=0.0 shot=AIMED bullets=3 spread=0 bulletSpeed=160 every=2.00 spin=-15 size=8 color=RED bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=arrowhead_red picsize=60 path=
  PHASE lasts=0.0 shot=RING bullets=16 spread=0 bulletSpeed=100 every=1.50 spin=-11 size=4 color=BLUE bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=tiny_orb_cyan picsize=70 together=yes
OCTOPUS start=465,-20 speed=200 count=6 repeats=2 gap=6.0 hp=1 formation=LINE spacing=80 face=no spawn=2.0 loop=no first=1.0 fire=always follows=2 when=spawned until=1 lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=414,-20 413,-20 411,-20 407,-20 406,-20 405,-20 404,-20 402,-20 402,-20 401,-20 400,-20 399,-20 398,-20 398,-20 398,-20 397,-20 397,-20 396,-20 395,-20 395,-20 394,-20 394,-20 394,-20 393,-20 392,-20 392,-20 391,-20 390,-20 390,-20 389,-20 389,-20 389,-20 385,26 374,33 363,38 351,42 339,46 327,50 315,54 305,60 294,66 283,72 274,79 264,86 254,94 244,103 236,113 227,122 218,132 211,142 203,151 196,161 190,171 182,182 178,194 173,206 168,217 162,228 157,239 152,252 147,264 142,276 139,289 137,301 134,313 133,325 130,338 128,350 126,363 125,377 125,389 125,401 126,413 127,425 129,438 131,450 134,464 139,476 146,487 155,498 166,506 176,513 187,518 199,521 212,524 225,524 238,524 250,524 262,522 274,519 286,515 297,507 307,501 317,493 326,484 333,474 338,462 342,450 346,438 348,426 349,414 347,402 347,390 346,378 341,366 335,355 328,346 319,337 310,330 298,324 286,318 274,314 264,307 252,302 241,298 229,294 217,291 205,288 193,287 181,286 169,286 156,286 144,286 132,286 119,284 107,284 95,284 82,284 70,286 58,288 46,291 34,296 -28,300 -28,301 -28,302 -28,302 -28,303 -28,304 -28,305 -28,306 -28,306 -28,306 -28,308 -28,308 -28,309 -28,310 -28,310 -28,311 -28,311 -28,312 -28,314 -28,314 -28,314 -28,314 -28,314 -28,314 -28,314 -28,315 -28,316 -28,317 -28,318 -28,318 -28,318 -28,319 -28,319
OCTOPUS start=95,-20 speed=200 count=6 repeats=2 gap=6.0 hp=1 formation=LINE spacing=150 face=yes spawn=3.0 loop=no first=1.0 fire=always follows=3 when=spawned until=1 lasts=0.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=-15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= path=146,-20 147,-20 149,-20 153,-20 154,-20 155,-20 156,-20 158,-20 158,-20 159,-20 160,-20 161,-20 162,-20 162,-20 162,-20 163,-20 163,-20 164,-20 165,-20 165,-20 166,-20 166,-20 166,-20 167,-20 168,-20 168,-20 169,-20 170,-20 170,-20 171,-20 171,-20 171,-20 175,26 186,33 197,38 209,42 221,46 233,50 245,54 255,60 266,66 277,72 286,79 296,86 306,94 316,103 324,113 333,122 342,132 349,142 357,151 364,161 370,171 378,182 382,194 387,206 392,217 398,228 403,239 408,252 413,264 418,276 421,289 423,301 426,313 427,325 430,338 432,350 434,363 435,377 435,389 435,401 434,413 433,425 431,438 429,450 426,464 421,476 414,487 405,498 394,506 384,513 373,518 361,521 348,524 335,524 322,524 310,524 298,522 286,519 274,515 263,507 253,501 243,493 234,484 227,474 222,462 218,450 214,438 212,426 211,414 213,402 213,390 214,378 219,366 225,355 232,346 241,337 250,330 262,324 274,318 286,314 296,307 308,302 319,298 331,294 343,291 355,288 367,287 379,286 391,286 404,286 416,286 428,286 441,284 453,284 465,284 478,284 490,286 502,288 514,291 526,296 588,300 588,301 588,302 588,302 588,303 588,304 588,305 588,306 588,306 588,306 588,308 588,308 588,309 588,310 588,310 588,311 588,311 588,312 588,314 588,314 588,314 588,314 588,314 588,314 588,314 588,315 588,316 588,317 588,318 588,318 588,318 588,319 588,319
WAVE 6 after=cleared wait=2.0
OCTOPUS start=588,-20 speed=400 count=1 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always boss=yes lasts=40.0 shot=RING bullets=20 spread=0 bulletSpeed=170 every=2.70 spin=15 size=16 color=WHITE bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=260 accel=200 curve=5 steps=0.70/10/20/5/40/no/0 pic=giant_orb_white picsize=90 cardhp=150 move=STAY hop=2.0 name=Undulating_Waves path=521,35 512,45 500,50 488,54 475,58 462,62 450,64 438,66 425,69 412,72 400,74 388,76 375,79 363,83 350,85 338,88 326,90 314,91 302,91 290,91
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=90 every=3.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=45 sweep=0 swing=0 beam=0.8 width=40 accel=0 curve=0 steps= pic=flame_red picsize=70 from=8,9 together=yes delay=1.0 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=70 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=315 sweep=5 swing=90 beam=0.8 width=60 accel=0 curve=0 steps= pic=flame_red picsize=70 from=8,551 together=yes delay=2.0 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=70 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=225 sweep=5 swing=90 beam=0.8 width=60 accel=0 curve=0 steps= pic=flame_red picsize=70 from=550,552 together=yes delay=4.0 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=90 every=3.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=135 sweep=0 swing=0 beam=0.8 width=60 accel=0 curve=0 steps= pic=flame_red picsize=70 from=552,5 together=yes delay=3.0 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=50 every=5.00 spin=15 size=4 color=PINK bounces=2 aim=FIXED direction=175 sweep=5 swing=90 beam=0.8 width=60 accel=0 curve=0 steps= pic=flame_blue picsize=70 from=550,286 together=yes cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=WALL bullets=12 spread=0 bulletSpeed=50 every=5.00 spin=15 size=4 color=PINK bounces=2 aim=FIXED direction=0 sweep=5 swing=90 beam=0.8 width=60 accel=0 curve=0 steps= pic=flame_blue picsize=70 from=15,284 together=yes cardhp=150 move=STAY hop=2.0
WAVE 7 after=cleared wait=2.0
OCTOPUS start=278,74 speed=120 count=1 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always boss=yes lasts=50.0 shot=RANDOM bullets=14 spread=220 bulletSpeed=200 every=3.00 spin=15 size=9 color=BLUE bounces=0 aim=FIXED direction=270 sweep=0 swing=0 beam=0.8 width=200 accel=-300 curve=0 steps=0.85/0/0/0/0/no/0;1.25/150/0/0/0/aim/0 pic=ringed_orb_blue picsize=70 cardhp=150 move=WANDER hop=2.0 name=Hail_Storm path=
  PHASE lasts=0.0 shot=WALL bullets=13 spread=0 bulletSpeed=80 every=3.00 spin=15 size=10 color=PURPLE bounces=1 aim=FIXED direction=90 sweep=50 swing=80 beam=0.8 width=100 accel=0 curve=0 steps= pic=tiny_orb_pink picsize=60 together=yes cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=AIMED bullets=1 spread=0 bulletSpeed=100 every=0.12 spin=15 size=5 color=WHITE bounces=0 aim=FIXED direction=90 sweep=90 swing=140 beam=0.8 width=200 accel=0 curve=0 steps= pic=star_green picsize=80 together=yes cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RING bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=5 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=trail_red picsize=70 from=10,546 together=yes cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RING bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=FIXED direction=90 sweep=5 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=trail_red picsize=70 from=554,6 together=yes cardhp=150 move=STAY hop=2.0
WAVE 8 after=cleared wait=2.0
OCTOPUS start=594,-31 speed=400 count=1 repeats=0 gap=3.0 hp=1 formation=LINE spacing=40 face=no spawn=0.0 loop=no first=1.0 fire=always boss=yes lasts=60.0 shot=NONE bullets=12 spread=0 bulletSpeed=120 every=2.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= cardhp=150 move=PATH hop=2.0 movespeed=400 moveloop=bounce movepath=276,80@2.0;60,184@3.0;275,91@2.5;490,185@3.0 path=588,-20 588,-20
  PHASE lasts=0.0 shot=RING bullets=25 spread=0 bulletSpeed=60 every=1.50 spin=15 size=8 color=WHITE bounces=1 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=orb_white together=yes atpoint=1 volleys=2 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=SPIRAL bullets=4 spread=0 bulletSpeed=120 every=0.10 spin=11 size=4 color=BLUE bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=arrowhead_cyan picsize=60 together=yes atpoint=3 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RANDOM bullets=3 spread=150 bulletSpeed=120 every=0.15 spin=15 size=3 color=BLUE bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=rice_cyan picsize=60 together=yes atpoint=2 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RANDOM bullets=3 spread=150 bulletSpeed=120 every=0.15 spin=15 size=3 color=BLUE bounces=0 aim=FIXED direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=rice_cyan picsize=60 together=yes atpoint=4 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RING bullets=40 spread=0 bulletSpeed=130 every=1.00 spin=15 size=4 color=PINK bounces=0 aim=PLAYER direction=90 sweep=0 swing=0 beam=0.8 width=200 accel=0 curve=0 steps= pic=crystal_red picsize=70 together=yes atpoint=3 volleys=2 cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RANDOM bullets=14 spread=220 bulletSpeed=260 every=5.00 spin=15 size=6 color=BLUE bounces=0 aim=FIXED direction=135 sweep=1 swing=90 beam=0.8 width=200 accel=-300 curve=0 steps=0.85/0/0/0/0/no/0;1.25/150/0/0/0/aim/0 pic=ringed_orb_blue picsize=90 from=535,27 together=yes cardhp=150 move=STAY hop=2.0
  PHASE lasts=0.0 shot=RANDOM bullets=14 spread=220 bulletSpeed=260 every=5.00 spin=15 size=6 color=BLUE bounces=0 aim=FIXED direction=270 sweep=0 swing=0 beam=0.8 width=200 accel=-300 curve=0 steps=0.85/0/0/0/0/no/0;1.25/150/0/0/0/aim/0 pic=ringed_orb_blue picsize=90 from=31,25 together=yes delay=3.5 cardhp=150 move=STAY hop=2.0                
                """,
                // Stage 2 (built like a song: intro, buildup, melody, break, climax, outro; no boss yet)
                """
                # Stage 2 - built like a song at 120 BPM (1 beat = 0.5 s), no boss yet
                # Intro: quiet lines of squids, one side then the other
                WAVE 1 after=cleared wait=2.0
                SQUID start=-28,70 speed=170 count=5 spawn=0.0 first=0.5 shot=AIMED bullets=1 bulletSpeed=170 every=1.00 color=PINK path=180,70 380,150 588,150
                SQUID start=588,70 speed=170 count=5 spawn=0.0 first=0.5 follows=1 shot=AIMED bullets=1 bulletSpeed=170 every=1.00 color=PINK path=380,70 180,150 -28,150
                # Buildup: two crabs drop in, park and fan, then the squid lines answer them
                WAVE 2 after=cleared wait=1.0
                CRAB start=140,-28 speed=140 count=1 spawn=0.0 first=0.2 fire=stopped shot=FAN bullets=5 spread=50 bulletSpeed=150 every=1.00 color=GREEN path=140,120@3.0 -28,220
                CRAB start=420,-28 speed=140 count=1 spawn=0.5 first=0.2 fire=stopped shot=FAN bullets=5 spread=50 bulletSpeed=150 every=1.00 color=GREEN path=420,120@3.0 588,220
                SQUID start=-28,40 speed=200 count=6 spawn=0.0 first=0.3 follows=1 shot=AIMED bullets=2 bulletSpeed=180 every=1.50 color=PINK path=300,260 588,300
                SQUID start=588,40 speed=200 count=6 spawn=0.0 first=0.3 follows=2 shot=AIMED bullets=2 bulletSpeed=180 every=1.50 color=PINK path=260,260 -28,300
                # Melody: an octopus parks in the middle - a fast spiral, then rings that freeze and home in
                WAVE 3 after=cleared wait=1.0
                OCTOPUS start=280,110@9.0 speed=120 count=1 spawn=0.0 first=0.0 lasts=4.0 shot=SPIRAL bullets=4 bulletSpeed=140 every=0.13 spin=11 color=BLUE path=280,-28
                  PHASE lasts=0.0 shot=RING bullets=16 bulletSpeed=170 every=1.00 color=PURPLE steps=0.60/0/0/0/0/no/0;1.20/170/0/0/0/aim/0
                SQUID start=-28,260 speed=90 count=1 spawn=1.0 first=0.0 shot=FAN bullets=3 spread=30 bulletSpeed=150 every=2.00 color=PINK path=60,260@6.0 -28,320
                SQUID start=588,260 speed=90 count=1 spawn=1.0 first=0.0 shot=FAN bullets=3 spread=30 bulletSpeed=150 every=2.00 color=PINK path=500,260@6.0 588,320
                # Break: a few slow squids whose single bullets burst into rings - learn the new trick
                WAVE 4 after=cleared wait=2.0
                SQUID start=120,-28 speed=110 count=1 spawn=0.0 first=0.5 shot=AIMED bullets=1 bulletSpeed=110 every=2.00 color=YELLOW size=6 steps=0.80/110/0/0/0/no/6 path=120,90@4.0 -28,90
                SQUID start=440,-28 speed=110 count=1 spawn=1.0 first=0.5 shot=AIMED bullets=1 bulletSpeed=110 every=2.00 color=YELLOW size=6 steps=0.80/110/0/0/0/no/6 path=440,90@4.0 588,90
                SQUID start=280,-28 speed=110 count=1 spawn=0.0 first=0.5 follows=1 shot=AIMED bullets=1 bulletSpeed=110 every=2.00 color=YELLOW size=6 steps=0.80/110/0/0/0/no/6 path=280,80@4.0 280,-28
                # Climax: crossing spirals from two octopuses, a laser squid, then bouncing rings
                WAVE 5 after=cleared wait=1.0
                OCTOPUS start=160,120@12.0 speed=120 count=1 spawn=0.0 first=0.0 lasts=6.0 shot=SPIRAL bullets=5 bulletSpeed=120 every=0.10 spin=7 color=BLUE path=-28,120
                  PHASE lasts=0.0 shot=RING bullets=20 bulletSpeed=150 every=1.50 color=BLUE bounces=1
                OCTOPUS start=400,120@12.0 speed=120 count=1 spawn=0.0 first=0.0 lasts=6.0 shot=SPIRAL bullets=5 bulletSpeed=120 every=0.10 spin=-7 color=PURPLE path=588,120
                  PHASE lasts=0.0 shot=RING bullets=20 bulletSpeed=150 every=1.50 color=PURPLE bounces=1
                SQUID start=280,50@9.0 speed=120 count=1 spawn=2.0 first=1.0 shot=LASER bullets=3 spread=50 size=4 every=2.50 color=YELLOW path=280,-28
                # Outro: fast lines with curving shots, then the stage ends (boss goes here later)
                WAVE 6 after=cleared wait=1.5
                SQUID start=-28,90 speed=230 count=8 spawn=0.0 first=0.3 shot=AIMED bullets=1 bulletSpeed=160 every=0.75 curve=40 color=RED path=588,200
                SQUID start=588,140 speed=230 count=8 spawn=1.0 first=0.3 shot=AIMED bullets=1 bulletSpeed=160 every=0.75 curve=-40 color=RED path=-28,250
                """,
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
        public static final double SHOT_INTERVAL = 0.12;           // seconds between shots while fire is held
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
        public static final double MIN_SPACING = 10;               // px between enemies of a formation
        public static final double MAX_SPACING = 150;
        public static final double FORMATION_TURN_SPEED = 240;     // degrees per second a facing formation turns
        public static final double ENEMY_TURN_SPEED = 540;         // degrees per second an enemy turns to point along its path
        /** How an enemy's picture points as it flies: upright (never turns), head first, or feet first along its path. */
        public enum Pointing { UPRIGHT, HEAD_FIRST, FEET_FIRST }
        public static final int MAX_REPEATS = 99;                  // extra batches a group can send down its path
        public static final double MIN_REPEAT_GAP = 0.2;           // seconds between batches
        public static final double MAX_REPEAT_GAP = 60;
        public static final int MAX_HEALTH = 300;                  // shots it takes to destroy the toughest tank
        public static final double HIT_FLASH_TIME = 0.08;          // seconds an enemy flashes white after a hit
        public static final double MAX_SPAWN_TIME = 120;           // seconds after the enemy's wave starts
        public static final double MAX_FIRST_SHOT_TIME = 60;       // seconds on screen before the first shot, at most
        public static final double MAX_STOP_TIME = 30;             // seconds an enemy can hold still at a stop
        public static final double MAX_WAVE_WAIT = 30;             // seconds a wave can wait before starting
        public static final double APPEAR_TIME = 0.4;              // seconds an on-screen spawn takes to fade in

        // Enemy bullets
        public static final double BULLET_RADIUS = 4;
        public static final int MAX_BULLETS_PER_SHOT = 200;
        public static final double MIN_BULLET_SPEED = 40;
        public static final double MAX_BULLET_SPEED = 600;
        public static final double MIN_FIRE_INTERVAL = 0.05;      // seconds between shots
        /** Most enemy bullets on screen at once; past this new shots are skipped, so a huge pattern can't freeze the game. */
        public static final int MAX_ENEMY_BULLETS = 4000;
        public static final double MAX_FIRE_INTERVAL = 5;
        public static final double MAX_BULLET_ACCEL = 1000;        // px/s gained (or lost) per second
        public static final double MAX_BULLET_CURVE = 360;         // degrees a bullet turns per second
        public static final double MAX_STEP_TIME = 10;             // seconds after firing a bullet step can happen
        public static final int MAX_BULLET_STEPS = 8;
        public static final double MAX_WAVE_SIZE = 200;            // px a wavy bullet swings to each side, at most
        public static final double MAX_WAVE_RATE = 10;             // side-to-side swings per second, at most
        public static final double OFFSCREEN_GRACE = 6;            // seconds a bullet fired off the screen has to fly onto it
        public static final double MAX_BULLET_AGE = 20;            // seconds before a bullet is removed anyway (e.g. one stopped for good)
        public static final int MAX_PHASES = 24;                   // patterns an enemy can have, layers included
        public static final double MIN_BULLET_SIZE = 2;
        public static final double MAX_BULLET_SIZE = 16;
        /** Picture size limits (1 = the picture fits the hitbox as usual): it only changes the look, never the hitbox. */
        public static final double MIN_PICTURE_SIZE = 0.25;
        public static final double MAX_PICTURE_SIZE = 3;
        public static final int MAX_BOUNCES = 5;
        public static final int MAX_SPLIT = 36;
        public static final double LASER_WARN_TIME = 0.6;          // seconds a laser shows as a thin warning line
        public static final double LASER_TIME = 0.8;               // seconds the beam then lasts
        public static final double LASER_LENGTH = 900;             // long enough to cross the playfield
        public static final double MAX_BEAM_TIME = 30;             // seconds a laser beam can last (long ones make cages)
        public static final double MAX_SWEEP = 360;                // degrees per second the aim can sweep
        public static final double MAX_WALL_WIDTH = 600;           // px, a bit wider than the playfield
        public static final double MAX_PHASE_TIME = 60;

        // Bosses
        public static final double BOSS_SCALE = 2;                 // a boss is drawn (and hit) this many times bigger
        public static final int MAX_CARD_HEALTH = 2000;            // shots to beat one boss attack
        public static final int CARD_HEALTH = 150;                 // a new boss attack's health
        public static final double CARD_TIME = 30;                 // a new boss attack's time limit, s
        public static final double CARD_BREAK = 1.5;               // s between boss attacks: no shooting, can't be hurt
        public static final double MIN_HOP = 0.5;                  // s between a moving boss's hops, at least
        public static final double MAX_HOP = 10;
        public static final double WANDER_RANGE_X = 110;           // px a moving boss strays left/right of its spot
        public static final double WANDER_RANGE_Y = 45;            // px it strays up/down
        public static final int MAX_VOLLEYS = 99;                  // shots a pattern fires per stop, at most
        public static final int MAX_MOVE_POINTS = 16;              // points in one boss attack's movement path
        public static final double BOSS_LEAVE_SPEED = 220;         // px/s it flies up and away when time runs out

        // ================= RUN SETTINGS: lives, bombs, power, score and items. Change any of these freely. =================

        // Lives
        public static final int STARTING_LIVES = 3;                // lives at the start of a run (the ship you fly counts as one)
        public static final int MAX_LIVES = 8;
        /** Each score here gives one extra life the first time the run's score reaches it. */
        public static final long[] EXTRA_LIFE_SCORES = {50_000, 125_000, 250_000, 400_000, 600_000};
        public static final int CONTINUES = 2;                     // continues a run gets; after the last one, out of lives is game over
        public static final int CONTINUE_LIVES = 2;                // lives after continuing
        public static final int CONTINUE_BOMBS = 2;                // bombs after continuing
        public static final double CONTINUE_TIME = 10;             // s to decide on the CONTINUE? screen before it counts as no
        public static final boolean ENEMIES_KILL_ON_TOUCH = true;  // an enemy (or boss) touching the hitbox costs a life
        public static final double ENEMY_TOUCH_SIZE = 0.7;         // how much of an enemy's drawing counts for touching (1 = all of it)
        public static final double DEATH_POWER_LOSS = 25;          // power lost each time the ship is hit
        public static final double DEATH_INVINCIBLE_TIME = 3;      // s the ship can't be hit after it comes back from losing a life
        public static final double DEATH_FREEZE_TIME = 2;          // s everything stops while the ship explodes after a hit
        public static final boolean DEATH_CLEARS_BULLETS = false;  // true: losing a life wipes the bullets off the screen (off: everything keeps going)
        public static final boolean DEATH_RESPAWNS_AT_START = false; // true: the ship comes back at the bottom middle (off: it stays where it was hit)

        // Power: the meter goes from 0 to MAX_POWER and changes how the ship shoots
        public static final double MAX_POWER = 120;
        public static final double SMALL_POWER_VALUE = 3;          // power from a small power box
        public static final double BIG_POWER_VALUE = 12;           // power from a big power box
        /**
         * How the ship shoots at each amount of power. Each row: {power needed, seconds between shots,
         * bullets per shot, px between side-by-side bullets, degrees they fan out}. The last row the
         * power has reached is used. Add rows, or change bullets and speeds, as you like.
         */
        public static final double[][] POWER_LEVELS = {
                {0,   0.120, 1, 0,  0},
                {50,  0.105, 1, 0,  0},
                {90,  0.090, 1, 0,  0},
                {120, 0.090, 2, 14, 0},
        };
        /** At full power the ship fires rockets. With this on, side-by-side rockets take turns (left, right, left...) instead of firing together. */
        public static final boolean MAX_POWER_ALTERNATES = true;
        /** How many hits one rocket counts as. Rockets fire this many times slower, so the total damage stays the same. */
        public static final int ROCKET_DAMAGE = 3;
        /**
         * Flak: when a rocket kills a small enemy or a tank, it explodes. In that instant the explosion destroys
         * every small enemy and tank inside it, and every enemy bullet. Enemies it destroys don't explode again,
         * and bosses aren't hurt by it.
         */
        public static final double FLAK_RADIUS = 36;               // px from the center the explosion reaches
        public static final double FLAK_TIME = 0.48;               // how long the explosion animation plays (seconds)
        public static final double ENEMY_DEATH_TIME = 0.32;        // how long an enemy's death animation plays (seconds)

        // Score
        public static final long SMALL_KILL_SCORE = 100;           // for shooting down a small enemy
        public static final long TANK_KILL_SCORE = 1_000;          // for a tank (TANK_HEALTH or more)
        public static final long BOSS_ATTACK_SCORE = 5_000;        // for beating one boss attack
        public static final long POINT_VALUE_MAX = 1_000;          // a score box picked up at or above the collect line
        public static final long POINT_VALUE_MIN = 300;            // a score box picked up at the very bottom
        public static final long POWER_ITEM_SCORE = 10;            // every power box also gives this
        public static final long FULL_POWER_ITEM_SCORE = 200;      // a small power box at full power gives this instead (big: 4x)

        // What enemies drop
        public static final int TANK_HEALTH = 10;                  // enemies with this much health or more count as tanks
        public static final double SMALL_POWER_CHANCE = 0.30;      // a small enemy's chance to drop a small power box
        public static final double BIG_POWER_CHANCE = 0.04;        // ...a big power box (a rarer roll, checked first)
        public static final double POINT_CHANCE = 0.30;            // ...a score box
        public static final int TANK_POWER_ITEMS = 5;              // small power boxes a tank bursts into
        public static final int TANK_BIG_POWER_ITEMS = 1;          // big power boxes a tank always drops
        public static final int TANK_POINT_ITEMS = 6;              // score boxes a tank bursts into
        public static final int BOSS_ATTACK_POINT_ITEMS = 20;      // score boxes from each beaten boss attack
        public static final int BOSS_ATTACK_POWER_ITEMS = 0;       // small power boxes from each beaten boss attack
        public static final boolean BOSS_ITEMS_FLY_TO_YOU = true;  // a beaten boss attack's items come to the ship by themselves

        // Items (the falling boxes)
        public static final double COLLECT_LINE = PLAYFIELD_HEIGHT * 0.25; // above this line every item flies to the ship
        public static final double ITEM_SIZE = 10;                 // px, a big power box is drawn bigger
        // Items are picked up when they touch the graze zone (GRAZE_RADIUS around the hitbox); they don't drift to the ship
        public static final double ITEM_POP_SPEED = 130;           // px/s a dropped item first jumps up
        public static final double ITEM_GRAVITY = 260;             // px/s gained falling, per second
        public static final double ITEM_FALL_SPEED = 110;          // px/s, the fastest an item falls
        public static final double ITEM_HOMING_SPEED = 650;        // px/s an item flies to the ship
        public static final double BURST_SPREAD = 32;              // px a burst of items scatters around the enemy

        // Grazing: bullets that pass close to the hitbox without touching it score points
        public static final double GRAZE_RADIUS = 20;              // px from the hitbox's centre: a bullet inside this zone grazes
        public static final long GRAZE_SCORE = 50;                 // points for each bullet grazed (each bullet counts once)
        public static final long LASER_GRAZE_SCORE = 50;           // points for grazing a laser beam (once per beam)

        // The run
        public static final int RUN_LEVELS = 3;                    // how many STAGES a run plays in a row (empty stages are skipped)
        public static final double STAGE_CLEAR_TIME = 3;           // s the "STAGE CLEAR" banner shows before the next level
        public static final int SCOREBOARD_SIZE = 10;              // names kept on the scoreboard

        // Bombs (X)
        public static final int STARTING_BOMBS = 2;
        public static final int MAX_BOMBS = 8;
        public static final double BEAM_TIME = 3;                  // s the big beam lasts (bomb while not focused)
        public static final double BEAM_WIDTH = 420;               // px at its widest (the playfield is 560, so a centred ship leaves ~70 px each side)
        public static final double BEAM_FLARE = 110;               // px above the ship over which the beam widens from thin to full width
        public static final double BEAM_SLOWDOWN = 0.35;           // the ship moves at this fraction of its speed while the beam fires
        public static final double BEAM_BOSS_DAMAGE = 60;          // boss attack health the beam takes away each second
        public static final boolean BEAM_CLEARS_BULLETS = true;    // bullets inside the beam vanish
        public static final double BARRIER_RADIUS = 50;            // px around the ship (bomb while focused)
        public static final double BARRIER_HOLD_TIME = 2;          // s the barrier stays around the ship
        public static final double BARRIER_EXPAND_TIME = 0.6;      // s it then takes to grow over the whole screen
        public static final double BOMB_INVINCIBLE_EXTRA = 1;      // s the ship stays safe after a bomb ends

        /** The kinds of item enemies drop. */
        public enum ItemType { SMALL_POWER, BIG_POWER, POINT, BOMB }

        /** The game runs in fixed steps of 1/60 s, like ZUN's games, so a pattern plays out the same every time. */
        public static final int TICKS_PER_SECOND = 60;
        public static final double TICK = 1.0 / TICKS_PER_SECOND;

        /** The shapes an enemy's shot can take. */
        public enum Pattern {
            NONE,    // doesn't shoot
            AIMED,   // straight at the player; more bullets make a quick line of them
            FAN,     // bullets spread evenly over an angle, centred on the player
            RING,    // a full circle of bullets
            SPIRAL,  // a circle that turns a little after every shot
            RANDOM,  // bullets sprayed at random angles within the spread, toward the player
            LASER,   // beams (fanned out by the spread; 360 spreads them evenly all around): a warning line, then the beam
            WALL     // a straight line of bullets side by side, all flying the same way
        }

        /** How a group's enemies fly together. */
        public enum Formation {
            LINE,    // one after another along the path
            ROW,     // side by side
            COLUMN,  // one above the other
            V,       // a V pointing down, the first enemy at the tip
            GRID,    // a square block
            CIRCLE,  // a ring
            // Outlines and lines
            TRIANGLE,      // a triangle's outline, pointing up
            SQUARE,        // a hollow square
            DIAMOND,       // a hollow diamond
            STAR,          // a five-pointed star's outline
            HEART,         // a heart's outline
            X,             // two crossing diagonals
            PLUS,          // a row and a column crossing in the middle
            ARROW_UP,      // an upside-down V, tip at the top
            DIAGONAL_DOWN, // a straight line from top left to bottom right
            DIAGONAL_UP,   // a straight line from bottom left to top right
            // Curves
            ARC,           // a curve like a smile (a U)
            ARCH,          // a curve like a rainbow (an upside-down U)
            WAVE,          // a row that waves up and down
            S_CURVE,       // a column that waves left and right
            SPIRAL         // winding out from the middle
        }

        /** Where a phase shoots. */
        public enum Aim {
            PLAYER,  // at the player, every shot
            ONCE,    // at the player on the phase's first shot, then keeps that direction
            FIXED    // always the phase's direction, whatever the player does
        }

        /** The colors a phase's bullets can have. */
        /** How a boss moves during an attack: holds its spot, or hops around near it. */
        public enum BossMove { STAY, WANDER, PATH }

        public enum BulletColor { PINK, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, WHITE }

        /** How a bullet looks: a round orb, a long rice grain, a pointed knife or a spinning star. Only the look changes. */
        public enum BulletShape { ORB, RICE, KNIFE, STAR }

        /**
         * One enemy bullet picture on enemy_bullets.png (from FoX71's shot sheet): its name (saved in levels as
         * pic=), the group it's listed under, and where it is on the sheet (x, y, width, height in px).
         * Pictures point up; in the game they turn to point the way they fly.
         */
        public static final class BulletPicture {
            public final String name;
            public final String group;
            public final int x, y, width, height;

            BulletPicture(String name, String group, int x, int y, int width, int height) {
                this.name = name;
                this.group = group;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            /** The hitbox radius at the picture's own size: BULLET_HITBOX_SCALE of its thinner side, so grazing its edge is safe. */
            public double getHitRadius() {
                return BULLET_HITBOX_SCALE * Math.min(width, height) / 2;
            }

            /** How much to grow or shrink the picture so its hitbox is the given radius (the pattern's bullet size). */
            public double scaleFor(double hitRadius) {
                return hitRadius / getHitRadius();
            }
        }

        /** How big a picture bullet's hitbox is next to the picture (0.6 = 60% of its thinner side). */
        public static final double BULLET_HITBOX_SCALE = 0.6;

        /** Every bullet picture, grouped by shape, each group in the same color order. */
        public static final List<BulletPicture> BULLET_PICTURES = List.of(
                new BulletPicture("tiny_orb_red", "Tiny orb", 1, 1, 8, 8),
                new BulletPicture("tiny_orb_orange", "Tiny orb", 13, 1, 8, 8),
                new BulletPicture("tiny_orb_yellow", "Tiny orb", 25, 1, 8, 8),
                new BulletPicture("tiny_orb_green", "Tiny orb", 37, 1, 8, 8),
                new BulletPicture("tiny_orb_cyan", "Tiny orb", 49, 1, 8, 8),
                new BulletPicture("tiny_orb_blue", "Tiny orb", 61, 1, 8, 8),
                new BulletPicture("tiny_orb_pink", "Tiny orb", 73, 1, 8, 8),
                new BulletPicture("tiny_orb_white", "Tiny orb", 85, 1, 8, 8),
                new BulletPicture("tiny_orb_plain", "Tiny orb", 97, 1, 10, 10),
                new BulletPicture("orb_red", "Orb", 1, 13, 16, 16),
                new BulletPicture("orb_orange", "Orb", 19, 13, 16, 16),
                new BulletPicture("orb_yellow", "Orb", 37, 13, 16, 16),
                new BulletPicture("orb_green", "Orb", 55, 13, 16, 16),
                new BulletPicture("orb_cyan", "Orb", 73, 13, 16, 16),
                new BulletPicture("orb_blue", "Orb", 91, 13, 16, 16),
                new BulletPicture("orb_pink", "Orb", 109, 13, 16, 16),
                new BulletPicture("orb_white", "Orb", 127, 13, 16, 16),
                new BulletPicture("ringed_orb_red", "Ringed orb", 1, 31, 16, 16),
                new BulletPicture("ringed_orb_orange", "Ringed orb", 19, 31, 16, 16),
                new BulletPicture("ringed_orb_yellow", "Ringed orb", 37, 31, 16, 16),
                new BulletPicture("ringed_orb_green", "Ringed orb", 55, 31, 16, 16),
                new BulletPicture("ringed_orb_cyan", "Ringed orb", 73, 31, 16, 16),
                new BulletPicture("ringed_orb_blue", "Ringed orb", 91, 31, 16, 16),
                new BulletPicture("ringed_orb_pink", "Ringed orb", 109, 31, 16, 16),
                new BulletPicture("ringed_orb_white", "Ringed orb", 127, 31, 16, 16),
                new BulletPicture("big_orb_red", "Big orb", 1, 49, 32, 32),
                new BulletPicture("big_orb_orange", "Big orb", 35, 49, 32, 32),
                new BulletPicture("big_orb_yellow", "Big orb", 69, 49, 32, 32),
                new BulletPicture("big_orb_green", "Big orb", 103, 49, 32, 32),
                new BulletPicture("big_orb_cyan", "Big orb", 137, 49, 32, 32),
                new BulletPicture("big_orb_blue", "Big orb", 171, 49, 32, 32),
                new BulletPicture("big_orb_pink", "Big orb", 205, 49, 32, 32),
                new BulletPicture("big_orb_white", "Big orb", 239, 49, 32, 32),
                new BulletPicture("big_orb_plain", "Big orb", 273, 49, 30, 30),
                new BulletPicture("giant_orb_red", "Giant orb", 1, 83, 64, 64),
                new BulletPicture("giant_orb_orange", "Giant orb", 67, 83, 64, 64),
                new BulletPicture("giant_orb_yellow", "Giant orb", 133, 83, 64, 64),
                new BulletPicture("giant_orb_green", "Giant orb", 199, 83, 64, 64),
                new BulletPicture("giant_orb_cyan", "Giant orb", 265, 83, 64, 64),
                new BulletPicture("giant_orb_blue", "Giant orb", 331, 83, 64, 64),
                new BulletPicture("giant_orb_pink", "Giant orb", 397, 83, 64, 64),
                new BulletPicture("giant_orb_white", "Giant orb", 463, 83, 64, 64),
                new BulletPicture("pellet_red", "Pellet", 1, 149, 5, 9),
                new BulletPicture("pellet_orange", "Pellet", 8, 149, 5, 9),
                new BulletPicture("pellet_yellow", "Pellet", 15, 149, 5, 9),
                new BulletPicture("pellet_green", "Pellet", 22, 149, 5, 9),
                new BulletPicture("pellet_cyan", "Pellet", 29, 149, 5, 9),
                new BulletPicture("pellet_blue", "Pellet", 36, 149, 5, 9),
                new BulletPicture("pellet_pink", "Pellet", 43, 149, 5, 9),
                new BulletPicture("pellet_white", "Pellet", 50, 149, 5, 9),
                new BulletPicture("rice_red", "Rice", 1, 160, 5, 12),
                new BulletPicture("rice_orange", "Rice", 8, 160, 5, 12),
                new BulletPicture("rice_yellow", "Rice", 15, 160, 5, 12),
                new BulletPicture("rice_green", "Rice", 22, 160, 5, 12),
                new BulletPicture("rice_cyan", "Rice", 29, 160, 5, 12),
                new BulletPicture("rice_blue", "Rice", 36, 160, 5, 12),
                new BulletPicture("rice_pink", "Rice", 43, 160, 5, 12),
                new BulletPicture("rice_white", "Rice", 50, 160, 5, 12),
                new BulletPicture("bar_red", "Bar", 1, 174, 12, 16),
                new BulletPicture("bar_orange", "Bar", 15, 174, 12, 16),
                new BulletPicture("bar_yellow", "Bar", 29, 174, 12, 16),
                new BulletPicture("bar_green", "Bar", 43, 174, 12, 16),
                new BulletPicture("bar_cyan", "Bar", 57, 174, 12, 16),
                new BulletPicture("bar_blue", "Bar", 71, 174, 12, 16),
                new BulletPicture("bar_pink", "Bar", 85, 174, 12, 16),
                new BulletPicture("bar_white", "Bar", 99, 174, 12, 16),
                new BulletPicture("bar_rainbow", "Bar", 113, 174, 12, 16),
                new BulletPicture("arrowhead_red", "Arrowhead", 1, 192, 7, 12),
                new BulletPicture("arrowhead_orange", "Arrowhead", 10, 192, 7, 12),
                new BulletPicture("arrowhead_yellow", "Arrowhead", 19, 192, 7, 12),
                new BulletPicture("arrowhead_green", "Arrowhead", 28, 192, 7, 12),
                new BulletPicture("arrowhead_cyan", "Arrowhead", 37, 192, 7, 12),
                new BulletPicture("arrowhead_blue", "Arrowhead", 46, 192, 7, 12),
                new BulletPicture("arrowhead_pink", "Arrowhead", 55, 192, 7, 12),
                new BulletPicture("arrowhead_white", "Arrowhead", 64, 192, 7, 12),
                new BulletPicture("dart_red", "Dart", 1, 206, 13, 14),
                new BulletPicture("dart_orange", "Dart", 16, 206, 13, 14),
                new BulletPicture("dart_yellow", "Dart", 31, 206, 13, 14),
                new BulletPicture("dart_green", "Dart", 46, 206, 13, 14),
                new BulletPicture("dart_cyan", "Dart", 61, 206, 13, 14),
                new BulletPicture("dart_blue", "Dart", 76, 206, 13, 14),
                new BulletPicture("dart_pink", "Dart", 91, 206, 13, 14),
                new BulletPicture("dart_white", "Dart", 106, 206, 13, 14),
                new BulletPicture("crystal_red", "Crystal", 1, 222, 11, 17),
                new BulletPicture("crystal_orange", "Crystal", 14, 222, 11, 17),
                new BulletPicture("crystal_yellow", "Crystal", 27, 222, 11, 17),
                new BulletPicture("crystal_green", "Crystal", 40, 222, 11, 17),
                new BulletPicture("crystal_cyan", "Crystal", 53, 222, 11, 17),
                new BulletPicture("crystal_blue", "Crystal", 66, 222, 11, 17),
                new BulletPicture("crystal_pink", "Crystal", 79, 222, 11, 17),
                new BulletPicture("crystal_white", "Crystal", 92, 222, 11, 17),
                new BulletPicture("star_red", "Star", 1, 241, 15, 15),
                new BulletPicture("star_orange", "Star", 18, 241, 15, 15),
                new BulletPicture("star_yellow", "Star", 35, 241, 15, 15),
                new BulletPicture("star_green", "Star", 52, 241, 15, 15),
                new BulletPicture("star_cyan", "Star", 69, 241, 15, 15),
                new BulletPicture("star_blue", "Star", 86, 241, 15, 15),
                new BulletPicture("star_pink", "Star", 103, 241, 15, 15),
                new BulletPicture("star_white", "Star", 120, 241, 15, 15),
                new BulletPicture("flame_red", "Flame", 1, 258, 9, 16),
                new BulletPicture("flame_orange", "Flame", 12, 258, 9, 16),
                new BulletPicture("flame_yellow", "Flame", 23, 258, 9, 16),
                new BulletPicture("flame_green", "Flame", 34, 258, 9, 16),
                new BulletPicture("flame_cyan", "Flame", 45, 258, 9, 16),
                new BulletPicture("flame_blue", "Flame", 56, 258, 9, 16),
                new BulletPicture("flame_pink", "Flame", 67, 258, 9, 16),
                new BulletPicture("flame_white", "Flame", 78, 258, 9, 16),
                new BulletPicture("trail_red", "Trail", 1, 276, 11, 30),
                new BulletPicture("trail_orange", "Trail", 14, 276, 11, 30),
                new BulletPicture("trail_yellow", "Trail", 27, 276, 11, 30),
                new BulletPicture("trail_green", "Trail", 40, 276, 11, 30),
                new BulletPicture("trail_cyan", "Trail", 53, 276, 11, 30),
                new BulletPicture("trail_blue", "Trail", 66, 276, 11, 30),
                new BulletPicture("trail_pink", "Trail", 79, 276, 11, 30),
                new BulletPicture("trail_white", "Trail", 92, 276, 11, 30));

        /** The bullet picture with this name, or null if there isn't one. */
        public static BulletPicture bulletPicture(String name) {
            for (BulletPicture picture : BULLET_PICTURES) {
                if (picture.name.equals(name)) {
                    return picture;
                }
            }
            return null;
        }

        private double playerX = PLAYFIELD_WIDTH / 2;
        private double playerY = PLAYER_START_Y;
        private boolean focused = false;
        private double shotCooldown = 0;
        /** Which side fires next when full-power rockets take turns. */
        private int nextShotSide = 0;
        /** Flak explosions playing: {x, y, seconds so far}. */
        private final List<double[]> flaks = new ArrayList<>();
        /** Enemy death animations playing: {x, y, seconds so far, size}. */
        private final List<double[]> deaths = new ArrayList<>();

        /** Player shots. Each one is {x, y, x speed, y speed, kind, power stage}: kind 0 is a bullet, 1 a full-power rocket; power stage is the POWER_LEVELS row it was fired at (picks its picture). */
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
        /** A wave that mustn't start yet even when its turn comes (the boss waiting for the asteroid to pass), or -1. */
        private int heldWave = -1;
        /** Each time the screen was cleared of tanks this run (all shot down or flown off), in level time. */
        private final List<Double> tanksGoneTimes = new ArrayList<>();
        private boolean tanksOnScreen;
        /** How many of each design's enemies have spawned so far this run. */
        private final Map<EnemyDesign, Integer> spawned = new HashMap<>();
        /** When each group was cleared this run (all spawned, none left flying). */
        private final Map<EnemyDesign, Double> groupClearTimes = new HashMap<>();
        // For groups whose next batch waits until the last one is gone: when that batch was gone, and when the batch being sent began
        private final Map<EnemyDesign, Double> batchGoneTimes = new HashMap<>();
        private final Map<EnemyDesign, Double> batchStartTimes = new HashMap<>();
        // When each batch of such a group began, so groups linked to it can send theirs alongside
        private final Map<EnemyDesign, List<Double>> batchHistory = new HashMap<>();
        /** When the first enemy of each group spawned this run. */
        private final Map<EnemyDesign, Double> groupSpawnTimes = new HashMap<>();
        private final List<EnemyBullet> enemyBullets = new ArrayList<>();
        private final List<Laser> lasers = new ArrayList<>();
        /** How many times the ship has been hit this run. */
        private int hits = 0;
        // The run: carried from one level to the next (see startNextLevel)
        private int lives = STARTING_LIVES;
        private int bombs = STARTING_BOMBS;
        private double power = 0;
        private long score = 0;
        private int extraLivesGiven = 0;
        private int continuesLeft = CONTINUES;
        /** Bullets (and beams) grazed this run. */
        private int grazes = 0;
        private boolean outOfLives = false;
        /** Set while a new level of the same run is loaded, so the run isn't reset. */
        private boolean keepRun = false;
        private final List<Item> items = new ArrayList<>();
        /** Seconds left of the beam bomb, and of the barrier bomb (hold, then grow); 0 when none is on. */
        private double beamTimeLeft = 0;
        private double barrierTime = -1;
        private double barrierRadius = 0;
        private final Random dropRandom = new Random();
        /** Seconds left before the ship can be hit again. */
        private double invincibleTimeLeft = 0;
        /** Seconds left of the stop after a hit, while the ship explodes (0 = playing). */
        private double deathFreezeLeft = 0;
        private final Random random = new Random();

        /** Advances the game by one tick (1/60 s). See {@link #update}. */
        public void tick(int dx, int dy, boolean focusHeld, boolean fireHeld) {
            tick(dx, dy, focusHeld, fireHeld, false);
        }

        /** Advances the game by one tick; bombPressed uses a bomb if there's one and none is going off. */
        public void tick(int dx, int dy, boolean focusHeld, boolean fireHeld, boolean bombPressed) {
            if (bombPressed) {
                useBomb(focusHeld);
            }
            update(TICK, dx, dy, focusHeld, fireHeld);
        }

        /**
         * Advances the game by the given time. dx and dy are -1, 0 or 1 from the arrow keys, focusHeld slows
         * the ship down, and fireHeld fires a stream of shots. The game calls it through tick().
         */
        public void update(double deltaSeconds, int dx, int dy, boolean focusHeld, boolean fireHeld) {
            if (deathFreezeLeft > 0) {
                // The ship was hit: everything stops while it explodes, then it comes back (invincible for a while)
                deathFreezeLeft = Math.max(0, deathFreezeLeft - deltaSeconds);
                flaks.removeIf(flak -> (flak[2] += deltaSeconds) >= FLAK_TIME);
                return;
            }
            levelTime += deltaSeconds;
            focused = focusHeld;
            movePlayer(deltaSeconds, dx, dy);

            shotCooldown = Math.max(0, shotCooldown - deltaSeconds);
            if (fireHeld && shotCooldown == 0) {
                fireShots();
            }
            for (double[] shot : shots) {
                shot[0] += shot[2] * deltaSeconds;
                shot[1] += shot[3] * deltaSeconds;
            }
            shots.removeIf(shot -> shot[1] + SHOT_HEIGHT / 2 < 0 || shot[0] < -SHOT_WIDTH || shot[0] > PLAYFIELD_WIDTH + SHOT_WIDTH);

            updateWaves();
            spawnDueEnemies();
            for (Enemy enemy : enemies) {
                enemy.fly(deltaSeconds);
                for (int layer : enemy.layersReadyToFire(deltaSeconds)) {
                    fire(enemy, layer);
                }
                if (enemy.takeAttackEnded()) {
                    clearEnemyBullets();
                }
            }
            enemies.removeIf(Enemy::hasLeftScreen);
            shots.removeIf(this::hitEnemy);
            flaks.removeIf(flak -> (flak[2] += deltaSeconds) >= FLAK_TIME);
            deaths.removeIf(death -> (death[2] += deltaSeconds) >= ENEMY_DEATH_TIME);
            boolean tanksNow = enemies.stream().anyMatch(enemy -> !enemy.getDesign().isBoss() && enemy.getDesign().getHealth() >= TANK_HEALTH);
            if (tanksOnScreen && !tanksNow) {
                tanksGoneTimes.add(levelTime);
            }
            tanksOnScreen = tanksNow;
            updateBombs(deltaSeconds);
            moveEnemyBullets(deltaSeconds);
            moveItems(deltaSeconds);
        }

        // ---- The run: power, score, lives, items and bombs

        /**
         * A rocket killed a small enemy or a tank: a flak explosion where it was. In that instant it destroys every
         * small enemy and tank inside FLAK_RADIUS (they play their normal death, no new explosion) and every enemy bullet.
         */
        private void flakExplosion(double x, double y) {
            deaths.remove(deaths.size() - 1); // the explosion shows instead of the killed enemy's own death animation
            flaks.add(new double[]{x, y, 0});
            for (Enemy enemy : new ArrayList<>(enemies)) {
                if (enemy.getDesign().isBoss() || enemy.isAppearing()
                        || Math.hypot(enemy.getX() - x, enemy.getY() - y) > FLAK_RADIUS) {
                    continue;
                }
                enemies.remove(enemy);
                enemyDestroyed(enemy);
            }
            enemyBullets.removeIf(b -> Math.hypot(b.getX() - x, b.getY() - y) < FLAK_RADIUS + b.getRadius());
        }

        /** The row of POWER_LEVELS the ship's power has reached. */
        public int getPowerLevel() {
            int level = 0;
            for (int i = 0; i < POWER_LEVELS.length; i++) {
                if (power >= POWER_LEVELS[i][0]) level = i;
            }
            return level;
        }

        /** Fires one shot as the power level says: one or more bullets, side by side and/or fanned out. */
        private void fireShots() {
            double[] level = POWER_LEVELS[getPowerLevel()];
            int count = Math.max(1, (int) level[2]);
            boolean fullPower = power >= MAX_POWER;
            // Full-power rockets can take turns: one side per shot, twice as often, so the damage stays the same
            boolean takeTurns = fullPower && MAX_POWER_ALTERNATES && count > 1;
            for (int i = 0; i < count; i++) {
                if (takeTurns && i != nextShotSide % count) {
                    continue;
                }
                double t = count == 1 ? 0 : i / (count - 1.0) - 0.5; // -0.5 .. 0.5 across the bullets
                double x = playerX + t * level[3] * (count - 1);
                double angle = Math.toRadians(-90 + t * level[4]);
                shots.add(new double[]{x, playerY - PLAYER_HEIGHT / 2, Math.cos(angle) * SHOT_SPEED, Math.sin(angle) * SHOT_SPEED,
                        fullPower ? 1 : 0, getPowerLevel()});
            }
            nextShotSide = (nextShotSide + 1) % count;
            shotCooldown = (takeTurns ? level[1] / count : level[1]) * (fullPower ? ROCKET_DAMAGE : 1);
        }

        private void addScore(long points) {
            score += points;
            while (extraLivesGiven < EXTRA_LIFE_SCORES.length && score >= EXTRA_LIFE_SCORES[extraLivesGiven]) {
                extraLivesGiven++;
                lives = Math.min(MAX_LIVES, lives + 1);
                outOfLives = false;
            }
        }

        /** The ship was hit: a life and some power are lost, and it gets a moment where it can't be hit. */
        private void loseLife() {
            hits++;
            lives = Math.max(0, lives - 1);
            if (lives == 0) {
                outOfLives = true;
            }
            power = Math.max(0, power - DEATH_POWER_LOSS);
            invincibleTimeLeft = DEATH_INVINCIBLE_TIME; // counts down only once the stop below is over
            deathFreezeLeft = DEATH_FREEZE_TIME;
            flaks.add(new double[]{playerX, playerY, 0}); // the ship explodes (the flak explosion's picture for now)
            if (DEATH_CLEARS_BULLETS) {
                clearEnemyBullets();
            }
            if (DEATH_RESPAWNS_AT_START) {
                playerX = PLAYFIELD_WIDTH / 2;
                playerY = PLAYER_START_Y;
            }
        }

        /** A non-boss enemy was shot down (or caught in the beam): score and its drops. */
        private void enemyDestroyed(Enemy enemy) {
            double x = enemy.getX();
            double y = enemy.getY();
            deaths.add(new double[]{x, y, 0, enemy.getDesign().getScale()});
            if (enemy.getDesign().getHealth() >= TANK_HEALTH) {
                addScore(TANK_KILL_SCORE);
                dropBurst(x, y, ItemType.BIG_POWER, TANK_BIG_POWER_ITEMS, false);
                dropBurst(x, y, ItemType.SMALL_POWER, TANK_POWER_ITEMS, false);
                dropBurst(x, y, ItemType.POINT, TANK_POINT_ITEMS, false);
                return;
            }
            addScore(SMALL_KILL_SCORE);
            double roll = dropRandom.nextDouble();
            if (roll < BIG_POWER_CHANCE) {
                dropBurst(x, y, ItemType.BIG_POWER, 1, false);
            } else if (roll < BIG_POWER_CHANCE + SMALL_POWER_CHANCE) {
                dropBurst(x, y, ItemType.SMALL_POWER, 1, false);
            } else if (roll < BIG_POWER_CHANCE + SMALL_POWER_CHANCE + POINT_CHANCE) {
                dropBurst(x, y, ItemType.POINT, 1, false);
            }
        }

        /** A boss attack was beaten: its score, and a burst of items. */
        private void bossAttackBeaten(Enemy boss) {
            addScore(BOSS_ATTACK_SCORE);
            dropBurst(boss.getX(), boss.getY(), ItemType.POINT, BOSS_ATTACK_POINT_ITEMS, BOSS_ITEMS_FLY_TO_YOU);
            dropBurst(boss.getX(), boss.getY(), ItemType.SMALL_POWER, BOSS_ATTACK_POWER_ITEMS, BOSS_ITEMS_FLY_TO_YOU);
        }

        /** Drops items around a spot; a single one drops right there. At full power, power items drop as point items. */
        private void dropBurst(double x, double y, ItemType type, int count, boolean flyToShip) {
            if (power >= MAX_POWER && (type == ItemType.SMALL_POWER || type == ItemType.BIG_POWER)) {
                type = ItemType.POINT;
            }
            for (int i = 0; i < count; i++) {
                double ix = x, iy = y;
                if (count > 1 || type == ItemType.BIG_POWER && flyToShip) {
                    ix += (dropRandom.nextDouble() * 2 - 1) * BURST_SPREAD;
                    iy += (dropRandom.nextDouble() * 2 - 1) * BURST_SPREAD;
                }
                Item item = new Item(type, Math.max(5, Math.min(PLAYFIELD_WIDTH - 5, ix)), Math.max(5, iy));
                item.vy = -ITEM_POP_SPEED * (count > 1 ? 0.6 + dropRandom.nextDouble() * 0.8 : 1);
                item.homing = flyToShip;
                item.fullValue = flyToShip; // sent to the ship: worth as much as if caught at the top
                items.add(item);
            }
        }

        /** Drops a bomb from the top of the screen (a wave set to give one was cleared). */
        private void dropBombFromTop() {
            Item bomb = new Item(ItemType.BOMB, PLAYFIELD_WIDTH / 2, -ITEM_SIZE);
            bomb.vy = ITEM_FALL_SPEED;
            items.add(bomb);
        }

        /** Moves the items: they hop up and fall, and fly to the ship only above the collect line or when a boss sends them. */
        private void moveItems(double deltaSeconds) {
            boolean collectAll = playerY < COLLECT_LINE;
            for (Item item : items) {
                double dx = playerX - item.x;
                double dy = playerY - item.y;
                double distance = Math.hypot(dx, dy);
                if (collectAll && !item.homing) {
                    item.homing = true;
                    item.fullValue = true; // collected at the top: score boxes are worth the most
                }
                if (item.homing) {
                    double step = Math.min(distance, ITEM_HOMING_SPEED * deltaSeconds);
                    if (distance > 0) {
                        item.x += dx / distance * step;
                        item.y += dy / distance * step;
                    }
                } else {
                    item.vy = Math.min(ITEM_FALL_SPEED, item.vy + ITEM_GRAVITY * deltaSeconds);
                    item.y += item.vy * deltaSeconds;
                }
            }
            items.removeIf(item -> {
                if (Math.hypot(playerX - item.x, playerY - item.y) < GRAZE_RADIUS + ITEM_SIZE / 2) {
                    collect(item);
                    return true;
                }
                return !item.homing && item.y > PLAYFIELD_HEIGHT + ITEM_SIZE * 2;
            });
        }

        private void collect(Item item) {
            switch (item.type) {
                case SMALL_POWER, BIG_POWER -> {
                    boolean big = item.type == ItemType.BIG_POWER;
                    if (power >= MAX_POWER) {
                        addScore(FULL_POWER_ITEM_SCORE * (big ? 4 : 1));
                    } else {
                        power = Math.min(MAX_POWER, power + (big ? BIG_POWER_VALUE : SMALL_POWER_VALUE));
                        addScore(POWER_ITEM_SCORE);
                    }
                }
                case POINT -> {
                    // Worth the most at the top of the screen, less the lower it's caught
                    double height = item.fullValue ? 1
                            : Math.max(0, Math.min(1, (PLAYFIELD_HEIGHT - item.y) / (PLAYFIELD_HEIGHT - COLLECT_LINE)));
                    addScore(Math.round(POINT_VALUE_MIN + (POINT_VALUE_MAX - POINT_VALUE_MIN) * height));
                }
                case BOMB -> bombs = Math.min(MAX_BOMBS, bombs + 1);
            }
        }

        /**
         * Uses a bomb if there's one left and none is going off. Not focused: a wide beam straight up
         * that destroys every enemy in it and hurts bosses. Focused: a barrier around the ship that
         * then grows over the whole screen, wiping out every bullet (enemies are left alone).
         */
        private void useBomb(boolean focusedNow) {
            if (bombs <= 0 || isBombGoingOff()) {
                return;
            }
            bombs--;
            if (focusedNow) {
                barrierTime = 0;
                barrierRadius = BARRIER_RADIUS;
                invincibleTimeLeft = Math.max(invincibleTimeLeft, BARRIER_HOLD_TIME + BARRIER_EXPAND_TIME + BOMB_INVINCIBLE_EXTRA);
            } else {
                beamTimeLeft = BEAM_TIME;
                invincibleTimeLeft = Math.max(invincibleTimeLeft, BEAM_TIME + BOMB_INVINCIBLE_EXTRA);
            }
        }

        private void updateBombs(double deltaSeconds) {
            if (beamTimeLeft > 0) {
                beamTimeLeft = Math.max(0, beamTimeLeft - deltaSeconds);
                for (Enemy enemy : new ArrayList<>(enemies)) {
                    double scale = enemy.getDesign().getScale();
                    boolean inBeam = !enemy.isAppearing() && enemy.getY() < playerY
                            && enemy.getY() > -Alien.HEIGHT * scale / 2
                            && Math.abs(enemy.getX() - playerX) < beamHalfWidth(enemy.getY()) + Alien.WIDTH * scale / 2;
                    if (!inBeam) continue;
                    if (enemy.getDesign().isBoss()) {
                        enemy.beamDamageOwed += BEAM_BOSS_DAMAGE * deltaSeconds;
                        int whole = (int) enemy.beamDamageOwed;
                        if (whole > 0) {
                            enemy.beamDamageOwed -= whole;
                            boolean destroyed = enemy.takeHit(whole);
                            afterBossHit(enemy, destroyed);
                        }
                    } else {
                        enemies.remove(enemy);
                        enemyDestroyed(enemy);
                    }
                }
                if (BEAM_CLEARS_BULLETS) {
                    enemyBullets.removeIf(b -> b.getY() < playerY + 10 && Math.abs(b.getX() - playerX) < beamHalfWidth(b.getY()) + b.getRadius());
                }
            }
            if (barrierTime >= 0) {
                barrierTime += deltaSeconds;
                if (barrierTime <= BARRIER_HOLD_TIME) {
                    barrierRadius = BARRIER_RADIUS;
                } else {
                    double grow = Math.min(1, (barrierTime - BARRIER_HOLD_TIME) / BARRIER_EXPAND_TIME);
                    double screen = Math.hypot(PLAYFIELD_WIDTH, PLAYFIELD_HEIGHT);
                    barrierRadius = BARRIER_RADIUS + (screen - BARRIER_RADIUS) * grow;
                }
                double r = barrierRadius;
                enemyBullets.removeIf(b -> Math.hypot(b.getX() - playerX, b.getY() - playerY) < r + b.getRadius());
                if (barrierTime >= BARRIER_HOLD_TIME + BARRIER_EXPAND_TIME) {
                    clearEnemyBullets(); // it has covered the whole screen: lasers go too
                    barrierTime = -1;
                    barrierRadius = 0;
                }
            }
        }

        /** After a boss takes damage: its beaten attack's items, the bullets cleared, and removed if destroyed. */
        private void afterBossHit(Enemy boss, boolean destroyed) {
            if (boss.takeAttackBeaten()) {
                bossAttackBeaten(boss);
            }
            if (destroyed) {
                enemies.remove(boss);
            }
            if (boss.takeAttackEnded()) {
                clearEnemyBullets();
            }
        }

        /**
         * Half the beam's width at a height on the screen: thin at the ship, flaring out like a bell over
         * BEAM_FLARE px, then BEAM_WIDTH wide all the way up. 0 below the ship.
         */
        public double beamHalfWidth(double y) {
            double above = playerY - y;
            if (above < 0) {
                return 0;
            }
            double t = Math.min(1, above / BEAM_FLARE);
            double bell = 1 - (1 - t) * (1 - t); // eases out: widens fast, then levels off
            return (12 + (BEAM_WIDTH / 2 - 12) * bell);
        }

        /** True while a bomb (the beam or the barrier) is going off. */
        public boolean isBombGoingOff() {
            return beamTimeLeft > 0 || barrierTime >= 0;
        }

        /** Seconds left of the beam bomb, or 0. */
        public double getBeamTimeLeft() {
            return beamTimeLeft;
        }

        /** The barrier bomb's radius right now, or 0 when there's none. */
        public double getBarrierRadius() {
            return barrierTime >= 0 ? barrierRadius : 0;
        }

        public List<Item> getItems() {
            return items;
        }

        public int getLives() {
            return lives;
        }

        public int getBombs() {
            return bombs;
        }

        public double getPower() {
            return power;
        }

        /** Sets the power meter (0 to MAX_POWER). For testing in the sandbox. */
        public void setPower(double value) {
            power = Math.max(0, Math.min(MAX_POWER, value));
        }

        public long getScore() {
            return score;
        }

        public int getContinuesLeft() {
            return continuesLeft;
        }

        /**
         * Uses a continue after the last life is lost: the ship comes back with CONTINUE_LIVES lives and
         * CONTINUE_BOMBS bombs, and the score is kept. False when there are none left.
         */
        public boolean useContinue() {
            if (continuesLeft <= 0) {
                return false;
            }
            continuesLeft--;
            lives = CONTINUE_LIVES;
            bombs = CONTINUE_BOMBS;
            outOfLives = false;
            invincibleTimeLeft = DEATH_INVINCIBLE_TIME;
            clearEnemyBullets();
            return true;
        }

        /** Bullets grazed this run. */
        public int getGrazes() {
            return grazes;
        }

        /** The levels a run plays, in order: the first RUN_LEVELS STAGES, skipping empty ones. */
        public static List<String> runLevels() {
            List<String> levels = new ArrayList<>();
            for (int i = 0; i < Math.min(RUN_LEVELS, STAGES.length); i++) {
                if (!STAGES[i].isBlank()) {
                    levels.add(STAGES[i]);
                }
            }
            return levels;
        }

        /** True once the last life is lost. (In the sandbox the game just keeps going.) */
        public boolean isOutOfLives() {
            return outOfLives;
        }

        /** Starts a new run: lives, bombs, power and score back to how a run begins. */
        public void resetRun() {
            lives = STARTING_LIVES;
            bombs = STARTING_BOMBS;
            power = 0;
            score = 0;
            extraLivesGiven = 0;
            continuesLeft = CONTINUES;
            grazes = 0;
            outOfLives = false;
            items.clear();
            beamTimeLeft = 0;
            barrierTime = -1;
            barrierRadius = 0;
        }

        /** Loads the next level of a run, keeping the lives, bombs, power and score. */
        public void startNextLevel(String text) {
            keepRun = true;
            try {
                loadLevel(text);
            } finally {
                keepRun = false;
            }
            playerX = PLAYFIELD_WIDTH / 2;
            playerY = PLAYER_START_Y;
        }

        /**
         * A small save file for the game's own data (the scoreboard), kept in a
         * folder in the player's home folder, not in the project. Plain key=value text.
         */
        public static final class SaveFile {
            /** The folder the game saves to: .spaceinvaders-capstone in the home folder. */
            public static final java.nio.file.Path FOLDER =
                    java.nio.file.Path.of(System.getProperty("user.home"), ".spaceinvaders-capstone");
            private final java.nio.file.Path path;
            private final java.util.Properties values = new java.util.Properties();

            /** Reads the file (an empty one if it isn't there yet). */
            public SaveFile(java.nio.file.Path path) {
                this.path = path;
                if (java.nio.file.Files.exists(path)) {
                    try (java.io.Reader in = java.nio.file.Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
                        values.load(in);
                    } catch (java.io.IOException e) {
                        // Can't read it: start empty
                    }
                }
            }

            public String get(String key) {
                return values.getProperty(key);
            }

            public void put(String key, String value) {
                values.setProperty(key, value);
            }

            public void remove(String key) {
                values.remove(key);
            }

            /** Writes the file. False if it couldn't be written (the data still lasts until the app closes). */
            public boolean save() {
                try {
                    java.nio.file.Files.createDirectories(path.getParent());
                    try (java.io.Writer out = java.nio.file.Files.newBufferedWriter(path, java.nio.charset.StandardCharsets.UTF_8)) {
                        values.store(out, "Space Invaders save data");
                    }
                    return true;
                } catch (java.io.IOException e) {
                    return false;
                }
            }
        }

        /**
         * The Extermination scoreboard: the best runs, highest score first, each with a 3-letter name and
         * how far the run got. Kept between plays in scoreboard.properties in the game's save folder.
         */
        public static final class Scoreboard {
            /** One line of the scoreboard. levelReached is the level the run ended on; RUN_LEVELS + 1 means it cleared them all. */
            public record Entry(String name, long score, int levelReached) { }

            private final SaveFile store;
            private final List<Entry> entries = new ArrayList<>();

            /** The saved scoreboard. */
            public Scoreboard() {
                this(SaveFile.FOLDER.resolve("scoreboard.properties"));
            }

            /** A scoreboard kept in the given file (tests use their own). */
            public Scoreboard(java.nio.file.Path file) {
                this.store = new SaveFile(file);
                for (int i = 0; i < SCOREBOARD_SIZE; i++) {
                    String name = store.get("name" + i);
                    if (name != null) {
                        try {
                            entries.add(new Entry(name, Long.parseLong(store.get("score" + i)), Integer.parseInt(store.get("level" + i))));
                        } catch (NumberFormatException | NullPointerException e) {
                            // a damaged line is skipped
                        }
                    }
                }
                entries.sort((x, y) -> Long.compare(y.score(), x.score()));
            }

            public List<Entry> getEntries() {
                return java.util.Collections.unmodifiableList(entries);
            }

            /** True when a run with this score gets on the board. */
            public boolean qualifies(long score) {
                return score > 0 && (entries.size() < SCOREBOARD_SIZE || score > entries.get(entries.size() - 1).score());
            }

            /** Adds a run, keeps the board sorted and its size, saves it, and returns the run's place (from 0), or -1 if it didn't make it. */
            public int add(String name, long score, int levelReached) {
                if (!qualifies(score)) {
                    return -1;
                }
                int place = 0;
                while (place < entries.size() && entries.get(place).score() >= score) {
                    place++; // a tie goes below the run that got there first, like the arcade
                }
                entries.add(place, new Entry(name, score, levelReached));
                while (entries.size() > SCOREBOARD_SIZE) {
                    entries.remove(entries.size() - 1);
                }
                for (int i = 0; i < entries.size(); i++) {
                    store.put("name" + i, entries.get(i).name());
                    store.put("score" + i, String.valueOf(entries.get(i).score()));
                    store.put("level" + i, String.valueOf(entries.get(i).levelReached()));
                }
                store.save();
                return place;
            }
        }

        /** A falling box an enemy dropped: power, score, or a bomb. */
        public static final class Item {
            private final ItemType type;
            private double x;
            private double y;
            private double vy;
            /** True once it's flying to the ship. */
            private boolean homing = false;
            /** True when it's collected from above the collect line, so a score box is worth the most. */
            private boolean fullValue = false;

            Item(ItemType type, double x, double y) {
                this.type = type;
                this.x = x;
                this.y = y;
            }

            public ItemType getType() {
                return type;
            }

            public double getX() {
                return x;
            }

            public double getY() {
                return y;
            }
        }

        /** Fires one shot of one of the enemy's patterns (its phase at that index) from where it is. */
        private void fire(Enemy enemy, int layer) {
            ShotPhase design = enemy.getDesign().getPhases().get(layer);
            int count = design.getBulletCount();
            double speed = design.getBulletSpeed();
            double spread = Math.toRadians(design.getSpread());
            // Bullets come from the enemy, or from the pattern's own spot on the screen
            double fromX = design.hasFromSpot() ? design.getFromX() : enemy.getX();
            double fromY = design.hasFromSpot() ? design.getFromY() : enemy.getY();
            double aim = aimAngle(enemy, design, layer, fromX, fromY);
            switch (design.getPattern()) {
                case AIMED -> {
                    // A line of bullets: same direction, each a little faster than the one before
                    for (int i = 0; i < count; i++) {
                        addEnemyBullet(fromX, fromY, design, aim, speed * (1 + 0.15 * i));
                    }
                }
                case FAN -> {
                    for (int i = 0; i < count; i++) {
                        double offset = count == 1 ? 0 : -spread / 2 + spread * i / (count - 1);
                        addEnemyBullet(fromX, fromY, design, aim + offset, speed);
                    }
                }
                case RING, SPIRAL -> {
                    // Starts at the aim (straight down unless changed); a spiral's start turns a bit more after every shot
                    double start = aim + Math.toRadians(enemy.getSpinAngle(layer));
                    for (int i = 0; i < count; i++) {
                        addEnemyBullet(fromX, fromY, design, start + 2 * Math.PI * i / count, speed);
                    }
                    if (design.getPattern() == Pattern.SPIRAL) {
                        enemy.addSpin(layer, design.getSpin());
                    }
                }
                case RANDOM -> {
                    for (int i = 0; i < count; i++) {
                        double offset = (random.nextDouble() - 0.5) * spread;
                        addEnemyBullet(fromX, fromY, design, aim + offset, speed * (0.7 + random.nextDouble() * 0.6));
                    }
                }
                case LASER -> {
                    // The beams keep following the sweep, so they drag across the screen as they last
                    double sweepNow = Math.toRadians(design.sweepAngle(enemy.getPhaseTime()));
                    boolean allAround = spread >= 2 * Math.PI - 1e-9;
                    for (int i = 0; i < count; i++) {
                        double offset = count == 1 ? 0
                                : allAround ? 2 * Math.PI * i / count
                                : -spread / 2 + spread * i / (count - 1);
                        lasers.add(new Laser(enemy, aim - sweepNow + offset, design, enemy.getPhaseTime()));
                    }
                }
                case WALL -> {
                    // A line across the direction it flies, centred on the enemy
                    double width = design.getWallWidth();
                    double acrossX = -Math.sin(aim);
                    double acrossY = Math.cos(aim);
                    for (int i = 0; i < count; i++) {
                        double along = count == 1 ? 0 : -width / 2 + width * i / (count - 1);
                        addEnemyBullet(fromX + acrossX * along, fromY + acrossY * along, aim, speed, design);
                        if (design.getWave() > 0 && !enemyBullets.isEmpty()) {
                            // A wavy wall bends like a snake: each bullet a little further along the wave
                            enemyBullets.get(enemyBullets.size() - 1).wavePhase = 2 * Math.PI * i / count;
                        }
                    }
                }
                case NONE -> { }
            }
        }

        private void addEnemyBullet(double x, double y, ShotPhase phase, double angle, double speed) {
            addEnemyBullet(x, y, angle, speed, phase);
        }

        private void addEnemyBullet(double x, double y, double angle, double speed, ShotPhase phase) {
            if (enemyBullets.size() >= MAX_ENEMY_BULLETS) {
                return;
            }
            enemyBullets.add(new EnemyBullet(x, y, speed, angle, phase));
        }

        /** The direction the enemy shoots in right now, in radians: its phase's aim, turned by the sweep. */
        private double aimAngle(Enemy enemy, ShotPhase phase, int layer, double fromX, double fromY) {
            double base = switch (phase.getAim()) {
                case PLAYER -> Math.atan2(playerY - fromY, playerX - fromX);
                case ONCE -> enemy.lockAim(layer, Math.atan2(playerY - fromY, playerX - fromX));
                case FIXED -> Math.toRadians(phase.getDirection());
            };
            return base + Math.toRadians(phase.sweepAngle(enemy.getPhaseTime()));
        }

        /** Moves the enemy bullets, drops the ones that left the screen, and checks them against the hitbox. */
        private void moveEnemyBullets(double deltaSeconds) {
            invincibleTimeLeft = Math.max(0, invincibleTimeLeft - deltaSeconds);
            List<EnemyBullet> splits = new ArrayList<>();
            for (EnemyBullet bullet : enemyBullets) {
                bullet.move(deltaSeconds, playerX, playerY, splits);
            }
            for (EnemyBullet piece : splits) {
                if (enemyBullets.size() < MAX_ENEMY_BULLETS) {
                    enemyBullets.add(piece);
                }
            }
            for (Laser laser : lasers) {
                laser.update(deltaSeconds);
            }
            lasers.removeIf(Laser::isOver);
            // A bullet that curves can come back, so it gets a margin before it counts as gone
            double margin = BULLET_RADIUS + 40;
            for (EnemyBullet b : enemyBullets) {
                if (b.getX() >= -margin && b.getX() <= PLAYFIELD_WIDTH + margin && b.getY() >= -margin && b.getY() <= PLAYFIELD_HEIGHT + margin) {
                    b.entered = true;
                }
            }
            // One fired from off the screen (like the end of a wall fired from an edge) gets a few seconds to fly in
            enemyBullets.removeIf(b -> (b.entered || b.getAge() > OFFSCREEN_GRACE)
                    && (b.getX() < -margin || b.getX() > PLAYFIELD_WIDTH + margin
                    || b.getY() < -margin || b.getY() > PLAYFIELD_HEIGHT + margin) || b.getAge() > MAX_BULLET_AGE);
            if (invincibleTimeLeft > 0) {
                return;
            }
            // Grazing: the first time a bullet comes inside the graze zone it scores (each one once)
            for (EnemyBullet bullet : enemyBullets) {
                if (!bullet.grazed && Math.hypot(bullet.getX() - playerX, bullet.getY() - playerY) < bullet.getRadius() + GRAZE_RADIUS) {
                    bullet.grazed = true;
                    grazes++;
                    addScore(GRAZE_SCORE);
                }
            }
            for (Laser laser : lasers) {
                if (!laser.grazed && laser.touches(playerX, playerY, GRAZE_RADIUS)) {
                    laser.grazed = true;
                    grazes++;
                    addScore(LASER_GRAZE_SCORE);
                }
            }
            for (EnemyBullet bullet : enemyBullets) {
                // Only the tiny hitbox counts, so bullets can graze the ship drawing
                if (Math.hypot(bullet.getX() - playerX, bullet.getY() - playerY) < bullet.getRadius() + HITBOX_RADIUS) {
                    enemyBullets.remove(bullet);
                    loseLife();
                    return;
                }
            }
            for (Laser laser : lasers) {
                if (laser.touches(playerX, playerY, HITBOX_RADIUS)) {
                    loseLife();
                    return;
                }
            }
            if (ENEMIES_KILL_ON_TOUCH) {
                // Flying into an enemy's body is a hit too (not while it's still fading in)
                for (Enemy enemy : enemies) {
                    double scale = enemy.getDesign().getScale();
                    if (!enemy.isAppearing()
                            && Math.abs(enemy.getX() - playerX) < Alien.WIDTH * scale * ENEMY_TOUCH_SIZE / 2 + HITBOX_RADIUS
                            && Math.abs(enemy.getY() - playerY) < Alien.HEIGHT * scale * ENEMY_TOUCH_SIZE / 2 + HITBOX_RADIUS) {
                        loseLife();
                        return;
                    }
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
                    if (waves.get(k).isDropsBomb()) {
                        dropBombFromTop();
                    }
                }
            }
            for (int k = firstWave; k < waves.size(); k++) {
                if (isWaveStarted(k)) {
                    continue;
                }
                if (k == heldWave) {
                    break; // held back for now; it starts as soon as it's let go
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
                if (design.getWave() == wave && spawned.getOrDefault(design, 0) < totalToSend(design)) {
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

        /**
         * Spawns every enemy whose time has come. A group's spawn time counts from the start of its wave,
         * or, for a group that follows another, from the moment that group was cleared.
         */
        private void spawnDueEnemies() {
            for (EnemyDesign design : layout) {
                int wave = design.getWave();
                if (!isWaveStarted(wave) || groupClearTimes.containsKey(design)) {
                    continue;
                }
                if (spawned.getOrDefault(design, 0) >= totalToSend(design)
                        && enemies.stream().noneMatch(enemy -> enemy.getDesign() == design)) {
                    groupClearTimes.put(design, levelTime);
                }
            }
            for (EnemyDesign design : layout) {
                int wave = design.getWave();
                if (!isWaveStarted(wave)) {
                    continue;
                }
                double startTime = waveStartTimes.get(wave);
                EnemyDesign leader = design.getFollows();
                if (leader != null) {
                    // It counts from when the group it waits for is cleared, or (if set) when it first spawns
                    Map<EnemyDesign, Double> times = design.isFollowsSpawn() ? groupSpawnTimes : groupClearTimes;
                    if (!times.containsKey(leader)) {
                        continue; // still waiting for the group it follows
                    }
                    startTime = times.get(leader);
                }
                double timeInWave = levelTime - startTime;
                int count = spawned.getOrDefault(design, 0);
                int total = totalToSend(design);
                boolean timed = !design.isRepeatAfterGone() && design.getUntil() == null;
                while (count < total) {
                    if (timed) {
                        // Every batch on the wave's timer
                        if (timeInWave < design.spawnTimeOf(count)) {
                            break;
                        }
                    } else {
                        int perBatch = design.getCount();
                        int first = count / perBatch * perBatch; // the first enemy of this batch
                        if (count > 0 && !batchStartTimes.containsKey(design)) {
                            // Switched to this while the run was going: carry on from where its timer had the last batch
                            int lastFirst = (count - 1) / perBatch * perBatch;
                            batchStartTimes.put(design, startTime + design.spawnTimeOf(lastFirst));
                        }
                        if (count == first) {
                            if (count == 0) {
                                if (timeInWave < design.spawnTimeOf(0)) {
                                    break;
                                }
                                batchStartTimes.put(design, startTime + design.spawnTimeOf(0));
                                batchHistory.computeIfAbsent(design, d -> new ArrayList<>()).add(startTime + design.spawnTimeOf(0));
                            } else {
                                double sinceLast = levelTime - batchStartTimes.get(design);
                                EnemyDesign pairedWith = linkedLeader(design);
                                if (pairedWith != null) {
                                    // Linked to the group it follows: each batch comes when that group's batch does
                                    List<Double> leaderBatches = batchHistory.getOrDefault(pairedWith, List.of());
                                    int batch = count / perBatch;
                                    if (leaderBatches.size() <= batch || levelTime < leaderBatches.get(batch) + design.spawnTimeOf(0)) {
                                        break;
                                    }
                                } else if (design.isRepeatAfterGone()) {
                                    // The next batch waits until the last one (and any group linked to it) is shot down or has flown off, then the gap
                                    List<EnemyDesign> linked = linkedGroups(design);
                                    if (enemies.stream().anyMatch(enemy -> linked.contains(enemy.getDesign()))) {
                                        batchGoneTimes.remove(design);
                                        break;
                                    }
                                    batchGoneTimes.putIfAbsent(design, levelTime);
                                    if (levelTime < batchGoneTimes.get(design) + design.getRepeatGap()) {
                                        break;
                                    }
                                } else if (sinceLast < design.spawnTimeOf(first) - design.spawnTimeOf(first - perBatch)) {
                                    break; // on the timer, a gap after the batch before
                                }
                                batchGoneTimes.remove(design);
                                batchStartTimes.put(design, levelTime);
                                batchHistory.computeIfAbsent(design, d -> new ArrayList<>()).add(levelTime);
                            }
                        } else if (levelTime < batchStartTimes.get(design) + design.spawnTimeOf(count) - design.spawnTimeOf(first)) {
                            break; // the rest of a line follow on
                        }
                    }
                    groupSpawnTimes.putIfAbsent(design, levelTime);
                    enemies.add(new Enemy(design, count % design.getCount())); // its place within its batch
                    count++;
                }
                spawned.put(design, count);
            }
        }

        /**
         * The group this one is linked to: both send their next batch only when both are gone. That's
         * a group set to "when gone" that follows another "when gone" group from when it spawns.
         */
        private static EnemyDesign linkedLeader(EnemyDesign design) {
            EnemyDesign leader = design.getFollows();
            if (leader != null && design.isFollowsSpawn() && design.isRepeatAfterGone() && leader.isRepeatAfterGone()) {
                return leader;
            }
            return null;
        }

        /** The first group of a chain of linked groups (see {@link #linkedLeader}). */
        private static EnemyDesign linkedRoot(EnemyDesign design) {
            for (int i = 0; i < 100 && linkedLeader(design) != null; i++) {
                design = linkedLeader(design);
            }
            return design;
        }

        /** The group and every group linked to it, directly or through others. */
        private List<EnemyDesign> linkedGroups(EnemyDesign root) {
            List<EnemyDesign> groups = new ArrayList<>(List.of(root));
            for (int i = 0; i < groups.size(); i++) {
                for (EnemyDesign d : layout) {
                    if (linkedLeader(d) == groups.get(i) && !groups.contains(d)) {
                        groups.add(d);
                    }
                }
            }
            return groups;
        }

        /**
         * How many enemies the group sends this run. One that keeps coming until another group is gone
         * sends batch after batch until then, and finishes the batch it's on.
         */
        private int totalToSend(EnemyDesign design) {
            EnemyDesign until = design.getUntil();
            if (until == null) {
                return design.getTotalCount();
            }
            for (EnemyDesign group : linkedGroups(linkedRoot(until))) {
                if (!groupClearTimes.containsKey(group)) {
                    return Integer.MAX_VALUE; // that group, or one linked to it, is still going
                }
            }
            int sent = spawned.getOrDefault(design, 0);
            int perBatch = design.getCount();
            return (sent + perBatch - 1) / perBatch * perBatch;
        }

        public boolean isWaveStarted(int wave) {
            return wave < waveStartTimes.size() && waveStartTimes.get(wave) >= 0;
        }

        public boolean isWaveCleared(int wave) {
            return wave < waveClearTimes.size() && waveClearTimes.get(wave) >= 0;
        }

        /** The level time when the wave started this run, or -1 if it hasn't. */
        public double getWaveStartTime(int wave) {
            return isWaveStarted(wave) ? waveStartTimes.get(wave) : -1;
        }

        /** The level time when the wave was cleared this run, or -1 if it hasn't been. */
        public double getWaveClearTime(int wave) {
            return isWaveCleared(wave) ? waveClearTimes.get(wave) : -1;
        }

        /** Each time the screen was cleared of tanks this run (all shot down or flown off), in level time, in order. */
        public List<Double> getTanksGoneTimes() {
            return tanksGoneTimes;
        }

        /** Keeps a wave from starting until this is set back to -1 (or to another wave). */
        public void setHeldWave(int wave) {
            heldWave = wave;
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
            if (beamTimeLeft > 0) {
                speed *= BEAM_SLOWDOWN; // firing the beam holds the ship back
            }
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
        /** Every enemy bullet and laser on screen vanishes, as when a boss attack ends. */
        public void clearEnemyBullets() {
            enemyBullets.clear();
            lasers.clear();
        }

        /** The boss flying now (the first one, if there are several), or null. */
        public Enemy getBoss() {
            for (Enemy enemy : enemies) {
                if (enemy.getDesign().isBoss()) {
                    return enemy;
                }
            }
            return null;
        }

        private boolean hitEnemy(double[] shot) {
            for (Enemy enemy : enemies) {
                double scale = enemy.getDesign().getScale();
                if (!enemy.isAppearing()
                        && Math.abs(shot[0] - enemy.getX()) < (Alien.WIDTH * scale + SHOT_WIDTH) / 2
                        && Math.abs(shot[1] - enemy.getY()) < (Alien.HEIGHT * scale + SHOT_HEIGHT) / 2) {
                    // A tank takes several hits; it flashes on each one and is destroyed when its health runs out
                    boolean rocket = shot.length > 4 && shot[4] == 1;
                    boolean destroyed = enemy.takeHit(rocket ? ROCKET_DAMAGE : 1);
                    if (enemy.getDesign().isBoss()) {
                        afterBossHit(enemy, destroyed); // a beaten attack drops its items and clears the bullets
                    } else if (destroyed) {
                        enemies.remove(enemy);
                        enemyDestroyed(enemy);
                        if (rocket) {
                            flakExplosion(enemy.getX(), enemy.getY());
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Ready-made shot patterns to start from: a name, then the shot settings as level text. Picked
         * in the editor, they replace the selected pattern's shot (see {@link ShotPhase#applyPattern}).
         */
        public static final String[][] QUICK_PATTERNS = {
                {"Water droplets: flung up, hang, then fall at you",
                        "shot=RANDOM bullets=14 spread=220 aim=FIXED direction=270 bulletSpeed=260 every=0.70 size=6 color=BLUE accel=-300 steps=0.85/0/0/0/0/no/0;1.25/150/0/0/0/aim/0"},
                {"Splash ring: slows down, then speeds up",
                        "shot=RING bullets=20 aim=PLAYER bulletSpeed=170 every=1.00 size=5 color=WHITE accel=-200 steps=0.70/50/120/0/0/no/0"},
                {"Squiggly stream, sweeping",
                        "shot=AIMED bullets=1 aim=FIXED direction=90 sweep=110 swing=150 bulletSpeed=180 every=0.06 size=4 color=BLUE curve=130 steps=0.15/180/0/-130/0/no/0;0.45/180/0/130/0/no/0;0.75/180/0/-130/0/no/0;1.05/180/0/130/0/no/0;1.35/180/0/-130/0/no/0;1.65/180/0/130/0/no/0;1.95/180/0/-130/0/no/0;2.25/180/0/130/0/no/0"},
                {"Squiggly stream, sweeping the other way",
                        "shot=AIMED bullets=1 aim=FIXED direction=90 sweep=-110 swing=150 bulletSpeed=180 every=0.06 size=4 color=PURPLE curve=-130 steps=0.15/180/0/130/0/no/0;0.45/180/0/-130/0/no/0;0.75/180/0/130/0/no/0;1.05/180/0/-130/0/no/0;1.35/180/0/130/0/no/0;1.65/180/0/-130/0/no/0;1.95/180/0/130/0/no/0;2.25/180/0/-130/0/no/0"},
                {"Wiper stream (straight bullets)",
                        "shot=AIMED bullets=1 aim=FIXED direction=90 sweep=90 swing=140 bulletSpeed=200 every=0.06 size=4 color=WHITE"},
                {"Ring pulse at you",
                        "shot=RING bullets=16 aim=PLAYER bulletSpeed=130 every=1.00 size=4 color=BLUE"},
                {"Spiral",
                        "shot=SPIRAL bullets=4 aim=FIXED direction=90 bulletSpeed=120 every=0.10 spin=11 size=4 color=BLUE"},
                {"Aimed burst (3 in a row)",
                        "shot=AIMED bullets=3 aim=PLAYER bulletSpeed=200 every=0.80 size=4 color=RED"},
                {"Rain from above",
                        "shot=RANDOM bullets=3 spread=150 aim=FIXED direction=90 bulletSpeed=120 every=0.15 size=3 color=BLUE"},
                {"Sweeping wall",
                        "shot=WALL bullets=13 width=260 aim=FIXED direction=90 sweep=50 swing=80 bulletSpeed=90 every=1.20 size=4 color=PURPLE"},
                {"Bouncing ring",
                        "shot=RING bullets=20 aim=FIXED direction=90 bulletSpeed=150 every=1.50 size=4 color=WHITE bounces=1"},
        };

        /**
         * Ready-made groups to start a wave from: a name, then enemy lines in the level text format.
         * Picked in the editor, they're added to the wave being edited and can then be changed.
         */
        public static final String[][] QUICK_WAVES = {
                {"Line from the left",
                        "SQUID start=-28,80 speed=160 count=5 spawn=0 first=0.5 path=200,80 360,200 588,200"},
                {"Line from the right",
                        "SQUID start=588,80 speed=160 count=5 spawn=0 first=0.5 path=360,80 200,200 -28,200"},
                {"Pincer (both sides at once)",
                        "SQUID start=-28,80 speed=160 count=5 spawn=0 first=0.5 path=200,80 360,200 588,200\n"
                        + "SQUID start=588,80 speed=160 count=5 spawn=0 first=0.5 path=360,80 200,200 -28,200"},
                {"Drop in, stop and shoot",
                        "CRAB start=140,-28 speed=140 count=1 spawn=0 first=0.2 fire=stopped path=140,110@3 140,-28\n"
                        + "CRAB start=280,-28 speed=140 count=1 spawn=0.4 first=0.2 fire=stopped path=280,110@3 280,-28\n"
                        + "CRAB start=420,-28 speed=140 count=1 spawn=0.8 first=0.2 fire=stopped path=420,110@3 420,-28"},
                {"Water midboss (example): droplets left and right, squiggly sweeps in the middle",
                        "OCTOPUS start=280,-40 speed=150 boss=yes spawn=0 first=0 lasts=30 cardhp=250 move=PATH movespeed=170 moveloop=bounce movepath=100,100@1.8;280,120@3.6;460,100@1.8 "
                        + QUICK_PATTERNS[0][1] + " atpoint=1,3 volleys=2 path=280,110\n"
                        + "  PHASE together=yes " + QUICK_PATTERNS[1][1] + " atpoint=1,3 volleys=2 every=0.70\n"
                        + "  PHASE together=yes " + QUICK_PATTERNS[2][1] + " atpoint=2\n"
                        + "  PHASE together=yes " + QUICK_PATTERNS[3][1] + " atpoint=2\n"
                        + "  PHASE together=yes " + QUICK_PATTERNS[5][1] + " atpoint=2 volleys=3"},
                {"Undulating waves (boss example): wavy walls sweep in from every side while the boss fires rings",
                        "OCTOPUS start=280,-40 speed=150 boss=yes spawn=0 first=0 lasts=30 cardhp=300 move=STAY name=Undulating_Waves "
                        + "shot=RING bullets=14 aim=PLAYER bulletSpeed=110 every=1.60 size=5 color=PINK path=280,100\n"
                        + "  PHASE together=yes shot=WALL bullets=13 width=460 aim=FIXED direction=0 bulletSpeed=80 every=4.00 size=5 color=BLUE wave=26 waverate=0.6 from=0,280\n"
                        + "  PHASE together=yes delay=1.0 shot=WALL bullets=13 width=460 aim=FIXED direction=90 bulletSpeed=80 every=4.00 size=5 color=BLUE wave=26 waverate=0.6 from=280,0\n"
                        + "  PHASE together=yes delay=2.0 shot=WALL bullets=13 width=460 aim=FIXED direction=180 bulletSpeed=80 every=4.00 size=5 color=BLUE wave=26 waverate=0.6 from=560,280\n"
                        + "  PHASE together=yes delay=3.0 shot=WALL bullets=13 width=460 aim=FIXED direction=270 bulletSpeed=80 every=4.00 size=5 color=BLUE wave=26 waverate=0.6 from=280,560"},
                {"V formation",
                        "SQUID start=160,-28 speed=150 count=1 spawn=0 first=0.3 fire=stopped path=160,80@2.5 -28,80\n"
                        + "SQUID start=220,-28 speed=150 count=1 spawn=0 first=0.3 fire=stopped path=220,110@2.5 -28,110\n"
                        + "SQUID start=280,-28 speed=150 count=1 spawn=0 first=0.3 fire=stopped path=280,140@2.5 280,-28\n"
                        + "SQUID start=340,-28 speed=150 count=1 spawn=0 first=0.3 fire=stopped path=340,110@2.5 588,110\n"
                        + "SQUID start=400,-28 speed=150 count=1 spawn=0 first=0.3 fire=stopped path=400,80@2.5 588,80"},
                {"Row moving down the screen",
                        "SQUID start=280,-28 speed=60 count=7 formation=ROW spacing=55 spawn=0 first=1 path=280,588"},
                {"Block sliding across",
                        "CRAB start=-60,120 speed=70 count=9 formation=GRID spacing=36 spawn=0 first=1 path=620,120"},
                {"Big one in the middle",
                        "OCTOPUS start=280,110@8 speed=120 count=1 spawn=0 first=0.5 path=280,-28"},
        };

        /**
         * Adds quick wave number index (see {@link #QUICK_WAVES}) to the given wave and returns its
         * groups. If the wave already has groups, it spawns 2 s after the last of them, so it doesn't land on top.
         */
        public List<EnemyDesign> addQuickWave(int index, int wave) {
            double later = 0;
            boolean waveHasGroups = false;
            for (EnemyDesign d : layout) {
                if (d.getWave() == wave) {
                    waveHasGroups = true;
                    later = Math.max(later, d.getSpawnTime() + 2);
                }
            }
            List<EnemyDesign> added = new ArrayList<>();
            for (String line : QUICK_WAVES[index][1].split("\n")) {
                if (line.strip().toUpperCase().startsWith("PHASE")) {
                    // The next pattern (or layer) of the group above
                    ShotPhase phase = added.get(added.size() - 1).addPhase();
                    parsePhaseSettings(readSettings(line.strip().substring("PHASE".length()).strip()), phase);
                    continue;
                }
                EnemyDesign design = parseEnemyLine(line);
                design.setWave(wave);
                if (waveHasGroups) {
                    design.setSpawnTime(design.getSpawnTime() + later);
                }
                layout.add(design);
                added.add(design);
            }
            return added;
        }

        /**
         * Adds a copy of a group (path, shots, phases and all) right after it, spawning 2 s later, so
         * a second batch flies the same path. Returns the copy.
         */
        public EnemyDesign duplicateGroup(EnemyDesign design) {
            EnemyDesign copy = copyGroup(design, false);
            copy.setSpawnTime(design.getSpawnTime() + 2);
            return copy;
        }

        /**
         * Adds a copy of a group flipped left to right, right after it and spawning at the same time:
         * its path, fixed directions, sweeps, spins and curves all turn the other way. Returns the copy.
         */
        public EnemyDesign mirrorGroup(EnemyDesign design) {
            EnemyDesign copy = copyGroup(design, true);
            // The leaning shapes lean the other way
            if (copy.getFormation() == Formation.DIAGONAL_DOWN) {
                copy.setFormation(Formation.DIAGONAL_UP);
            } else if (copy.getFormation() == Formation.DIAGONAL_UP) {
                copy.setFormation(Formation.DIAGONAL_DOWN);
            }
            for (ShotPhase phase : copy.getPhases()) {
                phase.setDirection(180 - phase.getDirection());
                phase.setSweep(-phase.getSweep());
                phase.setSpin(-phase.getSpin());
                phase.setCurve(-phase.getCurve());
                for (double[] point : phase.getMovePath()) {
                    point[0] = PLAYFIELD_WIDTH - point[0];
                }
                for (BulletStep step : phase.getSteps()) {
                    step.setTurn(-step.getTurn());
                    step.setCurve(-step.getCurve());
                }
            }
            return copy;
        }

        /** Copies a group through its level text (mirrored or not) and puts the copy right after it. */
        private EnemyDesign copyGroup(EnemyDesign design, boolean mirrored) {
            StringBuilder text = new StringBuilder();
            appendEnemyLine(text, design, mirrored);
            String[] lines = text.toString().split("\n");
            EnemyDesign copy = parseEnemyLine(lines[0]);
            for (int i = 1; i < lines.length; i++) {
                ShotPhase phase = copy.addPhase();
                parsePhaseSettings(readSettings(lines[i].strip().substring("PHASE".length()).strip()), phase);
            }
            copy.setWave(design.getWave());
            if (design.getFollows() != null && copy.canFollow(design.getFollows())) {
                copy.setFollows(design.getFollows());
            }
            if (design.getUntil() != null && copy.canKeepComingUntil(design.getUntil())) {
                copy.setUntil(design.getUntil());
            }
            layout.add(layout.indexOf(design) + 1, copy);
            return copy;
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

        /**
         * Every placed enemy of the given wave whose start marker is within PICK_RADIUS of (x, y),
         * nearest first (groups at the same spot, like a duplicate, stay in level order).
         */
        public List<EnemyDesign> findEnemiesNear(double x, double y, int wave) {
            List<EnemyDesign> near = new ArrayList<>();
            for (EnemyDesign design : layout) {
                if (design.getWave() == wave
                        && Math.hypot(design.getMarkerX() - x, design.getMarkerY() - y) <= PICK_RADIUS) {
                    near.add(design);
                }
            }
            near.sort(java.util.Comparator.comparingDouble(d -> Math.hypot(d.getMarkerX() - x, d.getMarkerY() - y)));
            return near;
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
            for (EnemyDesign other : layout) {
                if (other.getFollows() == design) {
                    other.setFollows(null); // goes back to counting from the wave's start
                }
                if (other.getUntil() == design) {
                    other.setUntil(null);
                }
            }
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
                text.append(String.format(Locale.ROOT, "WAVE %d after=%s wait=%.1f%s%n", k + 1,
                        wave.isAfterCleared() ? "cleared" : "started", wave.getWait(), wave.isDropsBomb() ? " bomb=yes" : ""));
                for (EnemyDesign d : layout) {
                    if (d.getWave() == k) {
                        appendEnemyLine(text, d);
                    }
                }
            }
            return text.toString();
        }

        /**
         * Adds a copied level's waves after this level's last wave that has enemies (an empty wave at
         * the end, like one just made with + New wave, is filled by the first pasted wave). Pasted
         * wave 1 becomes the next wave, and so on. Text with no WAVE line counts as one wave.
         * Throws IllegalArgumentException if the text can't be read; the level is left as it was.
         */
        public void addLevelAtEnd(String text) {
            int used = 0;
            for (EnemyDesign d : layout) {
                used = Math.max(used, d.getWave() + 1);
            }
            StringBuilder all = new StringBuilder();
            for (String line : saveLevel().split("\\R")) {
                String[] words = line.strip().split("\\s+");
                if (words[0].equalsIgnoreCase("WAVE") && words.length > 1 && Integer.parseInt(words[1]) > used) {
                    break;
                }
                all.append(line).append('\n');
            }
            boolean inWave = false;
            for (String raw : text.split("\\R")) {
                String line = raw.strip();
                if (line.toUpperCase().startsWith("WAVE")) {
                    String[] words = line.split("\\s+", 3);
                    if (words.length < 2) {
                        throw new IllegalArgumentException("WAVE needs a number, like WAVE 2");
                    }
                    line = "WAVE " + (Integer.parseInt(words[1]) + used) + (words.length > 2 ? " " + words[2] : "");
                    inWave = true;
                } else if (!inWave && !line.isEmpty() && !line.startsWith("#")) {
                    all.append("WAVE ").append(used + 1).append('\n');
                    inWave = true;
                }
                all.append(line).append('\n');
            }
            if (!inWave) {
                throw new IllegalArgumentException("there are no enemies in the pasted text");
            }
            loadLevel(all.toString());
        }

        /** The group's number in its wave, counting the wave's groups in order from 1. */
        public static int groupNumber(List<EnemyDesign> layout, EnemyDesign group) {
            int number = 0;
            for (EnemyDesign d : layout) {
                if (d.getWave() == group.getWave()) {
                    number++;
                    if (d == group) {
                        return number;
                    }
                }
            }
            return -1;
        }

        /** The group's number in its wave (see {@link #groupNumber(List, EnemyDesign)}). */
        public int groupNumber(EnemyDesign group) {
            return groupNumber(layout, group);
        }

        private void appendEnemyLine(StringBuilder text, EnemyDesign d) {
            appendEnemyLine(text, d, false);
        }

        /** The enemy's line (and PHASE lines) as level text; mirrored swaps left and right. */
        private void appendEnemyLine(StringBuilder text, EnemyDesign d, boolean mirrored) {
                text.append(String.format(Locale.ROOT, "%s start=%s speed=%d count=%d repeats=%d gap=%.1f hp=%d formation=%s spacing=%d face=%s spawn=%.1f loop=%s first=%.1f fire=%s ",
                        d.getType(), pointText(mirrored ? PLAYFIELD_WIDTH - d.getStartX() : d.getStartX(), d.getStartY(), d.getStopTime(0)), Math.round(d.getSpeed()),
                        d.getCount(), d.getRepeats(), d.getRepeatGap(), d.getHealth(), d.getFormation(), Math.round(d.getSpacing()), d.isFacesMovement() ? "yes" : "no", d.getSpawnTime(), d.isLoop() ? "yes" : "no", d.getFirstShotDelay(),
                        d.isFireOnlyWhenStopped() ? "stopped" : "always"));
                if (d.getPointing() != Pointing.UPRIGHT) {
                    text.append("point=").append(d.getPointing() == Pointing.HEAD_FIRST ? "head" : "feet").append(' ');
                }
                if (d.getFollows() != null) {
                    text.append("follows=").append(groupNumber(layout, d.getFollows()))
                            .append(d.isFollowsSpawn() ? " when=spawned " : " when=cleared ");
                }
                if (d.isRepeatAfterGone()) {
                    text.append("next=gone ");
                }
                if (d.getUntil() != null) {
                    text.append("until=").append(groupNumber(layout, d.getUntil())).append(' ');
                }
                if (d.isBoss()) {
                    text.append("boss=yes ");
                }
                text.append(phaseText(d.getPhases().get(0), d.isBoss())).append(" path=");
                List<String> points = new ArrayList<>();
                for (int i = 1; i <= d.getWaypoints().size(); i++) {
                    double[] point = d.getWaypoints().get(i - 1);
                    points.add(pointText(mirrored ? PLAYFIELD_WIDTH - point[0] : point[0], point[1], d.getStopTime(i)));
                }
                text.append(String.join(" ", points)).append("\n");
                for (int i = 1; i < d.getPhases().size(); i++) {
                    text.append("  PHASE ").append(phaseText(d.getPhases().get(i), d.isBoss())).append("\n");
                }
        }

        /** A shot phase's settings as level text, with its boss attack settings too when it's a boss's. */
        private static String phaseText(ShotPhase p, boolean boss) {
            String shots = phaseText(p);
            if (p.getShape() != BulletShape.ORB) {
                shots += " shape=" + p.getShape();
            }
            if (p.getPicture() != null) {
                shots += " pic=" + p.getPicture().name;
                if (p.getPictureSize() != 1) {
                    shots += " picsize=" + Math.round(p.getPictureSize() * 100);
                }
            }
            if (p.getWave() > 0) {
                shots += String.format(Locale.ROOT, " wave=%d waverate=%.1f", Math.round(p.getWave()), p.getWaveRate());
            }
            if (p.hasFromSpot()) {
                shots += String.format(Locale.ROOT, " from=%d,%d", Math.round(p.getFromX()), Math.round(p.getFromY()));
            }
            if (p.isTogether()) {
                shots += " together=yes";
            }
            if (p.getDelay() > 0) {
                shots += String.format(Locale.ROOT, " delay=%.1f", p.getDelay());
            }
            if (!p.getAtPoints().isEmpty()) {
                List<String> places = new ArrayList<>();
                for (int point : p.getAtPoints()) {
                    places.add(String.valueOf(point));
                }
                shots += " atpoint=" + String.join(",", places);
            }
            if (p.getVolleys() > 0) {
                shots += " volleys=" + p.getVolleys();
            }
            if (!boss) {
                return shots;
            }
            String card = String.format(Locale.ROOT, " cardhp=%d move=%s hop=%.1f", p.getCardHealth(), p.getMove(), p.getHop());
            if (!p.getMovePath().isEmpty()) {
                List<String> points = new ArrayList<>();
                for (double[] point : p.getMovePath()) {
                    points.add(pointText(point[0], point[1], point[2]));
                }
                card += String.format(Locale.ROOT, " movespeed=%d moveloop=%s movepath=%s",
                        Math.round(p.getMoveSpeed()), p.isMoveBounce() ? "bounce" : p.isMoveLoop() ? "yes" : "no", String.join(";", points));
            }
            return p.getCardName().isEmpty() ? shots + card : shots + card + " name=" + p.getCardName().replace(' ', '_');
        }

        /**
         * One attack (shot phase) as a PHASE line of level text, so it can be copied and pasted into
         * another enemy or boss, or kept in a text file to reuse.
         */
        public static String attackText(ShotPhase p) {
            return "PHASE " + phaseText(p, true);
        }

        /**
         * Adds an attack copied with {@link #attackText} to the end of an enemy's phases and returns it.
         * Throws IllegalArgumentException if the text isn't an attack or the enemy is full.
         */
        public static ShotPhase pasteAttack(EnemyDesign design, String text) {
            String line = text == null ? "" : text.strip();
            if (line.toUpperCase().startsWith("PHASE")) {
                line = line.substring("PHASE".length()).strip();
            }
            if (line.isEmpty() || line.contains("\n") || !line.contains("shot=")) {
                throw new IllegalArgumentException("that isn't a copied attack");
            }
            if (design.getPhases().size() >= MAX_PHASES) {
                throw new IllegalArgumentException("it already has " + MAX_PHASES + " phases");
            }
            Map<String, String> settings = readSettings(line);
            ShotPhase phase = design.addPhase();
            try {
                parsePhaseSettings(settings, phase);
            } catch (RuntimeException e) {
                design.getPhases().remove(phase);
                throw new IllegalArgumentException("that isn't a copied attack (" + e.getMessage() + ")", e);
            }
            return phase;
        }

        /** A shot phase's shot settings as level text. */
        private static String phaseText(ShotPhase p) {
            List<String> steps = new ArrayList<>();
            for (BulletStep step : p.getSteps()) {
                steps.add(String.format(Locale.ROOT, "%.2f/%d/%d/%d/%d/%s/%d", step.getTime(), Math.round(step.getSpeed()),
                        Math.round(step.getAccel()), Math.round(step.getCurve()), Math.round(step.getTurn()),
                        step.isAim() ? "aim" : "no", step.getSplit()));
            }
            return String.format(Locale.ROOT,
                    "lasts=%.1f shot=%s bullets=%d spread=%d bulletSpeed=%d every=%.2f spin=%d size=%.0f color=%s bounces=%d"
                            + " aim=%s direction=%d sweep=%d swing=%d beam=%.1f width=%d accel=%d curve=%d steps=%s",
                    p.getDuration(), p.getPattern(), p.getBulletCount(), Math.round(p.getSpread()),
                    Math.round(p.getBulletSpeed()), p.getFireInterval(), Math.round(p.getSpin()), p.getBulletSize(),
                    p.getColor(), p.getBounces(), p.getAim(), Math.round(p.getDirection()), Math.round(p.getSweep()),
                    Math.round(p.getSwing()), p.getBeamTime(), Math.round(p.getWallWidth()),
                    Math.round(p.getAccel()), Math.round(p.getCurve()), String.join(";", steps));
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
            // follows=N is looked up once every line is read, since group N can come after it
            Map<EnemyDesign, Integer> followNumbers = new HashMap<>();
            Map<EnemyDesign, Integer> followLines = new HashMap<>();
            Map<EnemyDesign, Integer> untilNumbers = new HashMap<>();
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
                    } else if (line.toUpperCase().startsWith("PHASE")) {
                        if (loaded.isEmpty()) {
                            throw new IllegalArgumentException("a PHASE line has to come after an enemy line");
                        }
                        EnemyDesign design = loaded.get(loaded.size() - 1);
                        if (design.getPhases().size() >= MAX_PHASES) {
                            throw new IllegalArgumentException("an enemy can have at most " + MAX_PHASES + " phases");
                        }
                        ShotPhase phase = design.addPhase();
                        parsePhaseSettings(readSettings(line.substring("PHASE".length()).strip()), phase);
                    } else {
                        EnemyDesign design = parseEnemyLine(line);
                        design.setWave(currentWave);
                        loaded.add(design);
                        int pathAt = pathStart(line);
                        String settings = pathAt >= 0 ? line.substring(0, pathAt) : line;
                        String[] typeAndSettings = settings.strip().split("\\s+", 2);
                        Map<String, String> keys = readSettings(typeAndSettings.length > 1 ? typeAndSettings[1] : "");
                        String follows = keys.get("follows");
                        if (follows != null) {
                            followNumbers.put(design, Integer.parseInt(follows));
                            followLines.put(design, i + 1);
                        }
                        if (keys.get("until") != null) {
                            untilNumbers.put(design, Integer.parseInt(keys.get("until")));
                            followLines.put(design, i + 1);
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": expected a number but found "
                            + e.getMessage().replace("For input string: ", ""), e);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": " + e.getMessage(), e);
                }
            }
            for (Map.Entry<EnemyDesign, Integer> entry : followNumbers.entrySet()) {
                EnemyDesign design = entry.getKey();
                EnemyDesign leader = null;
                int number = 0;
                for (EnemyDesign d : loaded) {
                    if (d.getWave() == design.getWave() && ++number == entry.getValue()) {
                        leader = d;
                    }
                }
                if (leader == null || !design.canFollow(leader)) {
                    throw new IllegalArgumentException("Line " + followLines.get(design) + ": follows=" + entry.getValue()
                            + " doesn't name another group of the same wave (or the groups would wait for each other)");
                }
                design.setFollows(leader);
            }
            for (Map.Entry<EnemyDesign, Integer> entry : untilNumbers.entrySet()) {
                EnemyDesign design = entry.getKey();
                EnemyDesign other = null;
                int number = 0;
                for (EnemyDesign d : loaded) {
                    if (d.getWave() == design.getWave() && ++number == entry.getValue()) {
                        other = d;
                    }
                }
                if (other == null || !design.canKeepComingUntil(other)) {
                    throw new IllegalArgumentException("Line " + followLines.get(design) + ": until=" + entry.getValue()
                            + " doesn't name another group of the same wave (or that group waits for this one)");
                }
                design.setUntil(other);
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
                    case "bomb" -> wave.setDropsBomb(keyValue[1].equalsIgnoreCase("yes"));
                    default -> throw new IllegalArgumentException("unknown wave setting \"" + keyValue[0] + "\"");
                }
            }
            return number - 1;
        }

        /** Reads one line of level text into an enemy design. */
        private static EnemyDesign parseEnemyLine(String line) {
            // Everything after "path=" is the list of points; the rest is the type and key=value settings
            String path = "";
            int pathStart = pathStart(line);
            if (pathStart >= 0) {
                path = line.substring(pathStart + "path=".length()).strip();
                line = line.substring(0, pathStart).strip();
            }
            String[] words = line.split("\\s+", 2);
            Alien.Type type = Alien.Type.valueOf(words[0].toUpperCase());
            Map<String, String> settings = readSettings(words.length > 1 ? words[1] : "");
            if (!settings.containsKey("start")) {
                throw new IllegalArgumentException("missing start=x,y");
            }
            double[] start = parsePoint(settings.get("start"));
            EnemyDesign design = new EnemyDesign(type, start[0], start[1]);
            design.setStopTime(0, start[2]);
            if (settings.containsKey("speed")) design.setSpeed(Double.parseDouble(settings.get("speed")));
            if (settings.containsKey("count")) design.setCount(Integer.parseInt(settings.get("count")));
            if (settings.containsKey("when")) design.setFollowsSpawn(settings.get("when").equalsIgnoreCase("spawned"));
            if (settings.containsKey("repeats")) design.setRepeats(Integer.parseInt(settings.get("repeats")));
            if (settings.containsKey("gap")) design.setRepeatGap(Double.parseDouble(settings.get("gap")));
            if (settings.containsKey("next")) design.setRepeatAfterGone(settings.get("next").equalsIgnoreCase("gone"));
            if (settings.containsKey("hp")) design.setHealth(Integer.parseInt(settings.get("hp")));
            if (settings.containsKey("formation")) design.setFormation(Formation.valueOf(settings.get("formation").toUpperCase()));
            if (settings.containsKey("spacing")) design.setSpacing(Double.parseDouble(settings.get("spacing")));
            if (settings.containsKey("face")) design.setFacesMovement(settings.get("face").equalsIgnoreCase("yes"));
            if (settings.containsKey("point")) {
                String point = settings.get("point").toLowerCase();
                design.setPointing(point.equals("head") ? Pointing.HEAD_FIRST : point.equals("feet") ? Pointing.FEET_FIRST : Pointing.UPRIGHT);
            }
            if (settings.containsKey("spawn")) design.setSpawnTime(Double.parseDouble(settings.get("spawn")));
            if (settings.containsKey("loop")) design.setLoop(settings.get("loop").equalsIgnoreCase("yes"));
            if (settings.containsKey("boss")) design.setBoss(settings.get("boss").equalsIgnoreCase("yes"));
            parsePhaseSettings(settings, design.getPhases().get(0));
            // Levels saved before first= existed fire their first shot after half an interval, as they did then
            design.setFirstShotDelay(settings.containsKey("first")
                    ? Double.parseDouble(settings.get("first")) : design.getPhases().get(0).getFireInterval() / 2);
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

        /**
         * Where the enemy's own "path=" starts in a line, or -1. It has to start a word, so a boss
         * attack's "movepath=" isn't taken for it.
         */
        private static int pathStart(String line) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(^|\\s)path=").matcher(line);
            return m.find() ? m.start() + m.group(1).length() : -1;
        }

        /** Reads "key=value key=value ..." into a map. */
        private static Map<String, String> readSettings(String text) {
            Map<String, String> settings = new HashMap<>();
            if (text.isBlank()) {
                return settings;
            }
            for (String word : text.strip().split("\\s+")) {
                String[] keyValue = word.split("=", 2);
                if (keyValue.length != 2) {
                    throw new IllegalArgumentException("expected key=value but found \"" + word + "\"");
                }
                settings.put(keyValue[0], keyValue[1]);
            }
            return settings;
        }

        /** Applies the shot settings found in the map to a phase; the ones left out keep their values. */
        private static void parsePhaseSettings(Map<String, String> settings, ShotPhase phase) {
            if (settings.containsKey("lasts")) phase.setDuration(Double.parseDouble(settings.get("lasts")));
            if (settings.containsKey("shot")) phase.setPattern(Pattern.valueOf(settings.get("shot").toUpperCase()));
            if (settings.containsKey("bullets")) phase.setBulletCount(Integer.parseInt(settings.get("bullets")));
            if (settings.containsKey("spread")) phase.setSpread(Double.parseDouble(settings.get("spread")));
            if (settings.containsKey("bulletSpeed")) phase.setBulletSpeed(Double.parseDouble(settings.get("bulletSpeed")));
            if (settings.containsKey("every")) phase.setFireInterval(Double.parseDouble(settings.get("every")));
            if (settings.containsKey("spin")) phase.setSpin(Double.parseDouble(settings.get("spin")));
            if (settings.containsKey("accel")) phase.setAccel(Double.parseDouble(settings.get("accel")));
            if (settings.containsKey("curve")) phase.setCurve(Double.parseDouble(settings.get("curve")));
            if (settings.containsKey("size")) phase.setBulletSize(Double.parseDouble(settings.get("size")));
            if (settings.containsKey("color")) phase.setColor(BulletColor.valueOf(settings.get("color").toUpperCase()));
            if (settings.containsKey("bounces")) phase.setBounces(Integer.parseInt(settings.get("bounces")));
            // shot= above already gave the pattern's usual aim, so levels from before aim= play the same
            if (settings.containsKey("aim")) phase.setAim(Aim.valueOf(settings.get("aim").toUpperCase()));
            if (settings.containsKey("direction")) phase.setDirection(Double.parseDouble(settings.get("direction")));
            if (settings.containsKey("sweep")) phase.setSweep(Double.parseDouble(settings.get("sweep")));
            if (settings.containsKey("swing")) phase.setSwing(Double.parseDouble(settings.get("swing")));
            if (settings.containsKey("beam")) phase.setBeamTime(Double.parseDouble(settings.get("beam")));
            if (settings.containsKey("width")) phase.setWallWidth(Double.parseDouble(settings.get("width")));
            if (settings.containsKey("shape")) phase.setShape(BulletShape.valueOf(settings.get("shape").toUpperCase()));
            if (settings.containsKey("pic")) phase.setPicture(bulletPicture(settings.get("pic")));
            if (settings.containsKey("picsize")) phase.setPictureSize(Double.parseDouble(settings.get("picsize")) / 100);
            if (settings.containsKey("wave")) phase.setWave(Double.parseDouble(settings.get("wave")));
            if (settings.containsKey("waverate")) phase.setWaveRate(Double.parseDouble(settings.get("waverate")));
            if (settings.containsKey("from")) {
                double[] spot = parsePoint(settings.get("from"));
                phase.setFromSpot(spot[0], spot[1]);
            }
            if (settings.containsKey("together")) phase.setTogether(settings.get("together").equalsIgnoreCase("yes"));
            if (settings.containsKey("atpoint")) {
                phase.clearAtPoints();
                for (String point : settings.get("atpoint").split(",")) {
                    if (!point.isBlank() && !phase.getAtPoints().contains(Integer.parseInt(point.strip()))) {
                        phase.toggleAtPoint(Integer.parseInt(point.strip()));
                    }
                }
            }
            if (settings.containsKey("volleys")) phase.setVolleys(Integer.parseInt(settings.get("volleys")));
            if (settings.containsKey("delay")) phase.setDelay(Double.parseDouble(settings.get("delay")));
            if (settings.containsKey("cardhp")) phase.setCardHealth(Integer.parseInt(settings.get("cardhp")));
            if (settings.containsKey("move")) phase.setMove(BossMove.valueOf(settings.get("move").toUpperCase()));
            if (settings.containsKey("movespeed")) phase.setMoveSpeed(Double.parseDouble(settings.get("movespeed")));
            if (settings.containsKey("moveloop")) {
                phase.setMoveLoop(!settings.get("moveloop").equalsIgnoreCase("no"));
                phase.setMoveBounce(settings.get("moveloop").equalsIgnoreCase("bounce"));
            }
            if (settings.containsKey("movepath") && !settings.get("movepath").isBlank()) {
                phase.getMovePath().clear();
                for (String point : settings.get("movepath").split(";")) {
                    double[] xy = parsePoint(point);
                    int index = phase.addMovePoint(xy[0], xy[1]);
                    phase.setMoveStop(index, xy[2]);
                }
            }
            if (settings.containsKey("hop")) phase.setHop(Double.parseDouble(settings.get("hop")));
            if (settings.containsKey("name")) phase.setCardName(settings.get("name").replace('_', ' '));
            if (settings.containsKey("steps") && !settings.get("steps").isBlank()) {
                phase.getSteps().clear();
                for (String stepText : settings.get("steps").split(";")) {
                    String[] f = stepText.split("/");
                    if (f.length != 6 && f.length != 7) {
                        throw new IllegalArgumentException("a bullet step needs at/speed/accel/curve/turn/aim/split but found \"" + stepText + "\"");
                    }
                    BulletStep step = phase.addStep();
                    step.setTime(Double.parseDouble(f[0]));
                    step.setSpeed(Double.parseDouble(f[1]));
                    step.setAccel(Double.parseDouble(f[2]));
                    step.setCurve(Double.parseDouble(f[3]));
                    step.setTurn(Double.parseDouble(f[4]));
                    step.setAim(f[5].equalsIgnoreCase("aim"));
                    if (f.length == 7) step.setSplit(Integer.parseInt(f[6]));
                }
            }
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
            tanksGoneTimes.clear();
            tanksOnScreen = false;
            waveClearTimes.clear();
            levelTime = 0;
            enemies.clear();
            spawned.clear();
            groupClearTimes.clear();
            groupSpawnTimes.clear();
            batchGoneTimes.clear();
            batchStartTimes.clear();
            batchHistory.clear();
            shots.clear();
            flaks.clear();
            deaths.clear();
            enemyBullets.clear();
            lasers.clear();
            hits = 0;
            invincibleTimeLeft = 0;
            deathFreezeLeft = 0;
            items.clear();
            beamTimeLeft = 0;
            barrierTime = -1;
            barrierRadius = 0;
            if (!keepRun) {
                resetRun(); // a restart in the sandbox starts the run over too
            }
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

        /** Flak explosions on screen, each {x, y, how far along it is from 0 to 1}. */
        public List<double[]> getFlaks() {
            List<double[]> list = new ArrayList<>();
            for (double[] flak : flaks) {
                list.add(new double[]{flak[0], flak[1], flak[2] / FLAK_TIME});
            }
            return list;
        }

        /** Enemy death animations: {x, y, how far along 0..1, size}. */
        public List<double[]> getDeaths() {
            List<double[]> list = new ArrayList<>();
            for (double[] death : deaths) {
                list.add(new double[]{death[0], death[1], death[2] / ENEMY_DEATH_TIME, death[3]});
            }
            return list;
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

        public List<Laser> getLasers() {
            return lasers;
        }

        public List<EnemyBullet> getEnemyBullets() {
            return enemyBullets;
        }

        public int getHits() {
            return hits;
        }

        /** True for a moment after the ship is hit, while it can't be hit again. */
        /** True during the stop after a hit, while the ship is exploding (it isn't drawn). */
        public boolean isShipExploding() {
            return deathFreezeLeft > 0;
        }

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
            /** True when clearing this wave drops a bomb from the top of the screen. */
            private boolean dropsBomb = false;

            public boolean isDropsBomb() {
                return dropsBomb;
            }

            public void setDropsBomb(boolean dropsBomb) {
                this.dropsBomb = dropsBomb;
            }

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
            private double startX;
            private double startY;
            private final List<double[]> waypoints = new ArrayList<>();
            /** How many waypoints each stroke added (a click is a stroke of one), so undo can take back a whole drag. */
            private final List<Integer> strokes = new ArrayList<>();
            private double speed = ENEMY_SPEED;
            private boolean loop = false;
            private int count = 1;
            /** Extra times the whole group is sent again, and the seconds between batches. */
            private int repeats = 0;
            private double repeatGap = 3;
            /** Shots each enemy of the group takes to destroy. */
            private int health = 1;
            private Formation formation = Formation.LINE;
            private double spacing = FOLLOW_SPACING;
            /** True if the formation turns to face the way it flies. */
            private boolean facesMovement = false;
            /** How each enemy's picture points as it flies along its path. */
            private Pointing pointing = Pointing.UPRIGHT;
            /** True for a boss: one big enemy whose phases are attacks with their own health (see STAGES). */
            private boolean boss = false;
            /** Which wave the group belongs to (0 is the first). */
            private int wave = 0;
            private double spawnTime = 0;

            /** The group this one waits for: it spawns once that group is cleared. Null = counts from the wave's start. */
            private EnemyDesign follows;
            /** A group it keeps sending batches until it's gone (null: it sends its repeats and stops). */
            private EnemyDesign until;
            /** True when each repeat waits until the batch before is gone (shot down or flown off), then the gap. */
            private boolean repeatAfterGone = false;
            /** True to count from when the group it follows first spawns, instead of from when it's cleared. */
            private boolean followsSpawn = false;
            /** The shot phases, run one after another (there is always at least one). */
            private final List<ShotPhase> phases = new ArrayList<>();
            private double firstShotDelay;  // seconds from coming on screen to the first shot
            private boolean fireOnlyWhenStopped = false;
            /** Seconds to hold still at the start before moving (waypoints keep their own in waypoint[2]). */
            private double startStopTime = 0;

            public EnemyDesign(Alien.Type type, double startX, double startY) {
                this.type = type;
                this.startX = startX;
                this.startY = startY;
                ShotPhase first = addPhase();
                firstShotDelay = first.getFireInterval() / 2;
            }

            /** Adds a phase at the end, starting with the alien type's own shot. */
            public ShotPhase addPhase() {
                ShotPhase phase = new ShotPhase();
                // Each alien type comes with its own shot, which can then be changed
                switch (type) {
                    case SQUID -> phase.setValues(Pattern.AIMED, 3, 0, 220, 1.2);
                    case CRAB -> phase.setValues(Pattern.FAN, 5, 60, 160, 1.5);
                    case OCTOPUS -> phase.setValues(Pattern.RING, 12, 0, 120, 2.0);
                }
                phases.add(phase);
                return phase;
            }

            /** Removes a phase; the last one left can't be removed. */
            public void removePhase(ShotPhase phase) {
                int index = phases.indexOf(phase);
                if (phases.size() > 1 && index >= 0) {
                    // Removing an attack's first pattern: the layer after it leads the attack now
                    if (!phase.isTogether() && index + 1 < phases.size()) {
                        ShotPhase next = phases.get(index + 1);
                        if (next.isTogether()) {
                            next.setTogether(false);
                            next.setDuration(phase.getDuration());
                            next.setCardName(phase.getCardName());
                            next.setCardHealth(phase.getCardHealth());
                            next.setMove(phase.getMove());
                            next.setHop(phase.getHop());
                            next.getMovePath().clear();
                            next.getMovePath().addAll(phase.getMovePath());
                            next.setMoveSpeed(phase.getMoveSpeed());
                            next.setMoveLoop(phase.isMoveLoop());
                            next.setMoveBounce(phase.isMoveBounce());
                        }
                    }
                    phases.remove(index);
                }
            }

            /** Adds a pattern right after the given one that fires together with it (a layer of the same attack). */
            public ShotPhase addLayer(int after) {
                ShotPhase layer = addPhase();
                phases.remove(phases.size() - 1);
                phases.add(Math.max(0, Math.min(after + 1, phases.size())), layer);
                layer.setTogether(true);
                return layer;
            }

            /** True if the phase at this index fires along with the one before it (the first never does). */
            public boolean isLayer(int index) {
                return index > 0 && index < phases.size() && phases.get(index).isTogether();
            }

            /** The index of the first pattern of the attack the given phase belongs to. */
            public int attackStart(int index) {
                index = Math.max(0, Math.min(index, phases.size() - 1));
                while (isLayer(index)) {
                    index--;
                }
                return index;
            }

            /** The index just past the last pattern of the attack starting at the given index. */
            public int attackEnd(int start) {
                int end = start + 1;
                while (isLayer(end)) {
                    end++;
                }
                return end;
            }

            /** How many attacks it has (its patterns, counting layers of one attack as one). */
            public int attackCount() {
                return attackNumber(phases.size() - 1);
            }

            /** Which attack (counting from 1) the phase at this index belongs to. */
            public int attackNumber(int index) {
                int number = 0;
                for (int i = 0; i <= Math.min(index, phases.size() - 1); i++) {
                    if (!isLayer(i)) {
                        number++;
                    }
                }
                return number;
            }

            public List<ShotPhase> getPhases() {
                return phases;
            }

            /** Moves the whole group, its start and every path point, by dx, dy pixels. */
            public void moveBy(double dx, double dy) {
                startX += dx;
                startY += dy;
                for (double[] point : waypoints) {
                    point[0] += dx;
                    point[1] += dy;
                }
                for (ShotPhase phase : phases) {
                    for (double[] point : phase.getMovePath()) {
                        point[0] += dx;
                        point[1] += dy;
                    }
                }
            }

            public boolean isFollowsSpawn() {
                return followsSpawn;
            }

            public void setFollowsSpawn(boolean fromSpawn) {
                followsSpawn = fromSpawn;
            }

            public EnemyDesign getFollows() {
                return follows;
            }

            public boolean isRepeatAfterGone() {
                return repeatAfterGone;
            }

            public void setRepeatAfterGone(boolean afterGone) {
                repeatAfterGone = afterGone;
            }

            public EnemyDesign getUntil() {
                return until;
            }

            /** Makes it keep sending batches until the other group is gone (null: its repeats, then stops). */
            public void setUntil(EnemyDesign other) {
                if (other == null || canKeepComingUntil(other)) {
                    until = other;
                }
            }

            /**
             * True if this group can keep coming until the other one is gone: another group of the same
             * wave that doesn't (through other groups) wait for or keep coming until this one, since then
             * neither would ever be gone.
             */
            public boolean canKeepComingUntil(EnemyDesign other) {
                if (other == null || other == this || other.getWave() != wave) {
                    return false;
                }
                List<EnemyDesign> todo = new ArrayList<>(List.of(other));
                java.util.Set<EnemyDesign> seen = new java.util.HashSet<>();
                while (!todo.isEmpty()) {
                    EnemyDesign d = todo.remove(todo.size() - 1);
                    if (d == this) {
                        return false;
                    }
                    if (seen.add(d)) {
                        if (d.follows != null) todo.add(d.follows);
                        if (d.until != null) todo.add(d.until);
                    }
                }
                return true;
            }

            /** Makes this group wait for another one (null to stop waiting). See {@link #canFollow}. */
            public void setFollows(EnemyDesign leader) {
                if (leader == null || canFollow(leader)) {
                    follows = leader;
                }
            }

            /**
             * True if this group can wait for the given one: another group of the same wave that doesn't
             * (through other groups) wait for this one, since then neither would ever spawn.
             */
            public boolean canFollow(EnemyDesign leader) {
                if (leader == null || leader == this || leader.getWave() != wave) {
                    return false;
                }
                for (EnemyDesign d = leader; d != null; d = d.follows) {
                    if (d == this) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * When the given enemy of the group spawns (0 is the first). In a LINE the rest follow spacing
             * pixels apart, so the gap stays the same at any speed; any other formation spawns together.
             */
            public double spawnTimeOf(int index) {
                // Enemies 0 to count-1 are the first batch; each repeat comes repeatGap seconds after the one before
                int batch = index / count;
                int inBatch = index % count;
                double batchStart = spawnTime + batch * repeatGap;
                if (formation != Formation.LINE) {
                    return batchStart; // a formation comes in all at once
                }
                return batchStart + inBatch * spacing / speed;
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
                this.count = boss ? 1 : Math.max(1, Math.min(MAX_COUNT, count));
            }

            public boolean isBoss() {
                return boss;
            }

            /** Makes it a boss or not. A boss is always one enemy, sent once, so its count and repeats go back to that. */
            public void setBoss(boolean isBoss) {
                boss = isBoss;
                if (boss) {
                    count = 1;
                    repeats = 0;
                    formation = Formation.LINE;
                }
            }

            /** How many times bigger it's drawn and hit than a normal alien. */
            public double getScale() {
                return boss ? BOSS_SCALE : 1;
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

            /** Sets how long after coming on screen it fires the first time (0 = straight away), kept between 0 and MAX_FIRST_SHOT_TIME. */
            public void setFirstShotDelay(double seconds) {
                firstShotDelay = Math.max(0, Math.min(MAX_FIRST_SHOT_TIME, seconds));
            }

            public double getFirstShotDelay() {
                return firstShotDelay;
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

            public int getRepeats() {
                return repeats;
            }

            public void setRepeats(int times) {
                repeats = boss ? 0 : Math.max(0, Math.min(MAX_REPEATS, times));
            }

            public double getRepeatGap() {
                return repeatGap;
            }

            public void setRepeatGap(double seconds) {
                repeatGap = Math.max(MIN_REPEAT_GAP, Math.min(MAX_REPEAT_GAP, seconds));
            }

            /** Every enemy the group sends in a run: its count for each batch, the first plus the repeats. */
            public int getTotalCount() {
                return count * (repeats + 1);
            }

            public Formation getFormation() {
                return formation;
            }

            public void setFormation(Formation formation) {
                this.formation = boss ? Formation.LINE : formation;
            }

            /** Switches to the next formation in the list, wrapping around. */
            public void nextFormation() {
                Formation[] all = Formation.values();
                formation = all[(formation.ordinal() + 1) % all.length];
            }

            public Pointing getPointing() {
                return pointing;
            }

            public void setPointing(Pointing pointing) {
                this.pointing = pointing;
            }

            public boolean isFacesMovement() {
                return facesMovement;
            }

            public void setFacesMovement(boolean faces) {
                facesMovement = faces;
            }

            /**
             * The way the shape faces when it isn't turned, in radians: up for the shapes that point up
             * (Arrow up, Triangle), down for the rest (a V's tip leads downward, a row is a wall moving down).
             */
            public double formationFront() {
                return formation == Formation.ARROW_UP || formation == Formation.TRIANGLE ? -Math.PI / 2 : Math.PI / 2;
            }

            /** How far a facing formation is turned as it starts: toward its first path point. 0 if it doesn't turn. */
            public double startTurn() {
                if (!facesMovement || waypoints.isEmpty()) {
                    return 0;
                }
                double[] first = waypoints.get(0);
                if (first[0] == startX && first[1] == startY) {
                    return 0;
                }
                return Math.atan2(first[1] - startY, first[0] - startX) - formationFront();
            }

            /** formationOffset(index) turned by the given angle in radians. */
            public double[] turnedOffset(int index, double angle) {
                double[] offset = formationOffset(index);
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                return new double[]{offset[0] * cos - offset[1] * sin, offset[0] * sin + offset[1] * cos};
            }

            public double getSpacing() {
                return spacing;
            }

            public void setSpacing(double px) {
                spacing = Math.max(MIN_SPACING, Math.min(MAX_SPACING, px));
            }

            /**
             * Where enemy number index (0 is the first) flies relative to the path, {x, y} in px, so the
             * group keeps its shape. All zero in a LINE, where they take turns on the path instead.
             */
            public double[] formationOffset(int index) {
                int n = count;
                switch (formation) {
                    case ROW -> {
                        return new double[]{(index - (n - 1) / 2.0) * spacing, 0};
                    }
                    case COLUMN -> {
                        return new double[]{0, (index - (n - 1) / 2.0) * spacing};
                    }
                    case V -> {
                        int back = (index + 1) / 2;
                        int side = index % 2 == 1 ? -1 : 1;
                        return new double[]{side * back * spacing, -back * spacing * 0.75};
                    }
                    case GRID -> {
                        int columns = (int) Math.ceil(Math.sqrt(n));
                        int rows = (int) Math.ceil(n / (double) columns);
                        return new double[]{(index % columns - (columns - 1) / 2.0) * spacing,
                                (index / columns - (rows - 1) / 2.0) * spacing};
                    }
                    case CIRCLE -> {
                        double radius = Math.max(spacing, spacing * n / (2 * Math.PI));
                        double angle = 2 * Math.PI * index / n - Math.PI / 2;
                        return new double[]{Math.cos(angle) * radius, Math.sin(angle) * radius};
                    }
                    case X -> {
                        return crossOffset(index, n, true);
                    }
                    case PLUS -> {
                        return crossOffset(index, n, false);
                    }
                    case LINE -> {
                        return new double[]{0, 0};
                    }
                    default -> {
                        return spreadAlongShape(index, n);
                    }
                }
            }

            /**
             * For X and PLUS: half the enemies on one line through the middle and half on the other, the
             * second line leaving the middle free so no two enemies sit on the same spot.
             */
            private double[] crossOffset(int index, int n, boolean diagonal) {
                int first = (n + 1) / 2;
                double along;
                boolean onFirst = index < first;
                if (onFirst) {
                    along = (index - (first - 1) / 2.0) * spacing;
                } else {
                    int k = index - first;               // 0, 1, 2, ... on the second line
                    int step = k / 2 + 1;                // 1, 1, 2, 2, ...
                    along = (k % 2 == 0 ? -1 : 1) * step * spacing;
                }
                if (diagonal) {
                    double d = along / Math.sqrt(2);
                    return onFirst ? new double[]{d, d} : new double[]{d, -d};
                }
                return onFirst ? new double[]{along, 0} : new double[]{0, along};
            }

            /**
             * Places enemy number index of n evenly along the outline of the formation's shape, scaled so
             * neighbours are about spacing apart, and centred on the path.
             */
            private double[] spreadAlongShape(int index, int n) {
                boolean closed = formation == Formation.TRIANGLE || formation == Formation.SQUARE
                        || formation == Formation.DIAMOND || formation == Formation.STAR || formation == Formation.HEART;
                List<double[]> outline = shapeOutline(formation);
                if (closed) {
                    outline.add(outline.get(0));
                }
                // Lengths along the outline, to share it out evenly
                double total = 0;
                double[] lengths = new double[outline.size()];
                for (int i = 1; i < outline.size(); i++) {
                    total += Math.hypot(outline.get(i)[0] - outline.get(i - 1)[0], outline.get(i)[1] - outline.get(i - 1)[1]);
                    lengths[i] = total;
                }
                int gaps = closed ? n : Math.max(1, n - 1);
                double scale = spacing * gaps / total;
                double wanted = n == 1 && !closed ? total / 2 : total * index / gaps;
                double x = outline.get(0)[0];
                double y = outline.get(0)[1];
                for (int i = 1; i < outline.size(); i++) {
                    if (wanted <= lengths[i] || i == outline.size() - 1) {
                        double segment = lengths[i] - lengths[i - 1];
                        double t = segment == 0 ? 0 : Math.min(1, (wanted - lengths[i - 1]) / segment);
                        x = outline.get(i - 1)[0] + (outline.get(i)[0] - outline.get(i - 1)[0]) * t;
                        y = outline.get(i - 1)[1] + (outline.get(i)[1] - outline.get(i - 1)[1]) * t;
                        break;
                    }
                }
                // Centre the shape's box on the path
                double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                for (double[] point : outline) {
                    minX = Math.min(minX, point[0]);
                    maxX = Math.max(maxX, point[0]);
                    minY = Math.min(minY, point[1]);
                    maxY = Math.max(maxY, point[1]);
                }
                return new double[]{(x - (minX + maxX) / 2) * scale, (y - (minY + maxY) / 2) * scale};
            }

            /** The outline of a shape, as points in any size (it's scaled to fit the spacing). y grows downward. */
            private static List<double[]> shapeOutline(Formation shape) {
                List<double[]> points = new ArrayList<>();
                switch (shape) {
                    case TRIANGLE -> {
                        points.add(new double[]{0, -1});
                        points.add(new double[]{0.866, 0.5});
                        points.add(new double[]{-0.866, 0.5});
                    }
                    case SQUARE -> {
                        points.add(new double[]{-1, -1});
                        points.add(new double[]{1, -1});
                        points.add(new double[]{1, 1});
                        points.add(new double[]{-1, 1});
                    }
                    case DIAMOND -> {
                        points.add(new double[]{0, -1});
                        points.add(new double[]{1, 0});
                        points.add(new double[]{0, 1});
                        points.add(new double[]{-1, 0});
                    }
                    case STAR -> {
                        for (int i = 0; i < 10; i++) {
                            double r = i % 2 == 0 ? 1 : 0.4;
                            double a = -Math.PI / 2 + Math.PI * i / 5;
                            points.add(new double[]{Math.cos(a) * r, Math.sin(a) * r});
                        }
                    }
                    case HEART -> {
                        for (int i = 0; i < 72; i++) {
                            double t = 2 * Math.PI * i / 72;
                            double x = 16 * Math.pow(Math.sin(t), 3);
                            double y = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t));
                            points.add(new double[]{x, y});
                        }
                    }
                    case ARROW_UP -> {
                        points.add(new double[]{-1, 0.75});
                        points.add(new double[]{0, 0});
                        points.add(new double[]{1, 0.75});
                    }
                    case DIAGONAL_DOWN -> {
                        points.add(new double[]{-1, -1});
                        points.add(new double[]{1, 1});
                    }
                    case DIAGONAL_UP -> {
                        points.add(new double[]{-1, 1});
                        points.add(new double[]{1, -1});
                    }
                    case ARC, ARCH -> {
                        // Half a circle: below the middle for an arc, above it for an arch
                        double flip = shape == Formation.ARC ? 1 : -1;
                        for (int i = 0; i <= 48; i++) {
                            double a = Math.PI - Math.PI * i / 48;
                            points.add(new double[]{Math.cos(a), flip * Math.sin(a)});
                        }
                    }
                    case WAVE -> {
                        for (int i = 0; i <= 96; i++) {
                            double x = -3 + 6.0 * i / 96;
                            points.add(new double[]{x, 0.7 * Math.sin(x * Math.PI / 1.5)});
                        }
                    }
                    case S_CURVE -> {
                        for (int i = 0; i <= 96; i++) {
                            double y = -3 + 6.0 * i / 96;
                            points.add(new double[]{0.7 * Math.sin(y * Math.PI / 1.5), y});
                        }
                    }
                    case SPIRAL -> {
                        // One and a half turns outward, so the loops stay well apart
                        for (int i = 0; i <= 160; i++) {
                            double a = 3 * Math.PI * i / 160;
                            double r = 0.2 + a / (3 * Math.PI) * 0.8;
                            points.add(new double[]{Math.cos(a - Math.PI / 2) * r, Math.sin(a - Math.PI / 2) * r});
                        }
                    }
                    default -> points.add(new double[]{0, 0});
                }
                return points;
            }

            public int getHealth() {
                return health;
            }

            /** Sets how many shots each enemy takes, 1 to MAX_HEALTH. */
            public void setHealth(int shots) {
                health = Math.max(1, Math.min(MAX_HEALTH, shots));
            }

            public double getSpawnTime() {
                return spawnTime;
            }
        }

        /**
         * One phase of an enemy's shooting: a bullet pattern, how its bullets move, and how long the
         * phase lasts before the next one starts.
         */
        public static final class ShotPhase {
            private Pattern pattern = Pattern.AIMED;
            private int bulletCount = 1;
            private double spread = 0;          // degrees, for FAN and RANDOM
            private double bulletSpeed = 200;
            private double fireInterval = 1;    // seconds between shots
            private double spin = 15;           // degrees the SPIRAL turns per shot
            private double accel = 0;           // px/s each bullet speeds up (or, below 0, slows down) per second
            private double curve = 0;           // degrees each bullet turns per second
            private double duration = 0;        // seconds this phase lasts; 0 = for good
            private double bulletSize = BULLET_RADIUS;
            private BulletColor color = BulletColor.PINK;
            private BulletShape shape = BulletShape.ORB;
            /** The bullet picture, or null to draw the bullet from shape, color and size. */
            private BulletPicture picture = null;
            /** How big the picture is drawn next to its usual size (1); the hitbox stays the same. */
            private double pictureSize = 1;
            private int bounces = 0;            // times a bullet bounces off the sides and top
            private Aim aim = Aim.PLAYER;
            private double direction = 90;      // degrees, for Aim.FIXED: 0 right, 90 down, 180 left, 270 up
            private double sweep = 0;           // degrees per second the aim turns while the phase runs
            private double swing = 0;           // degrees the sweep goes back and forth across; 0 = keeps turning
            private double beamTime = LASER_TIME; // seconds a laser beam lasts after its warning
            private double wallWidth = 200;     // px from one end of a WALL to the other
            /** Where its bullets come from: a spot on the screen, or NaN to fire from the enemy itself. */
            private double fromX = Double.NaN;
            private double fromY = Double.NaN;
            /** Wavy bullets: px they swing to each side of their way as they fly (0 = straight), and swings per second. */
            private double wave = 0;
            private double waveRate = 1;
            /** Boss attacks only: the spell card's name ("" for a plain attack), shots to beat it, how it moves. */
            /** True to fire at the same time as the phase before it, as another layer of the same attack. */
            private boolean together = false;
            /** Seconds into the attack before this pattern starts firing. */
            private double delay = 0;
            /** Boss attacks: the path points (from 1) it fires at while stopped there, -1 for while moving; empty = anywhere. */
            private final java.util.TreeSet<Integer> atPoints = new java.util.TreeSet<>();
            /** Shots it fires per stop at its point (or in the whole attack when it fires anywhere); 0 = no limit. */
            private int volleys = 0;
            private String cardName = "";
            private int cardHealth = CARD_HEALTH;
            private BossMove move = BossMove.STAY;
            private double hop = 2;             // s between a moving boss's hops
            /** move=PATH: the points it flies to in order, each {x, y, stop seconds}; its speed; and whether it loops. */
            private final List<double[]> movePath = new ArrayList<>();
            private double moveSpeed = ENEMY_SPEED;
            private boolean moveLoop = true;
            /** True to fly the path back and forth instead of looping round it. */
            private boolean moveBounce = false;
            /** Timed changes to every bullet of this phase, in the order they happen. */
            private final List<BulletStep> steps = new ArrayList<>();

            void setValues(Pattern pattern, int count, double spread, double speed, double interval) {
                setPattern(pattern);
                this.bulletCount = count;
                this.spread = spread;
                this.bulletSpeed = speed;
                this.fireInterval = interval;
            }

            /**
             * Sets the pattern, and the aim that pattern usually has: rings and spirals start pointing
             * straight down, the rest at the player. The aim can be changed after.
             */
            public void setPattern(Pattern pattern) {
                this.pattern = pattern;
                if (pattern == Pattern.RING || pattern == Pattern.SPIRAL) {
                    aim = Aim.FIXED;
                    direction = 90;
                } else {
                    aim = Aim.PLAYER;
                }
            }

            /** Switches to the next pattern in the list, wrapping around. */
            public void nextPattern() {
                Pattern[] all = Pattern.values();
                setPattern(all[(pattern.ordinal() + 1) % all.length]);
            }

            public void setAim(Aim aim) {
                this.aim = aim;
            }

            /** Sets the fixed direction in degrees, kept between 0 and 360. */
            public void setDirection(double degrees) {
                direction = ((degrees % 360) + 360) % 360;
            }

            public void setSweep(double degreesPerSecond) {
                sweep = Math.max(-MAX_SWEEP, Math.min(MAX_SWEEP, degreesPerSecond));
            }

            public void setSwing(double degrees) {
                swing = Math.max(0, Math.min(360, degrees));
            }

            public void setBeamTime(double seconds) {
                beamTime = Math.max(0.1, Math.min(MAX_BEAM_TIME, seconds));
            }

            public void setWallWidth(double px) {
                wallWidth = Math.max(0, Math.min(MAX_WALL_WIDTH, px));
            }

            public Aim getAim() {
                return aim;
            }

            public double getDirection() {
                return direction;
            }

            public double getSweep() {
                return sweep;
            }

            public double getSwing() {
                return swing;
            }

            public double getBeamTime() {
                return beamTime;
            }

            public double getWallWidth() {
                return wallWidth;
            }

            /**
             * How far the sweep has turned the aim, in degrees, after the phase has run this long. With a
             * swing it goes back and forth like a windshield wiper, starting from one end.
             */
            public double sweepAngle(double phaseSeconds) {
                double turned = sweep * phaseSeconds;
                if (swing <= 0) {
                    return turned;
                }
                double along = Math.abs(turned) % (2 * swing);
                double offset = along <= swing ? along : 2 * swing - along;
                return Math.signum(sweep) * (offset - swing / 2);
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

            public void setAccel(double pxPerSecond) {
                accel = Math.max(-MAX_BULLET_ACCEL, Math.min(MAX_BULLET_ACCEL, pxPerSecond));
            }

            public void setCurve(double degreesPerSecond) {
                curve = Math.max(-MAX_BULLET_CURVE, Math.min(MAX_BULLET_CURVE, degreesPerSecond));
            }

            public void setBulletSize(double radius) {
                bulletSize = Math.max(MIN_BULLET_SIZE, Math.min(MAX_BULLET_SIZE, radius));
            }

            public void setColor(BulletColor color) {
                this.color = color;
            }

            /** Switches to the next color in the list, wrapping around. */
            public void nextColor() {
                BulletColor[] all = BulletColor.values();
                color = all[(color.ordinal() + 1) % all.length];
            }

            public BulletShape getShape() {
                return shape;
            }

            public void setShape(BulletShape look) {
                shape = look;
            }

            public void nextShape() {
                BulletShape[] all = BulletShape.values();
                shape = all[(shape.ordinal() + 1) % all.length];
            }

            /** The bullet picture, or null when the bullet is drawn from its shape, color and size. */
            public BulletPicture getPicture() {
                return picture;
            }

            public void setPicture(BulletPicture picture) {
                this.picture = picture;
            }

            public double getPictureSize() {
                return pictureSize;
            }

            public void setPictureSize(double times) {
                pictureSize = Math.max(MIN_PICTURE_SIZE, Math.min(MAX_PICTURE_SIZE, times));
            }

            public void setBounces(int times) {
                bounces = Math.max(0, Math.min(MAX_BOUNCES, times));
            }

            public double getBulletSize() {
                return bulletSize;
            }

            public BulletColor getColor() {
                return color;
            }

            public int getBounces() {
                return bounces;
            }

            public boolean isTogether() {
                return together;
            }

            public void setTogether(boolean withThePhaseBefore) {
                together = withThePhaseBefore;
            }

            /** The path points it fires at (-1 = while moving); empty when it fires anywhere. */
            public java.util.SortedSet<Integer> getAtPoints() {
                return atPoints;
            }

            /** Adds the point to the ones it fires at, or takes it off if it's there. */
            public void toggleAtPoint(int point) {
                if (point == 0 || point < -1 || point > MAX_MOVE_POINTS) {
                    return;
                }
                if (!atPoints.remove(point)) {
                    atPoints.add(point);
                }
            }

            /** Fires anywhere again. */
            public void clearAtPoints() {
                atPoints.clear();
            }

            /**
             * Sets every shot setting from level text (shot= bullets= ... steps=), the rest going back to
             * plain values first, so a ready-made pattern looks the same whatever was there before. Its
             * place in the attack, where it fires and its boss settings are kept.
             */
            public void applyPattern(String text) {
                accel = 0;
                curve = 0;
                wave = 0;
                waveRate = 1;
                shape = BulletShape.ORB;
                sweep = 0;
                swing = 0;
                bounces = 0;
                spread = 0;
                bulletSize = BULLET_RADIUS;
                steps.clear();
                parsePhaseSettings(readSettings(text), this);
            }

            /** True when its bullets come from a spot on the screen rather than the enemy. */
            public boolean hasFromSpot() {
                return !Double.isNaN(fromX);
            }

            public double getFromX() {
                return fromX;
            }

            public double getFromY() {
                return fromY;
            }

            /** Fires from this spot on the screen (kept on the screen, its edges included). */
            public void setFromSpot(double x, double y) {
                fromX = Math.max(0, Math.min(PLAYFIELD_WIDTH, x));
                fromY = Math.max(0, Math.min(PLAYFIELD_HEIGHT, y));
            }

            /** Fires from the enemy again. */
            public void clearFromSpot() {
                fromX = Double.NaN;
                fromY = Double.NaN;
            }

            public double getWave() {
                return wave;
            }

            public void setWave(double px) {
                wave = Math.max(0, Math.min(MAX_WAVE_SIZE, px));
            }

            public double getWaveRate() {
                return waveRate;
            }

            public void setWaveRate(double perSecond) {
                waveRate = Math.max(0.1, Math.min(MAX_WAVE_RATE, perSecond));
            }

            public int getVolleys() {
                return volleys;
            }

            public void setVolleys(int shots) {
                volleys = Math.max(0, Math.min(MAX_VOLLEYS, shots));
            }

            public double getDelay() {
                return delay;
            }

            public void setDelay(double seconds) {
                delay = Math.max(0, Math.min(MAX_PHASE_TIME, seconds));
            }

            public String getCardName() {
                return cardName;
            }

            /** Sets the spell card's name; spaces at the ends are dropped, and "" makes it a plain attack. */
            public void setCardName(String name) {
                cardName = name == null ? "" : name.strip();
            }

            public int getCardHealth() {
                return cardHealth;
            }

            public void setCardHealth(int shots) {
                cardHealth = Math.max(1, Math.min(MAX_CARD_HEALTH, shots));
            }

            public BossMove getMove() {
                return move;
            }

            public void setMove(BossMove move) {
                this.move = move;
            }

            public List<double[]> getMovePath() {
                return movePath;
            }

            /** Adds a point to the end of the attack's movement path (unless it's full) and returns its index, or -1. */
            public int addMovePoint(double px, double py) {
                if (movePath.size() >= MAX_MOVE_POINTS) {
                    return -1;
                }
                movePath.add(new double[]{px, py, 0});
                return movePath.size() - 1;
            }

            /** The movement path point within radius of (px, py), or -1. */
            public int findMovePointNear(double px, double py, double radius) {
                for (int i = 0; i < movePath.size(); i++) {
                    if (Math.hypot(movePath.get(i)[0] - px, movePath.get(i)[1] - py) <= radius) {
                        return i;
                    }
                }
                return -1;
            }

            public void setMoveStop(int point, double seconds) {
                if (point >= 0 && point < movePath.size()) {
                    movePath.get(point)[2] = Math.max(0, Math.min(MAX_STOP_TIME, seconds));
                }
            }

            public double getMoveSpeed() {
                return moveSpeed;
            }

            public void setMoveSpeed(double speed) {
                moveSpeed = Math.max(MIN_ENEMY_SPEED, Math.min(MAX_ENEMY_SPEED, speed));
            }

            public boolean isMoveBounce() {
                return moveBounce;
            }

            public void setMoveBounce(boolean backAndForth) {
                moveBounce = backAndForth;
            }

            public boolean isMoveLoop() {
                return moveLoop;
            }

            public void setMoveLoop(boolean loop) {
                moveLoop = loop;
            }

            public double getHop() {
                return hop;
            }

            public void setHop(double seconds) {
                hop = Math.max(MIN_HOP, Math.min(MAX_HOP, seconds));
            }

            /** Sets how long the phase lasts, 0 (for good) to MAX_PHASE_TIME seconds. */
            public void setDuration(double seconds) {
                duration = Math.max(0, Math.min(MAX_PHASE_TIME, seconds));
            }

            /** Adds a step after the last one (half a second later), unless there are MAX_BULLET_STEPS already. */
            public BulletStep addStep() {
                if (steps.size() >= MAX_BULLET_STEPS) {
                    return steps.get(steps.size() - 1);
                }
                BulletStep step = new BulletStep();
                step.setTime(steps.isEmpty() ? 0.5 : steps.get(steps.size() - 1).getTime() + 0.5);
                step.setSpeed(bulletSpeed);
                steps.add(step);
                return step;
            }

            public void removeStep(BulletStep step) {
                steps.remove(step);
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

            public double getFireInterval() {
                return fireInterval;
            }

            public double getSpin() {
                return spin;
            }

            public double getAccel() {
                return accel;
            }

            public double getCurve() {
                return curve;
            }

            public double getDuration() {
                return duration;
            }

            public List<BulletStep> getSteps() {
                return steps;
            }
        }

        /**
         * A timed change to a bullet, like ZUN's bullet transforms: at the given time after the bullet was
         * fired its speed, acceleration and curve are set to new values, and it turns by some degrees or
         * re-aims at the player. With a few of these a plain ring can stop, wait, and then home in.
         */
        public static final class BulletStep {
            private double time;    // seconds after the bullet was fired
            private double speed;   // px/s
            private double accel;   // px/s per second
            private double curve;   // degrees per second
            private double turn;    // degrees turned at that moment (after aiming, if it aims)
            private boolean aim;    // points at the player
            private int split;      // bursts into this many bullets in a ring (0 = doesn't split)

            public void setTime(double seconds) {
                time = Math.max(0, Math.min(MAX_STEP_TIME, seconds));
            }

            public void setSpeed(double pxPerSecond) {
                speed = Math.max(0, Math.min(MAX_BULLET_SPEED, pxPerSecond));
            }

            public void setAccel(double pxPerSecond) {
                accel = Math.max(-MAX_BULLET_ACCEL, Math.min(MAX_BULLET_ACCEL, pxPerSecond));
            }

            public void setCurve(double degreesPerSecond) {
                curve = Math.max(-MAX_BULLET_CURVE, Math.min(MAX_BULLET_CURVE, degreesPerSecond));
            }

            public void setTurn(double degrees) {
                turn = Math.max(-180, Math.min(180, degrees));
            }

            public void setAim(boolean aim) {
                this.aim = aim;
            }

            public void setSplit(int bullets) {
                split = Math.max(0, Math.min(MAX_SPLIT, bullets));
            }

            public int getSplit() {
                return split;
            }

            public double getTime() {
                return time;
            }

            public double getSpeed() {
                return speed;
            }

            public double getAccel() {
                return accel;
            }

            public double getCurve() {
                return curve;
            }

            public double getTurn() {
                return turn;
            }

            public boolean isAim() {
                return aim;
            }
        }

        /** One enemy bullet: where it is, how fast and which way it's going, and how it changes over time. */
        public static final class EnemyBullet {
            private double x;
            private double y;
            private double speed;
            private double angle;   // radians, 0 = right, pi/2 = down
            private double accel;
            private double curve;   // radians per second
            private double age = 0;
            /** The next step to apply from the phase that fired it. */
            private int nextStep = 0;
            private int bouncesLeft;
            private final ShotPhase phase;
            /** True once it has been on the screen (or near it); before that, being off it doesn't remove it. */
            private boolean entered = false;
            /** Where along its side-to-side swing a wavy bullet starts, in radians. */
            private double wavePhase = 0;
            /** True once it has been grazed, so it scores only once. */
            private boolean grazed = false;

            EnemyBullet(double x, double y, double speed, double angle, ShotPhase phase) {
                this.x = x;
                this.y = y;
                this.speed = speed;
                this.angle = angle;
                this.phase = phase;
                this.accel = phase.getAccel();
                this.curve = Math.toRadians(phase.getCurve());
                this.bouncesLeft = phase.getBounces();
            }

            /** A copy heading another way, for when a bullet splits. */
            private EnemyBullet copyTurned(double newAngle) {
                EnemyBullet copy = new EnemyBullet(x, y, speed, newAngle, phase);
                copy.accel = accel;
                copy.curve = curve;
                copy.age = age;
                copy.nextStep = nextStep;
                copy.bouncesLeft = bouncesLeft;
                copy.wavePhase = wavePhase;
                return copy;
            }

            /** Moves by one time step, applying any steps that are due. Bullets it splits into go in newBullets. */
            void move(double deltaSeconds, double playerX, double playerY, List<EnemyBullet> newBullets) {
                age += deltaSeconds;
                List<BulletStep> steps = phase.getSteps();
                while (nextStep < steps.size() && age >= steps.get(nextStep).getTime()) {
                    BulletStep step = steps.get(nextStep++);
                    if (step.isAim()) {
                        angle = Math.atan2(playerY - y, playerX - x);
                    }
                    angle += Math.toRadians(step.getTurn());
                    speed = step.getSpeed();
                    accel = step.getAccel();
                    curve = Math.toRadians(step.getCurve());
                    // Splitting: this bullet keeps going as one of them, the others fan out evenly around it
                    for (int i = 1; i < step.getSplit(); i++) {
                        newBullets.add(copyTurned(angle + 2 * Math.PI * i / step.getSplit()));
                    }
                }
                speed = Math.max(0, Math.min(MAX_BULLET_SPEED, speed + accel * deltaSeconds));
                angle += curve * deltaSeconds;
                x += Math.cos(angle) * speed * deltaSeconds;
                y += Math.sin(angle) * speed * deltaSeconds;
                if (phase.getWave() > 0) {
                    // Wavy: swings side to side across its way, like a snake
                    double w = 2 * Math.PI * phase.getWaveRate();
                    double side = phase.getWave() * (Math.sin(w * age + wavePhase) - Math.sin(w * (age - deltaSeconds) + wavePhase));
                    x -= Math.sin(angle) * side;
                    y += Math.cos(angle) * side;
                }
                // Bouncing off the sides and the top (never the bottom, the player's side)
                if (bouncesLeft > 0) {
                    if (x < 0 || x > PLAYFIELD_WIDTH) {
                        x = Math.max(0, Math.min(PLAYFIELD_WIDTH, x));
                        angle = Math.PI - angle;
                        bouncesLeft--;
                    } else if (y < 0) {
                        y = 0;
                        angle = -angle;
                        bouncesLeft--;
                    }
                }
            }

            /** How close it has to be to hit: the pattern's bullet size, with a picture or without (the picture is scaled to fit it). */
            public double getRadius() {
                return phase.getBulletSize();
            }

            /** Its picture, or null when it's drawn from shape, color and size. */
            public BulletPicture getPicture() {
                return phase.getPicture();
            }

            /** How big its picture is drawn next to its usual size (the hitbox doesn't change). */
            public double getPictureSize() {
                return phase.getPictureSize();
            }

            public BulletColor getColor() {
                return phase.getColor();
            }

            public BulletShape getShape() {
                return phase.getShape();
            }

            /** The way it's flying, in radians (0 = right, pi/2 = down). */
            public double getAngle() {
                return angle;
            }

            public double getX() {
                return x;
            }

            public double getY() {
                return y;
            }

            public double getAge() {
                return age;
            }
        }

        /**
         * A laser beam fired by an enemy. It starts as a thin warning line that can't hurt, then turns
         * into a beam for LASER_TIME seconds. It stays attached to the enemy and keeps its direction.
         */
        public static final class Laser {
            private final Enemy enemy;
            /** Its direction before the sweep, in radians. */
            private final double baseAngle;
            private final ShotPhase phase;
            /** How long the phase had run when it was fired, so the sweep carries on from there. */
            private final double firedAtPhaseTime;
            private double age = 0;
            /** True once its beam has been grazed. */
            private boolean grazed = false;

            Laser(Enemy enemy, double baseAngle, ShotPhase phase, double firedAtPhaseTime) {
                this.enemy = enemy;
                this.baseAngle = baseAngle;
                this.phase = phase;
                this.firedAtPhaseTime = firedAtPhaseTime;
            }

            /** Its direction now, in radians: it turns with the phase's sweep while it lasts. */
            public double getAngle() {
                return baseAngle + Math.toRadians(phase.sweepAngle(firedAtPhaseTime + age));
            }

            void update(double deltaSeconds) {
                age += deltaSeconds;
            }

            boolean isOver() {
                return age > LASER_WARN_TIME + phase.getBeamTime();
            }

            /** True while it's the warning line, which can't hurt. */
            public boolean isWarning() {
                return age < LASER_WARN_TIME;
            }

            /** True if the beam (not the warning) touches a circle at x, y. */
            boolean touches(double x, double y, double radius) {
                if (isWarning()) {
                    return false;
                }
                // Distance from the point to the beam, which runs from the enemy along the angle
                double dx = x - getStartX();
                double dy = y - getStartY();
                double angle = getAngle();
                double along = dx * Math.cos(angle) + dy * Math.sin(angle);
                if (along < 0) {
                    return false;
                }
                double across = Math.abs(-dx * Math.sin(angle) + dy * Math.cos(angle));
                return across < getWidth() / 2 + radius;
            }

            public double getStartX() {
                return phase.hasFromSpot() ? phase.getFromX() : enemy.getX();
            }

            public double getStartY() {
                return phase.hasFromSpot() ? phase.getFromY() : enemy.getY();
            }

            public double getEndX() {
                return getStartX() + Math.cos(getAngle()) * LASER_LENGTH;
            }

            public double getEndY() {
                return getStartY() + Math.sin(getAngle()) * LASER_LENGTH;
            }

            /** Full width of the beam: the phase's bullet size is its half-width. */
            public double getWidth() {
                return phase.getBulletSize() * 2;
            }

            public BulletColor getColor() {
                return phase.getColor();
            }
        }

        /** One enemy in a run, flying along its design's path. Path edits show up right away. */
        public static final class Enemy {
            private final EnemyDesign design;
            /** Where it is on the path; a formation member flies offset from here. */
            private double x;
            private double y;
            /** Its place in the formation, relative to the path (0, 0 in a line), before any turning. */
            private final double[] baseOffset;
            /** How far the formation is turned now, in radians (it only turns when the design faces its movement). */
            private double turn;
            /** The way it's flying, in radians (NaN until it has moved). */
            private double heading = Double.NaN;
            /** The way it last flew, so a formation can carry on off the screen after its path ends. */
            private double headingX = 0;
            private double headingY = 1;
            /** Index of the waypoint being flown to. Equal to the number of waypoints when looping back to the start. */
            private int target = 0;
            /** Seconds left of the fade-in for an enemy that appears on screen. */
            private double appearTimeLeft;
            private boolean pathFinished = false;
            /** Seconds until the next shot. Only counts down on screen, so the first comes the design's first-shot delay after it shows up. */
            private final double[] fireTimeLeft = new double[MAX_PHASES];
            /** How far a spiral has turned so far, in degrees. */
            private final double[] spinAngle = new double[MAX_PHASES];
            /** Seconds left holding still at a stop. */
            private double stopTimeLeft;
            /** Which shot phase it's in, and for how long it has been in it. */
            private int phaseIndex = 0;
            private double phaseTime = 0;
            /** For Aim.ONCE: the direction it locked onto this phase, or NaN before its first shot. */
            private final double[] lockedAim = new double[MAX_PHASES];

            /** Hits it can still take; it starts with its design's health. */
            private int healthLeft;
            /** Seconds left of the white flash after a hit. */
            private double hitFlashLeft = 0;

            // Boss only
            /** True once it has reached the end of its path and started its attacks. */
            private boolean fighting = false;
            /** Hits the current attack can still take. */
            private int attackHealthLeft;
            /** Seconds left of the break before the next attack. */
            private double breakLeft = 0;
            /** True when its last attack timed out and it's flying away. */
            private boolean leaving = false;
            /** Set when an attack ends, until the game has cleared the bullets. */
            private boolean attackEnded = false;
            /** Set when an attack is beaten, until the game has dropped its items. */
            private boolean attackBeaten = false;
            /** Beam bomb damage not yet taken (it's taken in whole hits). */
            private double beamDamageOwed = 0;
            /** The spot it fights from (where its path ended), and where it's moving to now. */
            private double homeX;
            private double homeY;
            private double moveToX;
            private double moveToY;
            private double hopLeft = 0;
            /** move=PATH: the point it's flying to, and seconds left holding still at the one it reached. */
            private int routeTarget = 0;
            /** 1 going forward along the path, -1 coming back (back and forth). */
            private int routeDirection = 1;
            /** The path point it's holding still at (from 0), or -1 while it moves; and true once it has stopped for good at the end. */
            private int stoppedAt = -1;
            private boolean routeDone = false;
            /** Shots each pattern has fired at this stop (or this attack, for one that fires anywhere). */
            private final int[] shotsFired = new int[MAX_PHASES];
            private double routeStopLeft = 0;
            /** Picks a moving boss's spots; seeded so a level plays the same every time. */
            private final Random wander = new Random(7);

            public Enemy(EnemyDesign design, int index) {
                this.design = design;
                this.baseOffset = design.formationOffset(index);
                this.turn = design.startTurn();
                this.healthLeft = design.getHealth();
                this.x = design.getStartX();
                this.y = design.getStartY();
                this.appearTimeLeft = design.entersFromEdge() ? 0 : APPEAR_TIME;
                java.util.Arrays.fill(fireTimeLeft, design.getFirstShotDelay());
                java.util.Arrays.fill(lockedAim, Double.NaN);
                this.stopTimeLeft = design.getStopTime(0);
            }

            /** True while it holds still: at a stop, or at the end of its path. */
            public boolean isStopped() {
                return stopTimeLeft > 0 || pathFinished;
            }

            /**
             * Counts down to the next shot of each pattern of the attack it's on, and returns the phase
             * indexes of the ones due to fire now (only on screen, never while fading in).
             */
            List<Integer> layersReadyToFire(double deltaSeconds) {
                List<Integer> due = new ArrayList<>();
                boolean onScreen = getX() >= 0 && getX() <= PLAYFIELD_WIDTH && getY() >= 0 && getY() <= PLAYFIELD_HEIGHT;
                boolean allowedToFire = !design.isFireOnlyWhenStopped() || isStopped();
                if (isAppearing() || !onScreen) {
                    return due;
                }
                if (design.isBoss()) {
                    // A boss's attack time is kept in fly(); it only shoots during an attack
                    allowedToFire = fighting && !leaving && breakLeft <= 0;
                } else {
                    advancePhase(deltaSeconds);
                }
                if (!allowedToFire) {
                    return due;
                }
                List<ShotPhase> phases = design.getPhases();
                int start = design.attackStart(phaseIndex);
                int end = Math.min(design.attackEnd(start), start + MAX_PHASES);
                for (int i = start; i < end; i++) {
                    ShotPhase layer = phases.get(i);
                    int slot = i - start;
                    if (layer.getPattern() == Pattern.NONE || phaseTime < layer.getDelay() - 1e-9 || !inPlaceToFire(layer, slot)) {
                        continue;
                    }
                    fireTimeLeft[slot] -= deltaSeconds;
                    // The tiny margin stops rounding from adding a tick: every=0.05 fires every 3 ticks, not 4
                    if (fireTimeLeft[slot] > 1e-9) {
                        continue;
                    }
                    fireTimeLeft[slot] = layer.getFireInterval();
                    shotsFired[slot]++;
                    due.add(i);
                }
                return due;
            }

            /**
             * True if a boss's pattern may fire where the boss is now: at its path point (or while moving)
             * when it has one, and with shots left at this stop when it has a shot count.
             */
            private boolean inPlaceToFire(ShotPhase layer, int slot) {
                if (!design.isBoss()) {
                    return true;
                }
                boolean onPath = followsPath(getPhase());
                if (onPath && !layer.getAtPoints().isEmpty()
                        && !layer.getAtPoints().contains(stoppedAt >= 0 ? stoppedAt + 1 : -1)) {
                    return false; // not at one of its points (or not moving, for one that fires while moving)
                }
                return layer.getVolleys() == 0 || shotsFired[slot] < layer.getVolleys();
            }

            /** Reached path point i: patterns that fire there start their shots fresh, straight away. */
            private void arriveAt(int point) {
                stoppedAt = point;
                List<ShotPhase> phases = design.getPhases();
                int start = design.attackStart(phaseIndex);
                for (int i = start; i < design.attackEnd(start) && i - start < MAX_PHASES; i++) {
                    if (!phases.get(i).getAtPoints().isEmpty()) {
                        shotsFired[i - start] = 0;
                        fireTimeLeft[i - start] = 0;
                    }
                }
            }

            /** Every pattern of a new attack starts fresh: a first shot straight away, no spin, no locked aim. */
            private void resetLayers() {
                java.util.Arrays.fill(fireTimeLeft, 0);
                java.util.Arrays.fill(spinAngle, 0);
                java.util.Arrays.fill(lockedAim, Double.NaN);
            }

            /** Phases run on from when it shows up on screen, and after the last one start again at phase 1. */
            private void advancePhase(double deltaSeconds) {
                ShotPhase phase = getPhase();
                phaseTime += deltaSeconds;
                if (phase.getDuration() > 0 && phaseTime >= phase.getDuration() - 1e-9) {
                    phaseIndex = design.attackEnd(design.attackStart(phaseIndex)) % design.getPhases().size();
                    phaseTime = 0;
                    // A new phase starts with a shot (spirals keep turning on from where they were)
                    java.util.Arrays.fill(fireTimeLeft, 0);
                    java.util.Arrays.fill(lockedAim, Double.NaN);
                }
            }

            /**
             * A boss's turn: once its path is done it starts attack 1, keeps time on the attack (ending it
             * when time runs out), and moves the way the attack says. Called from fly().
             */
            private void bossTurn(double deltaSeconds) {
                if (leaving) {
                    y -= BOSS_LEAVE_SPEED * deltaSeconds;
                    return;
                }
                if (!fighting) {
                    if (!pathFinished) {
                        return;
                    }
                    fighting = true;
                    homeX = x;
                    homeY = y;
                    startAttack(0);
                    breakLeft = 1; // a moment to settle in before the first attack
                }
                ShotPhase attack = getPhase();
                if (breakLeft > 0) {
                    breakLeft = Math.max(0, breakLeft - deltaSeconds);
                    // Between attacks it goes back to its spot, or to the start of the next attack's path
                    if (followsPath(attack)) {
                        double[] first = attack.getMovePath().get(0);
                        moveToward(first[0], first[1], design.getSpeed(), deltaSeconds);
                    } else {
                        moveToward(homeX, homeY, deltaSeconds);
                    }
                    return;
                }
                phaseTime += deltaSeconds;
                if (attack.getDuration() > 0 && phaseTime >= attack.getDuration() - 1e-9) {
                    endAttack(true);
                    return;
                }
                if (attack.getMove() == BossMove.WANDER) {
                    hopLeft -= deltaSeconds;
                    if (hopLeft <= 0) {
                        hopLeft = attack.getHop();
                        moveToX = clampX(homeX + (wander.nextDouble() * 2 - 1) * WANDER_RANGE_X);
                        moveToY = Math.max(30, Math.min(PLAYFIELD_HEIGHT / 2.0, homeY + (wander.nextDouble() * 2 - 1) * WANDER_RANGE_Y));
                    }
                    moveToward(moveToX, moveToY, deltaSeconds);
                } else if (followsPath(attack)) {
                    followPath(attack, deltaSeconds);
                } else {
                    moveToward(homeX, homeY, deltaSeconds);
                }
            }

            private static boolean followsPath(ShotPhase attack) {
                return attack.getMove() == BossMove.PATH && !attack.getMovePath().isEmpty();
            }

            /** Flies the attack's own path point to point, holding still at each point's stop, looping if it should. */
            private void followPath(ShotPhase attack, double deltaSeconds) {
                List<double[]> path = attack.getMovePath();
                if (routeDone) {
                    return; // at the end of a path that doesn't loop: stays put
                }
                if (routeStopLeft > 0) {
                    routeStopLeft = Math.max(0, routeStopLeft - deltaSeconds);
                    return;
                }
                if (stoppedAt >= 0) {
                    // Leaving a point: patterns that fire while moving start fresh
                    arriveAt(-1);
                }
                routeTarget = Math.min(routeTarget, path.size() - 1);
                double[] point = path.get(routeTarget);
                moveToward(point[0], point[1], attack.getMoveSpeed(), deltaSeconds);
                if (x == point[0] && y == point[1]) {
                    boolean last = !attack.isMoveBounce() && routeTarget + 1 >= path.size() && !(attack.isMoveLoop() && path.size() > 1);
                    if (point[2] > 0 || last) {
                        arriveAt(routeTarget);
                    }
                    routeDone = last;
                    if (attack.isMoveBounce() && path.size() > 1) {
                        // Back and forth: turns round at either end
                        if (routeTarget + routeDirection < 0 || routeTarget + routeDirection >= path.size()) {
                            routeDirection = -routeDirection;
                        }
                        routeStopLeft = point[2];
                        routeTarget += routeDirection;
                    } else if (routeTarget + 1 < path.size()) {
                        routeStopLeft = point[2];
                        routeTarget++;
                    } else if (attack.isMoveLoop() && path.size() > 1) {
                        routeStopLeft = point[2];
                        routeTarget = 0;
                    }
                    // Otherwise it stays at the last point
                }
            }

            private static double clampX(double value) {
                return Math.max(40, Math.min(PLAYFIELD_WIDTH - 40, value));
            }

            /** Moves straight toward a spot at its speed, stopping on it. */
            private void moveToward(double toX, double toY, double deltaSeconds) {
                moveToward(toX, toY, design.getSpeed(), deltaSeconds);
            }

            private void moveToward(double toX, double toY, double speed, double deltaSeconds) {
                double dx = toX - x;
                double dy = toY - y;
                double distance = Math.hypot(dx, dy);
                double step = speed * deltaSeconds;
                if (distance <= step) {
                    x = toX;
                    y = toY;
                } else {
                    x += dx / distance * step;
                    y += dy / distance * step;
                }
            }

            /** Starts the given attack fresh: its health, its clock, and a first shot as soon as the break ends. */
            private void startAttack(int index) {
                phaseIndex = index;
                phaseTime = 0;
                attackHealthLeft = getPhase().getCardHealth();
                resetLayers();
                hopLeft = 0;
                routeTarget = 0;
                routeDirection = 1;
                routeStopLeft = 0;
                stoppedAt = -1;
                routeDone = false;
                java.util.Arrays.fill(shotsFired, 0);
                moveToX = homeX;
                moveToY = homeY;
                breakLeft = CARD_BREAK;
            }

            /**
             * Ends the current attack, beaten or timed out. The bullets on screen vanish and the next
             * attack starts after a break. True when that was the last one and it was beaten (the boss
             * is destroyed); if the last one timed out the boss flies away instead.
             */
            private boolean endAttack(boolean timedOut) {
                attackEnded = true;
                int next = design.attackEnd(design.attackStart(phaseIndex));
                if (next < design.getPhases().size()) {
                    startAttack(next);
                    return false;
                }
                if (timedOut) {
                    leaving = true;
                    pathFinished = true;
                    return false;
                }
                return true;
            }

            /** True once after an attack has ended, so the game clears the bullets once. */
            boolean takeAttackEnded() {
                boolean ended = attackEnded;
                attackEnded = false;
                return ended;
            }

            /** True for a boss that's fighting (it has started its attacks). */
            public boolean isFighting() {
                return fighting;
            }

            /** Hits the boss's current attack can still take. */
            public int getAttackHealthLeft() {
                return attackHealthLeft;
            }

            /** Seconds left on the boss's current attack, or -1 when it has no time limit. */
            public double getAttackTimeLeft() {
                double limit = getPhase().getDuration();
                return limit > 0 ? Math.max(0, limit - phaseTime) : -1;
            }

            /** True during the short break between a boss's attacks. */
            public boolean isBetweenAttacks() {
                return breakLeft > 0;
            }

            public boolean isLeaving() {
                return leaving;
            }

            /** The shot phase it's in now (the first pattern of the attack, when it has layers). */
            public ShotPhase getPhase() {
                List<ShotPhase> phases = design.getPhases();
                if (phaseIndex >= phases.size()) {
                    phaseIndex = 0; // a phase was removed in the editor
                }
                phaseIndex = design.attackStart(phaseIndex);
                return phases.get(phaseIndex);
            }

            public int getPhaseIndex() {
                return phaseIndex;
            }

            /** Takes a hit: loses that much health and flashes. True when that destroyed it. */
            boolean takeHit(int damage) {
                if (design.isBoss()) {
                    // A boss only takes damage while an attack is on, and each attack has its own health
                    if (!fighting || leaving || breakLeft > 0) {
                        return false;
                    }
                    hitFlashLeft = HIT_FLASH_TIME;
                    attackHealthLeft -= damage;
                    if (attackHealthLeft <= 0) {
                        attackBeaten = true;
                        return endAttack(false);
                    }
                    return false;
                }
                healthLeft -= damage;
                hitFlashLeft = HIT_FLASH_TIME;
                return healthLeft <= 0;
            }

            /** True once after one of its attacks was beaten (not timed out), so its items drop once. */
            boolean takeAttackBeaten() {
                boolean beaten = attackBeaten;
                attackBeaten = false;
                return beaten;
            }

            public int getHealthLeft() {
                return healthLeft;
            }

            /** True for a moment after it's hit, so it can be drawn flashing. */
            public boolean isFlashing() {
                return hitFlashLeft > 0;
            }

            /** Seconds it has been in its current phase. */
            public double getPhaseTime() {
                return phaseTime;
            }

            /** Keeps the first direction it's given this phase and returns it (for Aim.ONCE). */
            double lockAim(int layer, double angle) {
                int slot = slot(layer);
                if (Double.isNaN(lockedAim[slot])) {
                    lockedAim[slot] = angle;
                }
                return lockedAim[slot];
            }

            double getSpinAngle(int layer) {
                return spinAngle[slot(layer)];
            }

            void addSpin(int layer, double degrees) {
                int slot = slot(layer);
                spinAngle[slot] = (spinAngle[slot] + degrees) % 360;
            }

            /** Where a pattern's own aim, spin and timer are kept: its place within the attack. */
            private int slot(int layer) {
                return Math.max(0, Math.min(MAX_PHASES - 1, layer - design.attackStart(layer)));
            }

            /** Moves along the path by speed * deltaSeconds, passing through as many waypoints as that covers. */
            private void flyPath(double deltaSeconds) {
                hitFlashLeft = Math.max(0, hitFlashLeft - deltaSeconds);
                if (appearTimeLeft > 0) {
                    appearTimeLeft = Math.max(0, appearTimeLeft - deltaSeconds);
                    return; // holds still while it fades in
                }
                if (fighting || leaving) {
                    bossTurn(deltaSeconds);
                    return;
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
                    } else if (design.isLoop() && !design.isBoss() && !path.isEmpty()) {
                        point = new double[]{design.getStartX(), design.getStartY()};
                    } else {
                        pathFinished = true; // end of the path: stay put
                        // A formation whose path ends off-screen keeps going until every member is off too
                        boolean endsOffScreen = x < 0 || x > PLAYFIELD_WIDTH || y < 0 || y > PLAYFIELD_HEIGHT;
                        if (design.isBoss() && endsOffScreen) {
                            // A boss fights on screen: one whose path ends off the edge comes in to the nearest spot
                            x = clampX(x);
                            y = Math.max(40, Math.min(PLAYFIELD_HEIGHT / 2.0, y));
                        } else if (endsOffScreen && design.getFormation() != Formation.LINE) {
                            x += headingX * distanceLeft;
                            y += headingY * distanceLeft;
                        }
                        return;
                    }
                    double dx = point[0] - x;
                    double dy = point[1] - y;
                    double distance = Math.hypot(dx, dy);
                    if (distance > 0) {
                        headingX = dx / distance;
                        headingY = dy / distance;
                        turnToward(Math.atan2(headingY, headingX) - design.formationFront(), deltaSeconds);
                    }
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

            /** Flies along its path (see {@link #flyPath}); a boss starts its attacks once the path is done. */
            void fly(double deltaSeconds) {
                double oldX = getX();
                double oldY = getY();
                flyPath(deltaSeconds);
                // The way it's flying, for pointing its picture along the path; it turns smoothly, not in jumps
                double dx = getX() - oldX;
                double dy = getY() - oldY;
                if (Math.hypot(dx, dy) > 0.01) {
                    double target = Math.atan2(dy, dx);
                    if (Double.isNaN(heading)) {
                        heading = target;
                    } else {
                        double difference = Math.IEEEremainder(target - heading, 2 * Math.PI);
                        double most = Math.toRadians(ENEMY_TURN_SPEED) * deltaSeconds;
                        heading += Math.max(-most, Math.min(most, difference));
                    }
                }
                if (design.isBoss() && pathFinished && !fighting && !leaving && appearTimeLeft <= 0) {
                    bossTurn(0);
                }
            }

            /** Turns a facing formation a little toward the given angle, so it swings round smoothly. */
            private void turnToward(double target, double deltaSeconds) {
                if (!design.isFacesMovement()) {
                    turn = 0;
                    return;
                }
                double difference = Math.IEEEremainder(target - turn, 2 * Math.PI);
                double most = Math.toRadians(FORMATION_TURN_SPEED) * deltaSeconds;
                turn += Math.max(-most, Math.min(most, difference));
            }

            private double[] offset() {
                double cos = Math.cos(turn);
                double sin = Math.sin(turn);
                return new double[]{baseOffset[0] * cos - baseOffset[1] * sin, baseOffset[0] * sin + baseOffset[1] * cos};
            }

            /** True once the enemy has flown its whole path and ended up off-screen, so it can be removed. */
            boolean hasLeftScreen() {
                if (design.isBoss()) {
                    return leaving && getY() < -Alien.HEIGHT * BOSS_SCALE;
                }
                return pathFinished && (getX() < -Alien.WIDTH / 2 || getX() > PLAYFIELD_WIDTH + Alien.WIDTH / 2
                        || getY() < -Alien.HEIGHT / 2 || getY() > PLAYFIELD_HEIGHT + Alien.HEIGHT / 2);
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
                return x + offset()[0];
            }

            /** Degrees to turn its picture so it points along its path (0 when upright or not moving yet). */
            public double getPictureAngle() {
                if (Double.isNaN(heading) || design.isBoss() || design.getPointing() == Pointing.UPRIGHT) {
                    return 0;
                }
                // The picture's head is at its top (pointing up, -90 degrees)
                double angle = Math.toDegrees(heading) + 90;
                return design.getPointing() == Pointing.HEAD_FIRST ? angle : angle + 180;
            }

            public double getY() {
                return y + offset()[1];
            }
        }
    }
}
