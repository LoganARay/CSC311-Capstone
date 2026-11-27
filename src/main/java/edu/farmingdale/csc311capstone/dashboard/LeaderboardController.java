package edu.farmingdale.csc311capstone.dashboard;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Function;

public final class LeaderboardController extends PageController {
    @FXML private FlowPane filterBar;
    @FXML private StackPane trophy;
    @FXML private TilePane podium;
    @FXML private TableView<DashboardData.Ranking> rankings;

    @FXML
    private void initialize() {
        Label art = Ui.label("", "avatar");
        art.setGraphic(Icons.of("trophy"));
        trophy.getChildren().add(art);
        ComboBox<String> game =
                Ui.combo(
                        java.util.stream.Stream.concat(
                                        java.util.stream.Stream.of("Overall · unavailable"),
                                        GameCatalog.GAMES.stream().map(GameDefinition::title))
                                .toList());
        game.setId("leaderboard-game");
        ComboBox<String> mode = Ui.combo(List.of("All modes · unavailable"));
        mode.setDisable(true);
        ComboBox<String> period = Ui.combo(List.of("All time · unavailable"));
        period.setDisable(true);
        filterBar.getChildren().addAll(game, mode, period);
        for (int rank : new int[] {2, 1, 3}) {
            VBox box =
                    new VBox(
                            9,
                            Ui.label(rank == 1 ? "01" : "0" + rank, "podium-number"),
                            Ui.label("Awaiting a challenger", "section-title"),
                            Ui.label("No ranking available", "secondary"));
            box.setAlignment(Pos.CENTER);
            box.getStyleClass()
                    .addAll("podium", rank == 1 ? "gold" : rank == 3 ? "bronze" : "silver");
            box.setMinWidth(0);
            box.setMaxWidth(Double.MAX_VALUE);
            podium.getChildren().add(box);
        }
        podium.widthProperty().addListener((o, a, b) -> Ui.sizeTiles(podium, 3, 205));
        column("Rank", 65, r -> "#" + r.rank());
        column("Player", 160, DashboardData.Ranking::username);
        column("Game", 130, r -> GameCatalog.byId(r.gameId()).title());
        column("Mode", 110, DashboardData.Ranking::mode);
        column("Score", 100, r -> DisplayFormats.number(r.score()));
        column("Date", 120, r -> r.date().toString());
        rankings.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        rankings.setPlaceholder(
                Ui.empty("trophy", "The leaderboard is waiting", "No rankings are available yet."));
    }

    private void column(String name, int width, Function<DashboardData.Ranking, String> value) {
        TableColumn<DashboardData.Ranking, String> col = new TableColumn<>(name);
        col.setPrefWidth(width);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        rankings.getColumns().add(col);
    }

    @Override
    protected void onShown() {}

    @FXML
    private void statistics() {
        shell.showPage("Statistics");
    }
}
