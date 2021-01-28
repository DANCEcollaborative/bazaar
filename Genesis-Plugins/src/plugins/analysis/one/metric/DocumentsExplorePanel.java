package plugins.analysis.one.metric;

import java.awt.Font;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;

import plugins.analysis.one.display.SingleDocumentHighlight;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.view.util.AbstractListPanel;


public abstract class DocumentsExplorePanel extends AbstractListPanel {

	Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
	protected Set<SingleDocumentHighlight> visiblePlugins = new TreeSet<SingleDocumentHighlight>();
	
	protected boolean showLabels;
	
	public DocumentsExplorePanel(){
		this(true);
	}
	public DocumentsExplorePanel(boolean label){
		setLayout(new RiverLayout());
		showLabels = label;
	}

	@Override
	public abstract void refreshPanel();
	
	public void refreshPanel(Set<SingleDocumentHighlight> highlights){
		if(!highlights.equals(visiblePlugins)){
			visiblePlugins = highlights;
			this.removeAll();
			for(SingleDocumentHighlight highlight : highlights){
				if(showLabels){
					JLabel label = new JLabel("Document");
					label.setFont(font);
					this.add("br left", label);					
				}
				this.add("br hfill", highlight.getUI(this));
			}
			this.revalidate();
			this.repaint();
		}
	}
}
