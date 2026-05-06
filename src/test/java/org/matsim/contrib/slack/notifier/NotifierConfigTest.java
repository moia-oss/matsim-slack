package org.matsim.contrib.slack.notifier;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.slack.notifier.impl.LinkTrafficNotifierParams;
import org.matsim.contrib.slack.notifier.impl.MemoryObserverNotifierParams;
import org.matsim.contrib.slack.notifier.impl.StuckAgentNotifierParams;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author nkuehnel / MOIA
 */
class NotifierConfigTest {

    @Test
    void testBasicProperties() {
        StuckAgentNotifierParams config = new StuckAgentNotifierParams();
        assertEquals("stuckAgent", config.getType());
        assertTrue(config.isEnabled());
    }

    @Test
    void testDisabledConfig() {
        StuckAgentNotifierParams config = new StuckAgentNotifierParams();
        config.setEnabled(false);
        assertFalse(config.isEnabled());
    }

    @Test
    void testTypedParameters() {
        LinkTrafficNotifierParams config = new LinkTrafficNotifierParams();
        config.setThreshold(50);
        config.setLinkId("my_link");
        config.setMessageTemplate("custom {count}");

        assertEquals(50, config.getThreshold());
        assertEquals("my_link", config.getLinkId());
        assertEquals("custom {count}", config.getMessageTemplate());
    }

    @Test
    void testDefaultValues() {
        LinkTrafficNotifierParams config = new LinkTrafficNotifierParams();
        assertEquals(100, config.getThreshold());
        assertEquals("", config.getLinkId());
        assertEquals("{count} vehicles left link {linkId}", config.getMessageTemplate());
        assertEquals(":car:", config.getEmoji());
    }

    @Test
    void testEmojiOverride() {
        StuckAgentNotifierParams config = new StuckAgentNotifierParams();
        assertEquals(":warning:", config.getEmoji());

        config.setEmoji(":rocket:");
        assertEquals(":rocket:", config.getEmoji());
    }

    @Test
    void testMemoryObserverDefaults() {
        MemoryObserverNotifierParams config = new MemoryObserverNotifierParams();
        assertEquals(5, config.getReportInterval());
        assertEquals(80, config.getWarnThresholdPercent());
        assertEquals(":bar_chart:", config.getEmoji());
    }

    @Test
    void testUnknownParamsStillStored() {
        LinkTrafficNotifierParams config = new LinkTrafficNotifierParams();
        config.addParam("customKey", "customValue");
        assertEquals("customValue", config.getString("customKey"));
    }

    @Test
    void testHelperMethodsForUnknownParams() {
        NotifierConfig config = new NotifierConfig("test", true);
        config.addParam("count", "42");
        config.addParam("rate", "3.14");
        config.addParam("flag", "true");

        assertEquals(42, config.getInt("count", 0));
        assertEquals(3.14, config.getDouble("rate", 0.0), 0.001);
        assertTrue(config.getBoolean("flag", false));
        assertEquals(99, config.getInt("missing", 99));
    }
}