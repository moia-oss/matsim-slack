package org.matsim.contrib.slack.notifier;

import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.core.events.handler.EventHandler;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for notifiers that observe high-frequency mobsim events (link leave, person arrival, etc.).
 * Subclasses implement the appropriate event handler interface and call {@link #queueMessage} to produce notifications.
 * Messages are reset at each iteration boundary.
 *
 * @author nkuehnel / MOIA
 */
public abstract class EventHandlerNotifier implements SlackNotifier, EventHandler {

    protected final MatsimSlackClient slackClient;
    protected NotifierConfig config;
    private final AtomicReference<SlackMessage> pendingMessage = new AtomicReference<>();

    public EventHandlerNotifier(MatsimSlackClient slackClient) {
        this.slackClient = slackClient;
    }

    @Override
    public void configure(NotifierConfig config) {
        this.config = config;
    }

    @Override
    public Optional<SlackMessage> createMessage() {
        SlackMessage msg = pendingMessage.getAndSet(null);
        return Optional.ofNullable(msg);
    }

    @Override
    public boolean hasMessage() {
        return pendingMessage.get() != null;
    }

    protected void queueMessage(SlackMessage message) {
        this.pendingMessage.set(message);
    }

    protected void queueMessage(String text) {
        queueMessage(SlackMessage.builder()
                .text(text)
                .emoji(config.getEmoji())
                .build());
    }

    @Override
    public void reset(int iteration) {
        pendingMessage.set(null);
    }
}
