package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class StuckAgentNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "stuckAgent";

    public StuckAgentNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":warning:");
    }
}