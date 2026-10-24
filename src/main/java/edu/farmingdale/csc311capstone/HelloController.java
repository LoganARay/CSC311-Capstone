package edu.farmingdale.csc311capstone;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

/**
 * Main menu. Each game button calls a method here that swaps in that game's view.
 * To hook up another game, add a method like {@link #openSnake} and set
 * onAction="#yourMethod" on its button in hello-view.fxml.
 */
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
        SceneSwitcher.switchTo(
                (Node) event.getSource(),
                "pong-view.fxml",
                "Pong"
        );
    }

}
