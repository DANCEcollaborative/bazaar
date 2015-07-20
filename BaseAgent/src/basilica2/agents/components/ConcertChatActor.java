/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.components;

import de.fhg.ipsi.chatblocks2.awareness.DefaultAddOperation;
import de.fhg.ipsi.chatblocks2.awareness.DefaultRemoveOperation;
import de.fhg.ipsi.chatblocks2.awareness.IsTypingInfo;
import de.fhg.ipsi.chatblocks2.model.IReference;
import de.fhg.ipsi.chatblocks2.model.IReferenceableDocument;
import de.fhg.ipsi.chatblocks2.model.IReferencingDocument;
import de.fhg.ipsi.chatblocks2.model.messagebased.ChatMessage;
import de.fhg.ipsi.chatblocks2.model.messagebased.ReferencingMessage;
import de.fhg.ipsi.concertchat.framework.IPersistentSession;
import de.fhg.ipsi.concertchat.framework.UserChannel;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.events.AcknowledgeMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.util.TimeoutAdapter;
import de.fhg.ipsi.concertchat.framework.IPersistentChannel;
import de.fhg.ipsi.concertchat.model.ChannelReference;
import de.fhg.ipsi.utils.ImageUtilities;
import de.fhg.ipsi.whiteboard.Command;
import de.fhg.ipsi.whiteboard.Graphic;
//import de.fhg.ipsi.whiteboard.piece.text.TextStuff;
import de.fhg.ipsi.whiteboard.OutlineProperties;
import de.fhg.ipsi.whiteboard.operation.CreateCommand;
import de.fhg.ipsi.whiteboard.piece.image.ImageCache;
import de.fhg.ipsi.whiteboard.piece.image.ImageStuff;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author rohitk
 */
public class ConcertChatActor extends Component
{

	private static final String PRIVATE_MESSAGE_TYPE = "PRIVATE";
	public static final String PRIVATE_DELIMITER = "#!PRIVATE!#";
	public static String GENERIC_NAME = "ConcertChatActor";
	public static String GENERIC_TYPE = "Actor";
	private String agentName = "Tutor";
	protected IPersistentSession session = null;
	protected List<MessageEvent> messageQueue = new ArrayList<MessageEvent>();
	private boolean isTyping;
	private String currentlyTyping;
	private boolean doAcknowledgeAfterSending = false;
	private double typingScale;

	public ConcertChatActor(Agent a, String n, String pf)
	{
		this(a, a.getName(), n, pf);
	}

	public ConcertChatActor(Agent a, String user, String componentName, String pf)
	{

		super(a, componentName, pf);
		int underscore = user.indexOf("_");
		if (underscore > -1)
			agentName = user.substring(0, underscore);
		else
			agentName = user;

		if(myProperties!=null)
			typingScale = Double.parseDouble(myProperties.getProperty("typingScale", "1.5"));
	}

	public ConcertChatActor(Agent a, String n)
	{
		this(a, a.getName(), n, n+".properties");
	}

	public void setSession(IPersistentSession s)
	{
		session = s;
		if (messageQueue.size() != 0)
		{
			while (messageQueue.size() > 0)
			{
				handleMessageEvent(messageQueue.remove(0));
			}
		}
	}

	public IPersistentChannel getChannel()
	{
		return session.getChannel();
	}

	@Override
	protected void processEvent(Event e)
	{
		if (e instanceof PrivateMessageEvent)
		{
			handlePrivateMessageEvent((PrivateMessageEvent) e);
		}
		else if (e instanceof MessageEvent)
		{
			handleMessageEvent((MessageEvent) e);
		}

	}

	private void handlePrivateMessageEvent(PrivateMessageEvent e)
	{
		sendPrivateMessage(e.getDestinationUser(), e.getText());
	}

	@Override
	public String getType()
	{
		return GENERIC_TYPE;
	}

	protected void handleMessageEvent(MessageEvent me)
	{

		if(me.getFrom().contains(this.agentName))
		{
			if (session != null)
			{
				log(Logger.LOG_NORMAL,me.toString());
				if (isTyping)
				{
					messageQueue.add(me);
				}
				else
				{
					//sendReferencingMessage(me.getText(), me.getReference());
					doTyping(me);
				}
			}
			else
			{
				log(Logger.LOG_NORMAL, "pending: "+me);
				messageQueue.add(me);
			}
		}
		else
		{
			log(Logger.LOG_LOW, "message from "+me.getFrom()+" ignored for agent "+agentName);
			//System.err.println("CCActor: message from "+me.getFrom()+" rejected for user "+agentName);
		}
	}
	
	public void sendPrivateMessage(String user, String text)
	{
		if (text != null)
		{
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setAuthorName(agentName);
			chatMessage.setCreationTime(new Date());
			chatMessage.setMessageText(user+PRIVATE_DELIMITER+text);
			
			this.session.getChannel().sendMessage(PRIVATE_MESSAGE_TYPE, "blah", chatMessage);

		}
	}
	
	public void sendPrivateMessageShouldWorkButDoesnt(String user, String text)
	{
		if (text != null)
		{
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setAuthorName(agentName);
			chatMessage.setCreationTime(new Date());
			chatMessage.setMessageText(text);

			UserChannel.init(myAgent.getName());
			log(Logger.LOG_LOW, "sending private message to"+user+":"+text);
			
			UserChannel.getChannel().sendMessageTo(user, chatMessage, "Chatroom1");
			// this.session.getChannel().sendMessage("Chatroom1", "blah",
			// chatMessage);
			if (doAcknowledgeAfterSending)
			{
				// Acknowledge Message Sent
				AcknowledgeMessageEvent ame = new AcknowledgeMessageEvent(this);
				this.broadcast(ame);
				doAcknowledgeAfterSending = false;
			}
		}
	}

