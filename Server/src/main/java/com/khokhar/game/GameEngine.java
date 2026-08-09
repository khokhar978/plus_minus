package com.khokhar.game;

import java.util.List;

/**
 * The core engine that manages dealing phases and tracks the global game state.
 * The deck persists across rounds to simulate real-life card distribution.
 */
public class GameEngine {
    private List<Player> players;
    private Deck deck;
    private Symbol specialSymbol;
    private boolean firstRound;
    private int dealerIndex; // index into players list — the dealer (loser) position

    public GameEngine(List<Player> players) {
        this.players = players;
        this.deck = new Deck();
        this.specialSymbol = Symbol.SPADES;
        this.firstRound = true;
        this.dealerIndex = 0; // first round: player 0 deals
    }

    /**
     * Phase 1 of dealing: Deals exactly 5 cards to each player.
     * First round: shuffles a fresh deck.
     * Subsequent rounds: shuffles the recycled deck (cards returned during play).
     * Dealing starts from the player AFTER the dealer (the dealer gets cards last).
     */
    public void dealPhaseOne() {
        if (firstRound) {
            deck.shuffle();
            firstRound = false;
        } else {
            deck.shuffle();
        }

        int numPlayers = players.size();
        for (int i = 0; i < 5; i++) {
            for (int j = 1; j <= numPlayers; j++) {
                int playerIdx = (dealerIndex + j) % numPlayers;
                players.get(playerIdx).addCard(deck.drawCard());
            }
        }
        for (Player p : players) {
            p.sortHand();
        }
    }

    /**
     * Phase 2 of dealing: Deals the remaining 8 cards to each player.
     * Dealing starts from the player AFTER the dealer.
     */
    public void dealPhaseTwo() {
        int numPlayers = players.size();
        for (int i = 0; i < 8; i++) {
            for (int j = 1; j <= numPlayers; j++) {
                int playerIdx = (dealerIndex + j) % numPlayers;
                players.get(playerIdx).addCard(deck.drawCard());
            }
        }
        for (Player p : players) {
            p.sortHand();
        }
    }

    /**
     * Returns a played card back to the deck (called when a card is played in a trick).
     */
    public void returnCard(Card card) {
        deck.addCard(card);
    }

    /**
     * Returns all cards from all players' hands back to the deck.
     * Used when a round is auto-skipped (cards were dealt but never played).
     */
    public void returnAllHands() {
        for (Player p : players) {
            for (Card c : p.getHand()) {
                deck.addCard(c);
            }
            p.clearHand();
        }
    }

    /**
     * Sets the dealer index. The dealer deals cards but the first card 
     * goes to the player after them.
     */
    public void setDealerIndex(int index) {
        this.dealerIndex = index;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public Card peekBottomCard() {
        return deck.peekBottom();
    }

    public Symbol getSpecialSymbol(){
        return specialSymbol;
    }
    
    public void setSpecialSymbol(Symbol symbol){
        this.specialSymbol=symbol;
    }
}
