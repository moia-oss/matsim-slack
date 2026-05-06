package org.matsim.contrib.slack.notifier.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class IterationTimeNotifierTest {

    private IterationTimeNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new IterationTimeNotifier(null);

        IterationTimeNotifierParams config = new IterationTimeNotifierParams();
        config.setReportInterval(5);
        config.setWarnThresholdSeconds(1);
        notifier.configure(config);
    }

    @Test
    void testType() {
        assertEquals("iterationTime", notifier.getType());
    }

    @Test
    void testTracksDuration() {
        notifier.notifyIterationStartsInternal();
        notifier.notifyIterationEndsInternal(1);

        assertTrue(notifier.getLastIterationDuration() >= 0);
        assertTrue(notifier.getTotalDuration() >= 0);
    }

    @Test
    void testNoMessageBeforeReportInterval() {
        notifier.notifyIterationStartsInternal();
        notifier.notifyIterationEndsInternal(1);

        assertFalse(notifier.hasMessage());
    }

    @Test
    void testMessageAtReportInterval() {
        for (int i = 1; i <= 5; i++) {
            notifier.notifyIterationStartsInternal();
            notifier.notifyIterationEndsInternal(i);
        }

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertTrue(msg.get().getText().contains("Iteration Timing Report"));
        assertEquals(":stopwatch:", msg.get().getEmoji());
    }

    @Test
    void testAverageDurationStartsAtZero() {
        assertEquals(0, notifier.getAverageDuration());
    }

    @Test
    void testAverageDurationAfterIterations() {
        notifier.notifyIterationStartsInternal();
        notifier.notifyIterationEndsInternal(1);

        assertTrue(notifier.getAverageDuration() >= 0);
    }
}
