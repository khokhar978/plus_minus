package com.khokhar.game;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.net.InetSocketAddress;
import java.util.*;

public class GameServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    private Map<String, Player> players;
    private Map<String, WebSocket> connections;
    private Set<String> readyPlayers;
    private boolean gameStarted;

    private GameEngine engine;
    private Gson gson;
    private int bidsReceived;
    private int currentHighestBid;
    private String currentHighestBidder;
    private List<String> turnOrder;
    private int currentPlayTurnIndex;
    private Trick currentTrick;
    private int targetScore = 21;

    private static final int TURN_TIMEOUT_MS = 30000;
    private Timer turnTimer;
    private int turnTimerGeneration = 0;

    public GameServer(int port) {
        super(new InetSocketAddress(port));
        this.players = new LinkedHashMap<>();
        this.connections = new HashMap<>();
        this.readyPlayers = new HashSet<>();
        this.gameStarted = false;

        this.gson = new Gson();
        this.bidsReceived = 0;
        this.currentHighestBid = 4;
        this.currentHighestBidder = null;
        this.turnOrder = new ArrayList<>();
        this.currentPlayTurnIndex = 0;
        this.currentTrick = new Trick();
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    private synchronized void cancelTurnTimer() {
        turnTimerGeneration++;
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }

    private synchronized void startTurnTimer(String currentPlayer) {
        cancelTurnTimer();
        final int gen = turnTimerGeneration;
        turnTimer = new Timer();
        turnTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (GameServer.this) {
                    if (gen != turnTimerGeneration || !gameStarted) {
                        return;
                    }
                    handleTurnTimeout(currentPlayer);
                }
            }
        }, TURN_TIMEOUT_MS);

        JsonObject timerMsg = new JsonObject();
        timerMsg.addProperty("type", "TIMER_START");
        timerMsg.addProperty("player", currentPlayer);
        timerMsg.addProperty("duration", TURN_TIMEOUT_MS);
        broadcastToAll(timerMsg.toString());
    }

    private void handleTurnTimeout(String currentPlayer) {
        logger.info("Turn timeout for player: {}", currentPlayer);

        JsonObject autoMsg = new JsonObject();
        autoMsg.addProperty("type", "AUTO_PLAYED");
        autoMsg.addProperty("player", currentPlayer);
        broadcastToAll(autoMsg.toString());

        if (bidsReceived < 4) {
            // Auto Phase 1 bid: pass (0)
            if (currentPlayer.equals(turnOrder.get(bidsReceived))) {
                processBidPhase1(currentPlayer, 0, Symbol.SPADES);
            }
        } else if (bidsReceived < 8) {
            // Auto Phase 2 bid: min valid bid
            if (currentPlayer.equals(turnOrder.get(bidsReceived - 4))) {
                int minBid = currentPlayer.equals(currentHighestBidder) ? currentHighestBid : 2;
                processBidPhase2(currentPlayer, minBid);
            }
        } else {
            // Auto card play: lowest valid card
            if (currentPlayer.equals(turnOrder.get(currentPlayTurnIndex))) {
                Player p = players.get(currentPlayer);
                if (p != null && !p.getHand().isEmpty()) {
                    Card autoCard = AutoPlayHelper.selectAutoPlayCard(p, engine.getSpecialSymbol(), currentTrick);
                    processPlayCard(currentPlayer, autoCard);
                }
            }
        }
    }

    private void broadcastToAll(String text) {
        for (WebSocket conn : connections.values()) {
            conn.send(text);
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

    private String getPlayerName(WebSocket conn) {
        for (Map.Entry<String, WebSocket> entry : connections.entrySet()) {
            if (entry.getValue().equals(conn))
                return entry.getKey();
        }
        return null;
    }

    public int getConnectionCount() {
        return connections.size();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection from: {}", conn.getRemoteSocketAddress());

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (getPlayerName(conn) == null) {
                    logger.warn("Closing connection from {} - no JOIN received within 10s",
                            conn.getRemoteSocketAddress());
                    conn.close();
                }
            }
        }, 10000);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        synchronized (this) {
            String name = getPlayerName(conn);
            if (name != null) {
                connections.remove(name);
                logger.info("{} disconnected.", name);

                if (!gameStarted) {
                    // If game hasn't started, remove them completely to free up the slot
                    players.remove(name);
                    readyPlayers.remove(name);
                    turnOrder.remove(name);
                }
                broadcastPlayersSync();
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.debug("Received message: {}", message);

        synchronized (this) {
            try {
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String action = jsonMessage.get("action").getAsString();

                if (action.equals("JOIN")) {
                    String rawName = jsonMessage.get("name").getAsString().trim();
                    String playerName = players.keySet().stream()
                            .filter(k -> k.equalsIgnoreCase(rawName))
                            .findFirst()
                            .orElse(rawName);

                    if (players.containsKey(playerName)) {
                        if (connections.containsKey(playerName)) {
                            WebSocket oldConn = connections.get(playerName);
                            if (oldConn != conn && oldConn.isOpen()) {
                                conn.send("{\"type\":\"ERROR\",\"message\":\"Name already taken!\"}");
                                return;
                            }
                        }

                        // Reconnect!
                        connections.put(playerName, conn);
                        logger.info("{} reconnected.", playerName);
                        broadcastPlayersSync();

                        // Send full state sync for reconnecting player
                        if (gameStarted) {
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
                        return;
                    }

                    if (players.size() >= 4) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"Lobby is full!\"}");
                        return;
                    }

                    Player newPlayer = new Player(playerName);
                    players.put(playerName, newPlayer);
                    connections.put(playerName, conn);
                    turnOrder.add(playerName);
                    logger.info("{} joined the lobby.", playerName);

                    broadcastPlayersSync();
                } else if (action.equals("READY")) {
                    String name = getPlayerName(conn);
                    if (name == null)
                        return;

                    synchronized (this) {
                        if (gameStarted || !readyPlayers.add(name))
                            return;

                        JsonObject readyUpdate = new JsonObject();
                        readyUpdate.addProperty("type", "READY_UPDATE");
                        JsonArray readyArray = new JsonArray();
                        for (String p : readyPlayers)
                            readyArray.add(p);
                        readyUpdate.add("readyPlayers", readyArray);
                        broadcastToAll(readyUpdate.toString());

                        if (readyPlayers.size() == 4) {
                            logger.info("All players ready! Starting game...");
                            readyPlayers.clear();
                            gameStarted = true;

                        if (engine == null) {
                            // First game: create fresh engine
                            engine = new GameEngine(new ArrayList<>(players.values()));
                        }
                        engine.dealPhaseOne();

                        // The first bidder is the player AFTER the dealer
                        int dealerIdx = engine.getDealerIndex();
                        int firstPlayerIdx = (dealerIdx + 1) % 4;
                        // Rotate turnOrder so the first player is at index 0
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
                                JsonObject stateUpdate = new JsonObject();
                                stateUpdate.addProperty("type", "GAME_START");
                                stateUpdate.addProperty("turn", turnOrder.get(0));
                                stateUpdate.addProperty("dealer", dealerName);
                                stateUpdate.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                                if (entry.getKey().equals(dealerName) && peekCard != null) {
                                    JsonObject peekObj = new JsonObject();
                                    peekObj.addProperty("symbol", peekCard.getSymbol().toString());
                                    peekObj.addProperty("rank", peekCard.getRank().toString());
                                    stateUpdate.add("peekCard", peekObj);
                                }
                                playerConn.send(stateUpdate.toString());
                            }
                        }

                        startTurnTimer(turnOrder.get(0));
                    }
                    } // end synchronized

                } else if (action.equals("BID_PHASE_1")) {
                    String name = getPlayerName(conn);
                    if (name == null)
                        return;

                    if (!name.equals(turnOrder.get(bidsReceived))) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                        return;
                    }

                    int bidAmount = jsonMessage.get("amount").getAsInt();

                    if (bidAmount > 0) {
                        if (bidAmount <= currentHighestBid) {
                            conn.send(
                                    "{\"type\":\"ERROR\",\"message\":\"You must bid higher than the current highest bid!\"}");
                            return;
                        }
                    }

                    Symbol trump = Symbol.SPADES;
                    if (jsonMessage.has("trump")) {
                        try {
                            trump = Symbol.valueOf(jsonMessage.get("trump").getAsString());
                        } catch (Exception e) {
                        }
                    }

                    cancelTurnTimer();
                    processBidPhase1(name, bidAmount, trump);

                } else if (action.equals("BID_PHASE_2")) {
                    String name = getPlayerName(conn);
                    if (name == null)
                        return;

                    if (!name.equals(turnOrder.get(bidsReceived - 4))) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                        return;
                    }

                    int bidAmount = jsonMessage.get("amount").getAsInt();

                    if (name.equals(currentHighestBidder)) {
                        if (bidAmount < currentHighestBid) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid at least your Phase 1 bid ("
                                    + currentHighestBid + ")!\"}");
                            return;
                        }
                    } else {
                        if (bidAmount < 2) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"Minimum bid is 2!\"}");
                            return;
                        }
                    }

                    cancelTurnTimer();
                    processBidPhase2(name, bidAmount);

                } else if (action.equals("PLAY_CARD")) {
                    String name = getPlayerName(conn);
                    if (name == null)
                        return;
                    Player p = players.get(name);

                    if (!name.equals(turnOrder.get(currentPlayTurnIndex))) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to play!\"}");
                        return;
                    }

                    Symbol symbol = Symbol.valueOf(jsonMessage.get("symbol").getAsString());
                    Rank rank = Rank.valueOf(jsonMessage.get("rank").getAsString());
                    Card cardToPlay = new Card(symbol, rank);

                    if (!RuleValidator.isValidPlay(p, cardToPlay, engine.getSpecialSymbol(), currentTrick)) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid play! Please follow the game rules.\"}");
                        return;
                    }

                    cancelTurnTimer();
                    processPlayCard(name, cardToPlay);
                }
            } catch (Exception e) {
                logger.error("Error processing message", e);
            }
        }
    }

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
            if (currentHighestBidder == null) {
                engine.setSpecialSymbol(Symbol.SPADES);
            }
            engine.dealPhaseTwo();

            for (Map.Entry<String, Player> entry : players.entrySet()) {
                WebSocket playerConn = connections.get(entry.getKey());
                if (playerConn != null) {
                    JsonObject stateUpdate = new JsonObject();
                    stateUpdate.addProperty("type", "PHASE_2_START");
                    stateUpdate.addProperty("turn", turnOrder.get(0));
                    stateUpdate.addProperty("finalTrump", engine.getSpecialSymbol().toString());
                    stateUpdate.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                    playerConn.send(stateUpdate.toString());
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
        if (p != null) {
            p.setBidPoints(bidAmount);
        }
        bidsReceived++;

        JsonObject bidUpdate = new JsonObject();
        bidUpdate.addProperty("type", "BID_2_UPDATE");
        bidUpdate.addProperty("player", name);
        bidUpdate.addProperty("amount", bidAmount);

        if (bidsReceived == 8) {
            broadcastPlayersSync();

            int totalBid = 0;
            for (Player pl : players.values())
                totalBid += pl.getBidPoints();

            if (totalBid <= 10) {
                logger.info("Total bid {} <= 10, auto-skipping round.", totalBid);
                engine.returnAllHands();

                JsonArray scoresArray = new JsonArray();
                Player highestScorer = null;
                Player lowestScorer = null;

                for (Player player : players.values()) {
                    int bid = player.getBidPoints();
                    player.setTricksWon(bid);
                    player.incrementTotalScore(bid);

                    if (highestScorer == null || player.getTotalScore() > highestScorer.getTotalScore()) {
                        highestScorer = player;
                    }
                    if (lowestScorer == null || player.getTotalScore() < lowestScorer.getTotalScore()) {
                        lowestScorer = player;
                    }

                    JsonObject playerScore = new JsonObject();
                    playerScore.addProperty("player", player.getName());
                    playerScore.addProperty("bid", bid);
                    playerScore.addProperty("tricks", bid);
                    playerScore.addProperty("pointsEarned", bid);
                    playerScore.addProperty("totalScore", player.getTotalScore());
                    scoresArray.add(playerScore);
                }

                JsonObject roundUpdate = new JsonObject();
                if (highestScorer.getTotalScore() >= targetScore) {
                    roundUpdate.addProperty("type", "GAME_OVER");
                    roundUpdate.addProperty("winner", highestScorer.getName());
                } else {
                    roundUpdate.addProperty("type", "ROUND_OVER");
                }
                roundUpdate.addProperty("autoSkipped", true);
                roundUpdate.add("scores", scoresArray);
                broadcastToAll(roundUpdate.toString());

                List<String> baseOrder = new ArrayList<>(players.keySet());
                int loserIdx = baseOrder.indexOf(lowestScorer.getName());
                engine.setDealerIndex(loserIdx);

                for (Player player : players.values())
                    player.reset();
                broadcastPlayersSync();
                gameStarted = false;
                cancelTurnTimer();
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
                JsonArray scoresArray = new JsonArray();
                Player highestScorer = null;

                for (Player player : players.values()) {
                    int bid = player.getBidPoints();
                    int tricks = player.getTricksWon();
                    int pointsEarned = (tricks >= bid) ? bid : -bid;

                    player.incrementTotalScore(pointsEarned);

                    if (highestScorer == null || player.getTotalScore() > highestScorer.getTotalScore()) {
                        highestScorer = player;
                    }

                    JsonObject playerScore = new JsonObject();
                    playerScore.addProperty("player", player.getName());
                    playerScore.addProperty("bid", bid);
                    playerScore.addProperty("tricks", tricks);
                    playerScore.addProperty("pointsEarned", pointsEarned);
                    playerScore.addProperty("totalScore", player.getTotalScore());
                    scoresArray.add(playerScore);
                }

                JsonObject roundUpdate = new JsonObject();
                if (highestScorer.getTotalScore() >= targetScore) {
                    roundUpdate.addProperty("type", "GAME_OVER");
                    roundUpdate.addProperty("winner", highestScorer.getName());
                } else {
                    roundUpdate.addProperty("type", "ROUND_OVER");
                }
                roundUpdate.add("scores", scoresArray);
                broadcastToAll(roundUpdate.toString());

                Player lowestScorer = null;
                for (Player player : players.values()) {
                    if (lowestScorer == null || player.getTotalScore() < lowestScorer.getTotalScore()) {
                        lowestScorer = player;
                    }
                }
                List<String> baseOrder = new ArrayList<>(players.keySet());
                int loserIdx = baseOrder.indexOf(lowestScorer.getName());
                engine.setDealerIndex(loserIdx);

                for (Player player : players.values())
                    player.reset();
                broadcastPlayersSync();
                gameStarted = false;
                cancelTurnTimer();
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

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error occurred: ", ex);
    }

    @Override
    public void onStart() {
        logger.info("WebSocket Server started successfully on port: {}", getPort());
    }

    public static void main(String[] args) {
        int port = 8887;
        GameServer server = new GameServer(port);
        server.start();
    }
}
