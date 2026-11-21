package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the demo snake plays well enough to be worth watching. */
class SnakeAutopilotTest {

    /** Plays a demo game until it ends (or a step limit) and returns it. */
    private static SnakeMechanics playDemo(Mode mode, long seed) {
        SnakeMechanics game = new SnakeMechanics(mode, 24, 20, new Random(seed));
        game.start();
        for (int i = 0; i < 3000 && !game.isOver(); i++) {
            Direction move = SnakeAutopilot.chooseMove(game);
            assertNotEquals(game.direction().opposite(), move, "the demo never turns straight back");
            game.turn(move);
            game.step();
        }
        return game;
    }

    @Test
    void demoEatsPlentyOfApplesInEveryMode() {
        for (Mode mode : Mode.values()) {
            SnakeMechanics game = playDemo(mode, 7);
            assertTrue(game.applesEaten() >= 15,
                    mode + " demo only ate " + game.applesEaten() + " apples");
        }
    }

    @Test
    void demoSurvivesTheFirstMinuteOfClassic() {
        for (long seed = 1; seed <= 5; seed++) {
            SnakeMechanics game = new SnakeMechanics(Mode.CLASSIC, 24, 20, new Random(seed));
            game.start();
            int steps = 60_000 / game.tickIntervalMs();
            for (int i = 0; i < steps; i++) {
                game.turn(SnakeAutopilot.chooseMove(game));
                game.step();
            }
            assertTrue(!game.isOver(), "demo crashed early with seed " + seed + ": " + game.endReason());
        }
    }
}
