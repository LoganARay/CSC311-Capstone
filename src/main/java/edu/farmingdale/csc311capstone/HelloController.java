package edu.farmingdale.csc311capstone;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @FXML
    private void handleBreakout(ActionEvent event) throws IOException {
        openGame(event, "breakout-view.fxml");
    }

    @FXML
    private void handleBomberman(ActionEvent event) throws IOException {
        openGame(event, "bomberman-view.fxml");
    }

    private void openGame(ActionEvent event, String view) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(view));
        Node button = (Node) event.getSource();
        button.getScene().setRoot(loader.load());
    }
}
