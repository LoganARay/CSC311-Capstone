package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rocks and apples are never placed somewhere unfair. */
class SnakeFairnessTest {

    /** Plays Obstacles games with the demo autopilot and checks the board after every move. */
    @Test
    void rocksNeverMakeDeadEndsOrSealAnythingOff() {
        for (long seed = 1; seed <= 15; seed++) {
            SnakeMechanics game = new SnakeMechanics(Mode.OBSTACLES, 24, 20, new Random(seed));
            game.start();
            for (int i = 0; i < 2500 && !game.isOver(); i++) {
                game.turn(SnakeAutopilot.chooseMove(game));
                game.step();
                checkNoTraps(game, seed);
            }
        }
    }

    @Test
    void applesAlwaysHaveAWayInAndAWayOut() {
        for (long seed = 1; seed <= 15; seed++) {
            SnakeMechanics game = new SnakeMechanics(Mode.OBSTACLES, 24, 20, new Random(seed));
            game.start();
            for (int i = 0; i < 2500 && !game.isOver(); i++) {
                game.turn(SnakeAutopilot.chooseMove(game));
                game.step();
                if (game.food() != null) {
                    assertTrue(openSides(game, game.food()) >= 2,
                            "apple at " + game.food() + " is a dead end (seed " + seed + ")");
                }
            }
        }
    }

    @Test
    void nothingAppearsInKeepClearSquares() {
        Set<Point> topRows = new HashSet<>();
        for (int x = 0; x < 24; x++) {
            topRows.add(new Point(x, 0));
            topRows.add(new Point(x, 1));
        }
        for (Mode mode : new Mode[] {Mode.OBSTACLES, Mode.TIME_ATTACK}) {
            SnakeMechanics game = new SnakeMechanics(mode, 24, 20, new Random(3));
            game.keepClear(topRows);
            game.start();
            for (int i = 0; i < 2500 && !game.isOver(); i++) {
                assertFalse(topRows.contains(game.food()), "apple under the score");
                assertFalse(topRows.contains(game.goldenApple()), "golden apple under the score");
                for (Point rock : game.obstacles()) {
                    assertFalse(topRows.contains(rock), "rock under the score");
                }
                game.turn(SnakeAutopilot.chooseMove(game));
                game.step();
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static void checkNoTraps(SnakeMechanics game, long seed) {
        int open = 0;
        Point start = null;
        for (int y = 0; y < game.height(); y++) {
            for (int x = 0; x < game.width(); x++) {
                Point p = new Point(x, y);
                if (!game.obstacles().contains(p)) {
                    open++;
                    if (start == null) {
                        start = p;
                    }
                    assertTrue(openSides(game, p) >= 2, "dead end at " + p + " (seed " + seed + ")");
                }
            }
        }
        Set<Point> seen = new HashSet<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            for (Direction d : Direction.values()) {
                Point n = new Point(p.x() + d.dx, p.y() + d.dy);
                if (onBoard(game, n) && !game.obstacles().contains(n) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        assertEquals(open, seen.size(), "rocks sealed part of the board off (seed " + seed + ")");
    }

    private static int openSides(SnakeMechanics game, Point p) {
        int count = 0;
        for (Direction d : Direction.values()) {
            Point n = new Point(p.x() + d.dx, p.y() + d.dy);
            if (onBoard(game, n) && !game.obstacles().contains(n)) {
                count++;
            }
        }
        return count;
    }

    private static boolean onBoard(SnakeMechanics game, Point p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < game.width() && p.y() < game.height();
    }
}
