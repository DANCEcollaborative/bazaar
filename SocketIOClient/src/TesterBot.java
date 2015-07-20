import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class TesterBot
{
	static SocketIO socket;
	static IOCallback socketCallback;
	static final String socketURL = "http://localhost:8000";
	
	static class RobotSocketCallBack implements IOCallback
	{
		@Override
		public void onMessage(JSONObject json, IOAcknowledge ack)
		{
			try
			{
				System.out.println("Server said:" + json.toString(2));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
		

		@Override
		public void onMessage(String data, IOAcknowledge ack)
		{
			System.out.println("Server said: " + data);
		}

		@Override
		public void onError(SocketIOException socketIOException)
		{
			System.out.println("an Error occurred...");
			socketIOException.printStackTrace();

			System.out.println("attempting to reconnect...");
			try
			{
				socket = new SocketIO(socketURL);
				socket.connect(new RobotSocketCallBack());
				
				new Timer().schedule(new TimerTask()
				{
					public void run()
					{
						System.out.println("fish!");
						socket.emit("adduser", "Limbo", "ROBOT", new Boolean(false));
						socket.emit("sendchat" ,"...and we're back!");
					}
				}, 1000L);
				
			}
			catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		@Override
		public void onDisconnect()
		{
			System.out.println("Connection terminated.");
		}

		@Override
		public void onConnect()
		{
			System.out.println("Connection established");
		}

		@Override
		public void on(String event, IOAcknowledge ack, Object... args)
		{
			System.out.println("Server triggered event '" + event + "'");
		}
	}
	
	public static void main(String[] args) throws MalformedURLException
	{

		socket = new SocketIO(socketURL);
		
		Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
		sioLogger.setLevel(Level.WARNING);
		

		socket.connect(new RobotSocketCallBack());

		// This line is cached until the connection is establisched.
		socket.emit("adduser", "Limbo", "ROBOT", new Boolean(false));
		socket.emit("sendchat", "Hello!");
		
	}
}
