package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Event;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.State;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hearts and the black market items: clock, hammer and whip. */
class SnakeItemsTest {

    private static SnakeMechanics newGame(Mode mode) {
        SnakeMechanics game = new SnakeMechanics(mode, 20, 15, new Random(7));
        game.clearObstacles();
        game.placeFood(new Point(0, 0));   // out of the way unless a test moves it
        game.makeFoodHeart(false);
        game.start();
        game.pollEvents();
        return game;
    }

    private static Point ahead(SnakeMechanics game, int squares) {
        Point h = game.head();
        return new Point(h.x() + squares, h.y());   // the snake starts moving right
    }

    // ------------------------------------------------------------------ hearts

    @Test
    void eatingAHeartCountsAsAnAppleAndAHeart() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        game.placeFood(ahead(game, 1));
        game.makeFoodHeart(true);
        game.step();
        assertEquals(List.of(Event.ATE_APPLE, Event.ATE_HEART), game.pollEvents());
        assertEquals(1, game.applesEaten());
    }

    @Test
    void aboutOneAppleInTenIsAHeart() {
        SnakeMechanics game = new SnakeMechanics(Mode.CLASSIC, 20, 15, new Random(11));
        game.start();
        int hearts = 0;
        int apples = 2000;
        for (int i = 0; i < apples; i++) {
            game.reset();                     // every reset puts down a new apple
            if (game.foodIsHeart()) {
                hearts++;
            }
        }
        assertTrue(hearts > apples * 0.06 && hearts < apples * 0.14, hearts + " hearts out of " + apples);
    }

    // ------------------------------------------------------------------ hammer

    @Test
    void hammerSmashesRocksThenBreaks() {
        SnakeMechanics game = newGame(Mode.OBSTACLES);
        game.clearObstacles();
        game.setItems(false, 2, 0);

        game.placeObstacle(ahead(game, 1));
        game.step();
        assertEquals(List.of(Event.HAMMER_SMASH), game.pollEvents());
        assertEquals(State.RUNNING, game.state());
        assertTrue(game.obstacles().isEmpty(), "the rock is gone");
        assertEquals(1, game.hammerUsesLeft());

        game.placeObstacle(ahead(game, 1));
        game.step();
        assertEquals(List.of(Event.HAMMER_SMASH, Event.HAMMER_BROKE), game.pollEvents());
        assertEquals(0, game.hammerUsesLeft());

        game.placeObstacle(ahead(game, 1));
        game.step();
        assertEquals(List.of(Event.CRASHED), game.pollEvents(), "no hammer left, so it's a crash");
    }

    // ------------------------------------------------------------------ whip

    @Test
    void whipGrabsApplesBesideTheHead() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        game.setItems(false, 0, 5);
        Point next = ahead(game, 1);
        game.placeFood(new Point(next.x(), next.y() - 1));   // just above where the head is going
        game.step();
        assertEquals(List.of(Event.ATE_APPLE), game.pollEvents());
        assertEquals(4, game.whipGrabsLeft());
    }

    @Test
    void withoutTheWhipApplesBesideTheHeadAreMissed() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        Point next = ahead(game, 1);
        game.placeFood(new Point(next.x(), next.y() - 1));
        game.step();
        assertEquals(List.of(), game.pollEvents());
    }

    @Test
    void whipSnapsAfterItsLastGrab() {
        SnakeMechanics game = newGame(Mode.CLASSIC);
        game.setItems(false, 0, 1);
        game.placeFood(ahead(game, 1));
        game.step();
        assertEquals(List.of(Event.WHIP_SNAPPED, Event.ATE_APPLE), game.pollEvents());
        assertEquals(0, game.whipGrabsLeft());
    }

    // ------------------------------------------------------------------ clock

    @Test
    void clockTurnsTimeBackInsteadOfCrashingOnce() {
        // A wide board, so the snake travels for more than REWIND_MS before it reaches the wall.
        SnakeMechanics game = new SnakeMechanics(Mode.CLASSIC, 80, 15, new Random(7));
        game.placeFood(new Point(0, 0));
        game.start();
        game.setItems(true, 0, 0);
        int stepsUntilWall = game.width() - 1 - game.head().x();
        for (int i = 0; i < stepsUntilWall; i++) {
            game.step();
        }
        game.pollEvents();
        Point beforeCrash = game.head();

        game.step();   // into the wall
        assertEquals(List.of(Event.REWOUND), game.pollEvents());
        assertEquals(State.READY, game.state(), "waits for an arrow key");
        assertTrue(game.justRewound());
        assertFalse(game.hasClock(), "the clock is used up");
        int backBy = beforeCrash.x() - game.head().x();
        assertTrue(backBy >= SnakeMechanics.REWIND_MS / game.tickIntervalMs() - 1,
                "went back about " + SnakeMechanics.REWIND_MS + " ms, only moved back " + backBy + " squares");

        game.start();
        assertFalse(game.justRewound());
        for (int i = 0; i < 200 && !game.isOver(); i++) {
            game.step();
        }
        assertEquals(State.GAME_OVER, game.state(), "the second crash is real");
    }
}
