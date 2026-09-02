package com.khokhar.game;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Represents a single game room containing up to 4 players.
 * Holds all per-room state: lobby, bidding, playing, and scoring.
 *
 * All public methods that mutate state must be called from within
 * a synchronized(room) block (enforced by MessageRouter / RoomManager).
 */
public class Room {

    private static final Logger logger = LoggerFactory.getLogger(Room.class);
    private static final int TURN_TIMEOUT_MS = 30000;
    private static final int TARGET_SCORE = 21;

    private final String roomCode;
    private final Gson gson;
    private final TurnTimerManager timerManager;

    // ── Lobby state ──────────────────────────────────────────────────────────
    private final Map<String, Player> players;       // name → Player (insertion-ordered)
    private final Map<String, WebSocket> connections; // name → WebSocket
    private final Set<String> readyPlayers;
    private boolean gameStarted;

    // ── Bidding state ────────────────────────────────────────────────────────
    private GameEngine engine;
    private int bidsReceived;
    private int currentHighestBid;
    private String currentHighestBidder;
    private List<String> turnOrder;
    private int currentPlayTurnIndex;
    private Trick currentTrick;

    // ── Idle destruction timer (managed by RoomManager) ──────────────────────
    private ScheduledFuture<?> idleTimer;

    // ── Constructor ──────────────────────────────────────────────────────────

    public Room(String roomCode) {
        this.roomCode = roomCode;
        this.gson = new Gson();
        this.timerManager = new TurnTimerManager();
        this.players = new LinkedHashMap<>();
        this.connections = new HashMap<>();
        this.readyPlayers = new HashSet<>();
        this.gameStarted = false;
        this.bidsReceived = 0;
        this.currentHighestBid = 4;
        this.currentHighestBidder = null;
        this.turnOrder = new ArrayList<>();
        this.currentPlayTurnIndex = 0;
        this.currentTrick = new Trick();
    }

    // ── Getters (for RoomManager) ────────────────────────────────────────────

    public String getRoomCode() { return roomCode; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isEmpty() { return connections.isEmpty(); }
    public Map<String, WebSocket> getConnections() { return connections; }
    public int getPlayerCount() { return players.size(); }

    public void setIdleTimer(ScheduledFuture<?> future) { this.idleTimer = future; }
    public void cancelIdleTimer() {
        if (idleTimer != null) {
            idleTimer.cancel(false);
            idleTimer = null;
        }
    }
    public void cancelTurnTimer() { timerManager.cancel(); }

    // ── Broadcasting ─────────────────────────────────────────────────────────

    private void broadcastToAll(String text) {
        for (WebSocket conn : connections.values()) {
            if (conn != null && conn.isOpen()) {
                conn.send(text);
            }
        }
    }

    private void broadcastPlayersSync() {
        JsonObject syncMsg = new JsonObject();
        syncMsg.addProperty("type", "PLAYERS_SYNC");
        JsonArray playersArray = new JsonArray();
        for (String name : players.keySet()) {
            Player p = players.get(name);
            JsonObject pObj = new JsonObject();
            pObj.addProperty("name", name);
            pObj.addProperty("connected", connections.containsKey(name));
            pObj.addProperty("bid", p.getBidPoints());
            pObj.addProperty("tricks", p.getTricksWon());
            pObj.addProperty("totalScore", p.getTotalScore());
            playersArray.add(pObj);
        }
        syncMsg.add("players", playersArray);
        broadcastToAll(syncMsg.toString());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getPlayerName(WebSocket conn) {
        for (Map.Entry<String, WebSocket> entry : connections.entrySet()) {
            if (entry.getValue().equals(conn)) return entry.getKey();
        }
        return null;
    }

    private void startTurnTimer(String currentPlayer) {
        // Broadcast timer start to all clients
        JsonObject timerMsg = new JsonObject();
        timerMsg.addProperty("type", "TIMER_START");
        timerMsg.addProperty("player", currentPlayer);
        timerMsg.addProperty("duration", TURN_TIMEOUT_MS);
        broadcastToAll(timerMsg.toString());

        timerManager.start(currentPlayer, TURN_TIMEOUT_MS, player -> {
            synchronized (this) {
                if (!gameStarted) return;
                handleTurnTimeout(player);
            }
        });
    }

    // ── Action Handlers ───────────────────────────────────────────────────────

    /**
     * Handles a new or reconnecting player joining this room's lobby.
     */
    public void handleJoin(WebSocket conn, String rawName) {
        String playerName = players.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(rawName))
                .findFirst()
                .orElse(rawName);

        // Reconnect case
        if (players.containsKey(playerName)) {
            if (connections.containsKey(playerName)) {
                WebSocket oldConn = connections.get(playerName);
                if (oldConn != conn && oldConn.isOpen()) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"Name already taken!\"}");
                    return;
                }
            }
            connections.put(playerName, conn);
            logger.info("{} reconnected to room {}.", playerName, roomCode);
            broadcastPlayersSync();
            sendStateSync(conn, playerName);
            return;
        }

