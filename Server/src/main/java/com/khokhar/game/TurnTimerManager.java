package com.khokhar.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Manages the per-turn countdown timer for a single Room.
 * Each Room owns one TurnTimerManager instance.
 *
 * Uses a generation counter so stale callbacks (from cancelled timers)
 * are silently ignored — this is the same pattern that was in GameServer.
 */
public class TurnTimerManager {

    private static final Logger logger = LoggerFactory.getLogger(TurnTimerManager.class);

    private Timer timer;
    private int generation = 0;

    /**
     * Cancels any running timer. Safe to call multiple times.
     */
    public synchronized void cancel() {
        generation++;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Starts a new turn timer.
     *
     * @param player      Name of the player whose turn it is (for logging).
     * @param timeoutMs   Milliseconds before the timer fires.
     * @param onTimeout   Called (under the room's lock) when the timer expires.
     *                    The caller is responsible for acquiring the room lock inside this runnable.
     */
    public synchronized void start(String player, int timeoutMs, Consumer<String> onTimeout) {
        cancel(); // reset generation and cancel old timer
        final int gen = generation;

        timer = new Timer("turn-timer-" + player, true); // daemon thread
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (TurnTimerManager.this) {
                    if (gen != generation) return; // stale — a new timer was started
                }
                logger.info("Turn timeout fired for player: {}", player);
                onTimeout.accept(player);
            }
        }, timeoutMs);
    }

    /**
     * Returns true if a timer is currently running.
     */
    public synchronized boolean isRunning() {
        return timer != null;
    }
}
