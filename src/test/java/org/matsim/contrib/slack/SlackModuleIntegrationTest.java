package org.matsim.contrib.slack;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackNotifier;
import org.matsim.contrib.slack.notifier.impl.LinkTrafficNotifierParams;
import org.matsim.contrib.slack.notifier.impl.MemoryObserverNotifierParams;
import org.matsim.contrib.slack.notifier.impl.StuckAgentNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationTimeNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationProgressNotifierParams;

import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class SlackModuleIntegrationTest {

    @Test
    void testSlackConfigGroupRegistersAllNotifierTypes() {
        SlackConfigGroup config = new SlackConfigGroup();

        config.addNotifierConfig(new StuckAgentNotifierParams());
        config.addNotifierConfig(new LinkTrafficNotifierParams());
        config.addNotifierConfig(new IterationTimeNotifierParams());
        config.addNotifierConfig(new IterationProgressNotifierParams());
        config.addNotifierConfig(new MemoryObserverNotifierParams());

        Collection<NotifierConfig> notifiers = config.getNotifierConfigs();
        assertEquals(5, notifiers.size());
    }

    @Test
    void testSlackConfigGroupCreateParameterSet() {
        SlackConfigGroup config = new SlackConfigGroup();

        assertTrue(config.createParameterSet("linkTraffic") instanceof LinkTrafficNotifierParams);
        assertTrue(config.createParameterSet("stuckAgent") instanceof StuckAgentNotifierParams);
        assertTrue(config.createParameterSet("iterationTime") instanceof IterationTimeNotifierParams);
        assertTrue(config.createParameterSet("iterationProgress") instanceof IterationProgressNotifierParams);
        assertTrue(config.createParameterSet("memoryObserver") instanceof MemoryObserverNotifierParams);
    }

    @Test
    void testSlackConfigGroupRejectsUnknownType() {
        SlackConfigGroup config = new SlackConfigGroup();
        assertThrows(IllegalArgumentException.class, () -> config.createParameterSet("unknown"));
    }

    @Test
    void testRegisterCustomNotifierType() {
        SlackConfigGroup config = new SlackConfigGroup();
        config.registerNotifierType("custom", () -> new NotifierConfig("custom", true, "custom") {});

        var paramSet = config.createParameterSet("custom");
        assertNotNull(paramSet);
        assertTrue(paramSet instanceof NotifierConfig);
        assertEquals("custom", ((NotifierConfig) paramSet).getType());
    }

    @Test
    void testRegisteredCustomNotifierAppearsInGetNotifierConfigs() {
        SlackConfigGroup config = new SlackConfigGroup();
        config.registerNotifierType("custom", () -> new NotifierConfig("custom", true, "custom") {});

        NotifierConfig customConfig = new NotifierConfig("custom", true, "custom") {};
        config.addNotifierConfig(customConfig);

        Collection<NotifierConfig> notifiers = config.getNotifierConfigs();
        assertTrue(notifiers.stream().anyMatch(n -> "custom".equals(n.getType())));
    }

    @Test
    void testSlackModuleRegistersDefaultFactories() {
        SlackConfigGroup configGroup = new SlackConfigGroup();
        SlackModule module = new SlackModule(configGroup);

        module.registerNotifierFactory("custom", client -> new SlackNotifier() {
            @Override public String getType() { return "custom"; }
            @Override public void configure(NotifierConfig config) {}
            @Override public java.util.Optional<org.matsim.contrib.slack.notifier.SlackMessage> createMessage() {
                return java.util.Optional.empty();
            }
            @Override public boolean hasMessage() { return false; }
        });
    }

    @Test
    void testTypedParamsDefaultValues() {
        LinkTrafficNotifierParams linkParams = new LinkTrafficNotifierParams();
        assertEquals("linkTraffic", linkParams.getType());
        assertEquals(100, linkParams.getThreshold());
        assertEquals("", linkParams.getLinkId());
        assertEquals(":car:", linkParams.getEmoji());
        assertTrue(linkParams.isEnabled());

        MemoryObserverNotifierParams memParams = new MemoryObserverNotifierParams();
        assertEquals("memoryObserver", memParams.getType());
        assertEquals(5, memParams.getReportInterval());
        assertEquals(80, memParams.getWarnThresholdPercent());
        assertEquals(":bar_chart:", memParams.getEmoji());

        IterationTimeNotifierParams timeParams = new IterationTimeNotifierParams();
        assertEquals("iterationTime", timeParams.getType());
        assertEquals(5, timeParams.getReportInterval());
        assertEquals(3600, timeParams.getWarnThresholdSeconds());
        assertEquals(":stopwatch:", timeParams.getEmoji());
    }

    @Test
    void testDisabledNotifierConfig() {
        StuckAgentNotifierParams params = new StuckAgentNotifierParams();
        params.setEnabled(false);
        assertFalse(params.isEnabled());
    }
}
