package basilica2.agents.listeners.plan;
import basilica2.agents.components.*;
import basilica2.agents.data.State;
import basilica2.agents.events.*;
import basilica2.agents.listeners.RepresentationCameraListener;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class RotationTransportHarness {
    static class TestAgent extends Agent {
        TestAgent(){super("OPEBot_fcdsrepresentationfcds-p2-26-fall-1a-room260911970");hasUI=false;addComponent(new StateMemory(this,"stateMemory",""));}
        protected void createComponents(){} protected void connectComponents(){} public void receiveLoggerMessage(String text){}
    }
    static class Input extends InputCoordinator {
        List<Event> received=new ArrayList<Event>();Input(Agent agent){super(agent,"inputCoordinator","");}
        public void pushEventProposal(Event event){received.add(event);}
    }
    public static class TransportCamera extends RepresentationCameraListener {
        TransportCamera(Agent agent){super(agent);}
        public String getAllMessages(InputCoordinator source,String prompt,String sender){return "Test student question: "+prompt;}
        // No transport or rotation overrides: this exercises the production HTTPS sender and signed metadata lookup.
    }
    static void tests() throws Exception {
        TestAgent agent=new TestAgent();Input input=new Input(agent);agent.addComponent(input);
        State state=new State();state.setStepInfo("Paper","other","paper_work","representation_gate");
        String original=new String(Files.readAllBytes(Paths.get("source.base64")),StandardCharsets.UTF_8).trim();
        state.setCurrentImage("1",original);state.setCurrentImageMimeType("1","image/png");StateMemory.commitSharedState(state,agent);
        TransportCamera camera=new TransportCamera(agent);
        camera.displayImageOnPrivatePage(input,"1",original,"image/png");
        if(!((MessageEvent)input.received.get(0)).getText().endsWith(";%;image:::"+original))throw new AssertionError("Preview no longer identifies archived bytes");
        String textOnly=new JSONObject().put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content","Text-only test"))).toString();
        camera.sendToOpenAI(input,textOnly,false);
        for(int i=0;i<21;i++) {
            camera.openAIrequestAndResponse(input,"Inspect the test diagram "+i,false,"Private_1");
            if(!original.equals(StateMemory.getSharedState(agent).getCurrentImage("1")))throw new AssertionError("Original state was mutated");
        }
        boolean blocked=false;
        try {camera.openAIrequestAndResponse(input,"The metadata service is now unavailable",false,"Private_1");}
        catch(IllegalStateException expected){blocked=true;}
        if(!blocked)throw new AssertionError("Unavailable rotation silently used zero");
        // Comparison images in a payload are rotated once using the same request-time snapshot.
        String data="data:image/png;base64,"+original;
        JSONArray parts=new JSONArray().put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url",data)))
            .put(new JSONObject().put("type","text").put("text","compare"))
            .put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url",data)));
        String body=new JSONObject().put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",parts))).toString();
        final int[] lookups={0};
        TutorImageRotation.Prepared prepared=TutorImageRotation.prepare(body,new TutorImageRotation.Lookup(){public TutorImageRotation.Rotation get(){lookups[0]++;return new TutorImageRotation.Rotation(90,100);}});
        if(lookups[0]!=1 || prepared.images.size()!=2 || !Arrays.equals(prepared.images.get(0).bytes,prepared.images.get(1).bytes))throw new AssertionError("Comparison image mismatch");
        System.out.println("PASS: production rotation lookup, HTTPS transport, immutable originals, preview identity and comparison images");
    }
    public static void main(String[] args){try{tests();System.exit(0);}catch(Throwable error){error.printStackTrace();System.exit(1);}}
}
