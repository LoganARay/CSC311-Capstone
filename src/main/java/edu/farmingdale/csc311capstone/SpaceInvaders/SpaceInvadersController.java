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
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.Set;

/**
 * Connects spaceinvaders-view.fxml to {@link SpaceInvadersMechanics}: runs the game loop,
 * reads the keyboard and draws the game on a Canvas.
 *
 * Controls: Left/Right or A/D to move, Space to fire.
 */
public class SpaceInvadersController {

    private static final Color CANNON = Color.LIMEGREEN;
    private static final Color SHIELD = Color.LIMEGREEN;
    private static final Color SQUID = Color.MAGENTA;
    private static final Color CRAB = Color.CYAN;
    private static final Color OCTOPUS = Color.LIMEGREEN;

    /** Best score for as long as the app is open. */
    private static int highScore = 0;

    @FXML private BorderPane root;
    @FXML private Label scoreLabel;
    @FXML private Label bestLabel;
    @FXML private Label livesLabel;
    @FXML private Label waveLabel;
    @FXML private Canvas canvas;

    private SpaceInvadersMechanics game;
    private final Set<KeyCode> heldKeys = new HashSet<>();
    private long lastFrame;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double deltaSeconds = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000.0;
            lastFrame = now;
            // Cap the step so a slow frame doesn't teleport the cannon
            deltaSeconds = Math.min(deltaSeconds, 0.05);
            game.update(deltaSeconds, horizontalInput(), heldKeys.contains(KeyCode.SPACE));
            draw();
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

        draw();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // Filters (not handlers) so the Main Menu button can't swallow the arrow keys.
        // Movement needs to know which keys are held down, so both presses and releases are tracked.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            heldKeys.add(e.getCode());
            if (horizontalInput() != 0 || e.getCode() == KeyCode.SPACE) e.consume();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> heldKeys.remove(e.getCode()));
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

    @FXML
    private void onMainMenu() {
        loop.stop();
        SceneSwitcher.showMenu(root);
    }

    // ------------------------------------------------------------------ drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawShields(g);
        drawAliens(g);
        drawShot(g);
        g.setFill(CANNON);
        drawCannon(g, game.getPlayerX(), SpaceInvadersMechanics.PLAYER_Y);

        // Ground line under the cannon
        double groundY = SpaceInvadersMechanics.PLAYER_Y + SpaceInvadersMechanics.PLAYER_HEIGHT / 2 + 8;
        g.setStroke(CANNON);
        g.setLineWidth(2);
        g.strokeLine(0, groundY, canvas.getWidth(), groundY);

        drawHud();
    }

    private void drawHud() {
        highScore = Math.max(highScore, game.getScore());
        scoreLabel.setText(String.format("Score: %05d", game.getScore()));
        bestLabel.setText(String.format("Best: %05d", highScore));
        livesLabel.setText("Lives: " + game.getLives());
        waveLabel.setText("Wave: " + game.getWave());
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

    private void drawShot(GraphicsContext g) {
        Bullet shot = game.getPlayerShot();
        if (shot != null) {
            g.setFill(Color.WHITE);
            g.fillRect(shot.getLeft(), shot.getTop(), Bullet.WIDTH, Bullet.HEIGHT);
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
