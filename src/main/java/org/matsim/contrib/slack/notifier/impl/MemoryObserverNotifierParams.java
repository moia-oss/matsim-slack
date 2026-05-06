package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class MemoryObserverNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "memoryObserver";

    @Parameter
    @Comment("Report memory usage every N iterations")
    private int reportInterval = 5;

    @Parameter
    @Comment("Warn if used memory exceeds this percentage of max memory")
    private int warnThresholdPercent = 80;

    public MemoryObserverNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":bar_chart:");
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    public int getWarnThresholdPercent() {
        return warnThresholdPercent;
    }

    public void setWarnThresholdPercent(int warnThresholdPercent) {
        this.warnThresholdPercent = warnThresholdPercent;
    }
}