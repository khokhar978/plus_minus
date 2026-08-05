package com.khokhar.game;

public class RuleValidator {

    public static boolean isValidPlay(Player player, Card card, Symbol specialSymbol, Trick trick) {
        // 1. Verify the player actually has this card
        if (!player.hasCard(card)) {
            return false;
        }

        // 2. Is this the first card of the round?
        if (trick.getCards().isEmpty()) {
            return true;
        }

        Symbol startingSymbol = trick.getStartingSymbol();
        Card highestCard = trick.getCurrentHighestCard();

        // 3. Do they have the starting symbol?
        if (player.hasSymbol(startingSymbol)) {
            
            // If they played a different symbol, reject it
            if (card.getSymbol() != startingSymbol) {
                return false;
            }
            
            // Must Head logic (Only applies if a Trump hasn't been played yet!)
            if (!trick.isTrumpUsed()) {
                boolean hasHigher = false;
                for (Card c : player.getHand()) {
                    if (c.getSymbol() == startingSymbol && c.getRank().getValue() > highestCard.getRank().getValue()) {
                        hasHigher = true;
                        break;
                    }
                }
                
                // If they have a higher card, the card they chose MUST be higher
                if (hasHigher) {
                    if (card.getRank().getValue() < highestCard.getRank().getValue()) {
                        return false;
                    }
                }
            }
            
            return true;
        } else {
            // 4. They DO NOT have the starting symbol
            if (player.hasSymbol(specialSymbol)) {
                
                // If they try to play a non-trump, REJECT
                if (card.getSymbol() != specialSymbol) {
                    return false;
                }

                // We only need to check for over-trumping IF the highest card is ALSO a trump!
                if (highestCard.getSymbol() == specialSymbol) {
                    boolean hasHigherTrump = false;
                    for (Card c : player.getHand()) {
                        if (c.getSymbol() == specialSymbol && c.getRank().getValue() > highestCard.getRank().getValue()) {
                            hasHigherTrump = true;
                            break;
                        }
                    }

                    // If they have a higher trump, but the card they played is NOT higher, REJECT
                    if (hasHigherTrump) {
                        if (card.getRank().getValue() < highestCard.getRank().getValue()) {
                            return false;
                        }
                    }
                }

                // Valid trump play
                return true;

            } else {
                // They have neither the starting symbol nor a trump. They can play anything.
                return true;
            }
        }
    }
}
