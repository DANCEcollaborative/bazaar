package basilica2.agents.components;

public interface ChatClient
{

	public abstract void disconnect();

	public abstract void login(String roomName);

}