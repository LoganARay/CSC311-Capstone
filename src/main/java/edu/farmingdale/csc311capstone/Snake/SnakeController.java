package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.State;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Connects snake-view.fxml to {@link SnakeMechanics}: runs the game loop,
 * reads the keyboard, draws the board on a Canvas inside an old TV,
 * and shows the menus (start screen, levels, pause menu).
 *
 * In menus: Up/Down or W/S to choose, Enter to select, Esc to go back.
 * In the game: arrow keys / WASD to steer, Space or P to pause, Enter to restart, Esc for the menu.
 */
public class SnakeController {

    // ------------------------------------------------------------------ the TV picture
    // Measured in pixels on tv.png. If you swap in a different TV picture,
    // update these to match its size and where its screen is.
    private static final double TV_W = 1378;
    private static final double TV_H = 947;
    /** The whole see-through screen opening (filled with dark glass behind the TV). */
    private static final double GLASS_X = 179;
    private static final double GLASS_Y = 91;
    private static final double GLASS_W = 1039;
    private static final double GLASS_H = 778;
    /** The part of the opening that is fully visible (the photo is a tiny bit tilted). */
    private static final double SCREEN_X = 201;
    private static final double SCREEN_Y = 95;
    private static final double SCREEN_W = 1004;
    private static final double SCREEN_H = 760;
    /** Leave a little dark glass showing around the board. */
    private static final double SCREEN_MARGIN = 14;
    /** The TV takes up at most this much of the monitor. */
    private static final double MAX_SCREEN_FRACTION = 0.85;

    // ------------------------------------------------------------------ look
    private static final Color BOARD_A = Color.web("#1e2b33");
    private static final Color BOARD_B = Color.web("#22313a");
    private static final Color DEADLY_WALL = Color.web("#f5a142");
    private static final Color WRAP_WALL = Color.web("#4da3ff");
    private static final Color MENU_RED = Color.web("#c62828");
    private static final Color MENU_WHITE = Color.web("#dedede");
    private static final Color MENU_GOLD = Color.web("#ffd34d");

    // Snake and apple tiles from "Snake Game Assets" by hamuko27, see CREDITS.md
    private static final String GRAPHICS = "/edu/farmingdale/csc311capstone/SnakeGraphics/";
    private static final Image TV = loadImage("tv.png");
    private static final Image APPLE = loadImage("snake/apple.png");
    private static final Image GOLDEN_APPLE = loadImage("snake/golden-apple.png");
    private static final Image ROCK = loadImage("snake/rock.png");
    /** Snake pieces by file name, e.g. "head-up", "turn-down-left", "tail-right". */
    private static final Map<String, Image> SNAKE = new HashMap<>();

    /** Best score per mode for as long as the app is open. */
    private static final Map<Mode, Integer> HIGH_SCORES = new EnumMap<>(Mode.class);

    @FXML private StackPane root;
    @FXML private Pane tvPane;
    @FXML private Rectangle screenGlass;
    @FXML private Canvas canvas;
    @FXML private ImageView tvImage;
    @FXML private VBox menuBox;

    private SnakeMechanics game;
    private Mode mode = Mode.CLASSIC;
    private final SnakeSounds sounds = new SnakeSounds();
    private long lastTick;

    private int cell;             // size of one board square on screen, in pixels
    private double buttonHeight;  // size of the menu buttons, in pixels

    // ------------------------------------------------------------------ menus
    private enum Menu { NONE, START, LEVELS, PAUSE }

