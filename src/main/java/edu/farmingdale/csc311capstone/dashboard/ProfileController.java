package edu.farmingdale.csc311capstone.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class ProfileController extends PageController {
    @FXML private TilePane summary, recordsAndFavorites;
    @FXML private VBox recent;

    @FXML
    private void initialize() {
        Ui.statGrid(summary);
        VBox records = Ui.card("Personal Records · All 8 Games");
        records.setSpacing(4);
        for (GameDefinition game : GameCatalog.GAMES) {
            ArcadeArt art = new ArcadeArt(game.id());
            art.setPrefSize(52, 30);
            art.setMaxSize(52, 30);
            Label title = Ui.label(game.title(), "secondary");
            title.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);
            HBox row = Ui.row(art, title, Ui.label("—", "purple"));
            row.getStyleClass().add("record-row");
            records.getChildren().add(row);
        }
        records.getChildren()
                .add(
                        Ui.label(
                                "Game and mode records will appear with account history.",
                                "small,muted"));
        VBox favorites =
                Ui.card(
                        "Your Favorite Games",
                        Ui.empty(
                                "game",
                                "Room for a new favorite",
                                "Your most-played games will appear here once account history is"
                                    + " connected."),
                        new Separator(),
                        Ui.label("A place for your progress", "section-title"),
                        Ui.label(
                                "Player levels and achievements are not available yet. Your profile"
                                    + " will focus on actual scores, records, and game history.",
                                "secondary"));
        recordsAndFavorites.getChildren().addAll(records, favorites);
        recordsAndFavorites
                .widthProperty()
                .addListener((o, a, b) -> Ui.sizeTiles(recordsAndFavorites, 2, 320));
        recent.getChildren()
                .add(
                        Ui.empty(
                                "clock",
                                "No activity connected",
                                "You can explore all eight games as a guest."));
    }

    @Override
    protected void onShown() {}

    @FXML
    private void signIn() {
        shell.openAccount();
    }

    @FXML
    private void history() {
        shell.showPage("Statistics");
    }
}
