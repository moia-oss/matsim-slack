package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

/**
 * Tracks wall-clock iteration duration and posts timing reports. Warns when iterations exceed a threshold.
 *
 * @author nkuehnel / MOIA
 */
public class IterationTimeNotifier extends ControlerEventNotifier {

    private int reportInterval = 5;
    private long warnThresholdSeconds = 3600;

    private long iterationStartTime = 0;
    private long lastIterationDuration = 0;
    private long totalDuration = 0;
    private int iterationCount = 0;

    public IterationTimeNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() {
        return "iterationTime";
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        IterationTimeNotifierParams params = (IterationTimeNotifierParams) config;
        this.reportInterval = params.getReportInterval();
        this.warnThresholdSeconds = params.getWarnThresholdSeconds();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        super.notifyIterationStarts(event);
        notifyIterationStartsInternal();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        notifyIterationEndsInternal(event.getIteration());
    }

    void notifyIterationStartsInternal() {
        iterationStartTime = System.currentTimeMillis();
    }

    void notifyIterationEndsInternal(int iteration) {
        long iterationEndTime = System.currentTimeMillis();
        lastIterationDuration = iterationEndTime - iterationStartTime;
        totalDuration += lastIterationDuration;
        iterationCount++;

        long durationSeconds = lastIterationDuration / 1000;

        if (durationSeconds > warnThresholdSeconds) {
            String warningMessage = String.format(
                    "*Slow Iteration Detected*\n" +
                    "> Iteration %d took `%s` (threshold: %s)",
                    iteration,
                    formatDuration(durationSeconds),
                    formatDuration(warnThresholdSeconds)
            );

            queueMessage(SlackMessage.builder()
                    .text(warningMessage)
                    .emoji(config.getEmoji() != null ? config.getEmoji() : ":warning:")
                    .priority(SlackMessage.Priority.HIGH)
                    .build());
        }

        if (iteration % reportInterval == 0 && iteration > 0) {
            long avgDuration = totalDuration / iterationCount;

            String reportMessage = String.format(
                    "*Iteration Timing Report*\n" +
                    "> Iteration %d: `%s`\n" +
                    "> Average: `%s`\n" +
                    "> Total time: `%s`",
                    iteration,
                    formatDuration(durationSeconds),
                    formatDuration(avgDuration / 1000),
                    formatDuration(totalDuration / 1000)
            );

            queueMessage(SlackMessage.builder()
                    .text(reportMessage)
                    .emoji(config.getEmoji() != null ? config.getEmoji() : ":stopwatch:")
                    .priority(SlackMessage.Priority.NORMAL)
                    .build());
        }
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return String.format("%ds", seconds);
        } else if (seconds < 3600) {
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        } else {
            return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
        }
    }

    public long getLastIterationDuration() {
        return lastIterationDuration;
    }

    public long getAverageDuration() {
        return iterationCount > 0 ? totalDuration / iterationCount : 0;
    }

    public long getTotalDuration() {
        return totalDuration;
    }
}
