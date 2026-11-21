package edu.farmingdale.csc311capstone.SpaceInvaders;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Alien;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Bullet;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Extermination;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Shield;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;

/**
 * Connects spaceinvaders-view.fxml to {@link SpaceInvadersMechanics}: runs the game loop,
 * reads the keyboard and draws the game on a Canvas.
 *
 * The game opens on its own menu: a big pixel title and three options picked with the arrow keys
 * and Space. Start Game opens the mode select; Planet Defense shows its intro (what each alien is worth) for 5 seconds,
 * which Space skips, and then the game begins. During a game, Esc or the Space Invaders Menu button
 * returns to the menu. Sandbox (a developer screen, to be hidden before release) is where Extermination
 * levels get designed; Esc leaves it.
 *
 * Controls: Left/Right or A/D to move, Space to fire, Enter to start or play again after a game over.
 */
public class SpaceInvadersController {

    private static final Color CANNON = Color.LIMEGREEN;
    private static final Color SHIELD = Color.LIMEGREEN;
    private static final Color SQUID = Color.MAGENTA;
    private static final Color CRAB = Color.CYAN;
    private static final Color OCTOPUS = Color.LIMEGREEN;
    private static final Color ALIEN_SHOT = Color.ORANGERED;
    private static final Color MYSTERY = Color.RED;

    private static final Color TITLE = Color.LIMEGREEN;
    private static final Color OPTION = Color.WHITE;
    private static final Color OPTION_UNAVAILABLE = Color.GRAY;
    private static final Color MENU_ARROW = Color.YELLOW;

    /** Menu options, top to bottom. */
    private static final String[] MENU_OPTIONS = {"START GAME", "SCOREBOARD", "SANDBOX", "BACK TO MAIN MENU"};
    private static final int START_GAME = 0;
    private static final int SCOREBOARD = 1;
    /** Developer-only level designer for Extermination. To be hidden or removed before release. */
    private static final int SANDBOX = 2;
    private static final int BACK_TO_MAIN_MENU = 3;
    private static final Color OPTION_DEVELOPER = Color.ORANGE;

    /** Game modes on the mode select screen, top to bottom. Extermination isn't built yet. */
    private static final String[] MODE_OPTIONS = {"PLANET DEFENSE", "EXTERMINATION", "BACK"};
    private static final int PLANET_DEFENSE = 0;
    private static final int EXTERMINATION = 1;
    private static final int MODE_BACK = 2;

    /** How long the Planet Defense intro shows before the game starts on its own. */
    private static final double INTRO_SECONDS = 5;

    /** Which screen is showing. */
    private enum Screen { MENU, MODE_SELECT, INTRO, PLAYING, SANDBOX, RUN, SCORES }

    /** Where an Extermination run is: playing a level, between levels, just ended, or typing a name. */
    private enum RunPhase { PLAYING, STAGE_CLEAR, CONTINUE, ENDED, NAME }

    private static final String RUN_CONTROLS =
            "Arrows: move    Shift: focus    Z: fire    X: bomb    Enter: pause    Esc: quit";

    private static final String GAME_CONTROLS = "Left/Right or A/D: move    Space: fire    Enter: play again    Esc: menu";
    private static final String SANDBOX_CONTROLS =
            "SANDBOX (developer)    Enter: play/pause    Arrows: fly    Shift: focus    Z: fire    X: bomb    M: next power stage    Esc: menu";
    /** What the mouse does with each editor tool, shown under the tool buttons. */
    private static final String PLACE_TOOL_HINT = """
            Click: place (at an edge it flies in)
            Click an enemy or a path dot: select it
            Drag an enemy: move it with its path
            Right click an enemy: delete it""";
    private static final String PATH_TOOL_HINT = """
            Select an enemy, then click or drag to
            draw its path (at an edge it flies off)
            Right click: undo the last click or drag""";

    /** What each bullet pattern does, shown under the pattern buttons. */
    private static final Map<Extermination.Pattern, String> PATTERN_HINTS = Map.of(
            Extermination.Pattern.NONE, "Doesn't shoot.",
            Extermination.Pattern.AIMED, "At the player; extra bullets form a line.",
            Extermination.Pattern.FAN, "A spread of bullets aimed at the player.",
            Extermination.Pattern.RING, "A full circle of bullets.",
            Extermination.Pattern.SPIRAL, "A circle that turns a bit every shot.",
            Extermination.Pattern.RANDOM, "Bullets sprayed at random toward the player.",
            Extermination.Pattern.LASER, "Beams: a warning line, then the beam. Spread fans out several (360 = all around); size sets the width.",
            Extermination.Pattern.WALL, "A straight line of bullets side by side, all flying the same way.");
    /** What each aim does, shown on the Aim tab. */
    private static final Map<Extermination.Aim, String> AIM_HINTS = Map.of(
            Extermination.Aim.PLAYER, "Every shot goes at you.",
            Extermination.Aim.ONCE, "Locks onto you at the phase's first shot, then keeps shooting that way. Good for walls.",
            Extermination.Aim.FIXED, "Always the direction below, wherever you are. Aim at a wall and add bounces, or park "
                    + "enemies with long lasers to build a cage.");
    /** What each bullet color looks like. */
    private static final Map<Extermination.BulletColor, Color> BULLET_COLORS = Map.of(
            Extermination.BulletColor.PINK, Color.web("#ff4fa3"),
            Extermination.BulletColor.RED, Color.web("#ff3b3b"),
            Extermination.BulletColor.ORANGE, Color.web("#ff9a2e"),
            Extermination.BulletColor.YELLOW, Color.web("#ffe14a"),
            Extermination.BulletColor.GREEN, Color.web("#4cff6a"),
            Extermination.BulletColor.BLUE, Color.web("#4aa8ff"),
            Extermination.BulletColor.PURPLE, Color.web("#b46bff"),
            Extermination.BulletColor.WHITE, Color.web("#f2f2f2"));
    private static final Color PATH = Color.web("#555555");
    private static final Color PATH_SELECTED = Color.YELLOW;
    /** How much one press of - or + changes the selected enemy's speed. */
    private static final double SPEED_STEP = 20;
    /** While dragging a path, a waypoint is dropped each time the mouse has moved this many pixels. */
    private static final double DRAG_POINT_SPACING = 12;
    /** How much one press of , or . changes the selected enemy's spawn time, in seconds. */
    private static final double SPAWN_TIME_STEP = 0.5;

    // Extermination colours
    private static final Color SHIP = Color.WHITE;
    private static final Color HITBOX = Color.RED;
    private static final Color SHIP_SHOT = Color.web("#9fe8ff");

    /** Best score for as long as the app is open. */
    private static int highScore = 0;

    /** The player ship's picture (from SpaceInvadersGraphics), or null if it's missing: then a plain arrow is drawn. */
    private static final javafx.scene.image.Image PLAYER_SHIP = loadImage("player_ship.png");
    /** Degrees the ship turns at full lean, and how much narrower it gets, so it looks like it banks. */
    private static final double SHIP_LEAN_ANGLE = 8;
    private static final double SHIP_LEAN_SQUASH = 0.18;
    /** The engine flame pictures, played in a loop under each engine so it looks like it's running. */
    private static final javafx.scene.image.Image[] EXHAUST = {
            loadImage("exhaust_1.png"), loadImage("exhaust_2.png"),
            loadImage("exhaust_3.png"), loadImage("exhaust_4.png")};
    /** How long each flame picture shows, in milliseconds. Lower = faster flicker. */
    private static final int EXHAUST_FRAME_MS = 60;
    /** Where the two engines are on player_ship.png, in pixels from its left edge, and the row the flames start on. */
    private static final double[] EXHAUST_X = {15, 24};
    private static final double EXHAUST_Y = 39;
    /**
     * The ship's bullet for each power stage, in the same order as POWER_LEVELS (0, 50, 90 power...).
     * A stage past the end of this list uses the last one. At full power it fires PLAYER_ROCKET instead.
     */
    private static final javafx.scene.image.Image[] PLAYER_BULLETS = {
            loadImage("player_bullet_default_power.png"), // yellow, from 0 power
            loadImage("player_bullet_stage_2.png"),       // red, from 50 power
            loadImage("player_bullet_stage_1.png")};      // blue, from 90 power
    private static final javafx.scene.image.Image PLAYER_ROCKET = loadImage("player_bullet_max_power.png");
    /** How big each is drawn on screen, in pixels {width, height}. */
    private static final double[] PLAYER_BULLET_SIZE = {8, 31};
    private static final double[] PLAYER_ROCKET_SIZE = {15, 21};
    /** The aliens' pictures: two frames each, swapped back and forth so they look like they're moving. */
    /** Each is recolored to its alien's color (SQUID, CRAB, OCTOPUS above), keeping the picture's light and dark parts. */
    private static final javafx.scene.image.Image[] SQUID_FRAMES = {tint(loadImage("Squid_v1.png"), SQUID), tint(loadImage("Squid_v2.png"), SQUID)};
    private static final javafx.scene.image.Image[] CRAB_FRAMES = {tint(loadImage("Crab_v1.png"), CRAB), tint(loadImage("Crab_v2.png"), CRAB)};
    private static final javafx.scene.image.Image[] OCTOPUS_FRAMES = {tint(loadImage("Octopus_v1.png"), OCTOPUS), tint(loadImage("Octopus_v2.png"), OCTOPUS)};
    /** How many screen pixels each picture pixel takes (keep it a whole number so the pixels stay even). */
    private static final double ALIEN_PIXEL_SCALE = 2;
    /** Item pictures: a small power item (one bullet, tilted up-left or up-right) and a big one (three bullets). */
    private static final javafx.scene.image.Image SMALL_POWER_ITEM = loadImage("small_power_box.png");
    private static final javafx.scene.image.Image BIG_POWER_ITEM = loadImage("big_power_box.png");
    private static final javafx.scene.image.Image POINT_ITEM = loadImage("score_box.png");
    /** The bomb picture: the bomb pickup, and one per bomb in stock on the HUD. */
    private static final javafx.scene.image.Image BOMB_ITEM = loadImage("bomb_stock_art.png");
    // Heart picture for each life in the HUD
    private static final javafx.scene.image.Image LIFE_ICON = loadImage("heart_stock_art.png");
    /** Enemy death animation, played in order (small and tank enemies). */
    private static final javafx.scene.image.Image[] ENEMY_DEATH_FRAMES = {
            loadImage("enemy_death_frame1.png"), loadImage("enemy_death_frame2.png"),
            loadImage("enemy_death_frame3.png"), loadImage("enemy_death_frame4.png")};
    /** How many screen pixels each death animation pixel takes (bigger enemies get a bigger blast). */
    private static final double ENEMY_DEATH_PIXEL_SCALE = 3;
    /** Flak explosion from a rocket kill, played in order. */
    private static final javafx.scene.image.Image[] FLAK_FRAMES = new javafx.scene.image.Image[8];
    static {
        for (int i = 0; i < FLAK_FRAMES.length; i++) {
            FLAK_FRAMES[i] = loadImage("flak_gun_explosion_frame" + (i + 1) + ".png");
        }
    }
    /** How many screen pixels each flak explosion pixel takes (4.8 makes the biggest frame about as wide as FLAK_RADIUS reaches). */
    private static final double FLAK_PIXEL_SCALE = 4.8;
    /** How many screen pixels each item picture pixel takes, and how far a small power item tilts (degrees). */
    private static final double ITEM_PIXEL_SCALE = 1;
    private static final double SMALL_POWER_TILT = 45;
    /** How long each alien frame shows, in milliseconds. */
    private static final int ALIEN_FRAME_MS = 400;

