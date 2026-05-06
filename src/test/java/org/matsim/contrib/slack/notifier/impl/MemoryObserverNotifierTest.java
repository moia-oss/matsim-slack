package org.matsim.contrib.slack.notifier.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.slack.notifier.SlackMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class MemoryObserverNotifierTest {

    private MemoryObserverNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new MemoryObserverNotifier(null);

        MemoryObserverNotifierParams config = new MemoryObserverNotifierParams();
        config.setReportInterval(5);
        config.setWarnThresholdPercent(80);
        config.setEmoji(":bar_chart:");
        notifier.configure(config);
    }

    @Test
    void testType() {
        assertEquals("memoryObserver", notifier.getType());
    }

    @Test
    void testNoMessageAtNonIntervalIteration() {
        notifier.checkMemory(1);
        assertFalse(notifier.hasMessage());
    }

    @Test
    void testNoMessageAtIterationZero() {
        notifier.checkMemory(0);
        assertFalse(notifier.hasMessage());
    }

    @Test
    void testMessageAtReportInterval() {
        notifier.checkMemory(5);

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertTrue(msg.get().getText().contains("Memory Report"));
        assertTrue(msg.get().getText().contains("Used heap"));
        assertEquals(":bar_chart:", msg.get().getEmoji());
        assertEquals(SlackMessage.Priority.NORMAL, msg.get().getPriority());
    }

    @Test
    void testHighPriorityWhenThresholdExceeded() {
        MemoryObserverNotifierParams warningConfig = new MemoryObserverNotifierParams();
        warningConfig.setWarnThresholdPercent(0);
        notifier.configure(warningConfig);

        notifier.checkMemory(1);

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertTrue(msg.get().getText().contains("Memory Warning"));
        assertEquals(SlackMessage.Priority.HIGH, msg.get().getPriority());
        assertEquals(":warning:", msg.get().getEmoji());
    }

    @Test
    void testWarningTakesPrecedenceOverReport() {
        MemoryObserverNotifierParams warningConfig = new MemoryObserverNotifierParams();
        warningConfig.setReportInterval(5);
        warningConfig.setWarnThresholdPercent(0);
        notifier.configure(warningConfig);

        notifier.checkMemory(5);

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertTrue(msg.get().getText().contains("Memory Warning"));
    }

    @Test
    void testConfigurationParsing() {
        MemoryObserverNotifierParams config = new MemoryObserverNotifierParams();
        config.setReportInterval(10);
        config.setWarnThresholdPercent(90);

        MemoryObserverNotifier customNotifier = new MemoryObserverNotifier(null);
        customNotifier.configure(config);

        customNotifier.checkMemory(5);
        assertFalse(customNotifier.hasMessage());

        customNotifier.checkMemory(10);
        assertTrue(customNotifier.hasMessage());
    }
}