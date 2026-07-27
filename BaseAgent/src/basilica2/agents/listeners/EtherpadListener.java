package basilica2.agents.listeners;

import java.io.IOException;

import java.util.Properties;
import javax.net.ssl.SSLContext;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.listeners.*;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.time.Instant;
import java.time.Duration;

public class EtherpadListener extends BasilicaAdapter
{
	public String host;
	public String port;
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;
	private String apiKey;
	private String requestURL;
	private String model;
	private String modelName;
	private String context;
	private double temperature;
    private boolean contextFlag;
    private int contextLen;
    public String myName;
    public List<String> topics;
    private Instant start = Instant.now();
    private Instant finish;
    private String roomName;
    public static final String botName = "Bot1";

    // ---- Etherpad attachment ---------------------------------------------------
    // This bot writes into the Etherpad server set up on (currently) {bree,bazaar}
    //
    // Uses a PLAIN (non-group, non-session) pad, padID = roomName. This
    // deliberately matches HTML page document-chat-mm.html, which sets
    // `epPadId = room` -- `room` there and `roomName` here both come from
    // the exact same Bazaar room identifier (the browser resolves it from
    // the chat URL path; Agent.getRoomName() resolves it server-side), so
    // keying the pad on that one shared value is what keeps the bot and the
    // browser on the same document. 
    private String etherpadBaseUrl;
    private String etherpadApiKey;
    private String etherpadApiVersionCache;
    private String etherpadPadId;

    // Fixed message, written repeatedly rather than once: a single
    // attach-then-write at construction time had no retry if that one
    // attempt failed (e.g. Etherpad not up yet), which is what silently
    // dropped the write before. Writing on a timer means a still-down
    // Etherpad just gets retried on the next tick instead of failing
    // forever silently.
    private static final String ETHERPAD_MESSAGE = "Hi. I'm a bossy bot";
    private int etherpadWriteIntervalSeconds = 120;
    private ScheduledExecutorService etherpadScheduler;

    private static class EtherpadApiException extends RuntimeException {
        final int code;
        EtherpadApiException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

