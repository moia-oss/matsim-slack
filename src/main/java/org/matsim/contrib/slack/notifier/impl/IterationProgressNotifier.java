package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.core.controler.events.IterationStartsEvent;

/**
 * Posts a simple progress message ("Iteration N started") at configurable intervals.
 *
 * @author nkuehnel / MOIA
 */
public class IterationProgressNotifier extends ControlerEventNotifier {

    private int messageInterval = 5;

    public IterationProgressNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() {
        return "iterationProgress";
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        IterationProgressNotifierParams params = (IterationProgressNotifierParams) config;
        this.messageInterval = params.getMessageInterval();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        super.notifyIterationStarts(event);
        int iteration = event.getIteration();

        if (iteration > 0 && iteration % messageInterval == 0) {
            queueMessage(SlackMessage.builder()
                    .text(String.format("Iteration %d started", iteration))
                    .emoji(config.getEmoji())
                    .priority(SlackMessage.Priority.LOW)
                    .build());
        }
    }
}
