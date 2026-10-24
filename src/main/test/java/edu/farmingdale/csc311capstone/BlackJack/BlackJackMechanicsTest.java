package edu.farmingdale.csc311capstone.BlackJack;

import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Card;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Card.Rank;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Card.Suit;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Deck;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Hand;
import edu.farmingdale.csc311capstone.BlackJack.BlackJackMechanics.Mode;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BlackJackMechanicsTest {

    /**
     * Builds a game whose deck deals the given ranks in order.
     * Deal order is: player, dealer, player, dealer, then hits.
     */
    private static BlackJackMechanics rigged(Mode mode, Rank... ranks) {
        Deck deck = new Deck(6, new Random(1));
        for (int i = ranks.length - 1; i >= 0; i--) {
            deck.stackOnTop(new Card(ranks[i], Suit.SPADES));
        }
        return new BlackJackMechanics(mode, deck);
    }

    @Test
    void acesSwitchFromElevenToOne() {
        Hand hand = new Hand();
        hand.add(new Card(Rank.ACE, Suit.HEARTS));
        hand.add(new Card(Rank.SIX, Suit.HEARTS));
        assertEquals(17, hand.value());
        assertTrue(hand.isSoft());

        hand.add(new Card(Rank.NINE, Suit.HEARTS));
        assertEquals(16, hand.value());
        assertFalse(hand.isSoft());

        Hand twoAces = new Hand();
        twoAces.add(new Card(Rank.ACE, Suit.HEARTS));
        twoAces.add(new Card(Rank.ACE, Suit.CLUBS));
        assertEquals(12, twoAces.value());
    }

    @Test
    void naturalBlackjackPaysThreeToTwoInClassic() {
        // player A, dealer 9, player K, dealer 7
        BlackJackMechanics game = rigged(Mode.CLASSIC, Rank.ACE, Rank.NINE, Rank.KING, Rank.SEVEN);
        game.deal(100);
        assertEquals(BlackJackMechanics.Outcome.PLAYER_BLACKJACK, game.lastOutcome());
        assertEquals(500 + 150, game.chips());
        assertEquals(BlackJackMechanics.State.ROUND_OVER, game.state());
    }

    @Test
    void blackjackOnlyPaysEvenMoneyInDoubleExposure() {
        BlackJackMechanics game = rigged(Mode.DOUBLE_EXPOSURE, Rank.ACE, Rank.NINE, Rank.KING, Rank.SEVEN);
        game.deal(100);
        assertEquals(500 + 100, game.chips());
    }

    @Test
    void tiesPushInClassicButLoseInDoubleExposure() {
        // player 10+8 = 18, dealer 10+8 = 18
        BlackJackMechanics classic = rigged(Mode.CLASSIC, Rank.TEN, Rank.TEN, Rank.EIGHT, Rank.EIGHT);
        classic.deal(50);
        classic.stand();
        assertEquals(BlackJackMechanics.Outcome.PUSH, classic.lastOutcome());
        assertEquals(500, classic.chips());

        BlackJackMechanics exposed = rigged(Mode.DOUBLE_EXPOSURE, Rank.TEN, Rank.TEN, Rank.EIGHT, Rank.EIGHT);
        exposed.deal(50);
        assertFalse(exposed.isDealerHoleCardHidden());
        exposed.stand();
        assertEquals(BlackJackMechanics.Outcome.DEALER_WIN, exposed.lastOutcome());
        assertEquals(450, exposed.chips());
    }

    @Test
    void dealerHitsUntilSeventeen() {
        // player 10+9 = 19, dealer 10+2 = 12 then draws 3, 4 -> 19... use 5 to reach 17
        BlackJackMechanics game = rigged(Mode.CLASSIC,
                Rank.TEN, Rank.TEN, Rank.NINE, Rank.TWO, Rank.FIVE);
        game.deal(20);
        assertTrue(game.isDealerHoleCardHidden());
        game.stand();
        assertEquals(17, game.dealerHand().value());
        assertEquals(BlackJackMechanics.Outcome.PLAYER_WIN, game.lastOutcome());
        assertEquals(520, game.chips());
    }

    @Test
    void bustingLosesTheBet() {
        // player 10+6 = 16, hits a King -> 26
        BlackJackMechanics game = rigged(Mode.CLASSIC,
                Rank.TEN, Rank.TEN, Rank.SIX, Rank.SEVEN, Rank.KING);
        game.deal(40);
        game.hit();
        assertEquals(BlackJackMechanics.Outcome.PLAYER_BUST, game.lastOutcome());
        assertEquals(460, game.chips());
        assertEquals(2, game.dealerHand().size(), "dealer shouldn't draw when the player busts");
    }

    @Test
    void doubleDownDoublesBetAndTakesOneCard() {
        // player 6+5 = 11, double into a 10 = 21; dealer 10+7 = 17
        BlackJackMechanics game = rigged(Mode.CLASSIC,
                Rank.SIX, Rank.TEN, Rank.FIVE, Rank.SEVEN, Rank.TEN);
        game.deal(50);
        assertTrue(game.canDoubleDown());
        game.doubleDown();
        assertEquals(3, game.playerHand().size());
        assertEquals(100, game.currentBet());
        assertEquals(500 + 100, game.chips());
    }

    @Test
    void speedModeAddsBonusAndClockShrinks() {
        BlackJackMechanics game = rigged(Mode.SPEED,
                Rank.TEN, Rank.TEN, Rank.NINE, Rank.SEVEN);
        assertEquals(BlackJackMechanics.SPEED_START_SECONDS, game.turnSeconds());
        game.deal(100);
        game.timeExpired(); // auto-stand on 19 vs 17
        assertEquals(BlackJackMechanics.Outcome.PLAYER_WIN, game.lastOutcome());
        assertEquals(500 + 100 + 25, game.chips());
        assertTrue(game.message().startsWith("Time's up!"));
        assertTrue(game.turnSeconds() < BlackJackMechanics.SPEED_START_SECONDS);
    }

    @Test
    void survivalMinimumBetRisesAndGameEnds() {
        BlackJackMechanics game = rigged(Mode.SURVIVAL,
                Rank.TEN, Rank.TEN, Rank.SIX, Rank.TEN);   // 16 vs 20
        assertEquals(100, game.chips());
        assertEquals(10, game.minimumBet());
        assertThrows(IllegalArgumentException.class, () -> game.deal(5));

        game.deal(95);
        game.stand();
        assertEquals(5, game.chips());
        assertEquals(15, game.minimumBet());
        assertEquals(BlackJackMechanics.State.GAME_OVER, game.state());
        assertTrue(game.isGameFinished());
    }

    @Test
    void survivalBlackjackKeepsGameGoing() {
        BlackJackMechanics game = rigged(Mode.SURVIVAL,
                Rank.ACE, Rank.NINE, Rank.KING, Rank.SEVEN);
        game.deal(100); // all in: 100 -> 0 + 250 = 250
        assertEquals(250, game.chips());
        assertEquals(BlackJackMechanics.State.ROUND_OVER, game.state());
    }

    @Test
    void cannotActOutOfTurn() {
        BlackJackMechanics game = new BlackJackMechanics(Mode.CLASSIC);
        assertThrows(IllegalStateException.class, game::hit);
        assertThrows(IllegalStateException.class, game::stand);
        assertThrows(IllegalArgumentException.class, () -> game.deal(10_000));
    }

    @Test
    void randomGamesNeverLoseTrackOfMoney() {
        for (Mode mode : Mode.values()) {
            BlackJackMechanics game = new BlackJackMechanics(mode);
            Random r = new Random(42);
            int rounds = 0;
            while (!game.isGameFinished() && rounds++ < 300) {
                game.deal(game.minimumBet());
                while (game.state() == BlackJackMechanics.State.PLAYER_TURN) {
                    if (game.playerHand().value() < 15 && r.nextBoolean()) game.hit();
                    else game.stand();
                }
                assertTrue(game.chips() >= 0);
                assertFalse(game.playerHand().value() > 21 && game.lastOutcome() != BlackJackMechanics.Outcome.PLAYER_BUST);
            }
        }
    }
}
