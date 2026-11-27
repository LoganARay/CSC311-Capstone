package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.*;

import javafx.animation.PauseTransition;
import javafx.application.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

/**
 * Runs in a separate JavaFX process: real Controls, CSS, Stage navigation and rendered snapshots.
 * It fires JavaFX actions programmatically; it does not claim physical mouse/keyboard testing.
 */
public final class DashboardSceneProbe {
    public static void main(String[] args) {
        Application.launch(ProbeApp.class, args);
    }

    public static final class ProbeApp extends Application {
        private Stage stage;
        private Parent dashboard;
        private ArcadeShellController shell;
        private final Queue<Runnable> steps = new ArrayDeque<>();
        private Preferences node;
        private Path output;
        private int checks;

        @Override
        public void start(Stage stage) throws Exception {
            this.stage = stage;
            Platform.setImplicitExit(false);
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> fail(e));
            output =
                    Path.of(
                            System.getProperty(
                                    "arcade.screenshots",
                                    System.getProperty("java.io.tmpdir")
                                            + "/arcade-redesign/screenshots"));
            Files.createDirectories(output);
            node =
                    Preferences.userRoot()
                            .node("edu/farmingdale/csc311capstone/scene-test-" + UUID.randomUUID());
            FXMLLoader loader =
                    new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
            loader.setControllerFactory(
                    type -> {
                        try {
                            return type == ArcadeShellController.class
                                    ? new ArcadeShellController(new DashboardPreferences(node))
                                    : type.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            dashboard = loader.load();
            shell = loader.getController();
            stage.setScene(new Scene(dashboard, 1440, 900));
            stage.show();
            steps.add(
                    () -> {
                        shot(dashboard, "home", 1440, 900);
                        check(
                                dashboard.lookupAll(".game-card").size() == 8,
                                "All 8 game cards rendered");
                        var grid = (TilePane) dashboard.lookup("#library");
                        check(columns(grid) == 4, "Four columns at 1440");
                        shot(dashboard, "home-1280", 1280, 800);
                        check(columns(grid) == 4, "Four columns at 1280");
                        shot(dashboard, "home-1024", 1024, 768);
                        check(columns(grid) == 3, "Three columns at 1024");
                        shot(dashboard, "home-900", 900, 650);
                        check(columns(grid) < 4, "Adaptive smaller window");
                        check(
                                dashboard.lookup("#nav-settings").isVisible(),
                                "Sidebar accessible at minimum size");
                    });
            steps.add(
                    () -> {
                        TextField search = (TextField) dashboard.lookup("#search");
                        search.setText("PaC-MaN");
                        check(
                                dashboard.lookupAll(".game-card").size() == 1,
                                "Search is case insensitive");
                        shot(dashboard, "home-search", 1440, 900);
                        search.setText("zzzz");
                        check(dashboard.lookupAll(".game-card").isEmpty(), "No results state");
                        shot(dashboard, "home-no-results", 1024, 768);
                        findButton(dashboard, "Reset filters").fire();
                        check(
                                search.getText().isEmpty()
                                        && dashboard.lookupAll(".game-card").size() == 8,
                                "Reset search and filters action");
                        search.setText("Pong");
                        findButton(dashboard, "×").fire();
                        check(search.getText().isEmpty(), "Clear search action");
                        ((ToggleButton) dashboard.lookup("#filter-puzzle")).fire();
                        check(dashboard.lookupAll(".game-card").size() == 1, "Puzzle filter");
                        ((ToggleButton) dashboard.lookup("#filter-all-games")).fire();
                        ((ComboBox<?>) dashboard.lookup("#sort")).getSelectionModel().select(1);
                        check(
                                ((TilePane) dashboard.lookup("#library"))
                                        .getChildren()
                                        .getFirst()
                                        .getId()
                                        .equals("card-blackjack"),
                                "Name sort");
                        ((ComboBox<?>) dashboard.lookup("#sort")).getSelectionModel().selectFirst();
                    });
            for (String page : List.of("Leaderboards", "Statistics", "Profile", "Settings")) {
                steps.add(
                        () -> {
                            fire("nav-" + page.toLowerCase());
                        });
                steps.add(
                        () -> {
                            shot(dashboard, page.toLowerCase(), 1440, 900);
                            check(
                                    ((Label) dashboard.lookup("#pageTitle")).getText().equals(page),
                                    page + " navigation");
                            for (Node table : dashboard.lookupAll(".table-view"))
                                check(
                                        ((TableView<?>) table).getItems().isEmpty(),
                                        "No fabricated table data");
                        });
                steps.add(
                        () -> {
                            shot(dashboard, page.toLowerCase() + "-1024", 1024, 768);
                            fullPage(page.toLowerCase() + "-full");
                        });
            }
            for (String tab : List.of("Account", "Display", "About", "General")) {
                steps.add(() -> fire("settings-" + tab.toLowerCase()));
                steps.add(
                        () -> {
                            check(
                                    all((Parent) dashboard.lookup("#body")).stream()
                                            .filter(Label.class::isInstance)
                                            .map(Label.class::cast)
                                            .anyMatch(
                                                    l ->
                                                            l.getText()
                                                                    .equals(
                                                                            tab.equals("About")
                                                                                    ? "arcade"
                                                                                    : tab
                                                                                            + " Settings")),
                                    "Settings tab content: " + tab);
                            shot(dashboard, "settings-" + tab.toLowerCase(), 1440, 900);
                        });
            }
            steps.add(
                    () -> {
                        fire("settings-display");
                        ((ComboBox<?>) dashboard.lookup("#preferred-size"))
                                .getSelectionModel()
                                .select(2);
                        fire("save");
                        check(Math.abs(stage.getWidth() - 1024) < 3, "Saved window size applied");
                        ((ToggleButton) dashboard.lookup("#start-maximized")).fire();
                        fire("save");
                        check(
                                shell.preferences().current().startMaximized(),
                                "Start maximized preference saved");
                        fire("settings-general");
                        ToggleButton reduce = (ToggleButton) dashboard.lookup("#reduce-motion");
                        reduce.fire();
                        check(
                                ((Label) dashboard.lookup("#feedback"))
                                        .getText()
                                        .contains("Unsaved"),
                                "Settings dirty indicator");
                        fire("save");
                        check(shell.preferences().current().reduceMotion(), "Settings save action");
                        fire("reset");
                        check(
                                !((Button) dashboard.lookup("#save")).isDisabled(),
                                "Reset defaults requires save");
                        fire("save");
                        check(
                                shell.preferences().current().equals(DashboardPreferences.DEFAULTS),
                                "Settings reset saved");
                        shell.showPage("Home");
                    });
            for (GameDefinition game : GameCatalog.GAMES) {
                double[] window = new double[2];
                steps.add(
                        () -> {
                            ((TextField) dashboard.lookup("#search")).setText(game.title());
                            window[0] = stage.getWidth();
                            window[1] = stage.getHeight();
                            ((Button) dashboard.lookup("#play-" + game.id())).fire();
                            check(
                                    stage.getTitle().equals(game.title()),
                                    "Launched " + game.title());
                        });
                steps.add(
                        () -> {
                            Parent gameRoot = stage.getScene().getRoot();
                            // The native game return action where exposed; otherwise shared public
                            // route.
                            Optional<Button> back =
                                    all(gameRoot).stream()
                                            .filter(Button.class::isInstance)
                                            .map(Button.class::cast)
                                            .filter(
                                                    b ->
                                                            b.isVisible()
                                                                    && (b.getText().equals("Back")
                                                                            || b.getText()
                                                                                    .toLowerCase()
                                                                                    .contains(
                                                                                            "arcade")))
                                            .findFirst();
                            if (back.isPresent()) back.get().fire();
                            else SceneSwitcher.showMenu(gameRoot);
                            check(
                                    stage.getScene().getRoot() == dashboard,
                                    "Retained dashboard after " + game.title());
                            check(
                                    stage.getTitle().equals(SceneSwitcher.MENU_TITLE),
                                    "Restored title");
                            check(dashboard.lookup("#nav-home") != null, "Sidebar restored");
                            check(
                                    ((TextField) dashboard.lookup("#search"))
                                            .getText()
                                            .equals(game.title()),
                                    "Search preserved after " + game.title());
                            check(
                                    Math.abs(stage.getWidth() - window[0]) < 3
                                            && Math.abs(stage.getHeight() - window[1]) < 3,
                                    "Window size restored after " + game.title());
                            ((TextField) dashboard.lookup("#search")).clear();
                        });
            }
            steps.add(
                    () -> {
                        shell.showPage("Home");
                        fullPage("home-full");
                        ((ScrollPane) dashboard.lookup("#scroll")).setVvalue(1);
                    });
            steps.add(
                    () -> {
                        shot(dashboard, "home-recent-empty", 1440, 900);
                        ((ScrollPane) dashboard.lookup("#scroll")).setVvalue(0);
                    });
            steps.add(
                    () -> {
                        fire("featuredPlay");
                        check(stage.getTitle().equals("Pac-Man"), "Featured play action");
                        SceneSwitcher.showMenu(stage.getScene().getRoot());
                        fire("surprise");
                        check(
                                GameCatalog.GAMES.stream()
                                        .anyMatch(g -> g.title().equals(stage.getTitle())),
                                "Surprise Me launched an available game");
                        SceneSwitcher.showMenu(stage.getScene().getRoot());
                        shot(dashboard, "home-returned", 1440, 900);
                        shell.openAccount();
                    });
            steps.add(
                    () -> {
                        Parent r = stage.getScene().getRoot();
                        shot(r, "login", 1440, 900);
                        check(r.lookup("#password") != null, "Login loaded");
                        AuthController auth = (AuthController) r.getUserData();
                        auth.setBusy(true);
                        check(
                                ((Button) r.lookup("#submitButton")).isDisabled()
                                        && r.lookup("#loading").isVisible(),
                                "Authentication loading state");
                        auth.showAuthenticationError("Test-only unavailable service");
                        check(
                                ((Label) r.lookup("#feedback")).getStyleClass().contains("error"),
                                "Authentication error state");
                        findButton(r, "Log in  →").fire();
                        check(
                                ((Label) r.lookup("#feedback")).getText().contains("Enter"),
                                "Login required validation");
                        ((TextField) r.lookup("#username")).setText("probe-only");
                        ((TextField) r.lookup("#password")).setText("not-a-credential");
                        ((Button) r.lookup("#showPassword")).fire();
                        check(
                                r.lookup("#visiblePassword").isVisible(),
                                "Password visibility toggle");
                        findButton(r, "Log in  →").fire();
                        check(
                                ((Label) r.lookup("#feedback")).getText().contains("not connected"),
                                "No fake login");
                        check(
                                ((TextField) r.lookup("#password")).getText().isEmpty(),
                                "Password cleared");
                        shot(r, "login-1024", 1024, 768);
                        shot(r, "login-900", 900, 650);
                        findButton(r, "Create an account  →").fire();
                    });
            steps.add(
                    () -> {
                        Parent r = stage.getScene().getRoot();
                        shot(r, "registration", 1440, 900);
                        findButton(r, "Create account  →").fire();
                        check(
                                ((Label) r.lookup("#feedback")).getText().contains("Confirm"),
                                "Registration required validation");
                        ((TextField) r.lookup("#username")).setText("probe-only");
                        ((TextField) r.lookup("#password")).setText("test-value");
                        ((TextField) r.lookup("#confirm")).setText("mismatch");
                        findButton(r, "Create account  →").fire();
                        check(
                                ((Label) r.lookup("#feedback")).getText().contains("do not match"),
                                "Confirmation validation");
                        ((TextField) r.lookup("#confirm")).setText("test-value");
                        findButton(r, "Create account  →").fire();
                        check(
                                ((Label) r.lookup("#feedback")).getText().contains("not connected"),
                                "No fake registration");
                        shot(r, "registration-1024", 1024, 768);
                        shot(r, "registration-900", 900, 650);
                        findButton(r, "Continue as guest").fire();
                        check(
                                stage.getScene().getRoot() == dashboard,
                                "Guest returns to retained shell");
                    });
            steps.add(
                    () -> {
                        for (int i = 0; i < 20; i++) {
                            shell.showPage("Profile");
                            shell.showPage("Home");
                        }
                        check(
                                dashboard.lookupAll(".game-card").size() == 8,
                                "Repeated navigation has no duplicate cards");
                        System.out.println(
                                "SCENE PROBE PASSED: " + checks + " checks; snapshots: " + output);
                        finish(0);
                    });
            next();
        }

        private void next() {
            if (steps.isEmpty()) return;
            PauseTransition pause = new PauseTransition(Duration.millis(220));
            pause.setOnFinished(
                    e -> {
                        try {
                            steps.remove().run();
                            next();
                        } catch (Throwable error) {
                            fail(error);
                        }
                    });
            pause.play();
        }

        private int columns(TilePane grid) {
            return (int)
                    Math.round(
                            (grid.getWidth() + grid.getHgap())
                                    / (grid.getTileWidth() + grid.getHgap()));
        }

        private void fire(String id) {
            ButtonBase b = (ButtonBase) dashboard.lookup("#" + id);
            check(b != null, "Control exists: " + id);
            b.fire();
        }

        private static Button findButton(Parent root, String text) {
            return all(root).stream()
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .filter(b -> b.getText().equals(text))
                    .findFirst()
                    .orElseThrow();
        }

        private static List<Node> all(Parent parent) {
            List<Node> nodes = new ArrayList<>();
            for (Node n : parent.getChildrenUnmodifiable()) {
                nodes.add(n);
                if (n instanceof Parent p) nodes.addAll(all(p));
            }
            return nodes;
        }

        private void check(boolean result, String message) {
            if (!result) throw new AssertionError(message);
            checks++;
            System.out.println("PASS: " + message);
        }

        private void shot(Parent root, String name, int width, int height) {
            shell.suspend();
            root.resize(width, height);
            for (int i = 0; i < 4; i++) {
                root.applyCss();
                root.layout();
            }
            check(
                    root.getLayoutBounds().getWidth() <= width + 1,
                    "No root width overflow: " + name);
            if (root == dashboard) {
                ScrollPane sc = (ScrollPane) dashboard.lookup("#scroll");
                check(
                        sc.getContent().getLayoutBounds().getWidth()
                                <= sc.getViewportBounds().getWidth() + 1,
                        "Page fits viewport: " + name);
            }
            for (Node n : all(root))
                if (n instanceof Labeled l && treeVisible(l) && l.isTextTruncated())
                    throw new AssertionError("Truncated text [" + name + "]: " + l.getText());
            write(root, name, width, height);
        }

        private void fullPage(String name) {
            ScrollPane sc = (ScrollPane) dashboard.lookup("#scroll");
            Parent content = (Parent) sc.getContent();
            content.applyCss();
            content.layout();
            write(
                    content,
                    name,
                    (int) Math.ceil(content.getLayoutBounds().getWidth()),
                    (int) Math.ceil(content.getLayoutBounds().getHeight()));
        }

        private boolean treeVisible(Node node) {
            for (Node n = node; n != null; n = n.getParent()) if (!n.isVisible()) return false;
            return true;
        }

        private void write(Parent root, String name, int w, int h) {
            try {
                SnapshotParameters params = new SnapshotParameters();
                params.setViewport(new Rectangle2D(0, 0, w, h));
                WritableImage image = root.snapshot(params, new WritableImage(w, h));
                BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                int[] pixels = new int[w * h];
                image.getPixelReader()
                        .getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
                b.setRGB(0, 0, w, h, pixels, 0, w);
                ImageIO.write(b, "png", output.resolve(name + ".png").toFile());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void fail(Throwable error) {
            error.printStackTrace();
            finish(1);
        }

        private void finish(int code) {
            try {
                node.removeNode();
                node.flush();
            } catch (Exception ignored) {
            }
            stage.close();
            Platform.exit();
            System.exit(code);
        }
    }
}
