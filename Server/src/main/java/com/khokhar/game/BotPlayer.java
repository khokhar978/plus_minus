package com.khokhar.game;

/**
 * A bot-controlled player. Has no WebSocket connection — the server
 * drives all their actions automatically via BotStrategy / AutoPlayHelper.
 */
public class BotPlayer extends Player {

    public BotPlayer(String name) {
        super(name);
    }

    @Override
    public boolean isBot() {
        return true;
    }
}
