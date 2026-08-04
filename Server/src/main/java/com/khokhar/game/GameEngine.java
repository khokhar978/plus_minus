package com.khokhar.game;

import java.util.List;

/**
 * The core engine that manages dealing phases and tracks the global game state.
 */
public class GameEngine {
    private List<Player> players;
    private Deck deck;
    private Symbol specialSymbol;

    public GameEngine(List<Player> players) {
        this.players = players;
        deck = new Deck();
        specialSymbol = Symbol.SPADES; // Default trump symbol
    }

    /**
     * Phase 1 of dealing: Shuffles the deck and deals exactly 5 cards to each player.
     * After this phase, the bidding process begins.
     */
    public void dealPhaseOne() {
        deck.shuffle();
        for (int i = 0; i < 5; i++) {
            for (Player p : players) {
                p.addCard(deck.drawCard());
            }
        }
    }

    /**
     * Phase 2 of dealing: Deals the remaining 8 cards to each player.
     */
    public void dealPhaseTwo() {
        for (int i = 0; i < 8; i++) {
            for (Player p : players) {
                p.addCard(deck.drawCard());
            }
        }
    }

    public Symbol getSpecialSymbol(){
        return specialSymbol;
    }
    
    public void setSpecialSymbol(Symbol symbol){
        this.specialSymbol=symbol;
    }
}
