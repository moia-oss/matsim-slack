package org.matsim.contrib.slack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.EventHandlerNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackNotifier;
import org.matsim.contrib.slack.notifier.impl.DrtPerformanceNotifier;
import org.matsim.contrib.slack.notifier.impl.IterationProgressNotifier;
import org.matsim.contrib.slack.notifier.impl.IterationTimeNotifier;
import org.matsim.contrib.slack.notifier.impl.LinkTrafficNotifier;
import org.matsim.contrib.slack.notifier.impl.MemoryObserverNotifier;
import org.matsim.contrib.slack.notifier.impl.ModeShareNotifier;
import org.matsim.contrib.slack.notifier.impl.StuckAgentNotifier;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;

import com.google.inject.Key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Guice module that installs the Slack integration into a MATSim controller.
 * Binds {@link MatsimSlackClient}, registers notifiers from {@link SlackConfigGroup}, and wires
 * event handlers and controller listeners.
 *
 * @author nkuehnel / MOIA
 */
public class SlackModule extends AbstractModule {

    private static final Logger LOG = LogManager.getLogger(SlackModule.class);

    private final SlackConfigGroup slackConfigGroup;
    private final Map<String, Function<MatsimSlackClient, SlackNotifier>> notifierFactories = new HashMap<>();
    private final List<DrtPerformanceNotifier> drtNotifiers = new ArrayList<>();
    private final List<ModeShareNotifier> modeShareNotifiers = new ArrayList<>();

    public SlackModule(SlackConfigGroup slackConfigGroup) {
        super();
        this.slackConfigGroup = slackConfigGroup;
        registerDefaultNotifiers();
    }

    private void registerDefaultNotifiers() {
        notifierFactories.put("linkTraffic", LinkTrafficNotifier::new);
        notifierFactories.put("stuckAgent", StuckAgentNotifier::new);
        notifierFactories.put("drtPerformance", DrtPerformanceNotifier::new);
        notifierFactories.put("iterationTime", IterationTimeNotifier::new);
        notifierFactories.put("iterationProgress", IterationProgressNotifier::new);
        notifierFactories.put("memoryObserver", MemoryObserverNotifier::new);
        notifierFactories.put("modeShare", ModeShareNotifier::new);
    }

    public void registerNotifierFactory(String type, Function<MatsimSlackClient, SlackNotifier> factory) {
        notifierFactories.put(type, factory);
    }

    @Override
    public void install() {
        SlackInteractiveTerminationCriterion terminationCriterion = new SlackInteractiveTerminationCriterion(getConfig().controller());
        bind(TerminationCriterion.class).toInstance(terminationCriterion);

        MatsimSlackClient slackClient = new MatsimSlackClient(slackConfigGroup, terminationCriterion);
        bind(MatsimSlackClient.class).toInstance(slackClient);

        addControlerListenerBinding().to(MatsimSlackClient.class);
        addMobsimListenerBinding().to(MatsimSlackClient.class);

        installNotifiers(slackClient);
    }

    private void installNotifiers(MatsimSlackClient slackClient) {
        for (NotifierConfig config : slackConfigGroup.getNotifierConfigs()) {
            if (!config.isEnabled()) {
                LOG.info("Notifier '{}' is disabled, skipping.", config.getType());
                continue;
            }

            Function<MatsimSlackClient, SlackNotifier> factory = notifierFactories.get(config.getType());
            if (factory == null) {
                LOG.warn("Unknown notifier type '{}', skipping. Known types: {}", config.getType(), notifierFactories.keySet());
                continue;
            }

            SlackNotifier notifier = factory.apply(slackClient);
            notifier.configure(config);
            slackClient.addNotifier(notifier);

            if (notifier instanceof DrtPerformanceNotifier drtNotifier) {
                drtNotifiers.add(drtNotifier);
                addControlerListenerBinding().toInstance(drtNotifier);
            } else if (notifier instanceof ModeShareNotifier modeShareNotifier) {
                modeShareNotifiers.add(modeShareNotifier);
                addControlerListenerBinding().toInstance(modeShareNotifier);
            } else if (notifier instanceof EventHandlerNotifier eventHandlerNotifier) {
                addEventHandlerBinding().toInstance(eventHandlerNotifier);
            } else if (notifier instanceof ControlerEventNotifier controlerEventNotifier) {
                addControlerListenerBinding().toInstance(controlerEventNotifier);
            }

            LOG.info("Registered notifier: {} (type={})", notifier.getClass().getSimpleName(), config.getType());
        }

        if (!drtNotifiers.isEmpty()) {
            addControlerListenerBinding().toInstance((StartupListener) event -> {
                MultiModeDrtConfigGroup multiModeDrt = MultiModeDrtConfigGroup.get(event.getServices().getConfig());
                DrtConfigGroup firstDrtMode = multiModeDrt.getModalElements().iterator().next();
                DrtEventSequenceCollector collector = event.getServices().getInjector()
                        .getInstance(Key.get(DrtEventSequenceCollector.class, DvrpModes.mode(firstDrtMode.getMode())));
                drtNotifiers.forEach(n -> n.setDrtEventSequenceCollector(collector));
            });
        }

        if (!modeShareNotifiers.isEmpty()) {
            addControlerListenerBinding().toInstance((StartupListener) event -> {
                Population population = event.getServices().getScenario().getPopulation();
                AnalysisMainModeIdentifier mmi = event.getServices().getInjector()
                        .getInstance(AnalysisMainModeIdentifier.class);
                modeShareNotifiers.forEach(n -> {
                    n.setPopulation(population);
                    n.setMainModeIdentifier(mmi);
                });
            });
        }
    }
}
