package org.matsim.contrib.slack.notifier.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.contrib.slack.notifier.SlackMessage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class StuckAgentNotifierTest {

    private StuckAgentNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new StuckAgentNotifier(null);
        notifier.configure(new StuckAgentNotifierParams());
    }

    @Test
    void testType() {
        assertEquals("stuckAgent", notifier.getType());
    }

    @Test
    void testCountsStuckAgents() {
        notifier.handleEvent(new PersonStuckEvent(100.0, Id.createPersonId("p1"), Id.createLinkId("l1"), "car"));
        notifier.handleEvent(new PersonStuckEvent(200.0, Id.createPersonId("p2"), Id.createLinkId("l2"), "car"));

        assertEquals(2, notifier.getStuckAgentCount());
    }

    @Test
    void testNoMessageWhenNoStuckAgents() {
        notifier.reset(1);
        assertFalse(notifier.hasMessage());
    }

    @Test
    void testCriticalMessageOnReset() {
        notifier.handleEvent(new PersonStuckEvent(100.0, Id.createPersonId("p1"), Id.createLinkId("l1"), "car"));
        notifier.handleEvent(new PersonStuckEvent(200.0, Id.createPersonId("p2"), Id.createLinkId("l2"), "car"));

        notifier.reset(1);

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertEquals(SlackMessage.Priority.NORMAL, msg.get().getPriority());
        assertTrue(msg.get().getText().contains("2"));
    }

    @Test
    void testCountResetsAfterReset() {
        notifier.handleEvent(new PersonStuckEvent(100.0, Id.createPersonId("p1"), Id.createLinkId("l1"), "car"));
        notifier.reset(1);

        assertEquals(0, notifier.getStuckAgentCount());
    }
}