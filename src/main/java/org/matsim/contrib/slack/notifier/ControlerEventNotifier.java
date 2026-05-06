package org.matsim.contrib.slack.notifier;

import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ControllerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for notifiers that observe MATSim controller events (iteration starts/ends, shutdown).
 * Subclasses implement the appropriate listener interface and call {@link #queueMessage} to produce notifications.
 *
 * @author nkuehnel / MOIA
 */
public abstract class ControlerEventNotifier implements SlackNotifier, ControllerListener, IterationStartsListener, IterationEndsListener {

    protected final MatsimSlackClient slackClient;
    protected NotifierConfig config;
    private final AtomicReference<SlackMessage> pendingMessage = new AtomicReference<>();

    public ControlerEventNotifier(MatsimSlackClient slackClient) {
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
    public void notifyIterationStarts(IterationStartsEvent event) {
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
    }
}
