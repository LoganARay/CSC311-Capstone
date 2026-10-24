package edu.farmingdale.csc311capstone.BlackJack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * All of the BlackJack rules live here, with no JavaFX code, so the logic can be
 * unit tested on its own. The controller just calls these methods and redraws.
 *
 * Flow of a round:  BETTING -> deal(bet) -> PLAYER_TURN -> hit()/stand()/doubleDown()
 *                   -> dealer plays -> ROUND_OVER -> deal(...) again
 * The game ends in GAME_OVER (out of money) or WON (Survival target reached).
 */
public final class BlackJackMechanics {

    public enum State { BETTING, PLAYER_TURN, ROUND_OVER, GAME_OVER, WON }

    public enum Outcome { PLAYER_BLACKJACK, PLAYER_WIN, DEALER_BUST, PUSH, DEALER_WIN, PLAYER_BUST }

    // Tunable numbers for each mode
    public static final int STARTING_CHIPS = 500;
    public static final int SURVIVAL_STARTING_CHIPS = 100;
    public static final int SURVIVAL_TARGET = 1000;
    public static final int BASE_MIN_BET = 10;
    public static final int SURVIVAL_MIN_BET_STEP = 5;
    public static final double SPEED_START_SECONDS = 10.0;
    public static final double SPEED_MIN_SECONDS = 3.0;
    public static final double SPEED_SECONDS_LOST_PER_ROUND = 0.5;
    public static final int SPEED_BONUS_PERCENT = 25;
    private static final int DECKS_IN_SHOE = 6;

    private final Mode mode;
    private final Deck deck;
    private final Hand playerHand = new Hand();
    private final Hand dealerHand = new Hand();

    private State state = State.BETTING;
    private Outcome lastOutcome;
    private String message;
    private int chips;
    private int currentBet;
    private boolean doubled;

    private int roundsCompleted;
    private int wins;
    private int losses;
    private int pushes;
    private int peakChips;

    public BlackJackMechanics(Mode mode) {
        this(mode, new Deck(DECKS_IN_SHOE, new Random()));
    }

    /** Lets tests supply a deck with known cards. */
    BlackJackMechanics(Mode mode, Deck deck) {
        this.mode = mode;
        this.deck = deck;
        this.chips = mode == Mode.SURVIVAL ? SURVIVAL_STARTING_CHIPS : STARTING_CHIPS;
        this.peakChips = chips;
        this.message = "Place your bet. Minimum is $" + minimumBet() + ".";
    }

    // ------------------------------------------------------------------ actions

    /** Takes the bet and deals two cards each. */
    public void deal(int bet) {
        if (state != State.BETTING && state != State.ROUND_OVER) {
            throw new IllegalStateException("Can't deal right now: " + state);
        }
        if (bet < minimumBet()) {
            throw new IllegalArgumentException("Minimum bet is $" + minimumBet());
        }
        if (bet > chips) {
            throw new IllegalArgumentException("You only have $" + chips);
        }

        if (deck.needsReshuffle()) {
            deck.shuffle();
        }
        playerHand.clear();
        dealerHand.clear();
        doubled = false;
        lastOutcome = null;

        currentBet = bet;
        chips -= bet;

        playerHand.add(deck.draw());
        dealerHand.add(deck.draw());
        playerHand.add(deck.draw());
        dealerHand.add(deck.draw());

        state = State.PLAYER_TURN;
        message = "Hit or stand?";

        // Naturals end the round right away (the dealer "peeks" for blackjack).
        if (playerHand.isBlackjack() || dealerHand.isBlackjack()) {
            settle();
        }
    }

    public void hit() {
        requirePlayerTurn();
        playerHand.add(deck.draw());
        if (playerHand.isBust()) {
            settle();
        } else if (playerHand.value() == 21) {
            stand(); // nothing better to do on 21
        }
    }

    public void stand() {
        requirePlayerTurn();
        playDealer();
        settle();
    }

    /** Double the bet, take exactly one card, then stand. */
    public void doubleDown() {
        if (!canDoubleDown()) {
            throw new IllegalStateException("Can't double down now");
        }
        chips -= currentBet;
        currentBet *= 2;
        doubled = true;
        playerHand.add(deck.draw());
        if (playerHand.isBust()) {
            settle();
        } else {
            playDealer();
            settle();
        }
    }

    /** Speed mode: the shot clock ran out, so the player stands. */
    public void timeExpired() {
        if (state == State.PLAYER_TURN) {
            stand();
            message = "Time's up! " + message;
        }
    }

    // ------------------------------------------------------------------ rules

    private void playDealer() {
        // Dealer stands on all 17s (including soft 17).
        while (dealerHand.value() < 17) {
            dealerHand.add(deck.draw());
        }
    }

