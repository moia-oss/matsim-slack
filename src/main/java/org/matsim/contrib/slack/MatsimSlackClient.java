package org.matsim.contrib.slack;

import com.slack.api.Slack;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.event.MessageBotEvent;
import com.slack.api.model.event.MessageChangedEvent;
import com.slack.api.model.event.MessageEvent;
import com.slack.api.socket_mode.SocketModeClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.slack.notifier.SlackNotifier;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import java.io.IOException;
import java.util.*;

import static com.slack.api.model.block.Blocks.asBlocks;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;

/**
 * Core component bridging MATSim and Slack. Manages the Socket Mode connection lifecycle,
 * posts simulation status updates, handles interactive commands (stop/crash), and periodically
 * flushes notifier messages via {@link NotifierManager}.
 *
 * @author nkuehnel / MOIA
 */
public class MatsimSlackClient implements StartupListener, ShutdownListener, IterationStartsListener, MobsimAfterSimStepListener {

    private static final Logger LOG = LogManager.getLogger(MatsimSlackClient.class);
    private static final long MESSAGE_THROTTLE_MS = 10_000;

    private final SlackMessageSender messageSender;
    private final NotifierManager notifierManager;
    private final SocketModeApp socketApp;
    private final String userName;
    private final SlackInteractiveTerminationCriterion terminationCriterion;
    private final int notificationCheckIntervalSeconds;

    private String simulationThreadTimestamp;
    private String resolvedChannelId;
    private final Map<String, SectionBlock> additionalMainSections = new LinkedHashMap<>();
    private volatile boolean crashRequested = false;

