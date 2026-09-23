package basilica2.agents.listeners;

import basilica2.agents.listeners.plan.RepresentationPlanExecutor;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import basilica2.agents.data.State;
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
    private static final long BACKGROUND_COOLDOWN_MS = 120000L;
    private static class FeedbackState {
        long assessedAt = -1;
        String assessedImage, assessedMime;
        boolean assessedUseful;
        final List<String> recentReplies = new ArrayList<String>();
    }
    private static class Observation {
        final String user, previousImage, previousMime;
        final long previousAssessedAt;
        final boolean previousUseful;
        boolean requested;
        final List<String> recentReplies;
        Observation(String user, FeedbackState previous) {
            this.user=user;this.previousImage=previous.assessedImage;this.previousMime=previous.assessedMime;
            this.previousAssessedAt=previous.assessedAt;this.previousUseful=previous.assessedUseful;
            this.recentReplies=new ArrayList<String>(previous.recentReplies);
        }
    }
    private final Map<String, FeedbackState> feedback = new HashMap<String, FeedbackState>();
    private final ThreadLocal<Observation> observation = new ThreadLocal<Observation>();
    private final ThreadLocal<String> requestParticipant = new ThreadLocal<String>();
    protected long feedbackNowMillis() { return System.nanoTime() / 1000000L; }
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
        if (RepresentationPlanExecutor.controlCommand(event.getText()) != null) return false;
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
            if (observation.get() != null) observation.get().requested=true;
            String result=requestModel(source,payload,fromSystem);
            String modelResult=result;
            Observation current=observation.get();
            String user=requestParticipant.get();
            if (current != null && !"Paper".equals(stage())) result="No response";
            if (current != null && !"No response".equals(result)) {
                boolean quiet=backgroundReadabilityNag(result);
                synchronized(feedback) {
                    FeedbackState state=feedback.get(current.user);
                    for (String previous:state.recentReplies)
                        if (normalizedReply(previous).equals(normalizedReply(result))) quiet=true;
                }
                if (quiet) {
                    RepresentationCapture.record(agent,"tutor.background_suppressed",RepresentationCapture.data("participant_id",current.user,"reason","repeated_or_readability_only"));
                    result="No response";
                }
            }
            if (current != null) synchronized(feedback) {
                // The inherited transport uses the same sentinel for intentional
                // silence and a failed request. Quiet/failed observations may be
                // retried after cooldown; never cache them as final assessments.
                feedback.get(current.user).assessedUseful=result != null && !result.trim().isEmpty() && !"No response".equals(result);
            }
            if (user != null && result != null && !result.trim().isEmpty() && !"No response".equals(result)) {
                synchronized(feedback) {
                    FeedbackState state=feedback.get(user);
                    if (state == null) { state=new FeedbackState();feedback.put(user,state); }
                    state.recentReplies.add(result);
                    if (state.recentReplies.size()>4) state.recentReplies.remove(0);
                }
            }
            RepresentationCapture.record(agent,"tutor.response",RepresentationCapture.data("request_id",request,"duration_ms",System.currentTimeMillis()-started,"parsed_reply",modelResult,"delivered_reply",result));
            return result;
        } catch(Exception e) {
            RepresentationCapture.record(agent,"tutor.error",RepresentationCapture.data("request_id",request,"error_type",e.getClass().getSimpleName()));
            throw new IllegalStateException(e);
        }
    }

    /** Injection point for offline tests; production keeps the existing API transport. */
    protected String requestModel(InputCoordinator source,String payload,Boolean fromSystem) {
        return super.sendToOpenAI(source,payload,fromSystem);
    }

    @Override public void openAIrequestAndResponse(InputCoordinator source,String prompt,Boolean fromSystem,String sender) {
        String user = sender != null && sender.matches("(?:Private_|Camera_)[1-3]") ? sender.substring(sender.indexOf('_')+1) : null;
        requestParticipant.set(user);
        try { super.openAIrequestAndResponse(source,prompt,fromSystem,sender); }
        finally { requestParticipant.remove(); }
    }

    private static String normalizedReply(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }
    public static boolean backgroundReadabilityNag(String text) {
        String value=normalizedReply(text);
        return value.isEmpty() || value.matches(".*(?:no readable|not readable|can t read|cannot read|can t see|cannot see|unreadable|clearer photo|clearer image|blank page|blank paper|unable to read|don t see|do not see|no (?:visible|written|handwritten|legible)|only see the (?:activity|problem|prompt)).*");
    }

    @Override public boolean almostIdentical(String first,String second) throws java.io.IOException {
        boolean same=first.equals(second) || super.almostIdentical(first,second);
        RepresentationCapture.record(agent,"camera.similarity",RepresentationCapture.data("first_sha256",RepresentationCapture.hashImage(first),"second_sha256",RepresentationCapture.hashImage(second),"filtered",same));
        return same;
    }

    @Override public void handleImageEvent(InputCoordinator source, ImageEvent event) throws JSONException {
        boolean manual=event.getProblemId() != null && event.getProblemId().startsWith("manual:");
        String sender=event.getSenderUsername();
        RepresentationCapture.record(agent,"camera.agent_received",RepresentationCapture.data("sender",sender,"sha256",RepresentationCapture.hashImage(event.getImageBase64()),"relay_frame_count",event.getFrameCount(),"phase",stage(),"manual_photo",manual));
        if (sender == null || !sender.matches("Camera_[1-3]")) return;
        String user=sender.substring("Camera_".length());
        // Showing the latest paper is independent of whether a hint is useful.
        State state=State.copy(StateMemory.getSharedState(agent));
        state.setCurrentImage(user,event.getImageBase64());
        state.setCurrentImageMimeType(user,event.getMimeType());
        StateMemory.commitSharedState(state,agent);
        displayImageOnPrivatePage(source,user,event.getImageBase64(),event.getMimeType());
        String phase=stage();
        if (!"Paper".equals(phase)) {
            RepresentationCapture.record(agent,"camera.feedback_skipped",RepresentationCapture.data("participant_id",user,"reason","Setup".equals(phase)?"setup_preview_only":"paper_phase_ended"));
            if (manual && !"Setup".equals(phase)) {
                String notice="This photo arrived after the paper phase. Photo feedback is now closed; continue in the notebook. You can still ask a question in this private chat.";
                source.pushEventProposal(new PrivateMessageEvent(source,"Private_"+user,"OPEBot",notice));
                source.pushEventProposal(new PrivateMessageEvent(source,"Camera_"+user,"OPEBot",notice));
            }
            return;
        }
        Observation candidate;
        synchronized(feedback) {
            FeedbackState previous=feedback.get(user);
            if (previous == null) { previous=new FeedbackState();feedback.put(user,previous); }
            long now=feedbackNowMillis();
            if (previous.assessedAt >= 0 && now-previous.assessedAt < BACKGROUND_COOLDOWN_MS) {
                RepresentationCapture.record(agent,"camera.feedback_skipped",RepresentationCapture.data("participant_id",user,"reason","cooldown"));return;
            }
            if (previous.assessedImage != null && previous.assessedUseful) {
                try {
                    if (almostIdentical(event.getImageBase64(),previous.assessedImage)) {
                        RepresentationCapture.record(agent,"camera.feedback_skipped",RepresentationCapture.data("participant_id",user,"reason","unchanged_since_assessment"));return;
                    }
                } catch(java.io.IOException error) {
                    RepresentationCapture.record(agent,"camera.feedback_skipped",RepresentationCapture.data("participant_id",user,"reason","image_comparison_unavailable"));return;
                }
            }
            candidate=new Observation(user,previous);
            previous.assessedAt=now;previous.assessedUseful=false;
            previous.assessedImage=event.getImageBase64();previous.assessedMime=event.getMimeType();
        }
        observation.set(candidate);
        try { openAIrequestAndResponse(source,"Background paper observation; no student question was asked.",false,sender); }
        catch (RuntimeException error) {
            RepresentationCapture.record(agent,"tutor.background_error",RepresentationCapture.data("participant_id",user,"error_type",error.getClass().getSimpleName()));
        }
        finally {
            observation.remove();
            if (!candidate.requested) synchronized(feedback) {
                FeedbackState previous=feedback.get(user);
                previous.assessedAt=candidate.previousAssessedAt;previous.assessedUseful=candidate.previousUseful;
                previous.assessedImage=candidate.previousImage;previous.assessedMime=candidate.previousMime;
            }
        }
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
                + (observation.get() != null ?
                    " BACKGROUND IMAGE OBSERVATION, not a request for help. Default to response_type=\"No response\" and reply=\"\". Remain silent for a blank page, printed activity prompt only, unreadable writing, unchanged reasoning, hand motion, camera movement, or a student still working. A manual photo alone does not require a reply. Never routinely report missing or unreadable work, ask for a clearer photo, or repeat a prior hint. Reply only for a clearly legible, materially new reasoning error or a concrete useful next step not already addressed. Compare the labeled prior assessed image and this participant's recent feedback; visual movement alone is not new reasoning." :
                    " DIRECT STUDENT QUESTION: respond helpfully to the actual question even if the image is blank, unchanged, or not readable. Ask about visibility only if it is needed to answer that question. Do not give unsolicited reminders about readability.")
                + "\nCURRENT PHASE: " + phase + ". "
                + (("Paper".equals(phase) || "Setup".equals(phase))
                    ? "Students reason on paper. Ask about true versus recorded values, dependency expansion, coefficient signs and powers, and matrix row/column meaning. Do not give NumPy code or a complete solution. Use only clearly visible paper details."
                    : "Students are coding or submitting. Discuss the code or question they share. Do not infer their current code from an earlier paper image. Do not claim tests passed or submission succeeded without the platform receipt."));
            Observation background=observation.get();
            if (background != null) {
                system.put("content",system.getString("content") + " Recent feedback already given to this participant: " + background.recentReplies.toString());
                if (background.previousImage != null) {
                    JSONArray comparison=new JSONArray();
                    comparison.put(new JSONObject().put("type","text").put("text","PRIOR ASSESSED IMAGE for comparison only. The earlier image in this request is CURRENT. Identify changes in reasoning, not camera motion; do not describe this prior image as current work."));
                    comparison.put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:"+background.previousMime+";base64,"+background.previousImage)));
                    messages.put(new JSONObject().put("role","user").put("content",comparison));
                }
            }
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
