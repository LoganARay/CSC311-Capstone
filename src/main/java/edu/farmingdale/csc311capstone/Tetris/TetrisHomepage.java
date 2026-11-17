package edu.farmingdale.csc311capstone.Tetris;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class TetrisHomepage {

    @FXML
    private Button baseTetris;
    @FXML
    private void openBaseTetris(ActionEvent event) {
        SceneSwitcher.switchTo((Node) event.getSource(), "tetris-view.fxml", "Tetris");
    }
}
