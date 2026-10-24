package edu.farmingdale.csc311capstone.Pacman;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class PacmanController {

    private static final double TILE_SIZE = 28;

    @FXML
    private Pane mazePane;

    private final PacmanMechanics mechanics = new PacmanMechanics();

    @FXML
    private void initialize() {
        renderMaze();
    }

    private void renderMaze() {

        mazePane.getChildren().clear();

        mazePane.setPrefSize(
                mechanics.getColumns() * TILE_SIZE,
                mechanics.getRows() * TILE_SIZE
        );

        for (int row = 0; row < mechanics.getRows(); row++) {

            for (int col = 0; col < mechanics.getColumns(); col++) {

                char tile = mechanics.getTile(row, col);

                double x = col * TILE_SIZE;
                double y = row * TILE_SIZE;

                if (tile == PacmanMechanics.WALL) {

                    Rectangle wall = new Rectangle(
                            x, y, TILE_SIZE, TILE_SIZE
                    );

                    wall.setFill(Color.DARKBLUE);
                    wall.setStroke(Color.DODGERBLUE);
                    wall.setStrokeWidth(1);

                    mazePane.getChildren().add(wall);

                } else if (tile == PacmanMechanics.PELLET) {

                    Circle pellet = new Circle(
                            x + TILE_SIZE / 2,
                            y + TILE_SIZE / 2,
                            3
                    );

                    pellet.setFill(Color.LIGHTYELLOW);

                    mazePane.getChildren().add(pellet);
                }
            }
        }
    }
}
