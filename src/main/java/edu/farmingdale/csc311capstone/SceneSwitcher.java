package edu.farmingdale.csc311capstone;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Swaps the window between the main menu and each game's FXML view.
 * Any game can use this, e.g. {@code SceneSwitcher.switchTo(button, "pong-view.fxml", "Pong")}.
 */
public final class SceneSwitcher {

    public static final String MENU_VIEW = "hello-view.fxml";
    public static final String MENU_TITLE = "CSC311 Arcade";

    private SceneSwitcher() { }

    /**
     * @param source any node currently on screen (used to find the window)
     * @param fxml   file name in src/main/resources/edu/farmingdale/csc311capstone/
     * @param title  window title
     */
    public static void switchTo(Node source, String fxml, String title) {
        Stage stage = (Stage) source.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(SceneSwitcher.class.getResource(fxml));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + fxml, e);
        }
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.sizeToScene();
        stage.centerOnScreen();
        root.requestFocus();
    }

    public static void showMenu(Node source) {
        switchTo(source, MENU_VIEW, MENU_TITLE);
    }
}
