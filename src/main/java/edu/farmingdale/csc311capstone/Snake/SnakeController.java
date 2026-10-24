package edu.farmingdale.csc311capstone.Snake;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Direction;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Mode;
import edu.farmingdale.csc311capstone.Snake.SnakeMechanics.Point;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Connects snake-view.fxml to {@link SnakeMechanics}: runs the game loop,
 * reads the keyboard and draws the board on a Canvas.
 *
 * Controls: arrow keys / WASD to steer, Space or P to pause, Enter to restart.
 */
public class SnakeController {

    private static final int CELL = 26;

    private static final Color BOARD_A = Color.web("#1b2330");
    private static final Color BOARD_B = Color.web("#1f2836");
    private static final Color SNAKE_HEAD = Color.web("#7CFC8A");
    private static final Color SNAKE_BODY = Color.web("#3ecf5a");
    private static final Color SNAKE_TAIL = Color.web("#1f8a3a");
    private static final Color APPLE = Color.web("#ff4d4d");
    private static final Color GOLDEN = Color.web("#ffd34d");
    private static final Color ROCK = Color.web("#8a8f99");

    /** Best score per mode for as long as the app is open. */
    private static final Map<Mode, Integer> HIGH_SCORES = new EnumMap<>(Mode.class);

    @FXML private BorderPane root;
    @FXML private ComboBox<Mode> modeBox;
    @FXML private Label modeDescriptionLabel;
    @FXML private Label scoreLabel;
    @FXML private Label bestLabel;
    @FXML private Label infoLabel;
    @FXML private Canvas canvas;

