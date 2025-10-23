package smartlab.communication;

/**
 * Simple stub used in command-line builds where the original PSI messaging
 * infrastructure is unavailable. The Eclipse-based deployment provides an
 * implementation via the smartlab libraries; here we provide no-op methods so
 * classes depending on the API still compile.
 */
public class CommunicationManager {

    public CommunicationManager() {
        // No initialization required for the stub implementation.
    }

    /**
     * Sends a message to an external PSI topic. The real implementation forwards
     * the message to the PSI broker; the stub ignores the call.
     *
     * @param topic   channel name
     * @param message serialized payload
     */
    public void msgSender(String topic, String message) {
        // Intentionally left blank; stub for headless builds.
    }
}
