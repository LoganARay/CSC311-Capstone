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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
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
    private enum Screen { MENU, MODE_SELECT, INTRO, PLAYING, SANDBOX }

    private static final String GAME_CONTROLS = "Left/Right or A/D: move    Space: fire    Enter: play again    Esc: menu";
    private static final String SANDBOX_CONTROLS =
            "SANDBOX (developer)    Enter: play/pause    Arrows: fly    Shift: focus    Space: fire    Esc: menu";
    /** What the mouse does with each editor tool, shown under the tool buttons. */
    private static final String PLACE_TOOL_HINT = """
            Click: place (at an edge it flies in)
            Click an enemy or a path dot: select it
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
            Extermination.Pattern.RANDOM, "Bullets sprayed at random toward the player.");
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
    private static final Color ENEMY_BULLET = Color.web("#ff4fa3");

    /** Best score for as long as the app is open. */
    private static int highScore = 0;

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
    @FXML private Label speedLabel;
    @FXML private Spinner<Double> speedSpinner;
    @FXML private Label countLabel;
    @FXML private Spinner<Double> countSpinner;
    @FXML private Label spawnLabel;
    @FXML private Spinner<Double> spawnSpinner;
    @FXML private CheckBox loopCheck;
    @FXML private Label stopLabel;
    @FXML private Spinner<Double> stopSpinner;
    @FXML private CheckBox fireStoppedCheck;
    @FXML private Label levelInfo;
    @FXML private Label saveMessage;
    @FXML private ToggleButton patternNone;
    @FXML private ToggleButton patternAimed;
    @FXML private ToggleButton patternFan;
    @FXML private ToggleButton patternRing;
    @FXML private ToggleButton patternSpiral;
    @FXML private ToggleButton patternRandom;
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
    /** How close (pixels) a click must be to a path dot to select it. */
    private static final double POINT_PICK_RADIUS = 8;
    /** The wave being edited: new enemies go in it, and only its enemies are shown and picked. */
    private int editWave = 0;
    /** While paused the sandbox stands still, so enemies and paths can be set up calmly. */
    private boolean paused = true;
    /** True while the mouse button is held down drawing the selected enemy's path. */
    private boolean drawingPath = false;
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
                case SANDBOX -> {
                    if (!paused) {
                        boolean fire = heldKeys.contains(KeyCode.SPACE) && !waitForSpaceRelease;
                        sandbox.update(deltaSeconds, axis(KeyCode.LEFT, KeyCode.RIGHT), axis(KeyCode.UP, KeyCode.DOWN),
                                heldKeys.contains(KeyCode.SHIFT), fire);
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
        canvas.setOnMouseReleased(e -> drawingPath = false);
        canvas.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });
        canvas.setOnMouseExited(e -> mouseX = -1);

        // Number boxes: type a value (Enter to confirm, Esc to cancel) or use the arrows.
        // A change applies to the selected enemy, or to the wave being edited, right away.
        setUpNumberBox(speedSpinner, Extermination.MIN_ENEMY_SPEED, Extermination.MAX_ENEMY_SPEED, 10, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpeed(v); });
        setUpNumberBox(countSpinner, 1, Extermination.MAX_COUNT, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setCount((int) Math.round(v)); });
        setUpNumberBox(spawnSpinner, 0, Extermination.MAX_SPAWN_TIME, 0.5, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpawnTime(v); });
        setUpNumberBox(waitSpinner, 0, Extermination.MAX_WAVE_WAIT, 0.5, 1,
                v -> { if (sandbox != null) sandbox.getWaves().get(editWave).setWait(v); });
        setUpNumberBox(bulletCountSpinner, 1, Extermination.MAX_BULLETS_PER_SHOT, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setBulletCount((int) Math.round(v)); });
        setUpNumberBox(spreadSpinner, 0, 360, 5, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpread(v); });
        setUpNumberBox(bulletSpeedSpinner, Extermination.MIN_BULLET_SPEED, Extermination.MAX_BULLET_SPEED, 10, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setBulletSpeed(v); });
        setUpNumberBox(intervalSpinner, Extermination.MIN_FIRE_INTERVAL, Extermination.MAX_FIRE_INTERVAL, 0.05, 2,
                v -> { if (selectedEnemy != null) selectedEnemy.setFireInterval(v); });
        setUpNumberBox(firstShotSpinner, 0, Extermination.MAX_FIRST_SHOT_TIME, 0.1, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setFirstShotDelay(v); });
        setUpNumberBox(spinSpinner, -90, 90, 1, 0,
                v -> { if (selectedEnemy != null) selectedEnemy.setSpin(v); });
        setUpNumberBox(stopSpinner, 0, Extermination.MAX_STOP_TIME, 0.5, 1,
                v -> { if (selectedEnemy != null) selectedEnemy.setStopTime(selectedPoint, v); });

        showMenu();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // Filters (not handlers) so the Main Menu button can't swallow the arrow keys.
        // Movement needs to know which keys are held down, so both presses and releases are tracked.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
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
            if (e.getCode() == KeyCode.SPACE) {
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
                if (selectedEnemy != null) selectedEnemy.nextPattern();
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
            case ESCAPE -> showMenu();
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
        if (!pathMode) {
            if (e.getButton() == MouseButton.PRIMARY) {
                // A dot on the selected enemy's path: select that point, to give it a stop
                int point = selectedEnemy == null ? -1 : selectedEnemy.findWaypointNear(x, y, POINT_PICK_RADIUS);
                if (point > 0) {
                    selectedPoint = point;
                    return;
                }
                Extermination.EnemyDesign clicked = sandbox.findEnemyNear(x, y, editWave);
                selectedEnemy = clicked != null ? clicked : sandbox.placeEnemy(placingType, x, y, editWave);
                pointOwner = selectedEnemy;
                selectedPoint = 0;
            } else if (e.getButton() == MouseButton.SECONDARY
                    && sandbox.removeEnemyNear(x, y, editWave) == selectedEnemy) {
                selectedEnemy = null;
            }
            return;
        }
        if (e.getButton() == MouseButton.PRIMARY) {
            Extermination.EnemyDesign clicked = sandbox.findEnemyNear(x, y, editWave);
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
            selectedEnemy.setPattern(Extermination.Pattern.valueOf(name));
        }
    }

    @FXML
    private void onLoopToggled() {
        if (selectedEnemy != null) selectedEnemy.setLoop(loopCheck.isSelected());
    }

    @FXML
    private void onFireStoppedToggled() {
        if (selectedEnemy != null) selectedEnemy.setFireOnlyWhenStopped(fireStoppedCheck.isSelected());
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
            selectedTitle.setText("3. SELECTED ENEMY: none\n(click an enemy to change it)");
        } else {
            selectedTitle.setText("3. SELECTED ENEMY: " + e.getType() + "\n(" + e.getWaypoints().size()
                    + " path points, " + (e.entersFromEdge() ? "flies in from the edge)" : "appears on screen)"));
            showNumber(speedSpinner, e.getSpeed());
            showNumber(countSpinner, e.getCount());
            showNumber(spawnSpinner, e.getSpawnTime());
            loopCheck.setSelected(e.isLoop());
            if (pointOwner != e) {
                pointOwner = e;
                selectedPoint = 0;
            }
            selectedPoint = Math.min(selectedPoint, e.getWaypoints().size()); // in case points were undone
            stopLabel.setText(selectedPoint == 0 ? "Stop at start, s" : "Stop at point " + selectedPoint + ", s");
            showNumber(stopSpinner, e.getStopTime(selectedPoint));
            fireStoppedCheck.setSelected(e.isFireOnlyWhenStopped());
            showNumber(bulletCountSpinner, e.getBulletCount());
            showNumber(spreadSpinner, e.getSpread());
            showNumber(bulletSpeedSpinner, e.getBulletSpeed());
            showNumber(intervalSpinner, e.getFireInterval());
            showNumber(firstShotSpinner, e.getFirstShotDelay());
            showNumber(spinSpinner, e.getSpin());
        }
        refreshBulletSettings(e);
        // The keys in brackets change the same setting from the keyboard
        speedLabel.setText("Speed, px/s  (- +)");
        countLabel.setText("How many  ([ ])");
        spawnLabel.setText("Spawns, s into wave  (, .)");
        int running = sandbox.getRunningWave();
        String runState = sandbox.isLevelClear() ? "Level clear!"
                : running < 0 ? "Waiting for wave 1"
                : String.format("Running wave %d, %.1fs in", running + 1, sandbox.getTimeInWave(running));
        levelInfo.setText(runState + String.format("   Flying: %d   Hits: %d",
                sandbox.getEnemies().size(), sandbox.getHits()));
        refreshingPanel = false;
    }

    /** The Bullets tab: which pattern is picked, what it does, and only the boxes that pattern uses. */
    private void refreshBulletSettings(Extermination.EnemyDesign e) {
        Extermination.Pattern pattern = e == null ? Extermination.Pattern.NONE : e.getPattern();
        ToggleButton[] buttons = {patternNone, patternAimed, patternFan, patternRing, patternSpiral, patternRandom};
        for (ToggleButton button : buttons) {
            showPicked(button, e != null && pattern.name().equals(button.getUserData()));
        }
        patternHint.setText(PATTERN_HINTS.get(pattern));
        boolean shoots = pattern != Extermination.Pattern.NONE;
        boolean usesSpread = pattern == Extermination.Pattern.FAN || pattern == Extermination.Pattern.RANDOM;
        boolean usesSpin = pattern == Extermination.Pattern.SPIRAL;
        bulletCountSpinner.setDisable(!shoots);
        bulletSpeedSpinner.setDisable(!shoots);
        intervalSpinner.setDisable(!shoots);
        firstShotSpinner.setDisable(!shoots);
        spreadSpinner.setDisable(!usesSpread);
        spinSpinner.setDisable(!usesSpin);
        bulletCountLabel.setText("Bullets per shot");
        spreadLabel.setText(usesSpread ? "Spread, degrees" : "Spread (fan, random)");
        bulletSpeedLabel.setText("Bullet speed, px/s");
        intervalLabel.setText("Fires every, seconds");
        firstShotLabel.setText("1st shot, s after it shows");
        spinLabel.setText(usesSpin ? "Spin, degrees per shot" : "Spin (spiral only)");
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
        for (Spinner<Double> box : List.of(speedSpinner, countSpinner, spawnSpinner, waitSpinner, bulletCountSpinner,
                spreadSpinner, bulletSpeedSpinner, intervalSpinner, firstShotSpinner, spinSpinner, stopSpinner)) {
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
                    case BACK_TO_MAIN_MENU -> onMainMenu();
                    default -> { } // the scoreboard isn't built yet
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
                    case MODE_BACK -> showMenu();
                    default -> { } // Extermination isn't built yet
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
            g.setFill(i == SCOREBOARD ? OPTION_UNAVAILABLE : i == SANDBOX ? OPTION_DEVELOPER : OPTION);
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
            g.setFill(i == EXTERMINATION ? OPTION_UNAVAILABLE : OPTION);
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
            Map.entry('?', new String[]{" ### ", "#   #", "    #", "   # ", "  #  ", "     ", "  #  "})
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
            drawAlien(g, design.getType(), design.getMarkerX(), design.getMarkerY());
            g.setGlobalAlpha(1);
        }
        if (selectedEnemy != null) {
            drawPath(g, selectedEnemy, PATH_SELECTED);
            // The point whose stop is being edited
            int shown = Math.min(selectedPoint, selectedEnemy.getWaypoints().size());
            double[] point = shown == 0
                    ? new double[]{selectedEnemy.getMarkerX(), selectedEnemy.getMarkerY()}
                    : selectedEnemy.getWaypoints().get(shown - 1);
            g.setStroke(Color.YELLOW);
            g.setLineWidth(1.5);
            g.strokeOval(point[0] - 7, point[1] - 7, 14, 14);
        }

        for (Extermination.Enemy enemy : sandbox.getEnemies()) {
            if (enemy.isAppearing()) {
                drawAppearing(g, enemy);
            } else {
                drawAlien(g, enemy.getType(), enemy.getX(), enemy.getY());
            }
        }
        // A see-through preview of the enemy a click would place
        if (!pathMode && mouseX >= 0) {
            g.setGlobalAlpha(0.35);
            drawAlien(g, placingType, mouseX, mouseY);
            g.setGlobalAlpha(1);
        }

        g.setFill(Color.GRAY);
        g.setFont(Font.font("Monospaced", 13));
        g.fillText("EDITING WAVE " + (editWave + 1) + "    " + (pathMode ? "DRAW PATHS" : "PLACE ENEMIES"), 8, 18);

        drawEnemyBullets(g);

        g.setFill(SHIP_SHOT);
        for (double[] shot : sandbox.getShots()) {
            g.fillRect(shot[0] - Extermination.SHOT_WIDTH / 2, shot[1] - Extermination.SHOT_HEIGHT / 2,
                    Extermination.SHOT_WIDTH, Extermination.SHOT_HEIGHT);
        }
        drawShip(g);

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
        if (design.getCount() > 1) {
            g.fillText("x" + design.getCount(), design.getMarkerX() + 8, design.getMarkerY() + 14);
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
    private static void drawStop(GraphicsContext g, double seconds, double x, double y) {
        if (seconds <= 0) {
            return;
        }
        g.strokeOval(x - 5, y - 5, 10, 10);
        g.fillText(String.format(Locale.ROOT, "%.1fs", seconds), x + 8, y - 6);
    }

    /** Enemy bullets: round, with a white centre so they stand out from the enemies. */
    private void drawEnemyBullets(GraphicsContext g) {
        double r = Extermination.BULLET_RADIUS;
        g.setFill(ENEMY_BULLET);
        for (double[] bullet : sandbox.getEnemyBullets()) {
            g.fillOval(bullet[0] - r, bullet[1] - r, r * 2, r * 2);
        }
        g.setFill(Color.WHITE);
        for (double[] bullet : sandbox.getEnemyBullets()) {
            g.fillOval(bullet[0] - r / 2, bullet[1] - r / 2, r, r);
        }
    }

    /** An enemy appearing out of nowhere: it fades in inside a shrinking white ring. */
    private void drawAppearing(GraphicsContext g, Extermination.Enemy enemy) {
        double progress = enemy.getAppearProgress();
        g.setGlobalAlpha(progress);
        drawAlien(g, enemy.getType(), enemy.getX(), enemy.getY());
        g.setGlobalAlpha(1 - progress);
        g.setStroke(Color.WHITE);
        g.setLineWidth(2);
        double r = 10 + 25 * (1 - progress);
        g.strokeOval(enemy.getX() - r, enemy.getY() - r, r * 2, r * 2);
        g.setGlobalAlpha(1);
    }

    /** Placeholder ship (a simple arrowhead) until there's a sprite. Focus shows the tiny hitbox. */
    private void drawShip(GraphicsContext g) {
        double x = sandbox.getPlayerX();
        double y = sandbox.getPlayerY();
        double w = Extermination.PLAYER_WIDTH;
        double h = Extermination.PLAYER_HEIGHT;
        // Blinks red for a moment after a hit
        boolean blink = sandbox.isInvincible() && (System.nanoTime() / 80_000_000) % 2 == 0;
        g.setFill(blink ? Color.RED : SHIP);
        g.fillPolygon(new double[]{x, x + w / 2, x, x - w / 2},
                new double[]{y - h / 2, y + h / 2, y + h / 4, y + h / 2}, 4);
        if (sandbox.isFocused()) {
            double r = Extermination.HITBOX_RADIUS;
            g.setFill(HITBOX);
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

    /** Placeholder alien shapes drawn from rectangles until there's real art. */
    private void drawAlien(GraphicsContext g, Alien.Type type, double x, double y) {
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
