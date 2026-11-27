package edu.farmingdale.csc311capstone.dashboard;

import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class Ui {
    private Ui() {}

    public static Label label(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll(style.split(","));
        l.setWrapText(true);
        l.setMinHeight(Region.USE_PREF_SIZE);
        return l;
    }

    public static Button button(String text, Runnable action, String... styles) {
        Button b = new Button(text);
        b.getStyleClass().addAll(styles);
        b.setOnAction(e -> action.run());
        return b;
    }

    public static VBox card(String title, Node... children) {
        VBox box = new VBox(15);
        box.getStyleClass().add("panel");
        if (title != null) box.getChildren().add(label(title, "section-title"));
        box.getChildren().addAll(children);
        return box;
    }

    public static VBox empty(String icon, String title, String description) {
        VBox box =
                new VBox(
                        10,
                        Icons.of(icon),
                        label(title, "empty-title"),
                        label(description, "secondary"));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("empty-state");
        return box;
    }

    public static VBox stat(String icon, String title, String value, String note) {
        HBox name = new HBox(10, Icons.of(icon), label(title, "secondary"));
        name.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10, name, label(value, "stat-value"), label(note, "small,muted"));
        box.getStyleClass().add("stat-card");
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    public static void statGrid(TilePane grid) {
        grid.getChildren()
                .setAll(
                        stat("game", "Games played", "—", "No account history connected"),
                        stat("clock", "Time played", "—", "Across your arcade sessions"),
                        stat("star", "Personal bests", "—", "Your game-specific records"),
                        stat("trophy", "Wins", "—", "For games with win outcomes"));
        grid.widthProperty().addListener((o, a, b) -> sizeStats(grid, 210));
        sizeStats(grid, 210);
    }

    public static void sizeStats(TilePane grid, int min) {
        double width = grid.getWidth() > 0 ? grid.getWidth() : 1000;
        int columns =
                width >= 4 * min + 3 * grid.getHgap()
                        ? 4
                        : width >= 2 * min + grid.getHgap() ? 2 : 1;
        grid.setPrefTileWidth(Math.floor((width - grid.getHgap() * (columns - 1)) / columns));
    }

    public static void sizeTiles(TilePane grid, int max, int min) {
        double width = grid.getWidth() > 0 ? grid.getWidth() : 1000;
        int columns =
                Math.max(
                        1,
                        Math.min(max, (int) ((width + grid.getHgap()) / (min + grid.getHgap()))));
        grid.setPrefTileWidth(Math.floor((width - grid.getHgap() * (columns - 1)) / columns));
    }

    public static <T> ComboBox<T> combo(java.util.List<T> values) {
        ComboBox<T> c = new ComboBox<>();
        c.getItems().setAll(values);
        c.getSelectionModel().selectFirst();
        c.setMaxWidth(Double.MAX_VALUE);
        return c;
    }

    public static HBox row(Node... nodes) {
        HBox b = new HBox(14, nodes);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }
}
