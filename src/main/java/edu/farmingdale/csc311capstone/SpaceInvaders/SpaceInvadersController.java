package edu.farmingdale.csc311capstone.SpaceInvaders;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Alien;
import edu.farmingdale.csc311capstone.SpaceInvaders.SpaceInvadersMechanics.Shield;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * Connects spaceinvaders-view.fxml to {@link SpaceInvadersMechanics} and draws the game on a Canvas.
 *
 * Step 1 (graphics): draws the starting screen once. Movement and the game loop come in step 2.
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

    @FXML
    private void initialize() {
        game = new SpaceInvadersMechanics();
        canvas.setWidth(SpaceInvadersMechanics.PLAYFIELD_WIDTH);
        canvas.setHeight(SpaceInvadersMechanics.PLAYFIELD_HEIGHT);
        draw();
    }

    @FXML
    private void onMainMenu() {
        SceneSwitcher.showMenu(root);
    }

    // ------------------------------------------------------------------ drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        drawShields(g);
        drawAliens(g);
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
