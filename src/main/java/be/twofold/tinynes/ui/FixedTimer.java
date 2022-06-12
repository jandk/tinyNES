package be.twofold.tinynes.ui;

import javafx.animation.*;

import java.util.function.*;

/**
 * A timer that updates the UI at 60Hz.
 */
final class FixedTimer extends AnimationTimer {
    private static final long NanosPerFrame = 1_000_000_000 / 60;

    private final Consumer<Long> consumer;
    private long lastUpdateTime = 0;
    private long frameTime = 0;

    FixedTimer(Consumer<Long> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(long now) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = now;
            return;
        }

        frameTime += now - lastUpdateTime;
        if (frameTime >= NanosPerFrame) {
            frameTime %= NanosPerFrame;
            consumer.accept(now);
        }
        lastUpdateTime = now;
    }
}
