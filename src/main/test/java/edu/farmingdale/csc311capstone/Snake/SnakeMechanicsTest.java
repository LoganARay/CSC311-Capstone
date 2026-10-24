package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.State;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SnakeMechanicsTest {

    private static SnakeMechanics newGame(Mode mode) {
        SnakeMechanics game = new SnakeMechanics(mode, 20, 15, new Random(7));
        game.clearObstacles();
        game.placeFood(new Point(0, 0)); // out of the way unless a test moves it
        game.start();
        return game;
    }

    @Test
    void movesOneCellPerStep() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        Point head = game.head();
        game.step();
        assertEquals(new Point(head.x() + 1, head.y()), game.head());
        assertEquals(SnakeMechanics.STARTING_LENGTH, game.length());
    }

    @Test
    void eatingGrowsAndScores() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        Point head = game.head();
        game.placeFood(new Point(head.x() + 1, head.y()));
        game.step();
        assertEquals(SnakeMechanics.STARTING_LENGTH + 1, game.length());
        assertEquals(1, game.score());
        assertNotEquals(game.head(), game.food(), "a new apple should spawn somewhere else");
    }

    @Test
    void cannotReverseIntoItself() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        game.turn(Direction.LEFT);
        game.step();
        assertEquals(Direction.RIGHT, game.direction());
        assertEquals(State.RUNNING, game.state());
    }

    @Test
    void quickDoubleTurnIsBuffered() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        Point head = game.head();
        game.turn(Direction.UP);
        game.turn(Direction.LEFT); // valid because it follows UP
        game.step();
        game.step();
        assertEquals(new Point(head.x() - 1, head.y() - 1), game.head());
        assertEquals(State.RUNNING, game.state());
    }

    @Test
    void wallsKillInClassic() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        for (int i = 0; i < 30 && game.state() == State.RUNNING; i++) {
            game.step();
        }
        assertEquals(State.GAME_OVER, game.state());
        assertTrue(game.endReason().contains("wall"));
    }

    @Test
    void wrapAroundComesOutTheOtherSide() {
        SnakeMechanics game = newGame(Mode.WRAP_AROUND);
        int y = game.head().y();
        for (int i = 0; i < 30; i++) {
            game.step();
        }
        assertEquals(State.RUNNING, game.state());
        assertEquals(y, game.head().y());
        assertTrue(game.head().x() >= 0 && game.head().x() < game.width());
    }

    @Test
    void runningIntoYourselfEndsTheGame() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        // grow to length 5 so a tight loop hits the body
        for (int i = 0; i < 2; i++) {
            Point h = game.head();
            game.placeFood(new Point(h.x() + 1, h.y()));
            game.step();
        }
        game.placeFood(new Point(0, 0));
        game.turn(Direction.DOWN);
        game.step();
        game.turn(Direction.LEFT);
        game.step();
        game.turn(Direction.UP);
        game.step();
        assertEquals(State.GAME_OVER, game.state());
        assertTrue(game.endReason().contains("yourself"));
    }

    @Test
    void followingYourOwnTailIsAllowed() {
        // A length-4 snake moving in a 2x2 square chases its tail without dying.
        SnakeMechanics game = newGame(Mode.CLASSIC);
        Point h = game.head();
        game.placeFood(new Point(h.x() + 1, h.y()));
        game.step();
        game.placeFood(new Point(0, 0));
        Direction[] square = {Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT};
        for (int lap = 0; lap < 3; lap++) {
            for (Direction d : square) {
                game.turn(d);
                game.step();
                assertEquals(State.RUNNING, game.state(), "died on lap " + lap);
            }
        }
    }

    @Test
    void speedRushGetsFasterAndPaysMore() {
        SnakeMechanics game = new SnakeMechanics(Mode.SPEED_RUSH, 40, 10, new Random(1));
        game.start();
        int startInterval = game.tickIntervalMs();
        for (int i = 0; i < 10; i++) {
            Point h = game.head();
            game.placeFood(new Point(h.x() + 1, h.y()));
            game.step();
        }
        assertTrue(game.tickIntervalMs() < startInterval);
        assertEquals(3, game.pointsPerApple()); // 1 + 10 apples / 5
        assertEquals(5 * 1 + 5 * 2, game.score());
    }

    @Test
    void obstaclesModeAddsRocksAndRocksKill() {
        SnakeMechanics fresh = new SnakeMechanics(Mode.OBSTACLES, 20, 15, new Random(3));
        assertEquals(SnakeMechanics.OBSTACLES_AT_START, fresh.obstacles().size());

        SnakeMechanics game = newGame(Mode.OBSTACLES);
        Point h = game.head();
        game.placeFood(new Point(h.x() + 1, h.y()));
        game.step();
        assertEquals(1, game.obstacles().size());
        assertFalse(game.obstacles().contains(game.food()));

        Point ahead = new Point(game.head().x() + 1, game.head().y());
        game.placeObstacle(ahead);
        game.step();
        assertEquals(State.GAME_OVER, game.state());
        assertTrue(game.endReason().contains("rock"));
    }

    @Test
    void timeAttackRunsOutAndApplesAddTime() {
        SnakeMechanics game = newGame(Mode.TIME_ATTACK);
        long start = game.timeLeftMs();
        Point h = game.head();
        game.placeFood(new Point(h.x() + 1, h.y()));
        game.step();
        assertEquals(start - SnakeMechanics.TIME_ATTACK_TICK_MS + SnakeMechanics.TIME_ATTACK_BONUS_MS,
                game.timeLeftMs());

        game.placeFood(new Point(0, 0));
        int steps = 0;
        while (game.state() == State.RUNNING && steps++ < 10_000) {
            game.step();
        }
        assertEquals(State.GAME_OVER, game.state());
        assertEquals("Time's up!", game.endReason());
    }

    @Test
    void goldenAppleIsWorthFive() {
        SnakeMechanics game = newGame(Mode.TIME_ATTACK);
        Point h = game.head();
        game.placeGoldenApple(new Point(h.x() + 1, h.y()), 10);
        game.step();
        assertEquals(SnakeMechanics.GOLDEN_APPLE_POINTS, game.score());
        assertNull(game.goldenApple());
    }

    @Test
    void pausingStopsMovement() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        game.togglePause();
        Point head = game.head();
        game.step();
        assertEquals(head, game.head());
        game.togglePause();
        game.step();
        assertNotEquals(head, game.head());
    }

    @Test
    void foodNeverSpawnsOnSnakeOrRocks() {
        SnakeMechanics game = new SnakeMechanics(Mode.OBSTACLES, 10, 8, new Random(11));
        game.start();
        for (int i = 0; i < 2000 && !game.isOver(); i++) {
            var body = new HashSet<>(game.body());
            assertFalse(body.contains(game.food()));
            assertFalse(game.obstacles().contains(game.food()));
            // steer toward the food greedily
            Point h = game.head(), f = game.food();
            if (f.x() > h.x()) game.turn(Direction.RIGHT);
            else if (f.x() < h.x()) game.turn(Direction.LEFT);
            else if (f.y() > h.y()) game.turn(Direction.DOWN);
            else game.turn(Direction.UP);
            game.step();
        }
    }
}
