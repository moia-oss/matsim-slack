package org.matsim.contrib.slack.example;

import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.slack.SlackConfigGroup;
import org.matsim.contrib.slack.SlackModule;
import org.matsim.contrib.slack.notifier.impl.DrtPerformanceNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationProgressNotifierParams;
import org.matsim.contrib.slack.notifier.impl.IterationTimeNotifierParams;
import org.matsim.contrib.slack.notifier.impl.LinkTrafficNotifierParams;
import org.matsim.contrib.slack.notifier.impl.MemoryObserverNotifierParams;
import org.matsim.contrib.slack.notifier.impl.ModeShareNotifierParams;
import org.matsim.contrib.slack.notifier.impl.StuckAgentNotifierParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * Example demonstrating the Slack integration with a DRT scenario.
 * Requires SLACK_BOT_TOKEN and SLACK_APP_TOKEN environment variables.
 *
 * @author nkuehnel / MOIA
 */
public class RunSlackExample {

    public static void main(String[] args) {
        Config config;
        if (args == null || args.length == 0 || args[0] == null) {
            config = ConfigUtils.loadConfig(
                    IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml"),
                    new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
        } else {
            config = ConfigUtils.loadConfig(args);
        }
        config.controller().setLastIteration(10);
        config.controller().setRunId("Slacktest123");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        SlackConfigGroup slackConfigGroup = new SlackConfigGroup();
        slackConfigGroup.setCredentialSource(SlackConfigGroup.CredentialSource.fromEnvironment);

        StuckAgentNotifierParams stuckConfig = new StuckAgentNotifierParams();
        slackConfigGroup.addNotifierConfig(stuckConfig);

        //LinkTrafficNotifierParams linkConfig = new LinkTrafficNotifierParams();
        //linkConfig.setThreshold(50);
        //linkConfig.setEmoji(":car:");
        //slackConfigGroup.addNotifierConfig(linkConfig);

        IterationTimeNotifierParams timeConfig = new IterationTimeNotifierParams();
        timeConfig.setReportInterval(5);
        timeConfig.setWarnThresholdSeconds(3600);
        slackConfigGroup.addNotifierConfig(timeConfig);

        IterationProgressNotifierParams progressConfig = new IterationProgressNotifierParams();
        progressConfig.setMessageInterval(1);
        slackConfigGroup.addNotifierConfig(progressConfig);

        DrtPerformanceNotifierParams drtConfig = new DrtPerformanceNotifierParams();
        slackConfigGroup.addNotifierConfig(drtConfig);

        MemoryObserverNotifierParams memoryConfig = new MemoryObserverNotifierParams();
        memoryConfig.setReportInterval(2);
        slackConfigGroup.addNotifierConfig(memoryConfig);

        ModeShareNotifierParams modeShareConfig = new ModeShareNotifierParams();
        modeShareConfig.setReportInterval(5);
        slackConfigGroup.addNotifierConfig(modeShareConfig);

        Controler controler = DrtControlerCreator.createControler(config, false);
        controler.addOverridingModule(new SlackModule(slackConfigGroup));
        controler.run();
    }
}