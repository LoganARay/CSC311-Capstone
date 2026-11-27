package edu.farmingdale.csc311capstone.dashboard;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.function.Function;

public final class StatisticsController extends PageController {
    @FXML private TilePane summary, details, individualStats;
    @FXML private FlowPane filters, charts, individualFilters;
    @FXML private TableView<DashboardData.Match> history;

    @FXML
    private void initialize() {
        Ui.statGrid(summary);
        ComboBox<GameDefinition> game = Ui.combo(GameCatalog.GAMES);
        game.getSelectionModel().select(GameCatalog.byId("pacman"));
        game.setId("statistics-game");
        ComboBox<String> period = Ui.combo(List.of("Last 7 days", "Last 30 days", "All time"));
        period.getSelectionModel().select(1);
        ComboBox<String> mode = Ui.combo(List.of("Modes unavailable"));
        mode.setDisable(true);
        filters.getChildren()
                .addAll(
                        game,
                        mode,
                        period,
                        Ui.label("Scores stay within each game’s scale.", "small,muted"));
        NumberAxis y = new NumberAxis();
        y.setLabel("Score");
        y.setForceZeroInRange(true);
        CategoryAxis x = new CategoryAxis();
        x.setLabel("Session date");
        AreaChart<String, Number> line = new AreaChart<>(x, y);
        line.setLegendVisible(false);
        line.setAnimated(false);
        line.setPrefHeight(235);
        line.setMinHeight(235);
        VBox linePanel =
                Ui.card(
                        "Score Progression",
                        new StackPane(
                                line,
                                Ui.empty(
                                        "stats",
                                        "Your progress starts with a session",
                                        "No score history is connected for this game.")));
        Circle ring = new Circle(61, Color.TRANSPARENT);
        ring.setStroke(Color.web("#343047"));
        ring.setStrokeWidth(19);
        StackPane donut = new StackPane(ring, Ui.label("—\nGames", "secondary"));
        donut.setPrefHeight(155);
        VBox category =
                Ui.card(
                        "Games by Category",
                        donut,
                        Ui.label(
                                "No category history yet. Each session will count once using its"
                                    + " game’s primary category.",
                                "small,muted"));
        linePanel.setMinWidth(0);
        category.setMinWidth(0);
        charts.getChildren().addAll(linePanel, category);
        charts.widthProperty()
                .addListener(
                        (o, a, b) -> {
                            double w = b.doubleValue();
                            boolean wide = w > 820;
                            linePanel.setPrefWidth(
                                    wide ? Math.floor((w - 24) * .64) : Math.floor(w));
                            category.setPrefWidth(
                                    wide ? Math.floor((w - 24) * .36) : Math.floor(w));
                        });
        details.getChildren()
                .addAll(
                        Ui.card(
                                "Most-Played Games",
                                Ui.empty(
                                        "game",
                                        "Find your next favorite",
                                        "Play counts and playtime will appear with account"
                                            + " history.")),
                        Ui.card(
                                "Personal Bests",
                                Ui.empty(
                                        "star",
                                        "A fresh set of records",
                                        "Your best results will stay specific to each game and"
                                            + " mode.")));
        details.widthProperty().addListener((o, a, b) -> Ui.sizeTiles(details, 2, 320));
        ComboBox<GameDefinition> individual = Ui.combo(GameCatalog.GAMES);
        individual.setId("individual-game");
        individualFilters.getChildren().add(individual);
        individualStats
                .getChildren()
                .setAll(
                        Ui.stat("game", "Games played", "—", "History unavailable"),
                        Ui.stat("star", "Best score", "—", "Game-specific record"),
                        Ui.stat("stats", "Average score", "—", "Mode-specific, where meaningful"),
                        Ui.stat("clock", "Total playtime", "—", "History unavailable"));
        individualStats
                .widthProperty()
                .addListener((o, a, b) -> Ui.sizeStats(individualStats, 180));
        individualFilters
                .getChildren()
                .add(
                        Ui.label(
                                "Wins, losses and game-specific records appear when supported.",
                                "small,muted"));
        column("Game", 140, m -> GameCatalog.byId(m.gameId()).title());
        column("Mode", 120, DashboardData.Match::mode);
        column("Score", 90, m -> DisplayFormats.number(m.score()));
        column("Outcome", 110, m -> DisplayFormats.outcome(m.outcome()));
        column("Duration", 90, m -> DisplayFormats.duration(m.duration()));
        column("Date", 150, m -> m.playedAt().toString());
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        history.setPlaceholder(
                Ui.label("No match history connected. Your next chapter is waiting.", "secondary"));
    }

    private void column(String title, int width, Function<DashboardData.Match, String> value) {
        TableColumn<DashboardData.Match, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(r -> new ReadOnlyStringWrapper(value.apply(r.getValue())));
        history.getColumns().add(c);
    }

    @Override
    protected void onShown() {}
}
