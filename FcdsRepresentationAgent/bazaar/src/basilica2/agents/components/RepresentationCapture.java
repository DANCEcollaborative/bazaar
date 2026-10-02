package basilica2.agents.components;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Iterator;
import edu.cmu.cs.lti.basilica2.core.Agent;

/** Dev-only durable local journal. The Bree collector tails this persistent bind mount. */
public final class RepresentationCapture {
    private static final Map<String, RepresentationCapture> runs = new HashMap<String, RepresentationCapture>();
    private final String room, producer = "bazaar:" + UUID.randomUUID().toString();
    private long sequence;
    private final Path file;
    private RepresentationCapture(String room) throws Exception {
        this.room=room;
        Path folder=Paths.get("capture_spool"); Files.createDirectories(folder);
        file=folder.resolve(room+"-"+UUID.randomUUID().toString()+".jsonl");
        Files.createFile(file);
    }
    public static String room(Agent agent) {
        return agent.getName().replaceFirst("^OPEBot_", "").replaceFirst("^fcdsrepresentation", "");
    }
    public static JSONObject data(Object... values) {
        try {JSONObject result=new JSONObject();for(int i=0;i<values.length;i+=2)result.put(String.valueOf(values[i]),values[i+1]);return result;}
        catch(Exception e){throw new IllegalArgumentException(e);}
    }
    public static String redact(String text) {
        return text == null ? "" : text.replaceAll("([?&#](?:capture|token|access_token)=)[^\\s&#\\\"<>]+", "$1[REDACTED]");
    }
    private static Object clean(Object value) throws Exception {
        if(value instanceof String)return redact((String)value);
        if(value instanceof JSONObject) {
            JSONObject source=(JSONObject)value, result=new JSONObject();
            Iterator<?> keys=source.keys();
            while(keys.hasNext()){String key=String.valueOf(keys.next());result.put(key,clean(source.get(key)));}
            return result;
        }
        if(value instanceof JSONArray) {
            JSONArray source=(JSONArray)value, result=new JSONArray();
            for(int i=0;i<source.length();i++)result.put(clean(source.get(i)));
            return result;
        }
        return value;
    }
    public static synchronized void record(Agent agent, String kind, JSONObject payload) {
        String room=room(agent);
        if(!room.matches("fcds-p2-26-fall-1a-room[0-9]{9}"))return;
        try {
            RepresentationCapture run=runs.get(room);
            if(run==null){run=new RepresentationCapture(room);runs.put(room,run);}
            JSONObject event=data("schema_version",1,"room_id",room,"event_id",UUID.randomUUID().toString(),
                "producer_id",run.producer,"producer_sequence",++run.sequence,"event_type",kind,
                "occurred_at",Instant.now().toString(),"payload",payload);
            try(FileOutputStream out=new FileOutputStream(run.file.toFile(),true)) {
                out.write((clean(event).toString()+"\n").getBytes(StandardCharsets.UTF_8));out.getFD().sync();
            }
        } catch(Exception e) {System.err.println("FCDS_CAPTURE_ERROR: "+e.getClass().getSimpleName());}
    }
    public static String ticket(String bazaarRoom,int user) {
        String room=bazaarRoom.replaceFirst("^fcdsrepresentation", "");
        if(!room.matches("fcds-p2-26-fall-1a-room[0-9]{9}")||user<1||user>4)throw new IllegalArgumentException("Invalid camera scope");
        try {
            byte[] key=new String(Files.readAllBytes(Paths.get("capture.key")),StandardCharsets.UTF_8).trim().getBytes(StandardCharsets.UTF_8);
            if(key.length<32)throw new IllegalStateException("Missing capture signing key");
            String expires=Long.toString(Instant.now().getEpochSecond()+86400);
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return expires+"."+hex(mac.doFinal((room+"|"+user+"|"+expires).getBytes(StandardCharsets.UTF_8)));
        } catch(Exception e) {throw new IllegalStateException("Camera recording is not configured",e);}
    }
    public static String hashImage(String base64) {
        try {return hex(MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(base64)));}
        catch(Exception e){return "invalid-image";}
    }
    private static String hex(byte[] data) {StringBuilder b=new StringBuilder();for(byte v:data)b.append(String.format("%02x",v & 255));return b.toString();}
}
