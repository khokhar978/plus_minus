package com.khokhar.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private String name;
    private List<Card> hand;
    private int bidPoints;
    private int tricksWon;
    private int totalScore;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.bidPoints = 0;
        this.tricksWon = 0;
        this.totalScore = 0;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    public void removeCard(Card card) {
        hand.remove(card);
    }

    public void clearHand() {
        hand.clear();
    }

    public boolean hasCard(Card card) {
        return hand.contains(card);
    }

    public boolean isEmpty() {
        return hand.isEmpty();
    }

    public int getCardCount() {
        return hand.size();
    }

    public Card getCard(int index) {
        if (index < 0 || index >= hand.size()) {
            throw new IllegalArgumentException("Invalid card index.");
        }
        return hand.get(index);
    }

    public void setBidPoints(int bidPoints) {
        this.bidPoints = bidPoints;
    }

    public void incrementTricksWon() {
        this.tricksWon++;
    }

    public void incrementTotalScore(int value) {
        this.totalScore += value;
    }

    public int getBidPoints() {
        return bidPoints;
    }

    public int getTricksWon() {
        return tricksWon;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void reset() {
        hand.clear();
        bidPoints = 0;
        tricksWon = 0;
        totalScore = 0;
    }

    @Override
    public String toString() {
        return name + " (" + hand.size() + " cards)";
    }
}
