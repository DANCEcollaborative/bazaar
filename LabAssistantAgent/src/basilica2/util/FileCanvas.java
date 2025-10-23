package basilica2.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class FileCanvas
{
	private Graphics2D g2d;
	private BufferedImage image;
	
	public FileCanvas(int w, int h)
	{
		    image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		    g2d = image.createGraphics();
	}
	
	public Graphics2D getGraphics()
	{
		return g2d;
	}
	
	public boolean writeToFile(String filename, String type)
	{
	    if(!filename.toLowerCase().endsWith("."+type))
	    	filename = filename + "."+type;
	    
	    File file = new File(filename);
	    try
		{
			ImageIO.write(image, type, file);
			return true;
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	public static void main(String[] args)
	{
		FileCanvas fc = new FileCanvas(200, 100);
		Graphics2D g = fc.getGraphics();

		g.setColor(Color.white);
		g.fillRect(0, 0, 200, 100);
		
		g.setColor(Color.yellow);
		g.fillRect(10, 10, 180, 80);
		g.setColor(Color.black);
		g.drawRect(10, 10, 180, 80);
		g.setColor(Color.blue);
		g.drawString("you are great.", 20, 20);
		
		fc.writeToFile("test", "png");
	}
}
