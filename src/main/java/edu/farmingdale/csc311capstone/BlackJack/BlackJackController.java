package edu.farmingdale.csc311capstone.BlackJack;

import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Card;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Hand;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Mode;
import edu.farmingdale.csc311capstone.SceneSwitcher;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Connects blackjack-view.fxml to {@link BlackJackMechanics}.
 * Every button calls one mechanics method and then {@link #refresh()} redraws everything.
 */
public class BlackJackController {

    private static final double CARD_WIDTH = 72;
    private static final double CARD_HEIGHT = 104;

    @FXML private BorderPane root;
    @FXML private ComboBox<Mode> modeBox;
    @FXML private Label modeDescriptionLabel;
    @FXML private Label chipsLabel;
    @FXML private Label minBetLabel;
    @FXML private Label roundLabel;
    @FXML private Label dealerLabel;
    @FXML private HBox dealerCards;
    @FXML private Label messageLabel;
    @FXML private VBox timerBox;
    @FXML private ProgressBar timerBar;
    @FXML private Label timerLabel;
    @FXML private HBox playerCards;
    @FXML private Label playerLabel;
    @FXML private Spinner<Integer> betSpinner;
    @FXML private Button dealButton;
    @FXML private Button hitButton;
    @FXML private Button standButton;
    @FXML private Button doubleButton;
    @FXML private Label statsLabel;

    private BlackJackMechanics game;
    private long shotClockDeadline;
    private double shotClockSeconds;

    private final AnimationTimer shotClock = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double left = (shotClockDeadline - now) / 1e9;
            if (left <= 0) {
                stop();
                game.timeExpired();
                refresh();
                return;
            }
            timerBar.setProgress(left / shotClockSeconds);
            timerLabel.setText(String.format("%.1fs", left));
            timerBar.setStyle(left < 3 ? "-fx-accent: #e74c3c;" : "-fx-accent: #f1c40f;");
        }
    };

    @FXML
    private void initialize() {
        modeBox.getItems().setAll(Mode.values());
        modeBox.getSelectionModel().select(Mode.CLASSIC);
        modeBox.valueProperty().addListener((obs, oldMode, newMode) -> onNewGame());

        betSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 500, 25, 5));
        betSpinner.setEditable(true);

        // Stop the shot clock if the player leaves this screen.
        root.sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((o2, oldWin, win) -> {
                    if (win == null) shotClock.stop();
                });
            }
        });

        onNewGame();
    }

    // ------------------------------------------------------------------ buttons

    @FXML
    private void onNewGame() {
        shotClock.stop();
        game = new BlackJackMechanics(modeBox.getValue());
        int startingBet = Math.max(game.minimumBet(), 25);
        betSpinner.getValueFactory().setValue(startingBet);
        refresh();
    }

    @FXML
    private void onDeal() {
        int bet = readBet();
        try {
            game.deal(bet);
        } catch (IllegalArgumentException | IllegalStateException e) {
            messageLabel.setText(e.getMessage());
            return;
        }
        startShotClockIfNeeded();
        refresh();
    }

    @FXML
    private void onHit() {
        game.hit();
        afterAction();
    }

    @FXML
    private void onStand() {
        game.stand();
        afterAction();
    }

    @FXML
    private void onDouble() {
        game.doubleDown();
        afterAction();
    }

    @FXML
    private void onMainMenu() {
        shotClock.stop();
        SceneSwitcher.showMenu(root);
    }

    private void afterAction() {
        if (game.state() != BlackJackMechanics.State.PLAYER_TURN) {
            shotClock.stop();
        } else if (game.usesShotClock()) {
            startShotClockIfNeeded(); // fresh clock for every decision
        }
        refresh();
    }

    private void startShotClockIfNeeded() {
        if (game.usesShotClock() && game.state() == BlackJackMechanics.State.PLAYER_TURN) {
            shotClockSeconds = game.turnSeconds();
            shotClockDeadline = System.nanoTime() + (long) (shotClockSeconds * 1e9);
            shotClock.start();
        }
    }

    /** Reads the bet, including a value typed into the spinner that hasn't been committed yet. */
    private int readBet() {
        try {
            int typed = Integer.parseInt(betSpinner.getEditor().getText().trim());
            betSpinner.getValueFactory().setValue(typed);
            return typed;
        } catch (NumberFormatException e) {
            return betSpinner.getValue();
        }
    }

    // ------------------------------------------------------------------ drawing

    private void refresh() {
        BlackJackMechanics.State state = game.state();
        boolean playerTurn = state == BlackJackMechanics.State.PLAYER_TURN;
        boolean canBet = state == BlackJackMechanics.State.BETTING
                || state == BlackJackMechanics.State.ROUND_OVER;

        modeDescriptionLabel.setText(game.mode().description());
        chipsLabel.setText("Chips: $" + game.chips());
        minBetLabel.setText("Min bet: $" + game.minimumBet());
        roundLabel.setText(game.mode() == Mode.SURVIVAL
                ? "Round " + (game.roundsCompleted() + 1) + "  |  Goal: $" + BlackJackMechanics.SURVIVAL_TARGET
                : "Round " + (game.roundsCompleted() + 1));

        // Cards
        dealerCards.getChildren().clear();
        var dealer = game.dealerHand().cards();
        for (int i = 0; i < dealer.size(); i++) {
            boolean hidden = i == 1 && game.isDealerHoleCardHidden();
            dealerCards.getChildren().add(hidden ? cardBack() : cardFace(dealer.get(i)));
        }
        playerCards.getChildren().clear();
        for (Card c : game.playerHand().cards()) {
            playerCards.getChildren().add(cardFace(c));
        }

        if (dealer.isEmpty()) {
            dealerLabel.setText("Dealer");
        } else if (game.isDealerHoleCardHidden()) {
            dealerLabel.setText("Dealer shows " + dealer.get(0).rank().value());
        } else {
            dealerLabel.setText("Dealer: " + game.dealerHand().value());
        }

        Hand player = game.playerHand();
        if (player.size() == 0) {
            playerLabel.setText("You");
        } else {
            String soft = player.isSoft() && !player.isBlackjack() ? "soft " : "";
            String bet = "  |  Bet: $" + game.currentBet() + (game.isDoubled() ? " (doubled)" : "");
            playerLabel.setText("You: " + soft + player.value() + bet);
        }

        messageLabel.setText(game.message());
        messageLabel.setTextFill(colorFor(game.lastOutcome(), state));

        // Shot clock only shows in Speed mode
        timerBox.setVisible(game.usesShotClock());
        timerBox.setManaged(game.usesShotClock());
        if (game.usesShotClock() && !playerTurn) {
            timerBar.setProgress(1);
            timerLabel.setText(String.format("%.1fs per decision", game.turnSeconds()));
            timerBar.setStyle("-fx-accent: #f1c40f;");
        }

        // Buttons
        dealButton.setDisable(!canBet);
        betSpinner.setDisable(!canBet);
        hitButton.setDisable(!playerTurn);
        standButton.setDisable(!playerTurn);
        doubleButton.setDisable(!game.canDoubleDown());
        dealButton.setText(state == BlackJackMechanics.State.ROUND_OVER ? "Next Hand" : "Deal");

        if (canBet) {
            var factory = (SpinnerValueFactory.IntegerSpinnerValueFactory) betSpinner.getValueFactory();
            factory.setMin(game.minimumBet());
            factory.setMax(Math.max(game.minimumBet(), game.chips()));
        }

        statsLabel.setText(String.format("W %d  /  L %d  /  P %d   |   Best: $%d",
                game.wins(), game.losses(), game.pushes(), game.peakChips()));
    }

    private Color colorFor(BlackJackMechanics.Outcome outcome, BlackJackMechanics.State state) {
        if (state == BlackJackMechanics.State.WON) return Color.GOLD;
        if (outcome == null) return Color.WHITE;
        return switch (outcome) {
            case PLAYER_BLACKJACK, PLAYER_WIN, DEALER_BUST -> Color.web("#7dff9b");
            case PUSH -> Color.web("#ffe08a");
            case DEALER_WIN, PLAYER_BUST -> Color.web("#ff8a80");
        };
    }

    private Node cardFace(Card card) {
        Rectangle bg = new Rectangle(CARD_WIDTH, CARD_HEIGHT, Color.WHITE);
        bg.setArcWidth(12);
        bg.setArcHeight(12);
        bg.setStroke(Color.web("#bbbbbb"));

        Color ink = card.suit().isRed() ? Color.web("#c0392b") : Color.web("#1b1b1b");

        Text corner = new Text(card.rank().label() + "\n" + card.suit().symbol());
        corner.setFont(Font.font("System", FontWeight.BOLD, 15));
        corner.setFill(ink);
        StackPane.setAlignment(corner, Pos.TOP_LEFT);
        StackPane.setMargin(corner, new javafx.geometry.Insets(5, 0, 0, 7));

        Text center = new Text(card.suit().symbol());
        center.setFont(Font.font(38));
        center.setFill(ink);

        StackPane pane = new StackPane(bg, corner, center);
        pane.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        return pane;
    }

    private Node cardBack() {
        Rectangle bg = new Rectangle(CARD_WIDTH, CARD_HEIGHT, Color.web("#1f4e9c"));
        bg.setArcWidth(12);
        bg.setArcHeight(12);
        bg.setStroke(Color.WHITE);
        bg.setStrokeWidth(3);
        Rectangle inner = new Rectangle(CARD_WIDTH - 16, CARD_HEIGHT - 16, Color.TRANSPARENT);
        inner.setStroke(Color.web("#8fb3ff"));
        inner.setArcWidth(8);
        inner.setArcHeight(8);
        Text mark = new Text("?");
        mark.setFont(Font.font("System", FontWeight.BOLD, 30));
        mark.setFill(Color.web("#8fb3ff"));
        return new StackPane(bg, inner, mark);
    }
}
