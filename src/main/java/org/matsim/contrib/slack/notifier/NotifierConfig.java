package org.matsim.contrib.slack.notifier;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Base configuration class for all notifiers. Extend this to define typed parameter sets with
 * {@code @Parameter} fields that are automatically available in both Java API and XML configuration.
 *
 * @author nkuehnel / MOIA
 */
public class NotifierConfig extends ReflectiveConfigGroup {

    public static final String SET_TYPE = "notifier";

    @Parameter
    @Comment("Notifier type identifier (e.g., 'linkTraffic', 'stuckAgent', 'drtPerformance', 'iterationTime', 'memoryObserver')")
    private String type = "";

    @Parameter
    @Comment("Whether this notifier is active")
    private boolean enabled = true;

    @Parameter
    @Comment("Emoji icon for messages from this notifier")
    private String emoji = ":satellite_antenna:";

    public NotifierConfig() {
        super(SET_TYPE, true);
    }

    public NotifierConfig(String type, boolean enabled) {
        super(SET_TYPE, true);
        this.type = type;
        this.enabled = enabled;
    }

    protected NotifierConfig(String type, boolean enabled, String setType) {
        super(setType, true);
        this.type = type;
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getString(String key) {
        return getParams().get(key);
    }

    public String getString(String key, String defaultValue) {
        return getParams().getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = getParams().get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String value = getParams().get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getParams().get(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

}