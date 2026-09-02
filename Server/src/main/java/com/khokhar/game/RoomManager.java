package com.khokhar.game;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Manages all active rooms.
 * Responsibilities:
 *   - Generating unique 4-character room codes
 *   - Creating and destroying rooms (with idle timers)
 *   - Tracking which WebSocket connection belongs to which room
 *   - Rate-limiting room creation and join attempts per IP
 *   - Enforcing the max concurrent rooms cap
 */
public class RoomManager {

    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final int MAX_ROOMS = 5;
    private static final int IDLE_TIMEOUT_LOBBY_SECS  = 120;   // 2 min if pre-game
    private static final int IDLE_TIMEOUT_INGAME_SECS = 300;   // 5 min if mid-game
    private static final int IDLE_TIMEOUT_EMPTY_ROOM_SECS = 300; // 5 min if never joined

    // Security rate-limiting
    private static final int MAX_CREATE_PER_MINUTE = 3;
    private static final int MAX_JOIN_FAIL_PER_MINUTE = 10;

    // ── State ─────────────────────────────────────────────────────────────────
    private final Map<String, Room> rooms = new HashMap<>();           // roomCode → Room
    private final Map<WebSocket, String> connToRoom = new HashMap<>(); // conn → roomCode

    // Rate limiting: IP → (timestamp deque)
    private final Map<String, Deque<Long>> createAttempts = new HashMap<>();
    private final Map<String, Deque<Long>> joinFailAttempts = new HashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "room-idle-timer");
                t.setDaemon(true);
                return t;
            });

    private static final String ALPHANUMERIC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/I/1

    // ── Room Creation ─────────────────────────────────────────────────────────

    /**
     * Creates a new room for the given connection.
     * Returns the Room on success, or null if rate-limited or at capacity.
     * The caller must send the appropriate error to the client.
     */
    public synchronized Room createRoom(WebSocket conn) {
        String ip = getIp(conn);

        if (!checkRateLimit(createAttempts, ip, MAX_CREATE_PER_MINUTE)) {
            logger.warn("Rate limit hit for room creation from IP: {}", ip);
            conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Too many rooms created. Please wait a moment.\"}");
            return null;
        }

        if (rooms.size() >= MAX_ROOMS) {
            conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Server is at capacity. Please try again later.\"}");
            return null;
        }

        String code = generateUniqueCode();
        Room room = new Room(code);
        rooms.put(code, room);
        logger.info("Room {} created by {}.", code, ip);

        // Schedule destruction if room stays completely empty (no one joins)
        scheduleDestroy(code, IDLE_TIMEOUT_EMPTY_ROOM_SECS);

        return room;
    }

    /**
     * Joins a connection to an existing room.
     * Returns the Room on success, or null if not found or rate-limited.
     */
    public synchronized Room joinRoom(String code, WebSocket conn) {
        String ip = getIp(conn);
        String normalizedCode = code.trim().toUpperCase();
        Room room = rooms.get(normalizedCode);

        if (room == null) {
            recordFailedJoin(ip, conn);
            return null;
        }

        if (room.getPlayerCount() >= 4) {
            conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Room is full!\"}");
            return null;
        }

        connToRoom.put(conn, normalizedCode);
        room.cancelIdleTimer(); // room is active — cancel any pending destruction
        return room;
    }

    /**
     * Registers a connection→room mapping after a successful JOIN action inside a room.
     * Called from MessageRouter after handleJoin succeeds.
     */
    public synchronized void registerConnection(WebSocket conn, String roomCode) {
        connToRoom.put(conn, roomCode);
    }

    // ── Disconnect Handling ───────────────────────────────────────────────────

    /**
     * Called by GameServer.onClose(). Notifies the room and schedules
     * destruction if the room becomes completely empty.
     */
    public synchronized void handleDisconnect(WebSocket conn) {
        String roomCode = connToRoom.remove(conn);
        if (roomCode == null) return;

        Room room = rooms.get(roomCode);
        if (room == null) return;

        room.handleDisconnect(conn);

        // Grace period only starts when the room becomes COMPLETELY empty
        if (room.isEmpty()) {
            long delay = room.isGameStarted() ? IDLE_TIMEOUT_INGAME_SECS : IDLE_TIMEOUT_LOBBY_SECS;
            logger.info("Room {} is now empty. Scheduling destruction in {}s.", roomCode, delay);
            scheduleDestroy(roomCode, delay);
        } else {
            room.cancelIdleTimer(); // someone still connected — cancel pending destroy
        }
    }

    // ── Room Lookup ───────────────────────────────────────────────────────────

    public synchronized Room getRoomForConnection(WebSocket conn) {
        String roomCode = connToRoom.get(conn);
        return roomCode != null ? rooms.get(roomCode) : null;
    }

    // ── Room Destruction ──────────────────────────────────────────────────────

    private void scheduleDestroy(String roomCode, long delaySeconds) {
        Room room = rooms.get(roomCode);
        if (room == null) return;

        room.cancelIdleTimer(); // cancel any existing idle timer first
        ScheduledFuture<?> future = scheduler.schedule(
                () -> destroyRoom(roomCode),
                delaySeconds, TimeUnit.SECONDS
        );
        room.setIdleTimer(future);
    }

    /**
     * Destroys a room and frees all associated resources.
     * Only called when the room is already empty (no one to notify).
     */
    public synchronized void destroyRoom(String roomCode) {
        Room room = rooms.remove(roomCode);
        if (room == null) return; // already destroyed

        room.cancelIdleTimer();
        room.cancelTurnTimer();

        // Clean up any stale connection→room mappings
        connToRoom.entrySet().removeIf(e -> roomCode.equals(e.getValue()));

        logger.info("Room {} destroyed.", roomCode);
    }

    // ── Code Generation ───────────────────────────────────────────────────────

    private String generateUniqueCode() {
        Random rng = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                sb.append(ALPHANUMERIC.charAt(rng.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    // ── Security Helpers ──────────────────────────────────────────────────────

    private String getIp(WebSocket conn) {
        try {
            return conn.getRemoteSocketAddress().getAddress().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Sliding-window rate limiter.
     * Returns true if within limit, false if rate-limited.
     */
    private boolean checkRateLimit(Map<String, Deque<Long>> map, String ip, int maxPerMinute) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = map.computeIfAbsent(ip, k -> new ArrayDeque<>());
        // Remove timestamps older than 60 seconds
        while (!deque.isEmpty() && now - deque.peekFirst() > 60_000) {
            deque.pollFirst();
        }
        if (deque.size() >= maxPerMinute) return false;
        deque.addLast(now);
        return true;
    }

    private void recordFailedJoin(String ip, WebSocket conn) {
        if (!checkRateLimit(joinFailAttempts, ip, MAX_JOIN_FAIL_PER_MINUTE)) {
            logger.warn("Too many failed JOIN_ROOM attempts from IP: {}", ip);
            conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Too many failed attempts. Please wait.\"}");
        } else {
            conn.send("{\"type\":\"ROOM_ERROR\",\"message\":\"Room not found. Check the code and try again.\"}");
        }
    }
}
