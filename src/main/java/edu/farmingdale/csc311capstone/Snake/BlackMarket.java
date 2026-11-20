package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The secret black market: a dark room twice as wide as the screen. The snake starts on the
 * left, walks right to an altar where items are sold for apples, and leaves through a door
 * in the top-right corner. The snake can't die in here: walls and the altar just stop it.
 *
 * This class is only the rules (where things are, what the snake can do, buying).
 * SnakeController draws it and reads the keyboard.
 */
final class BlackMarket {

    /** Things for sale. Each one disappears from the altar while the player owns it. */
    enum Item {
        CLOCK("Clock", 100, "Turns time back 3 seconds when you crash. Works once."),
        HAMMER("Hammer", 100, "Smashes rocks you run into. Breaks after " + SnakeMechanics.HAMMER_USES + " rocks."),
        WHIP("Whip", 100, "Grabs apples 1 square to your left and right. Snaps after "
                + SnakeMechanics.WHIP_GRABS + " apples.");

        final String displayName;
        final int price;
        final String description;

        Item(String displayName, int price, String description) {
            this.displayName = displayName;
            this.price = price;
            this.description = description;
        }
    }

    enum State {
        WAITING,    // just arrived: nothing moves until the player presses an arrow key
        WALKING,
        CHOOSING,   // at the altar, picking an item with the arrow keys
        AT_DOOR,    // at the door, asked "Leave?"
        LEFT        // went through the door
    }

    /** What happened when the player tried to buy. */
    enum BuyResult { BOUGHT, NOT_ENOUGH_APPLES, NOTHING_SELECTED }

    // ------------------------------------------------------------------ the room's layout
    static final int WIDTH = 48;
    static final int HEIGHT = 20;
    static final int TICK_MS = 140;
    static final Point START = new Point(12, 10);
    static final Point ARROW = new Point(16, 10);          // the "move right" sign

    /** The altar's stone block, in squares. Items sit on top of it, one row above. */
    static final int ALTAR_X = 36;
    static final int ALTAR_Y = 9;
    static final int ALTAR_W = 7;
    static final int ALTAR_H = 2;
    /** Where each item sits on the altar (same order as Item.values()). */
    static final Point[] ITEM_SPOTS = {new Point(37, 8), new Point(39, 8), new Point(41, 8)};

    /** The door, in the top-right corner: 1 square wide, 2 tall. */
    static final int DOOR_X = WIDTH - 1;
    static final int DOOR_Y = 0;
    static final int DOOR_H = 2;

    private final SnakeSave save;
    private final Deque<Point> body = new ArrayDeque<>();
    private Direction direction = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private State state = State.WAITING;

    private int selected;                 // which of choices() is highlighted
    private boolean altarDismissed;       // pressed Esc at the altar: don't ask again until the snake walks away
    private boolean doorDismissed;
    private String message = "";          // e.g. "Not enough apples"

    BlackMarket(SnakeSave save) {
        this.save = save;
        for (int i = 0; i < 3; i++) {
            body.addLast(new Point(START.x() - i, START.y()));
        }
    }

    // ------------------------------------------------------------------ moving

    /** Arrow key while walking. The first one gets the snake moving. */
    void turn(Direction d) {
        if (state == State.WAITING) {
            state = State.WALKING;
        }
        if (state == State.WALKING && d != direction.opposite()) {
            nextDirection = d;
        }
    }

    /** Moves the snake one square, unless something is in the way. */
    void step() {
        if (state != State.WALKING) {
            return;
        }
        direction = nextDirection;
        Point head = body.peekFirst();
        Point next = new Point(head.x() + direction.dx, head.y() + direction.dy);
        if (isBlocked(next)) {
            return;                            // bump: the snake just waits there
        }
        body.addFirst(next);
        body.removeLast();

        boolean nearAltar = isNearAltar(next);
        boolean nearDoor = isNearDoor(next);
        if (!nearAltar) {
            altarDismissed = false;
        }
        if (!nearDoor) {
            doorDismissed = false;
        }
        if (nearAltar && !altarDismissed && !choices().isEmpty()) {
            state = State.CHOOSING;
            selected = 0;
            message = "";
        } else if (nearDoor && !doorDismissed) {
            state = State.AT_DOOR;
        }
    }

