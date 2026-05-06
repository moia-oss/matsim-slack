package org.matsim.contrib.slack.notifier.impl;

import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence.PersonEvents;
import org.matsim.contrib.slack.MatsimSlackClient;
import org.matsim.contrib.slack.notifier.ControlerEventNotifier;
import org.matsim.contrib.slack.notifier.NotifierConfig;
import org.matsim.contrib.slack.notifier.SlackMessage;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;

/**
 * Reports DRT performance metrics (requests, rejections, wait/ride times) at iteration end.
 *
 * @author nkuehnel / MOIA
 */
public class DrtPerformanceNotifier extends ControlerEventNotifier {

    private DrtEventSequenceCollector drtEventSequenceCollector;
    private int reportInterval = 1;

    public DrtPerformanceNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    public void setDrtEventSequenceCollector(DrtEventSequenceCollector collector) {
        this.drtEventSequenceCollector = collector;
    }

    @Override
    public String getType() {
        return "drtPerformance";
    }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        DrtPerformanceNotifierParams params = (DrtPerformanceNotifierParams) config;
        this.reportInterval = params.getReportInterval();
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        super.notifyIterationStarts(event);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (drtEventSequenceCollector == null) {
            return;
        }

        if (reportInterval > 0 && event.getIteration() % reportInterval != 0) {
            return;
        }

        Collection<EventSequence> performed = drtEventSequenceCollector.getPerformedRequestSequences().values();
        Collection<EventSequence> rejected = drtEventSequenceCollector.getRejectedRequestSequences().values();

        int totalRequests = performed.size() + rejected.size();
        int rejections = rejected.size();

        DoubleSummaryStatistics waitStats = new DoubleSummaryStatistics();
        DoubleSummaryStatistics rideStats = new DoubleSummaryStatistics();

        for (EventSequence seq : performed) {
            for (PersonEvents personEvents : seq.getPersonEvents().values()) {
                if (personEvents.getPickedUp().isPresent() && personEvents.getDeparture().isPresent()) {
                    double waitTime = personEvents.getPickedUp().get().getTime() - personEvents.getDeparture().get().getTime();
                    waitStats.accept(waitTime);
                }
                if (personEvents.getPickedUp().isPresent() && personEvents.getDroppedOff().isPresent()) {
                    double rideTime = personEvents.getDroppedOff().get().getTime() - personEvents.getPickedUp().get().getTime();
                    rideStats.accept(rideTime);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("*DRT Performance (Iteration %d)*\n", event.getIteration()));
        sb.append(String.format("> Requests: `%d` (rejected: `%d`)\n", totalRequests, rejections));

        if (waitStats.getCount() > 0) {
            sb.append(String.format("> Wait time — avg: `%s`, max: `%s`\n",
                    formatTime(waitStats.getAverage()), formatTime(waitStats.getMax())));
        }
        if (rideStats.getCount() > 0) {
            sb.append(String.format("> Ride time — avg: `%s`, max: `%s`",
                    formatTime(rideStats.getAverage()), formatTime(rideStats.getMax())));
        }

        queueMessage(SlackMessage.builder()
                .text(sb.toString())
                .emoji(config.getEmoji() != null ? config.getEmoji() : ":taxi:")
                .priority(SlackMessage.Priority.NORMAL)
                .build());
    }

    private String formatTime(double seconds) {
        if (seconds < 60) {
            return String.format("%.0fs", seconds);
        } else {
            return String.format("%.1fmin", seconds / 60.0);
        }
    }
}