    /**
     * A copy of a picture in another color: each pixel takes the color, as bright as that pixel was
     * compared with the brightest one, so shading stays. Null stays null.
     */
    private static javafx.scene.image.Image tint(javafx.scene.image.Image image, Color color) {
        if (image == null) {
            return null;
        }
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        javafx.scene.image.PixelReader in = image.getPixelReader();
        double brightest = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                brightest = Math.max(brightest, in.getColor(x, y).getBrightness());
            }
        }
        javafx.scene.image.WritableImage out = new javafx.scene.image.WritableImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color pixel = in.getColor(x, y);
                double shade = brightest == 0 ? 1 : pixel.getBrightness() / brightest;
                out.getPixelWriter().setColor(x, y, Color.color(color.getRed() * shade, color.getGreen() * shade,
                        color.getBlue() * shade, pixel.getOpacity()));
            }
        }
        return out;
    }

    /** Loads a picture from the SpaceInvadersGraphics folder; null if it isn't there. */
    private static javafx.scene.image.Image loadImage(String name) {
        java.io.InputStream in = SpaceInvadersController.class.getResourceAsStream(
                "/edu/farmingdale/csc311capstone/SpaceInvadersGraphics/" + name);
        return in == null ? null : new javafx.scene.image.Image(in);
    }

    @FXML private BorderPane root;
    @FXML private Label scoreLabel;
    @FXML private Label bestLabel;
    @FXML private Label livesLabel;
    @FXML private Label waveLabel;
    @FXML private Canvas canvas;
    @FXML private HBox topBar;
    @FXML private Button titleButton;
    @FXML private Label controlsLabel;

    // Sandbox level editor panel
    @FXML private ScrollPane sandboxPanel;
    @FXML private Button playButton;
    @FXML private Label waveTitle;
    @FXML private Label waveRuleLabel;
    @FXML private HBox waveRuleButtons;
    @FXML private ToggleButton afterClearedButton;
    @FXML private ToggleButton afterStartedButton;
    @FXML private Label waitLabel;
    @FXML private Spinner<Double> waitSpinner;
    @FXML private CheckBox waveBombCheck;
    @FXML private ToggleButton placeToolButton;
    @FXML private ToggleButton pathToolButton;
    @FXML private Label toolHint;
    @FXML private Label enemyTypeTitle;
    @FXML private HBox enemyTypeButtons;
    @FXML private ToggleButton squidButton;
    @FXML private ToggleButton crabButton;
    @FXML private ToggleButton octopusButton;
    @FXML private Label selectedTitle;
    @FXML private TabPane selectedTabs;
    @FXML private Label stopPointName;
    @FXML private Label repeatGapLabel;
    @FXML private Label untilLabel;
    @FXML private ToggleButton repeatTimerButton;
    @FXML private ToggleButton repeatGoneButton;
    @FXML private Label stopHint;
    @FXML private Label speedLabel;
    @FXML private Spinner<Double> speedSpinner;
    @FXML private Label countLabel;
    @FXML private Spinner<Double> countSpinner;
    @FXML private Spinner<Double> healthSpinner;
    @FXML private Spinner<Double> repeatsSpinner;
    @FXML private Spinner<Double> repeatGapSpinner;
    @FXML private MenuButton formationButton;
    @FXML private Spinner<Double> spacingSpinner;
    @FXML private Label spawnLabel;
    @FXML private Spinner<Double> spawnSpinner;
    @FXML private CheckBox loopCheck;
    @FXML private Label stopLabel;
    @FXML private Spinner<Double> stopSpinner;
    @FXML private CheckBox fireStoppedCheck;
    @FXML private CheckBox faceMovementCheck;
    @FXML private MenuButton pointingButton;
    @FXML private CheckBox bossCheck;
    @FXML private javafx.scene.layout.VBox bossBox;
    @FXML private TextField cardNameField;
    @FXML private Spinner<Double> cardHealthSpinner;
    @FXML private ToggleButton moveStayButton;
    @FXML private ToggleButton moveWanderButton;
    @FXML private Spinner<Double> hopSpinner;
    @FXML private Label lastsLabel;
    @FXML private Label attackMessage;
    @FXML private javafx.scene.control.Tab bossTab;
    @FXML private javafx.scene.layout.VBox bossTabBox;
    @FXML private javafx.scene.layout.VBox bulletsBox;
    @FXML private javafx.scene.layout.VBox attackBox;
    @FXML private javafx.scene.layout.VBox groupBox;
    @FXML private javafx.scene.layout.VBox aimSettings;
    @FXML private javafx.scene.layout.VBox directionBox;
    @FXML private HBox aimButtons;
    @FXML private Label attackTitle;
    @FXML private Label patternsInfo;
    @FXML private Label aimTitle;
    @FXML private Label pathTitle;
    @FXML private Spinner<Double> delaySpinner;
    @FXML private javafx.scene.layout.VBox pointFireBox;
    @FXML private MenuButton atPointButton;
    @FXML private MenuButton quickPatternMenu;
    @FXML private Label pointAttacksTitle;
    @FXML private javafx.scene.layout.VBox pointAttackList;
    @FXML private MenuButton addPointAttackMenu;
    @FXML private javafx.scene.layout.HBox pointShotsRow;
    @FXML private Label pointShotsLabel;
    @FXML private Spinner<Double> pointShotsSpinner;
    @FXML private ToggleButton movePathButton;
    @FXML private javafx.scene.layout.VBox routeBox;
    @FXML private ToggleButton drawRouteButton;
    @FXML private Label routeStopLabel;
    @FXML private Spinner<Double> routeStopSpinner;
    @FXML private Spinner<Double> routeSpeedSpinner;
    @FXML private ToggleButton routeOnceButton;
    @FXML private ToggleButton routeLoopButton;
    @FXML private ToggleButton routeBounceButton;
    @FXML private Label levelInfo;
    @FXML private Label saveMessage;
    @FXML private ToggleButton patternNone;
    @FXML private ToggleButton patternAimed;
    @FXML private ToggleButton patternFan;
    @FXML private ToggleButton patternRing;
    @FXML private ToggleButton patternSpiral;
    @FXML private ToggleButton patternRandom;
    @FXML private ToggleButton patternLaser;
    @FXML private ToggleButton patternWall;
    @FXML private MenuButton quickWaveMenu;
    @FXML private ToggleButton aimPlayer;
    @FXML private ToggleButton aimOnce;
    @FXML private ToggleButton aimFixed;
    @FXML private Label aimHint;
    @FXML private Label directionLabel;
    @FXML private Spinner<Double> directionSpinner;
    @FXML private Spinner<Double> sweepSpinner;
    @FXML private Spinner<Double> swingSpinner;
    @FXML private Spinner<Double> beamSpinner;
    @FXML private Spinner<Double> wallWidthSpinner;
    @FXML private Spinner<Double> bulletSizeSpinner;
    @FXML private Button colorButton;
    @FXML private Button shapeButton;
    @FXML private Spinner<Double> bouncesSpinner;
    @FXML private Spinner<Double> stepSplitSpinner;
    @FXML private Label patternHint;
    @FXML private Label bulletCountLabel;
    @FXML private Spinner<Double> bulletCountSpinner;
    @FXML private Label spreadLabel;
    @FXML private Spinner<Double> spreadSpinner;
    @FXML private Label bulletSpeedLabel;
    @FXML private Spinner<Double> bulletSpeedSpinner;
    @FXML private Label intervalLabel;
    @FXML private Spinner<Double> intervalSpinner;
    @FXML private Label firstShotLabel;
    @FXML private Spinner<Double> firstShotSpinner;
    @FXML private Label spinLabel;
    @FXML private Spinner<Double> spinSpinner;
    @FXML private Label followLabel;
    @FXML private ToggleButton followClearedButton;
    @FXML private ToggleButton followSpawnedButton;
    @FXML private Label phaseTitle;
    @FXML private Spinner<Double> lastsSpinner;
    @FXML private Label movesTitle;
    @FXML private Spinner<Double> accelSpinner;
    @FXML private Spinner<Double> curveSpinner;
    @FXML private Spinner<Double> waveSpinner;
    @FXML private Spinner<Double> waveRateSpinner;
    @FXML private HBox fromRow;
    @FXML private Button fromEnemyButton;
    @FXML private ToggleButton fromSpotButton;
    @FXML private Label fromHint;
    @FXML private Label stepTitle;
    @FXML private javafx.scene.layout.VBox stepBox;
    @FXML private Spinner<Double> stepTimeSpinner;
    @FXML private Spinner<Double> stepSpeedSpinner;
    @FXML private Spinner<Double> stepAccelSpinner;
    @FXML private Spinner<Double> stepCurveSpinner;
    @FXML private Spinner<Double> stepTurnSpinner;
    @FXML private CheckBox stepAimCheck;
    /** Set while the panel copies the selected enemy into the sliders, so that doesn't count as a change. */
    private boolean refreshingPanel = false;

    private SpaceInvadersMechanics game;
    /** The Extermination game running in the sandbox. */
    private Extermination sandbox;
    /** The alien type a click places in the sandbox (keys 1, 2, 3). */
    private Alien.Type placingType = Alien.Type.SQUID;
    /** Place mode puts down enemies; path mode gives the selected enemy its flight path (key P). */
    private boolean pathMode = false;
    /** The enemy whose path is being edited in path mode. */
    private Extermination.EnemyDesign selectedEnemy;
    /** The point of the selected enemy's path whose stop is being edited: 0 is the start, 1 the first waypoint. */
    private int selectedPoint = 0;
    /** The enemy selectedPoint belongs to; picking another enemy goes back to its start. */
    private Extermination.EnemyDesign pointOwner;
    /** True after "Wait for a group" is pressed: the next click on an enemy picks the group to wait for. */
    private boolean pickingFollow = false;
    /** True while the next click on the map picks the group the selected one keeps coming until. */
    private boolean pickingUntil = false;
    /** True while the next click on the map picks the spot the selected pattern fires from. */
    private boolean pickingFromSpot = false;
    /** The pattern whose fire spot (cyan cross) is being dragged, or null. */
    private Extermination.ShotPhase draggingSpot = null;
    /** Shown under the wait buttons when a picked group can't be used. */
    private String followProblem = "";
    /** The selected enemy's shot phase being edited (0 is the first), and the bullet step in it. */
    private int selectedPhase = 0;
    /** True while clicks on the map add points to the boss attack's own movement path; and the point being edited. */
    private boolean drawingRoute = false;
    private int routePoint = 0;
    private int selectedStep = 0;
    /** How close (pixels) a click must be to a path dot to select it. */
    private static final double POINT_PICK_RADIUS = 8;
    /** A new boss path point this close (px) to level with the last one lines up with it exactly. */
    private static final double ROUTE_SNAP = 15;
    /** The wave being edited: new enemies go in it, and only its enemies are shown and picked. */
    private int editWave = 0;
    /** While paused the sandbox stands still, so enemies and paths can be set up calmly. */
    private boolean paused = true;
    /** True while the mouse button is held down drawing the selected enemy's path. */
    private boolean drawingPath = false;
    /** Set while the mouse is held on a group in the Place tool: dragging then moves the group. */
    private boolean draggingGroup = false;
    /** Whether that drag has gone far enough to count as a move (a plain click shouldn't nudge the path). */
    private boolean groupMoved = false;
    /** Whether that press was on the group that was already selected. */
    private boolean pressedOnSelected = false;
    private double dragFromX;
    private double dragFromY;
    private static final double MOVE_THRESHOLD = 4;
    /** Where the mouse is over the canvas, for the placement preview. Negative when it's off the canvas. */
    private double mouseX = -1;
    private double mouseY = -1;
    private Screen screen = Screen.MENU;
    private double introTimeLeft;
    /**
     * Set when a Space press picks a menu option or skips the intro, so that the same press (or its key
     * repeat while held) doesn't also skip the intro or fire the first shot. Cleared when Space is let go.
     */
    private boolean waitForSpaceRelease;
    private int selectedOption = START_GAME;
    private int selectedMode = PLANET_DEFENSE;
    private final Set<KeyCode> heldKeys = new HashSet<>();
    private long lastFrame;
    /** Time not yet played in the sandbox, which runs in fixed ticks of 1/60 s. */
    private double sandboxTimeOwed = 0;
    /** How far the ship leans now, from -1 (left) to 1 (right); it eases toward the way it's flying. */
    private double shipLean = 0;
    /** Set when X is pressed, so the next game tick uses a bomb. */
    private boolean bombQueued = false;
    // An Extermination run. While it's on, `sandbox` holds the run's game and the editor's level waits in editorLevel.
    private boolean inRun = false;
    private Extermination editorLevel;
    private List<String> runLevels;
    private int runStage;
    private RunPhase runPhase;
    private double runTimer;
    private boolean runWon;
    private boolean runPaused;
    private double runTimeOwed;
    /** On the CONTINUE? screen: true while YES is picked. */
    private boolean continueYes = true;
    private final char[] nameLetters = {'A', 'A', 'A'};
    private int nameSlot;
    /** The scoreboard, read when first shown; and the line to light up on it (-1 for none). */
    private Extermination.Scoreboard scoreboard;
    private int scoreboardHighlight = -1;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double deltaSeconds = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
            lastFrame = now;
            // Cap the step so a slow frame doesn't teleport the cannon
            deltaSeconds = Math.min(deltaSeconds, 0.05);
            switch (screen) {
                case MENU -> drawMenu();
                case MODE_SELECT -> drawModeSelect();
                case INTRO -> {
                    introTimeLeft -= deltaSeconds;
                    if (introTimeLeft <= 0) {
                        startGame();
                    } else {
                        drawIntro();
                    }
                }
                case PLAYING -> {
                    boolean fire = heldKeys.contains(KeyCode.SPACE) && !waitForSpaceRelease;
                    game.update(deltaSeconds, horizontalInput(), fire);
                    draw();
                }
                case RUN -> {
                    updateRun(deltaSeconds);
                    drawRun();
                }
                case SCORES -> drawScoreboard();
                case SANDBOX -> {
                    if (!paused) {
                        // Fixed 60 ticks per second whatever the screen's frame rate, so patterns repeat exactly
                        boolean fire = heldKeys.contains(KeyCode.Z) && !waitForSpaceRelease;
                        sandboxTimeOwed += deltaSeconds;
                        while (sandboxTimeOwed >= Extermination.TICK) {
                            sandbox.tick(axis(KeyCode.LEFT, KeyCode.RIGHT), axis(KeyCode.UP, KeyCode.DOWN),
                                    heldKeys.contains(KeyCode.SHIFT), fire, bombQueued);
                            bombQueued = false;
                            sandboxTimeOwed -= Extermination.TICK;
                        }
                    }
                    drawSandbox();
                    refreshPanel();
                }
            }
        }
    };

    @FXML
    private void initialize() {
        game = new SpaceInvadersMechanics();
        canvas.setWidth(SpaceInvadersMechanics.PLAYFIELD_WIDTH);
        canvas.setHeight(SpaceInvadersMechanics.PLAYFIELD_HEIGHT);

        root.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                attachTo(scene);
            }
        });

        canvas.setOnMousePressed(this::onSandboxMouse);
        canvas.setOnMouseDragged(this::onSandboxDrag);
        canvas.setOnMouseReleased(e -> {
            drawingPath = false;
            if (draggingGroup && pressedOnSelected && !groupMoved) {
                selectedEnemy = groupAt(e.getX(), e.getY()); // a plain click on stacked groups picks the next
                pointOwner = selectedEnemy;
                selectedPoint = 0;
            }
            draggingGroup = false;
            pressedOnSelected = false;
            draggingSpot = null;
        });
        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });
        canvas.setOnMouseExited(e -> mouseX = -1);

        // Number boxes: type a value (Enter to confirm, Esc to cancel) or use the arrows.
        // A change applies to the selected enemy, or to the wave being edited, right away.
        setUpNumberBox(speedSpinner, Extermination.MIN_ENEMY_SPEED, Extermination.MAX_ENEMY_SPEED, 10, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpeed(v); });
        setUpNumberBox(spacingSpinner, Extermination.MIN_SPACING, Extermination.MAX_SPACING, 5, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpacing(v); });
        setUpNumberBox(repeatsSpinner, 0, Extermination.MAX_REPEATS, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setRepeats((int) Math.round(v)); });
        setUpNumberBox(repeatGapSpinner, Extermination.MIN_REPEAT_GAP, Extermination.MAX_REPEAT_GAP, 0.5, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setRepeatGap(v); });
        setUpNumberBox(healthSpinner, 1, Extermination.MAX_HEALTH, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setHealth((int) Math.round(v)); });
        setUpNumberBox(countSpinner, 1, Extermination.MAX_COUNT, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setCount((int) Math.round(v)); });
        setUpNumberBox(spawnSpinner, 0, Extermination.MAX_SPAWN_TIME, 0.5, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpawnTime(v); });
        setUpNumberBox(waitSpinner, 0, Extermination.MAX_WAVE_WAIT, 0.5, 1,
                v -> { if (sandbox != null) sandbox.getWaves().get(editWave).setWait(v); });
        setUpNumberBox(bulletCountSpinner, 1, Extermination.MAX_BULLETS_PER_SHOT, 1, 0,
                v -> { if (phase() != null) phase().setBulletCount((int) Math.round(v)); });
        setUpNumberBox(spreadSpinner, 0, 360, 5, 0,
                v -> { if (phase() != null) phase().setSpread(v); });
        setUpNumberBox(bulletSpeedSpinner, Extermination.MIN_BULLET_SPEED, Extermination.MAX_BULLET_SPEED, 10, 0,
                v -> { if (phase() != null) phase().setBulletSpeed(v); });
        setUpNumberBox(intervalSpinner, Extermination.MIN_FIRE_INTERVAL, Extermination.MAX_FIRE_INTERVAL, 0.05, 2,
                v -> { if (phase() != null) phase().setFireInterval(v); });
        setUpNumberBox(firstShotSpinner, 0, Extermination.MAX_FIRST_SHOT_TIME, 0.1, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setFirstShotDelay(v); });
        setUpNumberBox(spinSpinner, -90, 90, 1, 0,
                v -> { if (phase() != null) phase().setSpin(v); });
        setUpNumberBox(bulletSizeSpinner, Extermination.MIN_BULLET_SIZE, Extermination.MAX_BULLET_SIZE, 1, 0,
                v -> { if (phase() != null) phase().setBulletSize(v); });
        setUpNumberBox(bouncesSpinner, 0, Extermination.MAX_BOUNCES, 1, 0,
                v -> { if (phase() != null) phase().setBounces((int) Math.round(v)); });
        setUpNumberBox(stepSplitSpinner, 0, Extermination.MAX_SPLIT, 1, 0,
                v -> { if (step() != null) step().setSplit((int) Math.round(v)); });
        // Formations: one menu item per shape
        for (Extermination.Formation shape : Extermination.Formation.values()) {
            MenuItem item = new MenuItem(formationName(shape));
            item.setOnAction(e -> { if (selectedEnemy != null) selectedEnemy.setFormation(shape); });
            formationButton.getItems().add(item);
        }
        // How each enemy's picture points as it flies
        for (Extermination.Pointing pointing : Extermination.Pointing.values()) {
            MenuItem item = new MenuItem(pointingName(pointing));
            item.setOnAction(e -> { if (selectedEnemy != null) { selectedEnemy.setPointing(pointing); refreshPanel(); } });
            pointingButton.getItems().add(item);
        }
        // Ready-made patterns: picking one replaces the selected pattern's shot settings
        for (String[] preset : Extermination.QUICK_PATTERNS) {
            MenuItem item = new MenuItem(preset[0]);
            item.setOnAction(e -> {
                if (phase() != null) {
                    phase().applyPattern(preset[1]);
                    selectedStep = 0;
                    showAttackMessage("This pattern is now: " + preset[0] + ". Change anything you like.", false);
                }
            });
            quickPatternMenu.getItems().add(item);
        }
        // "+ Add an attack at this point": blank, or any ready-made pattern
        MenuItem blank = new MenuItem("Blank (set it up yourself)");
        blank.setOnAction(e -> addPointAttack(null));
        addPointAttackMenu.getItems().add(blank);
        for (String[] preset : Extermination.QUICK_PATTERNS) {
            MenuItem item = new MenuItem(preset[0]);
            item.setOnAction(e -> addPointAttack(preset[1]));
            addPointAttackMenu.getItems().add(item);
        }
        // Quick waves: one menu item per ready-made group in the mechanics
        for (int i = 0; i < Extermination.QUICK_WAVES.length; i++) {
            int index = i;
            MenuItem item = new MenuItem(Extermination.QUICK_WAVES[i][0]);
            item.setOnAction(e -> onAddQuickWave(index));
            quickWaveMenu.getItems().add(item);
        }
        setUpNumberBox(directionSpinner, 0, 359, 5, 0,
                v -> { if (phase() != null) phase().setDirection(v); });
        setUpNumberBox(sweepSpinner, -Extermination.MAX_SWEEP, Extermination.MAX_SWEEP, 5, 0,
                v -> { if (phase() != null) phase().setSweep(v); });
        setUpNumberBox(swingSpinner, 0, 360, 5, 0,
                v -> { if (phase() != null) phase().setSwing(v); });
        setUpNumberBox(beamSpinner, 0.1, Extermination.MAX_BEAM_TIME, 0.1, 1,
                v -> { if (phase() != null) phase().setBeamTime(v); });
        setUpNumberBox(wallWidthSpinner, 0, Extermination.MAX_WALL_WIDTH, 10, 0,
                v -> { if (phase() != null) phase().setWallWidth(v); });
        // The time, and a boss's attack settings, belong to the whole attack: they're kept on its first pattern
        setUpNumberBox(lastsSpinner, 0, Extermination.MAX_PHASE_TIME, 0.5, 1,
                v -> { if (lead() != null) lead().setDuration(v); });
        setUpNumberBox(cardHealthSpinner, 1, Extermination.MAX_CARD_HEALTH, 10, 0,
                v -> { if (lead() != null) lead().setCardHealth((int) Math.round(v)); });
        setUpNumberBox(hopSpinner, Extermination.MIN_HOP, Extermination.MAX_HOP, 0.5, 1,
                v -> { if (lead() != null) lead().setHop(v); });
        setUpNumberBox(routeStopSpinner, 0, Extermination.MAX_STOP_TIME, 0.5, 1,
                v -> { if (lead() != null) lead().setMoveStop(routePoint, v); });
        setUpNumberBox(routeSpeedSpinner, Extermination.MIN_ENEMY_SPEED, Extermination.MAX_ENEMY_SPEED, 10, 0,
                v -> { if (lead() != null) lead().setMoveSpeed(v); });
        setUpNumberBox(pointShotsSpinner, 0, Extermination.MAX_VOLLEYS, 1, 0,
                v -> { if (phase() != null) phase().setVolleys((int) Math.round(v)); });
        setUpNumberBox(delaySpinner, 0, Extermination.MAX_PHASE_TIME, 0.5, 1,
                v -> { if (phase() != null) phase().setDelay(v); });
        // The spell card's name is kept as it's typed
        cardNameField.textProperty().addListener((obs, old, text) -> {
            if (!refreshingPanel && lead() != null) lead().setCardName(text);
        });
        setUpNumberBox(accelSpinner, -Extermination.MAX_BULLET_ACCEL, Extermination.MAX_BULLET_ACCEL, 10, 0,
                v -> { if (phase() != null) phase().setAccel(v); });
        setUpNumberBox(curveSpinner, -Extermination.MAX_BULLET_CURVE, Extermination.MAX_BULLET_CURVE, 5, 0,
                v -> { if (phase() != null) phase().setCurve(v); });
        setUpNumberBox(waveSpinner, 0, Extermination.MAX_WAVE_SIZE, 2, 0,
                v -> { if (phase() != null) phase().setWave(v); });
        setUpNumberBox(waveRateSpinner, 0.1, Extermination.MAX_WAVE_RATE, 0.1, 1,
                v -> { if (phase() != null) phase().setWaveRate(v); });
        setUpNumberBox(stepTimeSpinner, 0, Extermination.MAX_STEP_TIME, 0.1, 2,
                v -> { if (step() != null) step().setTime(v); });
        setUpNumberBox(stepSpeedSpinner, 0, Extermination.MAX_BULLET_SPEED, 10, 0,
                v -> { if (step() != null) step().setSpeed(v); });
        setUpNumberBox(stepAccelSpinner, -Extermination.MAX_BULLET_ACCEL, Extermination.MAX_BULLET_ACCEL, 10, 0,
                v -> { if (step() != null) step().setAccel(v); });
        setUpNumberBox(stepCurveSpinner, -Extermination.MAX_BULLET_CURVE, Extermination.MAX_BULLET_CURVE, 5, 0,
                v -> { if (step() != null) step().setCurve(v); });
        setUpNumberBox(stepTurnSpinner, -180, 180, 5, 0,
                v -> { if (step() != null) step().setTurn(v); });
        setUpNumberBox(stopSpinner, 0, Extermination.MAX_STOP_TIME, 0.5, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setStopTime(selectedPoint, v); });

        showMenu();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // Filters (not handlers) so the Main Menu button can't swallow the arrow keys.
        // Movement needs to know which keys are held down, so both presses and releases are tracked.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (screen == Screen.SANDBOX && scene.getFocusOwner() == cardNameField) {
                // Typing a spell card name: the keys go to the box. Enter or Esc gives the keyboard back to the game.
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                    root.requestFocus();
                    e.consume();
                }
                return;
            }
            Spinner<Double> box = screen == Screen.SANDBOX ? numberBoxBeingTyped(scene) : null;
            if (box != null) {
                // Typing in a number box: number keys go to the box, not the ship. Enter keeps the value,
                // Esc puts back the old one; either way the keyboard goes back to the game.
                if (e.getCode() == KeyCode.ESCAPE) {
                    revertNumberBox(box);
                } else if (!isTypingKey(e)) {
                    warnIfNotKept(box);
                }
                if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE) {
                    root.requestFocus();
                    e.consume();
                    return;
                }
                if (isTypingKey(e)) {
                    return;
                }
                // Any other key (R, P, Space...) keeps the typed value first, then does its usual job,
                // so typing a number and pressing R restarts with the new number.
                root.requestFocus();
            }
            heldKeys.add(e.getCode());
            if (screen == Screen.MENU) {
                if (!(e.getCode() == KeyCode.SPACE && waitForSpaceRelease)) {
                    onMenuKey(e.getCode());
                }
                e.consume();
                return;
            }
            if (screen == Screen.MODE_SELECT) {
                if (!(e.getCode() == KeyCode.SPACE && waitForSpaceRelease)) {
                    onModeSelectKey(e.getCode());
                }
                e.consume();
                return;
            }
            if (screen == Screen.SANDBOX) {
                onSandboxKey(e.getCode());
                e.consume();
                return;
            }
            if (screen == Screen.RUN) {
                onRunKey(e.getCode());
                e.consume();
                return;
            }
            if (screen == Screen.SCORES) {
                if (!(e.getCode() == KeyCode.SPACE && waitForSpaceRelease)
                        && (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.ESCAPE)) {
                    waitForSpaceRelease = e.getCode() == KeyCode.SPACE;
                    showMenu();
                }
                e.consume();
                return;
            }
            if (screen == Screen.INTRO) {
                if (e.getCode() == KeyCode.SPACE && !waitForSpaceRelease) {
                    waitForSpaceRelease = true;
                    startGame();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    showMenu();
                }
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ENTER && game.isGameOver()) {
                game = new SpaceInvadersMechanics();
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                showMenu();
            }
            if (horizontalInput() != 0 || e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER
                    || e.getCode() == KeyCode.ESCAPE) e.consume();
        });
        // The panel buttons don't take the focus, so a click on them would leave a typed number unsaved.
        // Any click outside the box being typed in keeps the typed value before the click does its job.
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (scene.getFocusOwner() == cardNameField && !isInside(e.getTarget(), cardNameField)) {
                root.requestFocus(); // done typing the name
            }
            Spinner<Double> box = screen == Screen.SANDBOX ? numberBoxBeingTyped(scene) : null;
            if (box != null && !isInside(e.getTarget(), box)) {
                warnIfNotKept(box);
                root.requestFocus();
                if (e.getTarget() == canvas) {
                    // This click only finishes the typing. Without this it would also place a new enemy
                    // (or pick another one), and the panel would jump to that enemy's values.
                    e.consume();
                }
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            heldKeys.remove(e.getCode());
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.Z) {
                waitForSpaceRelease = false;
            }
        });
        // Stop the loop when the player goes back to the menu.
        scene.windowProperty().addListener((obs, oldWin, win) -> {
            if (win == null) loop.stop();
        });
    }

    /** -1 for left, 1 for right, 0 when both or neither are held. */
    private int horizontalInput() {
        int dx = 0;
        if (heldKeys.contains(KeyCode.LEFT) || heldKeys.contains(KeyCode.A)) dx -= 1;
        if (heldKeys.contains(KeyCode.RIGHT) || heldKeys.contains(KeyCode.D)) dx += 1;
        return dx;
    }

    /**
     * Sandbox keys: 1/2/3 pick the alien type to place, P switches between place and path mode, and for
     * the selected enemy L toggles the loop, -/+ change speed, [/] change how many fly the path and ,/.
     * change when they spawn. R starts the level over, Esc leaves.
     */
    private void onSandboxKey(KeyCode code) {
        switch (code) {
            case DIGIT1, NUMPAD1 -> placingType = Alien.Type.SQUID;
            case DIGIT2, NUMPAD2 -> placingType = Alien.Type.CRAB;
            case DIGIT3, NUMPAD3 -> placingType = Alien.Type.OCTOPUS;
            case P -> pathMode = !pathMode;
            case ENTER -> paused = !paused;
            case T -> {
                if (phase() != null) phase().nextPattern();
            }
            case L -> {
                if (selectedEnemy != null) selectedEnemy.toggleLoop();
            }
            case MINUS, SUBTRACT -> {
                if (selectedEnemy != null) selectedEnemy.changeSpeed(-SPEED_STEP);
            }
            case EQUALS, PLUS, ADD -> {
                if (selectedEnemy != null) selectedEnemy.changeSpeed(SPEED_STEP);
            }
            case OPEN_BRACKET -> {
                if (selectedEnemy != null) selectedEnemy.changeCount(-1);
            }
            case CLOSE_BRACKET -> {
                if (selectedEnemy != null) selectedEnemy.changeCount(1);
            }
            case COMMA -> {
                if (selectedEnemy != null) selectedEnemy.changeSpawnTime(-SPAWN_TIME_STEP);
            }
            case PERIOD -> {
                if (selectedEnemy != null) selectedEnemy.changeSpawnTime(SPAWN_TIME_STEP);
            }
            case R -> sandbox.resetEnemies();
            case X -> bombQueued = !paused;
            // Testing: M steps to the next power stage (each row of POWER_LEVELS), then back to 0 after the last
            case M -> {
                double next = 0;
                for (double[] level : Extermination.POWER_LEVELS) {
                    if (level[0] > sandbox.getPower()) {
                        next = level[0];
                        break;
                    }
                }
                sandbox.setPower(next);
            }
            case ESCAPE -> {
                if (pickingFollow || pickingUntil || pickingFromSpot) {
                    pickingFollow = false; // Esc only cancels picking a group or spot
                    pickingUntil = false;
                    pickingFromSpot = false;
                } else {
                    showMenu();
                }
            }
            default -> { }
        }
    }

    /**
     * Place mode: a left click places an enemy (or selects one that's already there), a right click
     * removes the nearest one.
     * Path mode: a left click on an enemy selects it, a left click anywhere else adds a waypoint to the
     * selected enemy's path (holding the button and dragging keeps adding them, see onSandboxDrag), and
     * a right click takes back the last click or drag.
     */
    private void onSandboxMouse(MouseEvent e) {
        if (screen != Screen.SANDBOX) {
            return;
        }
        root.requestFocus();
        double x = e.getX();
        double y = e.getY();
        if (drawingRoute && lead() != null && selectedEnemy.isBoss() && lead().getMove() == Extermination.BossMove.PATH) {
            onRouteClick(e);
            return;
        }
        // A gold point of the boss attack's path: pick it, to see and add its attacks
        if (!pathMode && e.getButton() == MouseButton.PRIMARY && selectedEnemy != null && selectedEnemy.isBoss()
                && lead() != null && lead().getMove() == Extermination.BossMove.PATH) {
            int near = lead().findMovePointNear(x, y, POINT_PICK_RADIUS + 2);
            if (near >= 0) {
                routePoint = near;
                return;
            }
        }
        if (pickingFromSpot) {
            // This click picks the spot the selected pattern fires from
            pickingFromSpot = false;
            if (e.getButton() == MouseButton.PRIMARY && phase() != null) {
                addSpot(x, y);
            }
            return;
        }
        // A cyan cross (a pattern's fire spot): click to edit that pattern and drag to move it; right-click removes it
        int spot = !pathMode && selectedEnemy != null ? fireSpotNear(x, y) : -1;
        if (spot >= 0) {
            Extermination.ShotPhase owner = selectedEnemy.getPhases().get(spot);
            if (e.getButton() == MouseButton.SECONDARY) {
                removeSpot(spot);
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                selectedPhase = spot;
                selectedStep = 0;
                draggingSpot = owner;
                for (javafx.scene.control.Tab tab : selectedTabs.getTabs()) {
                    if (tab.getText().equals("Bullets")) {
                        selectedTabs.getSelectionModel().select(tab);
                    }
                }
                return;
            }
        }
        if (pickingUntil) {
            // This click picks the group the selected one keeps coming until
            pickingUntil = false;
            Extermination.EnemyDesign other = null;
            for (Extermination.EnemyDesign near : sandbox.findEnemiesNear(x, y, editWave)) {
                if (near != selectedEnemy) {
                    other = near;
                    break;
                }
            }
            if (e.getButton() == MouseButton.PRIMARY && other != null && selectedEnemy != null) {
                if (selectedEnemy.canKeepComingUntil(other)) {
                    selectedEnemy.setUntil(other);
                    followProblem = "";
                } else {
                    followProblem = "That group waits for this one, so it can't be the one to keep coming until.";
                }
            }
            return;
        }
        if (pickingFollow) {
            // This click picks the group the selected one waits for
            pickingFollow = false;
            // Groups stacked on the same spot (like a duplicate) can't wait for themselves, so skip the selected one
            Extermination.EnemyDesign leader = null;
            for (Extermination.EnemyDesign near : sandbox.findEnemiesNear(x, y, editWave)) {
                if (near != selectedEnemy) {
                    leader = near;
                    break;
                }
            }
            if (e.getButton() == MouseButton.PRIMARY && leader != null && selectedEnemy != null) {
                if (selectedEnemy.canFollow(leader)) {
                    selectedEnemy.setFollows(leader);
                    followProblem = "";
                } else {
                    followProblem = leader == selectedEnemy ? "A group can't wait for itself."
                            : "That group already waits for this one.";
                }
            }
            return;
        }
        if (!pathMode) {
            if (e.getButton() == MouseButton.PRIMARY) {
                // A dot on the selected enemy's path: select that point, to give it a stop
                int point = selectedEnemy == null ? -1 : selectedEnemy.findWaypointNear(x, y, POINT_PICK_RADIUS);
                if (point > 0) {
                    selectedPoint = point;
                    return;
                }
                // Pressing on the selected group keeps it, so it can be dragged; a click without a drag
                // then moves on to the next group stacked there (see the mouse release)
                boolean onSelected = sandbox.findEnemiesNear(x, y, editWave).contains(selectedEnemy);
                Extermination.EnemyDesign clicked = onSelected ? selectedEnemy : groupAt(x, y);
                pressedOnSelected = onSelected;
                // Holding the mouse on a group and dragging moves it, path and all
                draggingGroup = clicked != null;
                groupMoved = false;
                dragFromX = x;
                dragFromY = y;
                selectedEnemy = clicked != null ? clicked : sandbox.placeEnemy(placingType, x, y, editWave);
                pointOwner = selectedEnemy;
                selectedPoint = 0;
            } else if (e.getButton() == MouseButton.SECONDARY) {
                // Of groups stacked here, the selected one goes first
                List<Extermination.EnemyDesign> near = sandbox.findEnemiesNear(x, y, editWave);
                if (!near.isEmpty()) {
                    Extermination.EnemyDesign removed = near.contains(selectedEnemy) ? selectedEnemy : near.get(0);
                    sandbox.removeEnemy(removed);
                    if (removed == selectedEnemy) {
                        selectedEnemy = null;
                    }
                }
            }
            return;
        }
        if (e.getButton() == MouseButton.PRIMARY) {
            Extermination.EnemyDesign clicked = groupAt(x, y);
            if (clicked != null) {
                selectedEnemy = clicked;
            } else if (selectedEnemy != null) {
                selectedEnemy.startStroke();
                selectedEnemy.addWaypoint(x, y);
                drawingPath = true;
            }
        } else if (e.getButton() == MouseButton.SECONDARY && selectedEnemy != null) {
            selectedEnemy.removeLastStroke();
        }
    }

    /**
     * The group a click at (x, y) selects, or null if there's none there. When several groups sit on
     * the same spot, each click picks the next one, so all of them can be reached.
     */
    private Extermination.EnemyDesign groupAt(double x, double y) {
        List<Extermination.EnemyDesign> near = sandbox.findEnemiesNear(x, y, editWave);
        if (near.isEmpty()) {
            return null;
        }
        int current = near.indexOf(selectedEnemy);
        return near.get((current + 1) % near.size()); // not selected yet (-1) gives the first
    }

    // ------------------------------------------------------------------ sandbox editor panel

    @FXML
    private void onPlayPause() {
        paused = !paused;
    }

    @FXML
    private void onPrevWave() {
        showWave(editWave - 1);
    }

    @FXML
    private void onNextWave() {
        showWave(editWave + 1);
    }

    @FXML
    private void onNewWave() {
        showWave(sandbox.addWave());
    }

    /** Switches to editing another wave (if it exists), clearing the selection. */
    private void showWave(int wave) {
        if (wave >= 0 && wave < sandbox.getWaves().size()) {
            editWave = wave;
            selectedEnemy = null;
        }
    }

    @FXML
    private void onAfterCleared() {
        sandbox.getWaves().get(editWave).setAfterCleared(true);
    }

    @FXML
    private void onAfterStarted() {
        sandbox.getWaves().get(editWave).setAfterCleared(false);
    }

    /** Clearing this wave drops a bomb from the top of the screen, or not. */
    @FXML
    private void onWaveBombToggled() {
        sandbox.getWaves().get(editWave).setDropsBomb(waveBombCheck.isSelected());
    }

    /** Starts the run from the wave being edited and plays it. */
    @FXML
    private void onPlayFromWave() {
        sandbox.playFromWave(editWave);
        paused = false;
    }

    @FXML
    private void onPlaceTool() {
        pathMode = false;
    }

    @FXML
    private void onPathTool() {
        pathMode = true;
    }

    @FXML
    private void onPickSquid() {
        placingType = Alien.Type.SQUID;
    }

    @FXML
    private void onPickCrab() {
        placingType = Alien.Type.CRAB;
    }

    @FXML
    private void onPickOctopus() {
        placingType = Alien.Type.OCTOPUS;
    }

    /** A pattern button: its userData in the fxml names the pattern. */
    @FXML
    private void onPickPattern(ActionEvent e) {
        if (selectedEnemy != null) {
            String name = (String) ((ToggleButton) e.getSource()).getUserData();
            phase().setPattern(Extermination.Pattern.valueOf(name));
        }
    }

    /** An aim button: its userData in the fxml names the aim. */
    @FXML
    private void onPickAim(ActionEvent e) {
        if (selectedEnemy != null) {
            String name = (String) ((ToggleButton) e.getSource()).getUserData();
            phase().setAim(Extermination.Aim.valueOf(name));
        }
    }

    /** The shot phase being edited, or null when no enemy is selected. */
    private Extermination.ShotPhase phase() {
        if (selectedEnemy == null) {
            return null;
        }
        List<Extermination.ShotPhase> phases = selectedEnemy.getPhases();
        selectedPhase = Math.max(0, Math.min(selectedPhase, phases.size() - 1));
        return phases.get(selectedPhase);
    }

    /** The bullet step being edited, or null when the phase has none. */
    private Extermination.BulletStep step() {
        Extermination.ShotPhase phase = phase();
        if (phase == null || phase.getSteps().isEmpty()) {
            return null;
        }
        selectedStep = Math.max(0, Math.min(selectedStep, phase.getSteps().size() - 1));
        return phase.getSteps().get(selectedStep);
    }

    @FXML
    private void onPickFollow() {
        if (selectedEnemy != null) {
            pickingFollow = true;
            followProblem = "";
        }
    }

    /**
     * The index of the selected enemy's pattern whose fire spot is near (x, y), or -1. The pattern
     * being edited wins when spots overlap.
     */
    private int fireSpotNear(double x, double y) {
        List<Extermination.ShotPhase> phases = selectedEnemy.getPhases();
        int best = -1;
        double bestDistance = POINT_PICK_RADIUS + 4;
        for (int i = 0; i < phases.size(); i++) {
            Extermination.ShotPhase p = phases.get(i);
            if (p.hasFromSpot()) {
                double d = Math.hypot(p.getFromX() - x, p.getFromY() - y);
                if (d < bestDistance || (d <= POINT_PICK_RADIUS + 4 && i == selectedPhase)) {
                    best = i;
                    bestDistance = i == selectedPhase ? -1 : d;
                }
            }
        }
        return best;
    }

    /** Removes the spot being edited, with its bullets (see {@link #removeSpot}). */
    @FXML
    private void onFromEnemy() {
        pickingFromSpot = false;
        if (phase() != null && phase().hasFromSpot()) {
            removeSpot(selectedPhase);
        }
    }

    /**
     * The next click on the map adds a spot there: a new pattern of this attack that fires its own
     * bullets from that spot, leaving the enemy's own patterns as they are. Pressing it again cancels.
     */
    @FXML
    private void onFromSpot() {
        if (phase() != null) {
            pickingFromSpot = !pickingFromSpot;
            pickingFollow = false;
            pickingUntil = false;
        }
    }

    /** Adds a spot at (x, y) to the attack being edited, as a new pattern after its others, and picks it. */
    private void addSpot(double x, double y) {
        if (selectedEnemy == null || selectedEnemy.getPhases().size() >= Extermination.MAX_PHASES) {
            showAttackMessage("This enemy can't have more patterns.", true);
            return;
        }
        int last = selectedEnemy.attackEnd(selectedEnemy.attackStart(selectedPhase)) - 1;
        Extermination.ShotPhase spot = selectedEnemy.addLayer(last);
        spot.setPattern(Extermination.Pattern.RING);
        spot.setFromSpot(x, y);
        selectedPhase = last + 1;
        selectedStep = 0;
    }

    /**
     * Removes a spot: its pattern goes with it, unless it's the attack's only pattern, which then fires
     * from the enemy again.
     */
    private void removeSpot(int index) {
        Extermination.ShotPhase owner = selectedEnemy.getPhases().get(index);
        int start = selectedEnemy.attackStart(index);
        if (selectedEnemy.attackEnd(start) - start > 1) {
            selectedEnemy.removePhase(owner);
            selectedPhase = Math.max(start, Math.min(index, selectedEnemy.attackEnd(start) - 1));
            showAttackMessage("Removed that spot and its bullets.", false);
        } else {
            owner.clearFromSpot();
            selectedPhase = index;
            showAttackMessage("Removed the spot; this pattern fires from the enemy again.", false);
        }
        selectedStep = 0;
    }

    @FXML
    private void onPickUntil() {
        if (selectedEnemy != null) {
            pickingUntil = true;
            pickingFollow = false;
            followProblem = "";
        }
    }

    @FXML
    private void onClearUntil() {
        if (selectedEnemy != null) {
            selectedEnemy.setUntil(null);
        }
        pickingUntil = false;
    }

    @FXML
    private void onRepeatOnTimer() {
        if (selectedEnemy != null) selectedEnemy.setRepeatAfterGone(false);
    }

    @FXML
    private void onRepeatAfterGone() {
        if (selectedEnemy != null) selectedEnemy.setRepeatAfterGone(true);
    }

    @FXML
    private void onFollowCleared() {
        if (selectedEnemy != null) selectedEnemy.setFollowsSpawn(false);
    }

    @FXML
    private void onFollowSpawned() {
        if (selectedEnemy != null) selectedEnemy.setFollowsSpawn(true);
    }

    @FXML
    private void onClearFollow() {
        if (selectedEnemy != null) {
            selectedEnemy.setFollows(null);
        }
        pickingFollow = false;
        followProblem = "";
    }

    /** The previous pattern of the same attack (the patterns of one attack all fire together). */
    @FXML
    private void onPrevPhase() {
        if (phase() != null && selectedPhase > selectedEnemy.attackStart(selectedPhase)) {
            selectedPhase--;
            selectedStep = 0;
        }
    }

    /** The next pattern of the same attack. */
    @FXML
    private void onNextPhase() {
        if (phase() != null && selectedPhase + 1 < selectedEnemy.attackEnd(selectedEnemy.attackStart(selectedPhase))) {
            selectedPhase++;
            selectedStep = 0;
        }
    }

    /** The first pattern of the attack before this one. */
    @FXML
    private void onPrevAttack() {
        if (phase() != null) {
            int start = selectedEnemy.attackStart(selectedPhase);
            if (start > 0) {
                selectedPhase = selectedEnemy.attackStart(start - 1);
                selectedStep = 0;
            }
        }
    }

    /** The first pattern of the attack after this one. */
    @FXML
    private void onNextAttack() {
        if (phase() != null) {
            int end = selectedEnemy.attackEnd(selectedEnemy.attackStart(selectedPhase));
            if (end < selectedEnemy.getPhases().size()) {
                selectedPhase = end;
                selectedStep = 0;
            }
        }
    }

    /** Deletes the whole attack being edited, all its patterns (the last attack left can't go). */
    @FXML
    private void onRemoveAttack() {
        if (phase() == null) {
            return;
        }
        List<Extermination.ShotPhase> phases = selectedEnemy.getPhases();
        int start = selectedEnemy.attackStart(selectedPhase);
        int end = selectedEnemy.attackEnd(start);
        if (end - start >= phases.size()) {
            showAttackMessage("This is the only one, so it can't be deleted. Change it instead.", true);
            return;
        }
        for (int i = end - 1; i > start; i--) {
            selectedEnemy.removePhase(phases.get(i));
        }
        selectedEnemy.removePhase(phases.get(start));
        selectedPhase = Math.max(0, Math.min(start, phases.size() - 1));
        selectedPhase = selectedEnemy.attackStart(selectedPhase);
        selectedStep = 0;
        showAttackMessage("", false);
    }

    /** Adds a phase after the last one and switches to it. The phase before it gets 3 s if it was "for good". */
    @FXML
    private void onAddPhase() {
        if (selectedEnemy == null || selectedEnemy.getPhases().size() >= Extermination.MAX_PHASES) {
            return;
        }
        List<Extermination.ShotPhase> phases = selectedEnemy.getPhases();
        Extermination.ShotPhase last = phases.get(phases.size() - 1);
        if (last.getDuration() == 0) {
            last.setDuration(selectedEnemy.isBoss() ? Extermination.CARD_TIME : 3);
        }
        Extermination.ShotPhase added = selectedEnemy.addPhase();
        if (selectedEnemy.isBoss()) {
            added.setDuration(Extermination.CARD_TIME);
        }
        selectedPhase = phases.size() - 1;
        selectedStep = 0;
    }

    @FXML
    private void onRemovePhase() {
        if (phase() != null) {
            selectedEnemy.removePhase(phase());
            selectedStep = 0;
        }
    }

    @FXML
    private void onPrevStep() {
        selectedStep--;
    }

    @FXML
    private void onNextStep() {
        selectedStep++;
    }

    @FXML
    private void onAddStep() {
        if (phase() != null) {
            phase().addStep();
            selectedStep = phase().getSteps().size() - 1;
        }
    }

    @FXML
    private void onRemoveStep() {
        if (step() != null) {
            phase().removeStep(step());
        }
    }

    @FXML
    private void onNextShape() {
        if (phase() != null) phase().nextShape();
    }

    @FXML
    private void onNextColor() {
        if (phase() != null) phase().nextColor();
    }

    @FXML
    private void onStepAimToggled() {
        if (step() != null) step().setAim(stepAimCheck.isSelected());
    }

    @FXML
    private void onLoopToggled() {
        if (selectedEnemy != null) selectedEnemy.setLoop(loopCheck.isSelected());
    }

    @FXML
    private void onFireStoppedToggled() {
        if (selectedEnemy != null) selectedEnemy.setFireOnlyWhenStopped(fireStoppedCheck.isSelected());
    }

    /** Makes the selected enemy a boss or a normal enemy. A new boss's attacks get a time limit if they had none. */
    @FXML
    private void onBossToggled() {
        if (selectedEnemy == null) {
            return;
        }
        selectedEnemy.setBoss(bossCheck.isSelected());
        showBossTab(selectedEnemy.isBoss());
        if (selectedEnemy.isBoss()) {
            selectedTabs.getSelectionModel().select(bossTab);
            for (Extermination.ShotPhase attack : selectedEnemy.getPhases()) {
                if (attack.getDuration() == 0) {
                    attack.setDuration(Extermination.CARD_TIME);
                }
            }
        }
    }

    @FXML
    private void onMoveStay() {
        if (lead() != null) lead().setMove(Extermination.BossMove.STAY);
    }

    @FXML
    private void onMoveWander() {
        if (lead() != null) lead().setMove(Extermination.BossMove.WANDER);
    }

    @FXML
    private void onMovePath() {
        if (lead() != null) {
            lead().setMove(Extermination.BossMove.PATH);
            drawingRoute = lead().getMovePath().isEmpty(); // nothing drawn yet: start drawing straight away
        }
    }

    /** Turns drawing the attack's movement path on the map on or off. */
    @FXML
    private void onDrawRoute() {
        drawingRoute = !drawingRoute;
    }

    /** Flies the attack's path once and stays at the end. */
    @FXML
    private void onRouteOnce() {
        if (lead() != null) {
            lead().setMoveLoop(false);
            lead().setMoveBounce(false);
        }
    }

    /** Goes back to point 1 after the last point, round and round. */
    @FXML
    private void onRouteLoop() {
        if (lead() != null) {
            lead().setMoveLoop(true);
            lead().setMoveBounce(false);
        }
    }

    /** Flies the path back and forth: 1, 2, 3, 2, 1, 2... */
    @FXML
    private void onRouteBounce() {
        if (lead() != null) {
            lead().setMoveLoop(true);
            lead().setMoveBounce(true);
        }
    }

    /** Takes back the last point of the attack's movement path. */
    @FXML
    private void onUndoRoutePoint() {
        if (lead() != null && !lead().getMovePath().isEmpty()) {
            lead().getMovePath().remove(lead().getMovePath().size() - 1);
        }
    }

    @FXML
    private void onClearRoute() {
        if (lead() != null) {
            lead().getMovePath().clear();
            routePoint = 0;
        }
    }

    /**
     * A click while drawing a boss attack's path: on one of its points it picks that point (to give it
     * a stop), anywhere else it adds a point; a right click takes back the last point.
     */
    private void onRouteClick(MouseEvent e) {
        Extermination.ShotPhase attack = lead();
        if (e.getButton() == MouseButton.SECONDARY) {
            onUndoRoutePoint();
            return;
        }
        // Shift-click always adds a point, even on top of another (to come back to the same spot)
        int near = e.isShiftDown() ? -1 : attack.findMovePointNear(e.getX(), e.getY(), POINT_PICK_RADIUS);
        double x = e.getX();
        double y = e.getY();
        List<double[]> points = attack.getMovePath();
        if (near < 0 && !points.isEmpty()) {
            // Close to level (or straight above/below) with the last point: line up with it exactly
            double[] last = points.get(points.size() - 1);
            if (Math.abs(y - last[1]) <= ROUTE_SNAP) y = last[1];
            if (Math.abs(x - last[0]) <= ROUTE_SNAP) x = last[0];
        }
        int added = near >= 0 ? near : attack.addMovePoint(x, y);
        if (added >= 0) {
            routePoint = added;
        }
    }

    /** Adds a pattern right after this one that fires at the same time, and switches to it. */
    @FXML
    private void onAddLayer() {
        if (selectedEnemy == null || selectedEnemy.getPhases().size() >= Extermination.MAX_PHASES) {
            return;
        }
        selectedEnemy.addLayer(selectedPhase);
        selectedPhase++;
        selectedStep = 0;
    }

    /**
     * Adds a new pattern to the attack that fires only at the picked path point (twice each stop), set
     * up from a ready-made pattern (or left as it is when null), and switches to it. A point with no
     * stop gets one, since it only fires while the boss holds still there.
     */
    private void addPointAttack(String preset) {
        Extermination.ShotPhase attack = lead();
        if (attack == null || attack.getMovePath().isEmpty()
                || selectedEnemy.getPhases().size() >= Extermination.MAX_PHASES) {
            return;
        }
        int point = Math.min(routePoint, attack.getMovePath().size() - 1);
        int start = selectedEnemy.attackStart(selectedPhase);
        int last = selectedEnemy.attackEnd(start) - 1;
        // The first attack given to a point: the boss's own pattern (which fires everywhere) is switched
        // off, since with point attacks it's almost never wanted. It can be switched back on.
        boolean firstPointAttack = true;
        for (int i = start; i <= last; i++) {
            if (!selectedEnemy.getPhases().get(i).getAtPoints().isEmpty()) {
                firstPointAttack = false;
            }
        }
        String note = "";
        if (firstPointAttack && attack.getAtPoints().isEmpty() && attack.getPattern() != Extermination.Pattern.NONE) {
            attack.setPattern(Extermination.Pattern.NONE);
            note = " Pattern 1 (the boss's own bullets that fire everywhere) is switched off now.";
        }
        Extermination.ShotPhase added = selectedEnemy.addLayer(last);
        if (preset != null) {
            added.applyPattern(preset);
        }
        added.toggleAtPoint(point + 1);
        // Quick-firing patterns (streams, rain) fire the whole stay; slower ones fire twice
        added.setVolleys(added.getFireInterval() < 0.3 ? 0 : 2);
        if (attack.getMovePath().get(point)[2] == 0) {
            attack.setMoveStop(point, 2);
        }
        selectedPhase = last + 1;
        selectedStep = 0;
        showAttackMessage("Added pattern " + (selectedPhase - selectedEnemy.attackStart(selectedPhase) + 1) + " at point " + (point + 1) + "." + note, false);
    }

    /** What the point's attack list was last built from, so it's only rebuilt when something changes. */
    private String pointListKey = "";

    /** The picked path point's attacks: one button per pattern that fires there, to jump to it. */
    private void refreshPointAttacks(Extermination.EnemyDesign e, Extermination.ShotPhase attack) {
        List<double[]> points = attack.getMovePath();
        if (points.isEmpty()) {
            pointAttacksTitle.setText("Draw the path first, then click a point to give it attacks.");
            addPointAttackMenu.setDisable(true);
            pointAttackList.getChildren().clear();
            pointListKey = "";
            return;
        }
        int point = Math.min(routePoint, points.size() - 1) + 1;
        addPointAttackMenu.setDisable(false);
        addPointAttackMenu.setText("+ Add an attack at point " + point);
        int start = e.attackStart(selectedPhase);
        StringBuilder key = new StringBuilder(point + ":" + selectedPhase);
        List<Integer> here = new java.util.ArrayList<>();
        for (int i = start; i < e.attackEnd(start); i++) {
            Extermination.ShotPhase p = e.getPhases().get(i);
            if (p.getAtPoints().contains(point)) {
                here.add(i);
                key.append(",").append(i).append(p.getPattern()).append(p.getColor()).append(p.getVolleys());
            }
        }
        pointAttacksTitle.setText("POINT " + point + (here.isEmpty() ? ": no attacks yet" : " fires:"));
        // The picked attack's shot count, when it's one of this point's
        boolean pickedHere = here.contains(selectedPhase);
        pointShotsRow.setVisible(pickedHere);
        pointShotsRow.setManaged(pickedHere);
        if (pickedHere) {
            pointShotsLabel.setText("Pattern " + (selectedPhase - start + 1) + " shots each stay (0 = keeps firing)");
            showNumber(pointShotsSpinner, e.getPhases().get(selectedPhase).getVolleys());
        }
        if (key.toString().equals(pointListKey)) {
            return;
        }
        pointListKey = key.toString();
        pointAttackList.getChildren().clear();
        for (int i : here) {
            Extermination.ShotPhase p = e.getPhases().get(i);
            String name = p.getPattern().name().charAt(0) + p.getPattern().name().substring(1).toLowerCase();
            String color = p.getColor().name().toLowerCase();
            Button go = new Button((i == selectedPhase ? "> " : "") + "Pattern " + (i - start + 1) + ": " + color + " " + name
                    + (p.getVolleys() > 0 ? ", " + p.getVolleys() + " shots" : ""));
            go.setFocusTraversable(false);
            go.setOnAction(ev -> {
                selectedPhase = i;
                selectedStep = 0;
            });
            showPicked(go, i == selectedPhase);
            pointAttackList.getChildren().add(go);
        }
    }

    /** How many path points the menu was last built for, so it's only rebuilt when that changes. */
    private int atPointMenuSize = -1;

    /**
     * The "Fires" menu: anywhere, or any mix of while moving and stopped at the attack's path points
     * (each one ticks on and off, so the left and right points can share a pattern).
     */
    private void refreshAtPointMenu(Extermination.ShotPhase p, Extermination.EnemyDesign e) {
        Extermination.ShotPhase attack = e.getPhases().get(e.attackStart(selectedPhase));
        boolean onPath = attack.getMove() == Extermination.BossMove.PATH;
        int points = onPath ? attack.getMovePath().size() : 0;
        if (points != atPointMenuSize) {
            atPointMenuSize = points;
            atPointButton.getItems().clear();
            MenuItem anywhere = new MenuItem("Fires anywhere");
            anywhere.setOnAction(ev -> { if (phase() != null) phase().clearAtPoints(); });
            atPointButton.getItems().add(anywhere);
            if (onPath) {
                atPointButton.getItems().add(atPointItem("While moving", -1));
                for (int i = 1; i <= points; i++) {
                    atPointButton.getItems().add(atPointItem("Stopped at point " + i, i));
                }
            }
        }
        java.util.SortedSet<Integer> at = p.getAtPoints();
        for (MenuItem item : atPointButton.getItems()) {
            if (item instanceof javafx.scene.control.CheckMenuItem check) {
                check.setSelected(at.contains((Integer) check.getUserData()));
            }
        }
        if (!onPath || at.isEmpty()) {
            atPointButton.setText("Fires anywhere");
        } else {
            List<String> places = new java.util.ArrayList<>();
            for (int point : at) {
                places.add(point < 0 ? "moving" : "point " + point);
            }
            atPointButton.setText("Fires at: " + String.join(", ", places));
        }
    }

    private MenuItem atPointItem(String text, int point) {
        javafx.scene.control.CheckMenuItem item = new javafx.scene.control.CheckMenuItem(text);
        item.setUserData(point);
        item.setOnAction(ev -> { if (phase() != null) phase().toggleAtPoint(point); });
        return item;
    }

    /** The first pattern of the attack being edited, which holds the attack's time and boss settings. */
    private Extermination.ShotPhase lead() {
        if (phase() == null) {
            return null;
        }
        return selectedEnemy.getPhases().get(selectedEnemy.attackStart(selectedPhase));
    }

    /** Copies the attack being edited as a PHASE line, to paste into another enemy or keep in a text file. */
    @FXML
    private void onCopyAttack() {
        if (phase() == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(Extermination.attackText(phase()));
        Clipboard.getSystemClipboard().setContent(content);
        showAttackMessage("Copied this attack. Select any enemy or boss and press Paste attack to add it there.", false);
    }

    /** Adds the attack on the clipboard after the selected enemy's last one and switches to it. */
    @FXML
    private void onPasteAttack() {
        if (selectedEnemy == null) {
            return;
        }
        try {
            Extermination.pasteAttack(selectedEnemy, Clipboard.getSystemClipboard().getString());
            selectedPhase = selectedEnemy.getPhases().size() - 1;
            selectedStep = 0;
            showAttackMessage("Pasted as " + (selectedEnemy.isBoss() ? "attack " : "phase ") + selectedEnemy.attackNumber(selectedPhase) + ".", false);
        } catch (IllegalArgumentException ex) {
            showAttackMessage("Couldn't paste: " + ex.getMessage() + ".", true);
        }
    }

    private void showAttackMessage(String message, boolean error) {
        attackMessage.setText(message);
        attackMessage.setTextFill(error ? Color.SALMON : Color.LIGHTGREEN);
    }

    private static String pointingName(Extermination.Pointing pointing) {
        return switch (pointing) {
            case UPRIGHT -> "stays upright";
            case HEAD_FIRST -> "head first along its path";
            case FEET_FIRST -> "feet first along its path";
        };
    }

    @FXML
    private void onFaceMovementToggled() {
        if (selectedEnemy != null) selectedEnemy.setFacesMovement(faceMovementCheck.isSelected());
    }

    @FXML
    private void onUndoStroke() {
        if (selectedEnemy != null) selectedEnemy.removeLastStroke();
    }

    @FXML
    private void onClearPath() {
        if (selectedEnemy != null) selectedEnemy.clearPath();
    }

    @FXML
    private void onDeleteEnemy() {
        if (selectedEnemy != null) {
            sandbox.removeEnemy(selectedEnemy);
            selectedEnemy = null;
        }
    }

    /**
     * Makes the tab area as tall as the open tab's contents (plus the tab headers), so nothing is cut
     * off and the editor panel's scrollbar reaches all of it.
     */
    private void fitTabsToContent() {
        javafx.scene.control.Tab open = selectedTabs.getSelectionModel().getSelectedItem();
        if (open != null && open.getContent() instanceof javafx.scene.layout.Region content) {
            double width = selectedTabs.getWidth() > 0 ? selectedTabs.getWidth() : -1;
            double height = content.prefHeight(width) + 40;
            if (Math.abs(selectedTabs.getPrefHeight() - height) > 1) {
                selectedTabs.setPrefHeight(height);
            }
        }
    }

    /** The previous point of the selected enemy's path to give a stop (0 is its start). */
    @FXML
    private void onPrevStopPoint() {
        if (selectedEnemy != null) {
            selectedPoint = Math.max(0, selectedPoint - 1);
        }
    }

    /** The next point of the selected enemy's path to give a stop. */
    @FXML
    private void onNextStopPoint() {
        if (selectedEnemy != null) {
            selectedPoint = Math.min(selectedEnemy.getWaypoints().size(), selectedPoint + 1);
        }
    }

    /** Shows the Boss tab (second, after Enemy) only while a boss is selected. */
    private void showBossTab(boolean boss) {
        List<javafx.scene.control.Tab> tabs = selectedTabs.getTabs();
        if (boss && !tabs.contains(bossTab)) {
            tabs.add(1, bossTab);
        } else if (!boss && tabs.contains(bossTab)) {
            tabs.remove(bossTab);
        }
    }

    /** Shows or hides a setting, taking its space away too while it's hidden. */
    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** A formation's name as shown in the menu, like "S curve" for S_CURVE. */
    private static String formationName(Extermination.Formation shape) {
        String name = shape.name().replace('_', ' ');
        return name.length() <= 1 ? name : name.charAt(0) + name.substring(1).toLowerCase();
    }

    @FXML
    private void onDuplicateEnemy() {
        if (selectedEnemy != null) {
            selectedEnemy = sandbox.duplicateGroup(selectedEnemy);
        }
    }

    @FXML
    private void onMirrorEnemy() {
        if (selectedEnemy != null) {
            selectedEnemy = sandbox.mirrorGroup(selectedEnemy);
        }
    }

    /** Adds a ready-made group to the wave being edited and selects its first enemy. */
    private void onAddQuickWave(int index) {
        List<Extermination.EnemyDesign> added = sandbox.addQuickWave(index, editWave);
        if (!added.isEmpty()) {
            selectedEnemy = added.get(0);
        }
    }

    @FXML
    private void onRestartLevel() {
        sandbox.resetEnemies();
    }

    @FXML
    private void onClearLevel() {
        sandbox.clearLayout();
        selectedEnemy = null;
        editWave = 0;
    }

    /** Copies the level as text, ready to paste into STAGES in SpaceInvadersMechanics. */
    @FXML
    private void onCopyLevel() {
        ClipboardContent content = new ClipboardContent();
        content.putString(sandbox.saveLevel());
        Clipboard.getSystemClipboard().setContent(content);
        showSaveMessage("Copied " + sandbox.getWaves().size() + " waves. Paste them between a stage's quotes in"
                + " STAGES (SpaceInvadersMechanics) to keep the level.", false);
    }

    /** Loads level text from the clipboard, e.g. a level copied earlier. */
    @FXML
    private void onPasteLevel() {
        String text = Clipboard.getSystemClipboard().getString();
        loadLevel(text == null ? "" : text, "the clipboard");
    }

    /** Adds the level on the clipboard after this level's waves, so separately made waves can be joined into one level. */
    @FXML
    private void onAddLevelAtEnd() {
        String text = Clipboard.getSystemClipboard().getString();
        try {
            sandbox.addLevelAtEnd(text == null ? "" : text);
            selectedEnemy = null;
            editWave = 0;
            paused = true;
            showSaveMessage("Added the copied waves at the end. The level now has " + sandbox.getWaves().size() + " waves ("
                    + sandbox.getLayout().size() + " enemy groups). Press Copy level to keep the whole thing.", false);
        } catch (IllegalArgumentException ex) {
            showSaveMessage("Couldn't add the copied level. " + ex.getMessage(), true);
        }
    }

    /** A Load Stage button: its userData in the fxml is the stage's index in STAGES. */
    @FXML
    private void onLoadStage(ActionEvent e) {
        int stage = Integer.parseInt((String) ((Button) e.getSource()).getUserData());
        loadLevel(Extermination.STAGES[stage], "Stage " + (stage + 1));
    }

    private void loadLevel(String text, String from) {
        try {
            sandbox.loadLevel(text);
            selectedEnemy = null;
            editWave = 0;
            paused = true;
            if (sandbox.getLayout().isEmpty()) {
                showSaveMessage(from + " is empty. Design a level, press Copy level and paste it into"
                        + " STAGES (SpaceInvadersMechanics).", false);
            } else {
                showSaveMessage("Loaded " + sandbox.getWaves().size() + " waves (" + sandbox.getLayout().size()
                        + " enemy groups) from " + from + ". Press Play to try it.", false);
            }
        } catch (IllegalArgumentException ex) {
            showSaveMessage("Couldn't load " + from + ". " + ex.getMessage(), true);
        }
    }

    private void showSaveMessage(String message, boolean error) {
        saveMessage.setText(message);
        saveMessage.setTextFill(error ? Color.SALMON : Color.LIGHTGREEN);
    }

    /**
     * Copies the editor state into the panel: which tool and enemy type are picked, the hint for the
     * tool, and the selected enemy's settings. Runs every frame, so keyboard shortcuts show up too.
     */
    private void refreshPanel() {
        refreshingPanel = true;
        playButton.setText(paused ? "Play  (Enter)" : "Pause  (Enter)");
        showPicked(playButton, paused);
        refreshWaveSettings();
        showPicked(placeToolButton, !pathMode);
        showPicked(pathToolButton, pathMode);
        toolHint.setText(pathMode ? PATH_TOOL_HINT : PLACE_TOOL_HINT);
        enemyTypeTitle.setDisable(pathMode);
        enemyTypeButtons.setDisable(pathMode);
        showPicked(squidButton, placingType == Alien.Type.SQUID);
        showPicked(crabButton, placingType == Alien.Type.CRAB);
        showPicked(octopusButton, placingType == Alien.Type.OCTOPUS);

        Extermination.EnemyDesign e = selectedEnemy;
        selectedTabs.setDisable(e == null);
        if (e == null) {
            showBossTab(false);
        }
        fitTabsToContent();
        if (e == null) {
            selectedTitle.setText("3. SELECTED ENEMY: none\n(click an enemy to change it)");
        } else {
            selectedTitle.setText("3. SELECTED ENEMY: " + e.getType() + " #" + sandbox.groupNumber(e) + "\n(" + e.getWaypoints().size()
                    + " path points, " + (e.entersFromEdge() ? "flies in from the edge)" : "appears on screen)"));
            showNumber(speedSpinner, e.getSpeed());
            showNumber(countSpinner, e.getCount());
            showNumber(healthSpinner, e.getHealth());
            showNumber(repeatsSpinner, e.getRepeats());
            showNumber(repeatGapSpinner, e.getRepeatGap());
            boolean keepsComing = e.getUntil() != null;
            repeatsSpinner.setDisable(keepsComing);
            repeatGapSpinner.setDisable(e.getRepeats() == 0 && !keepsComing);
            repeatGapLabel.setText(e.isRepeatAfterGone() ? "Then waits, s" : "Every, s (batch to batch)");
            showPicked(repeatTimerButton, !e.isRepeatAfterGone());
            showPicked(repeatGoneButton, e.isRepeatAfterGone());
            if (pickingUntil) {
                untilLabel.setText("Click the group it should keep coming until (Esc: cancel)");
            } else if (keepsComing) {
                untilLabel.setText("Keeps sending batches until group #" + sandbox.groupNumber(e.getUntil()) + " ("
                        + e.getUntil().getType() + ") is gone. Repeats doesn't count then.");
            } else {
                untilLabel.setText("Sends its repeats, then stops. When gone: the next batch waits until this one is shot down or has flown off.");
            }
            showNumber(spacingSpinner, e.getSpacing());
            formationButton.setText("Formation: " + formationName(e.getFormation()));
            showNumber(spawnSpinner, e.getSpawnTime());
            loopCheck.setSelected(e.isLoop());
            if (pointOwner != e) {
                drawingRoute = false;
                routePoint = 0;
                pointOwner = e;
                selectedPoint = 0;
                selectedPhase = 0;
                selectedStep = 0;
            }
            selectedPoint = Math.min(selectedPoint, e.getWaypoints().size()); // in case points were undone
            stopPointName.setText(selectedPoint == 0 ? "start" : "point " + selectedPoint);
            stopLabel.setText("For how long, s");
            stopHint.setText(e.getWaypoints().isEmpty() ? "Draw a path first (Path tool), then pick a point here."
                    : "Use < > to pick where it holds still (yellow ring on the map). 0 = doesn't stop.");
            showNumber(stopSpinner, e.getStopTime(selectedPoint));
            fireStoppedCheck.setSelected(e.isFireOnlyWhenStopped());
            faceMovementCheck.setSelected(e.isFacesMovement());
            faceMovementCheck.setDisable(e.getFormation() == Extermination.Formation.LINE || e.isBoss());
            pointingButton.setText("Picture: " + pointingName(e.getPointing()));
            pointingButton.setDisable(e.isBoss());
            // A boss is one enemy sent once, with health per attack, so the group settings are hidden for it
            boolean boss = e.isBoss();
            bossCheck.setSelected(boss);
            show(groupBox, !boss);
            show(loopCheck, !boss);
            show(fireStoppedCheck, !boss);
            pathTitle.setText(boss ? "ITS WAY IN  (fights where it ends)" : "ITS PATH");
            showBossTab(boss);
            // The attack (phase) picker sits on the Boss tab for a boss, else at the top of the Bullets tab
            javafx.scene.layout.VBox attackHome = boss ? bossTabBox : bulletsBox;
            if (attackBox.getParent() != attackHome) {
                attackHome.getChildren().add(0, attackBox);
            }
            Extermination.ShotPhase p = phase();
            showNumber(bulletCountSpinner, p.getBulletCount());
            showNumber(spreadSpinner, p.getSpread());
            showNumber(bulletSpeedSpinner, p.getBulletSpeed());
            showNumber(intervalSpinner, p.getFireInterval());
            showNumber(firstShotSpinner, e.getFirstShotDelay());
            showNumber(spinSpinner, p.getSpin());
            showNumber(bulletSizeSpinner, p.getBulletSize());
            showNumber(bouncesSpinner, p.getBounces());
            showNumber(directionSpinner, p.getDirection());
            showNumber(sweepSpinner, p.getSweep());
            showNumber(swingSpinner, p.getSwing());
            showNumber(beamSpinner, p.getBeamTime());
            showNumber(wallWidthSpinner, p.getWallWidth());
            String colorName = p.getColor().name();
            colorButton.setText("Color: " + colorName.charAt(0) + colorName.substring(1).toLowerCase());
            colorButton.setTextFill(BULLET_COLORS.get(p.getColor()).darker());
            String shapeName = p.getShape().name();
            shapeButton.setText("Shape: " + shapeName.charAt(0) + shapeName.substring(1).toLowerCase());
            // The attack being edited, and which of its patterns (they all fire at the same time)
            int start = e.attackStart(selectedPhase);
            int inAttack = e.attackEnd(start) - start;
            attackTitle.setText((boss ? "Attack " : "Phase ") + e.attackNumber(selectedPhase) + "/" + e.attackCount());
            phaseTitle.setText("Pattern " + (selectedPhase - start + 1) + "/" + inAttack);
            patternsInfo.setText(inAttack > 1
                    ? "The " + inAttack + " patterns of " + (boss ? "attack " : "phase ") + e.attackNumber(selectedPhase) + " fire at the same time."
                    : "+ Add: another pattern that fires at the same time.");
            showNumber(delaySpinner, p.getDelay());
            showPicked(fromSpotButton, pickingFromSpot);
            fromSpotButton.setText(pickingFromSpot ? "Click the map..." : "+ Add a spot");
            show(fromEnemyButton, p.hasFromSpot() && !pickingFromSpot);
            fromHint.setText(pickingFromSpot ? "Click the map where the new spot goes (the edges work too). Esc: cancel."
                    : p.hasFromSpot() ? "THIS IS A SPOT (bright cyan cross): its own extra bullets, separate from the enemy's. Set them up below. On the map: click a cross to edit it, drag to move it, right-click to remove it."
                    : (e.isBoss() ? "The boss's" : "The enemy's") + " own bullets. + Add a spot: extra bullets from anywhere you click, like an edge of the screen; this pattern keeps firing as it is.");
            Extermination.ShotPhase lead = lead();
            // On a boss path, a pattern can fire only at some of the points
            show(pointFireBox, boss && lead.getMove() == Extermination.BossMove.PATH && !lead.getMovePath().isEmpty());
            refreshAtPointMenu(p, e);
            lastsLabel.setText(boss ? "Time limit, s  (0 = none)" : "Lasts, s  (0 = for good)");
            showNumber(lastsSpinner, lead.getDuration());
            if (!cardNameField.isFocused() && !cardNameField.getText().equals(lead.getCardName())) {
                cardNameField.setText(lead.getCardName());
            }
            showNumber(cardHealthSpinner, lead.getCardHealth());
            showNumber(hopSpinner, lead.getHop());
            showPicked(moveStayButton, lead.getMove() == Extermination.BossMove.STAY);
            showPicked(moveWanderButton, lead.getMove() == Extermination.BossMove.WANDER);
            showPicked(movePathButton, lead.getMove() == Extermination.BossMove.PATH);
            show(hopSpinner.getParent(), lead.getMove() == Extermination.BossMove.WANDER);
            boolean route = boss && lead.getMove() == Extermination.BossMove.PATH;
            routeBox.setVisible(route);
            routeBox.setManaged(route);
            if (!route) {
                drawingRoute = false;
            }
            List<double[]> routePoints = lead.getMovePath();
            routePoint = Math.max(0, Math.min(routePoint, routePoints.size() - 1));
            showPicked(drawRouteButton, drawingRoute);
            drawRouteButton.setText(drawingRoute ? "Drawing: click the map" : "Draw its path on the map");
            routeStopLabel.setText(routePoints.isEmpty() ? "Stays at each point, s" : "Stays at point " + (routePoint + 1) + " for, s");
            if (route) {
                refreshPointAttacks(e, lead);
            }
            routeStopSpinner.setDisable(routePoints.isEmpty());
            showNumber(routeStopSpinner, routePoints.isEmpty() ? 0 : routePoints.get(routePoint)[2]);
            showNumber(routeSpeedSpinner, lead.getMoveSpeed());
            showPicked(routeOnceButton, !lead.isMoveLoop() && !lead.isMoveBounce());
            showPicked(routeLoopButton, lead.isMoveLoop() && !lead.isMoveBounce());
            showPicked(routeBounceButton, lead.isMoveBounce());
            refreshBulletMoves(p);
        }
        refreshBulletSettings(e == null ? null : phase());
        // The keys in brackets change the same setting from the keyboard
        speedLabel.setText("Speed, px/s  (- +)");
        countLabel.setText("How many  ([ ])");
        boolean follows = e != null && e.getFollows() != null;
        boolean fromSpawn = follows && e.isFollowsSpawn();
        spawnLabel.setText(!follows ? "Spawns, s into wave  (, .)" : fromSpawn ? "Spawns, s after it spawns" : "Spawns, s after clear");
        showPicked(followClearedButton, follows && !fromSpawn);
        showPicked(followSpawnedButton, fromSpawn);
        followClearedButton.setDisable(!follows);
        followSpawnedButton.setDisable(!follows);
        if (pickingFollow) {
            followLabel.setText("Click the group it should wait for (Esc: cancel)");
        } else if (follows) {
            followLabel.setText("Waits until group #" + sandbox.groupNumber(e.getFollows()) + " ("
                    + e.getFollows().getType() + (fromSpawn ? ") spawns" : ") is cleared") + ", then spawns.");
        } else {
            followLabel.setText("Spawns on the wave's timer." + (followProblem.isEmpty() ? "" : " " + followProblem));
        }
        int running = sandbox.getRunningWave();
        String runState = sandbox.isLevelClear() ? "Level clear!"
                : running < 0 ? "Waiting for wave 1"
                : String.format("Running wave %d, %.1fs in", running + 1, sandbox.getTimeInWave(running));
        levelInfo.setText(runState + String.format("   Flying: %d   Hits: %d",
                sandbox.getEnemies().size(), sandbox.getHits()));
        refreshingPanel = false;
    }

    /** The Moves tab: the phase's acceleration and curve, and the step being edited. */
    private void refreshBulletMoves(Extermination.ShotPhase p) {
        movesTitle.setText("Every bullet of this pattern starts with:");
        showNumber(accelSpinner, p.getAccel());
        showNumber(curveSpinner, p.getCurve());
        showNumber(waveSpinner, p.getWave());
        showNumber(waveRateSpinner, p.getWaveRate());
        waveRateSpinner.setDisable(p.getWave() <= 0);
        Extermination.BulletStep st = step();
        stepBox.setDisable(st == null);
        if (st == null) {
            stepTitle.setText("None yet");
            return;
        }
        stepTitle.setText("Change " + (selectedStep + 1) + "/" + p.getSteps().size());
        showNumber(stepTimeSpinner, st.getTime());
        showNumber(stepSpeedSpinner, st.getSpeed());
        showNumber(stepAccelSpinner, st.getAccel());
        showNumber(stepCurveSpinner, st.getCurve());
        showNumber(stepTurnSpinner, st.getTurn());
        showNumber(stepSplitSpinner, st.getSplit());
        stepAimCheck.setSelected(st.isAim());
    }

    /** The Bullets tab: which pattern is picked, what it does, and only the boxes that pattern uses. */
    private void refreshBulletSettings(Extermination.ShotPhase e) {
        Extermination.Pattern pattern = e == null ? Extermination.Pattern.NONE : e.getPattern();
        ToggleButton[] buttons = {patternNone, patternAimed, patternFan, patternRing, patternSpiral, patternRandom, patternLaser, patternWall};
        for (ToggleButton button : buttons) {
            showPicked(button, e != null && pattern.name().equals(button.getUserData()));
        }
        patternHint.setText(PATTERN_HINTS.get(pattern));
        boolean shoots = pattern != Extermination.Pattern.NONE;
        boolean laser = pattern == Extermination.Pattern.LASER;
        boolean usesSpread = pattern == Extermination.Pattern.FAN || pattern == Extermination.Pattern.RANDOM || laser;
        boolean usesSpin = pattern == Extermination.Pattern.SPIRAL;
        show(bulletCountSpinner.getParent(), shoots);
        show(bulletSpeedSpinner.getParent(), shoots && !laser);
        show(bulletSizeSpinner.getParent(), shoots);
        show(colorButton, shoots);
        show(shapeButton, shoots && !laser);
        show(bouncesSpinner.getParent(), shoots && !laser);
        show(intervalSpinner.getParent(), shoots);
        show(spreadSpinner.getParent(), usesSpread);
        show(spinSpinner.getParent(), usesSpin);
        show(beamSpinner.getParent(), laser);
        show(wallWidthSpinner.getParent(), pattern == Extermination.Pattern.WALL);
        bulletCountLabel.setText("Bullets per shot");
        spreadLabel.setText("Spread, degrees");
        bulletSpeedLabel.setText("Bullet speed, px/s");
        intervalLabel.setText("Fires every, seconds");
        firstShotLabel.setText("1st shot, s after it shows");
        spinLabel.setText("Spin, degrees per shot");
        // The Aim tab
        Extermination.Aim aim = e == null ? Extermination.Aim.PLAYER : e.getAim();
        for (ToggleButton button : new ToggleButton[]{aimPlayer, aimOnce, aimFixed}) {
            showPicked(button, e != null && aim.name().equals(button.getUserData()));
            button.setDisable(!shoots);
        }
        aimTitle.setText(shoots ? "Where this pattern shoots" : "");
        aimHint.setText(shoots ? AIM_HINTS.get(aim) : "This pattern doesn't shoot (None). Pick a shot pattern on the Bullets tab first.");
        boolean fixed = aim == Extermination.Aim.FIXED;
        show(aimButtons, shoots);
        show(aimSettings, shoots);
        show(directionBox, fixed);
        directionLabel.setText("Direction, degrees");
    }

    /**
     * Turns a Spinner into a number box: values from min to max, the arrows step by step, shown with
     * the given number of decimals. A typed value is kept (within min and max) when Enter is pressed or
     * the box loses focus; then onChange gets it.
     */
    private void setUpNumberBox(Spinner<Double> box, double min, double max, double step, int decimals,
                                DoubleConsumer onChange) {
        SpinnerValueFactory.DoubleSpinnerValueFactory numbers =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step);
        String format = decimals == 0 ? "%.0f" : "%." + decimals + "f";
        numbers.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : String.format(Locale.ROOT, format, value);
            }

            @Override
            public Double fromString(String text) {
                try {
                    return Double.parseDouble(text.strip().replace(',', '.'));
                } catch (NumberFormatException notANumber) {
                    return numbers.getValue(); // keep the old value
                }
            }
        });
        box.setValueFactory(numbers);
        box.setTooltip(new Tooltip("From " + numbers.getConverter().toString(min) + " to "
                + numbers.getConverter().toString(max) + ". A number outside that is set to the nearest limit."));
        box.setFocusTraversable(false);
        box.valueProperty().addListener((obs, old, value) -> {
            if (!refreshingPanel && value != null) onChange.accept(value);
        });
        // Keep what was typed when the box loses focus (Enter also ends up here)
        box.getEditor().focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) commitNumberBox(box);
        });
        box.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) commitNumberBox(box);
        });
        // Clicking into a box selects what's there, so typing replaces the old number instead of adding to it
        box.getEditor().focusedProperty().addListener((obs, was, focused) -> {
            if (focused) Platform.runLater(box.getEditor()::selectAll);
        });
    }

    /**
     * Flashes the box red for a moment if what was typed can't be kept as it is: it isn't a number
     * (the old value comes back) or it's outside the box's limits (the nearest limit is used).
     */
    private static void warnIfNotKept(Spinner<Double> box) {
        SpinnerValueFactory.DoubleSpinnerValueFactory numbers =
                (SpinnerValueFactory.DoubleSpinnerValueFactory) box.getValueFactory();
        boolean kept;
        try {
            double typed = Double.parseDouble(box.getEditor().getText().strip().replace(',', '.'));
            kept = typed >= numbers.getMin() && typed <= numbers.getMax();
        } catch (NumberFormatException notANumber) {
            kept = false;
        }
        if (!kept) {
            box.getEditor().setStyle("-fx-background-color: #ff9a9a;");
            PauseTransition flash = new PauseTransition(Duration.seconds(1));
            flash.setOnFinished(e -> box.getEditor().setStyle(""));
            flash.play();
        }
    }

    /** Reads the typed text into the box's value, kept within its limits; puts the old value back if it isn't a number. */
    private static void commitNumberBox(Spinner<Double> box) {
        SpinnerValueFactory.DoubleSpinnerValueFactory numbers =
                (SpinnerValueFactory.DoubleSpinnerValueFactory) box.getValueFactory();
        try {
            double typed = numbers.getConverter().fromString(box.getEditor().getText());
            numbers.setValue(Math.max(numbers.getMin(), Math.min(numbers.getMax(), typed)));
        } catch (NumberFormatException e) {
            // not a number: fall through and show the old value again
        }
        box.getEditor().setText(numbers.getConverter().toString(numbers.getValue()));
    }

    /** The number box the keyboard is in right now, or null if the keys belong to the game. */
    private Spinner<Double> numberBoxBeingTyped(Scene scene) {
        Object focused = scene.getFocusOwner();
        for (Spinner<Double> box : List.of(speedSpinner, countSpinner, healthSpinner, repeatsSpinner, repeatGapSpinner, spacingSpinner, spawnSpinner, waitSpinner, bulletCountSpinner,
                spreadSpinner, bulletSpeedSpinner, intervalSpinner, firstShotSpinner, spinSpinner, stopSpinner, lastsSpinner, accelSpinner, curveSpinner, waveSpinner, waveRateSpinner,
                stepTimeSpinner, stepSpeedSpinner, stepAccelSpinner, stepCurveSpinner, stepTurnSpinner, bulletSizeSpinner,
                bouncesSpinner, stepSplitSpinner, directionSpinner, sweepSpinner, swingSpinner, beamSpinner, wallWidthSpinner,
                cardHealthSpinner, hopSpinner, delaySpinner, routeStopSpinner, routeSpeedSpinner, pointShotsSpinner)) {
            if (focused == box || focused == box.getEditor()) {
                return box;
            }
        }
        return null;
    }

    /** Keys that edit the text of a number box: digits, the decimal point, minus, deleting and moving the cursor. */
    private static boolean isTypingKey(KeyEvent e) {
        KeyCode code = e.getCode();
        return code.isDigitKey() || code.isNavigationKey() || code.isModifierKey() || e.isShortcutDown()
                || code == KeyCode.PERIOD || code == KeyCode.DECIMAL || code == KeyCode.COMMA
                || code == KeyCode.MINUS || code == KeyCode.SUBTRACT
                || code == KeyCode.BACK_SPACE || code == KeyCode.DELETE;
    }

    /** True if the clicked thing is the given node or something inside it (like a number box's text or arrows). */
    private static boolean isInside(Object target, javafx.scene.Node node) {
        for (javafx.scene.Node n = target instanceof javafx.scene.Node t ? t : null; n != null; n = n.getParent()) {
            if (n == node) {
                return true;
            }
        }
        return false;
    }

    /** Esc in a number box: forget what was typed and show the current value again. */
    private static void revertNumberBox(Spinner<Double> box) {
        box.getEditor().setText(box.getValueFactory().getConverter().toString(box.getValue()));
    }

    /** Shows a value in a number box, unless someone is typing in it right now. */
    private static void showNumber(Spinner<Double> box, double value) {
        if (!box.isFocused() && !box.getEditor().isFocused() && (box.getValue() == null || box.getValue() != value)) {
            box.getValueFactory().setValue(value);
        }
    }

    /** The wave section: which wave is being edited and when it starts. */
    private void refreshWaveSettings() {
        Extermination.Wave wave = sandbox.getWaves().get(editWave);
        waveTitle.setText("Wave " + (editWave + 1) + " of " + sandbox.getWaves().size());
        boolean first = editWave == 0;
        waveRuleLabel.setText(first ? "Wave 1 starts with the level, after:"
                : "Starts once wave " + editWave + " has been:");
        waveRuleButtons.setDisable(first);
        showPicked(afterClearedButton, !first && wave.isAfterCleared());
        showPicked(afterStartedButton, !first && !wave.isAfterCleared());
        waitLabel.setText("Then wait, seconds");
        showNumber(waitSpinner, wave.getWait());
        waveBombCheck.setSelected(wave.isDropsBomb());
    }

    /** Marks a tool or enemy-type button as picked: pressed in and orange, so it's easy to see. */
    private static void showPicked(ButtonBase button, boolean picked) {
        if (button instanceof ToggleButton toggle) {
            toggle.setSelected(picked);
        }
        button.setStyle(picked ? "-fx-base: orange; -fx-font-weight: bold;" : "");
    }

    /** Shows or hides the editor panel and resizes the window to fit. */
    private void setSandboxPanelVisible(boolean visible) {
        sandboxPanel.setVisible(visible);
        sandboxPanel.setManaged(visible);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            // Resized after the next layout pass, once the panel's wrapped text knows its real height
            Platform.runLater(() -> {
                if (root.getScene() != null && root.getScene().getWindow() != null) {
                    root.getScene().getWindow().sizeToScene();
                }
            });
        }
    }

    /** Dragging in path mode draws the path: a waypoint is dropped every DRAG_POINT_SPACING pixels. */
    private void onSandboxDrag(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        if (screen == Screen.SANDBOX && draggingSpot != null) {
            draggingSpot.setFromSpot(e.getX(), e.getY());
            return;
        }
        if (screen == Screen.SANDBOX && draggingGroup && selectedEnemy != null) {
            double dx = e.getX() - dragFromX;
            double dy = e.getY() - dragFromY;
            if (groupMoved || Math.hypot(dx, dy) >= MOVE_THRESHOLD) {
                groupMoved = true;
                selectedEnemy.moveBy(dx, dy);
                dragFromX = e.getX();
                dragFromY = e.getY();
            }
            return;
        }
        if (screen != Screen.SANDBOX || !drawingPath || selectedEnemy == null) {
            return;
        }
        double[] end = selectedEnemy.getPathEnd();
        if (Math.hypot(e.getX() - end[0], e.getY() - end[1]) >= DRAG_POINT_SPACING) {
            selectedEnemy.addWaypoint(e.getX(), e.getY());
        }
    }

    /** -1 when the first key is held, 1 for the second, 0 when both or neither are held. */
    private int axis(KeyCode negative, KeyCode positive) {
        int value = 0;
        if (heldKeys.contains(negative)) value -= 1;
        if (heldKeys.contains(positive)) value += 1;
        return value;
    }

    /** Up/Down move the yellow arrow, Space picks the option it points at. */
    private void onMenuKey(KeyCode code) {
        switch (code) {
            case UP -> selectedOption = (selectedOption + MENU_OPTIONS.length - 1) % MENU_OPTIONS.length;
            case DOWN -> selectedOption = (selectedOption + 1) % MENU_OPTIONS.length;
            case SPACE -> {
                waitForSpaceRelease = true;
                switch (selectedOption) {
                    case START_GAME -> showModeSelect();
                    case SANDBOX -> openSandbox();
                    case SCOREBOARD -> showScoreboard(-1);
                    case BACK_TO_MAIN_MENU -> onMainMenu();
                    default -> { }
                }
            }
            default -> { }
        }
    }

    /** Up/Down move the yellow arrow between modes, Space picks one, Esc goes back to the menu. */
    private void onModeSelectKey(KeyCode code) {
        switch (code) {
            case UP -> selectedMode = (selectedMode + MODE_OPTIONS.length - 1) % MODE_OPTIONS.length;
            case DOWN -> selectedMode = (selectedMode + 1) % MODE_OPTIONS.length;
            case ESCAPE -> showMenu();
            case SPACE -> {
                waitForSpaceRelease = true;
                switch (selectedMode) {
                    case PLANET_DEFENSE -> showIntro();
                    case EXTERMINATION -> startRun();
                    case MODE_BACK -> showMenu();
                    default -> { }
                }
            }
            default -> { }
        }
    }

    private void showModeSelect() {
        screen = Screen.MODE_SELECT;
        selectedMode = PLANET_DEFENSE;
        drawModeSelect();
    }

    /** Shows the Space Invaders menu. Any game in progress is dropped. */
    @FXML
    private void onShowTitle() {
        showMenu();
    }

    private void showMenu() {
        endRun();
        screen = Screen.MENU;
        selectedOption = START_GAME;
        // The menu is only the title and the options, so the score bar and controls line are hidden
        setBarsVisible(false);
        if (sandboxPanel.isVisible()) {
            topBar.setManaged(true);
            setSandboxPanelVisible(false);
        }
        drawMenu();
        root.requestFocus();
    }

    /** The Planet Defense intro: what each alien is worth, before the game starts. */
    private void showIntro() {
        screen = Screen.INTRO;
        introTimeLeft = INTRO_SECONDS;
        drawIntro();
    }

    private void startGame() {
        game = new SpaceInvadersMechanics();
        heldKeys.remove(KeyCode.ENTER);
        screen = Screen.PLAYING;
        setBarsVisible(true);
        controlsLabel.setText(GAME_CONTROLS);
        root.requestFocus();
    }

    private void setBarsVisible(boolean visible) {
        topBar.setVisible(visible);
        controlsLabel.setVisible(visible);
    }

    /** The developer sandbox for designing Extermination levels, shown on this same canvas. */
    private void openSandbox() {
        // The level is kept while the app is open, so leaving the sandbox by accident doesn't lose it
        if (sandbox == null) {
            sandbox = new Extermination();
            saveMessage.setText("");
        }
        sandbox.resetEnemies();
        selectedEnemy = null;
        paused = true;
        pathMode = false;
        screen = Screen.SANDBOX;
        topBar.setVisible(false);
        topBar.setManaged(false); // the empty score bar takes no room, so the editor panel fits
        controlsLabel.setVisible(true);
        controlsLabel.setText(SANDBOX_CONTROLS);
        setSandboxPanelVisible(true);
        refreshPanel();
        root.requestFocus();
    }

    /** Back to the arcade's game selection. */
    @FXML
    private void onMainMenu() {
        loop.stop();
        SceneSwitcher.showMenu(root);
    }

    // ------------------------------------------------------------------ Extermination run

    /** Starts a run of Extermination: the run's levels one after another, keeping lives, bombs, power and score. */
    private void startRun() {
        runLevels = Extermination.runLevels();
        if (runLevels.isEmpty()) {
            return; // no levels in STAGES yet
        }
        if (!inRun) {
            editorLevel = sandbox; // the sandbox's level is kept for when the run ends
            inRun = true;
        }
        sandbox = new Extermination();
        sandbox.resetRun();
        runStage = 0;
        sandbox.startNextLevel(runLevels.get(0));
        runPhase = RunPhase.PLAYING;
        runPaused = false;
        runTimeOwed = 0;
        bombQueued = false;
        screen = Screen.RUN;
        topBar.setVisible(false);
        controlsLabel.setVisible(true);
        controlsLabel.setText(RUN_CONTROLS);
        root.requestFocus();
    }

    /** Puts the sandbox's own level back after a run. */
    private void endRun() {
        if (inRun) {
            sandbox = editorLevel;
            editorLevel = null;
            inRun = false;
        }
    }

    /** Plays the run in fixed ticks and moves it on: next level, game over, all clear, then the name. */
    private void updateRun(double deltaSeconds) {
        if (runPaused || runPhase == RunPhase.NAME) {
            return;
        }
        if (runPhase == RunPhase.CONTINUE) {
            runTimer -= deltaSeconds;
            if (runTimer <= 0) {
                answerContinue(false); // the countdown ran out: no
            }
            return;
        }
        if (runPhase == RunPhase.ENDED) {
            runTimer -= deltaSeconds;
            if (runWon) {
                tickRun(deltaSeconds, false); // the last items still fly in
            }
            if (runTimer <= 0) {
                if (scoreboard().qualifies(sandbox.getScore())) {
                    runPhase = RunPhase.NAME;
                    nameSlot = 0;
                    java.util.Arrays.fill(nameLetters, 'A');
                    // A held fire key must be let go first, or its key repeat would type the name by itself
                    waitForSpaceRelease = heldKeys.contains(KeyCode.SPACE) || heldKeys.contains(KeyCode.Z);
                } else {
                    showScoreboard(-1);
                }
            }
            return;
        }
        tickRun(deltaSeconds, runPhase == RunPhase.PLAYING);
        if (runPhase == RunPhase.PLAYING) {
            if (sandbox.isOutOfLives() && sandbox.getContinuesLeft() > 0) {
                runPhase = RunPhase.CONTINUE; // the game holds still while the player decides
                continueYes = true;
                runTimer = Extermination.CONTINUE_TIME;
                waitForSpaceRelease = heldKeys.contains(KeyCode.SPACE) || heldKeys.contains(KeyCode.Z);
            } else if (sandbox.isOutOfLives()) {
                runPhase = RunPhase.ENDED;
                runWon = false;
                runTimer = 3;
            } else if (sandbox.isLevelClear()) {
                boolean last = runStage == runLevels.size() - 1;
                runPhase = last ? RunPhase.ENDED : RunPhase.STAGE_CLEAR;
                runWon = last;
                runTimer = last ? 4 : Extermination.STAGE_CLEAR_TIME;
            }
        } else if (runPhase == RunPhase.STAGE_CLEAR) {
            runTimer -= deltaSeconds;
            if (runTimer <= 0) {
                runStage++;
                sandbox.startNextLevel(runLevels.get(runStage));
                runPhase = RunPhase.PLAYING;
            }
        }
    }

    /** Runs the game's 1/60 s ticks owed for this frame; firing and bombs only while a level is on. */
    private void tickRun(double deltaSeconds, boolean canShoot) {
        boolean fire = canShoot && heldKeys.contains(KeyCode.Z) && !waitForSpaceRelease;
        runTimeOwed += deltaSeconds;
        while (runTimeOwed >= Extermination.TICK) {
            sandbox.tick(axis(KeyCode.LEFT, KeyCode.RIGHT), axis(KeyCode.UP, KeyCode.DOWN),
                    heldKeys.contains(KeyCode.SHIFT), fire, bombQueued && canShoot);
            bombQueued = false;
            runTimeOwed -= Extermination.TICK;
        }
    }

    /** Keys during a run: X bombs, Enter pauses, Esc quits; typing the name uses the arrows and Space. */
    private void onRunKey(KeyCode code) {
        if (runPhase == RunPhase.NAME) {
            onNameKey(code);
            return;
        }
        if (runPhase == RunPhase.CONTINUE) {
            switch (code) {
                case UP, DOWN, LEFT, RIGHT -> continueYes = !continueYes;
                case SPACE, Z, ENTER -> {
                    if (!(code != KeyCode.ENTER && waitForSpaceRelease)) {
                        answerContinue(continueYes);
                    }
                }
                case ESCAPE -> answerContinue(false);
                default -> { }
            }
            return;
        }
        switch (code) {
            case X -> bombQueued = !runPaused;
            case ENTER -> runPaused = !runPaused && runPhase == RunPhase.PLAYING;
            case ESCAPE -> showMenu();
            default -> { }
        }
    }

    /**
     * Typing the 3-letter name, like the arcade: Up/Down change the letter, Left/Right move between the
     * letters, Space (or Z) keeps the letter and moves on; on the last one, or with Enter, the name is saved.
     */
    private void onNameKey(KeyCode code) {
        switch (code) {
            case UP -> nameLetters[nameSlot] = nameLetters[nameSlot] == 'Z' ? 'A' : (char) (nameLetters[nameSlot] + 1);
            case DOWN -> nameLetters[nameSlot] = nameLetters[nameSlot] == 'A' ? 'Z' : (char) (nameLetters[nameSlot] - 1);
            case LEFT -> nameSlot = Math.max(0, nameSlot - 1);
            case RIGHT -> nameSlot = Math.min(nameLetters.length - 1, nameSlot + 1);
            case SPACE, Z -> {
                if (waitForSpaceRelease) {
                    return;
                }
                if (nameSlot < nameLetters.length - 1) {
                    nameSlot++;
                } else {
                    saveName();
                }
            }
            case ENTER -> saveName();
            case ESCAPE -> showScoreboard(-1); // leave without putting a name on the board
            default -> { }
        }
    }

    /** Yes: the run goes on with a fresh set of lives and bombs. No: game over. */
    private void answerContinue(boolean yes) {
        if (yes && sandbox.useContinue()) {
            runPhase = RunPhase.PLAYING;
            waitForSpaceRelease = true; // the Space that said yes doesn't also start firing
        } else {
            runPhase = RunPhase.ENDED;
            runWon = false;
            runTimer = 2;
        }
    }

    private void saveName() {
        int levelReached = runWon ? Extermination.RUN_LEVELS + 1 : runStage + 1; // past the last level: cleared them all
        int place = scoreboard().add(new String(nameLetters), sandbox.getScore(), levelReached);
        waitForSpaceRelease = true; // the Space that saved the name doesn't also leave the scoreboard
        showScoreboard(place);
    }

    private Extermination.Scoreboard scoreboard() {
        if (scoreboard == null) {
            scoreboard = new Extermination.Scoreboard();
        }
        return scoreboard;
    }

    /** The scoreboard screen; highlight is the line to light up (a name just added), or -1. */
    private void showScoreboard(int highlight) {
        endRun();
        scoreboardHighlight = highlight;
        screen = Screen.SCORES;
        setBarsVisible(false);
        drawScoreboard();
        root.requestFocus();
    }

    /** A run in play: the enemies, items, bullets, bombs and ship, the counters, and the level banners. */
    private void drawRun() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (runPhase == RunPhase.NAME) {
            drawNameEntry(g);
            return;
        }
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, h);
        for (Extermination.Enemy enemy : sandbox.getEnemies()) {
            if (enemy.isAppearing()) {
                drawAppearing(g, enemy);
            } else {
                drawEnemy(g, enemy);
                drawEnemyHealth(g, enemy);
            }
        }
        drawBossBar(g);
        drawItems(g);
        drawEnemyBullets(g);
        drawBombs(g);
        drawDeaths(g);
        drawFlaks(g);
        drawPlayerShots(g);
        drawShip(g);
        drawRunHud(g);

        String banner = null;
        if (runPhase == RunPhase.PLAYING && sandbox.getLevelTime() < 2.5) {
            banner = "STAGE " + (runStage + 1);
        } else if (runPhase == RunPhase.STAGE_CLEAR) {
            banner = "STAGE CLEAR";
        } else if (runPhase == RunPhase.ENDED) {
            banner = runWon ? "ALL CLEAR!" : "GAME OVER";
        } else if (runPaused) {
            banner = "PAUSED";
        }
        if (banner != null) {
            g.setFill(runPhase == RunPhase.ENDED && !runWon ? Color.RED : Color.web("#ffe08a"));
            drawPixelTextCentered(g, banner, h / 2 - 60, 6);
        }
        if (runPhase == RunPhase.CONTINUE) {
            // CONTINUE? with a countdown, YES / NO, and how many continues are left
            g.setFill(Color.color(0, 0, 0, 0.6));
            g.fillRect(0, 0, w, h);
            g.setFill(Color.web("#ffe08a"));
            drawPixelTextCentered(g, "CONTINUE?", 150, 6);
            g.setFill(OPTION);
            drawPixelTextCentered(g, String.valueOf((int) Math.ceil(Math.max(0, runTimer - 0.001))), 215, 8);
            double scale = 4;
            double optionX = w / 2 - pixelTextWidth("YES", scale) / 2;
            drawPixelText(g, "YES", optionX, 320, scale);
            drawPixelText(g, "NO", optionX, 370, scale);
            drawMenuArrow(g, optionX - 30, continueYes ? 320 : 370, scale);
            g.setFill(Color.GRAY);
            int left = sandbox.getContinuesLeft();
            drawPixelTextCentered(g, left + (left == 1 ? " CONTINUE LEFT" : " CONTINUES LEFT"), 440, 2);
        }
    }

    /** The name entry: the score, and three big letters with the one being picked marked. */
    private void drawNameEntry(GraphicsContext g) {
        double w = canvas.getWidth();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, canvas.getHeight());
        g.setFill(runWon ? Color.web("#ffe08a") : Color.RED);
        drawPixelTextCentered(g, runWon ? "ALL CLEAR!" : "GAME OVER", 60, 6);
        g.setFill(TITLE);
        drawPixelTextCentered(g, "NEW HIGH SCORE", 150, 4);
        g.setFill(OPTION);
        drawPixelTextCentered(g, String.format("%09d", sandbox.getScore()), 200, 4);
        drawPixelTextCentered(g, "ENTER YOUR NAME", 270, 3);
        double scale = 10;
        double letterW = 5 * scale;
        double gap = 30;
        double left = (w - (3 * letterW + 2 * gap)) / 2;
        boolean blinkOn = (System.nanoTime() / 300_000_000) % 2 == 0;
        for (int i = 0; i < nameLetters.length; i++) {
            double x = left + i * (letterW + gap);
            boolean picked = i == nameSlot;
            g.setFill(picked ? MENU_ARROW : OPTION);
            if (!picked || blinkOn) {
                drawPixelText(g, String.valueOf(nameLetters[i]), x, 320, scale);
            }
            if (picked) {
                g.fillRect(x, 320 + 8 * scale, letterW, 6);
                // Little arrows above and below: Up/Down change it
                g.fillPolygon(new double[]{x + letterW / 2 - 10, x + letterW / 2 + 10, x + letterW / 2}, new double[]{310, 310, 296}, 3);
                g.fillPolygon(new double[]{x + letterW / 2 - 10, x + letterW / 2 + 10, x + letterW / 2}, new double[]{416, 416, 430}, 3);
            }
        }
        g.setFill(Color.GRAY);
        g.setFont(Font.font("Monospaced", 13));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("Up/Down: letter    Left/Right: move    Space: OK    Enter: done", w / 2, 480);
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** The scoreboard: the best runs, highest first, with the name just entered lit up. */
    private void drawScoreboard() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, canvas.getHeight());
        g.setFill(TITLE);
        drawPixelTextCentered(g, "SCOREBOARD", 40, 6);
        double scale = 3;
        g.setFill(Color.GRAY);
        String header = " NO  NAME     SCORE  LVL";
        double left = (w - pixelTextWidth(header, scale)) / 2;
        drawPixelText(g, header, left, 120, scale);
        List<Extermination.Scoreboard.Entry> entries = scoreboard().getEntries();
        for (int i = 0; i < Extermination.SCOREBOARD_SIZE; i++) {
            String line;
            if (i < entries.size()) {
                Extermination.Scoreboard.Entry e = entries.get(i);
                String stage = e.levelReached() > Extermination.RUN_LEVELS ? "ALL" : " " + e.levelReached() + " ";
                line = String.format("%3d  %s  %09d  %s", i + 1, e.name(), e.score(), stage);
            } else {
                line = String.format("%3d  ---  ---------  ---", i + 1);
            }
            g.setFill(i == scoreboardHighlight ? MENU_ARROW : i < entries.size() ? OPTION : Color.web("#555555"));
            drawPixelText(g, line, left, 160 + i * 34, scale);
        }
        g.setFill(Color.GRAY);
        drawPixelTextCentered(g, "SPACE: BACK", 520, 2);
    }

    // ------------------------------------------------------------------ drawing

    /** The menu: a big pixel title with the options under it and a yellow arrow on the selected one. */
    private void drawMenu() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, canvas.getHeight());

        // Title: two lines of big blocky letters filling most of the screen
        g.setFill(TITLE);
        drawPixelTextCentered(g, "SPACE", 60, 11);
        drawPixelTextCentered(g, "INVADERS", 160, 11);

        // Options, all starting at the same x so the arrow lines up
        double scale = 4;
        double optionX = w / 2 - pixelTextWidth("BACK TO MAIN MENU", scale) / 2;
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            double y = 320 + i * 50;
            g.setFill(i == SANDBOX ? OPTION_DEVELOPER : OPTION);
            drawPixelText(g, MENU_OPTIONS[i], optionX, y, scale);
            if (i == selectedOption) {
                drawMenuArrow(g, optionX - 30, y, scale);
            }
        }
    }

    /** The mode select: a heading and the modes, with the same yellow arrow as the menu. */
    private void drawModeSelect() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, canvas.getHeight());

        g.setFill(TITLE);
        drawPixelTextCentered(g, "SELECT MODE", 110, 7);

        double scale = 4;
        double optionX = w / 2 - pixelTextWidth("PLANET DEFENSE", scale) / 2;
        for (int i = 0; i < MODE_OPTIONS.length; i++) {
            double y = 260 + i * 60;
            g.setFill(OPTION);
            drawPixelText(g, MODE_OPTIONS[i], optionX, y, scale);
            if (i == selectedMode) {
                drawMenuArrow(g, optionX - 30, y, scale);
            }
        }
    }

    /** The Planet Defense intro: the mode name and the points table, like the arcade attract screen. */
    private void drawIntro() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, w, canvas.getHeight());

        g.setFill(TITLE);
        drawPixelTextCentered(g, "PLANET DEFENSE", 70, 6);

        double scale = 4;
        double textX = w / 2 - pixelTextWidth("= ? MYSTERY", scale) / 2 + 25;
        double iconX = textX - 40;
        String[] lines = {"= ? MYSTERY", "= 30 POINTS", "= 20 POINTS", "= 10 POINTS"};
        for (int i = 0; i < lines.length; i++) {
            double y = 200 + i * 70;
            double iconY = y + 7 * scale / 2;
            switch (i) {
                case 0 -> drawMysteryShape(g, iconX, iconY);
                case 1 -> drawAlien(g, Alien.Type.SQUID, iconX, iconY);
                case 2 -> drawAlien(g, Alien.Type.CRAB, iconX, iconY);
                default -> drawAlien(g, Alien.Type.OCTOPUS, iconX, iconY);
            }
            g.setFill(OPTION);
            drawPixelText(g, lines[i], textX, y, scale);
        }
    }

    /** A small right-pointing pixel arrow, as tall as one line of option text. */
    private void drawMenuArrow(GraphicsContext g, double x, double y, double scale) {
        g.setFill(MENU_ARROW);
        int[] rowLengths = {1, 2, 3, 4, 3, 2, 1};
        for (int row = 0; row < rowLengths.length; row++) {
            g.fillRect(x, y + row * scale, rowLengths[row] * scale, scale);
        }
    }

    private void drawPixelTextCentered(GraphicsContext g, String text, double y, double scale) {
        drawPixelText(g, text, (canvas.getWidth() - pixelTextWidth(text, scale)) / 2, y, scale);
    }

    /** Width of text drawn with {@link #drawPixelText}: 5 blocks per letter plus a 1-block gap. */
    private static double pixelTextWidth(String text, double scale) {
        return (text.length() * 6 - 1) * scale;
    }

    /** Draws text as blocky 5x7 letters, each block scale pixels wide, with the top-left corner at (x, y). */
    private static void drawPixelText(GraphicsContext g, String text, double x, double y, double scale) {
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = PIXEL_LETTERS.get(text.charAt(i));
            if (glyph == null) {
                continue; // spaces and letters without a pattern are left blank
            }
            double left = x + i * 6 * scale;
            for (int row = 0; row < glyph.length; row++) {
                for (int col = 0; col < glyph[row].length(); col++) {
                    if (glyph[row].charAt(col) == '#') {
                        g.fillRect(left + col * scale, y + row * scale, scale, scale);
                    }
                }
            }
        }
    }

    /** 5x7 block patterns for the characters the menus and intro use. */
    private static final Map<Character, String[]> PIXEL_LETTERS = Map.ofEntries(
            Map.entry('A', new String[]{" ### ", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"}),
            Map.entry('B', new String[]{"#### ", "#   #", "#   #", "#### ", "#   #", "#   #", "#### "}),
            Map.entry('C', new String[]{" ### ", "#   #", "#    ", "#    ", "#    ", "#   #", " ### "}),
            Map.entry('D', new String[]{"#### ", "#   #", "#   #", "#   #", "#   #", "#   #", "#### "}),
            Map.entry('E', new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#####"}),
            Map.entry('F', new String[]{"#####", "#    ", "#    ", "#### ", "#    ", "#    ", "#    "}),
            Map.entry('G', new String[]{" ### ", "#   #", "#    ", "# ###", "#   #", "#   #", " ####"}),
            Map.entry('I', new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "#####"}),
            Map.entry('K', new String[]{"#   #", "#  # ", "# #  ", "##   ", "# #  ", "#  # ", "#   #"}),
            Map.entry('L', new String[]{"#    ", "#    ", "#    ", "#    ", "#    ", "#    ", "#####"}),
            Map.entry('M', new String[]{"#   #", "## ##", "# # #", "# # #", "#   #", "#   #", "#   #"}),
            Map.entry('N', new String[]{"#   #", "##  #", "# # #", "#  ##", "#   #", "#   #", "#   #"}),
            Map.entry('O', new String[]{" ### ", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "}),
            Map.entry('P', new String[]{"#### ", "#   #", "#   #", "#### ", "#    ", "#    ", "#    "}),
            Map.entry('R', new String[]{"#### ", "#   #", "#   #", "#### ", "# #  ", "#  # ", "#   #"}),
            Map.entry('S', new String[]{" ####", "#    ", "#    ", " ### ", "    #", "    #", "#### "}),
            Map.entry('T', new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "  #  "}),
            Map.entry('U', new String[]{"#   #", "#   #", "#   #", "#   #", "#   #", "#   #", " ### "}),
            Map.entry('V', new String[]{"#   #", "#   #", "#   #", "#   #", "#   #", " # # ", "  #  "}),
            Map.entry('X', new String[]{"#   #", "#   #", " # # ", "  #  ", " # # ", "#   #", "#   #"}),
            Map.entry('Y', new String[]{"#   #", "#   #", " # # ", "  #  ", "  #  ", "  #  ", "  #  "}),
            Map.entry('0', new String[]{" ### ", "#   #", "#  ##", "# # #", "##  #", "#   #", " ### "}),
            Map.entry('1', new String[]{"  #  ", " ##  ", "  #  ", "  #  ", "  #  ", "  #  ", " ### "}),
            Map.entry('2', new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", " #   ", "#####"}),
            Map.entry('3', new String[]{"#####", "    #", "   # ", "  ## ", "    #", "#   #", " ### "}),
            Map.entry('=', new String[]{"     ", "     ", "#####", "     ", "#####", "     ", "     "}),
            Map.entry('?', new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  "}),
            Map.entry('H', new String[]{"#   #", "#   #", "#   #", "#####", "#   #", "#   #", "#   #"}),
            Map.entry('J', new String[]{"  ###", "   # ", "   # ", "   # ", "   # ", "#  # ", " ##  "}),
            Map.entry('Q', new String[]{" ### ", "#   #", "#   #", "#   #", "# # #", "#  # ", " ## #"}),
            Map.entry('W', new String[]{"#   #", "#   #", "#   #", "# # #", "# # #", "## ##", "#   #"}),
            Map.entry('Z', new String[]{"#####", "    #", "   # ", "  #  ", " #   ", "#    ", "#####"}),
            Map.entry('4', new String[]{"   # ", "  ## ", " # # ", "#  # ", "#####", "   # ", "   # "}),
            Map.entry('5', new String[]{"#####", "#    ", "#### ", "    #", "    #", "#   #", " ### "}),
            Map.entry('6', new String[]{"  ## ", " #   ", "#    ", "#### ", "#   #", "#   #", " ### "}),
            Map.entry('7', new String[]{"#####", "    #", "   # ", "  #  ", " #   ", " #   ", " #   "}),
            Map.entry('8', new String[]{" ### ", "#   #", "#   #", " ### ", "#   #", "#   #", " ### "}),
            Map.entry('9', new String[]{" ### ", "#   #", "#   #", " ####", "    #", "   # ", " ##  "}),
            Map.entry('.', new String[]{"     ", "     ", "     ", "     ", "     ", " ##  ", " ##  "}),
            Map.entry('-', new String[]{"     ", "     ", "     ", "#####", "     ", "     ", "     "}),
            Map.entry('!', new String[]{"  #  ", "  #  ", "  #  ", "  #  ", "  #  ", "     ", "  #  "}),
            Map.entry(':', new String[]{"     ", " ##  ", " ##  ", "     ", " ##  ", " ##  ", "     "})
    );

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawShields(g);
        drawAliens(g);
        drawMystery(g);
        drawShot(g);
        drawPlayer(g);

        // Ground line under the cannon
        double groundY = SpaceInvadersMechanics.PLAYER_Y + SpaceInvadersMechanics.PLAYER_HEIGHT / 2 + 8;
        g.setStroke(CANNON);
        g.setLineWidth(2);
        g.strokeLine(0, groundY, canvas.getWidth(), groundY);

        drawHud();
        if (game.isGameOver()) {
            drawGameOver(g);
        }
    }

    /**
     * For a phase with a fixed direction, a dashed cyan line from the point shows where it will shoot;
     * with a swing, two lines show the ends it sweeps between.
     */
    private void drawAimPreview(GraphicsContext g, Extermination.ShotPhase p, double x, double y) {
        if (p == null || p.getAim() != Extermination.Aim.FIXED || p.getPattern() == Extermination.Pattern.NONE) {
            return;
        }
        g.setStroke(Color.CYAN);
        g.setLineWidth(1);
        g.setLineDashes(6, 5);
        List<Double> angles = p.getSwing() > 0 && p.getSweep() != 0
                ? List.of(p.getDirection() - p.getSwing() / 2, p.getDirection() + p.getSwing() / 2)
                : List.of(p.getDirection());
        for (double degrees : angles) {
            double a = Math.toRadians(degrees);
            g.strokeLine(x, y, x + Math.cos(a) * 120, y + Math.sin(a) * 120);
        }
        g.setLineDashes((double[]) null);
    }

    /** The sandbox: flight paths, the enemies, a preview of the next one, and the ship and its shots. */
    private void drawSandbox() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // The edited wave's paths go under everything else, with a faint alien where each group starts;
        // the selected enemy's path is drawn last so it's on top
        for (Extermination.EnemyDesign design : sandbox.getLayout()) {
            if (design.getWave() != editWave) {
                continue;
            }
            if (design != selectedEnemy) {
                drawPath(g, design, PATH);
            }
            g.setGlobalAlpha(0.3);
            // A formation shows its whole shape; a line shows just the first enemy
            int shown = design.getFormation() == Extermination.Formation.LINE ? 1 : design.getCount();
            for (int i = 0; i < shown; i++) {
                double[] offset = design.turnedOffset(i, design.startTurn());
                drawAlien(g, design.getType(), design.getMarkerX() + offset[0], design.getMarkerY() + offset[1], design.getScale());
            }
            g.setGlobalAlpha(1);
            // Its number in the wave, and an arrow from the group it waits for
            g.setFill(Color.GRAY);
            g.setFont(Font.font("Monospaced", 11));
            g.fillText("#" + sandbox.groupNumber(design), design.getMarkerX() - 22, design.getMarkerY() - 12);
            Extermination.EnemyDesign leader = design.getFollows();
            if (leader != null) {
                g.setStroke(Color.ORANGE);
                g.setLineWidth(1);
                g.setLineDashes(3, 4);
                g.strokeLine(leader.getMarkerX(), leader.getMarkerY(), design.getMarkerX(), design.getMarkerY());
                g.setLineDashes((double[]) null);
                double angle = Math.atan2(design.getMarkerY() - leader.getMarkerY(), design.getMarkerX() - leader.getMarkerX());
                double tipX = design.getMarkerX() - Math.cos(angle) * 14;
                double tipY = design.getMarkerY() - Math.sin(angle) * 14;
                g.strokeLine(tipX, tipY, tipX - Math.cos(angle - 0.5) * 8, tipY - Math.sin(angle - 0.5) * 8);
                g.strokeLine(tipX, tipY, tipX - Math.cos(angle + 0.5) * 8, tipY - Math.sin(angle + 0.5) * 8);
            }
        }
        if (selectedEnemy != null) {
            drawPath(g, selectedEnemy, PATH_SELECTED);
            drawRoute(g);
            // The point whose stop is being edited
            int shown = Math.min(selectedPoint, selectedEnemy.getWaypoints().size());
            double[] point = shown == 0
                    ? new double[]{selectedEnemy.getMarkerX(), selectedEnemy.getMarkerY()}
                    : selectedEnemy.getWaypoints().get(shown - 1);
            g.setStroke(Color.YELLOW);
            g.setLineWidth(1.5);
            g.strokeOval(point[0] - 7, point[1] - 7, 14, 14);
            // Spots the selected enemy's patterns fire from: the picked pattern's bright, the others faint
            for (Extermination.ShotPhase other : selectedEnemy.getPhases()) {
                if (other.hasFromSpot()) {
                    boolean picked = other == phase();
                    g.setStroke(picked ? Color.CYAN : Color.CYAN.deriveColor(0, 1, 0.6, 0.5));
                    g.setLineWidth(picked ? 2 : 1);
                    double fx = other.getFromX(), fy = other.getFromY();
                    g.strokeLine(fx - 9, fy, fx + 9, fy);
                    g.strokeLine(fx, fy - 9, fx, fy + 9);
                    g.strokeOval(fx - 5, fy - 5, 10, 10);
                }
            }
            Extermination.ShotPhase picked = phase();
            if (picked != null && picked.hasFromSpot()) {
                drawAimPreview(g, picked, picked.getFromX(), picked.getFromY());
            } else {
                drawAimPreview(g, picked, point[0], point[1]);
            }
            // When groups are stacked on its spot, say which one this is and how to get to the others
            List<Extermination.EnemyDesign> stacked = sandbox.findEnemiesNear(
                    selectedEnemy.getMarkerX(), selectedEnemy.getMarkerY(), editWave);
            stacked.removeIf(d -> Math.hypot(d.getMarkerX() - selectedEnemy.getMarkerX(),
                    d.getMarkerY() - selectedEnemy.getMarkerY()) > 6);
            if (stacked.size() > 1) {
                double labelX = Math.max(4, Math.min(selectedEnemy.getMarkerX() - 60, canvas.getWidth() - 190));
                double labelY = Math.max(32, selectedEnemy.getMarkerY() + 32);
                g.setFill(Color.YELLOW);
                g.setFont(Font.font("Monospaced", 12));
                g.fillText((stacked.indexOf(selectedEnemy) + 1) + " of " + stacked.size() + " here, click again: next",
                        labelX, labelY);
            }
        }

        for (Extermination.Enemy enemy : sandbox.getEnemies()) {
            if (enemy.isAppearing()) {
                drawAppearing(g, enemy);
            } else {
                drawEnemy(g, enemy);
                drawEnemyHealth(g, enemy);
            }
        }
        drawBossBar(g);
        // A see-through preview of the enemy a click would place
        if (!pathMode && mouseX >= 0) {
            g.setGlobalAlpha(0.35);
            drawAlien(g, placingType, mouseX, mouseY);
            g.setGlobalAlpha(1);
        }

        g.setFill(Color.GRAY);
        g.setFont(Font.font("Monospaced", 13));
        g.fillText("EDITING WAVE " + (editWave + 1) + "    " + (pathMode ? "DRAW PATHS" : "PLACE ENEMIES"), 8, 18);

        drawItems(g);
        drawEnemyBullets(g);
        drawBombs(g);
        drawDeaths(g);
        drawFlaks(g);

        drawPlayerShots(g);
        drawShip(g);
        drawRunHud(g);

        if (pickingFollow || pickingUntil || pickingFromSpot) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(Color.ORANGE);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
            g.fillText(pickingFromSpot ? "Click where the bullets should come from" : pickingUntil ? "Click the group it keeps coming until" : "Click the group it should wait for", canvas.getWidth() / 2, 90);
            g.setTextAlign(TextAlignment.LEFT);
        }
        if (paused || sandbox.isLevelClear()) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(paused ? Color.ORANGE : Color.LIMEGREEN);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
            g.fillText(paused ? "PAUSED - press Enter to play" : "LEVEL CLEAR", canvas.getWidth() / 2, 60);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /**
     * A flight path: a line from the enemy's starting spot through each waypoint, with a dot on every
     * waypoint. A looping path also has a dashed line back to the start. When several enemies share the
     * path, the count is written next to the start.
     */
    private void drawPath(GraphicsContext g, Extermination.EnemyDesign design, Color color) {
        List<double[]> points = design.getWaypoints();
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(1.5);
        double lastX = design.getStartX();
        double lastY = design.getStartY();
        // The start: a circle where it appears, or a square on the edge where it flies in
        if (design.entersFromEdge()) {
            g.strokeRect(design.getMarkerX() - 5, design.getMarkerY() - 5, 10, 10);
        } else {
            g.strokeOval(lastX - 4, lastY - 4, 8, 8);
        }
        if (design.getCount() > 1 || design.getRepeats() > 0) {
            // x5 for a group of 5, x5 x4 when that batch is sent 4 times in all
            g.fillText("x" + design.getCount() + (design.getRepeats() > 0 ? " x" + (design.getRepeats() + 1) : ""),
                    design.getMarkerX() + 8, design.getMarkerY() + 14);
        }
        drawStop(g, design.getStopTime(0), design.getMarkerX(), design.getMarkerY());
        for (int i = 0; i < points.size(); i++) {
            double[] point = points.get(i);
            g.strokeLine(lastX, lastY, point[0], point[1]);
            g.fillOval(point[0] - 2, point[1] - 2, 4, 4);
            drawStop(g, design.getStopTime(i + 1), point[0], point[1]);
            lastX = point[0];
            lastY = point[1];
        }
        if (design.isLoop() && !points.isEmpty()) {
            g.setLineDashes(6, 6);
            g.strokeLine(lastX, lastY, design.getStartX(), design.getStartY());
            g.setLineDashes((double[]) null);
        }
    }

    /** A stop on a path: a ring around the point with how many seconds the enemy waits there. */
    /**
     * The selected boss attack's own movement path, in gold: from where the boss fights (the end of
     * its entry path) through each numbered point, with a dashed line back to point 1 when it loops.
     */
    private void drawRoute(GraphicsContext g) {
        Extermination.ShotPhase attack = lead();
        if (attack == null || !selectedEnemy.isBoss() || attack.getMove() != Extermination.BossMove.PATH) {
            return;
        }
        List<double[]> points = attack.getMovePath();
        List<double[]> entry = selectedEnemy.getWaypoints();
        double fromX = entry.isEmpty() ? selectedEnemy.getStartX() : entry.get(entry.size() - 1)[0];
        double fromY = entry.isEmpty() ? selectedEnemy.getStartY() : entry.get(entry.size() - 1)[1];
        Color gold = Color.web("#ffcc33");
        g.setStroke(gold);
        g.setFill(gold);
        g.setLineWidth(1.5);
        g.setFont(Font.font("Monospaced", 11));
        g.setLineDashes(2, 4);
        if (!points.isEmpty()) {
            g.strokeLine(fromX, fromY, points.get(0)[0], points.get(0)[1]);
        }
        g.setLineDashes((double[]) null);
        for (int i = 0; i < points.size(); i++) {
            double[] point = points.get(i);
            if (i > 0) {
                g.strokeLine(points.get(i - 1)[0], points.get(i - 1)[1], point[0], point[1]);
            }
            g.fillOval(point[0] - 4, point[1] - 4, 8, 8);
            g.fillText(String.valueOf(i + 1) + firesAtText(i + 1), point[0] + 6, point[1] + 14);
            drawStop(g, point[2], point[0], point[1]);
        }
        if (attack.isMoveLoop() && !attack.isMoveBounce() && points.size() > 1) {
            g.setLineDashes(6, 5);
            double[] last = points.get(points.size() - 1);
            g.strokeLine(last[0], last[1], points.get(0)[0], points.get(0)[1]);
            g.setLineDashes((double[]) null);
        }
        if (!points.isEmpty()) {
            double[] picked = points.get(Math.min(routePoint, points.size() - 1));
            g.setStroke(Color.YELLOW);
            g.strokeOval(picked[0] - 8, picked[1] - 8, 16, 16);
        }
        if (drawingRoute) {
            g.setFill(gold);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
            g.fillText("DRAWING ATTACK " + selectedEnemy.attackNumber(selectedPhase) + "'S PATH", 8, canvas.getHeight() - 10);
        }
    }

    /** For a point on a boss attack's path: the patterns that fire only there, like "  fires pattern 1 x3, pattern 2". */
    private String firesAtText(int point) {
        int start = selectedEnemy.attackStart(selectedPhase);
        List<String> firing = new java.util.ArrayList<>();
        for (int i = start; i < selectedEnemy.attackEnd(start); i++) {
            Extermination.ShotPhase p = selectedEnemy.getPhases().get(i);
            if (p.getAtPoints().contains(point)) {
                firing.add("#" + (i + 1) + (p.getVolleys() > 0 ? " x" + p.getVolleys() : ""));
            }
        }
        return firing.isEmpty() ? "" : "  fires " + String.join(", ", firing);
    }

    private static void drawStop(GraphicsContext g, double seconds, double x, double y) {
        if (seconds <= 0) {
            return;
        }
        g.strokeOval(x - 5, y - 5, 10, 10);
        g.fillText(String.format(Locale.ROOT, "%.1fs", seconds), x + 8, y - 6);
    }

    /** Enemy bullets: round, with a white centre so they stand out from the enemies. */
    /** Each bullet color's soft glow and its body: a white-hot middle fading out to the color at the rim. */
    private static final Map<Extermination.BulletColor, javafx.scene.paint.RadialGradient> BULLET_GLOWS = new java.util.EnumMap<>(Extermination.BulletColor.class);
    private static final Map<Extermination.BulletColor, javafx.scene.paint.RadialGradient> BULLET_BODIES = new java.util.EnumMap<>(Extermination.BulletColor.class);

    static {
        for (Map.Entry<Extermination.BulletColor, Color> entry : BULLET_COLORS.entrySet()) {
            Color color = entry.getValue();
            BULLET_GLOWS.put(entry.getKey(), new javafx.scene.paint.RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, color.deriveColor(0, 1, 1, 0.55)),
                    new javafx.scene.paint.Stop(1, color.deriveColor(0, 1, 1, 0))));
            BULLET_BODIES.put(entry.getKey(), new javafx.scene.paint.RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.WHITE),
                    new javafx.scene.paint.Stop(0.45, Color.WHITE),
                    new javafx.scene.paint.Stop(0.7, color.interpolate(Color.WHITE, 0.25)),
                    new javafx.scene.paint.Stop(1, color)));
        }
    }

    /**
     * Enemy bullets, drawn like a bullet hell's: a soft glow of their color first (added on, so crowded
     * bullets light up together), then each bullet's body with a white-hot middle and a colored rim, in
     * its shape. Only the small middle can hit the ship, so the glow is free to be big.
     */
    private void drawEnemyBullets(GraphicsContext g) {
        List<Extermination.EnemyBullet> bullets = sandbox.getEnemyBullets();
        g.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
        for (Extermination.EnemyBullet bullet : bullets) {
            double r = bullet.getRadius() * 2.3;
            g.setFill(BULLET_GLOWS.get(bullet.getColor()));
            g.fillOval(bullet.getX() - r, bullet.getY() - r, r * 2, r * 2);
        }
        g.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
        for (Extermination.EnemyBullet bullet : bullets) {
            drawBullet(g, bullet);
        }
        // Lasers: a thin line while warning, then a glowing beam with a white core
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        for (Extermination.Laser laser : sandbox.getLasers()) {
            Color color = BULLET_COLORS.get(laser.getColor());
            if (laser.isWarning()) {
                g.setStroke(color.deriveColor(0, 1, 1, 0.6));
                g.setLineWidth(1);
                g.strokeLine(laser.getStartX(), laser.getStartY(), laser.getEndX(), laser.getEndY());
            } else {
                g.setStroke(color);
                g.setLineWidth(laser.getWidth());
                g.strokeLine(laser.getStartX(), laser.getStartY(), laser.getEndX(), laser.getEndY());
                g.setStroke(Color.WHITE);
                g.setLineWidth(laser.getWidth() / 3);
                g.strokeLine(laser.getStartX(), laser.getStartY(), laser.getEndX(), laser.getEndY());
            }
        }
        g.setLineWidth(1);
    }

    /**
     * The falling boxes: red P for power (bigger for a big one), blue for score, green B for a bomb. A
     * faint dashed line marks the collect line: above it, every item flies to the ship.
     */
    private void drawItems(GraphicsContext g) {
        if (screen == Screen.SANDBOX) {
            g.setStroke(Color.web("#4aa8ff", 0.25));
            g.setLineWidth(1);
            g.setLineDashes(4, 8);
            g.strokeLine(0, Extermination.COLLECT_LINE, canvas.getWidth(), Extermination.COLLECT_LINE);
            g.setLineDashes((double[]) null);
        }
        g.setTextAlign(TextAlignment.CENTER);
        for (Extermination.Item item : sandbox.getItems()) {
            javafx.scene.image.Image picture = switch (item.getType()) {
                case SMALL_POWER -> SMALL_POWER_ITEM;
                case BIG_POWER -> BIG_POWER_ITEM;
                case POINT -> POINT_ITEM;
                case BOMB -> BOMB_ITEM;
            };
            if (picture != null) {
                double pw = picture.getWidth() * ITEM_PIXEL_SCALE;
                double ph = picture.getHeight() * ITEM_PIXEL_SCALE;
                g.save();
                g.setImageSmoothing(false); // keeps pixel art sharp
                g.translate(item.getX(), item.getY());
                if (item.getType() == Extermination.ItemType.SMALL_POWER) {
                    // Each small item leans up-left or up-right, picked at random once (it stays the same as it falls)
                    g.rotate((System.identityHashCode(item) & 1) == 0 ? -SMALL_POWER_TILT : SMALL_POWER_TILT);
                }
                g.drawImage(picture, -pw / 2, -ph / 2, pw, ph);
                g.restore();
                continue;
            }
            boolean big = item.getType() == Extermination.ItemType.BIG_POWER || item.getType() == Extermination.ItemType.BOMB;
            double size = Extermination.ITEM_SIZE * (big ? 1.6 : 1);
            Color fill = switch (item.getType()) {
                case SMALL_POWER, BIG_POWER -> Color.web("#e8323c");
                case POINT -> Color.web("#3c78e8");
                case BOMB -> Color.web("#2fbf4f");
            };
            double x = item.getX() - size / 2;
            double y = item.getY() - size / 2;
            g.setFill(fill);
            g.fillRect(x, y, size, size);
            g.setStroke(Color.WHITE);
            g.setLineWidth(1);
            g.strokeRect(x + 0.5, y + 0.5, size - 1, size - 1);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, size * 0.8));
            String letter = switch (item.getType()) {
                case SMALL_POWER, BIG_POWER -> "P";
                case POINT -> "\u70b9"; // the score kanji, like the real thing
                case BOMB -> "B";
            };
            g.fillText(letter, item.getX(), item.getY() + size * 0.3);
        }
        g.setTextAlign(TextAlignment.LEFT);
    }

    /** A bomb going off: the wide beam straight up from the ship, or the barrier ring around it. */
    private void drawBombs(GraphicsContext g) {
        double x = sandbox.getPlayerX();
        double y = sandbox.getPlayerY();
        double beam = sandbox.getBeamTimeLeft();
        if (beam > 0) {
            // Fades in quickly and out at the end, and flickers a little in width
            double fade = Math.min(1, Math.min((Extermination.BEAM_TIME - beam) / 0.15, beam / 0.4));
            double wobble = 0.96 + 0.04 * Math.sin(System.nanoTime() / 4e7);
            double w = Extermination.BEAM_WIDTH * wobble;
            // The outline: thin at the ship, flaring out like a bell, then straight up to the top
            int steps = 24;
            double[] xs = new double[steps * 2 + 2];
            double[] ys = new double[steps * 2 + 2];
            for (int i = 0; i <= steps; i++) {
                double py = y - Extermination.BEAM_FLARE * i / steps;
                double half = sandbox.beamHalfWidth(py) * wobble;
                xs[i] = x - half;
                ys[i] = py;
                xs[steps * 2 + 1 - i] = x + half;
                ys[steps * 2 + 1 - i] = py;
            }
            xs[steps] = x - w / 2;
            ys[steps] = 0;
            xs[steps + 1] = x + w / 2;
            ys[steps + 1] = 0;
            g.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
            g.setGlobalAlpha(fade);
            // Brightest in the middle, fading to the edges like a bell curve
            g.setFill(new javafx.scene.paint.LinearGradient(x - w / 2, 0, x + w / 2, 0, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(0.12, Color.web("#ff60c0", 0.35)),
                    new javafx.scene.paint.Stop(0.28, Color.web("#ffb060", 0.7)),
                    new javafx.scene.paint.Stop(0.40, Color.web("#ffe070")),
                    new javafx.scene.paint.Stop(0.5, Color.WHITE),
                    new javafx.scene.paint.Stop(0.60, Color.web("#ffe070")),
                    new javafx.scene.paint.Stop(0.72, Color.web("#60e0ff", 0.7)),
                    new javafx.scene.paint.Stop(0.88, Color.web("#6080ff", 0.35)),
                    new javafx.scene.paint.Stop(1, Color.TRANSPARENT)));
            g.fillPolygon(xs, ys, xs.length);
            g.setGlobalAlpha(1);
            g.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
        }
        double r = sandbox.getBarrierRadius();
        if (r > 0) {
            g.setGlobalBlendMode(javafx.scene.effect.BlendMode.ADD);
            g.setFill(new javafx.scene.paint.RadialGradient(0, 0, x, y, r, false,
                    javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(0.75, Color.web("#40c8ff", 0.15)),
                    new javafx.scene.paint.Stop(0.95, Color.web("#a0e8ff", 0.6)),
                    new javafx.scene.paint.Stop(1, Color.TRANSPARENT)));
            g.fillOval(x - r, y - r, r * 2, r * 2);
            g.setGlobalBlendMode(javafx.scene.effect.BlendMode.SRC_OVER);
            g.setStroke(Color.web("#c8f4ff", 0.9));
            g.setLineWidth(2);
            g.strokeOval(x - r, y - r, r * 2, r * 2);
            g.setLineWidth(1);
        }
    }

    /** Score, lives, bombs and power, bottom left of the playfield (until the side panel is made). */
    private void drawRunHud(GraphicsContext g) {
        double w = canvas.getWidth();
        double left = 12;
        double top = canvas.getHeight() - 98;
        g.setFill(Color.color(0, 0, 0, 0.6));
        g.fillRoundRect(left - 6, top, 170, 92, 8, 8);
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 12));
        g.setFill(Color.WHITE);
        g.fillText(String.format("Score  %09d", sandbox.getScore()), left, top + 14);
        g.setFill(Color.web("#ff6fa8"));
        if (LIFE_ICON != null) {
            // One heart picture per life
            g.fillText("Lives", left, top + 30);
            g.setImageSmoothing(false);
            for (int i = 0; i < sandbox.getLives(); i++) {
                g.drawImage(LIFE_ICON, left + 49 + i * (LIFE_ICON.getWidth() + 1), top + 31 - LIFE_ICON.getHeight());
            }
        } else {
            g.fillText("Lives  " + "\u2665".repeat(sandbox.getLives()), left, top + 30);
        }
        g.setFill(Color.web("#4cff6a"));
        if (BOMB_ITEM != null) {
            // One bomb picture per bomb in stock
            g.fillText("Bombs", left, top + 46);
            g.setImageSmoothing(false);
            for (int i = 0; i < sandbox.getBombs(); i++) {
                g.drawImage(BOMB_ITEM, left + 49 + i * (BOMB_ITEM.getWidth() + 1), top + 47 - BOMB_ITEM.getHeight());
            }
        } else {
            g.fillText("Bombs  " + "\u2605".repeat(sandbox.getBombs()), left, top + 46);
        }
        // Power: a bar that fills to MAX_POWER, with ticks where each power level starts
        g.setFill(Color.web("#ff9a2e"));
        g.fillText(String.format("Power  %3.0f/%.0f", sandbox.getPower(), Extermination.MAX_POWER), left, top + 62);
        double barW = 156;
        g.setFill(Color.web("#333333"));
        g.fillRect(left, top + 66, barW, 5);
        g.setFill(Color.web("#e8323c"));
        g.fillRect(left, top + 66, barW * sandbox.getPower() / Extermination.MAX_POWER, 5);
        g.setFill(Color.WHITE);
        for (double[] level : Extermination.POWER_LEVELS) {
            if (level[0] > 0 && level[0] < Extermination.MAX_POWER) {
                g.fillRect(left + barW * level[0] / Extermination.MAX_POWER, top + 65, 1, 7);
            }
        }
        g.setFill(Color.web("#b0b0b0"));
        g.fillText("Graze  " + sandbox.getGrazes(), left, top + 86);
        if (sandbox.isOutOfLives() && screen == Screen.SANDBOX) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFill(Color.RED);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 16));
            g.fillText("OUT OF LIVES (the sandbox keeps going, R restarts)", w / 2, canvas.getHeight() / 2);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /** One bullet's body in its shape, pointing the way it flies. */
    private void drawBullet(GraphicsContext g, Extermination.EnemyBullet bullet) {
        double r = bullet.getRadius();
        double x = bullet.getX();
        double y = bullet.getY();
        Color color = BULLET_COLORS.get(bullet.getColor());
        switch (bullet.getShape()) {
            case ORB -> {
                g.setFill(BULLET_BODIES.get(bullet.getColor()));
                g.fillOval(x - r * 1.15, y - r * 1.15, r * 2.3, r * 2.3);
            }
            case RICE, KNIFE, STAR -> {
                g.save();
                g.translate(x, y);
                if (bullet.getShape() == Extermination.BulletShape.STAR) {
                    g.rotate(bullet.getAge() * 240); // a star spins as it flies
                    double[] xs = new double[10];
                    double[] ys = new double[10];
                    for (int i = 0; i < 10; i++) {
                        double a = -Math.PI / 2 + i * Math.PI / 5;
                        double len = i % 2 == 0 ? r * 1.6 : r * 0.7;
                        xs[i] = Math.cos(a) * len;
                        ys[i] = Math.sin(a) * len;
                    }
                    g.setFill(color);
                    g.fillPolygon(xs, ys, 10);
                    g.setFill(Color.WHITE);
                    g.fillOval(-r * 0.45, -r * 0.45, r * 0.9, r * 0.9);
                } else {
                    g.rotate(Math.toDegrees(bullet.getAngle()));
                    if (bullet.getShape() == Extermination.BulletShape.RICE) {
                        // A long grain along the way it flies
                        g.setFill(color);
                        g.fillOval(-r * 1.7, -r * 0.8, r * 3.4, r * 1.6);
                        g.setFill(Color.WHITE);
                        g.fillOval(-r * 1.2, -r * 0.4, r * 2.4, r * 0.8);
                    } else {
                        // A knife: a sharp point in front, a short handle behind
                        g.setFill(color);
                        g.fillPolygon(new double[]{r * 2, -r * 0.6, -r * 1.4, -r * 0.6},
                                new double[]{0, -r * 0.8, 0, r * 0.8}, 4);
                        g.setFill(Color.WHITE);
                        g.fillPolygon(new double[]{r * 1.5, -r * 0.4, -r * 0.4},
                                new double[]{0, -r * 0.35, r * 0.35}, 3);
                    }
                }
                g.restore();
            }
        }
    }

    /**
     * A tank (more than 1 health) gets a small health bar above it, and any enemy that's just been hit
     * flashes white so each hit can be seen.
     */
    private void drawEnemyHealth(GraphicsContext g, Extermination.Enemy enemy) {
        double scale = enemy.getDesign().getScale();
        double left = enemy.getX() - Alien.WIDTH * scale / 2;
        double top = enemy.getY() - Alien.HEIGHT * scale / 2;
        if (enemy.isFlashing()) {
            g.setGlobalAlpha(0.7);
            g.setFill(Color.WHITE);
            g.fillRect(left, top, Alien.WIDTH * scale, Alien.HEIGHT * scale);
            g.setGlobalAlpha(1);
        }
        int full = enemy.getDesign().getHealth();
        if (full > 1 && !enemy.getDesign().isBoss()) {
            double fraction = Math.max(0, (double) enemy.getHealthLeft() / full);
            g.setFill(Color.web("#550000"));
            g.fillRect(left, top - 7, Alien.WIDTH, 4);
            g.setFill(fraction > 0.5 ? Color.LIMEGREEN : fraction > 0.25 ? Color.ORANGE : Color.RED);
            g.fillRect(left, top - 7, Alien.WIDTH * fraction, 4);
        }
    }

    /**
     * The boss's bar along the top: the current attack's health, how many attacks are left (stars), the
     * time left, and the spell card's name under it when the attack has one.
     */
    private void drawBossBar(GraphicsContext g) {
        Extermination.Enemy boss = sandbox.getBoss();
        if (boss == null || !boss.isFighting() || boss.isLeaving()) {
            return;
        }
        Extermination.ShotPhase attack = boss.getPhase();
        Extermination.EnemyDesign design = boss.getDesign();
        int left = design.attackCount() - design.attackNumber(boss.getPhaseIndex()); // attacks after this one
        double width = canvas.getWidth();
        double barLeft = 8 + left * 12 + (left > 0 ? 4 : 0);
        double barRight = width - 52;
        double fraction = boss.isBetweenAttacks() ? 1 : Math.max(0, (double) boss.getAttackHealthLeft() / attack.getCardHealth());
        g.setFill(Color.web("#330000"));
        g.fillRect(barLeft, 26, barRight - barLeft, 6);
        g.setFill(boss.isBetweenAttacks() ? Color.GRAY : Color.web("#ff5a5a"));
        g.fillRect(barLeft, 26, (barRight - barLeft) * fraction, 6);
        // A star for each attack still to come
        g.setFill(Color.GOLD);
        for (int i = 0; i < left; i++) {
            double cx = 13 + i * 12;
            g.fillPolygon(new double[]{cx, cx + 2, cx + 6, cx + 3, cx + 4, cx, cx - 4, cx - 3, cx - 6, cx - 2},
                    new double[]{23, 27, 27, 30, 35, 32, 35, 30, 27, 27}, 10);
        }
        double timeLeft = boss.getAttackTimeLeft();
        if (timeLeft >= 0) {
            g.setTextAlign(TextAlignment.RIGHT);
            g.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
            g.setFill(timeLeft < 5 ? Color.SALMON : Color.WHITE);
            g.fillText(String.format(Locale.ROOT, "%.1f", timeLeft), width - 6, 34);
            g.setTextAlign(TextAlignment.LEFT);
        }
        String name = attack.getCardName();
        if (!name.isEmpty() && !boss.isBetweenAttacks()) {
            g.setTextAlign(TextAlignment.RIGHT);
            g.setFont(Font.font("Serif", FontWeight.BOLD, 15));
            g.setFill(Color.web("#000000", 0.6));
            g.fillText(name, width - 7, 53);
            g.setFill(Color.web("#ffe08a"));
            g.fillText(name, width - 8, 52);
            g.setStroke(Color.web("#ffe08a", 0.6));
            g.setLineWidth(1);
            g.strokeLine(width - 8 - Math.min(width - 16, name.length() * 8.5), 56, width - 8, 56);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    /** An enemy appearing out of nowhere: it fades in inside a shrinking white ring. */
    private void drawAppearing(GraphicsContext g, Extermination.Enemy enemy) {
        double progress = enemy.getAppearProgress();
        double scale = enemy.getDesign().getScale();
        g.setGlobalAlpha(progress);
        drawEnemy(g, enemy);
        g.setGlobalAlpha(1 - progress);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        double r = (10 + 25 * (1 - progress)) * scale;
        g.strokeOval(enemy.getX() - r, enemy.getY() - r, r * 2, r * 2);
        g.setGlobalAlpha(1);
    }

    /** Placeholder ship (a simple arrowhead) until there's a sprite. Focus shows the tiny hitbox. */
    /** Enemy deaths: the death animation's frames, one after another, centered where the enemy died. */
    private void drawDeaths(GraphicsContext g) {
        g.save();
        g.setImageSmoothing(false);
        for (double[] death : sandbox.getDeaths()) {
            int frame = (int) Math.min(death[2] * ENEMY_DEATH_FRAMES.length, ENEMY_DEATH_FRAMES.length - 1);
            javafx.scene.image.Image img = ENEMY_DEATH_FRAMES[frame];
            if (img == null) continue;
            double w = img.getWidth() * ENEMY_DEATH_PIXEL_SCALE * death[3];
            double h = img.getHeight() * ENEMY_DEATH_PIXEL_SCALE * death[3];
            g.drawImage(img, death[0] - w / 2, death[1] - h / 2, w, h);
        }
        g.restore();
    }

    /** Flak explosions: the explosion's frames, one after another, centered where the rocket's kill was. */
    private void drawFlaks(GraphicsContext g) {
        g.save();
        g.setImageSmoothing(false);
        for (double[] flak : sandbox.getFlaks()) {
            int frame = (int) Math.min(flak[2] * FLAK_FRAMES.length, FLAK_FRAMES.length - 1);
            javafx.scene.image.Image img = FLAK_FRAMES[frame];
            if (img == null) continue;
            double w = img.getWidth() * FLAK_PIXEL_SCALE;
            double h = img.getHeight() * FLAK_PIXEL_SCALE;
            g.drawImage(img, flak[0] - w / 2, flak[1] - h / 2, w, h);
        }
        g.restore();
    }

    /** The ship's shots: bullet pictures, rockets at full power, or plain bars if a picture is missing. */
    private void drawPlayerShots(GraphicsContext g) {
        g.save();
        g.setImageSmoothing(false); // keeps pixel art sharp
        g.setFill(SHIP_SHOT);
        for (double[] shot : sandbox.getShots()) {
            boolean rocket = shot.length > 4 && shot[4] == 1;
            int stage = shot.length > 5 ? (int) Math.min(shot[5], PLAYER_BULLETS.length - 1) : 0;
            javafx.scene.image.Image img = rocket ? PLAYER_ROCKET : PLAYER_BULLETS[stage];
            double[] size = rocket ? PLAYER_ROCKET_SIZE : PLAYER_BULLET_SIZE;
            if (img == null) {
                g.fillRect(shot[0] - Extermination.SHOT_WIDTH / 2, shot[1] - Extermination.SHOT_HEIGHT / 2,
                        Extermination.SHOT_WIDTH, Extermination.SHOT_HEIGHT);
                continue;
            }
            g.save();
            g.translate(shot[0], shot[1]);
            g.rotate(Math.toDegrees(Math.atan2(shot[2], -shot[3]))); // points the way it flies
            g.drawImage(img, -size[0] / 2, -size[1] / 2, size[0], size[1]);
            g.restore();
        }
        g.restore();
    }

    private void drawShip(GraphicsContext g) {
        double x = sandbox.getPlayerX();
        double y = sandbox.getPlayerY();
        double w = Extermination.PLAYER_WIDTH;
        double h = Extermination.PLAYER_HEIGHT;
        // Leans the way it's flying, easing in and out
        boolean flying = screen == Screen.RUN ? !runPaused : screen == Screen.SANDBOX && !paused;
        double target = flying ? axis(KeyCode.LEFT, KeyCode.RIGHT) : 0;
        shipLean += (target - shipLean) * 0.2;
        // Blinks see-through while it can't be hit
        boolean blink = sandbox.isInvincible() && (System.nanoTime() / 80_000_000) % 2 == 0;
        if (PLAYER_SHIP != null) {
            g.save();
            g.setGlobalAlpha(blink ? 0.35 : 1);
            g.setImageSmoothing(false); // keeps pixel art sharp
            g.translate(x, y);
            g.rotate(shipLean * SHIP_LEAN_ANGLE);
            g.scale(1 - SHIP_LEAN_SQUASH * Math.abs(shipLean), 1);
            double iw = PLAYER_SHIP.getWidth();
            double ih = PLAYER_SHIP.getHeight();
            // Engine flames go under the ship; the two engines are a couple of pictures apart so they don't flicker together
            long frame = System.nanoTime() / 1_000_000 / EXHAUST_FRAME_MS;
            for (int e = 0; e < EXHAUST_X.length; e++) {
                javafx.scene.image.Image flame = EXHAUST[(int) ((frame + e * 2) % EXHAUST.length)];
                if (flame != null) {
                    g.drawImage(flame, EXHAUST_X[e] - flame.getWidth() / 2 - iw / 2, EXHAUST_Y - ih / 2,
                            flame.getWidth(), flame.getHeight());
                }
            }
            g.drawImage(PLAYER_SHIP, -iw / 2, -ih / 2, iw, ih);
            g.restore();
        } else {
            g.setFill(blink ? Color.RED : SHIP);
            g.fillPolygon(new double[]{x, x + w / 2, x, x - w / 2},
                    new double[]{y - h / 2, y + h / 2, y + h / 4, y + h / 2}, 4);
        }
        if (sandbox.isFocused()) {
            // The graze zone: bullets that pass through this ring without touching the dot score points
            double gr = Extermination.GRAZE_RADIUS;
            g.setStroke(Color.web("#ffffff", 0.25));
            g.setLineWidth(1);
            g.strokeOval(x - gr, y - gr, gr * 2, gr * 2);
            // The hitbox: a white dot with a red ring, so it shows up on any ship color
            double r = Extermination.HITBOX_RADIUS;
            g.setFill(HITBOX);
            g.fillOval(x - r - 1.5, y - r - 1.5, r * 2 + 3, r * 2 + 3);
            g.setFill(Color.WHITE);
            g.fillOval(x - r, y - r, r * 2, r * 2);
        }
    }

    private void drawGameOver(GraphicsContext g) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web("#000000", 0.55));
        g.fillRect(0, 0, w, h);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.RED);
        g.setFont(Font.font("Monospaced", FontWeight.BOLD, 40));
        g.fillText("GAME OVER", w / 2, h / 2 - 20);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Monospaced", 18));
        g.fillText("Score: " + game.getScore() + "\nPress Enter to play again", w / 2, h / 2 + 20);
        g.setTextAlign(TextAlignment.LEFT);
    }

    private void drawHud() {
        highScore = Math.max(highScore, game.getScore());
        scoreLabel.setText(String.format("Score: %05d", game.getScore()));
        bestLabel.setText(String.format("Best: %05d", highScore));
        livesLabel.setText("Lives: " + game.getLives());
        waveLabel.setText("Wave: " + game.getWave());
    }

    private void drawPlayer(GraphicsContext g) {
        double x = game.getPlayerX();
        double y = SpaceInvadersMechanics.PLAYER_Y;
        if (game.isRespawning()) {
            // A simple burst of debris where the cannon was hit, until the respawn pause ends
            g.setFill(Color.WHITE);
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                g.fillRect(x + Math.cos(angle) * 12 - 2, y + Math.sin(angle) * 6 - 2, 4, 4);
            }
            return;
        }
        g.setFill(CANNON);
        drawCannon(g, x, y);
    }

    /** A simple laser cannon: a wide base with a small turret on top. */
    private void drawCannon(GraphicsContext g, double x, double y) {
        double w = SpaceInvadersMechanics.PLAYER_WIDTH;
        double h = SpaceInvadersMechanics.PLAYER_HEIGHT;
        g.fillRect(x - w / 2, y - h / 2 + 6, w, h - 6);
        g.fillRect(x - w / 2 + 6, y - h / 2 + 3, w - 12, 3);
        g.fillRect(x - 2, y - h / 2 - 2, 4, 5);
    }

    private void drawAliens(GraphicsContext g) {
        for (Alien alien : game.getAliens()) {
            drawAlien(g, alien.getType(), alien.getX(), alien.getY());
        }
    }

    /** A flying enemy: its alien picture at its size, turned to point along its path if it's set to. */
    private void drawEnemy(GraphicsContext g, Extermination.Enemy enemy) {
        double angle = enemy.getPictureAngle();
        if (angle == 0) {
            drawAlien(g, enemy.getType(), enemy.getX(), enemy.getY(), enemy.getDesign().getScale());
            return;
        }
        g.save();
        g.translate(enemy.getX(), enemy.getY());
        g.rotate(angle);
        drawAlien(g, enemy.getType(), 0, 0, enemy.getDesign().getScale());
        g.restore();
    }

    /** An alien drawn the given number of times bigger (a boss is twice the size). */
    private void drawAlien(GraphicsContext g, Alien.Type type, double x, double y, double scale) {
        if (scale == 1) {
            drawAlien(g, type, x, y);
            return;
        }
        g.save();
        g.translate(x, y);
        g.scale(scale, scale);
        drawAlien(g, type, 0, 0);
        g.restore();
    }

    /** An alien's picture, swapping between its two frames; plain shapes if a picture is missing. */
    private void drawAlien(GraphicsContext g, Alien.Type type, double x, double y) {
        javafx.scene.image.Image[] frames = switch (type) {
            case SQUID -> SQUID_FRAMES;
            case CRAB -> CRAB_FRAMES;
            case OCTOPUS -> OCTOPUS_FRAMES;
        };
        javafx.scene.image.Image frame = frames[(int) (System.nanoTime() / 1_000_000 / ALIEN_FRAME_MS % frames.length)];
        if (frame != null) {
            double iw = frame.getWidth() * ALIEN_PIXEL_SCALE;
            double ih = frame.getHeight() * ALIEN_PIXEL_SCALE;
            boolean smooth = g.isImageSmoothing();
            g.setImageSmoothing(false); // keeps pixel art sharp
            g.drawImage(frame, x - iw / 2, y - ih / 2, iw, ih);
            g.setImageSmoothing(smooth);
            return;
        }
        double left = x - Alien.WIDTH / 2;
        double top = y - Alien.HEIGHT / 2;
        switch (type) {
            case SQUID -> {
                g.setFill(SQUID);
                g.fillRect(left + 9, top, 10, 4);
                g.fillRect(left + 5, top + 4, 18, 8);
                g.fillRect(left + 7, top + 12, 4, 6);
                g.fillRect(left + 17, top + 12, 4, 6);
                g.fillRect(left + 12, top + 12, 4, 4);
                g.setFill(Color.BLACK);
                g.fillRect(left + 9, top + 6, 3, 3);
                g.fillRect(left + 16, top + 6, 3, 3);
            }
            case CRAB -> {
                g.setFill(CRAB);
                g.fillRect(left + 4, top + 4, 20, 10);
                g.fillRect(left + 7, top, 3, 4);
                g.fillRect(left + 18, top, 3, 4);
                g.fillRect(left + 2, top + 10, 4, 6);
                g.fillRect(left + 22, top + 10, 4, 6);
                g.fillRect(left + 6, top + 14, 4, 4);
                g.fillRect(left + 18, top + 14, 4, 4);
                g.setFill(Color.BLACK);
                g.fillRect(left + 9, top + 7, 3, 3);
                g.fillRect(left + 16, top + 7, 3, 3);
            }
            case OCTOPUS -> {
                g.setFill(OCTOPUS);
                g.fillRect(left + 2, top + 2, 24, 10);
                g.fillRect(left + 8, top, 12, 2);
                g.fillRect(left + 4, top + 12, 5, 6);
                g.fillRect(left + 19, top + 12, 5, 6);
                g.fillRect(left + 11, top + 14, 6, 4);
                g.setFill(Color.BLACK);
                g.fillRect(left + 8, top + 5, 4, 3);
                g.fillRect(left + 16, top + 5, 4, 3);
            }
        }
    }

    private void drawMystery(GraphicsContext g) {
        if (game.isMysteryActive()) {
            drawMysteryShape(g, game.getMysteryX(), SpaceInvadersMechanics.MYSTERY_Y);
        }
    }

    private void drawMysteryShape(GraphicsContext g, double x, double y) {
        double w = SpaceInvadersMechanics.MYSTERY_WIDTH;
        double h = SpaceInvadersMechanics.MYSTERY_HEIGHT;
        // A saucer: a wide body, a dome on top and a row of lights
        g.setFill(MYSTERY);
        g.fillOval(x - w / 2, y - h / 4, w, h * 0.6);
        g.fillOval(x - w / 4, y - h / 2, w / 2, h * 0.6);
        g.setFill(Color.BLACK);
        for (int i = -1; i <= 1; i++) {
            g.fillRect(x + i * 10 - 2, y, 4, 3);
        }
    }

    private void drawShot(GraphicsContext g) {
        Bullet shot = game.getPlayerShot();
        if (shot != null) {
            g.setFill(Color.WHITE);
            g.fillRect(shot.getLeft(), shot.getTop(), Bullet.WIDTH, Bullet.HEIGHT);
        }
        // Alien shots zigzag so they read differently from the player's
        g.setStroke(ALIEN_SHOT);
        g.setLineWidth(2);
        for (Bullet alienShot : game.getAlienShots()) {
            double x = alienShot.getX();
            double top = alienShot.getTop();
            double third = Bullet.HEIGHT / 3;
            g.strokePolyline(new double[]{x - 2, x + 2, x - 2, x + 2},
                    new double[]{top, top + third, top + 2 * third, top + Bullet.HEIGHT}, 4);
        }
    }

    private void drawShields(GraphicsContext g) {
        g.setFill(SHIELD);
        double b = Shield.BLOCK_SIZE;
        for (Shield shield : game.getShields()) {
            for (int row = 0; row < Shield.ROWS; row++) {
                for (int col = 0; col < Shield.COLUMNS; col++) {
                    if (shield.isBlock(row, col)) {
                        g.fillRect(shield.getLeft() + col * b, shield.getTop() + row * b, b, b);
                    }
                }
            }
        }
    }
}
