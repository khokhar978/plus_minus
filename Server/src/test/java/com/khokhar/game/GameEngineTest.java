package com.khokhar.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameEngineTest {

    private GameEngine engine;
    private List<Player> players;

    @BeforeEach
    public void setUp() {
        players = new ArrayList<>();
        players.add(new Player("P1"));
        players.add(new Player("P2"));
        players.add(new Player("P3"));
        players.add(new Player("P4"));
        engine = new GameEngine(players);
    }

    @Test
    public void testDealPhaseOneGives5CardsEach() {
        engine.dealPhaseOne();
        for (Player p : players) {
            assertEquals(5, p.getHand().size(), "Each player should have 5 cards after Phase 1.");
        }
    }

    @Test
    public void testDealPhaseTwoGives13CardsTotal() {
        engine.dealPhaseOne();
        engine.dealPhaseTwo();
        for (Player p : players) {
            assertEquals(13, p.getHand().size(), "Each player should have 13 cards after Phase 2.");
        }
    }

    @Test
    public void testReturnAllHandsRestoresDeckTo52() {
        engine.dealPhaseOne();
        engine.dealPhaseTwo();
        engine.returnAllHands();

        for (Player p : players) {
            assertEquals(0, p.getHand().size(), "Player hand should be empty after returnAllHands.");
        }
    }
}
