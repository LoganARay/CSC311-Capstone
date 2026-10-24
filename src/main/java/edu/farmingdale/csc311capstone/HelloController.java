package edu.farmingdale.csc311capstone;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

public class HelloController {

    @FXML
    protected void openBlackjack(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "blackjack-view.fxml", "Blackjack");
    }

    @FXML
    protected void openSnake(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "snake-view.fxml", "Snake");
    }

    @FXML
    protected void openPong(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "pong-view.fxml", "Pong");
    }

    @FXML
    protected void openPacman(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "pacman-view.fxml", "Pac-Man");
    }

    @FXML
    private void openBreakout(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "breakout-view.fxml", "Breakout");
    }

    @FXML
    private void openBomberman(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "bomberman-view.fxml", "Bomberman");
    }

    @FXML
    private void openTetris(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "/edu/farmingdale/csc311capstone/tetris-view.fxml", "Tetris");
    }
}