    private void settle() {
        int player = playerHand.value();
        int dealer = dealerHand.value();
        boolean tiesGoToDealer = mode == Mode.DOUBLE_EXPOSURE;

        if (playerHand.isBust()) {
            lastOutcome = Outcome.PLAYER_BUST;
        } else if (playerHand.isBlackjack() && dealerHand.isBlackjack()) {
            lastOutcome = tiesGoToDealer ? Outcome.DEALER_WIN : Outcome.PUSH;
        } else if (playerHand.isBlackjack()) {
            lastOutcome = Outcome.PLAYER_BLACKJACK;
        } else if (dealerHand.isBlackjack()) {
            lastOutcome = Outcome.DEALER_WIN;
        } else if (dealerHand.isBust()) {
            lastOutcome = Outcome.DEALER_BUST;
        } else if (player > dealer) {
            lastOutcome = Outcome.PLAYER_WIN;
        } else if (player < dealer) {
            lastOutcome = Outcome.DEALER_WIN;
        } else {
            lastOutcome = tiesGoToDealer ? Outcome.DEALER_WIN : Outcome.PUSH;
        }

        int payout = payoutFor(lastOutcome);
        chips += payout;
        int net = payout - currentBet;

        switch (lastOutcome) {
            case PLAYER_BLACKJACK, PLAYER_WIN, DEALER_BUST -> wins++;
            case PUSH -> pushes++;
            default -> losses++;
        }
        roundsCompleted++;
        peakChips = Math.max(peakChips, chips);
        message = describe(lastOutcome, player, dealer, net);

        if (mode == Mode.SURVIVAL && chips >= SURVIVAL_TARGET) {
            state = State.WON;
            message += " You reached $" + SURVIVAL_TARGET + " - you survived!";
        } else if (chips < minimumBet()) {
            state = State.GAME_OVER;
            message += " You can't cover the $" + minimumBet() + " minimum. Game over.";
        } else {
            state = State.ROUND_OVER;
        }
    }

    /** Total chips handed back to the player (includes the original bet). */
    int payoutFor(Outcome outcome) {
        int bet = currentBet;
        int payout = switch (outcome) {
            case PLAYER_BLACKJACK -> mode == Mode.DOUBLE_EXPOSURE
                    ? bet * 2              // 1:1
                    : bet + (bet * 3) / 2; // 3:2
            case PLAYER_WIN, DEALER_BUST -> bet * 2;
            case PUSH -> bet;
            case DEALER_WIN, PLAYER_BUST -> 0;
        };
        boolean won = payout > bet;
        if (mode == Mode.SPEED && won) {
            payout += (bet * SPEED_BONUS_PERCENT) / 100;
        }
        return payout;
    }

    private String describe(Outcome outcome, int player, int dealer, int net) {
        String money = net > 0 ? " +$" + net : net < 0 ? " -$" + (-net) : " (bet returned)";
        return switch (outcome) {
            case PLAYER_BLACKJACK -> "Blackjack!" + money;
            case PLAYER_WIN -> "You win " + player + " to " + dealer + "." + money;
            case DEALER_BUST -> "Dealer busts with " + dealer + ". You win!" + money;
            case PUSH -> "Push at " + player + "." + money;
            case DEALER_WIN -> (dealerHand.isBlackjack() ? "Dealer has blackjack."
                    : player == dealer ? "Tie goes to the dealer at " + player + "."
                    : "Dealer wins " + dealer + " to " + player + ".") + money;
            case PLAYER_BUST -> "Bust with " + player + "." + money;
        };
    }

    private void requirePlayerTurn() {
        if (state != State.PLAYER_TURN) {
            throw new IllegalStateException("It's not your turn");
        }
    }

    // ------------------------------------------------------------------ queries

    public int minimumBet() {
        if (mode == Mode.SURVIVAL) {
            return BASE_MIN_BET + SURVIVAL_MIN_BET_STEP * roundsCompleted;
        }
        return BASE_MIN_BET;
    }

    /** Seconds on the shot clock for the current round (Speed mode only). */
    public double turnSeconds() {
        double seconds = SPEED_START_SECONDS - SPEED_SECONDS_LOST_PER_ROUND * roundsCompleted;
        return Math.max(SPEED_MIN_SECONDS, seconds);
    }

    public boolean usesShotClock() {
        return mode == Mode.SPEED;
    }

    public boolean canDoubleDown() {
        return state == State.PLAYER_TURN && playerHand.size() == 2 && chips >= currentBet;
    }

    /** Should the dealer's second card be drawn face down? */
    public boolean isDealerHoleCardHidden() {
        return state == State.PLAYER_TURN && mode != Mode.DOUBLE_EXPOSURE;
    }

    public boolean isGameFinished() {
        return state == State.GAME_OVER || state == State.WON;
    }

    public Mode mode() { return mode; }
    public State state() { return state; }
    public Outcome lastOutcome() { return lastOutcome; }
    public String message() { return message; }
    public Hand playerHand() { return playerHand; }
    public Hand dealerHand() { return dealerHand; }
    public int chips() { return chips; }
    public int currentBet() { return currentBet; }
    public boolean isDoubled() { return doubled; }
    public int roundsCompleted() { return roundsCompleted; }
    public int wins() { return wins; }
    public int losses() { return losses; }
    public int pushes() { return pushes; }
    public int peakChips() { return peakChips; }