	private void sendMessage(String text)
	{
		if (text != null)
		{
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setAuthorName(agentName);
			chatMessage.setCreationTime(new Date());
			chatMessage.setMessageText(text);
			
			this.session.getChannel().sendMessage("Chatroom1", "blah", chatMessage);
			if (doAcknowledgeAfterSending)
			{
				// Acknowledge Message Sent
				AcknowledgeMessageEvent ame = new AcknowledgeMessageEvent(this);
				this.broadcast(ame);
				doAcknowledgeAfterSending = false;
			}

		}
	}
	
	private void sendReferencingMessage(MessageEvent me)
	{
		String text = me.getText();
		Event referenced = me.getReference();
		if (text != null)
		{
			ReferencingMessage chatMessage = new ReferencingMessage();
			chatMessage.setAuthorName(agentName);
			chatMessage.setCreationTime(new Date());
			chatMessage.setMessageText(text);
			if(referenced != null)
				chatMessage.addReferenceTo(new ChannelReference(chatMessage, (IReferenceableDocument) referenced.getOriginal()));
			
			System.out.println("*** "+chatMessage.getReferences());
			
			this.session.getChannel().sendMessage("Chatroom1", "blah", chatMessage);
			if (doAcknowledgeAfterSending)
			{
				// Acknowledge Message Sent
				AcknowledgeMessageEvent ame = new AcknowledgeMessageEvent(this);
				this.broadcast(ame);
				doAcknowledgeAfterSending = false;
			}

		}
	}

	private double calcTypingTime(String text)
	{
		int x = text.length();
		return Math.log10((x * x) + 1) * typingScale;
	}

	private void doTyping(final MessageEvent me)
	{
		if (session != null)
		{
			currentlyTyping = me.getText();
			doAcknowledgeAfterSending = me.isAcknowledgementExpected();
			DefaultAddOperation addOp = new DefaultAddOperation(new IsTypingInfo(session.getUser()));
			double ms = calcTypingTime(currentlyTyping);
			Timer t = new Timer(ms, new TimeoutAdapter()
			{
				

				public void timedOut(String id)
				{
					// informObservers("<timedout id=\"" + id + "\" />");
					isTyping = false;
					DefaultRemoveOperation removeOp = new DefaultRemoveOperation(new IsTypingInfo(session.getUser()));
					session.getChannel().sendMessage("awareness", "isTyping", removeOp);

					sendReferencingMessage(me);

					currentlyTyping = null;
					if (messageQueue.size() != 0)
					{
						doTyping(messageQueue.remove(0));
					}
				}
			});
			isTyping = true;
			this.session.getChannel().sendMessage("awareness", "isTyping", addOp);
			t.start();
		}
	}

	public void log(String from, String level, String msg)
	{
		if (!(level.equals(Logger.LOG_LOW) || level.equals(Logger.LOG_NORMAL)))
		{
			log(level, from + ": " + msg);
		}
	}

	// private void removeImage() {
	// log(Logger.LOG_NORMAL, "Trying to remove image");
	// Selection s = new Selection(imageStuff);
	// Command command = new DeleteCommand(s);
	// this.session.getChannel().sendMessage("whiteboardDoc", "blah", command);
	// imageStuff = null;
	// hasImage = false;
	// log(Logger.LOG_NORMAL, "Image removed from Whiteboard");
	// }

	private void produceImage(String filename)
	{
		// if (hasImage) {
		// removeImage();
		// }

		log(Logger.LOG_NORMAL, "Trying to display imagefile = " + filename);

		// Get Image Description
		String imgDescription = filename;

		// Get Image Data
		byte[] imgData;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		File imgFile = new File(filename);
		try
		{
			InputStream inStream = new FileInputStream(imgFile);
			byte[] buffer = new byte[256];
			while (true)
			{
				int bytesRead = inStream.read(buffer);
				if (bytesRead == -1)
				{
					break;
				}
				out.write(buffer, 0, bytesRead);
			}
			inStream.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		imgData = out.toByteArray();

		// Get Image Dimensions
		Dimension d = ImageUtilities.getSize(filename);

		log(Logger.LOG_NORMAL, "Image upload : Step 1 Completed - Loaded Image Data");

		// Create a New Graphic description
		Graphic g = new Graphic();
		g.setPoint1(0, 0);
		g.setSize(d.width, d.height);
		g.setProperties(new OutlineProperties());

		// Create the Image Stuff
		ImageStuff imageStuff = new ImageStuff(g, imgDescription, imgData);

		// Get the Image Cache
		ImageCache imageCache = new ImageCache();

		// Encode the Image Stufff
		imageStuff.encodeData(imageCache);

		log(Logger.LOG_NORMAL, "Image upload : Step 2 Completed - Image Stuff Created");

		// Make a command and dispatch
		Command command = new CreateCommand(imageStuff);
		this.session.getChannel().sendMessage("whiteboardDoc", "blah", command);

		log(Logger.LOG_NORMAL, "Image upload : Step 3 Completed - Command Created and Sent");

		// hasImage = true;
		log(Logger.LOG_NORMAL, "Image displayed on Whiteboard");
	}
}
