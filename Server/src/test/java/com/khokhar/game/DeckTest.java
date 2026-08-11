package com.khokhar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    private Deck deck;

    @BeforeEach
    public void setUp() {
        deck = new Deck();
    }

    @Test
    public void testFreshDeckHas52Cards() {
        assertEquals(52, deck.size(), "Fresh deck should contain 52 cards.");
    }

    @Test
    public void testDrawReducesSize() {
        Card drawn = deck.drawCard();
        assertNotNull(drawn, "Drawn card should not be null.");
        assertEquals(51, deck.size(), "Deck size should decrease after draw.");
    }

    @Test
    public void testAddCardIncreasesSize() {
        Card drawn = deck.drawCard();
        deck.addCard(drawn);
        assertEquals(52, deck.size(), "Deck size should return to 52 after adding drawn card.");
    }

    @Test
    public void testPeekBottomDoesNotRemove() {
        Card peeked = deck.peekBottom();
        assertNotNull(peeked, "Peeked card should not be null.");
        assertEquals(52, deck.size(), "Peeking should not change deck size.");
    }

    @Test
    public void testDrawFromEmptyThrows() {
        for (int i = 0; i < 52; i++) {
            deck.drawCard();
        }
        assertEquals(0, deck.size());
        assertThrows(IllegalArgumentException.class, () -> deck.drawCard());
    }
}
