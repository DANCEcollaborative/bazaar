package com.example.etherpad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Bridges your Java agent's lifecycle to Etherpad:
 *
 *  (2a) When an agent instance is created, register() gives it its own
 *       Etherpad group + single pad that all of that instance's users share.
 *
 *  (2b) fetchText()/fetchTextNow() pull the pad's current text -- call
 *       fetchTextNow() on user request, and the manager also does it for you
 *       on a fixed schedule per instance.
 *
 * One EtherpadClient / manager can serve any number of agent instances --
 * Etherpad natively hosts unlimited pads, each identified by its own padID.
 */
public final class AgentEtherpadManager implements AutoCloseable {

    /** Everything you need to embed the shared pad for one agent instance. */
    public static final class AgentPadContext {
        private final String agentInstanceId;
        private final String groupID;
        private final String padID;

        AgentPadContext(String agentInstanceId, String groupID, String padID) {
            this.agentInstanceId = agentInstanceId;
            this.groupID = groupID;
            this.padID = padID;
        }

        public String agentInstanceId() { return agentInstanceId; }
        public String groupID() { return groupID; }
        public String padID() { return padID; }
    }

    /** Everything a single logged-in user needs to open the pad in their browser. */
    public static final class UserPadAccess {
        private final String padID;
        private final String sessionID;
        private final String authorID;

        UserPadAccess(String padID, String sessionID, String authorID) {
            this.padID = padID;
            this.sessionID = sessionID;
            this.authorID = authorID;
        }

        public String padID() { return padID; }
        public String sessionID() { return sessionID; }
        public String authorID() { return authorID; }
    }

    private final EtherpadClient client;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, AgentPadContext> instances = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();

    /** Called with (agentInstanceId, latestPadText) whenever a scheduled or on-demand fetch completes. */
    private final BiConsumer<String, String> onTextFetched;

    private final Duration pollInterval;
    /** How long a user session stays valid once granted (renew by calling registerUser again). */
    private final Duration sessionTtl;

    public AgentEtherpadManager(EtherpadClient client,
                                 Duration pollInterval,
                                 Duration sessionTtl,
                                 BiConsumer<String, String> onTextFetched) {
        this.client = client;
        this.pollInterval = pollInterval;
        this.sessionTtl = sessionTtl;
        this.onTextFetched = onTextFetched;
    }

    // ---- (2a) instance lifecycle --------------------------------------------

    /**
     * Call this exactly once when a new agent instance is created.
     * Idempotent: if called again with the same agentInstanceId (e.g. after a
     * restart) it reuses the same group/pad instead of creating duplicates.
     */
    public AgentPadContext register(String agentInstanceId) {
        return instances.computeIfAbsent(agentInstanceId, id -> {
            // groupMapper ties the Etherpad group deterministically to your own ID.
            String groupID = client.createGroupIfNotExistsFor("agent-instance:" + id);
            String padID = client.createGroupPad(groupID, "main");

            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> fetchTextSafely(id),
                    pollInterval.toSeconds(), pollInterval.toSeconds(), TimeUnit.SECONDS);
            pollingTasks.put(id, task);

            return new AgentPadContext(id, groupID, padID);
        });
    }

    /** Call when an agent instance is torn down. Stops polling; optionally delete the pad/group yourself. */
    public void unregister(String agentInstanceId) {
        ScheduledFuture<?> task = pollingTasks.remove(agentInstanceId);
        if (task != null) task.cancel(false);
        instances.remove(agentInstanceId);
    }

    /** Grants one of the agent instance's users edit access to the shared pad. */
    public UserPadAccess registerUser(String agentInstanceId, String userId, String displayName) {
        AgentPadContext ctx = instances.get(agentInstanceId);
        if (ctx == null) {
            throw new IllegalStateException("Agent instance " + agentInstanceId + " was never register()ed");
        }
        String authorID = client.createAuthorIfNotExistsFor("user:" + userId, displayName);
        long validUntil = Instant.now().plus(sessionTtl).getEpochSecond();
        String sessionID = client.createSession(ctx.groupID(), authorID, validUntil);
        return new UserPadAccess(ctx.padID(), sessionID, authorID);
    }

    // ---- (2b) text fetch -------------------------------------------------------

    /** Fetch on demand (e.g. a user clicked "sync" in the chat UI). Blocks on the HTTP call. */
    public String fetchTextNow(String agentInstanceId) {
        AgentPadContext ctx = instances.get(agentInstanceId);
        if (ctx == null) {
            throw new IllegalStateException("Agent instance " + agentInstanceId + " was never register()ed");
        }
        String text = client.getText(ctx.padID());
        if (onTextFetched != null) onTextFetched.accept(agentInstanceId, text);
        return text;
    }

    private void fetchTextSafely(String agentInstanceId) {
        try {
            fetchTextNow(agentInstanceId);
        } catch (Exception e) {
            // Don't let one failed poll kill the scheduled task; log and move on.
            System.err.println("[AgentEtherpadManager] periodic fetch failed for "
                    + agentInstanceId + ": " + e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    // ---- example wiring ---------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        EtherpadClient client = new EtherpadClient("http://localhost:9001", System.getenv("ETHERPAD_API_KEY"));

        AgentEtherpadManager manager = new AgentEtherpadManager(
                client,
                Duration.ofSeconds(30),   // poll every 30s
                Duration.ofHours(12),     // user sessions last 12h
                (agentInstanceId, text) -> System.out.println("[" + agentInstanceId + "] latest text:\n" + text)
        );

        // (2a) on agent instance creation:
        AgentEtherpadManager.AgentPadContext ctx = manager.register("agent-instance-42");
        System.out.println("Shared pad for this instance: " + ctx.padID());

        // when each user logs into that instance:
        AgentEtherpadManager.UserPadAccess access = manager.registerUser("agent-instance-42", "user-7", "Chas");
        System.out.println("Give the browser cookie sessionID=" + access.sessionID()
                + " for the Etherpad origin, then load /p/" + access.padID());

        // (2b) on request:
        System.out.println(manager.fetchTextNow("agent-instance-42"));

        Thread.sleep(2000);
        manager.close();
    }
}