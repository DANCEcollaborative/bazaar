package basilica2.agents.listeners;

import java.util.Locale;
import java.util.UUID;
import basilica2.agents.components.RepresentationCapture;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.events.ImageEvent;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reuses camera/private-chat transport with activity and phase-specific context. */
public class RepresentationCameraListener extends LlmCameraListener {
    public RepresentationCameraListener(Agent agent) { super(agent); }

    @Override public Class[] getPreprocessorEventClasses() {
        // PrivateMessageEvent already extends MessageEvent. Registering both makes
        // InputCoordinator deliver each private question twice.
        return new Class[] {MessageEvent.class, ImageEvent.class, PresenceEvent.class};
    }

    private String stage() {
        String value = StateMemory.getSharedState(agent).getStageName();
        return value == null ? "Setup" : value;
    }

    @Override public boolean messageFilter(MessageEvent event) {
        String text = event.getText() == null ? "" : event.getText().trim().toLowerCase(Locale.ROOT);
        if (text.matches("(paper|coding) (not )?done") || text.equals("submitted")) return false;
        return super.messageFilter(event);
    }

    @Override public void preProcessEvent(InputCoordinator source, Event event) {
        if(event instanceof MessageEvent) {
            MessageEvent m=(MessageEvent)event;
            RepresentationCapture.record(agent,"chat.input",RepresentationCapture.data("from",m.getFrom(),
                "to",event instanceof PrivateMessageEvent ? ((PrivateMessageEvent)event).getDestinationUser() : "public",
                "text",RepresentationCapture.redact(m.getText())));
            // The inherited preprocessor drops messages within 1.5 seconds of any
            // student's previous message, including readiness commands. Distinct
            // private questions must each reach the tutor, even when sent together.
            if (m.getFrom() != null && m.getText() != null && messageFilter(m)) {
                try { handleMessageEvent(source, m); }
                catch (JSONException error) { throw new IllegalStateException("Could not process tutor message", error); }
            }
            return;
        }
        super.preProcessEvent(source,event);
    }

    @Override public String sendToOpenAI(InputCoordinator source,String payload,Boolean fromSystem) {
        String request=UUID.randomUUID().toString();long started=System.currentTimeMillis();
        try {
            RepresentationCapture.record(agent,"tutor.request",RepresentationCapture.data("request_id",request,"phase",stage(),"body",new JSONObject(payload)));
            String result=super.sendToOpenAI(source,payload,fromSystem);
            RepresentationCapture.record(agent,"tutor.response",RepresentationCapture.data("request_id",request,"duration_ms",System.currentTimeMillis()-started,"parsed_reply",result));
            return result;
        } catch(Exception e) {
            RepresentationCapture.record(agent,"tutor.error",RepresentationCapture.data("request_id",request,"error_type",e.getClass().getSimpleName()));
            throw new IllegalStateException(e);
        }
    }

    @Override public boolean almostIdentical(String first,String second) throws java.io.IOException {
        boolean same=super.almostIdentical(first,second);
        RepresentationCapture.record(agent,"camera.similarity",RepresentationCapture.data("first_sha256",RepresentationCapture.hashImage(first),"second_sha256",RepresentationCapture.hashImage(second),"filtered",same));
        return same;
    }

    @Override public void handleImageEvent(InputCoordinator source, ImageEvent event) throws JSONException {
        RepresentationCapture.record(agent,"camera.agent_received",RepresentationCapture.data("sender",event.getSenderUsername(),"sha256",RepresentationCapture.hashImage(event.getImageBase64()),"relay_frame_count",event.getFrameCount(),"phase",stage()));
        if ("Paper".equals(stage()) || "Setup".equals(stage())) super.handleImageEvent(source, event);
        else RepresentationCapture.record(agent,"camera.ignored_phase",RepresentationCapture.data("phase",stage(),"sha256",RepresentationCapture.hashImage(event.getImageBase64())));
    }

    @Override public String getAllMessages(InputCoordinator source, String prompt, String sender) {
        StringBuilder result = new StringBuilder("Conversation in this room:\n");
        String target = sender.startsWith("Private_") ? sender : "public";
        ChatMultiHistoryListener history = (ChatMultiHistoryListener) source.getListenerByName("RepresentationHistoryListener");
        if (history == null) throw new IllegalStateException("Room history listener is missing");
        try {
            int count = Integer.parseInt(getProperties().getProperty("openai.context.length", "20"));
            JSONArray messages = history.retrieveChatHistory(count, target);
            for (int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.getJSONObject(i);
                result.append("sender:").append(message.optString("sender"))
                    .append(" receiver:").append(message.optString("receiver"))
                    .append(" content:").append(message.optString("content")).append('\n');
            }
        } catch (JSONException error) {
            throw new IllegalStateException("Could not read room history", error);
        }
        if (prompt != null) result.append(sender).append(": ").append(prompt);
        return result.toString();
    }

    @Override public String constructPayloadMultiParty(InputCoordinator source, String prompt, String sender) {
        try {
            JSONObject payload = new JSONObject(super.constructPayloadMultiParty(source, prompt, sender));
            String phase = stage();
            JSONArray messages = payload.getJSONArray("messages");
            JSONObject system = messages.getJSONObject(0);
            payload.put("response_format", new JSONObject().put("type", "json_object"));
            system.put("content", system.getString("content")
                + "\nReturn only a JSON object with string fields response_type and reply. "
                + "Use response_type=\"Reply\" and put your student-facing hint in reply. "
                + "Use response_type=\"No response\" and reply=\"\" only when no helpful intervention is needed. "
                + "Always respond to an explicit student question. Do not wrap the JSON in Markdown."
                + "\nCURRENT PHASE: " + phase + ". "
                + (("Paper".equals(phase) || "Setup".equals(phase))
                    ? "Students reason on paper. Ask about quantities, matrix shapes, axes, ties, and running averages. Do not give NumPy code or a complete solution. Use only clearly visible paper details."
                    : "Students are coding or submitting. Discuss the code or question they share. Do not infer their current code from an earlier paper image. Do not claim tests passed or submission succeeded without the platform receipt."));
            if (!"Paper".equals(phase) && !"Setup".equals(phase)) {
                for (int i = 1; i < messages.length(); i++) {
                    JSONObject message = messages.getJSONObject(i);
                    Object content = message.get("content");
                    if (content instanceof JSONArray) {
                        JSONArray clean = new JSONArray();
                        JSONArray parts = (JSONArray) content;
                        for (int j = 0; j < parts.length(); j++) {
                            JSONObject part = parts.getJSONObject(j);
                            if (!"image_url".equals(part.optString("type"))) clean.put(part);
                        }
                        message.put("content", clean);
                    }
                }
            }
            return payload.toString();
        } catch (JSONException error) {
            throw new IllegalStateException("Could not build representation tutor request", error);
        }
    }
}
