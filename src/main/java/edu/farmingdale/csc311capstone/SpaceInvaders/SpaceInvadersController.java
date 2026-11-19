package edu.farmingdale.csc311capstone.SpaceInvaders;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Alien;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Bullet;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Shield;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Connects spaceinvaders-view.fxml to {@link SpaceInvadersMechanics}: runs the game loop,
 * reads the keyboard and draws the game on a Canvas.
 *
 * The game opens on its own menu: a big pixel title and three options picked with the arrow keys
 * and Enter. Start Game shows the Planet Defense intro (what each alien is worth) for 5 seconds,
 * which Space skips, and then the game begins. During a game, Esc or the Space Invaders Menu button
 * returns to the menu.
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
    private static final String[] MENU_OPTIONS = {"START GAME", "SCOREBOARD", "BACK TO MAIN MENU"};
    private static final int START_GAME = 0;
    private static final int SCOREBOARD = 1;
    private static final int BACK_TO_MAIN_MENU = 2;

    /** How long the Planet Defense intro shows before the game starts on its own. */
    private static final double INTRO_SECONDS = 5;

    /** Which screen is showing. */
    private enum Screen { MENU, INTRO, PLAYING }

    private static final String GAME_CONTROLS = "Left/Right or A/D: move    Space: fire    Enter: play again    Esc: menu";

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

    private SpaceInvadersMechanics game;
    private Screen screen = Screen.MENU;
    private double introTimeLeft;
    /** Set when Space skips the intro, so that same press doesn't also fire the first shot. */
    private boolean waitForSpaceRelease;
    private int selectedOption = START_GAME;
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

        showMenu();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // Filters (not handlers) so the Main Menu button can't swallow the arrow keys.
        // Movement needs to know which keys are held down, so both presses and releases are tracked.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            heldKeys.add(e.getCode());
            if (screen == Screen.MENU) {
                onMenuKey(e.getCode());
                e.consume();
                return;
            }
            if (screen == Screen.INTRO) {
                if (e.getCode() == KeyCode.SPACE) {
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

    /** Up/Down move the yellow arrow, Enter picks the option it points at. */
    private void onMenuKey(KeyCode code) {
        switch (code) {
            case UP -> selectedOption = (selectedOption + MENU_OPTIONS.length - 1) % MENU_OPTIONS.length;
            case DOWN -> selectedOption = (selectedOption + 1) % MENU_OPTIONS.length;
            case ENTER -> {
                switch (selectedOption) {
                    case START_GAME -> showIntro();
                    case BACK_TO_MAIN_MENU -> onMainMenu();
                    default -> { } // the scoreboard isn't built yet
                }
            }
            default -> { }
        }
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
            double y = 340 + i * 50;
            g.setFill(i == SCOREBOARD ? OPTION_UNAVAILABLE : OPTION);
            drawPixelText(g, MENU_OPTIONS[i], optionX, y, scale);
            if (i == selectedOption) {
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

    /** 5x7 block patterns for the characters the menu and intro use. */
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
