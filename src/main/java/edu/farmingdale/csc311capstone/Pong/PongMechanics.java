package edu.farmingdale.csc311capstone.Pong;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import static edu.farmingdale.csc311capstone.Pong.PongConfig.*;

/** Deterministic, sub-stepped physics and match state. Time only advances through update(). */
public class PongMechanics {
    public enum State { READY, PLAYING, PAUSED, FINISHED }
    public enum Event { PADDLE, WALL, SCORE, COUNTDOWN, START, FINISH }
    private final PongConfig config;
    private final Random random;
    private final List<Event> events = new ArrayList<>();
    private double ballX, ballY, velocityX, velocityY, leftY, rightY;
    private double ready, elapsed, aiDelay, aiTarget;
    private int player1Score, player2Score, hits, rally, longestRally, serve;
    private State state, beforePause;
    private PongResult result;
    private boolean resultConsumed;

    public PongMechanics() { this(PongConfig.local(), 311); }
    public PongMechanics(PongConfig config, long seed) {
        this.config = config; this.random = new Random(seed); resetGame();
    }
    public void update(double seconds, int leftDirection, int rightDirection) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid delta time");
        if (state == State.PAUSED || state == State.FINISHED) return;
        // Small steps prevent tunnelling at maximum speed, including slow display frames.
        while (seconds > 1e-9 && state != State.FINISHED) {
            double dt = Math.min(seconds, 1.0 / 240); seconds -= dt;
            if (state == State.READY) {
                // Blitz keeps ticking between points, but excludes the initial match countdown.
                if (config.mode() == Mode.BLITZ && player1Score + player2Score > 0) {
                    elapsed = Math.min(BLITZ_SECONDS, elapsed + dt);
                    if (elapsed >= BLITZ_SECONDS - 1e-8) { finish(); continue; }
                }
                int previous = (int) Math.ceil(ready);
                ready = Math.max(0, ready - dt);
                if ((int) Math.ceil(ready) < previous) emit(Event.COUNTDOWN);
                if (ready <= 1e-9) { state = State.PLAYING; emit(Event.START); }
                continue;
            }
            double active = config.mode() == Mode.BLITZ ? Math.min(dt, Math.max(0, BLITZ_SECONDS - elapsed)) : dt;
            leftY = movePaddle(leftY, PADDLE_HEIGHT, HEIGHT, leftDirection, active);
            if (config.opponent() == Opponent.CPU) updateAI(active);
            else rightY = movePaddle(rightY, PADDLE_HEIGHT, HEIGHT, rightDirection, active);
            elapsed += active;
            moveBall(active); checkWallCollision(HEIGHT, BALL_RADIUS);
            checkPaddleCollision(LEFT_X, leftY, PADDLE_WIDTH, PADDLE_HEIGHT, BALL_RADIUS, true);
            checkPaddleCollision(RIGHT_X, rightY, PADDLE_WIDTH, PADDLE_HEIGHT, BALL_RADIUS, false);
            checkScore(WIDTH, HEIGHT, BALL_RADIUS);
            if (config.mode() == Mode.BLITZ && elapsed >= BLITZ_SECONDS - 1e-8) finish();
        }
    }
    private void updateAI(double dt) {
        aiDelay -= dt;
        if (aiDelay <= 0) {
            aiDelay += config.difficulty().reactionSeconds;
            double target = velocityX > 0 ? ballY : HEIGHT / 2;
            if (config.difficulty() == Difficulty.HARD && velocityX > 0)
                target = predictIntercept(ballY, velocityY, (RIGHT_X - BALL_RADIUS - ballX) / velocityX, HEIGHT, BALL_RADIUS);
            aiTarget = target + (random.nextDouble() * 2 - 1) * config.difficulty().error;
        }
        double distance = aiTarget - (rightY + PADDLE_HEIGHT / 2);
        double movement = Math.copySign(Math.min(Math.abs(distance), config.difficulty().speed * dt), distance);
        rightY = clamp(rightY + movement, 0, HEIGHT - PADDLE_HEIGHT);
    }
    /** Fold an unbounded trajectory into the playable vertical interval after any number of bounces. */
    public static double predictIntercept(double y, double vy, double seconds, double height, double radius) {
        double span = height - radius * 2;
        double unfolded = y - radius + vy * Math.max(0, seconds);
        double phase = ((unfolded % (span * 2)) + span * 2) % (span * 2);
        return radius + (phase <= span ? phase : span * 2 - phase);
    }
    public static double movePaddle(double y, double height, double fieldHeight, int direction, double dt) {
        return clamp(y + Math.signum(direction) * PADDLE_SPEED * Math.max(0, dt), 0, fieldHeight - height);
    }
    public void moveBall(double dt) {
        if (state != State.PLAYING) return;
        ballX += velocityX * dt; ballY += velocityY * dt;
    }
    public void checkWallCollision(double height, double radius) {
        if (state != State.PLAYING) return;
        if (ballY <= radius && velocityY < 0) { ballY = radius; velocityY = -velocityY; emit(Event.WALL); }
        if (ballY >= height - radius && velocityY > 0) { ballY = height - radius; velocityY = -velocityY; emit(Event.WALL); }
    }
    public void checkPaddleCollision(double x, double y, double width, double height, double radius, boolean left) {
        if (state != State.PLAYING || (left ? velocityX >= 0 : velocityX <= 0)) return;
        if (ballX + radius < x || ballX - radius > x + width || ballY + radius < y || ballY - radius > y + height) return;
        double offset = clamp((ballY - y - height / 2) / (height / 2), -1, 1);
        double speed = Math.min(MAX_SPEED, Math.hypot(velocityX, velocityY) * 1.055);
        double angle = offset * Math.toRadians(62);
        velocityX = (left ? 1 : -1) * Math.max(MIN_HORIZONTAL_SPEED, speed * Math.cos(angle));
        velocityY = Math.copySign(Math.sqrt(Math.max(0, speed * speed - velocityX * velocityX)), offset);
        ballX = left ? x + width + radius + .01 : x - radius - .01;
        hits++; rally++; longestRally = Math.max(longestRally, rally); emit(Event.PADDLE);
    }
    public boolean checkScore(double width, double height, double radius) {
        if (state != State.PLAYING) return false;
        if (ballX + radius < 0) player2Score++;
        else if (ballX - radius > width) player1Score++;
        else return false;
        emit(Event.SCORE); rally = 0;
        if (config.mode() != Mode.BLITZ && Math.max(player1Score, player2Score) >= WINNING_SCORE) finish();
        else { resetBall(width, height); ready = SERVE_SECONDS; state = State.READY; }
        return true;
    }
    private void resetBall(double width, double height) {
        ballX = width / 2; ballY = height / 2;
        // Alternating serves give both players the same starting opportunities.
        velocityX = (++serve % 2 == 0 ? -1 : 1) * INITIAL_SPEED * .94;
        velocityY = (serve % 3 == 0 ? -1 : 1) * INITIAL_SPEED * Math.sqrt(1 - .94 * .94);
        aiTarget = HEIGHT / 2; aiDelay = config.difficulty().reactionSeconds;
    }
    private void finish() {
        if (result != null) return;
        state = State.FINISHED;
        result = new PongResult("Pong", config.mode(), config.opponent() == Opponent.CPU ? config.difficulty().name() : "N/A",
                config.opponent(), player1Score, player2Score,
                player1Score == player2Score ? PongResult.Outcome.DRAW : player1Score > player2Score ? PongResult.Outcome.WIN : PongResult.Outcome.LOSS,
                elapsed, hits, longestRally);
        emit(Event.FINISH);
    }
    public void togglePause() {
        if (state == State.FINISHED) return;
        if (state == State.PAUSED) state = beforePause;
        else { beforePause = state; state = State.PAUSED; }
    }
    public void resetGame() {
        events.clear(); result = null; resultConsumed = false;
        player1Score = player2Score = hits = rally = longestRally = serve = 0;
        elapsed = 0; leftY = rightY = (HEIGHT - PADDLE_HEIGHT) / 2;
        resetBall(WIDTH, HEIGHT); ready = READY_SECONDS; state = State.READY;
    }
    private void emit(Event event) { if (events.size() < 128) events.add(event); }
    public List<Event> drainEvents() { List<Event> copy = List.copyOf(events); events.clear(); return copy; }
    public Optional<PongResult> getResult() { return Optional.ofNullable(result); }
    public Optional<PongResult> consumeResult() {
        if (resultConsumed || result == null) return Optional.empty();
        resultConsumed = true; return Optional.of(result);
    }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(v, max)); }
    public PongConfig getConfig() { return config; }
    public State getState() { return state; }
    public double getBallX() { return ballX; }
    public double getBallY() { return ballY; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getLeftY() { return leftY; }
    public double getRightY() { return rightY; }
    public double getReadySeconds() { return ready; }
    public double getRemainingSeconds() { return Math.max(0, BLITZ_SECONDS - elapsed); }
    public int getPlayer1Score() { return player1Score; }
    public int getPlayer2Score() { return player2Score; }
    public boolean isGameOver() { return state == State.FINISHED; }
    public int getWinner() { return !isGameOver() || player1Score == player2Score ? 0 : player1Score > player2Score ? 1 : 2; }
}
