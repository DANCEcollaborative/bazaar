package basilica2.agents.data;

public class AgentInfo extends Object {

    String _key = "";
    String _name = "";
    String _role = "";
    String _connected = "";

    public AgentInfo(String key, String name, String role, String connected) {

        this._key = key;
        this._name = name;
        this._role = role;
        this._connected = connected;
    }

    public void SetStatus(String newStatus) {
        this._connected = newStatus;
    }

    public String AgentKey() {
        return this._key;
    }

    public String AgentName() {
        return this._name;
    }

    public String AgentRole() {
        return this._role;
    }

    public String ConnectedStatus() {
        return this._connected;
    }
}