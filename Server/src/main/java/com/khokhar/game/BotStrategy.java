package com.khokhar.game;

import java.util.*;

/**
 * Decision-making logic for bot players.
 *
 * Phase 1 (Color/Trump bidding):
 *   Bot counts "power cards" (10, J, Q, K, A) in its hand.
 *   If it has enough to justify bidding, it bids that count with the suit
 *   it has the most cards in (maximises trump coverage).
 *   Otherwise it passes (bid = 0).
 *
 * Phase 2 (Personal bid):
 *   Bot counts cards likely to win tricks: trump cards + aces of other suits.
 *   Clamped to [minBid, 13].
 *
 * Card play: handled by existing AutoPlayHelper (lowest valid card).
 */
public class BotStrategy {

    // Rank values considered "power cards" for phase-1 bidding evaluation
    private static final Set<Rank> POWER_RANKS = EnumSet.of(
            Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE
    );

    /**
     * Decide the bot's Phase 1 bid.
     *
     * @param bot               The bot player
     * @param currentHighestBid The current highest bid (bot must beat this or pass)
     * @return int[2]: [bidAmount, trumpSymbolOrdinal].
     *         bidAmount == 0 means pass.
     */
    public static int[] decideBidPhase1(Player bot, int currentHighestBid) {
        List<Card> hand = bot.getHand();

        // Count power cards and cards per suit
        Map<Symbol, Integer> suitCount = new EnumMap<>(Symbol.class);
        int powerCount = 0;
        for (Card c : hand) {
            suitCount.merge(c.getSymbol(), 1, Integer::sum);
            if (POWER_RANKS.contains(c.getRank())) {
                powerCount++;
            }
        }

        // Choose trump = suit with the most cards (bot has best trump coverage)
        Symbol bestTrump = Symbol.SPADES;
        int bestCount = 0;
        for (Map.Entry<Symbol, Integer> entry : suitCount.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestTrump = entry.getKey();
            }
        }

        // Bid = power card count, but must exceed currentHighestBid
        int bidAmount = powerCount;
        // Ensure minimum bid is 5 (game rule floor)
        if (bidAmount < 5) bidAmount = 5;

        if (bidAmount <= currentHighestBid) {
            // Can't outbid — pass
            return new int[]{0, Symbol.SPADES.ordinal()};
        }

        return new int[]{bidAmount, bestTrump.ordinal()};
    }

    /**
     * Decide the bot's Phase 2 bid.
     *
     * @param bot                 The bot player
     * @param trump               The declared trump suit
     * @param currentHighestBidder Name of the phase-1 winner (must bid >= their bid)
     * @param currentHighestBid   The phase-1 winning bid (minimum for the winner)
     * @return bid amount (already clamped to valid range)
     */
    public static int decideBidPhase2(Player bot, Symbol trump,
                                       String currentHighestBidder, int currentHighestBid) {
        List<Card> hand = bot.getHand();
        int estimate = 0;

        for (Card c : hand) {
            if (c.getSymbol() == trump) {
                // Trump cards are likely to win tricks
                estimate++;
            } else if (c.getRank() == Rank.ACE) {
                // Aces of non-trump suits can often win too
                estimate++;
            } else if (c.getRank() == Rank.KING) {
                // Kings have a decent chance
                estimate++;
            }
        }

        // Slight conservative adjustment — don't overbid
        estimate = Math.max(estimate - 1, 2);

        // If this bot was the highest bidder in phase 1, it must bid >= that
        if (bot.getName().equals(currentHighestBidder)) {
            estimate = Math.max(estimate, currentHighestBid);
        }

        // Clamp to valid range [2, 13]
        return Math.max(2, Math.min(estimate, 13));
    }
}