    // ================================================================== game modes

    /**
     * The different ways to play Blackjack.
     */
    public enum Mode {

        CLASSIC("Classic",
                "Standard rules. Blackjack pays 3:2, dealer stands on soft 17. "
                        + "You can double down on your first two cards."),

        SPEED("Speed",
                "Same rules, but you're on a shot clock. Run out of time and you automatically stand. "
                        + "The clock gets shorter every round. Wins pay a 25% speed bonus."),

        DOUBLE_EXPOSURE("Double Exposure",
                "Both dealer cards are dealt face up so you know exactly what you're up against. "
                        + "The catch: blackjack only pays 1:1 and the dealer wins all ties."),

        SURVIVAL("Survival",
                "Start with $100 and grow it to $1,000. The minimum bet goes up every round, "
                        + "so you can't play it safe forever. Drop below the minimum and you're out.");

        private final String displayName;
        private final String description;

        Mode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String displayName() { return displayName; }
        public String description() { return description; }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // ================================================================== cards

    /**
     * A single playing card. Immutable.
     */
    public record Card(Rank rank, Suit suit) {

        public enum Suit {
            HEARTS("♥", true),
            DIAMONDS("♦", true),
            CLUBS("♣", false),
            SPADES("♠", false);

            private final String symbol;
            private final boolean red;

            Suit(String symbol, boolean red) {
                this.symbol = symbol;
                this.red = red;
            }

            public String symbol() { return symbol; }
            public boolean isRed() { return red; }
        }

        public enum Rank {
            ACE("A", 11), TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5),
            SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), TEN("10", 10),
            JACK("J", 10), QUEEN("Q", 10), KING("K", 10);

            private final String label;
            private final int value;

            Rank(String label, int value) {
                this.label = label;
                this.value = value;
            }

            public String label() { return label; }

            /** Blackjack value. Aces count as 11 here; {@link Hand} drops them to 1 when needed. */
            public int value() { return value; }
        }

        @Override
        public String toString() {
            return rank.label() + suit.symbol();
        }
    }

    /**
     * A shoe made of one or more standard 52-card decks.
     * Reshuffles automatically when it runs low, like a real casino shoe.
     */
    public static final class Deck {

        private final int numberOfDecks;
        private final Random random;
        private final List<Card> cards = new ArrayList<>();

        public Deck(int numberOfDecks, Random random) {
            if (numberOfDecks < 1) {
                throw new IllegalArgumentException("Need at least one deck");
            }
            this.numberOfDecks = numberOfDecks;
            this.random = random;
            shuffle();
        }

        /** Puts every card back in the shoe and shuffles. */
        public void shuffle() {
            cards.clear();
            for (int d = 0; d < numberOfDecks; d++) {
                for (Card.Suit suit : Card.Suit.values()) {
                    for (Card.Rank rank : Card.Rank.values()) {
                        cards.add(new Card(rank, suit));
                    }
                }
            }
            Collections.shuffle(cards, random);
        }

        /** True when fewer than 25% of the cards are left. Checked between rounds. */
        public boolean needsReshuffle() {
            return cards.size() < (numberOfDecks * 52) / 4;
        }

        public Card draw() {
            if (cards.isEmpty()) {
                shuffle();
            }
            return cards.remove(cards.size() - 1);
        }

        public int remaining() {
            return cards.size();
        }

        /** Test helper: puts a card on top so the next draw returns it. */
        void stackOnTop(Card card) {
            cards.add(card);
        }
    }

    /**
     * A blackjack hand. Handles soft/hard totals for aces.
     */
    public static final class Hand {

        private final List<Card> cards = new ArrayList<>();

        public void add(Card card) {
            cards.add(card);
        }

        public void clear() {
            cards.clear();
        }

        public List<Card> cards() {
            return Collections.unmodifiableList(cards);
        }

        public int size() {
            return cards.size();
        }

        /** Best total: aces count 11 unless that would bust, then 1. */
        public int value() {
            int total = 0;
            int aces = 0;
            for (Card c : cards) {
                total += c.rank().value();
                if (c.rank() == Card.Rank.ACE) {
                    aces++;
                }
            }
            while (total > 21 && aces > 0) {
                total -= 10;
                aces--;
            }
            return total;
        }

        /** A soft hand still has an ace counted as 11 (e.g. A+6 = soft 17). */
        public boolean isSoft() {
            int hardTotal = 0;
            boolean hasAce = false;
            for (Card c : cards) {
                hardTotal += c.rank() == Card.Rank.ACE ? 1 : c.rank().value();
                if (c.rank() == Card.Rank.ACE) {
                    hasAce = true;
                }
            }
            return hasAce && hardTotal + 10 <= 21;
        }

        public boolean isBlackjack() {
            return cards.size() == 2 && value() == 21;
        }

        public boolean isBust() {
            return value() > 21;
        }

        @Override
        public String toString() {
            return cards + " = " + value();
        }
    }
}
