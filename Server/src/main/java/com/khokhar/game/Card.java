package com.khokhar.game;

/**
 * Represents a single playing card in the Plus Minus game.
 */
public class Card {
    private Symbol symbol;
    private Rank rank;

    public Card(Symbol symbol, Rank rank) {
        this.symbol = symbol;
        this.rank = rank;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return rank + " of " + symbol;
    }
}
