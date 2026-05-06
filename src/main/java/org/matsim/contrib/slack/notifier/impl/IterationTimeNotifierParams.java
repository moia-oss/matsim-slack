package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class IterationTimeNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "iterationTime";

    @Parameter
    @Comment("Report timing every N iterations")
    private int reportInterval = 5;

    @Parameter
    @Comment("Warn if an iteration takes longer than this many seconds")
    private int warnThresholdSeconds = 3600;

    public IterationTimeNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":stopwatch:");
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    public int getWarnThresholdSeconds() {
        return warnThresholdSeconds;
    }

    public void setWarnThresholdSeconds(int warnThresholdSeconds) {
        this.warnThresholdSeconds = warnThresholdSeconds;
    }
}