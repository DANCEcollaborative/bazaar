package com.example.etherpad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin wrapper around Etherpad's built-in HTTP API
 * (https://etherpad.org/doc/v1.9.7/#index_http_api).
 *
 * Requires the "com.fasterxml.jackson.core:jackson-databind" dependency.
 *
 * One instance of this client can talk to one Etherpad server, which can host
 * an arbitrary number of pads simultaneously (that's native Etherpad
 * behaviour -- nothing extra is needed on the server side for "multiple
 * documents", each pad is just addressed by its own padID).
 */
public final class EtherpadClient {

    /** Etherpad's own error codes, returned in every response body as "code". */
    public static final class EtherpadApiException extends RuntimeException {
        public final int code;
        public EtherpadApiException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private final URI baseUri;      // e.g. http://localhost:9001
    private final String apiKey;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile String apiVersion; // lazily resolved, e.g. "1.2.15"

    public EtherpadClient(String baseUrl, String apiKey) {
        this.baseUri = URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Resolves the highest API version this server supports (cached after first call). */
    private String apiVersion() throws IOException, InterruptedException {
        if (apiVersion == null) {
            HttpRequest req = HttpRequest.newBuilder(baseUri.resolve("/api"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(resp.body());
            apiVersion = node.get("currentVersion").asText();
        }
        return apiVersion;
    }

    private JsonNode call(String function, Map<String, String> params) {
        try {
            Map<String, String> all = new LinkedHashMap<>(params);
            all.put("apikey", apiKey);

            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> e : all.entrySet()) {
                if (body.length() > 0) body.append('&');
                body.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
                body.append('=');
                body.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }

            URI uri = baseUri.resolve("/api/" + apiVersion() + "/" + function);
            HttpRequest req = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(resp.body());

            int code = json.get("code").asInt();
            if (code != 0) {
                String msg = json.has("message") ? json.get("message").asText() : "Etherpad API error";
                throw new EtherpadApiException(code, function + " failed (code " + code + "): " + msg);
            }
            return json.get("data");
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Etherpad API call to '" + function + "' failed", ex);
        }
    }

    // ---- Groups (one per agent instance) ------------------------------------

    /** Creates a brand-new, unmapped group. Returns groupID. */
    public String createGroup() {
        return call("createGroup", Map.of()).get("groupID").asText();
    }

    /**
     * Creates (or reuses) a group deterministically mapped from your own ID
     * (e.g. the agent instance's ID). Idempotent: calling this again with the
     * same mapper returns the same groupID.
     */
    public String createGroupIfNotExistsFor(String groupMapper) {
        return call("createGroupIfNotExistsFor", Map.of("groupMapper", groupMapper))
                .get("groupID").asText();
    }

    public void deleteGroup(String groupID) {
        call("deleteGroup", Map.of("groupID", groupID));
    }

    // ---- Pads -----------------------------------------------------------------

    /** Creates a pad inside a group. Returns the full padID ("groupID$padName"). */
    public String createGroupPad(String groupID, String padName) {
        return call("createGroupPad", Map.of("groupID", groupID, "padName", padName))
                .get("padID").asText();
    }

    public String createGroupPad(String groupID, String padName, String initialText) {
        return call("createGroupPad", Map.of(
                "groupID", groupID,
                "padName", padName,
                "text", initialText
        )).get("padID").asText();
    }

    /** This is (2b): fetch the full current text of a pad. */
    public String getText(String padID) {
        return call("getText", Map.of("padID", padID)).get("text").asText();
    }

    public String getHTML(String padID) {
        return call("getHTML", Map.of("padID", padID)).get("html").asText();
    }

    public void setText(String padID, String text) {
        call("setText", Map.of("padID", padID, "text", text));
    }

    public void deletePad(String padID) {
        call("deletePad", Map.of("padID", padID));
    }

    // ---- Authors (one per human user) ------------------------------------

    /**
     * Creates (or reuses) an author deterministically mapped from your own
     * user ID. Idempotent, same shape as createGroupIfNotExistsFor.
     */
    public String createAuthorIfNotExistsFor(String authorMapper, String displayName) {
        return call("createAuthorIfNotExistsFor", Map.of(
                "authorMapper", authorMapper,
                "name", displayName
        )).get("authorID").asText();
    }

    // ---- Sessions (grants one author access to one group's pads) --------------

    /**
     * Grants authorID access to all pads in groupID until validUntil
     * (epoch seconds). Returns a sessionID that must be set as a cookie
     * named "sessionID" in the user's browser for the Etherpad origin
     * before the pad iframe loads.
     */
    public String createSession(String groupID, String authorID, long validUntilEpochSeconds) {
        return call("createSession", Map.of(
                "groupID", groupID,
                "authorID", authorID,
                "validUntil", Long.toString(validUntilEpochSeconds)
        )).get("sessionID").asText();
    }

    public void deleteSession(String sessionID) {
        call("deleteSession", Map.of("sessionID", sessionID));
    }
}