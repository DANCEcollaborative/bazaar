package basilica2.agents.listeners.plan;

import basilica2.agents.components.*;
import basilica2.agents.data.State;
import basilica2.agents.events.*;
import basilica2.agents.listeners.*;
import edu.cmu.cs.lti.basilica2.core.Agent;
import org.json.*;

public class RepresentationHarness {
    static class TestInput extends InputCoordinator {
        java.util.List<edu.cmu.cs.lti.basilica2.core.Event> proposals = new java.util.ArrayList<edu.cmu.cs.lti.basilica2.core.Event>();
        TestInput(Agent a) { super(a, "inputCoordinator", ""); }
        public void pushEventProposal(edu.cmu.cs.lti.basilica2.core.Event event) { proposals.add(event); }
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
        if (task == 1 && plan instanceof TestPlan) ((TestPlan) plan).checkState = "passed";
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
    static class TestPlan extends RepresentationPlanExecutor {
        long receipt;
        String checkState = "failed";
        String checkId = "check-1";
        boolean unavailable;
        String[] roster;
        String responseRoom = "room260999001";
        java.util.List<String> notices = new java.util.ArrayList<String>();
        TestPlan(Agent agent) { super(agent); }
        protected String roomName() { return "room260999001"; }
        protected JSONObject fetchRoomStatus() throws Exception {
            if (unavailable) throw new java.io.IOException("offline");
            JSONArray participants = new JSONArray();
            String[] names = roster == null ? StateMemory.getSharedState(agent).getStudentIdsPresentOrNot() : roster;
            for (String id : names) {
                String name = roster == null ? StateMemory.getSharedState(agent).getStudentName(id) : id;
                participants.put(new JSONObject().put("name", name));
            }
            return new JSONObject().put("room_name", responseRoom).put("participants", participants)
                .put("stored", receipt > 0).put("submission_id", receipt)
                .put("latest_check", new JSONObject().put("check_id", checkId).put("state", checkState));
        }
        protected void announce(String destination, String text) { notices.add((destination == null ? "group" : destination) + ":" + text); }
    }
    static TestPlan plan(TestAgent agent, final InputCoordinator input, final boolean earlyPasses) {
        return plan(agent, input, earlyPasses, true, null);
    }
    static TestPlan plan(TestAgent agent, final InputCoordinator input,
            final boolean earlyPasses, final boolean autoReady, String[] roster) {
        new java.io.File("planstatus/" + agent.getName() + ".planstatus.txt").delete();
        TestPlan p = new TestPlan(agent);
        p.roster = roster;
        p.source = input;
        for (Stage stage : p.currentPlan.stages.values()) {
            for (Step step : stage.steps) { step.timeout = 0; step.delay = 0; }
        }
        p.getHandlers("prompt").clear();
        p.addStepHandler("prompt", new StepHandler() {
            public void execute(Step s, PlanExecutor executor, InputCoordinator source) {
                if (autoReady && "setup_instructions".equals(s.name)) {
                    for (String id : StateMemory.getSharedState(agent).getStudentIdsPresentOrNot())
                        say((RepresentationPlanExecutor) executor, input, id, "ready");
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
        for (int size : new int[] {1, 2, 3}) {
            TestAgent sized = new TestAgent("OPEBot_size00" + size);
            InputCoordinator sizedInput = new InputCoordinator(sized, "inputCoordinator", "");
            sized.addComponent(sizedInput);
            State sizedState = new State();
            for (int i = 1; i <= size; i++) {
                sizedState.addStudent("s" + i);
                sizedState.setName("s" + i, "Student " + i);
            }
            StateMemory.commitSharedState(sizedState, sized);
            TestPlan sizedPlan = plan(sized, sizedInput, false);
            for (int i = 1; i <= size; i++) {
                check(step(sizedPlan).equals("paper_work"), "wait for every actual participant, size=" + size);
                say(sizedPlan, sizedInput, "Student " + i, "paper done");
            }
            check(step(sizedPlan).equals("coding_work"), "paper completes for size=" + size);
            pass(sizedPlan, sizedInput, 2);
            for (int i = 1; i <= size; i++) say(sizedPlan, sizedInput, "s" + i, "coding done");
            check(step(sizedPlan).equals("coding_work"), "unrelated task cannot satisfy the recovery check for size=" + size);
            pass(sizedPlan, sizedInput, 1);
            for (int i = 1; i <= size; i++) say(sizedPlan, sizedInput, "s" + i, "coding done");
            check(step(sizedPlan).equals("submission_acknowledgement"), "coding completes for size=" + size);
            say(sizedPlan, sizedInput, "Student 1", "submitted");
            check(step(sizedPlan).equals("submission_acknowledgement"), "typed submitted cannot fake a stored receipt");
            sizedPlan.receipt = 42;
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
        TestPlan p = plan(a, input, false);
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
        check(step(p).equals("coding_work"), "unrelated duplicate task callbacks ignored");
        pass(p, input, 1);
        say(p, input, "Alice", "coding done"); say(p, input, "Bob", "coding done");
        check(step(p).equals("submission_acknowledgement"), "one recovery pass plus readiness finishes coding");
        pass(p, input, 1); p.timedOut("coding_work");
        say(p, input, "Camera_1", "submitted");
        check(step(p).equals("submission_acknowledgement"), "duplicate pass and stale timeout cannot skip submission");
        p.receipt = 99;
        p.responseRoom = "room260999002";
        say(p, input, "Alice", "submitted");
        check(step(p).equals("submission_acknowledgement"), "receipt from a different room is rejected");
        p.responseRoom = "room260999001";
        p.unavailable = true;
        say(p, input, "Alice", "submitted");
        check(step(p).equals("submission_acknowledgement"), "receipt lookup fails closed");
        p.unavailable = false;
        say(p, input, "Alice", "Submitted!");
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
        check(welcome.contains("Start here") && welcome.contains("Open paper tutor"), "welcome directs students to their own Jupyter view");
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
        pass(reconnectPlan, reconnectInput, 1);
        say(reconnectPlan, reconnectInput, "Alex", "coding done");
        say(reconnectPlan, reconnectInput, "Alexander", "coding done");
        check(step(reconnectPlan).equals("coding_work"), "unready third participant holds coding");
        presence(watcher, reconnectPlan, reconnectInput, "Bob", PresenceEvent.ABSENT);
        check(step(reconnectPlan).equals("coding_work"), "an unready student's departure must not advance the group");
        presence(watcher, reconnectPlan, reconnectInput, "Bob", PresenceEvent.PRESENT);
        check(StateMemory.getSharedState(reconnect).getStudentIdList().size() == 3,
            "later return restores all three students");
        say(reconnectPlan, reconnectInput, "Bob", "coding done");
        check(step(reconnectPlan).equals("submission_acknowledgement"), "reconnected student can explicitly advance the group");
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

        State setupState = new State();
        setupState.addStudent("1"); setupState.setName("1", "Alice");
        setupState.addStudent("2"); setupState.setName("2", "Bob");
        StateMemory.commitSharedState(setupState, a);
        TestPlan setup = plan(a, input, false, false, null);
        check(step(setup).equals("setup_ready"), "paper clock does not start during setup");
        setup.timedOut("setup_ready");
        check(step(setup).equals("setup_ready"), "setup cannot time out");
        say(setup, input, "Alice", "paper done");
        check(step(setup).equals("setup_ready") && setup.notices.get(setup.notices.size()-1).contains("Type ready"),
            "wrong phase command explains current action");
        setup.processEvent(input, new PrivateMessageEvent(input, "OPEBot", "Private_1", " READY! "));
        check(setup.notices.get(setup.notices.size()-1).startsWith("Private_1:Type ready in"),
            "private controls get a private group-chat redirect");
        say(setup, input, "Alice", " READY! ");
        say(setup, input, "Alice", "ready");
        check(setup.notices.get(setup.notices.size()-1).contains("1/2 ready"), "duplicate readiness counts once");
        say(setup, input, "Alice", "not ready.");
        say(setup, input, "Bob", "ready");
        check(step(setup).equals("setup_ready"), "setup withdrawal respected");
        say(setup, input, "Alice", "ready");
        check(step(setup).equals("paper_work"), "all ready starts paper");
        say(setup, input, "Alice", " Paper   done! ");
        say(setup, input, "Bob", "paper done.");
        pass(setup, input, 1);
        say(setup, input, "Alice", "coding done");
        setup.checkState = "checking";
        setup.processEvent(input, new FileEvent(input, "testcase-checking_1", FileEvent.fileEventType.created));
        check(setup.notices.get(setup.notices.size()-1).contains("Checking the saved notebook"), "new check reports checking, never a false failure");
        say(setup, input, "Bob", "coding done");
        check(step(setup).equals("coding_work"), "old passing result cannot advance while the new check is in progress");
        setup.checkState = "failed";
        setup.processEvent(input, new FileEvent(input, "testcase-failed_1", FileEvent.fileEventType.created));
        say(setup, input, "Bob", "coding done");
        say(setup, input, "Alice", "coding done");
        check(step(setup).equals("coding_work"), "later failed check invalidates the earlier passing check");
        setup.processEvent(input, new FileEvent(input, "testcase-complete_1", FileEvent.fileEventType.created));
        check(step(setup).equals("coding_work"), "success callback cannot override authoritative failed check");
        setup.checkState = "passed";
        setup.checkId = "check-2";
        setup.unavailable = true;
        say(setup, input, "Alice", "coding done");
        check(step(setup).equals("coding_work"), "check-status outage fails closed");
        setup.unavailable = false;
        say(setup, input, "Alice", "coding done");
        check(step(setup).equals("coding_work"), "commands before a passing check do not count as readiness");
        // No FileEvent at all for this new check: Alice's old readiness must not survive.
        setup.checkState = "passed"; setup.checkId = "check-3";
        say(setup, input, "Bob", "coding done");
        check(step(setup).equals("coding_work"), "new authoritative check ID invalidates readiness even if checking callback was lost");
        check(setup.notices.toString().contains("Everyone needs to confirm"), "new-check readiness reset is explained");
        say(setup, input, "Alice", "coding done");
        check(step(setup).equals("submission_acknowledgement"), "authoritative pass advances after all confirm even when success FileEvent was lost");
        for (String command : new String[] {" READY! ", "Paper   done.", "coding not done?", "Submitted!"})
            check(!camera.messageFilter(new MessageEvent(input, "Alice", command)), "normalized control never reaches tutor: " + command);

        TestAgent staggered = new TestAgent("OPEBot_staggered");
        TestInput staggeredInput = new TestInput(staggered); staggered.addComponent(staggeredInput);
        State staggeredState = new State(); staggeredState.addStudent("Alice");
        StateMemory.commitSharedState(staggeredState, staggered);
        TestPlan staggeredPlan = plan(staggered, staggeredInput, false, false, new String[] {"Alice", "Bob"});
        say(staggeredPlan, staggeredInput, "Alice", "ready");
        check(step(staggeredPlan).equals("setup_ready"), "assigned partner must connect and ready even before first presence");
        TestPresence staggeredWatcher = new TestPresence(staggered);
        presence(staggeredWatcher, staggeredPlan, staggeredInput, "Bob", PresenceEvent.PRESENT);
        say(staggeredPlan, staggeredInput, "Bob", "ready");
        check(step(staggeredPlan).equals("paper_work"), "assigned partner can join and start the group");
        TestPlan missingRoster = plan(staggered, staggeredInput, false, false, new String[0]);
        say(missingRoster, staggeredInput, "Alice", "ready");
        check(step(missingRoster).equals("setup_ready"), "missing roster never silently becomes a solo room");
        missingRoster.roster = new String[] {"Alice", "Alice"};
        say(missingRoster, staggeredInput, "Alice", "ready");
        check(step(missingRoster).equals("setup_ready") && missingRoster.notices.get(missingRoster.notices.size()-1).contains("same display name"),
            "duplicate display names get an actionable explanation rather than endless retry advice");
        missingRoster.roster = new String[] {"Alice"};
        say(missingRoster, staggeredInput, "Alice", "ready");
        check(step(missingRoster).equals("paper_work"), "roster lookup can recover on the next readiness attempt");
        State photoState = StateMemory.getSharedState(staggered);
        photoState.setStepInfo("Coding", "other", "coding_work", "representation_gate");
        StateMemory.commitSharedState(photoState, staggered);
        int beforePhotos = staggeredInput.proposals.size();
        camera.handleImageEvent(staggeredInput, new ImageEvent(staggeredInput, "Camera_1", "Zm9v", "image/jpeg", 1, 1, "manual:test", 1));
        check(staggeredInput.proposals.size() == beforePhotos + 2, "late manual photo explicitly reports closed paper feedback");
        for (int i = beforePhotos; i < staggeredInput.proposals.size(); i++) {
            check(staggeredInput.proposals.get(i) instanceof PrivateMessageEvent, "late photo response stays private");
            check(((MessageEvent) staggeredInput.proposals.get(i)).getText().contains("after the paper phase"), "late photo notice states why there is no tutor feedback");
        }
        camera.handleImageEvent(staggeredInput, new ImageEvent(staggeredInput, "Camera_1", "Zm9v", "image/jpeg", 1, 1, "continuous", 2));
        check(staggeredInput.proposals.size() == beforePhotos + 2, "late continuous frames do not spam notices");
        System.out.println("PASS: phase gates, timeouts, duplicates, early callbacks, histories, and camera phase context");
    }
    public static void main(String[] args) {
        try { tests(); System.exit(0); }
        catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
