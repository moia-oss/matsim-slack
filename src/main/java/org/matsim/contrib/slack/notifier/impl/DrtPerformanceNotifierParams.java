package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class DrtPerformanceNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "drtPerformance";

    @Parameter
    @Comment("Report DRT performance every N iterations")
    private int reportInterval = 1;

    public DrtPerformanceNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":taxi:");
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }
}