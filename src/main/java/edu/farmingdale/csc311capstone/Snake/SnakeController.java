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
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
    private static final double TV_W = 1404;
    private static final double TV_H = 954;
    /** The see-through screen opening: an exact rectangle, filled with dark glass behind the TV. */
    private static final double SCREEN_X = 199;
    private static final double SCREEN_Y = 93;
    private static final double SCREEN_W = 1021;
    private static final double SCREEN_H = 769;
    /** Leave at least this much dark glass showing around the board. */
    private static final double SCREEN_MARGIN = 14;
    /** The TV takes up at most this much of the monitor. */
    private static final double MAX_SCREEN_FRACTION = 0.85;

    // ------------------------------------------------------------------ CRT effect
    // Drawn over the whole TV screen (game, grass and menus). Set CRT_EFFECT to false to turn it off.
    private static final boolean CRT_EFFECT = true;
    /** One dark scanline every this many pixels, and how dark the lines are (0 = invisible, 1 = black). */
    private static final int SCANLINE_GAP = 3;
    private static final double SCANLINE_DARKNESS = 0.22;
    /** Faint red / green / blue stripes, like the colored dots on a real picture tube. */
    private static final double PHOSPHOR_STRENGTH = 0.05;
    /** How dark the edges and corners of the glass get. */
    private static final double VIGNETTE_DARKNESS = 0.55;
    /** The soft reflection on the glass in the top-left corner. */
    private static final double GLARE_STRENGTH = 0.10;
    /** A faint bright band that slowly rolls down the screen, taking this many seconds per trip. */
    private static final double ROLL_SECONDS = 7;
    private static final double ROLL_BRIGHTNESS = 0.05;
    /** How much the picture flickers (0 = steady). */
    private static final double FLICKER = 0.05;

    // ------------------------------------------------------------------ crash sequence
    // When the snake crashes: the crash sound, then STATIC_DELAY_MS later the screen fills with
    // TV static (and its sound) that fades in, holds, and fades out. When the screen is back to
    // normal, the funeral march and "game over" voice play.
    private static final long STATIC_DELAY_MS = 500;
    private static final long STATIC_FADE_IN_MS = 400;
    private static final long STATIC_HOLD_MS = 600;
    private static final long STATIC_FADE_OUT_MS = 800;
    /** How strong the static is at its peak (1 = you can barely see the game). */
    private static final double STATIC_STRENGTH = 0.85;
    /** Different random static pictures, swapped every frame so it looks alive. */
    private static final int STATIC_FRAMES = 8;
    /** When the crash leads to the black market, the static holds this long instead. */
    private static final long SECRET_HOLD_MS = 1_800;

    // ------------------------------------------------------------------ black market
    // Graphics: clock, hammer, whip, altar, door and arrow drawn for this project;
    // skull candle and lantern from "Haunted Evidence Lite" by certainity (see CREDITS.md).
    private static final Image MARKET_ARROW = loadImage("market/arrow.png");
    private static final Image MARKET_ALTAR = loadImage("market/altar.png");
    private static final Image MARKET_DOOR = loadImage("market/door.png");
    private static final Image MARKET_LANTERN = loadImage("market/lantern.png");
    private static final Image SKULL_CANDLE = loadImage("market/skull-candle.png");
    private static final Map<BlackMarket.Item, Image> ITEM_ICONS = Map.of(
            BlackMarket.Item.CLOCK, loadImage("market/clock.png"),
            BlackMarket.Item.HAMMER, loadImage("market/hammer.png"),
            BlackMarket.Item.WHIP, loadImage("market/whip.png"));
    /** How far each light reaches in the dark room, in squares. */
    private static final double SNAKE_LIGHT = 5;
    private static final double CANDLE_LIGHT = 6.5;
    private static final double LANTERN_LIGHT = 3.5;
    private static final double SIGN_LIGHT = 2.2;

    // ------------------------------------------------------------------ look
    private static final Color WRAP_WALL = Color.web("#4da3ff");
    private static final Color BUTTON_TEXT = Color.web("#fffbe0");
    private static final Color MENU_GOLD = Color.web("#ffe45c");
    private static final Color SOFT_TEXT = Color.web("#e4ecd8");

    // All text uses the "Press Start 2P" pixel font by CodeMan38 (SIL Open Font License, see SnakeFonts/OFL.txt).
    private static final String PIXEL_FONT = loadPixelFont();

    // Menu buttons from "Game Interface kit" by mmkhlv. The -selected versions glow.
    // The images have room around the button for the glow: the button itself is BUTTON_SHARE of the image height.
    private static final double BUTTON_SHARE = 200.0 / 244.0;
    private static final double BUTTON_ASPECT = 594.0 / 200.0;   // width / height of the button itself
    private static final Color TITLE = Color.web("#f5f0c8");

    // Snake and apple tiles from "Snake Game Assets" by hamuko27, see CREDITS.md
    private static final String GRAPHICS = "/edu/farmingdale/csc311capstone/SnakeGraphics/";
    private static final Image TV = loadImage("tv.png");
    private static final Image APPLE = loadImage("snake/apple.png");
    private static final Image GOLDEN_APPLE = loadImage("snake/golden-apple.png");
    // "Real heart" sprite by Iamrenz, shrunk to 32x32. 1 apple in 10 looks like this.
    private static final Image HEART = loadImage("snake/heart.png");
    private static final Image ROCK = loadImage("snake/rock.png");
    // Grass from "GRASS+" by SciGho (ninjikin on itch.io), see CREDITS.md
    private static final Image GRASS_MAIN = loadImage("grass/grass-main.png");      // playing area
    private static final Image GRASS_ACCENT = loadImage("grass/grass-accent.png");  // scattered tufts
    private static final String GRASS_DARK = "grass/grass-dark.png";                 // around the board
    /** About 1 grass tile in this many gets tufts. */
    private static final int ACCENT_ONE_IN = 15;
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
    @FXML private Pane crtLayer;
    @FXML private Canvas crtGlass;
    @FXML private Rectangle crtRollBar;
    @FXML private Canvas crtStatic;

    private SnakeMechanics game;
    private Mode mode = Mode.CLASSIC;

    // ------------------------------------------------------------------ demo
    // Like an arcade machine, a game plays itself behind the start screen.
    /** The demo starts over after this long, even if the snake is still alive. */
    private static final long DEMO_MAX_NS = 40_000_000_000L;
    /** How long a crashed demo stays on screen before it starts over. */
    private static final long DEMO_PAUSE_NS = 1_500_000_000L;

    /**
     * The demo plays the eating and crash sounds. The long game-over music is left out
     * so it doesn't repeat every time the demo snake crashes; set this to true to hear it too.
     */
    private static final boolean DEMO_PLAYS_GAME_OVER_MUSIC = false;

    /**
     * A burst of TV static: waits delayMs, fades in, holds, fades out. atPeak runs when it's
     * at full strength (a good moment to switch screens) and atEnd when it's gone.
     */
    private record StaticPlan(long delayMs, long fadeInMs, long holdMs, long fadeOutMs,
                              boolean withSound, boolean locksKeys, Runnable atPeak, Runnable atEnd) { }

    private StaticPlan staticPlan;              // the static running right now, or null
    private long staticStartedAt;
    private boolean staticSoundStarted;
    private boolean staticPeakDone;
    private final List<Image> staticFrames = new ArrayList<>();

    // ------------------------------------------------------------------ saved progress + black market
    private final SnakeSave save = SnakeSave.load();
    private final Random random = new Random();

    private enum View { GAME, MARKET }
    private View view = View.GAME;
    private BlackMarket market;
    private long marketLastTick;
    private double cameraX;                     // how far the market view has scrolled, in squares
    private ImagePattern darkGrass;             // fills the screen around the board

    private SnakeMechanics demo;
    private long demoStartedAt;
    private long demoLastTick;
    private long demoOverAt;

    /** The game being drawn right now: the demo on the start screen, otherwise the real game. */
    private SnakeMechanics shown;
    private final SnakeSounds sounds = new SnakeSounds();
    private long lastTick;

    private int cell;             // size of one board square on screen, in pixels
    private int grassSize;        // grass tiles are drawn at a whole-number multiple of 16 px so they stay crisp
    private boolean[][] tuftAt;   // which grass tiles get tufts, picked at random once
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
            animateCrt(now);
            runStatic(now);
            if (view == View.MARKET) {
                runMarket(now);
                drawMarket();
                return;
            }
            if (demoIsShowing()) {
                runDemo(now);
                lastTick = now;   // the real game waits while the start screen is up
                draw();
                return;
            }
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
                onGameEvent(event, now);
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
        game.keepClear(hudSquares(mode));   // now that the board's size on screen is known
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

        // The dark glass fills the screen opening exactly.
        screenGlass.setX(SCREEN_X * scale);
        screenGlass.setY(SCREEN_Y * scale);
        screenGlass.setWidth(SCREEN_W * scale);
        screenGlass.setHeight(SCREEN_H * scale);

        // The board gets the biggest whole-pixel cell size that fits on the screen,
        // and is centered in the middle of it.
        double safeW = (SCREEN_W - 2 * SCREEN_MARGIN) * scale;
        double safeH = (SCREEN_H - 2 * SCREEN_MARGIN) * scale;
        cell = (int) Math.floor(Math.min(safeW / game.width(), safeH / game.height()));
        canvas.setWidth(game.width() * cell);
        canvas.setHeight(game.height() * cell);

        // Dark grass fills the screen around the board (the part you can't play on).
        grassSize = 16 * Math.max(1, Math.round(cell / 16f));
        darkGrass = new ImagePattern(loadImage(GRASS_DARK, grassSize), screenGlass.getX(), screenGlass.getY(),
                grassSize, grassSize, false);
        screenGlass.setFill(darkGrass);

        double centerX = (SCREEN_X + SCREEN_W / 2) * scale;
        Random random = new Random();
        tuftAt = new boolean[(int) Math.ceil(canvas.getWidth() / grassSize)][(int) Math.ceil(canvas.getHeight() / grassSize)];
        for (boolean[] column : tuftAt) {
            for (int i = 0; i < column.length; i++) {
                column[i] = random.nextInt(ACCENT_ONE_IN) == 0;
            }
        }
        double centerY = (SCREEN_Y + SCREEN_H / 2) * scale;
        canvas.setLayoutX(Math.round(centerX - canvas.getWidth() / 2));
        canvas.setLayoutY(Math.round(centerY - canvas.getHeight() / 2));

        // The menu covers the board. The Levels screen is the tallest:
        // a title, 6 buttons (5 levels + Back) and a hint line.
        menuBox.setLayoutX(canvas.getLayoutX());
        menuBox.setLayoutY(canvas.getLayoutY());
        menuBox.setPrefSize(canvas.getWidth(), canvas.getHeight());
        buttonHeight = Math.min(80, (canvas.getHeight() - cell * 4 - 7 * menuBox.getSpacing()) / 6 * BUTTON_SHARE);

        layoutCrt(SCREEN_X * scale, SCREEN_Y * scale, SCREEN_W * scale, SCREEN_H * scale);
    }

    // ------------------------------------------------------------------ demo

    /** The demo plays on the start screen and on the Levels screen opened from it. */
    private boolean demoIsShowing() {
        return menu == Menu.START || (menu == Menu.LEVELS && levelsBackTo == Menu.START);
    }

    /** Moves the demo snake along, and starts a new demo when it crashes or runs too long. */
    private void runDemo(long now) {
        boolean needNew = demo == null || demo.mode() != mode
                || now - demoStartedAt > DEMO_MAX_NS
                || (demo.isOver() && now - demoOverAt > DEMO_PAUSE_NS);
        if (needNew) {
            demo = newGameFor(mode);   // shows the level that's currently picked
            demo.setItems(false, 0, 0);
            demo.start();
            demoStartedAt = now;
            demoLastTick = now;
            demoOverAt = 0;
        }
        long interval = demo.tickIntervalMs() * 1_000_000L;
        int safety = 0;
        while (!demo.isOver() && now - demoLastTick >= interval && safety++ < 5) {
            demoLastTick += interval;
            demo.turn(SnakeAutopilot.chooseMove(demo));
            demo.step();
            interval = demo.tickIntervalMs() * 1_000_000L;
        }
        for (SnakeMechanics.Event event : demo.pollEvents()) {
            if (DEMO_PLAYS_GAME_OVER_MUSIC) {
                sounds.play(event);
            } else {
                sounds.playShort(event);
            }
        }
        if (demo.isOver() && demoOverAt == 0) {
            demoOverAt = now;
        }
    }

    // ------------------------------------------------------------------ CRT effect

    /** Puts the CRT layer exactly over the screen opening and draws the parts that don't move. */
    private void layoutCrt(double x, double y, double w, double h) {
        crtLayer.setVisible(CRT_EFFECT);
        crtLayer.setLayoutX(x);
        crtLayer.setLayoutY(y);
        crtLayer.setPrefSize(w, h);
        crtLayer.setClip(new Rectangle(w, h));   // the rolling band never shows outside the glass
        crtGlass.setWidth(w);
        crtGlass.setHeight(h);
        crtStatic.setWidth(w);
        crtStatic.setHeight(h);
        makeStaticFrames(w, h);

        GraphicsContext g = crtGlass.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        // Phosphor stripes: a faint red, green, blue column pattern.
        Color[] phosphor = {Color.rgb(255, 0, 0, PHOSPHOR_STRENGTH),
                Color.rgb(0, 255, 0, PHOSPHOR_STRENGTH), Color.rgb(0, 0, 255, PHOSPHOR_STRENGTH)};
        for (int col = 0; col < w; col++) {
            g.setFill(phosphor[col % 3]);
            g.fillRect(col, 0, 1, h);
        }

        // Scanlines.
        g.setFill(Color.rgb(0, 0, 0, SCANLINE_DARKNESS));
        for (int row = 0; row < h; row += SCANLINE_GAP) {
            g.fillRect(0, row, w, 1);
        }

        // Vignette: clear in the middle, darker toward the edges and corners.
        g.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.TRANSPARENT), new Stop(1, Color.rgb(0, 0, 0, VIGNETTE_DARKNESS))));
        g.fillRect(0, 0, w, h);

        // Glare: a soft light reflection coming from the top-left.
        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255, GLARE_STRENGTH)), new Stop(0.35, Color.TRANSPARENT)));
        g.fillRect(0, 0, w, h);

        // The rolling band: a soft bright stripe, faded at the top and bottom.
        crtRollBar.setWidth(w);
        crtRollBar.setHeight(h * 0.15);
        crtRollBar.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.rgb(255, 255, 255, ROLL_BRIGHTNESS)),
                new Stop(1, Color.TRANSPARENT)));
    }

    /** Moves the rolling band down the screen and makes the glass flicker a tiny bit. */
    private void animateCrt(long now) {
        if (!CRT_EFFECT) {
            return;
        }
        double h = crtGlass.getHeight();
        double band = crtRollBar.getHeight();
        double trip = (now / 1e9 % ROLL_SECONDS) / ROLL_SECONDS;   // 0 to 1, over and over
        crtRollBar.setTranslateY(-band + trip * (h + band));
        crtGlass.setOpacity(1 - FLICKER * Math.random());
    }

    // ------------------------------------------------------------------ crash sequence

    /**
     * The snake crashed: the crash sound, then static. Usually the game-over music follows;
     * with luck (the secret chance from hearts) the static lasts longer and the snake wakes
     * up in the black market instead.
     */
    private void startCrashSequence(long now) {
        sounds.playCrash();
        if (save.rollForMarket(random)) {
            startStatic(now, new StaticPlan(STATIC_DELAY_MS, STATIC_FADE_IN_MS, SECRET_HOLD_MS,
                    STATIC_FADE_OUT_MS, true, true, () -> {
                        resetSecretChance();         // the chance starts over at 0
                        enterMarket();
                    }, null));
        } else {
            startStatic(now, new StaticPlan(STATIC_DELAY_MS, STATIC_FADE_IN_MS, STATIC_HOLD_MS,
                    STATIC_FADE_OUT_MS, true, false, null, sounds::playGameOverMusic));
        }
    }

    private void startStatic(long now, StaticPlan plan) {
        staticPlan = plan;
        staticStartedAt = now;
        staticSoundStarted = false;
        staticPeakDone = false;
    }

    /** Called every frame: fades the static in and out and runs the plan's atPeak / atEnd. */
    private void runStatic(long now) {
        StaticPlan plan = staticPlan;
        if (plan == null) {
            return;
        }
        double ms = (now - staticStartedAt) / 1e6 - plan.delayMs();   // time since the static began
        if (ms < 0) {
            return;
        }
        if (plan.withSound() && !staticSoundStarted) {
            staticSoundStarted = true;
            sounds.startStatic();
        }

        double fadeOutAt = plan.fadeInMs() + plan.holdMs();
        double strength;
        if (ms < plan.fadeInMs()) {
            strength = ms / plan.fadeInMs();                         // fading in
        } else if (ms < fadeOutAt) {
            strength = 1;                                            // full static
        } else if (ms < fadeOutAt + plan.fadeOutMs()) {
            strength = 1 - (ms - fadeOutAt) / plan.fadeOutMs();      // fading out
        } else {
            staticPlan = null;                                       // back to normal
            crtStatic.setVisible(false);
            if (plan.withSound()) {
                sounds.stopStatic();
            }
            if (plan.atEnd() != null) {
                plan.atEnd().run();
            }
            return;
        }

        if (!staticPeakDone && ms >= plan.fadeInMs()) {
            staticPeakDone = true;
            if (plan.atPeak() != null) {
                plan.atPeak().run();
                if (staticPlan != plan) {
                    return;                                          // atPeak stopped this static
                }
            }
        }
        if (plan.withSound()) {
            sounds.setStaticVolume(strength);
        }
        drawStatic(strength);
    }

    /** Stops any static early, e.g. when a new game starts. */
    private void cancelCrashSequence() {
        staticPlan = null;
        if (crtStatic != null) {
            crtStatic.setVisible(false);
        }
    }

    /** Draws one frame of static over the screen: random snow plus a few bright streaks. */
    private void drawStatic(double strength) {
        double w = crtStatic.getWidth();
        double h = crtStatic.getHeight();
        GraphicsContext g = crtStatic.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        g.setImageSmoothing(false);
        g.setGlobalAlpha(STATIC_STRENGTH * strength);
        Image frame = staticFrames.get((int) (Math.random() * staticFrames.size()));
        double shift = Math.random() * 6 - 3;                  // the picture jitters up and down
        g.drawImage(frame, 0, shift, w, h);
        g.setFill(Color.rgb(255, 255, 255, 0.35));
        for (int i = 0; i < 3; i++) {                          // horizontal streaks
            g.fillRect(0, Math.random() * h, w, 1 + Math.random() * 4);
        }
        g.setGlobalAlpha(1);
        crtStatic.setVisible(true);
        crtGlass.setOpacity(1 - (FLICKER + 0.4 * strength) * Math.random());   // flickers harder too
    }

    /** Makes the random snow pictures once, at a third of the screen's size (chunky pixels). */
    private void makeStaticFrames(double w, double h) {
        staticFrames.clear();
        int sw = Math.max(1, (int) (w / 3));
        int sh = Math.max(1, (int) (h / 3));
        Random random = new Random();
        for (int f = 0; f < STATIC_FRAMES; f++) {
            WritableImage frame = new WritableImage(sw, sh);
            PixelWriter pixels = frame.getPixelWriter();
            for (int y = 0; y < sh; y++) {
                for (int x = 0; x < sw; x++) {
                    int gray = random.nextInt(256);
                    pixels.setArgb(x, y, 0xFF000000 | gray << 16 | gray << 8 | gray);
                }
            }
            staticFrames.add(frame);
        }
    }

    // ------------------------------------------------------------------ game events

    /** Sounds, apples for the black market, and item uses, for everything that happens in a real game. */
    private void onGameEvent(SnakeMechanics.Event event, long now) {
        switch (event) {
            case CRASHED -> startCrashSequence(now);
            case ATE_APPLE, ATE_GOLDEN_APPLE -> {
                sounds.play(event);
                save.apples++;                 // every apple is money in the black market
                saveItems();
            }
            case ATE_HEART -> {
                save.addHeart();               // +15% chance of finding the black market
                save.save();
            }
            case HAMMER_SMASH -> saveItems();
            case HAMMER_BROKE, WHIP_SNAPPED -> {
                sounds.playItem(event);
                saveItems();
            }
            case REWOUND -> {
                sounds.playItem(event);
                saveItems();
                startStatic(now, new StaticPlan(0, 150, 250, 450, false, false, null, null));
            }
            default -> sounds.play(event);
        }
    }

    /** The black market chance goes back to 0: at the start of every game, and on entering and leaving the market. */
    private void resetSecretChance() {
        save.secretChance = 0;
        save.save();
    }

    /** Copies what's left of the items from the game into the save file. */
    private void saveItems() {
        save.clock = game.hasClock();
        save.hammerUses = game.hammerUsesLeft();
        save.whipGrabs = game.whipGrabsLeft();
        save.save();
    }

    // ------------------------------------------------------------------ black market

    /** Switches the TV to the black market (called at the peak of the static). */
    private void enterMarket() {
        view = View.MARKET;
        market = new BlackMarket(save);
        screenGlass.setFill(Color.BLACK);            // no grass in here: just black
        sounds.startMarketMusic();                   // loops until the player leaves
        marketLastTick = 0;
        cameraX = 0;
        menu = Menu.NONE;
        menuBox.setVisible(false);
    }

    /** Back through the door: static, then the start screen. */
    private void leaveMarket() {
        startStatic(System.nanoTime(), new StaticPlan(0, 400, 700, 800, true, true, () -> {
            view = View.GAME;
            market = null;
            sounds.stopMarketMusic();
            resetSecretChance();
            screenGlass.setFill(darkGrass);           // the grass comes back
            game = newGameFor(mode);   // not startNewGame(): that would cut off the static sound
            lastTick = 0;
            showStartScreen();
        }, null));
    }

    /** Moves the market snake along, and scrolls the view to follow it. */
    private void runMarket(long now) {
        if (marketLastTick == 0) {
            marketLastTick = now;
        }
        long interval = BlackMarket.TICK_MS * 1_000_000L;
        int safety = 0;
        while (now - marketLastTick >= interval && safety++ < 5) {
            marketLastTick += interval;
            BlackMarket.State before = market.state();
            market.step();
            if (before != BlackMarket.State.AT_DOOR && market.state() == BlackMarket.State.AT_DOOR) {
                sounds.playDoor();                     // the snake touched the door: creak
            }
        }
        if (market.state() != BlackMarket.State.WALKING) {
            marketLastTick = now;
        }
        double viewW = game.width();
        double target = Math.max(0, Math.min(BlackMarket.WIDTH - viewW, market.head().x() - viewW / 2));
        cameraX += (target - cameraX) * 0.12;          // glide instead of jumping
    }

    private void onMarketKey(KeyCode key) {
        switch (market.state()) {
            case WAITING, WALKING -> {
                Direction d = directionFor(key);
                if (d != null) {
                    market.turn(d);
                }
            }
            case CHOOSING -> {
                switch (key) {
                    case LEFT, A, UP, W -> market.moveSelection(-1);
                    case RIGHT, D, DOWN, S -> market.moveSelection(1);
                    case ENTER, SPACE -> {
                        switch (market.buy()) {
                            case BOUGHT -> sounds.playBought();             // a creepy laugh
                            case NOT_ENOUGH_APPLES -> sounds.playNotEnough();
                            default -> { }
                        }
                    }
                    case ESCAPE -> market.cancel();
                    default -> { }
                }
            }
            case AT_DOOR -> {
                switch (key) {
                    case ENTER, SPACE -> {
                        market.leave();
                        leaveMarket();
                    }
                    case ESCAPE -> market.cancel();
                    default -> { }
                }
            }
            default -> { }
        }
    }

    private static Direction directionFor(KeyCode key) {
        return switch (key) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ game actions

    private void startNewGame() {
        sounds.stopAll(); // cut off the game-over / win sounds from the last round
        cancelCrashSequence();
        resetSecretChance();   // only hearts from this game count toward the black market
        game = newGameFor(mode);
        lastTick = 0;
        if (cell > 0) {
            draw();
        }
    }

    /** A new game where apples and rocks never appear under the score and info text. */
    private SnakeMechanics newGameFor(Mode m) {
        SnakeMechanics g = new SnakeMechanics(m);
        if (cell > 0) {
            g.keepClear(hudSquares(m));
        }
        g.setItems(save.clock, save.hammerUses, save.whipGrabs);
        return g;
    }

    /**
     * The board squares covered by the score (top right) and the level info (top left),
     * measured from the longest text each can show.
     */
    private Set<Point> hudSquares(Mode m) {
        double pad = cell * 0.6;
        double w = canvas.getWidth();
        Set<Point> squares = new HashSet<>();

        double scoreWidth = Math.max(textWidth("Score: 9999", pixelFont(cell * 0.7)),
                textWidth("High Score: 9999", pixelFont(cell * 0.45)));
        addSquares(squares, w - pad - scoreWidth, 0, w, pad + cell * 1.7);

        String longestInfo = switch (m) {
            case TIME_ATTACK -> "Time: 99.9s";
            case SPEED_RUSH -> "Speed: 22.2/s  9 pts/apple";
            case OBSTACLES -> "Rocks: 999";
            default -> null;
        };
        if (longestInfo != null) {
            addSquares(squares, 0, 0, pad + textWidth(longestInfo, pixelFont(cell * 0.45)), pad + cell * 0.9);
        }
        // The black market items, bottom-left.
        addSquares(squares, 0, canvas.getHeight() - pad - cell, pad + itemsHudWidth(), canvas.getHeight() - 1);
        return squares;
    }

    /** Adds every board square that overlaps the rectangle (x0, y0)-(x1, y1), in pixels. */
    private void addSquares(Set<Point> squares, double x0, double y0, double x1, double y1) {
        for (int x = (int) Math.floor(x0 / cell); x <= (int) Math.floor(x1 / cell); x++) {
            for (int y = (int) Math.floor(y0 / cell); y <= (int) Math.floor(y1 / cell); y++) {
                squares.add(new Point(x, y));
            }
        }
    }

    private static double textWidth(String text, Font font) {
        Text measure = new Text(text);
        measure.setFont(font);
        return measure.getLayoutBounds().getWidth();
    }

    private void quitToMainMenu() {
        cancelCrashSequence();
        loop.stop();
        sounds.stopAll();
        SceneSwitcher.showMenu(root);
    }

    private void onKey(KeyEvent e) {
        if (staticPlan != null && staticPlan.locksKeys()) {
            e.consume();               // switching screens: wait for the static
            return;
        }
        if (view == View.MARKET) {
            onMarketKey(e.getCode());
            e.consume();
            return;
        }
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

        Direction dir = directionFor(e.getCode());

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
        list.add(item("Start", () -> {
            closeMenu();
            startNewGame();
        }));
        list.add(item("Levels", () -> showLevels(Menu.START)));
        list.add(quitItem(this::quitToMainMenu));
        openMenu(Menu.START, "SNAKE", "Level: " + mode.displayName(), list, 0);
    }

    /** Every level, plus Back. The current level is shown in gold and starts out highlighted. */
    private void showLevels(Menu backTo) {
        levelsBackTo = backTo;
        List<MenuItem> list = new ArrayList<>();
        for (Mode m : Mode.values()) {
            list.add(levelItem(m, () -> {
                mode = m;
                pausedByMenu = false;
                closeMenu();
                startNewGame();
            }));
        }
        list.add(item("Back", this::goBack));
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
            list.add(item("Continue", this::closeMenu));
        }
        list.add(item("New Game", () -> {
            pausedByMenu = false;
            closeMenu();
            startNewGame();
        }));
        list.add(item("Levels", () -> showLevels(Menu.PAUSE)));
        list.add(item("Menu", () -> {
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
        children.add(menuLabel(title, cell * 2, TITLE));
        if (subtitle != null) {
            children.add(menuLabel(subtitle, cell * 0.5, SOFT_TEXT));
        }
        // Every button on a screen gets the same text size: the size that fits the longest label.
        double textSize = Double.MAX_VALUE;
        for (MenuItem item : items) {
            textSize = Math.min(textSize, item.label.getFont().getSize());
        }
        for (MenuItem item : items) {
            item.label.setFont(Font.font(PIXEL_FONT, textSize));
            children.add(item.button);
        }
        children.add(menuLabel("W/S or arrows: choose   Enter: select", cell * 0.4, SOFT_TEXT));
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
        label.setFont(pixelFont(size));
        label.setTextFill(color);
        label.setStyle("-fx-effect: dropshadow(one-pass-box, #10240c, 0, 0, 3, 3);");
        label.setMouseTransparent(true);
        return label;
    }

    // ------------------------------------------------------------------ menu buttons

    /** One button in a menu: a jelly button with pixel-font text. Selected = the glowing version. */
    private final class MenuItem {
        final Button button;
        final Runnable action;
        private final ImageView view;
        private final Image normal;
        private final Image glowing;
        final Label label;

        MenuItem(String text, boolean red, Color textColor, Runnable action) {
            this.button = new Button();
            String file = red ? "Buttons/button-red" : "Buttons/button";
            this.normal = loadImage(file + ".png");
            this.glowing = loadImage(file + "-selected.png");
            this.action = action;
            this.view = new ImageView(normal);
            view.setFitHeight(buttonHeight / BUTTON_SHARE);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            label = new Label(text.toUpperCase());
            label.setFont(fitFont(label.getText(), buttonHeight * 0.32, buttonHeight * BUTTON_ASPECT * 0.72));
            label.setTextFill(textColor);
            label.setStyle("-fx-effect: dropshadow(one-pass-box, #1d4d12, 0, 0, 3, 3);");
            label.setMouseTransparent(true);

            button.setGraphic(new StackPane(view, label));
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
            view.setImage(on ? glowing : normal);
        }
    }

    /** A green button. */
    private MenuItem item(String text, Runnable action) {
        return new MenuItem(text, false, BUTTON_TEXT, action);
    }

    /** A level button; the level being played has gold text. */
    private MenuItem levelItem(Mode m, Runnable action) {
        return new MenuItem(m.displayName(), false, m == mode ? MENU_GOLD : BUTTON_TEXT, action);
    }

    /** The red Quit button. */
    private MenuItem quitItem(Runnable action) {
        return new MenuItem("Quit", true, BUTTON_TEXT, action);
    }

    /**
     * The biggest pixel font, up to maxSize, that keeps the text narrower than maxWidth.
     * Sizes go down in steps of 4 px so the letters stay sharp.
     */
    private static Font fitFont(String text, double maxSize, double maxWidth) {
        Text measure = new Text(text);
        for (double size = Math.max(8, Math.floor(maxSize / 4) * 4); size > 8; size -= 4) {
            Font font = Font.font(PIXEL_FONT, size);
            measure.setFont(font);
            if (measure.getLayoutBounds().getWidth() <= maxWidth) {
                return font;
            }
        }
        return Font.font(PIXEL_FONT, 8);
    }

    /**
     * The pixel font at the nearest multiple of 8 px (at least 8). Press Start 2P is drawn
     * on an 8-pixel grid, so these sizes keep every letter sharp.
     */
    private static Font pixelFont(double size) {
        return Font.font(PIXEL_FONT, Math.max(8, Math.round(size / 8) * 8));
    }

    private static String loadPixelFont() {
        InputStream in = SnakeController.class.getResourceAsStream(
                "/edu/farmingdale/csc311capstone/SnakeFonts/PressStart2P-Regular.ttf");
        Font font = in == null ? null : Font.loadFont(in, 16);
        if (font == null) {
            System.err.println("Pixel font missing, using the default font.");
            return "Monospaced";
        }
        return font.getFamily();
    }

    // ------------------------------------------------------------------ drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);   // keep the pixel art crisp
        boolean isDemo = demoIsShowing() && demo != null;
        shown = isDemo ? demo : game;

        // Bright grass on the playing area, with tufts here and there.
        for (int tx = 0; tx < tuftAt.length; tx++) {
            for (int ty = 0; ty < tuftAt[tx].length; ty++) {
                Image tile = tuftAt[tx][ty] ? GRASS_ACCENT : GRASS_MAIN;
                g.drawImage(tile, tx * grassSize, ty * grassSize, grassSize, grassSize);
            }
        }

        // The dark grass around the board is the wall. Wrap modes, where you go through
        // the edge instead of crashing, also get a dashed blue line to show it.
        if (shown.wrapsAround()) {
            g.setLineWidth(3);
            g.setStroke(WRAP_WALL);
            g.setLineDashes(8, 6);
            g.strokeRect(1.5, 1.5, canvas.getWidth() - 3, canvas.getHeight() - 3);
            g.setLineDashes();
        }

        for (Point p : shown.obstacles()) {
            drawTile(g, ROCK, p);
        }
        if (shown.food() != null) {
            drawTile(g, shown.foodIsHeart() ? HEART : APPLE, shown.food());
        }
        if (shown.goldenApple() != null) {
            boolean blinking = shown.goldenTicksLeft() < 12 && (shown.goldenTicksLeft() / 2) % 2 == 0;
            if (!blinking) {
                drawTile(g, GOLDEN_APPLE, shown.goldenApple());
            }
        }

        drawSnake(g);
        if (menu == Menu.NONE) {
            drawHud(g);   // keep the menus uncluttered
            if (!isDemo) {
                drawItemsHud(g);
            }
        }
        drawOverlay(g);
    }

    /**
     * Picks a tile for each piece of the snake by looking at which sides it
     * connects to: the head faces away from its neck, the tail points away from
     * the body, and every middle piece is a straight or a turn.
     */
    private void drawSnake(GraphicsContext g) {
        drawSnakeBody(g, shown.body(), shown.direction(), shown.state() == State.GAME_OVER, 0);
    }

    /**
     * Picks a tile for each piece of the snake by looking at which sides it connects to,
     * and draws it xOffset pixels to the side (the black market scrolls).
     */
    private void drawSnakeBody(GraphicsContext g, List<Point> body, Direction heading, boolean dead, double xOffset) {
        int last = body.size() - 1;
        List<Image> tiles = new ArrayList<>();
        for (int i = 0; i <= last; i++) {
            Point p = body.get(i);
            Direction toFront = i > 0 ? sideTowards(p, body.get(i - 1)) : null;
            Direction toBack = i < last ? sideTowards(p, body.get(i + 1)) : null;

            String name;
            if (i == 0) {
                Direction facing = toBack != null ? toBack.opposite() : heading;
                name = "head-" + word(facing) + (dead ? "-dead" : "");
            } else if (i == last) {
                Direction pointing = toFront != null ? toFront.opposite() : heading.opposite();
                name = "tail-" + word(pointing);
            } else {
                name = middlePiece(toFront, toBack);
            }
            tiles.add(snakeTile(name));
        }

        // A thin dark outline so the green snake stands out on the green grass:
        // draw a very dark copy of every piece nudged up, down, left and right, then the snake on top.
        double alpha = g.getGlobalAlpha();
        int o = Math.max(2, Math.round(cell / 16f));
        g.setEffect(new ColorAdjust(0, 0, -0.85, 0));
        g.setGlobalAlpha(alpha * 0.85);
        for (int[] nudge : new int[][] {{o, 0}, {-o, 0}, {0, o}, {0, -o}}) {
            for (int i = last; i >= 0; i--) {
                Point p = body.get(i);
                g.drawImage(tiles.get(i), p.x() * cell + nudge[0] + xOffset, p.y() * cell + nudge[1], cell, cell);
            }
        }
        g.setEffect(null);
        g.setGlobalAlpha(alpha);

        for (int i = last; i >= 0; i--) {
            Point p = body.get(i);
            g.drawImage(tiles.get(i), p.x() * cell + xOffset, p.y() * cell, cell, cell);
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

    /** The black market items the player owns, bottom-left: the clock, and the hammer and whip with uses left. */
    private void drawItemsHud(GraphicsContext g) {
        double pad = cell * 0.6;
        double size = cell * 0.9;
        double x = pad;
        double y = canvas.getHeight() - pad - size;
        g.setFont(pixelFont(cell * 0.45));
        g.setTextAlign(TextAlignment.LEFT);
        Object[][] owned = {
                {BlackMarket.Item.CLOCK, game.hasClock() ? "" : null},
                {BlackMarket.Item.HAMMER, game.hammerUsesLeft() > 0 ? String.valueOf(game.hammerUsesLeft()) : null},
                {BlackMarket.Item.WHIP, game.whipGrabsLeft() > 0 ? String.valueOf(game.whipGrabsLeft()) : null}};
        for (Object[] item : owned) {
            if (item[1] == null) {
                continue;
            }
            g.drawImage(ITEM_ICONS.get((BlackMarket.Item) item[0]), x, y, size, size);
            x += size + cell * 0.15;
            String count = (String) item[1];
            if (!count.isEmpty()) {
                shadowText(g, count, x, y + size * 0.8, Color.WHITE);
                x += textWidth(count, pixelFont(cell * 0.45));
            }
            x += cell * 0.5;
        }
    }

    /** Room for all three items with their biggest numbers. */
    private double itemsHudWidth() {
        double size = cell * 0.9;
        return 3 * (size + cell * 0.65) + 2 * textWidth("100", pixelFont(cell * 0.45));
    }

    /**
     * The black market: a dark room. Everything is drawn, then darkness is laid over it
     * square by square, lit up near the snake, the altar's candles, the door's lantern and the sign.
     */
    private void drawMarket() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double off = -cameraX * cell;                      // everything in the room shifts by this
        int firstCol = (int) Math.floor(cameraX);
        int lastCol = Math.min(BlackMarket.WIDTH - 1, firstCol + game.width());

        // Floor: a barely visible stone checkerboard.
        for (int x = firstCol; x <= lastCol; x++) {
            for (int y = 0; y < BlackMarket.HEIGHT; y++) {
                g.setFill((x + y) % 2 == 0 ? Color.web("#16141b") : Color.web("#1a1820"));
                g.fillRect(x * cell + off, y * cell, cell + 1, cell);
            }
        }

        // The sign that says which way to go.
        Point arrow = BlackMarket.ARROW;
        g.drawImage(MARKET_ARROW, arrow.x() * cell + off, arrow.y() * cell, cell, cell);
        g.setFont(pixelFont(cell * 0.4));
        g.setTextAlign(TextAlignment.CENTER);
        shadowText(g, "MOVE RIGHT", (arrow.x() + 0.5) * cell + off, (arrow.y() + 1.8) * cell, Color.web("#f0e8c4"));

        // The altar, with a skull candle at each end and the items for sale on top.
        g.drawImage(MARKET_ALTAR, BlackMarket.ALTAR_X * cell + off, BlackMarket.ALTAR_Y * cell,
                BlackMarket.ALTAR_W * cell, BlackMarket.ALTAR_H * cell);
        double candleY = (BlackMarket.ALTAR_Y - 1.3) * cell;
        g.drawImage(SKULL_CANDLE, (BlackMarket.ALTAR_X - 0.1) * cell + off, candleY, cell * 1.3, cell * 1.3);
        g.drawImage(SKULL_CANDLE, (BlackMarket.ALTAR_X + BlackMarket.ALTAR_W - 1.2) * cell + off, candleY, cell * 1.3, cell * 1.3);

        BlackMarket.Item selectedItem = market.selectedItem();
        BlackMarket.Item[] all = BlackMarket.Item.values();
        for (int i = 0; i < all.length; i++) {
            if (!market.owns(all[i]) && all[i] != selectedItem) {   // bought ones aren't on the altar
                Point spot = BlackMarket.ITEM_SPOTS[i];
                g.drawImage(ITEM_ICONS.get(all[i]), spot.x() * cell + off, spot.y() * cell, cell, cell);
            }
        }
        if (market.choices().isEmpty()) {
            g.setFont(pixelFont(cell * 0.4));
            g.setTextAlign(TextAlignment.CENTER);
            shadowText(g, "SOLD OUT", (BlackMarket.ALTAR_X + BlackMarket.ALTAR_W / 2.0) * cell + off,
                    (BlackMarket.ALTAR_Y + 1.3) * cell, Color.web("#c9c3d6"));
        }

        // The door in the corner, with a lantern beside it.
        g.drawImage(MARKET_DOOR, BlackMarket.DOOR_X * cell + off, BlackMarket.DOOR_Y * cell, cell, cell * BlackMarket.DOOR_H);
        g.drawImage(MARKET_LANTERN, (BlackMarket.DOOR_X - 1) * cell + off, (BlackMarket.DOOR_Y + 0.2) * cell, cell * 0.8, cell * 0.8);

        drawSnakeBody(g, market.body(), market.direction(), false, off);

        drawDarkness(g, firstCol, lastCol, off);

        // Over the darkness so they're easy to read: the prices, and the selected item glowing and bobbing.
        for (int i = 0; i < all.length; i++) {
            if (market.owns(all[i])) {
                continue;
            }
            Point spot = BlackMarket.ITEM_SPOTS[i];
            double ix = spot.x() * cell + off;
            if (all[i] == selectedItem) {
                double iy = spot.y() * cell - cell * 0.2 * (1 + Math.sin(System.nanoTime() / 1.5e8));
                g.setFill(Color.rgb(255, 214, 120, 0.3));
                g.fillOval(ix - cell * 0.3, iy - cell * 0.3, cell * 1.6, cell * 1.6);
                g.drawImage(ITEM_ICONS.get(all[i]), ix, iy, cell, cell);
            }
            g.drawImage(APPLE, ix + cell * 0.05, (BlackMarket.ALTAR_Y + 0.9) * cell, cell * 0.45, cell * 0.45);
            g.setFont(pixelFont(cell * 0.3));
            g.setTextAlign(TextAlignment.LEFT);
            shadowText(g, String.valueOf(all[i].price), ix + cell * 0.5, (BlackMarket.ALTAR_Y + 1.3) * cell, Color.web("#ffe45c"));
        }

        // On top of the darkness: the apple counter and any question being asked.
        drawAppleCounter(g, w);
        switch (market.state()) {
            case CHOOSING -> {
                BlackMarket.Item item = market.selectedItem();
                List<String> lines = new ArrayList<>(wrap(item.description, pixelFont(cell * 0.4), w * 0.8));
                lines.add("");
                lines.add("Arrows: choose   Enter: buy   Esc: back");
                drawPanel(g, item.displayName.toUpperCase() + "  -  " + item.price + " apples", lines, market.message());
            }
            case AT_DOOR -> drawPanel(g, "Leave the black market?", List.of("Enter: yes   Esc: no"), "");
            default -> {
                if (!market.message().isEmpty()) {
                    drawPanel(g, market.message(), List.of(), "");
                }
            }
        }
    }

    /** Darkness, one square at a time: dark everywhere except near the lights (chunky, like the pixel art). */
    private void drawDarkness(GraphicsContext g, int firstCol, int lastCol, double off) {
        double flicker = 0.92 + 0.08 * Math.random();     // candle flames aren't steady
        Point head = market.head();
        double[][] lights = {
                {head.x() + 0.5, head.y() + 0.5, SNAKE_LIGHT, 1.0},
                {BlackMarket.ALTAR_X + 0.5, BlackMarket.ALTAR_Y - 0.5, CANDLE_LIGHT * flicker, 1.0},
                {BlackMarket.ALTAR_X + BlackMarket.ALTAR_W - 0.5, BlackMarket.ALTAR_Y - 0.5, CANDLE_LIGHT * flicker, 1.0},
                {BlackMarket.DOOR_X - 0.5, BlackMarket.DOOR_Y + 0.7, LANTERN_LIGHT * flicker, 0.9},
                {BlackMarket.ARROW.x() + 0.5, BlackMarket.ARROW.y() + 0.8, SIGN_LIGHT, 0.7}};
        for (int x = firstCol; x <= lastCol; x++) {
            for (int y = 0; y < BlackMarket.HEIGHT; y++) {
                double light = 0;
                for (double[] l : lights) {
                    double d = Math.hypot(x + 0.5 - l[0], y + 0.5 - l[1]);
                    light = Math.max(light, Math.pow(Math.max(0, 1 - d / l[2]), 0.6) * l[3]);
                }
                double dark = Math.round((1 - light) * 8) / 8.0 * 0.96;   // 8 steps of darkness
                if (dark > 0) {
                    g.setFill(Color.rgb(0, 0, 0, dark));
                    g.fillRect(x * cell + off, y * cell, cell + 1, cell);
                }
            }
        }
    }

    /** An apple and how many the player has, in the top-left corner (the door's lantern is top-right). */
    private void drawAppleCounter(GraphicsContext g, double w) {
        double pad = cell * 0.6;
        g.drawImage(APPLE, pad, pad, cell, cell);
        g.setFont(pixelFont(cell * 0.6));
        g.setTextAlign(TextAlignment.LEFT);
        shadowText(g, "x " + market.apples(), pad + cell * 1.2, pad + cell * 0.8, Color.WHITE);
    }

    /** A dark box at the bottom of the screen with a title, some lines, and an optional message. */
    private void drawPanel(GraphicsContext g, String title, List<String> lines, String message) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        Font small = pixelFont(cell * 0.4);
        double lineH = small.getSize() * 1.8;
        double boxH = cell * 1.6 + lines.size() * lineH + (message.isEmpty() ? 0 : lineH * 1.3);
        double top = h - boxH - cell * 0.6;
        g.setFill(Color.rgb(8, 6, 10, 0.88));
        g.fillRect(cell * 0.6, top, w - cell * 1.2, boxH);
        g.setStroke(Color.web("#6b4a2b"));
        g.setLineWidth(3);
        g.strokeRect(cell * 0.6, top, w - cell * 1.2, boxH);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(pixelFont(cell * 0.5));
        double y = top + cell * 0.9;
        shadowText(g, title, w / 2, y, Color.web("#ffe45c"));
        g.setFont(small);
        y += cell * 0.4;
        for (String line : lines) {
            y += lineH;
            shadowText(g, line, w / 2, y, SOFT_TEXT);
        }
        if (!message.isEmpty()) {
            y += lineH * 1.3;
            boolean bad = message.startsWith("Not");
            shadowText(g, message, w / 2, y, bad ? Color.web("#ff6b6b") : Color.web("#9be37a"));
        }
    }

    /** Score and high score in the top-right corner; mode info (time, speed, rocks) top-left. */
    private void drawHud(GraphicsContext g) {
        double pad = cell * 0.6;
        double w = canvas.getWidth();
        int best = Math.max(HIGH_SCORES.getOrDefault(game.mode(), 0), game.score());

        g.setTextAlign(TextAlignment.RIGHT);
        g.setFont(pixelFont(cell * 0.7));
        shadowText(g, "Score: " + game.score(), w - pad, pad + cell * 0.7, Color.WHITE);
        g.setFont(pixelFont(cell * 0.45));
        shadowText(g, "High Score: " + best, w - pad, pad + cell * 1.6, MENU_GOLD);

        String info = switch (game.mode()) {
            case TIME_ATTACK -> String.format("Time: %.1fs", game.timeLeftMs() / 1000.0);
            case SPEED_RUSH -> String.format("Speed: %.1f/s  %d pts/apple",
                    1000.0 / game.tickIntervalMs(), game.pointsPerApple());
            case OBSTACLES -> "Rocks: " + game.obstacles().size();
            default -> null;
        };
        if (info != null) {
            boolean lowTime = game.mode() == Mode.TIME_ATTACK && game.timeLeftMs() < 10_000;
            g.setTextAlign(TextAlignment.LEFT);
            g.setFont(pixelFont(cell * 0.45));
            shadowText(g, info, pad, pad + cell * 0.7, lowTime ? Color.web("#ff6b6b") : Color.WHITE);
        }
    }

    private void drawOverlay(GraphicsContext g) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        if (menu != Menu.NONE) {
            g.setFill(Color.web("#000000", demoIsShowing() ? 0.35 : 0.72));
            g.fillRect(0, 0, w, h);
            return; // the menu buttons are drawn on top by JavaFX
        }

        String title;
        List<String> lines = new ArrayList<>();
        switch (game.state()) {
            case READY -> {
                if (game.justRewound()) {
                    title = "Rewind!";
                    lines.add("The clock turned time back 3 seconds.");
                    lines.add("");
                    lines.add("Press an arrow key to keep going");
                    break;
                }
                title = mode.displayName();
                lines.add(mode.description());
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

        // Wrap every line so long ones fit (the pixel font is wide).
        List<String> wrapped = new ArrayList<>();
        for (String line : lines) {
            if (line.isEmpty()) {
                wrapped.add(line);
            } else {
                wrapped.addAll(wrap(line, smallFont(), w * 0.85));
            }
        }
        lines = wrapped;

        double lineHeight = smallFont().getSize() * 1.8;
        double blockHeight = cell * 2.2 + lines.size() * lineHeight;
        double y = (h - blockHeight) / 2 + cell * 1.6;

        g.setTextAlign(TextAlignment.CENTER);
        g.setFont(pixelFont(cell * 1.4));
        shadowText(g, title, w / 2, y, Color.WHITE);

        y += cell * 1.4;
        g.setFont(smallFont());
        for (String line : lines) {
            y += lineHeight;
            shadowText(g, line, w / 2, y, SOFT_TEXT);
        }
    }

    private Font smallFont() {
        return pixelFont(cell * 0.5);
    }

    private static void shadowText(GraphicsContext g, String text, double x, double y, Color color) {
        g.setFill(Color.web("#10240c", 0.85));
        g.fillText(text, x + 3, y + 3);
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

    /** Loads a pixel-art image already scaled to size x size with no blurring. */
    private static Image loadImage(String fileName, double size) {
        InputStream in = SnakeController.class.getResourceAsStream(GRAPHICS + fileName);
        if (in == null) {
            throw new IllegalStateException("Missing Snake graphic: " + GRAPHICS + fileName);
        }
        return new Image(in, size, size, false, false);
    }

    private static Image loadImage(String fileName) {
        InputStream in = SnakeController.class.getResourceAsStream(GRAPHICS + fileName);
        if (in == null) {
            throw new IllegalStateException("Missing Snake graphic: " + GRAPHICS + fileName);
        }
        return new Image(in);
    }
}