    /** Walls, the altar (with its items) and the door can't be walked through, and neither can the snake itself. */
    boolean isBlocked(Point p) {
        if (p.x() < 0 || p.y() < 0 || p.x() >= WIDTH || p.y() >= HEIGHT) {
            return true;
        }
        if (isAltar(p) || isDoor(p)) {
            return true;
        }
        List<Point> b = new ArrayList<>(body);
        return b.subList(0, b.size() - 1).contains(p);   // the tail moves out of the way
    }

    /** The altar block plus the row of items on top of it. */
    static boolean isAltar(Point p) {
        return p.x() >= ALTAR_X && p.x() < ALTAR_X + ALTAR_W
                && p.y() >= ALTAR_Y - 1 && p.y() < ALTAR_Y + ALTAR_H;
    }

    static boolean isDoor(Point p) {
        return p.x() == DOOR_X && p.y() >= DOOR_Y && p.y() < DOOR_Y + DOOR_H;
    }

    /** Right next to the altar (including diagonally). */
    static boolean isNearAltar(Point p) {
        return !isAltar(p)
                && p.x() >= ALTAR_X - 1 && p.x() <= ALTAR_X + ALTAR_W
                && p.y() >= ALTAR_Y - 2 && p.y() <= ALTAR_Y + ALTAR_H;
    }

    static boolean isNearDoor(Point p) {
        return !isDoor(p) && p.x() >= DOOR_X - 1 && p.y() <= DOOR_Y + DOOR_H;
    }

    // ------------------------------------------------------------------ the altar

    /** Items on the altar right now: everything the player doesn't already own. */
    List<Item> choices() {
        List<Item> list = new ArrayList<>();
        for (Item item : Item.values()) {
            if (!owns(item)) {
                list.add(item);
            }
        }
        return list;
    }

    boolean owns(Item item) {
        return switch (item) {
            case CLOCK -> save.clock;
            case HAMMER -> save.hammerUses > 0;
            case WHIP -> save.whipGrabs > 0;
        };
    }

    /** Left/right (or up/down) while choosing: moves the highlight, wrapping around. */
    void moveSelection(int by) {
        List<Item> list = choices();
        if (state == State.CHOOSING && !list.isEmpty()) {
            selected = Math.floorMod(selected + by, list.size());
            message = "";
        }
    }

    Item selectedItem() {
        List<Item> list = choices();
        return state == State.CHOOSING && !list.isEmpty() ? list.get(Math.min(selected, list.size() - 1)) : null;
    }

    /** Enter while choosing: buys the highlighted item if the player has enough apples. */
    BuyResult buy() {
        Item item = selectedItem();
        if (item == null) {
            return BuyResult.NOTHING_SELECTED;
        }
        if (save.apples < item.price) {
            message = "Not enough apples";
            return BuyResult.NOT_ENOUGH_APPLES;
        }
        save.apples -= item.price;
        switch (item) {
            case CLOCK -> save.clock = true;
            case HAMMER -> save.hammerUses = SnakeMechanics.HAMMER_USES;
            case WHIP -> save.whipGrabs = SnakeMechanics.WHIP_GRABS;
        }
        save.save();
        message = "Bought the " + item.displayName + "!";
        if (choices().isEmpty()) {
            state = State.WALKING;     // nothing left to buy
            altarDismissed = true;
        } else {
            selected = Math.min(selected, choices().size() - 1);
        }
        return BuyResult.BOUGHT;
    }

    // ------------------------------------------------------------------ Esc and Enter

    /** Esc: stop choosing (or stay instead of leaving) and walk again. */
    void cancel() {
        if (state == State.CHOOSING) {
            altarDismissed = true;
            state = State.WALKING;
        } else if (state == State.AT_DOOR) {
            doorDismissed = true;
            state = State.WALKING;
        }
    }

    /** Enter at the door: leave the black market. */
    void leave() {
        if (state == State.AT_DOOR) {
            state = State.LEFT;
        }
    }

    // ------------------------------------------------------------------ for drawing

    State state() { return state; }
    List<Point> body() { return new ArrayList<>(body); }
    Point head() { return body.peekFirst(); }
    Direction direction() { return direction; }
    int apples() { return save.apples; }
    String message() { return message; }
}
