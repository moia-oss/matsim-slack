package org.matsim.contrib.slack.notifier.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class LinkTrafficNotifierTest {

    private LinkTrafficNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new LinkTrafficNotifier(null);

        LinkTrafficNotifierParams config = new LinkTrafficNotifierParams();
        config.setThreshold(10);
        config.setEmoji(":car:");
        notifier.configure(config);
    }

    @Test
    void testType() {
        assertEquals("linkTraffic", notifier.getType());
    }

    @Test
    void testNoMessageBeforeThreshold() {
        Id<Link> linkId = Id.createLinkId("link_1");

        for (int i = 0; i < 9; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i * 10.0, Id.createVehicleId("v" + i), linkId));
        }

        assertFalse(notifier.hasMessage());
        assertEquals(9, notifier.getTotalCount());
    }

    @Test
    void testMessageAtThreshold() {
        Id<Link> linkId = Id.createLinkId("link_1");

        for (int i = 0; i < 10; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i * 10.0, Id.createVehicleId("v" + i), linkId));
        }

        assertTrue(notifier.hasMessage());
        var msg = notifier.createMessage();
        assertTrue(msg.isPresent());
        assertTrue(msg.get().getText().contains("10"));
        assertTrue(msg.get().getText().contains("link_1"));
        assertEquals(":car:", msg.get().getEmoji());
    }

    @Test
    void testMonitorSpecificLink() {
        LinkTrafficNotifierParams config = new LinkTrafficNotifierParams();
        config.setThreshold(5);
        config.setLinkId("monitored_link");
        config.setEmoji(":car:");
        notifier.configure(config);

        Id<Link> monitoredLink = Id.createLinkId("monitored_link");
        Id<Link> otherLink = Id.createLinkId("other_link");

        for (int i = 0; i < 20; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i, Id.createVehicleId("v" + i), otherLink));
        }
        assertFalse(notifier.hasMessage());

        for (int i = 0; i < 5; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i, Id.createVehicleId("v" + i), monitoredLink));
        }
        assertTrue(notifier.hasMessage());
    }

    @Test
    void testReset() {
        Id<Link> linkId = Id.createLinkId("link_1");

        for (int i = 0; i < 10; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i, Id.createVehicleId("v" + i), linkId));
        }
        assertTrue(notifier.hasMessage());

        notifier.reset(1);

        assertFalse(notifier.hasMessage());
        assertEquals(0, notifier.getTotalCount());
        assertTrue(notifier.getLinkCounts().isEmpty());
    }

    @Test
    void testMessageConsumedAfterCreate() {
        Id<Link> linkId = Id.createLinkId("link_1");

        for (int i = 0; i < 10; i++) {
            notifier.handleEvent(new LinkLeaveEvent(i, Id.createVehicleId("v" + i), linkId));
        }

        assertTrue(notifier.hasMessage());
        notifier.createMessage();
        assertFalse(notifier.hasMessage());
        assertTrue(notifier.createMessage().isEmpty());
    }
}