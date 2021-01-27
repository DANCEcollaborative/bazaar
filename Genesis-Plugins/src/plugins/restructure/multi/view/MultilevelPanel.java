package plugins.restructure.multi.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import plugins.restructure.GrowthModel;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.view.util.AbstractListPanel;

public class MultilevelPanel extends AbstractListPanel{

	MultilevelDomainsPanel domains = new MultilevelDomainsPanel();
	MultilevelFeaturesPanel features = new MultilevelFeaturesPanel();
	MultilevelBottomPanel bottom = new MultilevelBottomPanel();
	
	GrowthModel growthPlugin;
	
	public MultilevelPanel(GrowthModel plugin){
		this.growthPlugin = plugin;
		setLayout(new BorderLayout());
		domains.setPreferredSize(new Dimension(150,180));
		features.setPreferredSize(new Dimension(150,180));
		bottom.setPreferredSize(new Dimension(350,200));
		JPanel grid = new JPanel(new GridLayout(1,2));
		grid.add(domains);
		grid.add(features);
		add.setText("v      Add Domain      v");
		add.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				FeatureTable table = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
				growthPlugin.addFromUI(table, domains.getHighlightedLevels(table), features.getHighlightedFeatureNames(table));
				refreshPanel();
			}
			
		});
		JPanel center = new JPanel(new BorderLayout());
		center.add(BorderLayout.CENTER, grid);
		center.add(BorderLayout.SOUTH, add);
		add(BorderLayout.CENTER, center);
		add(BorderLayout.SOUTH, bottom);
	}
	
	@Override
	public void refreshPanel(){
		domains.refreshPanel();
		features.refreshPanel();
		bottom.refreshPanel();
		add.setEnabled(RestructureTablesControl.hasHighlightedFeatureTable());
		revalidate();
	}
}
