package com.khokhar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RuleValidatorTest {

    private Player p1;
    private Trick trick;

    @BeforeEach
    public void setUp() {
        p1 = new Player("Player 1");
        trick = new Trick();
    }

    @Test
    public void testFirstCardAlwaysValidIfInHand() {
        Card s5 = new Card(Symbol.SPADES, Rank.FIVE);
        p1.addCard(s5);

        assertTrue(RuleValidator.isValidPlay(p1, s5, Symbol.HEARTS, trick));
    }

    @Test
    public void testCantPlayCardNotInHand() {
        Card s5 = new Card(Symbol.SPADES, Rank.FIVE);
        assertFalse(RuleValidator.isValidPlay(p1, s5, Symbol.HEARTS, trick));
    }

    @Test
    public void testMustFollowStartingSymbol() {
        Card s5 = new Card(Symbol.SPADES, Rank.FIVE);
        Card h10 = new Card(Symbol.HEARTS, Rank.TEN);
        
        Player lead = new Player("Lead");
        lead.addCard(s5);
        trick.playCard(lead, s5, Symbol.CLUBS); // Lead plays Spade 5

        p1.addCard(s5); // has Spade
        p1.addCard(h10); // has Heart

        // Must play Spade if they have it
        assertTrue(RuleValidator.isValidPlay(p1, s5, Symbol.CLUBS, trick));
        assertFalse(RuleValidator.isValidPlay(p1, h10, Symbol.CLUBS, trick));
    }

    @Test
    public void testMustHeadWhenNoTrumpPlayed() {
        Card leadCard = new Card(Symbol.SPADES, Rank.FIVE);
        Player lead = new Player("Lead");
        lead.addCard(leadCard);
        trick.playCard(lead, leadCard, Symbol.CLUBS);

        Card s2 = new Card(Symbol.SPADES, Rank.TWO);
        Card s10 = new Card(Symbol.SPADES, Rank.TEN);
        p1.addCard(s2);
        p1.addCard(s10);

        // Player has s10 which beats s5. Player cannot play s2!
        assertFalse(RuleValidator.isValidPlay(p1, s2, Symbol.CLUBS, trick));
        assertTrue(RuleValidator.isValidPlay(p1, s10, Symbol.CLUBS, trick));
    }

    @Test
    public void testCanPlayAnythingIfNoStartingAndNoTrump() {
        Card leadCard = new Card(Symbol.SPADES, Rank.FIVE);
        Player lead = new Player("Lead");
        lead.addCard(leadCard);
        trick.playCard(lead, leadCard, Symbol.CLUBS); // Trump is CLUBS, Lead played SPADES

        Card h2 = new Card(Symbol.HEARTS, Rank.TWO);
        Card d9 = new Card(Symbol.DIAMONDS, Rank.NINE);
        p1.addCard(h2);
        p1.addCard(d9);

        // Player has no Spades and no Clubs (trump), can play anything
        assertTrue(RuleValidator.isValidPlay(p1, h2, Symbol.CLUBS, trick));
        assertTrue(RuleValidator.isValidPlay(p1, d9, Symbol.CLUBS, trick));
    }
}