    private SnakeMechanics game;
    private long lastTick;

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
            if (game.state() != SnakeMechanics.State.RUNNING) {
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
        modeBox.getItems().setAll(Mode.values());
        modeBox.getSelectionModel().select(Mode.CLASSIC);
        modeBox.valueProperty().addListener((obs, oldMode, newMode) -> {
            onNewGame();
            root.requestFocus(); // give the arrow keys back to the game
        });
        modeBox.setFocusTraversable(false);

        root.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                attachTo(scene);
            }
        });

        onNewGame();
        loop.start();
    }

    private void attachTo(Scene scene) {
        // A filter (not a handler) so the ComboBox and buttons can't swallow the arrow keys.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        // Stop the loop when the player goes back to the menu.
        scene.windowProperty().addListener((obs, oldWin, win) -> {
            if (win == null) loop.stop();
        });
    }

    @FXML
    private void onNewGame() {
        Mode mode = modeBox.getValue();
        game = new SnakeMechanics(mode);
        canvas.setWidth(game.width() * CELL);
        canvas.setHeight(game.height() * CELL);
        modeDescriptionLabel.setText(mode.description());
        lastTick = 0;
        draw();
    }

    @FXML
    private void onMainMenu() {
        loop.stop();
        SceneSwitcher.showMenu(root);
    }

    private void onKey(KeyEvent e) {
        Direction dir = switch (e.getCode()) {
            case UP, W -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, A -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };

        if (dir != null) {
            if (game.state() == SnakeMechanics.State.READY) {
                game.start();
            }
            game.turn(dir);
            e.consume();
            return;
        }

        switch (e.getCode()) {
            case SPACE, P -> {
                if (game.isOver()) {
                    onNewGame();
                } else {
                    game.togglePause();
                }
                e.consume();
            }
            case ENTER -> {
                if (game.isOver()) {
                    onNewGame();
                    e.consume();
                }
            }
            case ESCAPE -> {
                if (game.state() == SnakeMechanics.State.RUNNING) {
                    game.togglePause();
                }
                e.consume();
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ drawing

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // Checkerboard
        for (int y = 0; y < game.height(); y++) {
            for (int x = 0; x < game.width(); x++) {
                g.setFill((x + y) % 2 == 0 ? BOARD_A : BOARD_B);
                g.fillRect(x * CELL, y * CELL, CELL, CELL);
            }
        }

        // Walls glow red in modes where they kill you; wrap modes get a dashed blue edge.
        g.setLineWidth(4);
        if (game.wrapsAround()) {
            g.setStroke(Color.web("#4da3ff"));
            g.setLineDashes(10, 8);
        } else {
            g.setStroke(Color.web("#ff6b6b"));
            g.setLineDashes();
        }
        g.strokeRect(2, 2, canvas.getWidth() - 4, canvas.getHeight() - 4);
        g.setLineDashes();

        // Rocks
        for (Point p : game.obstacles()) {
            g.setFill(ROCK);
            g.fillRoundRect(p.x() * CELL + 2, p.y() * CELL + 2, CELL - 4, CELL - 4, 8, 8);
            g.setFill(ROCK.darker());
            g.fillRoundRect(p.x() * CELL + 6, p.y() * CELL + CELL / 2.0, CELL - 12, CELL / 2.0 - 4, 6, 6);
        }

        // Apples
        if (game.food() != null) {
            drawApple(g, game.food(), APPLE);
        }
        if (game.goldenApple() != null) {
            boolean blinking = game.goldenTicksLeft() < 12 && (game.goldenTicksLeft() / 2) % 2 == 0;
            if (!blinking) {
                drawApple(g, game.goldenApple(), GOLDEN);
            }
        }

        // Snake: fades from bright head to darker tail
        List<Point> body = game.body();
        for (int i = body.size() - 1; i >= 0; i--) {
            Point p = body.get(i);
            double t = body.size() == 1 ? 0 : (double) i / (body.size() - 1);
            g.setFill(i == 0 ? SNAKE_HEAD : SNAKE_BODY.interpolate(SNAKE_TAIL, t));
            g.fillRoundRect(p.x() * CELL + 1.5, p.y() * CELL + 1.5, CELL - 3, CELL - 3, 10, 10);
        }
        drawEyes(g, game.head(), game.direction());

        drawHud();
        drawOverlay(g);
    }

    private void drawApple(GraphicsContext g, Point p, Color color) {
        double cx = p.x() * CELL + CELL / 2.0;
        double cy = p.y() * CELL + CELL / 2.0 + 1;
        double r = CELL / 2.0 - 3;
        g.setFill(color);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setFill(Color.web("#ffffff", 0.35));
        g.fillOval(cx - r / 2, cy - r / 1.5, r / 1.6, r / 1.6);
        g.setStroke(Color.web("#6b3e1f"));
        g.setLineWidth(2);
        g.strokeLine(cx, cy - r, cx + 2, cy - r - 4);
    }

    private void drawEyes(GraphicsContext g, Point head, Direction dir) {
        double cx = head.x() * CELL + CELL / 2.0;
        double cy = head.y() * CELL + CELL / 2.0;
        // Two eyes, offset sideways from the direction of travel
        double fx = dir.dx * 5, fy = dir.dy * 5;
        double sx = -dir.dy * 5, sy = dir.dx * 5;
        g.setFill(Color.web("#10151d"));
        g.fillOval(cx + fx + sx - 2.5, cy + fy + sy - 2.5, 5, 5);
        g.fillOval(cx + fx - sx - 2.5, cy + fy - sy - 2.5, 5, 5);
    }

    private void drawHud() {
        scoreLabel.setText("Score: " + game.score());
        int best = Math.max(HIGH_SCORES.getOrDefault(game.mode(), 0), game.score());
        bestLabel.setText("Best: " + best);

        String info = switch (game.mode()) {
            case TIME_ATTACK -> String.format("Time: %.1fs", game.timeLeftMs() / 1000.0);
            case SPEED_RUSH -> String.format("Speed: %.1f moves/s  |  %d pts per apple",
                    1000.0 / game.tickIntervalMs(), game.pointsPerApple());
            case OBSTACLES -> "Rocks: " + game.obstacles().size();
            default -> "Length: " + game.length();
        };
        infoLabel.setText(info);
        infoLabel.setTextFill(game.mode() == Mode.TIME_ATTACK && game.timeLeftMs() < 10_000
                ? Color.web("#ff6b6b") : Color.web("#cfd8e3"));
    }

    private void drawOverlay(GraphicsContext g) {
        String title;
        String subtitle;
        switch (game.state()) {
            case READY -> {
                title = game.mode().displayName();
                subtitle = "Press an arrow key or WASD to start";
            }
            case PAUSED -> {
                title = "Paused";
                subtitle = "Press Space to keep going";
            }
            case GAME_OVER -> {
                title = "Game Over";
                subtitle = game.endReason() + "\nScore: " + game.score() + "\nPress Enter to play again";
            }
            case WON -> {
                title = "You Win!";
                subtitle = game.endReason() + "\nScore: " + game.score() + "\nPress Enter to play again";
            }
            default -> {
                return;
            }
        }

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.web("#000000", 0.55));
        g.fillRect(0, 0, w, h);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("System", FontWeight.BOLD, 40));
        g.fillText(title, w / 2, h / 2 - 30);
        g.setFont(Font.font("System", 18));
        g.setFill(Color.web("#cfd8e3"));
        g.fillText(subtitle, w / 2, h / 2 + 10);
    }
}
