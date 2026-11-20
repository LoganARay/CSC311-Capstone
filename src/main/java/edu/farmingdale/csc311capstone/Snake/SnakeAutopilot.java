package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Steers the snake by itself, for the demo that plays behind the start screen
 * (like the "attract mode" on an arcade machine).
 *
 * Each move it:
 *  1. finds the shortest path to an apple (the golden one first, if there is one),
 *  2. takes the first step of that path, but only if the snake would still have
 *     enough open space afterward,
 *  3. otherwise moves toward whichever side has the most open space, so it
 *     doesn't trap itself.
 *
 * It isn't perfect on purpose: sooner or later it crashes, and the demo restarts.
 */
final class SnakeAutopilot {

    private SnakeAutopilot() { }

    /** The direction the snake should go next. */
    static Direction chooseMove(SnakeMechanics game) {
        Point head = game.head();
        Set<Point> blocked = blockedSquares(game);

        Point target = game.goldenApple() != null ? game.goldenApple() : game.food();
        if (target != null) {
            Direction toApple = firstStepTowards(game, head, target, blocked);
            if (toApple != null) {
                Point next = neighbour(game, head, toApple);
                if (openSpace(game, next, blocked) >= game.length()) {
                    return toApple;
                }
            }
        }

        // No safe path to the apple: go wherever there's the most room.
        Direction best = game.direction();
        int bestSpace = -1;
        for (Direction d : Direction.values()) {
            if (d == game.direction().opposite()) {
                continue;
            }
            Point next = neighbour(game, head, d);
            if (next == null || blocked.contains(next)) {
                continue;
            }
            int space = openSpace(game, next, blocked);
            if (space > bestSpace) {
                bestSpace = space;
                best = d;
            }
        }
        return best;
    }

    /** Squares the snake can't move into: its own body (except the tail tip, which moves away) and rocks. */
    private static Set<Point> blockedSquares(SnakeMechanics game) {
        List<Point> body = game.body();
        Set<Point> blocked = new HashSet<>(body.subList(0, body.size() - 1));
        blocked.addAll(game.obstacles());
        return blocked;
    }

    /** Breadth-first search from the head; returns the first step of a shortest path, or null. */
    private static Direction firstStepTowards(SnakeMechanics game, Point head, Point target, Set<Point> blocked) {
        Map<Point, Direction> firstStep = new HashMap<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        for (Direction d : Direction.values()) {
            if (d == game.direction().opposite()) {
                continue;
            }
            Point next = neighbour(game, head, d);
            if (next != null && !blocked.contains(next) && !firstStep.containsKey(next)) {
                firstStep.put(next, d);
                queue.add(next);
            }
        }
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (p.equals(target)) {
                return firstStep.get(p);
            }
            for (Direction d : Direction.values()) {
                Point next = neighbour(game, p, d);
                if (next != null && !blocked.contains(next) && !firstStep.containsKey(next) && !next.equals(head)) {
                    firstStep.put(next, firstStep.get(p));
                    queue.add(next);
                }
            }
        }
        return null;
    }

    /** How many open squares can be reached from 'start' (counting it), without passing through the head. */
    private static int openSpace(SnakeMechanics game, Point start, Set<Point> blocked) {
        Set<Point> seen = new HashSet<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        seen.add(start);
        seen.add(game.head());
        queue.add(start);
        int count = 0;
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            count++;
            for (Direction d : Direction.values()) {
                Point next = neighbour(game, p, d);
                if (next != null && !blocked.contains(next) && seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return count;
    }

    /** The square next to p in direction d, wrapping around in wrap modes; null if that's off the board. */
    private static Point neighbour(SnakeMechanics game, Point p, Direction d) {
        int x = p.x() + d.dx;
        int y = p.y() + d.dy;
        if (game.wrapsAround()) {
            x = Math.floorMod(x, game.width());
            y = Math.floorMod(y, game.height());
        } else if (x < 0 || y < 0 || x >= game.width() || y >= game.height()) {
            return null;
        }
        return new Point(x, y);
    }
}
