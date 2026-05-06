package org.matsim.contrib.slack;

import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.contrib.slack.notifier.SlackNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class NotifierManagerTest {

    private RecordingMessageSender recordingSender;
    private NotifierManager manager;

    @BeforeEach
    void setUp() {
        recordingSender = new RecordingMessageSender();
        manager = new NotifierManager(recordingSender, 0);
        manager.setThreadTs("thread-123");
    }

    @Test
    void testNoNotifications() {
        manager.checkAndSendNotifications(true);
        assertTrue(recordingSender.sentMessages.isEmpty());
    }

    @Test
    void testCriticalMessageSentImmediately() {
        manager.addNotifier(new FakeNotifier(SlackMessage.builder()
                .text("CRITICAL!")
                .priority(SlackMessage.Priority.CRITICAL)
                .build()));

        manager.checkAndSendNotifications(true);

        assertEquals(1, recordingSender.sentMessages.size());
        assertTrue(recordingSender.sentMessages.get(0).contains("CRITICAL!"));
    }

    @Test
    void testNormalMessageSent() {
        manager.addNotifier(new FakeNotifier(SlackMessage.builder()
                .text("Normal update")
                .priority(SlackMessage.Priority.NORMAL)
                .build()));

        manager.checkAndSendNotifications(true);

        assertEquals(1, recordingSender.sentMessages.size());
        assertTrue(recordingSender.sentMessages.get(0).contains("Normal update"));
    }

    @Test
    void testThrottling() {
        RecordingMessageSender throttledSender = new RecordingMessageSender();
        NotifierManager throttledManager = new NotifierManager(throttledSender, 60_000);
        throttledManager.setThreadTs("t1");

        throttledManager.addNotifier(new FakeNotifier(SlackMessage.builder()
                .text("msg1")
                .priority(SlackMessage.Priority.NORMAL)
                .build()));

        // First call sends (no prior message time)
        throttledManager.checkAndSendNotifications(false);
        assertEquals(1, throttledSender.sentMessages.size());

        // Second call within throttle window should not send
        throttledManager.addNotifier(new FakeNotifier(SlackMessage.builder()
                .text("msg2")
                .priority(SlackMessage.Priority.NORMAL)
                .build()));
        throttledManager.checkAndSendNotifications(false);
        assertEquals(1, throttledSender.sentMessages.size());

        // But flush=true bypasses throttle
        throttledManager.checkAndSendNotifications(true);
        assertEquals(2, throttledSender.sentMessages.size());
    }

    @Test
    void testRemoveNotifier() {
        FakeNotifier notifier = new FakeNotifier(SlackMessage.builder()
                .text("x")
                .build());
        manager.addNotifier(notifier);
        manager.removeNotifier(notifier);
        manager.checkAndSendNotifications(true);

        assertTrue(recordingSender.sentMessages.isEmpty());
    }

    @Test
    void testNotifierWithNoMessage() {
        manager.addNotifier(new FakeNotifier(null));
        manager.checkAndSendNotifications(true);
        assertTrue(recordingSender.sentMessages.isEmpty());
    }

    // Test doubles that avoid Mockito

    static class RecordingMessageSender extends SlackMessageSender {
        final List<String> sentMessages = new ArrayList<>();

        RecordingMessageSender() {
            super(null, "test-channel", "test-user");
        }

        @Override
        public ChatPostMessageResponse postMessage(String text, List<LayoutBlock> blocks, String iconEmoji, String threadTs) {
            sentMessages.add(text);
            return null;
        }
    }

    static class FakeNotifier implements SlackNotifier {
        private SlackMessage message;

        FakeNotifier(SlackMessage message) {
            this.message = message;
        }

        @Override
        public String getType() {
            return "fake";
        }

        @Override
        public void configure(NotifierConfig config) {}

        @Override
        public Optional<SlackMessage> createMessage() {
            SlackMessage msg = message;
            message = null;
            return Optional.ofNullable(msg);
        }

        @Override
        public boolean hasMessage() {
            return message != null;
        }
    }
}
