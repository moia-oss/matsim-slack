package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.core.controler.events.IterationEndsEvent;

/**
 * Reports JVM heap memory usage at iteration boundaries and warns when usage exceeds a configured threshold.
 *
 * @author nkuehnel / MOIA
 */
public class MemoryObserverNotifier extends ControlerEventNotifier {

    private int reportInterval = 5;
    private int warnThresholdPercent = 80;

    public MemoryObserverNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() {
        return "memoryObserver";
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        MemoryObserverNotifierParams params = (MemoryObserverNotifierParams) config;
        this.reportInterval = params.getReportInterval();
        this.warnThresholdPercent = params.getWarnThresholdPercent();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        checkMemory(event.getIteration());
    }

    void checkMemory(int iteration) {
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMB = runtime.totalMemory() / (1024 * 1024);
        long maxMB = runtime.maxMemory() / (1024 * 1024);
        double usedPercent = (double) usedMB / maxMB * 100.0;

        if (usedPercent > warnThresholdPercent) {
            queueMessage(SlackMessage.builder()
                    .text(String.format("*Memory Warning (Iteration %d)*\n" +
                                    "> Used: `%d MB` (%.1f%% of max)\n" +
                                    "> Total heap: `%d MB`\n" +
                                    "> Max memory: `%d MB`\n" +
                                    "> Threshold: `%d%%`",
                            iteration, usedMB, usedPercent, totalMB, maxMB, warnThresholdPercent))
                    .emoji(":warning:")
                    .priority(SlackMessage.Priority.HIGH)
                    .build());
            return;
        }

        if (reportInterval > 0 && iteration > 0 && iteration % reportInterval == 0) {
            queueMessage(SlackMessage.builder()
                    .text(String.format("*Memory Report (Iteration %d)*\n" +
                                    "> Used heap: `%d MB` (%.1f%%)\n" +
                                    "> Total heap: `%d MB`\n" +
                                    "> Max memory: `%d MB`",
                            iteration, usedMB, usedPercent, totalMB, maxMB))
                    .emoji(config.getEmoji() != null ? config.getEmoji() : ":bar_chart:")
                    .priority(SlackMessage.Priority.NORMAL)
                    .build());
        }
    }
}
