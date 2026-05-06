package org.matsim.contrib.slack.notifier;

import java.util.Optional;

/**
 * Core interface for all Slack notifiers. Implementations observe MATSim events and produce
 * {@link SlackMessage}s that are collected and posted by the {@link org.matsim.contrib.slack.NotifierManager}.
 *
 * @author nkuehnel / MOIA
 * @see org.matsim.contrib.slack.notifier.impl for built-in implementations
 */
public interface SlackNotifier {

    /** Unique type identifier matching the corresponding {@link NotifierConfig} type. */
    String getType();

    /** Configures this notifier from a typed parameter set. */
    void configure(NotifierConfig config);

    /** Creates and returns a pending message, clearing the internal state. */
    Optional<SlackMessage> createMessage();

    /** Returns true if a message is queued and ready to send. */
    boolean hasMessage();
}
