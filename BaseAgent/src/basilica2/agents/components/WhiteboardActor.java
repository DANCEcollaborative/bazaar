

/**
 *
 * @author dadamson
 */

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
import de.fhg.ipsi.chatblocks2.model.messagebased.ChatMessage;
import de.fhg.ipsi.concertchat.framework.IPersistentSession;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.events.AcknowledgeMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.WhiteboardEvent;
import de.fhg.ipsi.utils.ImageUtilities;
import de.fhg.ipsi.whiteboard.Command;
import de.fhg.ipsi.whiteboard.Graphic;
import de.fhg.ipsi.whiteboard.Selection;
//import de.fhg.ipsi.whiteboard.piece.text.TextStuff;
import de.fhg.ipsi.whiteboard.OutlineProperties;
import de.fhg.ipsi.whiteboard.operation.CreateCommand;
import de.fhg.ipsi.whiteboard.operation.DeleteCommand;
import de.fhg.ipsi.whiteboard.piece.icon.AbstractImageStuff;
import de.fhg.ipsi.whiteboard.piece.image.ImageCache;
import de.fhg.ipsi.whiteboard.piece.image.ImageStuff;
import de.fhg.ipsi.whiteboard.piece.image.URLImageStuff;
import de.fhg.ipsi.whiteboard.piece.image.UploadedImageStuff;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class WhiteboardActor extends ConcertChatActor 
{

	private Map<String, AbstractImageStuff> currentImages = new HashMap<String, AbstractImageStuff>();

    public WhiteboardActor(Agent a, String n) 
    {
        super(a, n, null);
    }
    
    public WhiteboardActor(Agent a, String n, String pf) 
    {
        super(a, n, pf);
    }


    public WhiteboardActor(Agent a, String user, String componentName, String pf)
	{
        super(a, user, componentName, pf);
	}


	@Override
    protected void processEvent(Event e) {
        if (e instanceof WhiteboardEvent)
            handleWhiteboardEvent((WhiteboardEvent) e);
        else
            super.processEvent(e);

    }
    
    //     private void removeImage() {
//        log(Logger.LOG_NORMAL, "Trying to remove image");
//        Selection s = new Selection(imageStuff);
//        Command command = new DeleteCommand(s);
//        this.session.getChannel().sendMessage("whiteboardDoc", "blah", command);
//        imageStuff = null;
//        hasImage = false;
//        log(Logger.LOG_NORMAL, "Image removed from Whiteboard");
//    }
    
    
    private void produceImage(String filename, String key, java.awt.Point location) 
    {
    	removeImage(key);
    	
        log(Logger.LOG_NORMAL, "Trying to display imagefile = " + filename);

        //Get Image Description
        String imgDescription = filename;

        //Get Image Data
        byte[] imgData;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        File imgFile = new File(filename);
        try {
            InputStream inStream = new FileInputStream(imgFile);
            byte[] buffer = new byte[256];
            while (true) {
                int bytesRead = inStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
            inStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        imgData = out.toByteArray();

        //Get Image Dimensions
        Dimension d = ImageUtilities.getSize(filename);

        log(Logger.LOG_NORMAL, "Image upload : Step 1 Completed - Loaded Image Data");

        //Create a New Graphic description
        Graphic g = new Graphic();
        if(location != null)
            g.setPoint1(location.x, location.y);
        
        g.setSize(d.width, d.height);
        g.setProperties(new OutlineProperties());

        //Create the Image Stuff
        ImageStuff imageStuff = new ImageStuff(g, imgDescription, imgData);

        //Get the Image Cache
        ImageCache imageCache = new ImageCache();

        //Encode the Image Stufff
        imageStuff.encodeData(imageCache);

        log(Logger.LOG_NORMAL, "Image upload : Step 2 Completed - Image Stuff Created");

        //Make a command and dispatch
        Command command = new CreateCommand(imageStuff);
        this.getChannel().sendMessage("whiteboardDoc", "blah", command);

        log(Logger.LOG_NORMAL, "Image upload : Step 3 Completed - Command Created and Sent");

        //hasImage = true;
        log(Logger.LOG_NORMAL, "Image displayed on Whiteboard");
        
        currentImages.put(key, (AbstractImageStuff)imageStuff);
    }

    private void removeImage(String imageName) 
    {
    	if(currentImages.containsKey(imageName))
    	{
    		try
    		{
		    	AbstractImageStuff imageStuff = currentImages.get(imageName);
		        log(Logger.LOG_NORMAL, "Trying to remove image");
		        Selection s = new Selection(imageStuff);
		        Command command = new DeleteCommand(s);
		        this.session.getChannel().sendMessage("whiteboardDoc", "blah", command);
		        imageStuff = null;
		        log(Logger.LOG_NORMAL, "Image removed from Whiteboard");
    		}
    		catch(Exception e)
    		{

				log(Logger.LOG_ERROR,"could not delete"+imageName);
    		}
    	}
    }
    
    private void handleWhiteboardEvent(WhiteboardEvent we) 
    {
        if(!we.delete)
        {
            this.produceImage(we.filename, we.key, we.location);
        }
        else
        {
        	this.removeImage(we.filename);
        }
    }
}
   
