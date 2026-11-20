package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.BlackMarket.BuyResult;
import edu.farmingdale.csc311capstone.Snake.BlackMarket.Item;
import edu.farmingdale.csc311capstone.Snake.BlackMarket.State;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlackMarketTest {

    private static SnakeSave newSave(int apples) throws IOException {
        SnakeSave save = SnakeSave.load(Files.createTempDirectory("market-test").resolve("save.properties"));
        save.apples = apples;
        return save;
    }

    /** Walks right from the start until something happens. */
    private static void walkRightToAltar(BlackMarket market) {
        market.turn(Direction.RIGHT);
        for (int i = 0; i < 100 && market.state() == State.WALKING; i++) {
            market.step();
        }
    }

    @Test
    void waitsForAnArrowKeyBeforeMoving() throws IOException {
        BlackMarket market = new BlackMarket(newSave(0));
        market.step();
        assertEquals(State.WAITING, market.state());
        assertEquals(BlackMarket.START, market.head());
    }

    @Test
    void walkingRightReachesTheAltar() throws IOException {
        BlackMarket market = new BlackMarket(newSave(0));
        walkRightToAltar(market);
        assertEquals(State.CHOOSING, market.state());
        assertEquals(List.of(Item.CLOCK, Item.HAMMER, Item.WHIP), market.choices());
    }

    @Test
    void cantBuyWithoutEnoughApples() throws IOException {
        SnakeSave save = newSave(99);
        BlackMarket market = new BlackMarket(save);
        walkRightToAltar(market);
        assertEquals(BuyResult.NOT_ENOUGH_APPLES, market.buy());
        assertEquals(99, save.apples);
        assertFalse(save.clock);
        assertEquals("Not enough apples", market.message());
    }

    @Test
    void buyingTakesApplesAndTheItemLeavesTheAltar() throws IOException {
        SnakeSave save = newSave(250);
        BlackMarket market = new BlackMarket(save);
        walkRightToAltar(market);

        market.moveSelection(1);                     // Clock -> Hammer
        assertEquals(Item.HAMMER, market.selectedItem());
        assertEquals(BuyResult.BOUGHT, market.buy());
        assertEquals(150, save.apples);
        assertEquals(SnakeMechanics.HAMMER_USES, save.hammerUses);
        assertEquals(List.of(Item.CLOCK, Item.WHIP), market.choices());

        assertEquals(BuyResult.BOUGHT, market.buy());   // whatever is highlighted now
        assertEquals(50, save.apples);
        assertEquals(1, market.choices().size());
    }

    @Test
    void boughtItemsDontShowUpUntilUsedUp() throws IOException {
        SnakeSave save = newSave(0);
        save.clock = true;
        save.whipGrabs = 40;
        assertEquals(List.of(Item.HAMMER), new BlackMarket(save).choices());
        save.whipGrabs = 0;
        assertEquals(List.of(Item.HAMMER, Item.WHIP), new BlackMarket(save).choices());
    }

    @Test
    void escLeavesTheAltarUntilTheSnakeWalksAway() throws IOException {
        BlackMarket market = new BlackMarket(newSave(0));
        walkRightToAltar(market);
        market.cancel();
        assertEquals(State.WALKING, market.state());

        market.turn(Direction.DOWN);                 // still next to the altar
        market.step();
        assertEquals(State.WALKING, market.state(), "doesn't ask again right away");

        for (int i = 0; i < 3; i++) {
            market.step();                           // walk away
        }
        market.turn(Direction.RIGHT);                // loop around (a snake can't reverse)
        market.step();
        market.turn(Direction.UP);
        for (int i = 0; i < 10 && market.state() == State.WALKING; i++) {
            market.step();                           // and come back
        }
        assertEquals(State.CHOOSING, market.state());
    }

    @Test
    void wallsAndTheAltarStopTheSnakeInsteadOfKillingIt() throws IOException {
        BlackMarket market = new BlackMarket(newSave(0));
        market.turn(Direction.UP);
        for (int i = 0; i < 30; i++) {
            market.step();
        }
        assertEquals(0, market.head().y(), "stopped at the top wall");
        assertEquals(State.WALKING, market.state());
    }

    @Test
    void theDoorAsksBeforeLeaving() throws IOException {
        BlackMarket market = new BlackMarket(newSave(0));
        market.turn(Direction.UP);
        for (int i = 0; i < 30; i++) {
            market.step();                           // up to the top wall
        }
        market.turn(Direction.RIGHT);
        for (int i = 0; i < 60 && market.state() == State.WALKING; i++) {
            market.step();                           // along the top to the door
        }
        assertEquals(State.AT_DOOR, market.state());

        market.cancel();
        assertEquals(State.WALKING, market.state(), "Esc: stay");
        assertTrue(BlackMarket.isNearDoor(market.head()));

        market.turn(Direction.DOWN);                 // walk away...
        for (int i = 0; i < 4; i++) {
            market.step();
        }
        market.turn(Direction.LEFT);                 // ...loop around...
        market.step();
        market.turn(Direction.UP);
        for (int i = 0; i < 3; i++) {
            market.step();
        }
        market.turn(Direction.RIGHT);                // ...and come back to the door
        for (int i = 0; i < 3 && market.state() == State.WALKING; i++) {
            market.step();
        }
        assertEquals(State.AT_DOOR, market.state());
        market.leave();
        assertEquals(State.LEFT, market.state());
    }
}
