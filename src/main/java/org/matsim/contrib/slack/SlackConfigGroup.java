package org.matsim.contrib.slack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.impl.DrtPerformanceNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationProgressNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationTimeNotifierParams;
import org.matsim.contrib.slack.notifier.impl.LinkTrafficNotifierParams;
import org.matsim.contrib.slack.notifier.impl.MemoryObserverNotifierParams;
import org.matsim.contrib.slack.notifier.impl.ModeShareNotifierParams;
import org.matsim.contrib.slack.notifier.impl.StuckAgentNotifierParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * MATSim config group for the Slack integration. Defines connection settings (tokens, channel)
 * and hosts notifier parameter sets. Custom notifier types can be registered via
 * {@link #registerNotifierType(String, Supplier)} before config loading.
 *
 * @author nkuehnel / MOIA
 */
public class SlackConfigGroup extends ReflectiveConfigGroup {

    private static final Logger LOG = LogManager.getLogger(SlackConfigGroup.class);

    public static final String GROUP_NAME = "slack";

    public enum CredentialSource {fromEnvironment, fromConfig}

    @Parameter
    @Comment("Defines the source of tokens and channel ID. " +
            "fromEnvironment will load from SLACK_BOT_TOKEN, SLACK_APP_TOKEN, and SLACK_CHANNEL environment variables. " +
            "fromConfig reads from the respective config settings in this group. " +
            "Individual config values (e.g. channelName) override the environment variable if set regardless of this setting.")
    private CredentialSource credentialSource = CredentialSource.fromEnvironment;

    @Parameter
    @Comment("Bot token for the installed workspace bot user. Starts with 'xoxb-'. Only used with credentialSource == fromConfig. " +
            "Be aware that setting it here might lead to the token written out in a config file.")
    private String botToken = "";

    @Parameter
    @Comment("App token of the app (across workspaces). Starts with 'xapp-'. Only used with credentialSource == fromConfig. " +
            "Be aware that setting it here might lead to the token written out in a config file.")
    private String appToken = "";

    @Parameter
    @Comment("Channel name or ID of the existing workspace channel where the output is posted to. " +
            "Both channel names (e.g. 'monitoring') and IDs (e.g. 'C0123456789') are accepted. " +
            "If set, overrides the SLACK_CHANNEL environment variable regardless of credentialSource.")
    private String channelName = "";

    @Parameter
    @Comment("Desired display name of the bot user when posting messages.")
    private String userName = "MATSim Slack Bot";

    @Parameter
    @Comment("Interval in simulated seconds at which queued notifier messages are flushed during the mobsim. " +
            "Lower values give faster alerts but increase overhead from checking all notifiers each time. " +
            "Set to 0 to check after every sim step (not recommended for large scenarios).")
    private int notificationCheckIntervalSeconds = 3600;

    public SlackConfigGroup() {
        super(GROUP_NAME);
    }

    private final Map<String, Supplier<NotifierConfig>> notifierTypes = new java.util.LinkedHashMap<>(Map.of(
            LinkTrafficNotifierParams.SET_NAME, LinkTrafficNotifierParams::new,
            StuckAgentNotifierParams.SET_NAME, StuckAgentNotifierParams::new,
            DrtPerformanceNotifierParams.SET_NAME, DrtPerformanceNotifierParams::new,
            IterationTimeNotifierParams.SET_NAME, IterationTimeNotifierParams::new,
            IterationProgressNotifierParams.SET_NAME, IterationProgressNotifierParams::new,
            MemoryObserverNotifierParams.SET_NAME, MemoryObserverNotifierParams::new,
            ModeShareNotifierParams.SET_NAME, ModeShareNotifierParams::new
    ));

    public void registerNotifierType(String type, Supplier<NotifierConfig> factory) {
        notifierTypes.put(type, factory);
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        var factory = notifierTypes.get(type);
        if (factory != null) {
            return factory.get();
        }
        throw new IllegalArgumentException("Unknown parameter set type: '" + type + "'. Known types: " + notifierTypes.keySet());
    }

    public void addNotifierConfig(NotifierConfig notifierConfig) {
        addParameterSet(notifierConfig);
    }

    public Collection<NotifierConfig> getNotifierConfigs() {
        List<NotifierConfig> configs = new ArrayList<>();
        for (String type : notifierTypes.keySet()) {
            for (ConfigGroup paramSet : getParameterSets(type)) {
                configs.add((NotifierConfig) paramSet);
            }
        }
        return configs;
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        if (credentialSource == CredentialSource.fromConfig) {
            LOG.warn("Credential source is {}. Please be aware that tokens may be written to file in the output folder.", credentialSource);
            if (botToken == null || botToken.isEmpty()) {
                LOG.warn("No bot token set, will likely crash...");
            } else if (!botToken.startsWith("xoxb")) {
                LOG.warn("Bot token should start with 'xoxb', please check for the correct token. Will likely crash...");
            }
            if (appToken == null || appToken.isEmpty()) {
                LOG.warn("No app token set, will likely crash...");
            } else if (!appToken.startsWith("xapp")) {
                LOG.warn("App token should start with 'xapp', please check for the correct token. Will likely crash...");
            }
        }
        if (channelName != null && !channelName.isEmpty() && channelName.startsWith("#")) {
            LOG.warn("Channel starts with '#'. Please omit the '#' prefix — use the plain channel name or ID.");
        }
    }

    public CredentialSource getCredentialSource() {
        return credentialSource;
    }

    public void setCredentialSource(CredentialSource credentialSource) {
        this.credentialSource = credentialSource;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getNotificationCheckIntervalSeconds() {
        return notificationCheckIntervalSeconds;
    }

    public void setNotificationCheckIntervalSeconds(int notificationCheckIntervalSeconds) {
        this.notificationCheckIntervalSeconds = notificationCheckIntervalSeconds;
    }
}
