package basilica2.agents.listeners.plan;

import java.util.HashSet;
import basilica2.agents.components.RepresentationCapture;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.StepDoneEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

/** Activity-local gates; the installed shared PlanExecutor is unchanged. */
public class RepresentationPlanExecutor extends PlanExecutor {
    private final Set<String> ready = new HashSet<String>();
    private final Set<String> passed = new HashSet<String>();
    private Step gate;
    private Step completionRequested;
    private boolean submitted;

    public RepresentationPlanExecutor(Agent agent) { super(agent); }

    private Step current() {
        return currentPlan == null || currentPlan.currentStage == null
            ? null : currentPlan.currentStage.currentStep;
    }

    public synchronized void beginGate(Step step) {
        gate = step;
        checkGate();
    }

    @Override synchronized void activateStage(String name) {
        ready.clear();
        passed.clear();
        submitted = false;
        super.activateStage(name);
        RepresentationCapture.record(agent,"activity.phase",RepresentationCapture.data("phase",name));
    }

    @Override public Class[] getListenerEventClasses() {
        return new Class[] {LaunchEvent.class, StepDoneEvent.class, MessageEvent.class, FileEvent.class};
    }

    @Override public synchronized void processEvent(InputCoordinator input, Event event) {
        source = input;
        if(event instanceof FileEvent) RepresentationCapture.record(agent,"activity.checkpoint",RepresentationCapture.data("file",((FileEvent)event).getFileName()));
        Step step = current();
        String phase = currentPlan == null || currentPlan.currentStage == null ? ""
            : currentPlan.currentStage.name.toLowerCase(Locale.ROOT);
        // The announcement is already visible while its speaking timer runs.
        // Remember commands during that interval instead of silently dropping them.
        if (event instanceof MessageEvent && !(event instanceof PrivateMessageEvent)) {
            MessageEvent message = (MessageEvent) event;
            String id = participant(message.getFrom());
            if (id != null && message.getText() != null) {
                String text = message.getText().trim().toLowerCase(Locale.ROOT);
                if ((phase + " done").equals(text)) ready.add(id);
                if ((phase + " not done").equals(text)) ready.remove(id);
                if ("submit".equals(phase) && "submitted".equals(text)) submitted = true;
            }
        }
        // Preserve checks that arrive during the coding introduction, in either order.
        if (event instanceof FileEvent && currentPlan != null && currentPlan.currentStage != null
                && "Coding".equals(currentPlan.currentStage.name)) {
            String file = ((FileEvent) event).getFileName();
            if ("testcase-complete_1".equals(file) || "testcase-complete_2".equals(file)) passed.add(file);
        }
        if (event instanceof LaunchEvent) {
            super.processEvent(input, event);
        } else if (event instanceof StepDoneEvent) {
            // A stale/generic completion must never bypass a readiness gate.
            if (step != null && step != gate)
                stepDone(((StepDoneEvent) event).getStepName());
        } else checkGate();
    }

    private void checkGate() {
        Step step = current();
        if (step == null || step != gate || step == completionRequested) return;
        String phase = step.attributes.get("phase");
        if (("submit".equals(phase) && submitted)
                || (!"submit".equals(phase) && allPresentReady()
                    && (!"coding".equals(phase) || passed.size() == 2))) completeCurrent(true);
    }

    private String participant(String sender) {
        if (sender == null || sender.startsWith("Private_") || sender.startsWith("Camera_")
                || sender.startsWith("tab_group") || sender.startsWith("OPEBot")) return null;
        State state = StateMemory.getSharedState(agent);
        for (String id : state.getStudentIdList()) {
            if (sender.equals(id) || sender.equals(state.getStudentName(id))) return id;
        }
        return null;
    }

    private boolean allPresentReady() {
        List<String> ids = StateMemory.getSharedState(agent).getStudentIdList();
        return !ids.isEmpty() && ready.containsAll(ids);
    }

    @Override public synchronized void stepDone(String name) {
        Step step = current();
        if (step != null && step.name.equals(name)) stepDone();
    }

    @Override public synchronized void stepDone() {
        completeCurrent(false);
    }

    private void completeCurrent(boolean allowGate) {
        Step step = current();
        if (step == null || step == completionRequested || (step == gate && !allowGate)) return;
        completionRequested = step;
        super.stepDone();
    }

    @Override public synchronized void timedOut(String id) {
        RepresentationCapture.record(agent,"activity.timer",RepresentationCapture.data("step",id));
        Step step = current();
        if (step != null && step.name.equals(id)) {
            completionRequested = step;
            super.timedOut(id);
        } else if (currentPlan != null && id != null
                && ((currentPlan.currentStage == null && currentPlan.stages.containsKey(id))
                    || (currentPlan.currentStage != null && id.equals(currentPlan.currentStage.nextStage)))) {
            super.timedOut(id);
        }
        // Unique step names make delayed timers from a previous phase harmless.
    }
}
