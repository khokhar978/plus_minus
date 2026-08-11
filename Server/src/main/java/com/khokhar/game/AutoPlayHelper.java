package com.khokhar.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility helper for auto-playing valid cards and bids when turn timers expire.
 */
public class AutoPlayHelper {

    /**
     * Selects the lowest rank valid card from the player's hand according to rule validation.
     *
     * @param player        The player whose turn it is to auto-play
     * @param specialSymbol The trump symbol for the current round
     * @param trick         The active trick being played
     * @return The chosen valid Card to play
     */
    public static Card selectAutoPlayCard(Player player, Symbol specialSymbol, Trick trick) {
        List<Card> handCopy = new ArrayList<>(player.getHand());
        if (handCopy.isEmpty()) {
            throw new IllegalStateException("Cannot auto-play with an empty hand.");
        }

        // Sort by rank value ascending so we prefer playing lowest card
        handCopy.sort(Comparator.comparingInt(c -> c.getRank().getValue()));

        for (Card card : handCopy) {
            if (RuleValidator.isValidPlay(player, card, specialSymbol, trick)) {
                return card;
            }
        }

        // Fallback: return the first card if no validator matched (should not occur)
        return handCopy.get(0);
    }
}
