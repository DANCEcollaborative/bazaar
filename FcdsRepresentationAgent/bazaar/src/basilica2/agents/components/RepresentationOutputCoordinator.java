package basilica2.agents.components;

import java.net.URLEncoder;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.listeners.RepresentationCameraListener;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

/** Adapt the inherited camera welcome to one personal, clearly labelled entry point. */
public class RepresentationOutputCoordinator extends OutputCoordinator {
    private final Agent owner;
    private static final String LEGACY_WELCOME = "! Everyone should open the following URL";

    public RepresentationOutputCoordinator(Agent agent, String name, String properties) {
        super(agent, name, "OutputCoordinator.properties");
        owner = agent;
    }

    public static String onboardingText(String name, String room, int user) {
        if (!room.matches("fcdsrepresentationfcds-p2-26-fall-1a-room[0-9]+") || user < 1 || user > 4)
            throw new IllegalArgumentException("Unexpected representation room or participant");
        try {
            return name + " — open your Paper tutor on this laptop:\n"
                + "https://bree.lti.cs.cmu.edu/bazaar/chat/" + room + "/Private_" + user + "/Private_" + user
                + "/?html=representation-student-recorded&user=" + user + "&name=" + URLEncoder.encode(name, "UTF-8")
                + "#capture=" + RepresentationCapture.ticket(room,user)
                + "\nIt contains your private chat, paper preview, and a QR code to connect your phone. "
                + "Keep workspace.ipynb open for the questions, coding, readiness commands, and submission.";
        } catch (java.io.UnsupportedEncodingException error) {
            throw new IllegalStateException(error);
        }
    }

    @Override protected void publishEvent(Event event) {
        if (event instanceof MessageEvent && !(event instanceof PrivateMessageEvent)) {
            MessageEvent message = (MessageEvent) event;
            String text = message.getText();
            if ("OPEBot".equals(message.getFrom()) && text != null) {
                if (text.startsWith("With your smartphone, open URL")) return;
                int end = text.indexOf(LEGACY_WELCOME);
                if (text.startsWith("Welcome, ") && end > 9) {
                    String name = text.substring(9, end);
                    InputCoordinator input = (InputCoordinator) owner.getComponent("inputCoordinator");
                    RepresentationCameraListener camera = (RepresentationCameraListener) input.getPreProcessor("RepresentationCameraListener");
                    Integer user = camera.getUserNum(name);
                    if (user != null) message.setText(onboardingText(name, owner.getName().substring("OPEBot_".length()), user));
                }
            }
        }
        if (event instanceof MessageEvent) {
            MessageEvent message=(MessageEvent)event;
            RepresentationCapture.record(owner,"chat.output",RepresentationCapture.data("from",message.getFrom(),
                "to",event instanceof PrivateMessageEvent ? ((PrivateMessageEvent)event).getDestinationUser() : "public",
                "text",RepresentationCapture.redact(message.getText())));
        }
        super.publishEvent(event);
    }
}
