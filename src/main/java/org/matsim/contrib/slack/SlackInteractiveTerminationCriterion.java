package org.matsim.contrib.slack;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.TerminationCriterion;

/**
 * Termination criterion that can be triggered interactively via Slack by typing "stop" in the simulation thread.
 *
 * @author nkuehnel / MOIA
 */
public class SlackInteractiveTerminationCriterion implements TerminationCriterion {

    private final int lastIteration;
    private volatile boolean interactiveTermination = false;

    public SlackInteractiveTerminationCriterion(ControllerConfigGroup controllerConfigGroup) {
        this.lastIteration = controllerConfigGroup.getLastIteration();
    }

    @Override
    public boolean mayTerminateAfterIteration(int iteration) {
        return iteration >= lastIteration || interactiveTermination;
    }

    @Override
    public boolean doTerminate(int iteration) {
        return iteration >= lastIteration || interactiveTermination;
    }

    public void requestInteractiveTermination() {
        interactiveTermination = true;
    }
}
