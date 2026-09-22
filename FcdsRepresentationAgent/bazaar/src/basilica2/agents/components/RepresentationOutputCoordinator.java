package basilica2.agents.components;

import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.listeners.RepresentationCameraListener;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

/** Keep personal tutor routing out of the shared group chat. */
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
        return "Welcome, " + name + ". Your own Paper tutor link appears in your JupyterLab view "
            + "after chat connects. Open it on this laptop, then scan its QR code with your phone. "
            + "Keep workspace.ipynb open for the questions, coding, readiness commands, and submission.";
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
                    if (user != null) {
                        RepresentationCapture.record(owner,"activity.personal_tutor",RepresentationCapture.data(
                            "display_name",name,"participant_id",user));
                        message.setText(onboardingText(name, owner.getName().substring("OPEBot_".length()), user));
                    } else message.setText("Welcome, " + name + ". Your personal Paper tutor link is being prepared in your JupyterLab view.");
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
