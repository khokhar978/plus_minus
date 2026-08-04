package com.khokhar.game;

import java.util.LinkedHashMap;
import java.util.Map;

public class Trick {
    private Map<Player, Card> cards;
    private Symbol startingSymbol;
    private boolean isTrumpUsed;
    private Card currentHighestCard;
    private Player currentlyWinning;

    public Trick() {
        cards = new LinkedHashMap<>(); 
        startingSymbol = null;
        isTrumpUsed = false;
        currentHighestCard = null;
        currentlyWinning = null;
    }

    public void playCard(Player player, Card card, Symbol specialSymbol) {
        if (cards.isEmpty()) {
            startingSymbol = card.getSymbol();
            currentHighestCard = card;
            currentlyWinning = player;
            
            if (startingSymbol == specialSymbol) {
                isTrumpUsed = true;
            }
        } else {
            if (card.getSymbol() == specialSymbol && startingSymbol != specialSymbol) {
                isTrumpUsed = true;
            }

            // Determine if the new card beats the current highest card
            boolean isNewWinner = false;
            
            // If the new card is a trump, and the highest card is NOT a trump, the new card wins
            if (card.getSymbol() == specialSymbol && currentHighestCard.getSymbol() != specialSymbol) {
                isNewWinner = true; 
            } 
            // If they are the same symbol, the higher rank wins
            else if (card.getSymbol() == currentHighestCard.getSymbol()) {
                if (card.getRank().getValue() > currentHighestCard.getRank().getValue()) {
                    isNewWinner = true;
                }
            }

            if (isNewWinner) {
                currentHighestCard = card;
                currentlyWinning = player;
            }
        }
        
        cards.put(player, card);
    }

    public boolean isComplete() {
        return cards.size() == 4;
    }

    public Map<Player, Card> getCards() {
        return cards;
    }

    public Symbol getStartingSymbol() {
        return startingSymbol;
    }

    public boolean isTrumpUsed() {
        return isTrumpUsed;
    }

    public Card getCurrentHighestCard() {
        return currentHighestCard;
    }

    public Player getCurrentlyWinning() {
        return currentlyWinning;
    }
}
