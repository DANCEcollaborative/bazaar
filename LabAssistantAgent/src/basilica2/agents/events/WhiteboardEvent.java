/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import basilica2.util.FileCanvas;

/**
 *
 * @author dadamson
 */
public class WhiteboardEvent extends Event  implements Serializable 
{
     public String filename;
     public String key;
     public boolean delete;
     public Point location;
     public String author;

    public WhiteboardEvent(Component parent, String imageFilename, boolean delete)
    {
        super(parent);
        this.filename = imageFilename;
        this.key = filename;
        this.delete = delete;
    }

    public WhiteboardEvent(Component parent, String imageFilename, String key, String author)
    {
        super(parent);
        this.filename = imageFilename;
        this.key = key;
        this.author = author;
    }


    public WhiteboardEvent(Component parent, String imageFilename, String key)
    {
        super(parent);
        this.filename = imageFilename;
        this.key = key;
    }

    
    public WhiteboardEvent(Component parent, String imageFilename, String key, java.awt.Point location)
    {
        this(parent, imageFilename, key);
        this.location = location;
    }
    
    public WhiteboardEvent(Component parent, String imageFilename, java.awt.Point location)
    {
        this(parent, imageFilename, false);
        this.location = location;
    }
    
    public static WhiteboardEvent deleteWhiteboardImageEvent(Component parent, String key)
    {
    	return new WhiteboardEvent(parent, key, true);
    }
    
    @Override
    public String getName() 
    {
        return "Whiteboard Event";
    }

    @Override
    public String toString() 
    {
        return (delete?"REMOVE":"CREATE")+" whiteboard-image "+filename;
    }


	public static WhiteboardEvent makeWhiteboardMessage(String message, String messageName, Component source)
	{
		FileCanvas fc = new FileCanvas(200, 50);
		Graphics2D g = fc.getGraphics();
	
		g.setColor(Color.white);
		g.fillRect(0, 0, 200, 50);
		
		g.setColor(new Color(255, 255, 192));
		g.fillRect(5, 5, 190, 40);
		g.setColor(Color.black);
		g.drawRect(5, 5, 190, 40);
		g.setFont(g.getFont().deriveFont(16.0f).deriveFont(Font.BOLD));
		g.setColor(Color.blue);
		g.drawString(message, 20, 35);
		
		String filename = "images"+File.separator+messageName+".png";
		
		fc.writeToFile(messageName, "png");
		
		return new WhiteboardEvent(source, filename, messageName, new Point(20, 20));
	}


	public static WhiteboardEvent makeWhiteboardImage(String path, String key, Point loc, Component source, boolean delete)
	{
		if(delete)
			return new WhiteboardEvent(source, key, delete);
		else
			return new WhiteboardEvent(source, path, key, loc);
	}

}
