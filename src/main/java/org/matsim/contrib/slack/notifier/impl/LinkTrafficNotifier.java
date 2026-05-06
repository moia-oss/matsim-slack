package org.matsim.contrib.slack.notifier.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.EventHandlerNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitors link leave events and sends notifications when the vehicle count on a link crosses a threshold.
 *
 * @author nkuehnel / MOIA
 */
public class LinkTrafficNotifier extends EventHandlerNotifier implements LinkLeaveEventHandler {

    private final Map<Id<Link>, Integer> linkCounts = new HashMap<>();
    private Id<Link> monitoredLink = null;
    private int threshold = 100;
    private String messageTemplate = "{count} vehicles left link {linkId}";
    private int totalCount = 0;

    public LinkTrafficNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() {
        return "linkTraffic";
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        LinkTrafficNotifierParams params = (LinkTrafficNotifierParams) config;
        String linkIdStr = params.getLinkId();
        if (linkIdStr != null && !linkIdStr.isEmpty()) {
            this.monitoredLink = Id.createLinkId(linkIdStr);
        }
        this.threshold = params.getThreshold();
        this.messageTemplate = params.getMessageTemplate();
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (monitoredLink != null && !event.getLinkId().equals(monitoredLink)) {
            return;
        }

        Id<Link> linkId = event.getLinkId();
        int count = linkCounts.merge(linkId, 1, Integer::sum);
        totalCount++;

        if (count % threshold == 0) {
            String message = formatMessage(linkId, count);
            queueMessage(SlackMessage.builder()
                    .text(message)
                    .emoji(config.getEmoji())
                    .priority(SlackMessage.Priority.NORMAL)
                    .build());
        }
    }

    private String formatMessage(Id<Link> linkId, int count) {
        return messageTemplate
                .replace("{count}", String.valueOf(count))
                .replace("{linkId}", linkId.toString())
                .replace("{totalCount}", String.valueOf(totalCount));
    }

    @Override
    public void reset(int iteration) {
        super.reset(iteration);
        linkCounts.clear();
        totalCount = 0;
    }

    public Map<Id<Link>, Integer> getLinkCounts() {
        return new HashMap<>(linkCounts);
    }

    public int getTotalCount() {
        return totalCount;
    }
}
