package edu.farmingdale.csc311capstone.SpaceInvaders;

import java.util.ArrayList;
import java.util.List;

/**
 * Game rules and state for classic Space Invaders. Kept free of JavaFX so it can be unit tested.
 *
 * Step 1 (graphics): only the starting layout. The cannon, the alien formation and the shields
 * are placed where they belong so the controller can draw them. Nothing moves yet.
 */
public final class SpaceInvadersMechanics {
    public static final double PLAYFIELD_WIDTH = 560;
    public static final double PLAYFIELD_HEIGHT = 560;

    // Player cannon: sits along the bottom
    public static final double PLAYER_WIDTH = 36;
    public static final double PLAYER_HEIGHT = 16;
    public static final double PLAYER_Y = PLAYFIELD_HEIGHT - 30;   // center of the cannon
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
    private int lives = STARTING_LIVES;
    private int score = 0;
    private int wave = 1;

    private final List<Alien> aliens = new ArrayList<>();
    private final List<Shield> shields = new ArrayList<>();

    public SpaceInvadersMechanics() {
        startWave();
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
     * A bunker the player hides behind, built from a grid of small blocks
     * (so later steps can knock them out one at a time).
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
