package com.khokhar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TrickTest {

    private Trick trick;
    private Player p1, p2, p3, p4;

    @BeforeEach
    public void setUp() {
        trick = new Trick();
        p1 = new Player("Alice");
        p2 = new Player("Bob");
        p3 = new Player("Charlie");
        p4 = new Player("Diana");
    }

    @Test
    public void testHighestStartingSymbolWins() {
        Symbol trump = Symbol.CLUBS;

        trick.playCard(p1, new Card(Symbol.SPADES, Rank.FIVE), trump);
        trick.playCard(p2, new Card(Symbol.SPADES, Rank.TEN), trump);
        trick.playCard(p3, new Card(Symbol.SPADES, Rank.TWO), trump);
        trick.playCard(p4, new Card(Symbol.SPADES, Rank.EIGHT), trump);

        assertEquals(p2, trick.getCurrentlyWinning(), "Bob's 10 of Spades should win the trick.");
        assertTrue(trick.isComplete());
    }

    @Test
    public void testTrumpBeatsStartingSymbol() {
        Symbol trump = Symbol.HEARTS;

        trick.playCard(p1, new Card(Symbol.SPADES, Rank.ACE), trump); // Lead Ace of Spades
        trick.playCard(p2, new Card(Symbol.HEARTS, Rank.TWO), trump); // Bob plays Trump 2 of Hearts

        assertEquals(p2, trick.getCurrentlyWinning(), "Trump 2 of Hearts should beat Ace of Spades.");
    }

    @Test
    public void testHigherTrumpBeatsLowerTrump() {
        Symbol trump = Symbol.HEARTS;

        trick.playCard(p1, new Card(Symbol.SPADES, Rank.ACE), trump);
        trick.playCard(p2, new Card(Symbol.HEARTS, Rank.FIVE), trump);
        trick.playCard(p3, new Card(Symbol.HEARTS, Rank.JACK), trump);

        assertEquals(p3, trick.getCurrentlyWinning(), "Jack of Hearts should beat 5 of Hearts.");
    }
}
