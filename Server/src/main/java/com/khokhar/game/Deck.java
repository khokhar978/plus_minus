package com.khokhar.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a standard 52-card deck.
 */
public class Deck {
    private List<Card> cards;

    /**
     * Initializes the deck with all 52 combinations of Symbols and Ranks.
     */
    public Deck() {
        cards = new ArrayList<>();
        for (Symbol symbol : Symbol.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(symbol, rank));
            }
        }
    }

    /**
     * Randomizes the order of the cards in the deck.
     */
    public void shuffle(){
        Collections.shuffle(cards);
    }

    /**
     * Draws the top card from the deck.
     * @return the drawn Card
     * @throws IllegalArgumentException if the deck is empty
     */
    public Card drawCard(){
        if(cards.isEmpty()){
            throw new IllegalArgumentException("No cards left in the deck.");
        }
        return cards.remove(cards.size()-1); // O(1) removal optimization
    }

    /**
     * Returns a card to the bottom of the deck.
     */
    public void addCard(Card card) {
        cards.add(0, card);
    }

    /**
     * Returns the number of cards currently in the deck.
     */
    public int size() {
        return cards.size();
    }

    /**
     * Peeks at the bottom card of the deck without removing it.
     * @return the bottom Card, or null if deck is empty
     */
    public Card peekBottom() {
        if (cards.isEmpty()) return null;
        return cards.get(0);
    }
}