        // New player
        if (players.size() >= 4) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"Room is full!\"}");
            return;
        }

        Player newPlayer = new Player(playerName);
        players.put(playerName, newPlayer);
        connections.put(playerName, conn);
        turnOrder.add(playerName);
        logger.info("{} joined room {}.", playerName, roomCode);
        broadcastPlayersSync();
    }

    /**
     * Sends a full state snapshot to a reconnecting player.
     */
    private void sendStateSync(WebSocket conn, String playerName) {
        if (!gameStarted) return;

        Player p = players.get(playerName);
        JsonObject stateUpdate = new JsonObject();

        if (bidsReceived < 4) {
            stateUpdate.addProperty("type", "GAME_START");
            stateUpdate.addProperty("turn", turnOrder.get(bidsReceived));
            List<String> baseList = new ArrayList<>(players.keySet());
            if (engine != null && engine.getDealerIndex() < baseList.size()) {
                String dealerName = baseList.get(engine.getDealerIndex());
                stateUpdate.addProperty("dealer", dealerName);
                if (playerName.equals(dealerName)) {
                    Card peekCard = engine.peekBottomCard();
                    if (peekCard != null) {
                        JsonObject peekObj = new JsonObject();
                        peekObj.addProperty("symbol", peekCard.getSymbol().toString());
                        peekObj.addProperty("rank", peekCard.getRank().toString());
                        stateUpdate.add("peekCard", peekObj);
                    }
                }
            }
        } else if (bidsReceived < 8) {
            stateUpdate.addProperty("type", "PHASE_2_START");
            stateUpdate.addProperty("turn", turnOrder.get(bidsReceived - 4));
            stateUpdate.addProperty("finalTrump", engine.getSpecialSymbol().toString());
        } else {
            stateUpdate.addProperty("type", "GAME_READY");
            stateUpdate.addProperty("turn", turnOrder.get(currentPlayTurnIndex));
            JsonArray trickArray = new JsonArray();
            for (Map.Entry<Player, Card> entry : currentTrick.getCards().entrySet()) {
                JsonObject pcObj = new JsonObject();
                pcObj.addProperty("player", entry.getKey().getName());
                pcObj.addProperty("symbol", entry.getValue().getSymbol().toString());
                pcObj.addProperty("rank", entry.getValue().getRank().toString());
                trickArray.add(pcObj);
            }
            stateUpdate.add("table", trickArray);
        }
        stateUpdate.add("yourHand", gson.toJsonTree(p.getHand()));
        conn.send(stateUpdate.toString());
    }

    /**
     * Marks a player as ready. Starts the game when all 4 are ready.
     */
    public void handleReady(WebSocket conn) {
        String name = getPlayerName(conn);
        if (name == null) return;
        if (gameStarted || !readyPlayers.add(name)) return;

        JsonObject readyUpdate = new JsonObject();
        readyUpdate.addProperty("type", "READY_UPDATE");
        JsonArray readyArray = new JsonArray();
        for (String p : readyPlayers) readyArray.add(p);
        readyUpdate.add("readyPlayers", readyArray);
        broadcastToAll(readyUpdate.toString());

        if (readyPlayers.size() == 4) {
            startGame();
        }
    }

    private void startGame() {
        logger.info("All players ready in room {}. Starting game...", roomCode);
        readyPlayers.clear();
        gameStarted = true;

        if (engine == null) {
            engine = new GameEngine(new ArrayList<>(players.values()));
        }
        engine.dealPhaseOne();

        int dealerIdx = engine.getDealerIndex();
        int firstPlayerIdx = (dealerIdx + 1) % 4;
        List<String> baseTurnOrder = new ArrayList<>(players.keySet());
        turnOrder.clear();
        for (int k = 0; k < 4; k++) {
            turnOrder.add(baseTurnOrder.get((firstPlayerIdx + k) % 4));
        }

        bidsReceived = 0;
        currentHighestBid = 4;
        currentHighestBidder = null;
        currentPlayTurnIndex = 0;
        currentTrick = new Trick();

        String dealerName = baseTurnOrder.get(dealerIdx);
        Card peekCard = engine.peekBottomCard();

        for (Map.Entry<String, Player> entry : players.entrySet()) {
            WebSocket playerConn = connections.get(entry.getKey());
            if (playerConn != null) {
                JsonObject msg = new JsonObject();
                msg.addProperty("type", "GAME_START");
                msg.addProperty("turn", turnOrder.get(0));
                msg.addProperty("dealer", dealerName);
                msg.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                if (entry.getKey().equals(dealerName) && peekCard != null) {
                    JsonObject peekObj = new JsonObject();
                    peekObj.addProperty("symbol", peekCard.getSymbol().toString());
                    peekObj.addProperty("rank", peekCard.getRank().toString());
                    msg.add("peekCard", peekObj);
                }
                playerConn.send(msg.toString());
            }
        }
        startTurnTimer(turnOrder.get(0));
    }

    public void handleBidPhase1(WebSocket conn, int bidAmount, Symbol trump) {
        String name = getPlayerName(conn);
        if (name == null) return;

        if (!name.equals(turnOrder.get(bidsReceived))) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
            return;
        }
        if (bidAmount > 0 && bidAmount <= currentHighestBid) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid higher than the current highest bid!\"}");
            return;
        }

        timerManager.cancel();
        processBidPhase1(name, bidAmount, trump);
    }

    public void handleBidPhase2(WebSocket conn, int bidAmount) {
        String name = getPlayerName(conn);
        if (name == null) return;

        if (!name.equals(turnOrder.get(bidsReceived - 4))) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
            return;
        }
        if (name.equals(currentHighestBidder)) {
            if (bidAmount < currentHighestBid) {
                conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid at least your Phase 1 bid (" + currentHighestBid + ")!\"}");
                return;
            }
        } else if (bidAmount < 2) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"Minimum bid is 2!\"}");
            return;
        }

        timerManager.cancel();
        processBidPhase2(name, bidAmount);
    }

    public void handlePlayCard(WebSocket conn, Symbol symbol, Rank rank) {
        String name = getPlayerName(conn);
        if (name == null) return;

        Player p = players.get(name);
        if (!name.equals(turnOrder.get(currentPlayTurnIndex))) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to play!\"}");
            return;
        }

        Card cardToPlay = new Card(symbol, rank);
        if (!RuleValidator.isValidPlay(p, cardToPlay, engine.getSpecialSymbol(), currentTrick)) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid play! Please follow the game rules.\"}");
            return;
        }

        timerManager.cancel();
        processPlayCard(name, cardToPlay);
    }

    /**
     * Called when a player's WebSocket disconnects.
     */
    public void handleDisconnect(WebSocket conn) {
        String name = getPlayerName(conn);
        if (name == null) return;

        connections.remove(name);
        logger.info("{} disconnected from room {}.", name, roomCode);

        if (!gameStarted) {
            players.remove(name);
            readyPlayers.remove(name);
            turnOrder.remove(name);
        }
        broadcastPlayersSync();
    }

    // ── Game Logic ────────────────────────────────────────────────────────────

    private void processBidPhase1(String name, int bidAmount, Symbol trump) {
        if (bidAmount > 0) {
            currentHighestBid = bidAmount;
            currentHighestBidder = name;
            engine.setSpecialSymbol(trump);
        }
        bidsReceived++;

        JsonObject bidUpdate = new JsonObject();
        bidUpdate.addProperty("type", "BID_1_UPDATE");
        bidUpdate.addProperty("highestBid", currentHighestBid);
        bidUpdate.addProperty("highestBidder", currentHighestBidder != null ? currentHighestBidder : "None");

        if (bidsReceived == 4) {
            if (currentHighestBidder == null) engine.setSpecialSymbol(Symbol.SPADES);
            engine.dealPhaseTwo();

            for (Map.Entry<String, Player> entry : players.entrySet()) {
                WebSocket playerConn = connections.get(entry.getKey());
                if (playerConn != null) {
                    JsonObject msg = new JsonObject();
                    msg.addProperty("type", "PHASE_2_START");
                    msg.addProperty("turn", turnOrder.get(0));
                    msg.addProperty("finalTrump", engine.getSpecialSymbol().toString());
                    msg.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                    playerConn.send(msg.toString());
                }
            }
            startTurnTimer(turnOrder.get(0));
        } else {
            bidUpdate.addProperty("nextTurn", turnOrder.get(bidsReceived));
            broadcastToAll(bidUpdate.toString());
            startTurnTimer(turnOrder.get(bidsReceived));
        }
    }

    private void processBidPhase2(String name, int bidAmount) {
        Player p = players.get(name);
        if (p != null) p.setBidPoints(bidAmount);
        bidsReceived++;

        JsonObject bidUpdate = new JsonObject();
        bidUpdate.addProperty("type", "BID_2_UPDATE");
        bidUpdate.addProperty("player", name);
        bidUpdate.addProperty("amount", bidAmount);

        if (bidsReceived == 8) {
            broadcastPlayersSync();

            int totalBid = 0;
            for (Player pl : players.values()) totalBid += pl.getBidPoints();

            if (totalBid <= 10) {
                // Auto-skip round
                logger.info("Total bid {} <= 10, auto-skipping round in room {}.", totalBid, roomCode);
                engine.returnAllHands();
                JsonArray scoresArray = ScoreManager.calculateScores(players.values(), true);
                Player highestScorer = ScoreManager.findHighestScorer(players.values());
                Player lowestScorer = ScoreManager.findLowestScorer(players.values());

                JsonObject roundUpdate = new JsonObject();
                if (ScoreManager.isGameWon(highestScorer, TARGET_SCORE)) {
                    roundUpdate.addProperty("type", "GAME_OVER");
                    roundUpdate.addProperty("winner", highestScorer.getName());
                } else {
                    roundUpdate.addProperty("type", "ROUND_OVER");
                }
                roundUpdate.addProperty("autoSkipped", true);
                roundUpdate.add("scores", scoresArray);
                broadcastToAll(roundUpdate.toString());

                List<String> baseOrder = new ArrayList<>(players.keySet());
                engine.setDealerIndex(baseOrder.indexOf(lowestScorer.getName()));
                for (Player player : players.values()) player.reset();
                broadcastPlayersSync();
                gameStarted = false;
                timerManager.cancel();
            } else {
                JsonObject stateUpdate = new JsonObject();
                stateUpdate.addProperty("type", "GAME_READY");
                stateUpdate.addProperty("turn", turnOrder.get(0));
                broadcastToAll(stateUpdate.toString());
                startTurnTimer(turnOrder.get(0));
            }
        } else {
            broadcastPlayersSync();
            bidUpdate.addProperty("nextTurn", turnOrder.get(bidsReceived - 4));
            broadcastToAll(bidUpdate.toString());
            startTurnTimer(turnOrder.get(bidsReceived - 4));
        }
    }

    private void processPlayCard(String name, Card cardToPlay) {
        Player p = players.get(name);
        p.removeCard(cardToPlay);
        engine.returnCard(cardToPlay);
        currentTrick.playCard(p, cardToPlay, engine.getSpecialSymbol());

        JsonObject cardUpdate = new JsonObject();
        cardUpdate.addProperty("type", "CARD_PLAYED");
        cardUpdate.addProperty("player", name);
        cardUpdate.addProperty("symbol", cardToPlay.getSymbol().toString());
        cardUpdate.addProperty("rank", cardToPlay.getRank().toString());
        broadcastToAll(cardUpdate.toString());

        if (currentTrick.isComplete()) {
            Player winner = currentTrick.getCurrentlyWinning();
            winner.incrementTricksWon();
            broadcastPlayersSync();

            JsonObject winnerUpdate = new JsonObject();
            winnerUpdate.addProperty("type", "TRICK_WINNER");
            winnerUpdate.addProperty("winner", winner.getName());
            broadcastToAll(winnerUpdate.toString());

            currentPlayTurnIndex = turnOrder.indexOf(winner.getName());
            currentTrick = new Trick();

            if (winner.getHand().isEmpty()) {
                // Round over
                JsonArray scoresArray = ScoreManager.calculateScores(players.values(), false);
                Player highestScorer = ScoreManager.findHighestScorer(players.values());
                Player lowestScorer = ScoreManager.findLowestScorer(players.values());

                JsonObject roundUpdate = new JsonObject();
                if (ScoreManager.isGameWon(highestScorer, TARGET_SCORE)) {
                    roundUpdate.addProperty("type", "GAME_OVER");
                    roundUpdate.addProperty("winner", highestScorer.getName());
                } else {
                    roundUpdate.addProperty("type", "ROUND_OVER");
                }
                roundUpdate.add("scores", scoresArray);
                broadcastToAll(roundUpdate.toString());

                List<String> baseOrder = new ArrayList<>(players.keySet());
                engine.setDealerIndex(baseOrder.indexOf(lowestScorer.getName()));
                for (Player player : players.values()) player.reset();
                broadcastPlayersSync();
                gameStarted = false;
                timerManager.cancel();
            } else {
                JsonObject turnUpdate = new JsonObject();
                turnUpdate.addProperty("type", "NEXT_TURN");
                turnUpdate.addProperty("turn", winner.getName());
                broadcastToAll(turnUpdate.toString());
                startTurnTimer(winner.getName());
            }
        } else {
            currentPlayTurnIndex = (currentPlayTurnIndex + 1) % 4;
            JsonObject turnUpdate = new JsonObject();
            turnUpdate.addProperty("type", "NEXT_TURN");
            turnUpdate.addProperty("turn", turnOrder.get(currentPlayTurnIndex));
            broadcastToAll(turnUpdate.toString());
            startTurnTimer(turnOrder.get(currentPlayTurnIndex));
        }
    }

    private void handleTurnTimeout(String currentPlayer) {
        logger.info("Auto-playing for {} in room {}.", currentPlayer, roomCode);

        JsonObject autoMsg = new JsonObject();
        autoMsg.addProperty("type", "AUTO_PLAYED");
        autoMsg.addProperty("player", currentPlayer);
        broadcastToAll(autoMsg.toString());

        if (bidsReceived < 4) {
            if (currentPlayer.equals(turnOrder.get(bidsReceived))) {
                processBidPhase1(currentPlayer, 0, Symbol.SPADES);
            }
        } else if (bidsReceived < 8) {
            if (currentPlayer.equals(turnOrder.get(bidsReceived - 4))) {
                int minBid = currentPlayer.equals(currentHighestBidder) ? currentHighestBid : 2;
                processBidPhase2(currentPlayer, minBid);
            }
        } else {
            if (currentPlayer.equals(turnOrder.get(currentPlayTurnIndex))) {
                Player p = players.get(currentPlayer);
                if (p != null && !p.getHand().isEmpty()) {
                    Card autoCard = AutoPlayHelper.selectAutoPlayCard(p, engine.getSpecialSymbol(), currentTrick);
                    processPlayCard(currentPlayer, autoCard);
                }
            }
        }
    }
}
