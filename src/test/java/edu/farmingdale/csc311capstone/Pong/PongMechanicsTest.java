package edu.farmingdale.csc311capstone.Pong;

import static edu.farmingdale.csc311capstone.Pong.PongConfig.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

class PongMechanicsTest {
    private PongMechanics playing() {
        PongMechanics m = new PongMechanics();
        m.update(READY_SECONDS, 0, 0);
        return m;
    }

    private PongMechanics blitz() {
        PongMechanics m =
                new PongMechanics(
                        new PongConfig(Mode.BLITZ, Opponent.HUMAN, Difficulty.MEDIUM), 311);
        m.update(READY_SECONDS, 0, 0);
        return m;
    }

    private PongMechanics ai(Difficulty d) {
        PongMechanics m =
                new PongMechanics(new PongConfig(Mode.PLAYER_VS_AI, Opponent.CPU, d), 311);
        m.update(READY_SECONDS, 0, 0);
        return m;
    }

    private void set(PongMechanics m, String name, Object value) {
        try {
            Field f = PongMechanics.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(m, value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void ball(PongMechanics m, double x, double y, double vx, double vy) {
        set(m, "ballX", x);
        set(m, "ballY", y);
        set(m, "velocityX", vx);
        set(m, "velocityY", vy);
    }

    private void point(PongMechanics m, boolean leftWins) {
        set(m, "state", PongMechanics.State.PLAYING);
        ball(m, leftWins ? WIDTH + 20 : -20, 200, leftWins ? 300 : -300, 0);
        assertTrue(m.checkScore(WIDTH, HEIGHT, BALL_RADIUS));
    }

    @Test
    void paddleMovesWithDeltaTime() {
        assertEquals(140, PongMechanics.movePaddle(100, 88, 480, 1, .1), 1e-6);
    }

    @Test
    void paddleClampsTop() {
        assertEquals(0, PongMechanics.movePaddle(10, 88, 480, -1, 1));
    }

    @Test
    void paddleClampsBottom() {
        assertEquals(392, PongMechanics.movePaddle(380, 88, 480, 1, 1));
    }

    @Test
    void simultaneousInputMovesBothPaddles() {
        var m = playing();
        m.update(.1, -1, 1);
        assertTrue(m.getLeftY() < 196);
        assertTrue(m.getRightY() > 196);
    }

    @Test
    void ballMovementUsesSeconds() {
        var m = playing();
        ball(m, 400, 240, 300, 100);
        m.moveBall(.2);
        assertEquals(460, m.getBallX());
        assertEquals(260, m.getBallY());
    }

    @Test
    void readyCountdownBlocksMovement() {
        var m = new PongMechanics();
        m.update(1, 1, 1);
        assertEquals(WIDTH / 2, m.getBallX());
        assertEquals(PongMechanics.State.READY, m.getState());
    }

    @Test
    void topWallReflectsAndSeparates() {
        var m = playing();
        ball(m, 400, 2, 300, -120);
        m.checkWallCollision(HEIGHT, 8);
        assertEquals(8, m.getBallY());
        assertEquals(120, m.getVelocityY());
    }

    @Test
    void bottomWallReflectsAndSeparates() {
        var m = playing();
        ball(m, 400, 479, 300, 120);
        m.checkWallCollision(HEIGHT, 8);
        assertEquals(472, m.getBallY());
        assertEquals(-120, m.getVelocityY());
    }

    @Test
    void wallsDoNotReflectAnOutgoingBallAgain() {
        var m = playing();
        ball(m, 400, 8, 300, 120);
        m.checkWallCollision(HEIGHT, 8);
        assertEquals(120, m.getVelocityY());
    }

    @Test
    void leftPaddleReflectsAndSeparates() {
        var m = playing();
        ball(m, 50, 240, -345, 0);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertTrue(m.getVelocityX() > 0);
        assertTrue(m.getBallX() > 52);
    }

    @Test
    void rightPaddleReflects() {
        var m = playing();
        ball(m, 830, 240, 345, 0);
        m.checkPaddleCollision(RIGHT_X, 196, 14, 88, 8, false);
        assertTrue(m.getVelocityX() < 0);
    }

    @Test
    void edgeHitChangesAngle() {
        var m = playing();
        ball(m, 50, 200, -345, 0);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertTrue(m.getVelocityY() < 0);
        assertTrue(Math.abs(m.getVelocityX()) >= MIN_HORIZONTAL_SPEED);
    }

    @Test
    void centerHitIsHorizontal() {
        var m = playing();
        ball(m, 50, 240, -345, 0);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertEquals(0, m.getVelocityY(), 1e-6);
    }

    @Test
    void speedAcceleratesAndIsCapped() {
        var m = playing();
        ball(m, 50, 240, -775, 0);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertEquals(MAX_SPEED, Math.hypot(m.getVelocityX(), m.getVelocityY()), 1e-6);
    }

    @Test
    void ballDoesNotGetTrappedInsidePaddle() {
        var m = playing();
        ball(m, 50, 240, -345, 0);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        double vx = m.getVelocityX();
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertEquals(vx, m.getVelocityX());
    }

    @Test
    void smallPhysicsStepsPreventTunnelling() {
        var m = playing();
        ball(m, 110, 240, -780, 0);
        m.update(.12, 0, 0);
        assertTrue(m.getVelocityX() > 0);
        assertEquals(0, m.getPlayer2Score());
    }

    @Test
    void scoresOnlyBeyondEntireBall() {
        var m = playing();
        ball(m, -7, 240, -300, 0);
        assertFalse(m.checkScore(WIDTH, HEIGHT, 8));
        ball(m, -9, 240, -300, 0);
        assertTrue(m.checkScore(WIDTH, HEIGHT, 8));
        assertEquals(1, m.getPlayer2Score());
    }

    @Test
    void pointResetsBallAndStartsCountdown() {
        var m = playing();
        point(m, true);
        assertEquals(WIDTH / 2, m.getBallX());
        assertEquals(HEIGHT / 2, m.getBallY());
        assertEquals(PongMechanics.State.READY, m.getState());
    }

    @Test
    void servesAlternate() {
        var m = playing();
        double vx = m.getVelocityX();
        point(m, true);
        assertEquals(-Math.signum(vx), Math.signum(m.getVelocityX()));
        point(m, true);
        assertEquals(Math.signum(vx), Math.signum(m.getVelocityX()));
    }

    @Test
    void firstToSevenFinishes() {
        var m = playing();
        for (int i = 0; i < 7; i++) point(m, true);
        assertTrue(m.isGameOver());
        assertEquals(1, m.getWinner());
        assertEquals(7, m.getResult().orElseThrow().playerScore());
    }

    @Test
    void opponentCanWin() {
        var m = playing();
        for (int i = 0; i < 7; i++) point(m, false);
        assertEquals(2, m.getWinner());
        assertEquals(PongResult.Outcome.LOSS, m.getResult().orElseThrow().outcome());
    }

    @Test
    void restartClearsEveryMatchStatistic() {
        var m = playing();
        point(m, true);
        m.resetGame();
        assertEquals(0, m.getPlayer1Score());
        assertEquals(60, m.getRemainingSeconds());
        assertTrue(m.getResult().isEmpty());
        assertEquals(PongMechanics.State.READY, m.getState());
    }

    @Test
    void pauseAndResumeFreezePhysics() {
        var m = playing();
        double x = m.getBallX();
        m.togglePause();
        m.update(10, 1, 1);
        assertEquals(x, m.getBallX());
        m.togglePause();
        m.update(.1, 0, 0);
        assertNotEquals(x, m.getBallX());
    }

    @Test
    void pausedReadyCountdownFreezes() {
        var m = new PongMechanics();
        m.togglePause();
        m.update(2, 0, 0);
        assertEquals(READY_SECONDS, m.getReadySeconds());
        m.togglePause();
        assertEquals(PongMechanics.State.READY, m.getState());
    }

    @Test
    void aiHonorsReactionDelay() {
        var m = ai(Difficulty.EASY);
        ball(m, 400, 440, 300, 0);
        double y = m.getRightY();
        m.update(.2, 0, 0);
        assertEquals(y, m.getRightY(), 1e-6);
        m.update(.2, 0, 0);
        assertTrue(m.getRightY() > y);
    }

    @Test
    void aiMovesNaturallyAndWithinBoundaries() {
        var m = ai(Difficulty.HARD);
        ball(m, 450, 460, 300, 0);
        double y = m.getRightY();
        m.update(.2, 0, 0);
        assertTrue(Math.abs(m.getRightY() - y) <= Difficulty.HARD.speed * .2 + 1e-6);
        m.update(10, 0, 0);
        assertTrue(m.getRightY() >= 0 && m.getRightY() <= HEIGHT - PADDLE_HEIGHT);
    }

    @Test
    void difficultyHasDistinctSpeedDelayAndError() {
        assertTrue(Difficulty.EASY.speed < Difficulty.MEDIUM.speed);
        assertTrue(Difficulty.MEDIUM.speed < Difficulty.HARD.speed);
        assertTrue(Difficulty.EASY.reactionSeconds > Difficulty.HARD.reactionSeconds);
        assertTrue(Difficulty.EASY.error > Difficulty.HARD.error);
    }

    @Test
    void aiDifficultiesProduceDifferentMotion() {
        var easy = ai(Difficulty.EASY);
        var hard = ai(Difficulty.HARD);
        ball(easy, 450, 440, 300, 0);
        ball(hard, 450, 440, 300, 0);
        easy.update(.25, 0, 0);
        hard.update(.25, 0, 0);
        assertTrue(hard.getRightY() > easy.getRightY());
    }

    @Test
    void predictionIncludesMultipleWallBounces() {
        assertEquals(100, PongMechanics.predictIntercept(100, 928, 1, 480, 8), 1e-6);
        assertEquals(244, PongMechanics.predictIntercept(100, 300, 2, 480, 8), 1e-6);
    }

    @Test
    void blitzIgnoresFirstToSeven() {
        var m = blitz();
        for (int i = 0; i < 9; i++) point(m, true);
        assertFalse(m.isGameOver());
    }

    @Test
    void blitzCountdownUsesActiveTime() {
        var m = blitz();
        m.update(.5, 0, 0);
        assertEquals(59.5, m.getRemainingSeconds(), 1e-6);
    }

    @Test
    void blitzTimerContinuesDuringServeCountdown() {
        var m = blitz();
        point(m, true);
        m.update(.5, 0, 0);
        assertEquals(59.5, m.getRemainingSeconds(), 1e-6);
    }

    @Test
    void blitzPauseFreezesTimer() {
        var m = blitz();
        m.togglePause();
        m.update(20, 0, 0);
        assertEquals(60, m.getRemainingSeconds());
    }

    @Test
    void blitzRestartRestoresTime() {
        var m = blitz();
        m.update(.5, 0, 0);
        m.resetGame();
        assertEquals(60, m.getRemainingSeconds());
    }

    @Test
    void blitzExpiresWithDraw() {
        var m = blitz();
        ball(m, 440, 240, 0, 0);
        m.update(60, 0, 0);
        assertTrue(m.isGameOver());
        assertEquals(PongResult.Outcome.DRAW, m.getResult().orElseThrow().outcome());
        assertEquals(0, m.getRemainingSeconds(), 1e-6);
    }

    @Test
    void blitzWinnerUsesHighestScore() {
        var m = blitz();
        set(m, "player1Score", 2);
        set(m, "player2Score", 4);
        ball(m, 440, 240, 0, 0);
        m.update(60, 0, 0);
        assertEquals(2, m.getWinner());
    }

    @Test
    void resultDeliveredExactlyOnceAndFrozen() {
        var m = blitz();
        ball(m, 440, 240, 0, 0);
        m.update(60, 0, 0);
        var r = m.consumeResult().orElseThrow();
        assertTrue(m.consumeResult().isEmpty());
        m.update(100, 1, 1);
        assertSame(r, m.getResult().orElseThrow());
        assertEquals(60, r.durationSeconds(), 1e-6);
    }

    @Test
    void resultIncludesModeOpponentAndDifficulty() {
        var m = new PongMechanics(new PongConfig(Mode.BLITZ, Opponent.CPU, Difficulty.HARD), 4);
        set(m, "elapsed", 59.999);
        set(m, "state", PongMechanics.State.PLAYING);
        m.update(.01, 0, 0);
        var r = m.getResult().orElseThrow();
        assertEquals("Pong", r.gameName());
        assertEquals("HARD", r.difficulty());
        assertEquals(Opponent.CPU, r.opponentType());
    }

    @Test
    void nearVerticalShotGetsMinimumHorizontalSpeed() {
        var m = playing();
        ball(m, 50, 196, -1, -345);
        m.checkPaddleCollision(30, 196, 14, 88, 8, true);
        assertEquals(MIN_HORIZONTAL_SPEED, m.getVelocityX(), 1e-6);
        assertTrue(Math.hypot(m.getVelocityX(), m.getVelocityY()) <= MAX_SPEED);
    }

    @Test
    void invalidDeltaIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> playing().update(Double.NaN, 0, 0));
    }

    @Test
    void framePartitionDoesNotChangePhysics() {
        var a = playing();
        var b = playing();
        a.update(.2, 0, 0);
        for (int i = 0; i < 20; i++) b.update(.01, 0, 0);
        assertEquals(a.getBallX(), b.getBallX(), 1e-6);
        assertEquals(a.getBallY(), b.getBallY(), 1e-6);
    }
}
