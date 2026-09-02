package com.khokhar.game;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses incoming WebSocket messages and dispatches them to the correct Room.
 * This is the only place that reads raw JSON from the client.
 *
 * Protocol:
 *   CREATE_ROOM  — create a new room; no room needed yet
 *   JOIN_ROOM    — join an existing room by code; no room needed yet
 *   JOIN         — join the lobby inside a room (requires being in a room first)
 *   READY        — mark yourself ready
 *   BID_PHASE_1  — place a phase-1 bid
 *   BID_PHASE_2  — place a phase-2 bid
 *   PLAY_CARD    — play a card during a trick
 */
public class MessageRouter {

    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);

    private final RoomManager roomManager;
    private final Gson gson;

    public MessageRouter(RoomManager roomManager) {
        this.roomManager = roomManager;
        this.gson = new Gson();
    }

    public void route(WebSocket conn, String message) {
        JsonObject msg;
        try {
            msg = gson.fromJson(message, JsonObject.class);
        } catch (Exception e) {
            logger.warn("Malformed JSON from {}: {}", conn.getRemoteSocketAddress(), message);
            conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid message format.\"}");
            return;
        }

        if (!msg.has("action")) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"Missing action field.\"}");
            return;
        }

        String action = msg.get("action").getAsString();

        // ── Pre-room actions (no room required) ───────────────────────────────
        if ("CREATE_ROOM".equals(action)) {
            Room room = roomManager.createRoom(conn);
            if (room != null) {
                // Register but don't JOIN yet — client still needs to send JOIN with their name
                roomManager.registerConnection(conn, room.getRoomCode());
                JsonObject resp = new JsonObject();
                resp.addProperty("type", "ROOM_CREATED");
                resp.addProperty("roomCode", room.getRoomCode());
                conn.send(resp.toString());
                logger.info("Room {} created.", room.getRoomCode());
            }
            return;
        }

        if ("JOIN_ROOM".equals(action)) {
            String code = getStringField(msg, "code");
            if (code == null) {
                conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Room code required.\"}");
                return;
            }
            Room room = roomManager.joinRoom(code, conn);
            if (room != null) {
                JsonObject resp = new JsonObject();
                resp.addProperty("type", "ROOM_JOINED");
                resp.addProperty("roomCode", room.getRoomCode());
                conn.send(resp.toString());
                logger.info("{} joined room {}.", conn.getRemoteSocketAddress(), room.getRoomCode());
            }
            // If room is null, RoomManager already sent the error
            return;
        }

        // ── In-room actions (room required) ───────────────────────────────────
        Room room = roomManager.getRoomForConnection(conn);
        if (room == null) {
            conn.send("{\"type\":\"ERROR\",\"message\":\"You are not in a room. Please create or join one first.\"}");
            return;
        }

        synchronized (room) {
            try {
                switch (action) {
                    case "JOIN": {
                        String name = getStringField(msg, "name");
                        if (name == null || name.isEmpty()) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"Name is required.\"}");
                            return;
                        }
                        // Sanitize: max 16 chars, strip non-alphanumeric/space
                        name = name.trim().replaceAll("[^a-zA-Z0-9 _-]", "");
                        if (name.length() > 16) name = name.substring(0, 16);
                        if (name.isEmpty()) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid name.\"}");
                            return;
                        }
                        room.handleJoin(conn, name);
                        break;
                    }

                    case "READY":
                        room.handleReady(conn);
                        break;

                    case "BID_PHASE_1": {
                        int bidAmount = getIntField(msg, "amount", 0);
                        Symbol trump = Symbol.SPADES;
                        if (msg.has("trump")) {
                            try { trump = Symbol.valueOf(msg.get("trump").getAsString()); }
                            catch (Exception ignored) {}
                        }
                        room.handleBidPhase1(conn, bidAmount, trump);
                        break;
                    }

                    case "BID_PHASE_2": {
                        int bidAmount = getIntField(msg, "amount", 2);
                        room.handleBidPhase2(conn, bidAmount);
                        break;
                    }

                    case "PLAY_CARD": {
                        String symbolStr = getStringField(msg, "symbol");
                        String rankStr = getStringField(msg, "rank");
                        if (symbolStr == null || rankStr == null) {
                            conn.send("{\"type\":\"ERROR\",\"message\":\"symbol and rank required.\"}");
                            return;
                        }
                        Symbol symbol = Symbol.valueOf(symbolStr);
                        Rank rank = Rank.valueOf(rankStr);
                        room.handlePlayCard(conn, symbol, rank);
                        break;
                    }

                    default:
                        logger.warn("Unknown action '{}' from {}", action, conn.getRemoteSocketAddress());
                        conn.send("{\"type\":\"ERROR\",\"message\":\"Unknown action: " + action + "\"}");
                }
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid enum value in message: {}", message);
                conn.send("{\"type\":\"ERROR\",\"message\":\"Invalid card symbol or rank.\"}");
            } catch (Exception e) {
                logger.error("Error processing action '{}': ", action, e);
                conn.send("{\"type\":\"ERROR\",\"message\":\"Server error. Please try again.\"}");
            }
        }
    }

    // ── JSON field helpers ────────────────────────────────────────────────────

    private String getStringField(JsonObject msg, String field) {
        if (msg.has(field) && !msg.get(field).isJsonNull()) {
            return msg.get(field).getAsString();
        }
        return null;
    }

    private int getIntField(JsonObject msg, String field, int defaultValue) {
        if (msg.has(field) && !msg.get(field).isJsonNull()) {
            try { return msg.get(field).getAsInt(); }
            catch (Exception ignored) {}
        }
        return defaultValue;
    }
}
