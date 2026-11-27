package edu.farmingdale.csc311capstone;

import edu.farmingdale.csc311capstone.dashboard.ArcadeShellController;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.*;

import java.io.*;

/** Scene-level navigation. Dashboard state belongs to the Stage and survives game round trips. */
public final class SceneSwitcher {
    public static final String MENU_VIEW = "hello-view.fxml";
    public static final String MENU_TITLE = "CSC311 Arcade";
    private static final String STATE_KEY = SceneSwitcher.class.getName() + ".dashboard";

    private record DashboardWindow(
            Scene scene, double width, double height, double x, double y, boolean maximized) {}

    private SceneSwitcher() {}

    public static void switchTo(Node source, String fxml, String title) {
        Stage stage = (Stage) source.getScene().getWindow();
        if (MENU_VIEW.equals(fxml)
                && stage.getProperties().get(STATE_KEY) instanceof DashboardWindow state) {
            stage.setScene(state.scene());
            stage.setTitle(MENU_TITLE);
            stage.setMinWidth(900);
            stage.setMinHeight(650);
            stage.setMaximized(false);
            stage.setWidth(state.width());
            stage.setHeight(state.height());
            stage.setX(state.x());
            stage.setY(state.y());
            stage.setMaximized(state.maximized());
            if (state.scene().getRoot().getUserData() instanceof ArcadeShellController shell)
                shell.resume();
            state.scene().getRoot().requestFocus();
            return;
        }
        FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxml));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + fxml, e);
        }
        // Load first so a broken destination never destroys the current scene.
        if (stage.getScene().getRoot().getUserData() instanceof ArcadeShellController shell) {
            shell.suspend();
            stage.getProperties()
                    .put(
                            STATE_KEY,
                            new DashboardWindow(
                                    stage.getScene(),
                                    stage.getWidth(),
                                    stage.getHeight(),
                                    stage.getX(),
                                    stage.getY(),
                                    stage.isMaximized()));
        }
        if ("tetris-home.fxml".equals(fxml)) {
            Button back = new Button("← Back to Arcade");
            back.setId("tetris-back-to-arcade");
            back.setOnAction(e -> showMenu(back));
            HBox toolbar = new HBox(back);
            toolbar.setStyle("-fx-padding: 10; -fx-background-color: #111421;");
            back.setStyle(
                    "-fx-background-color: #302544; -fx-text-fill: #F2F2F7; -fx-padding: 8 14;");
            BorderPane frame = new BorderPane(root);
            frame.setTop(toolbar);
            root = frame;
        }
        boolean dashboard = MENU_VIEW.equals(fxml), auth = fxml.startsWith("dashboard/");
        stage.setMaximized(false);
        stage.setMinWidth(dashboard || auth ? 900 : 0);
        stage.setMinHeight(dashboard || auth ? 650 : 0);
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        if (dashboard || auth) applyDashboardSize(stage, "1440 × 900");
        else stage.sizeToScene();
        stage.centerOnScreen();
        root.requestFocus();
    }

    public static void showMenu(Node source) {
        switchTo(source, MENU_VIEW, MENU_TITLE);
    }

    public static void applyDashboardSize(Stage stage, String size) {
        String[] dimensions = size.split(" × ");
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(Integer.parseInt(dimensions[0]), screen.getWidth()));
        stage.setHeight(Math.min(Integer.parseInt(dimensions[1]), screen.getHeight()));
        stage.centerOnScreen();
    }
}
