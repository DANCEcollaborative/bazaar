package basilica2.agents.listeners;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.RepresentationCapture;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

/** Exact student identities and reconnects, confined to this activity. */
public class RepresentationPresenceWatcher extends PresenceWatcher {
    private final Map<String, State.Student> participants = new LinkedHashMap<String, State.Student>();

    public RepresentationPresenceWatcher(Agent agent) { super(agent); }

    @Override public synchronized void preProcessEvent(InputCoordinator source, Event event) {
        if (!(event instanceof PresenceEvent)) return;
        PresenceEvent presence = (PresenceEvent) event;
        String name = presence.getUsername();
        if (name == null || name.isEmpty() || source.isAgentName(name)
                || name.startsWith("OPEBot") || name.startsWith("Private_")
                || name.startsWith("Camera_") || name.startsWith("tab_group")
                || "Group Chat".equals(name) || "group".equals(name)) return;
        boolean present = PresenceEvent.PRESENT.equals(presence.getType());
        if (!present && !PresenceEvent.ABSENT.equals(presence.getType())) return;

        State state = State.copy(StateMemory.getSharedState(agent));
        State.Student student = participants.get(name);
        boolean changed = student == null || student.isPresent != present;
        if (student == null) {
            if (!present) return;
            // State.addStudent matches prefixes. Insert with a unique temporary ID,
            // then use its public Student record to retain the exact transport name.
            // State.copy keeps these records, so presence updates survive later copies.
            String temporary = "fcds-student-" + UUID.randomUUID().toString();
            state.addStudent(temporary);
            student = state.getStudentById(temporary);
            student.chatId = name;
            student.name = name;
            participants.put(name, student);
        }
        student.isPresent = present;
        StateMemory.commitSharedState(state, agent);
        if (changed) RepresentationCapture.record(agent, "activity.presence",
            RepresentationCapture.data("participant", name, "present", present));
        if (present) initiate(source, state);
    }
}