	public EtherpadListener(Agent a)
	{
		super(a);
		roomName = a.getRoomName();

		Properties ep_prop = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		try {
			etherpadBaseUrl = ep_prop.getProperty("etherpad.base.url", "https://bree.lti.cs.cmu.edu/pad");
			etherpadApiKey = ep_prop.getProperty("etherpad.api.key");
			etherpadWriteIntervalSeconds = Integer.parseInt(
					ep_prop.getProperty("etherpad.write.interval.seconds", "120"));

		}
		catch (Exception e){}

		// Separate from the LLM-config parsing above: an Etherpad attach
		// failure (bad key, server down, etc.) shouldn't be silently
		// swallowed by that unrelated catch block, and shouldn't prevent
		// this constructor from finishing either.
		startEtherpadWriter();
	}


	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
	        System.err.println("EtherpadListener preProcessEvent for MessageEvent");
//			finish = Instant.now();
//			long timeElapsed = Duration.between(start, finish).toMillis();
//			if (timeElapsed > 1500) {
//				boolean proceed = messageFilter((MessageEvent) e);
//				if (proceed) {
//			        System.err.println("EtherpadListener preProcessEvent: calling handleMessageEvent");
//					try {
//						handleMessageEvent(source, (MessageEvent) e);
//					} catch (JSONException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//				}
//				start = finish;
//			}
		}
	}
	
	
	/**
	 * Attaches the bot instance to its room's Etherpad pad: padID =
	 * roomName, the same value document-chat-mm.html uses for its `room`-
	 * keyed pad, so bot and users land on the same document. Plain pads
	 * auto-create on first write, so this doesn't strictly need to call the
	 * API at all -- it does anyway (createPad) purely to fail fast and log
	 * clearly if the API key or Etherpad server are misconfigured, rather
	 * than discovering that silently on the first typeIntoDocument() call.
	 * Safe to call once from the constructor -- failures are logged, not
	 * thrown, so a down/misconfigured Etherpad doesn't prevent the rest of
	 * the agent from starting up.
	 */
	public void attachToEtherpad() {
		etherpadPadId = roomName;
		try {
			etherpadApiCall("createPad", mapOf("padID", etherpadPadId));
			System.err.println("EtherpadListener attachToEtherpad -- created new pad " + etherpadPadId);
		} catch (EtherpadApiException ex) {
			if (ex.code == 1) {
				// "pad already exists" -- the expected case whenever the bot
				// restarts, or a user already opened this room's document.
				System.err.println("EtherpadListener attachToEtherpad -- pad " + etherpadPadId
						+ " already exists, reusing it");
			} else {
				System.err.println("EtherpadListener attachToEtherpad -- Etherpad rejected createPad: " + ex.getMessage());
				etherpadPadId = null; // don't let typeIntoDocument write against an unverified pad/key
			}
		} catch (Exception ex) {
			System.err.println("EtherpadListener attachToEtherpad -- failed to reach Etherpad: " + ex);
			ex.printStackTrace();
			etherpadPadId = null;
		}
	}

	/**
	 * Appends a line of text to the attached pad (does not overwrite
	 * existing content). No-ops with a log line if attachToEtherpad()
	 * hasn't succeeded yet.
	 */
	public void typeIntoDocument(String text) {
		if (etherpadPadId == null) {
			System.err.println("EtherpadListener typeIntoDocument -- not attached to a pad, skipping: " + text);
			return;
		}
		try {
			etherpadApiCall("appendText", mapOf("padID", etherpadPadId, "text", "\n" + text));
			System.err.println("EtherpadListener typeIntoDocument -- appended to " + etherpadPadId + ": " + text);
		} catch (Exception ex) {
			System.err.println("EtherpadListener typeIntoDocument -- failed to append text: " + ex);
			ex.printStackTrace();
		}
	}

	/**
	 * Starts a background task that writes ETHERPAD_MESSAGE to the room's
	 * pad every etherpadWriteIntervalSeconds (default 120; override via
	 * "etherpad.write.interval.seconds" in EtherpadListener.properties).
	 * Each tick calls attachToEtherpad() first if a prior attempt hasn't
	 * succeeded yet (etherpadPadId == null), so a slow-starting or
	 * transiently-unreachable Etherpad server gets retried on the next
	 * tick instead of only being tried once, at construction time, with no
	 * retry if that single attempt lost the race against Etherpad coming
	 * up.
	 */
	private void startEtherpadWriter() {
		etherpadScheduler = Executors.newSingleThreadScheduledExecutor();
		etherpadScheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					if (etherpadPadId == null) {
						attachToEtherpad();
					}
					typeIntoDocument(ETHERPAD_MESSAGE);
				} catch (Exception ex) {
					// scheduleAtFixedRate silently cancels all future runs if
					// the task ever throws -- attachToEtherpad/typeIntoDocument
					// already catch their own errors, but this is a backstop
					// so one unexpected exception can't kill the periodic write.
					System.err.println("EtherpadListener startEtherpadWriter -- unexpected error: " + ex);
					ex.printStackTrace();
				}
			}
		}, 0, etherpadWriteIntervalSeconds, TimeUnit.SECONDS);
	}

	/** Stops the periodic write started by startEtherpadWriter(). */
	public void stopEtherpadWriter() {
		if (etherpadScheduler != null) {
			etherpadScheduler.shutdownNow();
		}
	}

	private static Map<String, String> mapOf(String... kv) {
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}

	private String etherpadApiVersion() throws IOException, JSONException {
		if (etherpadApiVersionCache == null) {
			URL url = new URL(etherpadBaseUrl + "/api");
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			try {
				conn.setRequestMethod("GET");
				StringBuilder resp = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						resp.append(line);
					}
				}
				etherpadApiVersionCache = new JSONObject(resp.toString()).getString("currentVersion");
			} finally {
				conn.disconnect();
			}
		}
		return etherpadApiVersionCache;
	}

	/**
	 * Calls Etherpad's classic HTTP API (createGroup*, createAuthor*,
	 * appendText, getText, ...) and returns the "data" object from the
	 * response, or null if the call succeeded but returned no data (most
	 * functions used here -- createPad, appendText, setText -- return
	 * "data":null on success; only functions like createGroup/getText
	 * return an actual object). Throws EtherpadApiException for any
	 * non-zero "code" (see Etherpad's HTTP API docs: 1 = bad params,
	 * 2 = internal error, 3 = no such function, 4 = bad API key).
	 */
	private JSONObject etherpadApiCall(String function, Map<String, String> params) throws IOException, JSONException {
		// URLEncoder.encode(null, ...) throws a bare, unhelpful NPE with no
		// indication of which value was missing. etherpadApiKey being null
		// (e.g. "etherpad.api.key" absent from apiKey.properties) is the
		// most likely cause if this fires on every call; a null padID
		// (roomName / Agent.getRoomName() came back null) is the other.
		// Fail with a message that names the actual problem instead.
		if (etherpadApiKey == null) {
			throw new IllegalStateException("etherpadApiKey is null -- check that apiKey.properties has an "
					+ "\"etherpad.api.key\" entry");
		}
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (entry.getValue() == null) {
				throw new IllegalStateException("etherpadApiCall(\"" + function + "\") called with null value for \""
						+ entry.getKey() + "\" -- likely roomName (Agent.getRoomName()) came back null");
			}
		}

		Map<String, String> all = new LinkedHashMap<>(params);
		all.put("apikey", etherpadApiKey);

		StringBuilder body = new StringBuilder();
		for (Map.Entry<String, String> entry : all.entrySet()) {
			if (body.length() > 0) {
				body.append('&');
			}
			body.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
			body.append('=');
			body.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
		}

		URL url = new URL(etherpadBaseUrl + "/api/" + etherpadApiVersion() + "/" + function);
		HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
		try {
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
			}

			int responseCode = conn.getResponseCode();
			InputStream is = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
			StringBuilder resp = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					resp.append(line);
				}
			}

			JSONObject json = new JSONObject(resp.toString());
			int code = json.getInt("code");
			if (code != 0) {
				throw new EtherpadApiException(code, function + " failed (code " + code + "): " + json.optString("message"));
			}
			// "data" is JSON null (not merely absent) for functions with
			// nothing to return -- getJSONObject("data") throws on that
			// (org.json's null sentinel isn't a JSONObject), so check first.
			if (json.isNull("data")) {
				return null;
			}
			return json.getJSONObject("data");
		} finally {
			conn.disconnect();
		}
	}




	@Override
	public void processEvent(InputCoordinator source, Event e) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}
}