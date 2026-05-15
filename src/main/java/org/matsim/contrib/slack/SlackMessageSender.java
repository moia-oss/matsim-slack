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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Low-level wrapper around the Slack API for posting and updating messages.
 * All API calls are dispatched to a background thread so they never block
 * the MATSim simulation thread. Implements exponential backoff on repeated
 * failures to avoid futile retries against a broken connection.
 *
 * @author nkuehnel / MOIA
 */
public class SlackMessageSender {

    private static final Logger LOG = LogManager.getLogger(SlackMessageSender.class);
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long INITIAL_BACKOFF_MS = 60_000;
    private static final long MAX_BACKOFF_MS = 600_000;

    private final MethodsClient methodsClient;
    private volatile String channel;
    private final String userName;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "matsim-slack-sender");
        t.setDaemon(true);
        return t;
    });

    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong backoffUntil = new AtomicLong(0);

    SlackMessageSender(MethodsClient methodsClient, String channel, String userName) {
        this.methodsClient = methodsClient;
        this.channel = channel;
        this.userName = userName;
    }

    public void setChannelId(String channelId) {
        this.channel = channelId;
    }

    private boolean isInBackoff() {
        long until = backoffUntil.get();
        if (until == 0) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            LOG.info("Slack backoff period ended, will retry sending messages.");
            backoffUntil.set(0);
            consecutiveFailures.set(0);
            return false;
        }
        return true;
    }

    private void recordSuccess() {
        consecutiveFailures.set(0);
        backoffUntil.set(0);
    }

    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            long backoff = Math.min(INITIAL_BACKOFF_MS * (1L << (failures - MAX_CONSECUTIVE_FAILURES)), MAX_BACKOFF_MS);
            backoffUntil.set(System.currentTimeMillis() + backoff);
            LOG.warn("Slack: {} consecutive failures, backing off for {}s.", failures, backoff / 1000);
        }
    }

    public ChatPostMessageResponse postMessage(String text, List<LayoutBlock> blocks, String iconEmoji, String threadTs) {
        if (isInBackoff()) {
            return null;
        }
        Future<ChatPostMessageResponse> future = executor.submit(() -> doPostMessage(text, blocks, iconEmoji, threadTs));
        try {
            return future.get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Slack postMessage timed out after {}s, continuing simulation.", SEND_TIMEOUT_SECONDS);
            future.cancel(true);
            recordFailure();
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            LOG.error("Slack postMessage failed: {}", text, e.getCause());
            recordFailure();
            return null;
        }
    }

    private ChatPostMessageResponse doPostMessage(String text, List<LayoutBlock> blocks, String iconEmoji, String threadTs) {
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
                recordFailure();
            } else {
                recordSuccess();
            }
            return response;
        } catch (SlackApiException e) {
            if (e.getResponse().code() == 429) {
                handleRateLimit(e);
            } else {
                LOG.error("Slack API exception (HTTP {}): {}", e.getResponse().code(), text, e);
                recordFailure();
            }
            return null;
        } catch (IOException e) {
            LOG.error("Failed to send Slack message: {}", text, e);
            recordFailure();
            return null;
        }
    }

    public ChatUpdateResponse updateMessage(String ts, List<LayoutBlock> blocks, String fallbackText) {
        if (isInBackoff()) {
            return null;
        }
        executor.submit(() -> doUpdateMessage(ts, blocks, fallbackText));
        return null;
    }

    private ChatUpdateResponse doUpdateMessage(String ts, List<LayoutBlock> blocks, String fallbackText) {
        try {
            ChatUpdateResponse response = methodsClient.chatUpdate(r -> r
                    .channel(channel)
                    .ts(ts)
                    .blocks(blocks)
                    .text(fallbackText)
            );
            if (!response.isOk()) {
                LOG.error("Slack chatUpdate error: {} (ts: {})", response.getError(), ts);
                recordFailure();
            } else {
                recordSuccess();
            }
            return response;
        } catch (SlackApiException e) {
            if (e.getResponse().code() == 429) {
                handleRateLimit(e);
            } else {
                LOG.error("Slack chatUpdate exception (HTTP {}): {}", e.getResponse().code(), ts, e);
                recordFailure();
            }
            return null;
        } catch (IOException e) {
            LOG.error("Failed to update Slack message: {}", ts, e);
            recordFailure();
            return null;
        }
    }

    private void handleRateLimit(SlackApiException e) {
        String retryAfter = e.getResponse().header("Retry-After");
        long waitMs;
        if (retryAfter != null) {
            waitMs = Long.parseLong(retryAfter) * 1000;
        } else {
            waitMs = 30_000;
        }
        LOG.warn("Slack rate limited (429). Backing off for {}s.", waitMs / 1000);
        backoffUntil.set(System.currentTimeMillis() + waitMs);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
