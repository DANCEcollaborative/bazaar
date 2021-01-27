package plugins.restructure.multi.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JScrollPane;

import plugins.restructure.GrowthModel.FeatureSource;
import plugins.restructure.GrowthModel.PluginFeatureSource;
import plugins.restructure.GrowthModel.SingleFeatureSource;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.GenesisControl.PluginCheckboxListener;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.FeatureFetcher;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.FeatureTableModel;
import edu.cmu.side.view.util.SIDETable;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class MultilevelFeaturesPanel extends AbstractListPanel {

	SIDETable display = new SIDETable();
	FeatureTableModel model = new FeatureTableModel();

	Map<FeatureTable, Map<FeatureSource, Boolean>> featureSets = new HashMap<FeatureTable, Map<FeatureSource, Boolean>>();

	public MultilevelFeaturesPanel(){
		setLayout(new RiverLayout());
		add("left", new JLabel("Select Features in Level:"));
		display.addMouseListener(new ToggleMouseAdapter(display, true){
			@Override
			public void setHighlight(Object row, String col) {}
		});
		describeScroll = new JScrollPane(display);
		add("br hfill vfill", describeScroll);
		add.setText("All");
		delete.setText("None");
		add.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(int i = 0; i < model.getRowCount(); i++){
					if(model.getValueAt(i, 0) instanceof CheckBoxListEntry){
						((CheckBoxListEntry)model.getValueAt(i, 0)).setSelected(true);
					}
				}
			}
		});
		delete.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(int i = 0; i < model.getRowCount(); i++){
					if(model.getValueAt(i, 0) instanceof CheckBoxListEntry){
						((CheckBoxListEntry)model.getValueAt(i, 0)).setSelected(false);
					}
				}
			}
		});
		add("br center", add);
		add("center", delete);
	}

	public Collection<FeatureSource> getHighlightedFeatureNames(FeatureTable table){
		Collection<FeatureSource> result = new TreeSet<FeatureSource>();
		for(FeatureSource s : featureSets.get(table).keySet()){
			if(featureSets.get(table).get(s)) result.add(s);
		}
		return result;
	}
	
	
	
	@Override
	public void refreshPanel(){
		model = new FeatureTableModel();
		model.addColumn("Feature Source");
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			Recipe recipe = RestructureTablesControl.getHighlightedFeatureTableRecipe();
			FeatureTable table = recipe.getTrainingTable();
			if(!featureSets.containsKey(table)){
				Map<FeatureSource, Boolean> objects = new TreeMap<FeatureSource, Boolean>();
				
				for(SIDEPlugin extractor : recipe.getExtractors().keySet())
				{
					objects.put(new PluginFeatureSource((FeatureFetcher) extractor), false);
				}
				
				for(Feature f : table.getFeatureSet())
				{
					if(f.getExtractorPrefix().equals("columns"))
						objects.put(new SingleFeatureSource(f), false);
				}
//				for(String s : table.getDocumentList().getAnnotationNames())
//				{
//					Feature f = Feature.fetchFeature("growth_column", s, Feature.Type.NOMINAL, null);	
//					objects.put(new SingleFeatureSource(f), false);
//				}
				featureSets.put(table, objects);
			}
			PluginCheckboxListener<FeatureSource> eval = new PluginCheckboxListener<FeatureSource>(this, featureSets.get(table));
			for(FeatureSource s : featureSets.get(table).keySet()){
				Object[] row = new Object[1];
				CheckBoxListEntry entry = new CheckBoxListEntry(s, false);
				entry.addItemListener(eval);
				row[0] = entry;
				model.addRow(row);
			}
		}
		display.setModel(model);
		display.setDefaultRenderer(Object.class, new MultilevelCellRenderer());
		revalidate();
	}
}
