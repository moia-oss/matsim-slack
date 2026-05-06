package org.matsim.contrib.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.chat.ChatUpdateResponse;
import com.slack.api.model.block.LayoutBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Low-level wrapper around the Slack API for posting and updating messages.
 *
 * @author nkuehnel / MOIA
 */
public class SlackMessageSender {

    private static final Logger LOG = LogManager.getLogger(SlackMessageSender.class);

    private final MethodsClient methodsClient;
    private volatile String channel;
    private final String userName;

    SlackMessageSender(MethodsClient methodsClient, String channel, String userName) {
        this.methodsClient = methodsClient;
        this.channel = channel;
        this.userName = userName;
    }

    public void setChannelId(String channelId) {
        this.channel = channelId;
    }

    public ChatPostMessageResponse postMessage(String text, List<LayoutBlock> blocks, String iconEmoji, String threadTs) {
        ChatPostMessageRequest.ChatPostMessageRequestBuilder builder = ChatPostMessageRequest.builder()
                .blocks(blocks)
                .iconEmoji(iconEmoji)
                .channel(channel)
                .username(userName)
                .text(text);
        if (threadTs != null) {
            builder.threadTs(threadTs);
        }
        ChatPostMessageRequest request = builder.build();
        try {
            ChatPostMessageResponse response = methodsClient.chatPostMessage(request);
            if (!response.isOk()) {
                LOG.error("Slack API error: {} (message: {})", response.getError(), text);
            }
            return response;
        } catch (IOException | SlackApiException e) {
            LOG.error("Failed to send Slack message: {}", text, e);
            return null;
        }
    }

    public ChatUpdateResponse updateMessage(String ts, List<LayoutBlock> blocks, String fallbackText) {
        try {
            ChatUpdateResponse response = methodsClient.chatUpdate(r -> r
                    .channel(channel)
                    .ts(ts)
                    .blocks(blocks)
                    .text(fallbackText)
            );
            if (!response.isOk()) {
                LOG.error("Slack chatUpdate error: {} (ts: {})", response.getError(), ts);
            }
            return response;
        } catch (IOException | SlackApiException e) {
            LOG.error("Failed to update Slack message: {}", ts, e);
            return null;
        }
    }
}
