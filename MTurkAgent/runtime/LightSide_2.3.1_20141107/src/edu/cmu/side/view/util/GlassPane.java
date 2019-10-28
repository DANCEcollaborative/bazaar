package edu.cmu.side.view.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.view.extract.ExtractFeaturesPane;


/**
 * We have to provide our own glass pane so that it can paint.
 */
public class GlassPane extends JComponent
implements ItemListener {
	Point point;

	Container contentPane;
	//React to change button clicks.
	@Override
	public void itemStateChanged(ItemEvent e) {
	}

	@Override
	protected void paintComponent(Graphics g) {
		Font f = new Font(Font.SANS_SERIF, Font.BOLD, 30);
		g.setFont(f);
		Component c = ((JTabbedPane)contentPane).getSelectedComponent();

		if(c instanceof ExtractFeaturesPane){
			boolean hasDocs = ExtractFeaturesControl.hasHighlightedDocumentList();
			boolean hasFeats = ExtractFeaturesControl.hasHighlightedFeatureTable();
			if(!hasDocs){
				for(Component c2 : ((ExtractFeaturesPane)c).getComponents()){
					if(c2 instanceof JSplitPane){
						for(Component c3 : ((JSplitPane)c2).getComponents()){
//							if(c3 instanceof JSplitPane){
//								int offsetX = 13; // FIXME
//								int offsetY = 35;
//
//								Component c4 = ((JSplitPane)c3).getRightComponent();
//								g.fillRect(c4.getX()+offsetX, c4.getY()+offsetY, c4.getWidth(), c4.getHeight());
//								String warning = "Load Documents to Extract Features";
//								g.setColor(Color.white);
//								g.drawChars(warning.toCharArray(), 0, warning.length(), c4.getX()+offsetX+10, c4.getY()+offsetY+(c4.getHeight()/2));
//								g.setColor(new Color(128, 128, 128, 128));
//							}
						}
					}
				}
			}
//			if(!hasFeats){
//				for(Component c2 : ((ExtractFeaturesPane)c).getComponents()){
//					if(c2 instanceof JSplitPane){
//						int offsetX = 11; // FIXME
//						int offsetY = 33;
//
//						Component c4 = ((JSplitPane)c2).getBottomComponent();
//						g.fillRect(c4.getX()+offsetX, c4.getY()+offsetY, c4.getWidth(), c4.getHeight());
//						String warning = "Extract Features to View Table";
//						g.setColor(Color.white);
//						g.drawChars(warning.toCharArray(), 0, warning.length(), c4.getX()+offsetX+10, c4.getY()+offsetY+(c4.getHeight()/2));
//						g.setColor(Color.gray);
//
//					}
//				}
//			}
		}
	}

	public void setPoint(Point p) {
		point = p;
	}

	public GlassPane(Container c) {
		contentPane = c;
		CBListener listener = new CBListener(this, contentPane);
		addMouseListener(listener);
		addMouseMotionListener(listener);
	}
	@Override
	public boolean contains(int x, int y)
	{
		Component[] components = getComponents();
		for(int i = 0; i < components.length; i++)
		{
			Component component = components[i];
			Point containerPoint = SwingUtilities.convertPoint(
					this,
					x, y,
					component);
			if(component.contains(containerPoint))
			{
				return true;
			}
		}
		return false;
	}
}

/**
 * Listen for all events that our check box is likely to be
 * interested in.  Redispatch them to the check box.
 */
class CBListener extends MouseInputAdapter {
	Toolkit toolkit;
	GlassPane glassPane;
	Container contentPane;

	public CBListener(GlassPane glassPane, Container contentPane) {
		toolkit = Toolkit.getDefaultToolkit();
		this.glassPane = glassPane;
		this.contentPane = contentPane;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		redispatchMouseEvent(e, false);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		redispatchMouseEvent(e, true);
	}

	//A basic implementation of redispatching events.
	private void redispatchMouseEvent(MouseEvent e,
			boolean repaint) {
		Point glassPanePoint = e.getPoint();
		Container container = contentPane;
		Point containerPoint = SwingUtilities.convertPoint(
				glassPane,
				glassPanePoint,
				contentPane);
		if (containerPoint.y >= 0) {
			//The mouse event is probably over the content pane.
			//Find out exactly which component it's over.  
			Component component = 
					SwingUtilities.getDeepestComponentAt(
							container,
							containerPoint.x,
							containerPoint.y);

			if (component != null) {
				//Forward events over the check box.
				Point componentPoint = SwingUtilities.convertPoint(
						glassPane,
						glassPanePoint,
						component);
				component.dispatchEvent(new MouseEvent(component,
						e.getID(),
						e.getWhen(),
						e.getModifiers(),
						componentPoint.x,
						componentPoint.y,
						e.getClickCount(),
						e.isPopupTrigger()));
			}
		}

		//Update the glass pane if requested.
		glassPane.setPoint(glassPanePoint);
		glassPane.repaint();
	}
}