    private Menu menu = Menu.NONE;
    private Menu levelsBackTo = Menu.START;   // where Back on the Levels screen goes
    private boolean pausedByMenu;             // true if opening the pause menu paused a running game
    private final List<MenuItem> items = new ArrayList<>();
    private int selected;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTick == 0) {
                lastTick = now;
            }
            // Catch up on as many steps as are due (keeps speed steady if a frame is late).
            long interval = game.tickIntervalMs() * 1_000_000L;
            int safety = 0;
            while (now - lastTick >= interval && safety++ < 5) {
                lastTick += interval;
                game.step();
                interval = game.tickIntervalMs() * 1_000_000L;
            }
            for (SnakeMechanics.Event event : game.pollEvents()) {
                sounds.play(event);
            }
            if (game.state() != State.RUNNING) {
                lastTick = now;
            }
            if (game.isOver()) {
                HIGH_SCORES.merge(game.mode(), game.score(), Math::max);
            }
            draw();
        }
    };

    @FXML
    private void initialize() {
        root.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                attachTo(scene);
            }
        });

        startNewGame();
        layoutTv();
        showStartScreen();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // A filter (not a handler) so the menu buttons can't swallow the arrow keys.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        // Stop the loop when the player goes back to the main menu.
        scene.windowProperty().addListener((obs, oldWin, win) -> {
            if (win == null) {
                loop.stop();
                sounds.stopAll();
            }
        });
    }

    /**
     * Sizes the TV to fit the monitor, then puts the screen glass, the board
     * and the menu inside the TV's screen.
     */
    private void layoutTv() {
        var monitor = Screen.getPrimary().getVisualBounds();
        double scale = Math.min(1.0, MAX_SCREEN_FRACTION * Math.min(
                monitor.getWidth() / TV_W, monitor.getHeight() / TV_H));
        double tvW = TV_W * scale;
        double tvH = TV_H * scale;

        tvImage.setImage(TV);
        tvImage.setFitWidth(tvW);
        tvPane.setMinSize(tvW, tvH);
        tvPane.setPrefSize(tvW, tvH);
        tvPane.setMaxSize(tvW, tvH);

        // The dark glass fills the whole screen opening.
        screenGlass.setX(GLASS_X * scale);
        screenGlass.setY(GLASS_Y * scale);
        screenGlass.setWidth(GLASS_W * scale);
        screenGlass.setHeight(GLASS_H * scale);

        // The board gets the biggest whole-pixel cell size that fits on the screen.
        double safeW = (SCREEN_W - 2 * SCREEN_MARGIN) * scale;
        double safeH = (SCREEN_H - 2 * SCREEN_MARGIN) * scale;
        cell = (int) Math.floor(Math.min(safeW / game.width(), safeH / game.height()));
        canvas.setWidth(game.width() * cell);
        canvas.setHeight(game.height() * cell);

        double centerX = (SCREEN_X + SCREEN_W / 2) * scale;
        double centerY = (SCREEN_Y + SCREEN_H / 2) * scale;
        canvas.setLayoutX(Math.round(centerX - canvas.getWidth() / 2));
        canvas.setLayoutY(Math.round(centerY - canvas.getHeight() / 2));

        // The menu covers the board. The Levels screen is the tallest:
        // a title, 6 buttons (5 levels + Back) and a hint line.
        menuBox.setLayoutX(canvas.getLayoutX());
        menuBox.setLayoutY(canvas.getLayoutY());
        menuBox.setPrefSize(canvas.getWidth(), canvas.getHeight());
        buttonHeight = Math.min(80, (canvas.getHeight() - cell * 4.5 - 7 * menuBox.getSpacing()) / 6);
    }

    // ------------------------------------------------------------------ game actions

    private void startNewGame() {
        sounds.stopAll(); // cut off the game-over / win sounds from the last round
        game = new SnakeMechanics(mode);
        lastTick = 0;
        if (cell > 0) {
            draw();
        }
    }

    private void quitToMainMenu() {
        loop.stop();
        sounds.stopAll();
        SceneSwitcher.showMenu(root);
    }

    private void onKey(KeyEvent e) {
        if (menu != Menu.NONE) {
            onMenuKey(e.getCode());
            e.consume(); // the game ignores keys while a menu is up
            return;
        }

        if (e.getCode() == KeyCode.ESCAPE) {
            showPauseMenu();
            e.consume();
            return;
        }

        Direction dir = switch (e.getCode()) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };

        if (dir != null) {
            if (game.state() == State.READY) {
                game.start();
            }
            game.turn(dir);
            e.consume();
            return;
        }

        switch (e.getCode()) {
            case SPACE, P -> {
                if (game.isOver()) {
                    startNewGame();
                } else {
                    game.togglePause();
                }
                e.consume();
            }
            case ENTER -> {
                if (game.isOver()) {
                    startNewGame();
                    e.consume();
                }
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ menu screens

    /** The first thing you see: Start, Levels, Quit. */
    private void showStartScreen() {
        List<MenuItem> list = new ArrayList<>();
        list.add(imageItem("start", () -> {
            closeMenu();
            startNewGame();
        }));
        list.add(textItem("Levels", false, () -> showLevels(Menu.START)));
        list.add(imageItem("quit", this::quitToMainMenu));
        openMenu(Menu.START, "SNAKE", "Level: " + mode.displayName(), list, 0);
    }

    /** Every level, plus Back. The current level is shown in gold and starts out highlighted. */
    private void showLevels(Menu backTo) {
        levelsBackTo = backTo;
        List<MenuItem> list = new ArrayList<>();
        for (Mode m : Mode.values()) {
            list.add(textItem(m.displayName(), m == mode, () -> {
                mode = m;
                pausedByMenu = false;
                closeMenu();
                startNewGame();
            }));
        }
        list.add(imageItem("back", this::goBack));
        openMenu(Menu.LEVELS, "LEVELS", null, list, mode.ordinal());
    }

    /** Esc during a game: Continue, New Game, Levels, Menu. */
    private void showPauseMenu() {
        if (menu == Menu.NONE) {
            pausedByMenu = game.state() == State.RUNNING;
            if (pausedByMenu) {
                game.togglePause();
            }
        }
        List<MenuItem> list = new ArrayList<>();
        if (!game.isOver()) {
            list.add(imageItem("continue", this::closeMenu));
        }
        list.add(imageItem("new-game", () -> {
            pausedByMenu = false;
            closeMenu();
            startNewGame();
        }));
        list.add(textItem("Levels", false, () -> showLevels(Menu.PAUSE)));
        list.add(imageItem("menu", () -> {
            pausedByMenu = false;
            startNewGame();
            showStartScreen();
        }));
        openMenu(Menu.PAUSE, "PAUSED", null, list, 0);
    }

    /** Esc / Back: Levels goes back to where it came from, the pause menu closes. */
    private void goBack() {
        switch (menu) {
            case LEVELS -> {
                if (levelsBackTo == Menu.PAUSE) {
                    showPauseMenu();
                } else {
                    showStartScreen();
                }
            }
            case PAUSE -> closeMenu();
            default -> { }   // nothing to go back to from the start screen
        }
    }

    private void onMenuKey(KeyCode key) {
        switch (key) {
            case UP, W -> select(selected - 1);
            case DOWN, S -> select(selected + 1);
            case ENTER, SPACE -> items.get(selected).action.run();
            case ESCAPE -> goBack();
            default -> { }
        }
    }

    private void openMenu(Menu which, String title, String subtitle, List<MenuItem> list, int startAt) {
        menu = which;
        items.clear();
        items.addAll(list);

        List<Node> children = new ArrayList<>();
        children.add(menuLabel(title, cell * 2.2, Color.web("#8db04f")));
        if (subtitle != null) {
            children.add(menuLabel(subtitle, cell * 0.7, Color.web("#cfd8e3")));
        }
        for (MenuItem item : items) {
            children.add(item.button);
        }
        children.add(menuLabel("↑ ↓ or W S to choose  •  Enter to select", cell * 0.55, Color.web("#8a96a3")));
        menuBox.getChildren().setAll(children);
        menuBox.setVisible(true);

        select(startAt);
    }

    /** Continue: closes the menu and picks the game back up where it was. */
    private void closeMenu() {
        menu = Menu.NONE;
        menuBox.setVisible(false);
        if (pausedByMenu && game.state() == State.PAUSED) {
            game.togglePause();
        }
        pausedByMenu = false;
        root.requestFocus();
    }

    /** Highlights one button (wrapping around at the ends) and un-highlights the rest. */
    private void select(int index) {
        selected = Math.floorMod(index, items.size());
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setHighlighted(i == selected);
        }
    }

    private Label menuLabel(String text, double size, Color color) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, size));
        label.setTextFill(color);
        label.setStyle("-fx-effect: dropshadow(one-pass-box, black, 0, 0, 2, 2);");
        label.setMouseTransparent(true);
        return label;
    }

    // ------------------------------------------------------------------ menu buttons

    /** One button in a menu. Highlighted = the white version from the button pack. */
    private final class MenuItem {
        final Button button = new Button();
        final Runnable action;
        private final ImageView view;
        private final Image normal;
        private final Image highlight;
        private final Label label;            // only for text buttons
        private final Color labelColor;

        MenuItem(Image normal, Image highlight, Label label, Color labelColor, Runnable action) {
            this.normal = normal;
            this.highlight = highlight;
            this.label = label;
            this.labelColor = labelColor;
            this.action = action;
            this.view = new ImageView(normal);
            view.setFitHeight(buttonHeight);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            button.setGraphic(label == null ? view : new StackPane(view, label));
            button.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            button.setFocusTraversable(false);
            button.setCursor(Cursor.HAND);
            button.setOnAction(e -> action.run());
            // Pointing at a button with the mouse selects it too, so only one is ever lit.
            button.hoverProperty().addListener((obs, was, isHover) -> {
                if (isHover) {
                    select(items.indexOf(this));
                }
            });
        }

        void setHighlighted(boolean on) {
            view.setImage(on ? highlight : normal);
            if (label != null) {
                label.setTextFill(on ? MENU_WHITE : labelColor);
            }
        }
    }

    /** A button from the button pack, e.g. "start" uses Buttons/start.png and Buttons/start-hover.png. */
    private MenuItem imageItem(String name, Runnable action) {
        return new MenuItem(loadImage("Buttons/" + name + ".png"),
                loadImage("Buttons/" + name + "-hover.png"), null, null, action);
    }

    /** The pack has no "Levels" or level-name buttons, so these use a blank button with text on top. */
    private MenuItem textItem(String text, boolean current, Runnable action) {
        Color color = current ? MENU_GOLD : MENU_RED;
        Label label = new Label(text.toUpperCase());
        label.setFont(fitFont(label.getText(), buttonHeight * 0.38, buttonHeight * 3 * 0.75));
        label.setTextFill(color);
        label.setMouseTransparent(true);
        return new MenuItem(loadImage("Buttons/blank.png"), loadImage("Buttons/blank-hover.png"),
                label, color, action);
    }

    /** The biggest bold font, up to maxSize, that keeps the text narrower than maxWidth. */
    private static Font fitFont(String text, double maxSize, double maxWidth) {
        Text measure = new Text(text);
        double size = maxSize;
        while (size > 8) {
            Font font = Font.font("System", FontWeight.BOLD, size);
            measure.setFont(font);
            if (measure.getLayoutBounds().getWidth() <= maxWidth) {
                return font;
            }
            size -= 1;
        }
        return Font.font("System", FontWeight.BOLD, size);
    }

    // ------------------------------------------------------------------ drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);   // keep the pixel art crisp

        // Checkerboard
        for (int y = 0; y < game.height(); y++) {
            for (int x = 0; x < game.width(); x++) {
                g.setFill((x + y) % 2 == 0 ? BOARD_A : BOARD_B);
                g.fillRect(x * cell, y * cell, cell, cell);
            }
        }

        // Solid orange walls in modes where they kill you; wrap modes get a dashed blue edge.
        g.setLineWidth(3);
        if (game.wrapsAround()) {
            g.setStroke(WRAP_WALL);
            g.setLineDashes(8, 6);
        } else {
            g.setStroke(DEADLY_WALL);
            g.setLineDashes();
        }
        g.strokeRect(1.5, 1.5, canvas.getWidth() - 3, canvas.getHeight() - 3);
        g.setLineDashes();

        for (Point p : game.obstacles()) {
            drawTile(g, ROCK, p);
        }
        if (game.food() != null) {
            drawTile(g, APPLE, game.food());
        }
        if (game.goldenApple() != null) {
            boolean blinking = game.goldenTicksLeft() < 12 && (game.goldenTicksLeft() / 2) % 2 == 0;
            if (!blinking) {
                drawTile(g, GOLDEN_APPLE, game.goldenApple());
            }
        }

        drawSnake(g);
        if (menu == Menu.NONE) {
            drawHud(g);   // keep the menus uncluttered
        }
        drawOverlay(g);
    }

    /**
     * Picks a tile for each piece of the snake by looking at which sides it
     * connects to: the head faces away from its neck, the tail points away from
     * the body, and every middle piece is a straight or a turn.
     */
    private void drawSnake(GraphicsContext g) {
        List<Point> body = game.body();
        int last = body.size() - 1;
        boolean dead = game.state() == State.GAME_OVER;

        for (int i = last; i >= 0; i--) {
            Point p = body.get(i);
            Direction toFront = i > 0 ? sideTowards(p, body.get(i - 1)) : null;
            Direction toBack = i < last ? sideTowards(p, body.get(i + 1)) : null;

            String name;
            if (i == 0) {
                Direction facing = toBack != null ? toBack.opposite() : game.direction();
                name = "head-" + word(facing) + (dead ? "-dead" : "");
            } else if (i == last) {
                Direction pointing = toFront != null ? toFront.opposite() : game.direction().opposite();
                name = "tail-" + word(pointing);
            } else {
                name = middlePiece(toFront, toBack);
            }
            drawTile(g, snakeTile(name), p);
        }
    }

    /** "body-horizontal", "body-vertical" or "turn-<up/down>-<left/right>". */
    private static String middlePiece(Direction a, Direction b) {
        if (a == null) a = b == null ? Direction.RIGHT : b.opposite();   // two pieces on one square
        if (b == null) b = a.opposite();
        boolean vertical = a == Direction.UP || a == Direction.DOWN;
        if (a == b.opposite()) {
            return vertical ? "body-vertical" : "body-horizontal";
        }
        Direction upDown = vertical ? a : b;
        Direction leftRight = vertical ? b : a;
        return "turn-" + word(upDown) + "-" + word(leftRight);
    }

    /** Which side of square 'from' the square 'to' is on. Handles wrapping around the board edge. */
    private Direction sideTowards(Point from, Point to) {
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        if (dx > 1) dx = -1;      // wrapped around the right edge
        if (dx < -1) dx = 1;      // wrapped around the left edge
        if (dy > 1) dy = -1;
        if (dy < -1) dy = 1;
        for (Direction d : Direction.values()) {
            if (d.dx == dx && d.dy == dy) {
                return d;
            }
        }
        return null;              // same square
    }

    private static String word(Direction d) {
        return d.name().toLowerCase();
    }

    private static Image snakeTile(String name) {
        return SNAKE.computeIfAbsent(name, n -> loadImage("snake/" + n + ".png"));
    }

    private void drawTile(GraphicsContext g, Image image, Point p) {
        g.drawImage(image, p.x() * cell, p.y() * cell, cell, cell);
    }

    /** Score and high score in the top-right corner; mode info (time, speed, rocks) top-left. */
    private void drawHud(GraphicsContext g) {
        double pad = cell * 0.6;
        double w = canvas.getWidth();
        int best = Math.max(HIGH_SCORES.getOrDefault(game.mode(), 0), game.score());

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(Font.font("System", FontWeight.BOLD, cell * 0.9));
        shadowText(g, "Score: " + game.score(), w - pad, pad + cell * 0.7, Color.WHITE);
        g.setFont(Font.font("System", FontWeight.BOLD, cell * 0.65));
        shadowText(g, "High Score: " + best, w - pad, pad + cell * 1.5, MENU_GOLD);

        String info = switch (game.mode()) {
            case TIME_ATTACK -> String.format("Time: %.1fs", game.timeLeftMs() / 1000.0);
            case SPEED_RUSH -> String.format("Speed: %.1f moves/s  |  %d pts per apple",
                    1000.0 / game.tickIntervalMs(), game.pointsPerApple());
            case OBSTACLES -> "Rocks: " + game.obstacles().size();
            default -> null;
        };
        if (info != null) {
            boolean lowTime = game.mode() == Mode.TIME_ATTACK && game.timeLeftMs() < 10_000;
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(Font.font("System", FontWeight.BOLD, cell * 0.65));
            shadowText(g, info, pad, pad + cell * 0.7, lowTime ? Color.web("#ff6b6b") : Color.web("#cfd8e3"));
        }
    }

    private void drawOverlay(GraphicsContext g) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        if (menu != Menu.NONE) {
            g.setFill(Color.web("#000000", 0.72));
            g.fillRect(0, 0, w, h);
            return; // the menu buttons are drawn on top by JavaFX
        }

        String title;
        List<String> lines = new ArrayList<>();
        switch (game.state()) {
            case READY -> {
                title = mode.displayName();
                lines.addAll(wrap(mode.description(), smallFont(), w * 0.8));
                lines.add("");
                lines.add("Press an arrow key or WASD to start");
                lines.add("Press Esc for menu");
            }
            case PAUSED -> {
                title = "Paused";
                lines.add("Press Space to keep going");
                lines.add("Press Esc for menu");
            }
            case GAME_OVER, WON -> {
                title = game.state() == State.WON ? "You Win!" : "Game Over";
                lines.add(game.endReason());
                lines.add("Score: " + game.score());
                lines.add("");
                lines.add("Press Enter to play again");
                lines.add("Press Esc for menu");
            }
            default -> {
                return;
            }
        }

        g.setFill(Color.web("#000000", 0.55));
        g.fillRect(0, 0, w, h);

        double lineHeight = cell * 0.95;
        double blockHeight = cell * 2.2 + lines.size() * lineHeight;
        double y = (h - blockHeight) / 2 + cell * 1.6;

        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(Font.font("System", FontWeight.BOLD, cell * 1.8));
        shadowText(g, title, w / 2, y, Color.WHITE);

        y += cell * 1.4;
        g.setFont(smallFont());
        for (String line : lines) {
            y += lineHeight;
            shadowText(g, line, w / 2, y, Color.web("#cfd8e3"));
        }
    }

    private Font smallFont() {
        return Font.font("System", cell * 0.75);
    }

    private static void shadowText(GraphicsContext g, String text, double x, double y, Color color) {
        g.setFill(Color.web("#000000", 0.7));
        g.fillText(text, x + 1.5, y + 1.5);
        g.setFill(color);
        g.fillText(text, x, y);
    }

    /** Splits text into lines no wider than maxWidth. */
    private static List<String> wrap(String text, Font font, double maxWidth) {
        Text measure = new Text();
        measure.setFont(font);
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String tryLine = line.isEmpty() ? word : line + " " + word;
            measure.setText(tryLine);
            if (measure.getLayoutBounds().getWidth() > maxWidth && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(tryLine);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static Image loadImage(String fileName) {
        InputStream in = SnakeController.class.getResourceAsStream(GRAPHICS + fileName);
        if (in == null) {
            throw new IllegalStateException("Missing Snake graphic: " + GRAPHICS + fileName);
        }
        return new Image(in);
    }
}
