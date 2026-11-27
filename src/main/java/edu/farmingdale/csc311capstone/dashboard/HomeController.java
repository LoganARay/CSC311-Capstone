package edu.farmingdale.csc311capstone.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

public final class HomeController extends PageController {
    @FXML private FlowPane heroRow, filters;
    @FXML private StackPane featured, featuredArt, searchIcon;
    @FXML private TilePane quickStats, library;
    @FXML private TextField search;
    @FXML private ComboBox<String> sort;
    @FXML private Label resultCount;
    @FXML private VBox noResults, recent;
    private String category = "All games";

    @FXML
    private void initialize() {
        featuredArt.getChildren().add(new ArcadeArt("pacman"));
        searchIcon.getChildren().add(Icons.of("search"));
        quickStats
                .getChildren()
                .setAll(
                        Ui.stat("game", "Games played", "—", "Account history unavailable"),
                        Ui.stat("star", "Personal bests", "—", "Your records, per game"),
                        Ui.stat("clock", "Time played", "—", "Account history unavailable"),
                        Ui.stat("trophy", "Total points", "—", "No cross-game scoring defined"));
        sort.getItems().setAll("Featured order", "Name A–Z", "Name Z–A");
        sort.getSelectionModel().selectFirst();
        ToggleGroup group = new ToggleGroup();
        for (String value : GameCatalog.CATEGORIES) {
            ToggleButton b = new ToggleButton(value);
            b.setToggleGroup(group);
            b.getStyleClass().add("filter");
            b.setId("filter-" + value.toLowerCase().replace(' ', '-'));
            b.setSelected(value.equals(category));
            b.setOnAction(
                    e -> {
                        if (!b.isSelected()) b.setSelected(true);
                        category = value;
                        refresh();
                    });
            filters.getChildren().add(b);
        }
        search.textProperty().addListener((o, a, b) -> refresh());
        sort.valueProperty().addListener((o, a, b) -> refresh());
        library.widthProperty().addListener((o, a, b) -> Ui.sizeTiles(library, 4, 205));
        heroRow.widthProperty().addListener((o, a, b) -> resizeHero(b.doubleValue()));
        noResults
                .getChildren()
                .add(Ui.empty("search", "No games found", "Try another title or category."));
        noResults
                .getChildren()
                .add(
                        Ui.button(
                                "Reset filters",
                                () -> {
                                    category = "All games";
                                    ((ToggleButton) filters.getChildren().getFirst())
                                            .setSelected(true);
                                    search.clear();
                                    refresh();
                                }));
    }

    private void resizeHero(double width) {
        boolean wide = width >= 1020;
        double featureWidth = wide ? Math.floor((width - 24) * .63) : Math.floor(width);
        featured.setPrefWidth(featureWidth);
        featured.setMaxWidth(featureWidth);
        featuredArt.setPrefWidth(featureWidth * .48);
        featuredArt.setVisible(featureWidth > 520);
        featuredArt.setManaged(featureWidth > 520);
        quickStats.setPrefWidth(wide ? Math.floor(width - featureWidth - 24) : Math.floor(width));
        quickStats.setPrefColumns(wide ? 2 : 4);
        int columns = wide ? 2 : (width < 650 ? 2 : 4);
        quickStats.setPrefTileWidth(
                Math.floor((quickStats.getPrefWidth() - (columns - 1) * 12) / columns));
    }

    @Override
    protected void onShown() {
        if (recent.getChildren().isEmpty())
            recent.getChildren()
                    .add(
                            Ui.card(
                                    null,
                                    Ui.empty(
                                            "clock",
                                            "Your next session starts here",
                                            "Sign in when accounts are available to see scores,"
                                                + " records, and replay history.")));
        refresh();
    }

    private void refresh() {
        var games = GameCatalog.search(search.getText(), category, sort.getValue());
        library.getChildren()
                .setAll(games.stream().map(g -> new GameCard(g, () -> shell.launch(g))).toList());
        library.setManaged(!games.isEmpty());
        library.setVisible(!games.isEmpty());
        noResults.setVisible(games.isEmpty());
        noResults.setManaged(games.isEmpty());
        resultCount.setText(games.size() + " of 8 games");
        Ui.sizeTiles(library, 4, 205);
    }

    @FXML
    private void clearSearch() {
        search.clear();
        search.requestFocus();
    }

    @FXML
    private void playFeatured() {
        shell.launch(GameCatalog.byId("pacman"));
    }

    @FXML
    private void surprise() {
        shell.launch(GameCatalog.random(new Random()));
    }

    @FXML
    private void viewHistory() {
        shell.showPage("Statistics");
    }
}
