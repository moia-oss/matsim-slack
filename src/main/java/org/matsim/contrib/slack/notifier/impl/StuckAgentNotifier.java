package org.matsim.contrib.slack.notifier.impl;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.EventHandlerNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;

/**
 * Counts stuck agents during a mobsim iteration and reports the total at reset (iteration boundary).
 *
 * @author nkuehnel / MOIA
 */
public class StuckAgentNotifier extends EventHandlerNotifier implements PersonStuckEventHandler {

    public static final String TYPE = "stuckAgent";
    private int stuckAgentCount = 0;

    public StuckAgentNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        stuckAgentCount++;
    }

    @Override
    public void reset(int iteration) {
        super.reset(iteration);

        if (stuckAgentCount > 0) {
            String message = "> Total stuck agents in iteration " + iteration + ": " + stuckAgentCount;
            queueMessage(SlackMessage.builder()
                    .text(message)
                    .emoji(config.getEmoji() != null ? config.getEmoji() : ":warning:")
                    .priority(SlackMessage.Priority.NORMAL)
                    .build());
        }

        stuckAgentCount = 0;
    }

    public int getStuckAgentCount() {
        return stuckAgentCount;
    }
}
