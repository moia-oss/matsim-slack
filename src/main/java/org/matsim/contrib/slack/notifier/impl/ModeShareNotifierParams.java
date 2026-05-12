package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class ModeShareNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "modeShare";

    @Parameter
    @Comment("Report mode shares every N iterations")
    private int reportInterval = 10;

    public ModeShareNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":bar_chart:");
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }
}
