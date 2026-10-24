package edu.farmingdale.csc311capstone.Bomberman;

import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class BombermanController {
    private BombermanGame game;

    @FXML
    private GridPane gameArea;
    @FXML
    private Label statusLabel;

    @FXML
    private void initialize() {
        game = new BombermanGame();
        int[][] grid = game.getMap().getGrid();
        for (int row = 0; row < grid.length; row++) {
            for (int column = 0; column < grid[row].length; column++) {
                Rectangle tile = new Rectangle(34, 34);
                if (grid[row][column] == GameMap.WALL) {
                    tile.setFill(Color.SLATEGRAY);
                } else if (grid[row][column] == GameMap.BLOCK) {
                    tile.setFill(Color.SANDYBROWN);
                } else {
                    tile.setFill(Color.WHITESMOKE);
                }
                tile.setStroke(Color.LIGHTGRAY);
                gameArea.add(tile, column, row);
            }
        }
        Player player = game.getPlayer();
        Circle playerShape = new Circle(11, Color.ROYALBLUE);
        GridPane.setHalignment(playerShape, HPos.CENTER);
        GridPane.setValignment(playerShape, VPos.CENTER);
        gameArea.add(playerShape, player.getColumn(), player.getRow());
    }

    @FXML
    private void handleStart() {
        game.startGame();
        statusLabel.setText("Bomberman skeleton only - gameplay coming later.");
    }

    @FXML
    private void handleBack() {
        SceneSwitcher.showMenu(gameArea);
    }
}
