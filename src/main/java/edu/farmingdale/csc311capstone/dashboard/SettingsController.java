package edu.farmingdale.csc311capstone.dashboard;

import edu.farmingdale.csc311capstone.SceneSwitcher;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public final class SettingsController extends PageController {
    @FXML private FlowPane tabs;
    @FXML private VBox body;
    @FXML private Label feedback;
    @FXML private Button save, reset;
    private ToggleButton reduceMotion, animations, startMaximized;
    private ComboBox<String> size;
    private boolean syncing, loaded;
    private String activeTab = "General";

    @FXML
    private void initialize() {
        ToggleGroup group = new ToggleGroup();
        for (String title : List.of("General", "Account", "Display", "About")) {
            ToggleButton b = new ToggleButton(title);
            b.setId("settings-" + title.toLowerCase());
            b.setToggleGroup(group);
            b.getStyleClass().add("filter");
            b.setSelected(title.equals(activeTab));
            b.setOnAction(
                    e -> {
                        if (!b.isSelected()) b.setSelected(true);
                        activeTab = title;
                        showTab();
                    });
            tabs.getChildren().add(b);
        }
        reduceMotion = toggle();
        animations = toggle();
        startMaximized = toggle();
        reduceMotion.setId("reduce-motion");
        animations.setId("dashboard-animations");
        startMaximized.setId("start-maximized");
        size = Ui.combo(DashboardPreferences.SIZES);
        size.setId("preferred-size");
        reduceMotion.selectedProperty().addListener((o, a, b) -> changed());
        animations.selectedProperty().addListener((o, a, b) -> changed());
        startMaximized.selectedProperty().addListener((o, a, b) -> changed());
        size.valueProperty().addListener((o, a, b) -> changed());
    }

    private ToggleButton toggle() {
        ToggleButton b = new ToggleButton("Off");
        b.getStyleClass().add("toggle-switch");
        b.selectedProperty().addListener((o, a, v) -> b.setText(v ? "On" : "Off"));
        return b;
    }

    @Override
    protected void onShown() {
        if (!loaded) {
            load();
            loaded = true;
        }
        showTab();
    }

    private void load() {
        syncing = true;
        var v = shell.preferences().current();
        reduceMotion.setSelected(v.reduceMotion());
        animations.setSelected(v.animations());
        startMaximized.setSelected(v.startMaximized());
        size.setValue(v.size());
        syncing = false;
        changed();
    }

    private DashboardPreferences.Values values() {
        return new DashboardPreferences.Values(
                reduceMotion.isSelected(),
                animations.isSelected(),
                startMaximized.isSelected(),
                size.getValue());
    }

    private boolean isDirty() {
        return shell != null && !values().equals(shell.preferences().current());
    }

    private void changed() {
        if (syncing || shell == null) return;
        feedback.getStyleClass().remove("error");
        feedback.setText(isDirty() ? "●  Unsaved changes" : "All changes saved");
        save.setDisable(!isDirty());
    }

    private void showTab() {
        if (shell == null) return;
        body.getChildren().clear();
        switch (activeTab) {
            case "General" -> {
                body.getChildren()
                        .addAll(
                                Ui.label("General Settings", "section-heading"),
                                Ui.label(
                                        "A few small things to make the arcade yours.",
                                        "secondary"),
                                row(
                                        "Reduce motion",
                                        "Use immediate page changes and keep movement to a"
                                            + " minimum.",
                                        reduceMotion),
                                row(
                                        "Dashboard animations",
                                        "Enable subtle page transitions. Reduce motion takes"
                                            + " priority.",
                                        animations),
                                row(
                                        "Dashboard sound effects",
                                        "Dashboard audio is not included in this version.",
                                        unavailable()),
                                row(
                                        "Background music",
                                        "Individual games manage their own music and volume.",
                                        unavailable()),
                                row(
                                        "Library view",
                                        "Your games are shown in an adaptive card grid.",
                                        Ui.label("Grid", "secondary")));
            }
            case "Account" -> {
                body.getChildren()
                        .addAll(
                                Ui.label("Account Settings", "section-heading"),
                                Ui.label(
                                        "Your account, player identity, and saved progress.",
                                        "secondary"),
                                Ui.label(
                                        "You’re playing as a guest. Account services are not"
                                            + " connected yet.",
                                        "notice"),
                                row(
                                        "Username",
                                        "Available when you sign in.",
                                        Ui.label("—", "secondary")),
                                row(
                                        "Email",
                                        "An email requirement will follow the team’s account"
                                            + " service.",
                                        Ui.label("Unavailable", "secondary")),
                                row(
                                        "Edit account details",
                                        "Your account service will manage profile changes.",
                                        disabled("Edit details")),
                                row(
                                        "Change password",
                                        "Password changes require the team’s authentication"
                                            + " service.",
                                        disabled("Change password")),
                                row(
                                        "Sign out",
                                        "There is no active account session.",
                                        disabled("Sign out")),
                                Ui.button("Go to Log in  →", shell::openAccount, "primary"));
            }
            case "Display" -> {
                body.getChildren()
                        .addAll(
                                Ui.label("Display Settings", "section-heading"),
                                Ui.label(
                                        "Window preferences apply to the dashboard. Games keep"
                                            + " their own layout.",
                                        "secondary"),
                                row(
                                        "Preferred window size",
                                        "Save to apply this size to the current dashboard window.",
                                        size),
                                row(
                                        "Start maximized",
                                        "Open the dashboard maximized on the next launch.",
                                        startMaximized),
                                row(
                                        "Window mode",
                                        "Change the current dashboard window now.",
                                        Ui.button(
                                                ((Stage) shell.root().getScene().getWindow())
                                                                .isMaximized()
                                                        ? "Use windowed mode"
                                                        : "Maximize window",
                                                () -> {
                                                    Stage stage =
                                                            (Stage)
                                                                    shell.root()
                                                                            .getScene()
                                                                            .getWindow();
                                                    stage.setMaximized(!stage.isMaximized());
                                                    showTab();
                                                })),
                                Ui.label(
                                        "The initial window is constrained to the available"
                                            + " display. The library adapts as you resize.",
                                        "notice"));
            }
            case "About" -> {
                ArcadeArt art = new ArcadeArt("invaders");
                art.setPrefHeight(120);
                art.setMaxHeight(120);
                body.getChildren()
                        .addAll(
                                Ui.label("arcade", "hero-title"),
                                Ui.label("THE CLASSICS. RELOADED.", "eyebrow,purple"),
                                art,
                                Ui.label(
                                        "CSC311 Advanced Programming · Group Capstone",
                                        "section-title"),
                                Ui.label("Version 1.0-SNAPSHOT · Java 25 · JavaFX 25", "secondary"),
                                Ui.label(
                                        "Eight classic game routes in a native desktop arcade."
                                            + " Built by the CSC311 capstone team.",
                                        "secondary"),
                                Ui.label(
                                        "Dashboard artwork and line icons are original JavaFX"
                                            + " Canvas and vector drawings. Existing game assets"
                                            + " keep their original credits. See CREDITS.md in the"
                                            + " project for full team and asset attribution.",
                                        "secondary"),
                                Ui.label(
                                        "Accounts, shared rankings, score history, and player"
                                            + " records are awaiting the team’s backend"
                                            + " integration.",
                                        "notice"));
            }
        }
    }

    private Label unavailable() {
        return Ui.label("Unavailable", "muted");
    }

    private Button disabled(String text) {
        Button b = new Button(text);
        b.setDisable(true);
        return b;
    }

    private HBox row(String title, String detail, Node control) {
        VBox copy = new VBox(5, Ui.label(title, "setting-name"), Ui.label(detail, "small,muted"));
        copy.setMinWidth(0);
        HBox.setHgrow(copy, Priority.ALWAYS);
        if (control instanceof javafx.scene.layout.Region r) r.setMaxWidth(245);
        HBox row = Ui.row(copy, control);
        row.getStyleClass().add("setting-row");
        return row;
    }

    @FXML
    private void saveChanges() {
        try {
            String previous = shell.preferences().current().size();
            shell.preferences().save(values());
            if (!previous.equals(values().size())) {
                Stage s = (Stage) shell.root().getScene().getWindow();
                s.setMaximized(false);
                SceneSwitcher.applyDashboardSize(s, values().size());
            }
            changed();
            feedback.setText("✓  Preferences saved");
        } catch (Exception failure) {
            feedback.getStyleClass().add("error");
            feedback.setText("Could not save preferences. Your changes are still here.");
        }
    }

    @FXML
    private void resetDefaults() {
        syncing = true;
        var v = DashboardPreferences.DEFAULTS;
        reduceMotion.setSelected(v.reduceMotion());
        animations.setSelected(v.animations());
        startMaximized.setSelected(v.startMaximized());
        size.setValue(v.size());
        syncing = false;
        changed();
        feedback.setText("Defaults restored · Save Changes to apply");
    }
}
