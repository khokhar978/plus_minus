package com.khokhar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AutoPlayHelperTest {

    private Player p1;
    private Trick trick;

    @BeforeEach
    public void setUp() {
        p1 = new Player("Player 1");
        trick = new Trick();
    }

    @Test
    public void testSelectsLowestValidCardOnLead() {
        p1.addCard(new Card(Symbol.HEARTS, Rank.ACE));
        p1.addCard(new Card(Symbol.SPADES, Rank.THREE));
        p1.addCard(new Card(Symbol.SPADES, Rank.KING));

        Card selected = AutoPlayHelper.selectAutoPlayCard(p1, Symbol.CLUBS, trick);

        assertEquals(Rank.THREE, selected.getRank(), "Auto-play should select the lowest rank card (Three of Spades).");
        assertEquals(Symbol.SPADES, selected.getSymbol());
    }

    @Test
    public void testFollowsSuitWhenRequired() {
        Player lead = new Player("Lead");
        trick.playCard(lead, new Card(Symbol.DIAMONDS, Rank.FIVE), Symbol.CLUBS);

        p1.addCard(new Card(Symbol.SPADES, Rank.ACE));
        p1.addCard(new Card(Symbol.DIAMONDS, Rank.EIGHT));
        p1.addCard(new Card(Symbol.DIAMONDS, Rank.TEN));

        Card selected = AutoPlayHelper.selectAutoPlayCard(p1, Symbol.CLUBS, trick);

        // Must follow suit (DIAMONDS), and must head (EIGHT beats FIVE, lowest valid)
        assertEquals(Symbol.DIAMONDS, selected.getSymbol());
        assertEquals(Rank.EIGHT, selected.getRank());
    }
}
