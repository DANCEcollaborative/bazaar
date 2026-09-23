package basilica2.agents.listeners.plan;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;
import org.json.JSONArray;
import basilica2.agents.components.RepresentationCapture;
import java.util.Locale;
import java.util.Set;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.StepDoneEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

/** Activity-local gates; the installed shared PlanExecutor is unchanged. */
public class RepresentationPlanExecutor extends PlanExecutor {
    private final Set<String> ready = new HashSet<String>();
    private Set<String> cohort;
    private String rosterProblem;
    private String readyCheckId;
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
        if (cohort == null) loadCohort();
        checkGate(true);
    }

    @Override synchronized void activateStage(String name) {
        ready.clear();
        readyCheckId = null;
        submitted = false;
        super.activateStage(name);
        RepresentationCapture.record(agent,"activity.phase",RepresentationCapture.data("phase",name));
    }

    @Override public Class[] getListenerEventClasses() {
        return new Class[] {LaunchEvent.class, StepDoneEvent.class, MessageEvent.class, FileEvent.class, PresenceEvent.class};
    }

    @Override public synchronized void processEvent(InputCoordinator input, Event event) {
        source = input;
        if(event instanceof FileEvent) RepresentationCapture.record(agent,"activity.checkpoint",RepresentationCapture.data("file",((FileEvent)event).getFileName()));
        Step step = current();
        String phase = currentPlan == null || currentPlan.currentStage == null ? ""
            : currentPlan.currentStage.name.toLowerCase(Locale.ROOT);
        if (event instanceof MessageEvent) handleControl((MessageEvent) event, phase);
        if (event instanceof FileEvent && "coding".equals(phase)) {
            String file = ((FileEvent) event).getFileName();
            if ("testcase-complete_1".equals(file) || "testcase-checking_1".equals(file) || "testcase-failed_1".equals(file)) {
                try {
                    JSONObject latest = latestCheck();
                    String checkId = latest.optString("check_id");
                    String state = latest.optString("state");
                    if (!"passed".equals(state) || !checkId.equals(readyCheckId)) {
                        ready.clear();
                        readyCheckId = "passed".equals(state) ? checkId : null;
                    }
                    announce(null, "passed".equals(state) && !checkId.isEmpty() ?
                        "The latest check passed. Each person can type coding done when ready." :
                        "checking".equals(state) ? "Checking the saved notebook. Please wait for the result." :
                        "The latest check did not pass. Update the code, run the check again, then each person types coding done.");
                } catch (Exception error) {
                    announce(null, "I could not verify the latest check just now. Check the notebook result, then type coding done to retry.");
                }
            }
        }
        if (event instanceof LaunchEvent) {
            super.processEvent(input, event);
        } else if (event instanceof StepDoneEvent) {
            // A stale/generic completion must never bypass a readiness gate.
            if (step != null && step != gate)
                stepDone(((StepDoneEvent) event).getStepName());
        } else checkGate(event instanceof MessageEvent || event instanceof FileEvent);
    }

    private void checkGate(boolean verifyCoding) {
        Step step = current();
        if (step == null || step != gate || step == completionRequested) return;
        String phase = step.attributes.get("phase");
        if ("submit".equals(phase)) {
            if (submitted) completeCurrent(true);
            return;
        }
        if (!allParticipantsReady()) return;
        if (!"coding".equals(phase)) { completeCurrent(true); return; }
        // FileEvents are delivery hints, not proof: the gRPC relay can lose or
        // delay a callback. Read the durable latest check before advancing.
        if (!verifyCoding) return;
        try {
            JSONObject latest = latestCheck();
            String checkId = latest.optString("check_id");
            if ("passed".equals(latest.optString("state")) && !checkId.isEmpty() && checkId.equals(readyCheckId)) {
                completeCurrent(true);
            } else {
                ready.clear();
                readyCheckId = null;
                announce(null, "The latest check changed. After it passes, each person needs to type coding done again.");
            }
        } catch (Exception error) {
            announce(null, "I could not verify the latest check just now. Please type coding done again in a moment.");
        }
    }

    private JSONObject latestCheck() throws Exception {
        JSONObject status = fetchRoomStatus();
        if (!validRoomStatus(status)) throw new IllegalStateException("Room mismatch");
        JSONObject latest = status.optJSONObject("latest_check");
        return latest == null ? new JSONObject() : latest;
    }

    public static String controlCommand(String text) {
        if (text == null) return null;
        String normalized = text.trim().toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}]+$", "")
            .trim().replaceAll("\\s+", " ");
        return normalized.matches("ready|not ready|(?:paper|coding) (?:not )?done|submitted") ? normalized : null;
    }

    private void handleControl(MessageEvent message, String phase) {
        String command = controlCommand(message.getText());
        if (command == null) return;
        String sender = message.getFrom();
        if (message instanceof PrivateMessageEvent || (sender != null &&
                (sender.startsWith("Private_") || sender.startsWith("Camera_")))) {
            String destination = sender;
            if ("OPEBot".equals(sender) && message instanceof PrivateMessageEvent)
                destination = ((PrivateMessageEvent) message).getDestinationUser();
            announce(destination, "Type " + command + " in the JupyterLab group chat, beside the notebook.");
            return;
        }
        String name = participant(sender);
        if (name == null) return;
        if (cohort == null && !loadCohort()) {
            announce(null, rosterProblem != null ? rosterProblem : "I could not confirm the group roster. Please try " + command + " again in a moment.");
            return;
        }
        if (!cohort.contains(name)) return;
        String expected = "setup".equals(phase) ? "ready" :
            "paper".equals(phase) ? "paper done" : "coding".equals(phase) ? "coding done" :
            "submit".equals(phase) ? "submitted" : null;
        boolean withdraw = "not ready".equals(command) || command.contains(" not done");
        String positive = command.replace("not ready", "ready").replace(" not done", " done");
        if (!positive.equals(expected)) {
            announce(null, expected == null ? "This activity has finished." :
                "We are in " + phase + ". " + ("submitted".equals(expected) ?
                "Run the submission cell, then type submitted." : "Type " + expected + " here when ready."));
            return;
        }
        if ("submitted".equals(command)) {
            try {
                JSONObject status = fetchRoomStatus();
                if (!validRoomStatus(status)) throw new IllegalStateException("Room mismatch");
                long receipt = status.optLong("submission_id", 0);
                if (status.optBoolean("stored", false) && receipt > 0) {
                    submitted = true;
                    announce(null, "Submission " + receipt + " is stored. The activity is complete.");
                } else announce(null, "No stored submission was found for this room yet. Run the submission cell, wait for its submission ID, then type submitted again.");
            } catch (Exception error) {
                announce(null, "I could not verify the submission just now. Keep your submission ID and type submitted again in a moment.");
            }
            return;
        }
        if ("coding".equals(phase) && !withdraw) {
            try {
                JSONObject latest = latestCheck();
                String checkId = latest.optString("check_id");
                if (!"passed".equals(latest.optString("state")) || checkId.isEmpty()) {
                    ready.clear(); readyCheckId = null;
                    announce(null, "checking".equals(latest.optString("state")) ?
                        "The latest check is still running. Wait for it to pass, then type coding done." :
                        "Run the check cell first. After it passes, each person types coding done.");
                    return;
                }
                if (!checkId.equals(readyCheckId)) {
                    boolean hadReady = !ready.isEmpty();
                    ready.clear(); readyCheckId = checkId;
                    if (hadReady) announce(null, "A new check has passed. Everyone needs to confirm coding done again for this result.");
                }
            } catch (Exception error) {
                announce(null, "I could not verify the latest check just now. Please type coding done again in a moment.");
                return;
            }
        }
        if (withdraw) ready.remove(name); else ready.add(name);
        announce(null, name + (withdraw ? " is not ready. " : " is ready. ") + ready.size() + "/" + cohort.size() + " ready.");
    }

    /** Overridden by the offline harness; production reads the activity server's stored receipt. */
    protected JSONObject fetchRoomStatus() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
            "https://bree.lti.cs.cmu.edu/api/activity/fcds-p2-26-fall-1a/rooms/" + roomName() + "/submission-status").openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setUseCaches(false);
        try {
            if (connection.getResponseCode() != 200) throw new java.io.IOException("Room status unavailable");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream body = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024]; int count;
                while ((count = input.read(buffer)) != -1) {
                    body.write(buffer, 0, count);
                    if (body.size() > 16384) throw new java.io.IOException("Room status too large");
                }
                return new JSONObject(new String(body.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } finally { connection.disconnect(); }
    }

    protected String roomName() {
        Matcher match = Pattern.compile("room[0-9]+$").matcher(agent.getName());
        if (!match.find()) throw new IllegalStateException("Invalid room name");
        return match.group();
    }

    private boolean validRoomStatus(JSONObject status) {
        return roomName().equals(status.optString("room_name"));
    }

    private boolean loadCohort() {
        rosterProblem = null;
        try {
            JSONObject status = fetchRoomStatus();
            if (!validRoomStatus(status)) return false;
            JSONArray participants = status.getJSONArray("participants");
            Set<String> names = new LinkedHashSet<String>();
            for (int i = 0; i < participants.length(); i++) {
                String name = participants.getJSONObject(i).getString("name");
                if (name.trim().isEmpty()) return false;
                if (!names.add(name)) {
                    rosterProblem = "Two people in this group have the same display name, so I cannot distinguish their readiness. Please contact the instructor to use different display names or separate rooms.";
                    return false;
                }
            }
            if (names.isEmpty() || names.size() > 3) return false;
            cohort = names;
            return true;
        } catch (Exception error) { return false; }
    }

    protected void announce(String destination, String text) {
        if (source == null) return;
        MessageEvent response = destination == null ? new MessageEvent(source, "OPEBot", text) :
            new PrivateMessageEvent(source, destination, "OPEBot", text);
        source.pushEventProposal(response);
    }

    private String participant(String sender) {
        if (sender == null || sender.startsWith("Private_") || sender.startsWith("Camera_")
                || sender.startsWith("tab_group") || sender.startsWith("OPEBot")) return null;
        State state = StateMemory.getSharedState(agent);
        for (String id : state.getStudentIdsPresentOrNot()) {
            String name = state.getStudentName(id);
            if (sender.equals(id) || sender.equals(name)) return name;
        }
        return null;
    }

    private boolean allParticipantsReady() {
        return cohort != null && !cohort.isEmpty() && ready.containsAll(cohort);
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
            if ("setup".equals(step.attributes.get("phase"))) return;
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
