package com.khokhar.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Collection;

/**
 * Stateless utility class for all scoring calculations.
 * Extracted to eliminate the duplicate scoring logic that previously
 * existed in both processPlayCard() and processBidPhase2() in GameServer.
 */
public class ScoreManager {

    /**
     * Calculates scores for all players at the end of a round.
     * For a normal round: points = bid if tricks >= bid, else -bid.
     * For an auto-skipped round: each player simply earns their bid amount.
     */
    public static JsonArray calculateScores(Collection<Player> players, boolean autoSkipped) {
        JsonArray scoresArray = new JsonArray();
        for (Player player : players) {
            int bid = player.getBidPoints();
            int tricks = player.getTricksWon();
            int pointsEarned;

            if (autoSkipped) {
                pointsEarned = bid; // auto-skip: everyone earns their bid
            } else {
                pointsEarned = (tricks >= bid) ? bid : -bid;
            }

            player.incrementTotalScore(pointsEarned);

            JsonObject playerScore = new JsonObject();
            playerScore.addProperty("player", player.getName());
            playerScore.addProperty("bid", bid);
            playerScore.addProperty("tricks", autoSkipped ? bid : tricks);
            playerScore.addProperty("pointsEarned", pointsEarned);
            playerScore.addProperty("totalScore", player.getTotalScore());
            scoresArray.add(playerScore);
        }
        return scoresArray;
    }

    /**
     * Returns the player with the highest total score.
     * In case of a tie, the first encountered is returned.
     */
    public static Player findHighestScorer(Collection<Player> players) {
        Player highest = null;
        for (Player p : players) {
            if (highest == null || p.getTotalScore() > highest.getTotalScore()) {
                highest = p;
            }
        }
        return highest;
    }

    /**
     * Returns the player with the lowest total score.
     * This player becomes the dealer for the next round.
     * In case of a tie, the first encountered is returned.
     */
    public static Player findLowestScorer(Collection<Player> players) {
        Player lowest = null;
        for (Player p : players) {
            if (lowest == null || p.getTotalScore() < lowest.getTotalScore()) {
                lowest = p;
            }
        }
        return lowest;
    }

    /**
     * Returns true if the given player has reached or exceeded the target score.
     */
    public static boolean isGameWon(Player player, int targetScore) {
        return player != null && player.getTotalScore() >= targetScore;
    }
}
