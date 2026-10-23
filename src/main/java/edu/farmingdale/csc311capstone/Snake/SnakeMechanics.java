package edu.farmingdale.csc311capstone.Snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * All of the Snake rules, with no JavaFX code, so it can be unit tested.
 * The controller calls {@link #step()} once every {@link #tickIntervalMs()} milliseconds
 * and redraws the board from the getters.
 */
public final class SnakeMechanics {

    public record Point(int x, int y) { }

    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        public final int dx;
        public final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case LEFT -> RIGHT;
                case RIGHT -> LEFT;
            };
        }
    }

    public enum State { READY, RUNNING, PAUSED, GAME_OVER, WON }

    // Tunable numbers
    public static final int DEFAULT_WIDTH = 24;
    public static final int DEFAULT_HEIGHT = 20;
    public static final int STARTING_LENGTH = 3;
    public static final int NORMAL_TICK_MS = 120;
    public static final int SPEED_RUSH_START_MS = 140;
    public static final int SPEED_RUSH_MIN_MS = 45;
    public static final int SPEED_RUSH_MS_PER_APPLE = 7;
    public static final int TIME_ATTACK_TICK_MS = 100;
    public static final int TIME_ATTACK_START_MS = 60_000;
    public static final int TIME_ATTACK_BONUS_MS = 2_000;
    public static final int GOLDEN_APPLE_POINTS = 5;
    public static final int GOLDEN_APPLE_LIFETIME_TICKS = 45;
    public static final int GOLDEN_APPLE_CHANCE = 4;        // 1 in 4 after each normal apple
    public static final int OBSTACLES_AT_START = 4;
    public static final int OBSTACLE_SAFE_DISTANCE = 4;     // rocks never spawn this close to the head

    private final int width;
    private final int height;
    private final Mode mode;
    private final Random random;

    private final Deque<Point> body = new ArrayDeque<>();     // head is first
    private final Deque<Direction> pendingTurns = new ArrayDeque<>();
    private final Set<Point> obstacles = new LinkedHashSet<>();

    private Direction direction = Direction.RIGHT;
    private State state = State.READY;
    private Point food;
    private Point goldenApple;
    private int goldenTicksLeft;
    private int score;
    private int applesEaten;
    private long timeLeftMs;
    private String endReason = "";

    public SnakeMechanics(Mode mode) {
        this(mode, DEFAULT_WIDTH, DEFAULT_HEIGHT, new Random());
    }

    public SnakeMechanics(Mode mode, int width, int height, Random random) {
        if (width < 8 || height < 6) {
            throw new IllegalArgumentException("Board too small");
        }
        this.mode = mode;
        this.width = width;
        this.height = height;
        this.random = random;
        reset();
    }

    /** Puts everything back to the starting position. */
    public void reset() {
        body.clear();
        pendingTurns.clear();
        obstacles.clear();
        direction = Direction.RIGHT;
        state = State.READY;
        goldenApple = null;
        goldenTicksLeft = 0;
        score = 0;
        applesEaten = 0;
        endReason = "";
        timeLeftMs = mode == Mode.TIME_ATTACK ? TIME_ATTACK_START_MS : 0;

        int startX = width / 4 + STARTING_LENGTH - 1;
        int startY = height / 2;
        for (int i = 0; i < STARTING_LENGTH; i++) {
            body.addLast(new Point(startX - i, startY));
        }
        if (mode == Mode.OBSTACLES) {
            for (int i = 0; i < OBSTACLES_AT_START; i++) {
                addObstacle();
            }
        }
        food = randomEmptyCell();
    }

    // ------------------------------------------------------------------ input

    public void start() {
        if (state == State.READY || state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    public void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        } else if (state == State.PAUSED || state == State.READY) {
            state = State.RUNNING;
        }
    }

    /**
     * Queues a turn. Up to two turns are buffered so quick key presses
     * (like a fast U-turn: UP then LEFT) aren't lost between ticks.
     * Reversing straight into yourself is ignored.
     */
    public void turn(Direction newDirection) {
        Direction last = pendingTurns.isEmpty() ? direction : pendingTurns.peekLast();
        if (newDirection == last || newDirection == last.opposite()) {
            return;
        }
        if (pendingTurns.size() < 2) {
            pendingTurns.addLast(newDirection);
        }
    }

    // ------------------------------------------------------------------ game loop

    /** Advances the game by one move. */
    public void step() {
        if (state != State.RUNNING) {
            return;
        }

        if (mode == Mode.TIME_ATTACK) {
            timeLeftMs -= tickIntervalMs();
            if (timeLeftMs <= 0) {
                timeLeftMs = 0;
                end(State.GAME_OVER, "Time's up!");
                return;
            }
        }

        if (!pendingTurns.isEmpty()) {
            direction = pendingTurns.pollFirst();
        }

        Point head = body.peekFirst();
        int nx = head.x() + direction.dx;
        int ny = head.y() + direction.dy;

        if (wrapsAround()) {
            nx = Math.floorMod(nx, width);
            ny = Math.floorMod(ny, height);
        } else if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
            end(State.GAME_OVER, "You hit the wall.");
            return;
        }
        Point next = new Point(nx, ny);

        if (obstacles.contains(next)) {
            end(State.GAME_OVER, "You crashed into a rock.");
            return;
        }

        boolean eatsApple = next.equals(food);
        boolean eatsGolden = next.equals(goldenApple);
        boolean grows = eatsApple || eatsGolden;

        // The tail moves out of the way this tick unless we're growing.
        Point tail = body.peekLast();
        if (body.contains(next) && (grows || !next.equals(tail))) {
            end(State.GAME_OVER, "You ran into yourself.");
            return;
        }

        body.addFirst(next);
        if (!grows) {
            body.removeLast();
        }

        if (eatsGolden) {
            score += GOLDEN_APPLE_POINTS;
            goldenApple = null;
        }
        if (eatsApple) {
            score += pointsPerApple();
            applesEaten++;
            onAppleEaten();
            if (state != State.RUNNING) {
                return; // the board filled up
            }
        }

        if (goldenApple != null && --goldenTicksLeft <= 0) {
            goldenApple = null;
        }
    }

    private void onAppleEaten() {
        if (mode == Mode.TIME_ATTACK) {
            timeLeftMs += TIME_ATTACK_BONUS_MS;
            if (goldenApple == null && random.nextInt(GOLDEN_APPLE_CHANCE) == 0) {
                goldenApple = randomEmptyCell();
                goldenTicksLeft = GOLDEN_APPLE_LIFETIME_TICKS;
            }
        }
        if (mode == Mode.OBSTACLES) {
            addObstacle();
        }
        food = randomEmptyCell();
        if (food == null) {
            end(State.WON, "You filled the whole board!");
        }
    }

    private void end(State finalState, String reason) {
        state = finalState;
        endReason = reason;
        pendingTurns.clear();
    }

    private void addObstacle() {
        Point head = body.peekFirst();
        List<Point> options = new ArrayList<>();
        for (Point p : emptyCells()) {
            int distance = Math.abs(p.x() - head.x()) + Math.abs(p.y() - head.y());
            if (distance >= OBSTACLE_SAFE_DISTANCE) {
                options.add(p);
            }
        }
        if (!options.isEmpty()) {
            obstacles.add(options.get(random.nextInt(options.size())));
        }
    }

    private Point randomEmptyCell() {
        List<Point> empty = emptyCells();
        return empty.isEmpty() ? null : empty.get(random.nextInt(empty.size()));
    }

    private List<Point> emptyCells() {
        List<Point> empty = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point p = new Point(x, y);
                if (!body.contains(p) && !obstacles.contains(p)
                        && !p.equals(food) && !p.equals(goldenApple)) {
                    empty.add(p);
                }
            }
        }
        return empty;
    }

    // ------------------------------------------------------------------ queries

    /** How long the controller should wait between steps. */
    public int tickIntervalMs() {
        return switch (mode) {
            case SPEED_RUSH -> Math.max(SPEED_RUSH_MIN_MS,
                    SPEED_RUSH_START_MS - SPEED_RUSH_MS_PER_APPLE * applesEaten);
            case TIME_ATTACK -> TIME_ATTACK_TICK_MS;
            default -> NORMAL_TICK_MS;
        };
    }

    /** Speed Rush pays more as the snake speeds up. */
    public int pointsPerApple() {
        return mode == Mode.SPEED_RUSH ? 1 + applesEaten / 5 : 1;
    }

    public boolean wrapsAround() {
        return mode == Mode.WRAP_AROUND || mode == Mode.TIME_ATTACK;
    }

    public boolean isOver() {
        return state == State.GAME_OVER || state == State.WON;
    }

    public Mode mode() { return mode; }
    public State state() { return state; }
    public int width() { return width; }
    public int height() { return height; }
    public Point head() { return body.peekFirst(); }
    public List<Point> body() { return Collections.unmodifiableList(new ArrayList<>(body)); }
    public int length() { return body.size(); }
    public Direction direction() { return direction; }
    public Point food() { return food; }
    public Point goldenApple() { return goldenApple; }
    public int goldenTicksLeft() { return goldenTicksLeft; }
    public Set<Point> obstacles() { return Collections.unmodifiableSet(obstacles); }
    public int score() { return score; }
    public int applesEaten() { return applesEaten; }
    public long timeLeftMs() { return timeLeftMs; }
    public String endReason() { return endReason; }

    // ------------------------------------------------------------------ test hooks

    void placeFood(Point p) { food = p; }
    void placeGoldenApple(Point p, int ticks) { goldenApple = p; goldenTicksLeft = ticks; }
    void placeObstacle(Point p) { obstacles.add(p); }
    void clearObstacles() { obstacles.clear(); }

    // ================================================================== game modes

    /**
     * The different ways to play Snake.
     */
    public enum Mode {

        CLASSIC("Classic",
                "Eat apples to grow. Don't hit the walls or yourself."),

        WRAP_AROUND("Wrap-Around",
                "No walls! Leave one side of the board and come back on the other. "
                        + "Your own tail is the only thing that can stop you."),

        SPEED_RUSH("Speed Rush",
                "Every apple makes the snake faster. Apples are worth more the faster you go."),

        OBSTACLES("Obstacles",
                "Every apple you eat drops a new rock somewhere on the board. "
                        + "The board gets more crowded the better you do."),

        TIME_ATTACK("Time Attack",
                "60 seconds on the clock. Apples add 2 seconds. Golden apples are worth 5 points "
                        + "but disappear fast. Walls wrap around.");

        private final String displayName;
        private final String description;

        Mode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() { return displayName; }
        public String description() { return description; }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
