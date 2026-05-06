package org.matsim.contrib.slack;

import com.slack.api.model.block.LayoutBlock;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.contrib.slack.notifier.SlackNotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.slack.api.model.block.Blocks.divider;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

/**
 * Collects messages from registered {@link SlackNotifier} instances and posts them to Slack
 * with throttling and batching. Critical-priority messages bypass throttling; normal messages
 * are batched and sent at most once per throttle interval or at iteration boundaries.
 *
 * @author nkuehnel / MOIA
 */
public class NotifierManager {

    private final List<SlackNotifier> notifiers = new CopyOnWriteArrayList<>();
    private final SlackMessageSender messageSender;
    private final long throttleMs;

    private volatile String threadTs;
    private long lastMessageTime = 0;
    private final List<SlackMessage> pendingBatch = new ArrayList<>();

    public NotifierManager(SlackMessageSender messageSender, long throttleMs) {
        this.messageSender = messageSender;
        this.throttleMs = throttleMs;
    }

    public void setThreadTs(String threadTs) {
        this.threadTs = threadTs;
    }

    public void addNotifier(SlackNotifier notifier) {
        notifiers.add(notifier);
    }

    public void removeNotifier(SlackNotifier notifier) {
        notifiers.remove(notifier);
    }

    public void checkAndSendNotifications(boolean flush) {
        long currentTime = System.currentTimeMillis();

        for (SlackNotifier notifier : notifiers) {
            if (!notifier.hasMessage()) {
                continue;
            }

            Optional<SlackMessage> messageOpt = notifier.createMessage();
            if (messageOpt.isEmpty()) {
                continue;
            }

            SlackMessage message = messageOpt.get();

            if (message.getPriority() == SlackMessage.Priority.CRITICAL) {
                sendSlackMessage(message);
                lastMessageTime = currentTime;
                continue;
            }

            pendingBatch.add(message);
        }

        if (!pendingBatch.isEmpty() && (flush || currentTime - lastMessageTime >= throttleMs)) {
            SlackMessage combined = combineMessages(pendingBatch);
            sendSlackMessage(combined);
            pendingBatch.clear();
            lastMessageTime = currentTime;
        }
    }

    private SlackMessage combineMessages(List<SlackMessage> messages) {
        if (messages.size() == 1) {
            return messages.get(0);
        }

        StringBuilder combinedText = new StringBuilder();
        List<LayoutBlock> combinedBlocks = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                combinedText.append("\n---\n");
                combinedBlocks.add(divider());
            }

            SlackMessage msg = messages.get(i);
            String prefix = msg.getEmoji() != null ? msg.getEmoji() + " " : "";
            combinedText.append(prefix).append(msg.getText());

            if (!msg.getBlocks().isEmpty()) {
                combinedBlocks.addAll(msg.getBlocks());
            } else {
                combinedBlocks.add(section(s -> s.text(markdownText(prefix + msg.getText()))));
            }
        }

        String emoji = messages.get(0).getEmoji() != null
                ? messages.get(0).getEmoji()
                : SlackConstants.DEFAULT_EMOJI;

        return SlackMessage.builder()
                .text(combinedText.toString())
                .blocks(combinedBlocks)
                .emoji(emoji)
                .priority(SlackMessage.Priority.NORMAL)
                .build();
    }

    private void sendSlackMessage(SlackMessage message) {
        String emoji = message.getEmoji() != null ? message.getEmoji() : SlackConstants.DEFAULT_EMOJI;
        messageSender.postMessage(emoji + " " + message.getText(), message.getBlocks(), emoji, threadTs);
    }
}
