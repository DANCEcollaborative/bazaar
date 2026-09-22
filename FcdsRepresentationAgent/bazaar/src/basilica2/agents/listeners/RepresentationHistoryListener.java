package basilica2.agents.listeners;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import edu.cmu.cs.lti.basilica2.core.Agent;

/** Keep each agent instance's tutoring context in its own history file. */
public class RepresentationHistoryListener extends ChatMultiHistoryListener {
    @Override
    public synchronized void saveMessageToHistory(String sender, String receiver, String content) {
        super.saveMessageToHistory(sender, receiver,
            basilica2.agents.components.RepresentationCapture.redact(content));
    }
    public RepresentationHistoryListener(Agent agent) {
        super(agent);
        String room = agent.getName().replaceAll("[^A-Za-z0-9_.-]", "_");
        path = "chat_history/" + room + "-" + UUID.randomUUID().toString() + ".jsonl";
        try {
            Files.createDirectories(Paths.get(path).getParent());
            Files.createFile(Paths.get(path));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot create room-specific tutoring history", error);
        }
    }
}
