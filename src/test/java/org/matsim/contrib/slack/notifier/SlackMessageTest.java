package org.matsim.contrib.slack.notifier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class SlackMessageTest {

    @Test
    void testBuilderBasic() {
        SlackMessage msg = SlackMessage.builder()
                .text("Hello")
                .build();

        assertEquals("Hello", msg.getText());
        assertEquals(":satellite_antenna:", msg.getEmoji());
        assertEquals(SlackMessage.Priority.NORMAL, msg.getPriority());
        assertTrue(msg.getBlocks().isEmpty());
    }

    @Test
    void testBuilderAllFields() {
        SlackMessage msg = SlackMessage.builder()
                .text("Alert!")
                .emoji(":warning:")
                .priority(SlackMessage.Priority.CRITICAL)
                .blocks(List.of())
                .build();

        assertEquals("Alert!", msg.getText());
        assertEquals(":warning:", msg.getEmoji());
        assertEquals(SlackMessage.Priority.CRITICAL, msg.getPriority());
    }

    @Test
    void testBuilderFailsWithoutText() {
        assertThrows(IllegalStateException.class, () ->
                SlackMessage.builder().build());
    }

    @Test
    void testBuilderFailsWithEmptyText() {
        assertThrows(IllegalStateException.class, () ->
                SlackMessage.builder().text("").build());
    }

    @Test
    void testNullBlocksBecomesEmptyList() {
        SlackMessage msg = SlackMessage.builder()
                .text("test")
                .blocks(null)
                .build();

        assertNotNull(msg.getBlocks());
        assertTrue(msg.getBlocks().isEmpty());
    }

    @Test
    void testPriorityOrdering() {
        SlackMessage.Priority[] priorities = SlackMessage.Priority.values();
        assertEquals(4, priorities.length);
        assertEquals(SlackMessage.Priority.LOW, priorities[0]);
        assertEquals(SlackMessage.Priority.NORMAL, priorities[1]);
        assertEquals(SlackMessage.Priority.HIGH, priorities[2]);
        assertEquals(SlackMessage.Priority.CRITICAL, priorities[3]);
    }
}
