package basilica2.agents.listeners.plan;

import basilica2.agents.components.*;
import basilica2.agents.data.State;
import basilica2.agents.events.*;
import basilica2.agents.listeners.*;
import edu.cmu.cs.lti.basilica2.core.Agent;
import org.json.*;

public class RepresentationHarness {
    static class TestInput extends InputCoordinator {
        TestInput(Agent a) { super(a, "inputCoordinator", ""); }
        public boolean isAgentName(String name) { return name.startsWith("OPEBot"); }
    }
    static class TestPresence extends RepresentationPresenceWatcher {
        TestPresence(Agent a) { super(a); }
        public void initiate(InputCoordinator input, State state) {}
    }
    static class CountingCamera extends RepresentationCameraListener {
        int handled;
        java.util.List<String> senders = new java.util.ArrayList<String>();
        CountingCamera(Agent a) { super(a); }
        public boolean messageFilter(MessageEvent event) {
            return !event.getText().matches("(paper|coding) (not )?done");
        }
        public void handleMessageEvent(InputCoordinator input, MessageEvent event) {
            handled++; senders.add(event.getFrom());
        }
    }
    static class TestAgent extends Agent {
        TestAgent(String name) {
            super(name);
            hasUI = false;
            addComponent(new StateMemory(this, "stateMemory", ""));
        }
        protected void createComponents() {}
        protected void connectComponents() {}
        public void receiveLoggerMessage(String message) {}
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static String step(RepresentationPlanExecutor plan) { return plan.currentPlan.getCurrentStep(); }
    static void say(RepresentationPlanExecutor plan, InputCoordinator input, String from, String text) {
        plan.processEvent(input, new MessageEvent(input, from, text));
    }
    static void pass(RepresentationPlanExecutor plan, InputCoordinator input, int task) {
        plan.processEvent(input, new FileEvent(input, "testcase-complete_" + task, FileEvent.fileEventType.created));
    }
    static void presence(TestPresence watcher, RepresentationPlanExecutor plan, InputCoordinator input,
            String name, String type) {
        PresenceEvent event = new PresenceEvent(input, name, type);
        watcher.preProcessEvent(input, event);
        if (plan != null) plan.processEvent(input, event);
    }
    static void deliver(CountingCamera camera, InputCoordinator input, MessageEvent event) {
        // Match InputCoordinator's assignable-class dispatch, including subclasses.
        for (Class<?> type : camera.getPreprocessorEventClasses())
            if (type.isInstance(event)) camera.preProcessEvent(input, event);
    }
    static RepresentationPlanExecutor plan(TestAgent agent, final InputCoordinator input, final boolean earlyPasses) {
        return plan(agent, input, earlyPasses, new String[0]);
    }
    static RepresentationPlanExecutor plan(TestAgent agent, final InputCoordinator input,
            final boolean earlyPasses, final String[] setupReady) {
        new java.io.File("planstatus/" + agent.getName() + ".planstatus.txt").delete();
        RepresentationPlanExecutor p = new RepresentationPlanExecutor(agent);
        p.source = input;
        for (Stage stage : p.currentPlan.stages.values()) {
            for (Step step : stage.steps) { step.timeout = 0; step.delay = 0; }
        }
        p.getHandlers("prompt").clear();
        p.addStepHandler("prompt", new StepHandler() {
            public void execute(Step s, PlanExecutor executor, InputCoordinator source) {
                if ("setup_instructions".equals(s.name)) {
                    for (String sender : setupReady) {
                        boolean withdraw = sender.startsWith("!");
                        say((RepresentationPlanExecutor) executor, input, withdraw ? sender.substring(1) : sender,
                            withdraw ? "paper not done" : "paper done");
                    }
                }
                if (earlyPasses && "paper_instructions".equals(s.name))
                    say((RepresentationPlanExecutor) executor, input, "Alice", "paper done");
                if (earlyPasses && "coding_instructions".equals(s.name)) {
                    pass((RepresentationPlanExecutor) executor, input, 2);
                    pass((RepresentationPlanExecutor) executor, input, 1);
                    say((RepresentationPlanExecutor) executor, input, "Alice", "coding done");
                    say((RepresentationPlanExecutor) executor, input, "Bob", "coding done");
                }
                executor.stepDone(s.name);
            }
        });
        p.getHandlers("logout").clear();
        p.addStepHandler("logout", new StepHandler() {
            public void execute(Step s, PlanExecutor executor, InputCoordinator source) {}
        });
        p.activateStage("Setup");
        return p;
    }
    static void tests() throws Exception {
        for (int size : new int[] {1, 2, 3, 4}) {
            TestAgent sized = new TestAgent("OPEBot_size00" + size);
            InputCoordinator sizedInput = new InputCoordinator(sized, "inputCoordinator", "");
            sized.addComponent(sizedInput);
            State sizedState = new State();
            for (int i = 1; i <= size; i++) {
                sizedState.addStudent("s" + i);
                sizedState.setName("s" + i, "Student " + i);
            }
            StateMemory.commitSharedState(sizedState, sized);
            RepresentationPlanExecutor sizedPlan = plan(sized, sizedInput, false);
            for (int i = 1; i <= size; i++) {
                check(step(sizedPlan).equals("paper_work"), "wait for every actual participant, size=" + size);
                say(sizedPlan, sizedInput, "Student " + i, "paper done");
            }
            check(step(sizedPlan).equals("coding_work"), "paper completes for size=" + size);
            pass(sizedPlan, sizedInput, 1);
            for (int i = 1; i <= size; i++) say(sizedPlan, sizedInput, "s" + i, "coding done");
            check(step(sizedPlan).equals("coding_work"), "both checks still required for size=" + size);
            pass(sizedPlan, sizedInput, 2);
            check(step(sizedPlan).equals("submission_acknowledgement"), "coding completes for size=" + size);
            say(sizedPlan, sizedInput, "Student 1", "submitted");
            check(step(sizedPlan).equals("logout"), "one submitter completes size=" + size);
        }
        TestAgent a = new TestAgent("OPEBot_test001");
        InputCoordinator input = new InputCoordinator(a, "inputCoordinator", "");
        a.addComponent(input);
        State state = new State();
        state.addStudent("1"); state.setName("1", "Alice");
        state.addStudent("2"); state.setName("2", "Bob");
        StateMemory.commitSharedState(state, a);
        RepresentationPlanExecutor p = plan(a, input, false);
        check(step(p).equals("paper_work"), "paper starts");
        p.stepDone();
        p.processEvent(input, new StepDoneEvent(input, "paper_work"));
        say(p, input, "Private_1", "paper done");
        say(p, input, "stranger", "paper done");
        p.processEvent(input, new PrivateMessageEvent(input, "1", "OPEBot", "paper done"));
        say(p, input, "Bob", "paper done");
        check(step(p).equals("paper_work"), "synthetic, private, unknown and generic completion ignored");
        say(p, input, "Bob", "paper not done");
        say(p, input, "Alice", "paper done");
        check(step(p).equals("paper_work"), "withdrawal removes readiness");
        say(p, input, "2", "paper done");
        check(step(p).equals("coding_work"), "names and IDs resolve to one participant");
        p.timedOut("paper_work");
        say(p, input, "Alice", "paper done");
        check(step(p).equals("coding_work"), "stale paper timer/message ignored");
        pass(p, input, 2); pass(p, input, 2);
        say(p, input, "Alice", "coding done"); say(p, input, "Bob", "coding done");
        check(step(p).equals("coding_work"), "both distinct tasks required");
        pass(p, input, 1);
        check(step(p).equals("submission_acknowledgement"), "out of order passes plus readiness finish coding");
        pass(p, input, 1); p.timedOut("coding_work");
        say(p, input, "Camera_1", "submitted");
        check(step(p).equals("submission_acknowledgement"), "duplicate pass and stale timeout cannot skip submission");
        say(p, input, "Alice", "submitted");
        check(step(p).equals("logout"), "one real submitter acknowledges");

        RepresentationPlanExecutor early = plan(a, input, true);
        say(early, input, "Bob", "paper done");
        check(step(early).equals("submission_acknowledgement"), "paper/coding readiness during visible announcements is retained");
        say(early, input, "Alice", "coding done"); say(early, input, "Bob", "coding done");
        check(step(early).equals("submission_acknowledgement"), "passes during intro are retained");
        RepresentationPlanExecutor timed = plan(a, input, false);
        timed.timedOut("paper_work"); timed.timedOut("coding_work");
        check(step(timed).equals("submission_acknowledgement"), "deadline permits unfinished submission");

        RepresentationHistoryListener history = new RepresentationHistoryListener(a);
        TestAgent b = new TestAgent("OPEBot_test002");
        RepresentationHistoryListener other = new RepresentationHistoryListener(b);
        history.saveMessageToHistory("Alice", "group", "ROOM_ONE_ONLY");
        other.saveMessageToHistory("Bob", "group", "ROOM_TWO_ONLY");
        check(!history.path.equals(other.path), "different room histories");
        check(!history.retrieveChatHistory(20, "public").toString().contains("ROOM_TWO_ONLY"), "no cross-room context");
        for (Class type : history.getListenerEventClasses()) input.addListener(type, history);
        RepresentationCameraListener camera = new RepresentationCameraListener(a);
        for (Class type : camera.getPreprocessorEventClasses()) input.addPreProcessor(type, camera);
        check(input.getPreProcessor("RepresentationCameraListener") == camera, "onboarding finds camera in the preprocessor registry");
        state = StateMemory.getSharedState(a);
        state.setStepInfo("Paper", "other", "paper_work", "representation_gate");
        state.setCurrentImage("1", "Zm9v"); state.setCurrentImageMimeType("1", "image/jpeg");
        StateMemory.commitSharedState(state, a);
        String paper = camera.constructPayloadMultiParty(input, "help", "Private_1");
        check(paper.contains("CURRENT PHASE: Paper") && paper.contains("image_url"), "paper has correct phase and image");
        org.json.JSONObject payload = new org.json.JSONObject(paper);
        check("json_object".equals(payload.getJSONObject("response_format").getString("type")), "legacy tutor parser requires JSON output");
        check(payload.getJSONArray("messages").getJSONObject(0).getString("content").contains("response_type and reply"), "legacy tutor fields specified");
        state = StateMemory.getSharedState(a);
        state.setStepInfo("Coding", "other", "coding_work", "representation_gate");
        StateMemory.commitSharedState(state, a);
        String coding = camera.constructPayloadMultiParty(input, "help", "Private_1");
        check(coding.contains("CURRENT PHASE: Coding") && !coding.contains("image_url"), "coding omits stale image");
        check(!camera.messageFilter(new MessageEvent(input, "Alice", "paper done")), "control phrases are not tutoring prompts");
        String welcome = RepresentationOutputCoordinator.onboardingText("Alice Smith", "fcdsrepresentationfcds-p2-26-fall-1a-room260911995", 1);
        check(welcome.contains("JupyterLab view") && welcome.contains("QR code"), "welcome directs students to their own Jupyter view");
        check(!welcome.contains("https://") && !welcome.contains("Private_") && !welcome.contains("#capture="),
            "shared welcome does not expose a student's personal link");

        TestAgent reconnect = new TestAgent("OPEBot_reconnect");
        TestInput reconnectInput = new TestInput(reconnect);
        reconnect.addComponent(reconnectInput);
        StateMemory.commitSharedState(new State(), reconnect);
        TestPresence watcher = new TestPresence(reconnect);
        for (String name : new String[] {"Alex", "Alexander", "Bob"})
            presence(watcher, null, reconnectInput, name, PresenceEvent.PRESENT);
        State exact = StateMemory.getSharedState(reconnect);
        check(exact.getStudentIdList().size() == 3 && exact.getStudentIdList().contains("Alexander"),
            "prefix names remain three distinct participants");
        RepresentationPlanExecutor reconnectPlan = plan(reconnect, reconnectInput, false);
        say(reconnectPlan, reconnectInput, "Alex", "paper done");
        presence(watcher, reconnectPlan, reconnectInput, "Alexander", PresenceEvent.ABSENT);
        presence(watcher, reconnectPlan, reconnectInput, "Alexander", PresenceEvent.PRESENT);
        check(StateMemory.getSharedState(reconnect).getStudentIdList().size() == 3,
            "reload restores the known student to present");
        say(reconnectPlan, reconnectInput, "Bob", "paper done");
        check(step(reconnectPlan).equals("paper_work"), "returning Alexander still has to become ready");
        say(reconnectPlan, reconnectInput, "Alexander", "paper done");
        check(step(reconnectPlan).equals("coding_work"), "all three exact names advance paper");
        pass(reconnectPlan, reconnectInput, 1); pass(reconnectPlan, reconnectInput, 2);
        say(reconnectPlan, reconnectInput, "Alex", "coding done");
        say(reconnectPlan, reconnectInput, "Alexander", "coding done");
        check(step(reconnectPlan).equals("coding_work"), "unready third participant holds coding");
        presence(watcher, reconnectPlan, reconnectInput, "Bob", PresenceEvent.ABSENT);
        check(step(reconnectPlan).equals("submission_acknowledgement"), "a departure reevaluates readiness of remaining students");
        presence(watcher, reconnectPlan, reconnectInput, "Bob", PresenceEvent.PRESENT);
        check(StateMemory.getSharedState(reconnect).getStudentIdList().size() == 3,
            "later return restores all three students");
        presence(watcher, reconnectPlan, reconnectInput, "Private_1", PresenceEvent.PRESENT);
        check(StateMemory.getSharedState(reconnect).getStudentIdList().size() == 3, "private pages never add a participant");

        CountingCamera concurrent = new CountingCamera(reconnect);
        deliver(concurrent, reconnectInput, new MessageEvent(reconnectInput, "Alex", "paper done"));
        deliver(concurrent, reconnectInput, new PrivateMessageEvent(reconnectInput, "OPEBot", "Private_1", "first question"));
        deliver(concurrent, reconnectInput, new PrivateMessageEvent(reconnectInput, "OPEBot", "Private_2", "second question"));
        deliver(concurrent, reconnectInput, new PrivateMessageEvent(reconnectInput, "OPEBot", "Private_3", "third question"));
        check(concurrent.handled == 3 && concurrent.senders.size() == 3,
            "adjacent private questions are each handled once, including after a readiness command");
        check(concurrent.senders.get(0).equals("Private_1") && concurrent.senders.get(2).equals("Private_3"),
            "rapid private questions retain each participant's identity");

        TestAgent earlyFour = new TestAgent("OPEBot_early_four");
        TestInput earlyInput = new TestInput(earlyFour);
        earlyFour.addComponent(earlyInput);
        State earlyState = new State();
        String[] fourNames = {"Test Student A", "Test Student B", "Test Student C", "Test Student D"};
        for (String name : fourNames) earlyState.addStudent(name);
        StateMemory.commitSharedState(earlyState, earlyFour);
        RepresentationPlanExecutor setupReady = plan(earlyFour, earlyInput, false, fourNames);
        check(step(setupReady).equals("coding_work"), "four paper done commands during Setup survive the phase transition");
        RepresentationPlanExecutor setupWithdrawal = plan(earlyFour, earlyInput, false,
            new String[] {fourNames[0], fourNames[1], fourNames[2], fourNames[3], "!" + fourNames[3]});
        check(step(setupWithdrawal).equals("paper_work"), "paper not done during Setup withdraws early readiness");
        say(setupWithdrawal, earlyInput, fourNames[3], "paper done");
        check(step(setupWithdrawal).equals("coding_work"), "fourth participant can restore readiness after setup");
        System.out.println("PASS: phase gates, timeouts, duplicates, early callbacks, histories, and camera phase context");
    }
    public static void main(String[] args) {
        try { tests(); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
