package com.khokhar.game;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.net.InetSocketAddress;
import java.util.*;

public class GameServer extends WebSocketServer {
    
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
            if (entry.getValue().equals(conn)) return entry.getKey();
        }
        return null;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = getPlayerName(conn);
        if (name != null) {
            connections.remove(name);
            System.out.println(name + " disconnected.");
            
            if (!gameStarted) {
                // If game hasn't started, remove them completely to free up the slot
                players.remove(name);
                readyPlayers.remove(name);
                turnOrder.remove(name);
            }
            broadcastPlayersSync();
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);
        
        synchronized (this) {
            try {
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String action = jsonMessage.get("action").getAsString();
            
                if (action.equals("JOIN")) {
                    String playerName = jsonMessage.get("name").getAsString();
                    
                    if (players.containsKey(playerName)) {
                        if (connections.containsKey(playerName)) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"Name already taken or player is already connected!\"}");
                            return;
                        } else {
                            // Reconnect!
                            connections.put(playerName, conn);
                            System.out.println(playerName + " reconnected.");
                            broadcastPlayersSync();
                            
                            // Send full state sync for reconnecting player
                            if (gameStarted) {
                                Player p = players.get(playerName);
                                JsonObject stateUpdate = new JsonObject();
                                
                                if (bidsReceived < 4) {
                                    stateUpdate.addProperty("type", "GAME_START");
                                    stateUpdate.addProperty("turn", turnOrder.get(bidsReceived)); 
                                } else if (bidsReceived < 8) {
                                    stateUpdate.addProperty("type", "PHASE_2_START");
                                    stateUpdate.addProperty("turn", turnOrder.get(bidsReceived - 4));
                                    stateUpdate.addProperty("finalTrump", engine.getSpecialSymbol().toString());
                                } else {
                                    stateUpdate.addProperty("type", "GAME_READY");
                                    stateUpdate.addProperty("turn", turnOrder.get(currentPlayTurnIndex));
                                }
                                stateUpdate.add("yourHand", gson.toJsonTree(p.getHand()));
                                conn.send(stateUpdate.toString());
                            }
                            return;
                        }
                    }
                    
                    if (players.size() >= 4) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"Lobby is full!\"}");
                        return;
                    }
                    
                    Player newPlayer = new Player(playerName);
                    players.put(playerName, newPlayer);
                    connections.put(playerName, conn);
                    turnOrder.add(playerName);
                    System.out.println(playerName + " joined the lobby.");
                    
                    broadcastPlayersSync();
                
                } else if (action.equals("READY")) {
                    String name = getPlayerName(conn);
                    if (name == null) return;
                    
                    readyPlayers.add(name);
                    
                    JsonObject readyUpdate = new JsonObject();
                    readyUpdate.addProperty("type", "READY_UPDATE");
                    JsonArray readyArray = new JsonArray();
                    for (String p : readyPlayers) readyArray.add(p);
                    readyUpdate.add("readyPlayers", readyArray);
                    broadcastToAll(readyUpdate.toString());
                    
                    if (readyPlayers.size() == 4) {
                        System.out.println("All players ready! Starting game...");
                        readyPlayers.clear(); 
                        gameStarted = true;
                        
                        engine = new GameEngine(new ArrayList<>(players.values()));
                        engine.dealPhaseOne();
                        
                        bidsReceived = 0;
                        currentHighestBid = 4;
                        currentHighestBidder = null;
                        currentPlayTurnIndex = 0;
                        currentTrick = new Trick();
                        
                        for (Map.Entry<String, Player> entry : players.entrySet()) {
                            WebSocket playerConn = connections.get(entry.getKey());
                            if (playerConn != null) {
                                JsonObject stateUpdate = new JsonObject();
                                stateUpdate.addProperty("type", "GAME_START");
                                stateUpdate.addProperty("turn", turnOrder.get(0)); 
                                stateUpdate.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                                playerConn.send(stateUpdate.toString());
                            }
                        }
                    }

                } else if (action.equals("BID_PHASE_1")) {
                    String name = getPlayerName(conn);
                    if (name == null) return;
                    
                    if (!name.equals(turnOrder.get(bidsReceived))) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                        return; 
                    }
                    
                    int bidAmount = jsonMessage.get("amount").getAsInt();
                    
                    if (bidAmount > 0) { 
                        if (bidAmount <= currentHighestBid) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid higher than the current highest bid!\"}");
                            return;
                        }
                        currentHighestBid = bidAmount;
                        currentHighestBidder = name;
                        engine.setSpecialSymbol(Symbol.valueOf(jsonMessage.get("trump").getAsString()));
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
                    } else {
                        bidUpdate.addProperty("nextTurn", turnOrder.get(bidsReceived));
                        broadcastToAll(bidUpdate.toString()); 
                    }
                    
                } else if (action.equals("BID_PHASE_2")) {
                    String name = getPlayerName(conn);
                    if (name == null) return;
                    
                    if (!name.equals(turnOrder.get(bidsReceived - 4))) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                        return; 
                    }
                    
                    int bidAmount = jsonMessage.get("amount").getAsInt();
                    Player p = players.get(name);
                    
                    if (name.equals(currentHighestBidder)) {
                        if (bidAmount < currentHighestBid) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid at least your Phase 1 bid (\" + currentHighestBid + \")!\"}");
                            return;
                        }
                    } else {
                        if (bidAmount < 2) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"Minimum bid is 2!\"}");
                            return;
                        }
                    }
                    
                    p.setBidPoints(bidAmount);
                    bidsReceived++;
                    
                    JsonObject bidUpdate = new JsonObject();
                    bidUpdate.addProperty("type", "BID_2_UPDATE");
                    bidUpdate.addProperty("player", name);
                    bidUpdate.addProperty("amount", bidAmount);
                    
                    if (bidsReceived == 8) {
                        broadcastPlayersSync(); // Sync bids for everyone
                        JsonObject stateUpdate = new JsonObject();
                        stateUpdate.addProperty("type", "GAME_READY");
                        stateUpdate.addProperty("turn", turnOrder.get(0)); 
                        broadcastToAll(stateUpdate.toString());
                    } else {
                        broadcastPlayersSync(); // Sync bids for everyone incrementally
                        bidUpdate.addProperty("nextTurn", turnOrder.get(bidsReceived - 4));
                        broadcastToAll(bidUpdate.toString()); 
                    }

                } else if (action.equals("PLAY_CARD")) {
                    String name = getPlayerName(conn);
                    if (name == null) return;
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
                    
                    p.removeCard(cardToPlay);
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
                        broadcastPlayersSync(); // Sync new tricks won
                        
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
                            if (highestScorer.getTotalScore() >= 21) {
                                roundUpdate.addProperty("type", "GAME_OVER");
                                roundUpdate.addProperty("winner", highestScorer.getName());
                            } else {
                                roundUpdate.addProperty("type", "ROUND_OVER");
                            }
                            roundUpdate.add("scores", scoresArray);
                            broadcastToAll(roundUpdate.toString());
                            
                            for (Player player : players.values()) player.reset();
                            broadcastPlayersSync();
                            gameStarted = false; // Need to READY up again for next round
                        } else {
                            JsonObject turnUpdate = new JsonObject();
                            turnUpdate.addProperty("type", "NEXT_TURN");
                            turnUpdate.addProperty("turn", winner.getName());
                            broadcastToAll(turnUpdate.toString());
                        }
                    } else {
                        currentPlayTurnIndex = (currentPlayTurnIndex + 1) % 4;
                        JsonObject turnUpdate = new JsonObject();
                        turnUpdate.addProperty("type", "NEXT_TURN");
                        turnUpdate.addProperty("turn", turnOrder.get(currentPlayTurnIndex));
                        broadcastToAll(turnUpdate.toString());
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error parsing message: " + e.getMessage());
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket Server started successfully on port: " + getPort());
    }

    public static void main(String[] args) {
        int port = 8887; 
        GameServer server = new GameServer(port);
        server.start();
    }
}
