package plugins.analysis.one.display;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.view.util.AbstractListPanel;

public class DocumentDisplayExplorePanel extends AbstractListPanel
{

	JPanel documentsPanel = new JPanel(new RiverLayout());
	JCheckBox highlightFeatureButton = new JCheckBox("Highlight feature hits");
	JPanel controlPanel = new JPanel(new RiverLayout());

	ActionListener refreshButtonListener = new ActionListener()
	{

		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			Workbench.update(this);
		}

	};

	public DocumentDisplayExplorePanel()
	{
//		controlPanel.add("br left", highlightFeatureButton);
//		highlightFeatureButton.addActionListener(refreshButtonListener);

		setLayout(new BorderLayout());
		add(BorderLayout.NORTH, controlPanel);
		add(BorderLayout.CENTER, new JScrollPane(documentsPanel));
		
		GenesisControl.addListenerToMap(refreshButtonListener, this);
	}

	boolean updating = false; //FIXME: use Workbench.update properly to avoid this?
	
	// TODO: don't reload already-healthy document panels
	public void refreshPanel(Recipe selected, Collection<Integer> displayedDocumentIndices)
	{
		if(updating)
		{
			System.out.println("already updating!");
			return;
		}
		updating = true;
		
		documentsPanel.removeAll();
		if (selected != null)
		{
			boolean highlightFeature = highlightFeatureButton.isSelected();
			String targetClassLabel = null;
			Feature targetFeature = ExploreResultsControl.getHighlightedFeature();
			TrainingResult result = selected.getTrainingResult();


			for (Integer index : displayedDocumentIndices)
			{
				SingleDocumentHighlight one = new SingleDocumentHighlight(result, index, targetFeature);
				Component c = one.getUI(documentsPanel);
				documentsPanel.add("br hfill", c);
			}

		}
		revalidate();
		repaint();
		
		updating = false;
	}
}
