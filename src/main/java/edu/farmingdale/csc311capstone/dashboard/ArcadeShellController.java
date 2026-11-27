package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.SceneSwitcher;

import javafx.animation.FadeTransition;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public final class ArcadeShellController {
    @FXML private BorderPane root;
    @FXML private VBox sidebar, navigation;
    @FXML private HBox brand;
    @FXML private ScrollPane scroll;
    @FXML private StackPane pageHost;
    @FXML private Label pageTitle, pageDescription, breadcrumb;
    @FXML private Button accountButton;
    private final Map<String, Button> nav = new LinkedHashMap<>();
    private final Map<String, Parent> pages = new HashMap<>();
    private final Map<String, PageController> controllers = new HashMap<>();
    private final DashboardPreferences preferences;

    public ArcadeShellController() {
        this(new DashboardPreferences());
    }

    ArcadeShellController(DashboardPreferences preferences) {
        this.preferences = preferences;
    }

    private DashboardData data = DashboardData.unavailable();
    private String active;
    private FadeTransition transition;

    @FXML
    private void initialize() {
        root.setUserData(this);
        Label logo = Ui.label("", "brand-icon");
        logo.setGraphic(Icons.of("game"));
        brand.getChildren().setAll(logo, Ui.label("arcade", "brand-name"));
        String[] names = {"Home", "Leaderboards", "Statistics", "Profile", "Settings"};
        String[] icons = {"home", "trophy", "stats", "profile", "settings"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Button b = Ui.button(name, () -> showPage(name), "nav-button");
            b.setId("nav-" + name.toLowerCase());
            b.setGraphic(Icons.of(icons[i]));
            b.setMaxWidth(Double.MAX_VALUE);
            nav.put(name, b);
            navigation.getChildren().add(b);
        }
        accountButton.setGraphic(Ui.label("G", "small-avatar"));
        accountButton.setText("Guest player\nExplore the arcade");
        root.widthProperty()
                .addListener((o, a, b) -> sidebar.setPrefWidth(b.doubleValue() < 1150 ? 190 : 220));
        showPage("Home");
    }

    public void showPage(String name) {
        if (transition != null) transition.stop();
        if (active != null) controllers.get(active).onHidden();
        try {
            if (!pages.containsKey(name)) {
                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource(name.toLowerCase() + "-view.fxml"));
                Parent page = loader.load();
                pages.put(name, page);
                controllers.put(name, loader.getController());
            }
            pageHost.getChildren().setAll(pages.get(name));
            active = name;
            controllers.get(name).attach(this);
            nav.forEach(
                    (key, b) -> {
                        b.getStyleClass().remove("selected");
                        if (key.equals(name)) b.getStyleClass().add("selected");
                        b.setAccessibleText(key.equals(name) ? key + ", current page" : key);
                    });
            pageTitle.setText(name.equals("Home") ? "Welcome to your arcade" : name);
            pageDescription.setText(
                    switch (name) {
                        case "Home" -> "Pick a classic. Make your next high score.";
                        case "Leaderboards" -> "A little friendly rivalry. A new score to chase.";
                        case "Statistics" -> "Every session counts. Here’s your progress.";
                        case "Profile" -> "Your corner of the arcade.";
                        default -> "Make yourself at home. Keep your game your way.";
                    });
            breadcrumb.setText("ARCADE  /  " + name.toUpperCase(Locale.ROOT));
            scroll.setVvalue(0);
            pageHost.setOpacity(1);
            if (preferences.animate()) {
                transition = new FadeTransition(Duration.millis(160), pageHost);
                transition.setFromValue(.6);
                transition.setToValue(1);
                transition.play();
            }
        } catch (IOException ex) {
            pageHost.getChildren()
                    .setAll(
                            Ui.empty(
                                    "game",
                                    "This page could not open",
                                    "Return Home and try again."));
            ex.printStackTrace();
        }
    }

    public void launch(GameDefinition game) {
        try {
            SceneSwitcher.switchTo(root, game.route(), game.title());
        } catch (RuntimeException failure) {
            Alert a =
                    new Alert(
                            Alert.AlertType.ERROR,
                            "Could not open " + game.title() + ". Please try again.",
                            ButtonType.OK);
            a.initOwner(root.getScene().getWindow());
            a.getDialogPane().getStyleClass().add("arcade-root");
            a.getDialogPane()
                    .getStylesheets()
                    .add(getClass().getResource("arcade-theme.css").toExternalForm());
            a.showAndWait();
        }
    }

    @FXML
    public void openAccount() {
        SceneSwitcher.switchTo(root, "dashboard/login-view.fxml", "Arcade · Log in");
    }

    public DashboardPreferences preferences() {
        return preferences;
    }

    public DashboardData data() {
        return data;
    }

    /**
     * Integration point for an eventual authenticated backend snapshot, not a persistence service.
     */
    public void setData(DashboardData snapshot) {
        data = Objects.requireNonNull(snapshot);
        accountButton.setText(
                snapshot.player()
                        .map(p -> p.displayName() + "\n@" + p.username())
                        .orElse("Guest player\nExplore the arcade"));
        if (active != null) showPage(active);
    }

    public BorderPane root() {
        return root;
    }

    public void suspend() {
        if (transition != null) transition.stop();
        pageHost.setOpacity(1);
        if (active != null) controllers.get(active).onHidden();
    }

    public void resume() {
        if (active != null) controllers.get(active).attach(this);
    }
}
