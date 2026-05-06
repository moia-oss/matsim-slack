package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class IterationProgressNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "iterationProgress";

    @Parameter
    @Comment("Post a progress message every N iterations")
    private int messageInterval = 5;

    public IterationProgressNotifierParams() {
        super(SET_NAME, true, SET_NAME);
    }

    public int getMessageInterval() {
        return messageInterval;
    }

    public void setMessageInterval(int messageInterval) {
        this.messageInterval = messageInterval;
    }
}