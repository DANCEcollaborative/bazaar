package basilica2.agents.components;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import javax.imageio.stream.ImageOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Request-time rendering only: originals in State and the archive stay immutable. */
public final class TutorImageRotation {
    public static final class Rotation {
        public final double degrees;
        public final long revision;
        public Rotation(double degrees,long revision) {
            if(Double.isNaN(degrees)||Double.isInfinite(degrees)||degrees < -180||degrees > 180)
                throw new IllegalArgumentException("Invalid clockwise rotation");
            this.degrees=degrees;this.revision=revision;
        }
    }
    public interface Lookup { Rotation get() throws Exception; }
    public static final class Image {
        public final byte[] bytes;
        public final String mime, originalHash;
        public final double degrees;
        public final long revision;
        public final int messageIndex,partIndex;
        Image(byte[] bytes,String mime,String originalHash,Rotation rotation,int message,int part) {
            this.bytes=bytes;this.mime=mime;this.originalHash=originalHash;
            this.degrees=rotation.degrees;this.revision=rotation.revision;this.messageIndex=message;this.partIndex=part;
        }
    }
    public static final class Prepared {
        public final String payload;
        public final List<Image> images;
        Prepared(String payload,List<Image> images){this.payload=payload;this.images=images;}
    }
    public static Rotation lookup(String endpoint,String bazaarRoom,String user) throws Exception {
        if(user==null || !user.matches("[1-4]"))throw new IOException("Missing image participant");
        String room=bazaarRoom.replaceFirst("^fcdsrepresentation", "");
        JSONObject body=RepresentationCapture.data("room_id",room,"participant_id",user,"current_rotation",true);
        HttpURLConnection connection=(HttpURLConnection)new URL(endpoint).openConnection();
        connection.setConnectTimeout(2000);connection.setReadTimeout(3000);
        connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type","application/json");
        connection.setRequestProperty("Authorization","Camera "+RepresentationCapture.ticket(bazaarRoom,Integer.parseInt(user)));
        connection.setDoOutput(true);
        try {
            try(java.io.OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            if(connection.getResponseCode()!=200)throw new IOException("Rotation lookup unavailable");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){
                byte[] buffer=new byte[1024];int count;
                while((count=in.read(buffer))!=-1){bytes.write(buffer,0,count);if(bytes.size()>16384)throw new IOException("Oversized rotation reply");}
            }
            JSONObject saved=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            if(!user.equals(saved.getString("participant_id")))throw new IOException("Rotation participant mismatch");
            Object degrees=saved.get("rotation_degrees");
            if(!(degrees instanceof Number))throw new IOException("Invalid rotation reply");
            return new Rotation(((Number)degrees).doubleValue(),saved.getLong("rotation_revision"));
        } finally {connection.disconnect();}
    }
    public static Prepared prepare(String original,Lookup lookup) throws Exception {
        JSONObject payload=new JSONObject(original);JSONArray messages=payload.getJSONArray("messages");
        List<Image> images=new ArrayList<Image>();Rotation rotation=null;
        for(int i=0;i<messages.length();i++) {
            Object content=messages.getJSONObject(i).get("content");
            if(!(content instanceof JSONArray))continue;
            JSONArray parts=(JSONArray)content;
            for(int j=0;j<parts.length();j++) {
                JSONObject part=parts.getJSONObject(j);
                if(!"image_url".equals(part.optString("type")))continue;
                if(rotation==null)rotation=lookup.get(); // One current angle per request, even for comparison images.
                JSONObject imageUrl=part.getJSONObject("image_url");String url=imageUrl.getString("url");
                int split=url.indexOf(";base64,");
                if(split<0 || !(url.startsWith("data:image/jpeg;")||url.startsWith("data:image/png;")))
                    throw new IOException("Unsupported tutor image");
                String mime=url.substring(5,split);byte[] raw=Base64.getDecoder().decode(url.substring(split+8));
                String originalHash=hash(raw);byte[] rendered=raw;
                if(rotation.degrees!=0) {rendered=rotate(raw,rotation.degrees);mime="image/jpeg";}
                imageUrl.put("url","data:"+mime+";base64,"+Base64.getEncoder().encodeToString(rendered));
                images.add(new Image(rendered,mime,originalHash,rotation,i,j));
            }
        }
        return new Prepared(payload.toString(),images);
    }
    public static byte[] rotate(byte[] original,double clockwise) throws IOException {
        // The camera's canvas has already normalized EXIF; rotate those decoded pixels once.
        BufferedImage source=ImageIO.read(new ByteArrayInputStream(original));
        if(source==null || (long)source.getWidth()*source.getHeight()>16000000)throw new IOException("Invalid/oversized tutor image");
        double radians=Math.toRadians(clockwise),cos=Math.abs(Math.cos(radians)),sin=Math.abs(Math.sin(radians));
        int width=(int)Math.ceil(source.getWidth()*cos+source.getHeight()*sin-1e-9);
        int height=(int)Math.ceil(source.getWidth()*sin+source.getHeight()*cos-1e-9);
        BufferedImage result=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=result.createGraphics();
        try {
            graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.translate(width/2.0,height/2.0);graphics.rotate(radians);
            graphics.translate(-source.getWidth()/2.0,-source.getHeight()/2.0);graphics.drawImage(source,0,0,null);
        } finally {graphics.dispose();}
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageWriter writer=ImageIO.getImageWritersByFormatName("jpeg").next();
        try(ImageOutputStream output=ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);ImageWriteParam parameters=writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);parameters.setCompressionQuality(0.9f);
            writer.write(null,new IIOImage(result,null,null),parameters);
        } finally {writer.dispose();}
        return bytes.toByteArray();
    }
    public static String hash(byte[] bytes) {
        try {
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();
            for(byte value:digest)text.append(String.format("%02x",value & 255));return text.toString();
        } catch(Exception error){throw new IllegalStateException(error);}
    }
    public static JSONArray renderingMetadata(Prepared prepared) throws Exception {
        JSONArray records=new JSONArray();
        for(Image image:prepared.images)records.put(RepresentationCapture.data("message_index",image.messageIndex,"part_index",image.partIndex,
            "original_sha256",image.originalHash,"sent_sha256",hash(image.bytes),"rotation_degrees",image.degrees,
            "rotation_revision",image.revision,"rotation_convention","clockwise"));
        return records;
    }
    private static Path evidenceDirectory;
    private static int evidenceCount;
    public static synchronized void captureEvidence(Prepared prepared,String request,String room,String user) throws Exception {
        for(Image image:prepared.images) {
            if(evidenceCount>=20)return;
            if(evidenceDirectory==null) {
                String configured=System.getProperty("fcds.llm.evidence.dir");
                evidenceDirectory=configured==null?Files.createTempDirectory("fcds-llm-image-evidence-"):Files.createDirectories(Paths.get(configured));
                Files.setPosixFilePermissions(evidenceDirectory,PosixFilePermissions.fromString("rwx------"));
                System.out.println("FCDS_LLM_IMAGE_EVIDENCE_DIR: "+evidenceDirectory.toAbsolutePath());
            }
            String name=String.format("image-%02d",evidenceCount+1);
            Path file=evidenceDirectory.resolve(name+("image/png".equals(image.mime)?".png":".jpg"));
            // CREATE_NEW avoids ever overwriting evidence from an earlier process.
            Files.write(file,image.bytes,java.nio.file.StandardOpenOption.CREATE_NEW);
            JSONObject metadata=RepresentationCapture.data("request_id",request,"room_id",room,"participant_id",user,
                "message_index",image.messageIndex,"part_index",image.partIndex,"rotation_degrees",image.degrees,
                "rotation_revision",image.revision,"rotation_convention","clockwise","original_sha256",image.originalHash,
                "sent_sha256",hash(image.bytes),"request_sha256",hash(prepared.payload.getBytes(StandardCharsets.UTF_8)),
                "image_file",file.getFileName().toString(),"mime_type",image.mime,"capture_point","requestModel input",
                "note","Exact bytes handed to LLM transport; capture alone does not establish provider receipt.");
            Files.write(evidenceDirectory.resolve(name+".json"),metadata.toString(2).getBytes(StandardCharsets.UTF_8),java.nio.file.StandardOpenOption.CREATE_NEW);
            evidenceCount++;
        }
    }
}
