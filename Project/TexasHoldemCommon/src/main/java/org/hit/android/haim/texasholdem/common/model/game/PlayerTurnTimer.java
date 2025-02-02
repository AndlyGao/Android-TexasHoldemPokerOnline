package org.hit.android.haim.texasholdem.common.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hit.android.haim.texasholdem.common.model.bean.game.GameSettings;
import org.hit.android.haim.texasholdem.common.util.CustomThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A timer like implementation, which is used for network games, to start a counter for
 * each turn, such that when a player does not finish his turn within {@link GameSettings#getTurnTime()} minute,
 * we force him to fold, and move the turn forward.
 * @author Haim Adrian
 * @since 22-Jun-21
 */
@NoArgsConstructor // For Jackson
@ToString(onlyExplicitlyIncluded = true)
public class PlayerTurnTimer {
    /**
     * Keep a reference to {@link GameEngine}, so we can get and modify the current player, and
     * manage turn timeouts.
     */
    @JsonIgnore
    private PlayerTurnTimerListener listener;

    /**
     * How much time to wait before raising a timeout event.
     */
    @ToString.Include
    private long timeoutMillis;

    /**
     * Keep the time (since epoch) that a player turn has started at, to make sure we
     * do not let players to take too much time and block a game.<br/>
     * Each turn can take up to {@link GameSettings#getTurnTime()} minute. In case player
     * has not played within that time, he is forced to fold.<br/>
     * Yet, when game mode is versus the AI, and not network, there is no timeout.
     */
    @ToString.Include
    @JsonProperty
    private final AtomicLong turnStartedAt = new AtomicLong();

    /**
     * In case of network game, we schedule a task to check if player turn took too much time, so
     * we can perform a turn timeout, and move the turn to the next player.<br/>
     * Each player on network got {@link GameSettings#getTurnTime()} minute to play, and when he's not
     * finishing the turn within that time frame, we force him to fold.<br/>
     * In case of AI game, this data member refers to null.
     */
    @JsonIgnore
    private ScheduledExecutorService turnTimeCounter;

    /**
     * Constructs a new {@link PlayerTurnTimer}
     * @param listener Listener to be notified about timeout
     * @param timeoutMillis How much time to wait before raising a timeout event.
     */
    public PlayerTurnTimer(PlayerTurnTimerListener listener, long timeoutMillis) {
        this.listener = listener;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Call this method whenever a turn is started.<br/>
     * This will start counting from now, for the specified timeout settings.
     */
    public void startOrReset() {
        turnStartedAt.set(System.currentTimeMillis());

        if (turnTimeCounter == null) {
            turnTimeCounter = Executors.newSingleThreadScheduledExecutor(new CustomThreadFactory(PlayerTurnTimer.class.getSimpleName()));
            turnTimeCounter.scheduleAtFixedRate(() -> {
                    if (System.currentTimeMillis() - turnStartedAt.get() > timeoutMillis) {
                        listener.turnTimeoutOccurred();
                        turnStartedAt.set(System.currentTimeMillis());
                    }
                },
                3, 3, TimeUnit.SECONDS);
        }
    }

    /**
     * Call this method to free resources. (Stop the timer)<br/>
     * Note that after calling this method you can still use this reference and call {@link #startOrReset()}.
     */
    public void stop() {
        if (turnTimeCounter != null) {
            turnTimeCounter.shutdownNow();
            turnTimeCounter = null;
        }
    }

    /**
     * @return The time that player started playing at
     */
    @JsonIgnore
    public long getTurnStartTime() {
        return turnStartedAt.get();
    }

    /**
     * Provides a mechanism to listen to turn timeout events.<br/>
     *
     */
    @FunctionalInterface
    public interface PlayerTurnTimerListener {
        /**
         * When a previously started timer passes the amount of time that this timer was configured
         * to count, this event will be raised, letting a listener to respond to timeouts.
         */
        void turnTimeoutOccurred();
    }
}

