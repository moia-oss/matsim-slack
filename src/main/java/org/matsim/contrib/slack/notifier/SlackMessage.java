package org.matsim.contrib.slack.notifier;

import com.slack.api.model.block.LayoutBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable message produced by notifiers. Includes text, optional Block Kit blocks,
 * an emoji prefix, and a priority level that controls throttling behavior.
 *
 * @author nkuehnel / MOIA
 */
public class SlackMessage {

    private final String text;
    private final List<LayoutBlock> blocks;
    private final String emoji;
    private final Priority priority;

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    private SlackMessage(String text, List<LayoutBlock> blocks, String emoji, Priority priority) {
        this.text = text;
        this.blocks = blocks != null ? blocks : new ArrayList<>();
        this.emoji = emoji;
        this.priority = priority;
    }

    public String getText() {
        return text;
    }

    public List<LayoutBlock> getBlocks() {
        return blocks;
    }

    public String getEmoji() {
        return emoji;
    }

    public Priority getPriority() {
        return priority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private List<LayoutBlock> blocks;
        private String emoji = ":satellite_antenna:";
        private Priority priority = Priority.NORMAL;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder blocks(List<LayoutBlock> blocks) {
            this.blocks = blocks;
            return this;
        }

        public Builder emoji(String emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public SlackMessage build() {
            if (text == null || text.isEmpty()) {
                throw new IllegalStateException("Message text cannot be null or empty");
            }
            return new SlackMessage(text, blocks, emoji, priority);
        }
    }
}