    MatsimSlackClient(SlackConfigGroup slackConfigGroup, SlackInteractiveTerminationCriterion terminationCriterion) {
        this.userName = slackConfigGroup.getUserName();
        this.terminationCriterion = terminationCriterion;
        this.notificationCheckIntervalSeconds = slackConfigGroup.getNotificationCheckIntervalSeconds();

        String botToken;
        String appToken;
        String channel;

        switch (slackConfigGroup.getCredentialSource()) {
            case fromEnvironment -> {
                botToken = System.getenv("SLACK_BOT_TOKEN");
                appToken = System.getenv("SLACK_APP_TOKEN");
                channel = System.getenv("SLACK_CHANNEL");
            }
            case fromConfig -> {
                botToken = slackConfigGroup.getBotToken();
                appToken = slackConfigGroup.getAppToken();
                channel = slackConfigGroup.getChannelName();
            }
            default -> throw new IllegalStateException("Unknown credential source: " + slackConfigGroup.getCredentialSource());
        }

        // Config channel overrides environment if explicitly set
        String configChannel = slackConfigGroup.getChannelName();
        if (configChannel != null && !configChannel.isEmpty()) {
            channel = configChannel;
        }

        if (channel == null || channel.isEmpty()) {
            LOG.error("No Slack channel configured. Set SLACK_CHANNEL environment variable or channelName in config.");
        } else if (channel.startsWith("#")) {
            channel = channel.substring(1);
        }

        Slack slack = Slack.getInstance();
        MethodsClient methodsClient = slack.methods(botToken);
        this.messageSender = new SlackMessageSender(methodsClient, channel, userName);
        this.notifierManager = new NotifierManager(messageSender, MESSAGE_THROTTLE_MS);

        AppConfig appConfig = AppConfig.builder().singleTeamBotToken(botToken).build();
        App app = new App(appConfig);

        try {
            socketApp = new SocketModeApp(appToken, SocketModeClient.Backend.Tyrus, app);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addNotifier(SlackNotifier notifier) {
        notifierManager.addNotifier(notifier);
    }

    public void removeNotifier(SlackNotifier notifier) {
        notifierManager.removeNotifier(notifier);
    }

    public void addMessageEventHandler(BoltEventHandler<MessageEvent> messageEventHandler) {
        socketApp.getApp().event(MessageEvent.class, messageEventHandler);
    }

    public void addAdditionalSection(String name, SectionBlock section) {
        additionalMainSections.put(name, section);
    }

    public void removeAdditionalSection(String name) {
        additionalMainSections.remove(name);
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        SectionBlock statusSection = section(s -> s.text(markdownText(
                ":large_yellow_circle: *Simulation Status*\n" +
                        "> Iteration: `" + startupEvent.getServices().getConfig().controller().getFirstIteration() + "`\n" +
                        "> Status: *Starting*"
        )));

        ChatPostMessageResponse response = messageSender.postMessage(
                "Simulation starting", asBlocks(statusSection), SlackConstants.DEFAULT_EMOJI, null);

        if (response != null && response.isOk()) {
            simulationThreadTimestamp = response.getTs();
            resolvedChannelId = response.getChannel();
            messageSender.setChannelId(resolvedChannelId);
            notifierManager.setThreadTs(simulationThreadTimestamp);
        } else if (response != null && "not_in_channel".equals(response.getError())) {
            LOG.error("Bot is not a member of the channel. Invite it with: /invite @YourBotName");
        }

        String text = ":rocket: " + formatStartupMessage(startupEvent);
        messageSender.postMessage(text, List.of(), SlackConstants.DEFAULT_EMOJI, simulationThreadTimestamp);

        addStopTerminationEvent();
        socketApp.getApp().event(MessageChangedEvent.class, (payload, ctx) -> ctx.ack());
        socketApp.getApp().event(MessageBotEvent.class, (payload, ctx) -> ctx.ack());

        try {
            socketApp.startAsync();
        } catch (Exception e) {
            LOG.error("Failed to start Socket Mode app", e);
        }
    }

    private void addStopTerminationEvent() {
        addMessageEventHandler((payload, ctx) -> {
            MessageEvent ev = payload.getEvent();

            if (simulationThreadTimestamp != null
                    && simulationThreadTimestamp.equals(ev.getThreadTs())
                    && ev.getBotId() == null) {
                if ("stop".equals(ev.getText())) {
                    terminationCriterion.requestInteractiveTermination();
                    ctx.client().chatPostMessage(r -> r
                            .channel(ev.getChannel())
                            .threadTs(ev.getTs())
                            .iconEmoji(":octagonal_sign:")
                            .username(userName)
                            .text(":octagonal_sign: Will stop the simulation gracefully!"));
                } else if ("crash".equals(ev.getText())) {
                    crashRequested = true;
                    ctx.client().chatPostMessage(r -> r
                            .channel(ev.getChannel())
                            .threadTs(ev.getTs())
                            .iconEmoji(":boom:")
                            .username(userName)
                            .text(":boom: Crash requested — will crash on next sim step."));
                }
            }
            return ctx.ack();
        });
    }

    private static String formatStartupMessage(StartupEvent startupEvent) {
        return String.format("Simulation startup event %s \n*crs*: %s \n*last iteration*: %d \n*output dir*: %s \n*number of agents*: %d",
                Optional.ofNullable(startupEvent.getServices().getConfig().controller().getRunId()).orElse(""),
                startupEvent.getServices().getConfig().global().getCoordinateSystem(),
                startupEvent.getServices().getConfig().controller().getLastIteration(),
                startupEvent.getServices().getConfig().controller().getOutputDirectory(),
                startupEvent.getServices().getScenario().getPopulation().getPersons().values().size()
        );
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        int iteration = iterationStartsEvent.getIteration();

        SectionBlock statusSection = section(s -> s.text(new MarkdownTextObject(
                ":large_blue_circle: *Simulation Status*\n" +
                        "> Iteration: `" + iteration + "`\n" +
                        "> Status: *Running*", false)));

        List<LayoutBlock> blocks = new ArrayList<>();
        blocks.add(statusSection);
        blocks.addAll(additionalMainSections.values());

        messageSender.updateMessage(simulationThreadTimestamp, blocks, "Simulation running");

        notifierManager.checkAndSendNotifications(true);
    }

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        SectionBlock statusSection = section(s -> s.text(new MarkdownTextObject(
                (shutdownEvent.isUnexpected() ? ":large_red_square: " : ":large_green_circle: ") + "*Simulation Status*\n" +
                        "> Iteration: `" + shutdownEvent.getIteration() + "`\n" +
                        "> Status: *Ended*", false)));

        messageSender.updateMessage(simulationThreadTimestamp, asBlocks(statusSection), "Simulation complete");

        String emoji = shutdownEvent.isUnexpected() ? ":large_red_square:" : ":large_green_circle:";
        String text = String.format("%s Shutting down %s in iteration %d.",
                emoji, (shutdownEvent.isUnexpected() ? "unexpectedly" : "normally"), shutdownEvent.getIteration());
        messageSender.postMessage(text, List.of(), SlackConstants.DEFAULT_EMOJI, simulationThreadTimestamp);

        try {
            socketApp.stop();
            socketApp.close();
        } catch (Exception e) {
            LOG.error("Error closing Socket Mode app", e);
        }
    }

    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
        if (crashRequested) {
            throw new RuntimeException("Crash requested via Slack");
        }
        if (notificationCheckIntervalSeconds > 0
                && e.getSimulationTime() % notificationCheckIntervalSeconds == 0) {
            notifierManager.checkAndSendNotifications(false);
        }
    }
}
