package edu.farmingdale.csc311capstone.dashboard;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class GameCard extends VBox {
    public GameCard(GameDefinition game, Runnable launch) {
        getStyleClass().add("game-card");
        setId("card-" + game.id());
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);
        ArcadeArt art = new ArcadeArt(game.id());
        art.setPrefHeight(112);
        art.setMaxHeight(112);
        Label name = Ui.label(game.title(), "game-title");
        Label category =
                Ui.label(
                        game.id().equals("bomberman") ? "PREVIEW" : game.category().toUpperCase(),
                        "category-label");
        VBox words = new VBox(3, name, category);
        HBox.setHgrow(words, Priority.ALWAYS);
        Button play = Ui.button("▶  Play", launch, "play-button");
        play.setId("play-" + game.id());
        play.setAccessibleText("Play " + game.title());
        play.setMinWidth(64);
        HBox title = Ui.row(words, play);
        title.setAlignment(Pos.CENTER_LEFT);
        VBox copy = new VBox(7, title, Ui.label(game.description(), "card-description"));
        copy.getStyleClass().add("card-copy");
        getChildren().addAll(art, copy);
    }
}
