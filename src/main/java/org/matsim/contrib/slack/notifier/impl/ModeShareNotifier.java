package org.matsim.contrib.slack.notifier.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

import java.util.*;

/**
 * Reports mode share analysis at configurable intervals. Numbers match MATSim's standard modestats.png output
 * by using selected plans and the AnalysisMainModeIdentifier.
 *
 * @author nkuehnel / MOIA
 */
public class ModeShareNotifier extends ControlerEventNotifier {

    public static final String TYPE = "modeShare";

    private int reportInterval = 10;

    private Population population;
    private AnalysisMainModeIdentifier mainModeIdentifier;
    private Map<String, Double> previousShares;

    public ModeShareNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    public void setPopulation(Population population) {
        this.population = population;
    }

    public void setMainModeIdentifier(AnalysisMainModeIdentifier mainModeIdentifier) {
        this.mainModeIdentifier = mainModeIdentifier;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        ModeShareNotifierParams params = (ModeShareNotifierParams) config;
        this.reportInterval = params.getReportInterval();
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (population == null || mainModeIdentifier == null) {
            return;
        }

        int iteration = event.getIteration();
        if (reportInterval <= 0 || iteration % reportInterval != 0) {
            return;
        }

        Map<String, Long> modeCounts = new TreeMap<>();
        long totalTrips = 0;

        for (Person person : population.getPersons().values()) {
            if (isFilteredAgent(person.getId())) {
                continue;
            }
            Plan plan = person.getSelectedPlan();
            if (plan == null) {
                continue;
            }
            List<Trip> trips = TripStructureUtils.getTrips(plan);
            for (Trip trip : trips) {
                String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                modeCounts.merge(mode, 1L, Long::sum);
                totalTrips++;
            }
        }

        if (totalTrips == 0) {
            return;
        }

        Map<String, Double> currentShares = new LinkedHashMap<>();
        long finalTotalTrips = totalTrips;
        modeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> currentShares.put(e.getKey(), 100.0 * e.getValue() / finalTotalTrips));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*Mode Share (Iteration %d)*\n", iteration));
        sb.append(String.format("> Total trips: `%,d`\n", totalTrips));

        for (Map.Entry<String, Double> entry : currentShares.entrySet()) {
            String mode = entry.getKey();
            double share = entry.getValue();
            String delta = "";
            if (previousShares != null && previousShares.containsKey(mode)) {
                double diff = share - previousShares.get(mode);
                delta = String.format(" (%+.1fpp)", diff);
            } else if (previousShares != null) {
                delta = " (new)";
            }
            sb.append(String.format("> `%-12s` %5.1f%%%s\n", mode, share, delta));
        }

        if (previousShares != null) {
            for (String oldMode : previousShares.keySet()) {
                if (!currentShares.containsKey(oldMode)) {
                    sb.append(String.format("> `%-12s`   0.0%% (was %.1f%%)\n", oldMode, previousShares.get(oldMode)));
                }
            }
        }

        queueMessage(SlackMessage.builder()
                .text(sb.toString())
                .emoji(config.getEmoji() != null ? config.getEmoji() : ":bar_chart:")
                .priority(SlackMessage.Priority.NORMAL)
                .build());

        previousShares = currentShares;
    }

    private boolean isFilteredAgent(Id<Person> personId) {
        String id = personId.toString();
        return id.contains("freight")
                || id.startsWith("pt_")
                || id.startsWith("drt_");
    }
}
