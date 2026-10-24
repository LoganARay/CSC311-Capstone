package edu.farmingdale.csc311capstone.Pong;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class PongController {

    @FXML
    private Pane gamePane;

    @FXML
    private Rectangle leftPaddle;

    @FXML
    private Rectangle rightPaddle;

    @FXML
    private Circle ball;

    @FXML
    private Label player1Score;

    @FXML
    private Label player2Score;

}
