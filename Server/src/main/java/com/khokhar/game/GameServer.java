package com.khokhar.game;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * WebSocket server entry-point.
 * Responsibility: WebSocket lifecycle events only.
 * All game and room logic is delegated to MessageRouter and RoomManager.
 */
public class GameServer extends WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    private final RoomManager roomManager;
    private final MessageRouter router;

    public GameServer(int port) {
        super(new InetSocketAddress(port));
        this.roomManager = new RoomManager();
        this.router = new MessageRouter(roomManager);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        logger.info("New connection from: {}", conn.getRemoteSocketAddress());

        // Close connections that never identify themselves within 10 seconds
        new Timer("join-timeout", true).schedule(new TimerTask() {
            @Override
            public void run() {
                if (roomManager.getRoomForConnection(conn) == null && conn.isOpen()) {
                    logger.warn("Closing idle connection from {} — no room joined within 10s",
                            conn.getRemoteSocketAddress());
                    conn.close();
                }
            }
        }, 10_000);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.debug("Message from {}: {}", conn.getRemoteSocketAddress(), message);
        router.route(conn, message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("Connection closed from {}: {}", conn.getRemoteSocketAddress(), reason);
        roomManager.handleDisconnect(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error("WebSocket error from {}: ", conn != null ? conn.getRemoteSocketAddress() : "unknown", ex);
    }

    @Override
    public void onStart() {
        logger.info("WebSocket Server started successfully on port: {}", getPort());
    }

    public static void main(String[] args) {
        int port = 8887;
        GameServer server = new GameServer(port);
        server.start();
        logger.info("Plus Minus Game Server running on port {}", port);
    }
}
