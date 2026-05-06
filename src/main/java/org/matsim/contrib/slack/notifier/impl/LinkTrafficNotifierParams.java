package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.slack.notifier.NotifierConfig;

/**
 * @author nkuehnel / MOIA
 */
public class LinkTrafficNotifierParams extends NotifierConfig {

    public static final String SET_NAME = "linkTraffic";

    @Parameter
    @Comment("Optional link ID to monitor. If empty, all links are monitored.")
    private String linkId = "";

    @Parameter
    @Comment("Number of vehicles leaving a link before a notification is sent")
    private int threshold = 100;

    @Parameter
    @Comment("Message template. Placeholders: {count}, {linkId}, {totalCount}")
    private String messageTemplate = "{count} vehicles left link {linkId}";

    public LinkTrafficNotifierParams() {
        super(SET_NAME, true, SET_NAME);
        setEmoji(":car:");
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }
}