package edu.farmingdale.csc311capstone.Breakout;

import edu.farmingdale.csc311capstone.HelloApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.io.IOException;

public class BreakoutController {
    @FXML
    private Pane gameArea;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        // TODO: Display the initial paddle, ball, and bricks.
    }

    @FXML
    private void handleStart() {
        statusLabel.setText("Breakout skeleton only - gameplay coming later.");
        // TODO: Start the game once gameplay is implemented.
    }

    @FXML
    private void handleBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        gameArea.getScene().setRoot(loader.load());
    }
}
