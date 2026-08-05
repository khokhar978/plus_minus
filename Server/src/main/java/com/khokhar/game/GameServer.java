package com.khokhar.game;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameServer extends WebSocketServer {
    
    // The lobby maps the physical WebSocket connection to the Player object
    private Map<WebSocket, Player> lobby;
    private GameEngine engine;
    private Gson gson;
    private int bidsReceived;
    private int currentHighestBid;
    private String currentHighestBidder;
    private List<WebSocket> turnOrder; // Used to lock in the turn order
    private int currentPlayTurnIndex;
    private Trick currentTrick;

    public GameServer(int port) {
        super(new InetSocketAddress(port));
        this.lobby = new HashMap<>();
        this.gson = new Gson();
        this.bidsReceived = 0;
        this.currentHighestBid = 4;
        this.currentHighestBidder = null;
        this.turnOrder = new ArrayList<>();
        this.currentPlayTurnIndex = 0;
        this.currentTrick = new Trick();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress());
        // If they were in the lobby waiting, remove them so they don't take up a slot!
        if (lobby.containsKey(conn)) {
            System.out.println(lobby.get(conn).getName() + " disconnected.");
            lobby.remove(conn);
        }
        
        // TODO: Handle mid-game disconnects.
        // If engine != null (game started), wait 15 seconds. If no reconnect, auto-play their turn!
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message: " + message);
        
        // Use synchronized to prevent race conditions when a client spams messages quickly
        synchronized (this) {
            try {
                JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
                String action = jsonMessage.get("action").getAsString();
            
            if (action.equals("JOIN")) {
                String playerName = jsonMessage.get("name").getAsString();
                
                // 1. Check for duplicate name
                boolean isDuplicate = false;
                for (Player p : lobby.values()) {
                    if (p.getName().equalsIgnoreCase(playerName)) {
                        isDuplicate = true;
                        break;
                    }
                }
                
                // 2. Reject if duplicate
                if (isDuplicate) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"Name already taken!\"}");
                    return; // Stop here, don't add them to the lobby
                }

                // 3. Accept if unique
                Player newPlayer = new Player(playerName);
                lobby.put(conn, newPlayer);
                System.out.println(playerName + " joined the lobby. Total players: " + lobby.size());
                
                // Broadcast the lobby count to everyone waiting
                JsonObject lobbyUpdate = new JsonObject();
                lobbyUpdate.addProperty("type", "LOBBY_UPDATE");
                lobbyUpdate.addProperty("count", lobby.size());
                broadcast(lobbyUpdate.toString());
                
                if (lobby.size() == 4) {
                    System.out.println("Lobby full! Starting GameEngine...");
                    
                    // Lock in the turn order based on join order
                    turnOrder = new ArrayList<>(lobby.keySet());
                    
                    // We extract the players from the map into a List for the GameEngine
                    engine = new GameEngine(new ArrayList<>(lobby.values()));
                    engine.dealPhaseOne();
                    
                    // Broadcast the starting hands back to all clients
                    for (Map.Entry<WebSocket, Player> entry : lobby.entrySet()) {
                        WebSocket playerConn = entry.getKey();
                        Player p = entry.getValue();
                        
                        JsonObject stateUpdate = new JsonObject();
                        stateUpdate.addProperty("type", "GAME_START");
                        // Tell everyone whose turn it is to bid!
                        stateUpdate.addProperty("turn", lobby.get(turnOrder.get(0)).getName()); 
                        // Serialize this specific player's hand into JSON
                        stateUpdate.add("yourHand", gson.toJsonTree(p.getHand()));
                        
                        playerConn.send(stateUpdate.toString());
                    }
                }
            } else if (action.equals("BID_PHASE_1")) {
                Player p = lobby.get(conn);
                if (p == null) return;
                
                // 1. Check if it's actually their turn to bid!
                if (!conn.equals(turnOrder.get(bidsReceived))) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                    return; 
                }
                
                int bidAmount = jsonMessage.get("amount").getAsInt();
                
                if (bidAmount > 0) { // If they didn't skip
                    if (bidAmount <= currentHighestBid) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid higher than the current highest bid!\"}");
                        return;
                    }
                    currentHighestBid = bidAmount;
                    currentHighestBidder = p.getName();
                    engine.setSpecialSymbol(Symbol.valueOf(jsonMessage.get("trump").getAsString()));
                }
                
                bidsReceived++;
                
                JsonObject bidUpdate = new JsonObject();
                bidUpdate.addProperty("type", "BID_1_UPDATE");
                bidUpdate.addProperty("highestBid", currentHighestBid);
                bidUpdate.addProperty("highestBidder", currentHighestBidder != null ? currentHighestBidder : "None");
                
                if (bidsReceived == 4) {
                    if (currentHighestBidder == null) {
                        engine.setSpecialSymbol(Symbol.SPADES); // Default trump if everyone skips
                    }
                    engine.dealPhaseTwo();
                    
                    for (Map.Entry<WebSocket, Player> entry : lobby.entrySet()) {
                        JsonObject stateUpdate = new JsonObject();
                        stateUpdate.addProperty("type", "PHASE_2_START");
                        stateUpdate.addProperty("turn", lobby.get(turnOrder.get(0)).getName()); 
                        stateUpdate.addProperty("finalTrump", engine.getSpecialSymbol().toString());
                        stateUpdate.add("yourHand", gson.toJsonTree(entry.getValue().getHand()));
                        entry.getKey().send(stateUpdate.toString());
                    }
                } else {
                    bidUpdate.addProperty("nextTurn", lobby.get(turnOrder.get(bidsReceived)).getName());
                    broadcast(bidUpdate.toString()); 
                }
                
            } else if (action.equals("BID_PHASE_2")) {
                Player p = lobby.get(conn);
                if (p == null) return;
                
                if (!conn.equals(turnOrder.get(bidsReceived - 4))) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to bid!\"}");
                    return; 
                }
                
                int bidAmount = jsonMessage.get("amount").getAsInt();
                
                if (p.getName().equals(currentHighestBidder)) {
                    if (bidAmount < currentHighestBid) {
                        conn.send("{\"type\":\"ERROR\",\"message\":\"You must bid at least your Phase 1 bid (" + currentHighestBid + ")!\"}");
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
                bidUpdate.addProperty("player", p.getName());
                bidUpdate.addProperty("amount", bidAmount);
                
                if (bidsReceived == 8) {
                    JsonObject stateUpdate = new JsonObject();
                    stateUpdate.addProperty("type", "GAME_READY");
                    stateUpdate.addProperty("turn", lobby.get(turnOrder.get(0)).getName()); 
                    broadcast(stateUpdate.toString());
                } else {
                    bidUpdate.addProperty("nextTurn", lobby.get(turnOrder.get(bidsReceived - 4)).getName());
                    broadcast(bidUpdate.toString()); 
                }
            } else if (action.equals("NEXT_ROUND")) {
                // Anyone can trigger the next round
                engine = new GameEngine(new ArrayList<>(lobby.values()));
                engine.dealPhaseOne();
                
                for (Map.Entry<WebSocket, Player> entry : lobby.entrySet()) {
                    WebSocket playerConn = entry.getKey();
                    Player p = entry.getValue();
                    
                    JsonObject stateUpdate = new JsonObject();
                    stateUpdate.addProperty("type", "GAME_START");
                    stateUpdate.addProperty("turn", lobby.get(turnOrder.get(0)).getName()); 
                    stateUpdate.add("yourHand", gson.toJsonTree(p.getHand()));
                    
                    playerConn.send(stateUpdate.toString());
                }
                
                bidsReceived = 0;
                currentHighestBid = 4;
                currentHighestBidder = null;
                currentPlayTurnIndex = 0;
            } else if (action.equals("PLAY_CARD")) {
                Player p = lobby.get(conn);
                if (p == null) return;
                
                // 1. Enforce turn order for playing
                if (!conn.equals(turnOrder.get(currentPlayTurnIndex))) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"It is not your turn to play!\"}");
                    return; 
                }
                
                // 2. Parse the card they want to play
                Symbol symbol = Symbol.valueOf(jsonMessage.get("symbol").getAsString());
                Rank rank = Rank.valueOf(jsonMessage.get("rank").getAsString());
                Card cardToPlay = new Card(symbol, rank);
                
                // 3. Strict Rule Validation!
                if (!RuleValidator.isValidPlay(p, cardToPlay, engine.getSpecialSymbol(), currentTrick)) {
                    conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid play! Please follow the game rules.\"}");
                    return;
                }
                
                // 4. Execute the legal play
                p.removeCard(cardToPlay); 
                currentTrick.playCard(p, cardToPlay, engine.getSpecialSymbol());
                
                // Broadcast to everyone what card was thrown on the table
                JsonObject playUpdate = new JsonObject();
                playUpdate.addProperty("type", "CARD_PLAYED");
                playUpdate.addProperty("player", p.getName());
                playUpdate.addProperty("symbol", symbol.toString());
                playUpdate.addProperty("rank", rank.toString());
                broadcast(playUpdate.toString());
                
                // 5. Check if the trick (round of 4 cards) is finished
                if (currentTrick.isComplete()) {
                    Player winner = currentTrick.getCurrentlyWinning();
                    winner.incrementTricksWon();
                    
                    // 1. Announce the winner of this trick
                    JsonObject winnerUpdate = new JsonObject();
                    winnerUpdate.addProperty("type", "TRICK_WINNER");
                    winnerUpdate.addProperty("winner", winner.getName());
                    broadcast(winnerUpdate.toString());
                    
                    // 2. Find the winner's index to lead the next trick
                    WebSocket winnerConn = null;
                    for (Map.Entry<WebSocket, Player> entry : lobby.entrySet()) {
                        if (entry.getValue() == winner) {
                            winnerConn = entry.getKey();
                            break;
                        }
                    }
                    currentPlayTurnIndex = turnOrder.indexOf(winnerConn);
                    
                    // 3. Reset the table for the next trick
                    currentTrick = new Trick();
                    
                    // 4. Check if the entire round is over (13 tricks played)
                    if (winner.getHand().isEmpty()) {
                        
                        JsonArray scoresArray = new JsonArray();
                        Player highestScorer = null;
                        
                        for (Player player : lobby.values()) {
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
                        broadcast(roundUpdate.toString());
                        
                        // Reset for next round
                        for (Player player : lobby.values()) player.reset();
                        bidsReceived = 0;
                        currentHighestBid = 4;
                        currentHighestBidder = null;
                        
                    } else {
                        // Start the next trick with the winner
                        JsonObject turnUpdate = new JsonObject();
                        turnUpdate.addProperty("type", "NEXT_TURN");
                        turnUpdate.addProperty("turn", winner.getName());
                        broadcast(turnUpdate.toString());
                    }
                } else {
                    // Pass the turn to the next player clockwise
                    currentPlayTurnIndex = (currentPlayTurnIndex + 1) % 4;
                    JsonObject turnUpdate = new JsonObject();
                    turnUpdate.addProperty("type", "NEXT_TURN");
                    turnUpdate.addProperty("turn", lobby.get(turnOrder.get(currentPlayTurnIndex)).getName());
                    broadcast(turnUpdate.toString());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing message: " + e.getMessage());
        }
        } // End synchronized block
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
