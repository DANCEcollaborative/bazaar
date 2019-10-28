package edu.cmu.side.view.generic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.FeatureMetricPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.FastListModel;
import edu.cmu.side.view.util.SelectPluginList;

public abstract class GenericMetricChecklistPanel<E extends FeatureMetricPlugin> extends AbstractListPanel{

	FastListModel pluginsModel = new FastListModel();
	SelectPluginList pluginsList = new SelectPluginList();

	Recipe localRecipe;
	
	public GenericMetricChecklistPanel(){
		setLayout(new RiverLayout(10, 5));
		pluginsModel = new FastListModel();
		Map<E, Map<String, Boolean>> evalPlugins = getEvaluationPlugins();
		ArrayList pluginsToPass = new ArrayList();
		for(E plug : evalPlugins.keySet()){
			pluginsToPass.add(plug);
			Map<String, Boolean> opts = new TreeMap<String, Boolean>();
			for(Object s : plug.getAvailableEvaluations().keySet()){
				opts.put(s.toString(), false);
				CheckBoxListEntry entry = new CheckBoxListEntry(s, false);
				entry.addItemListener(getCheckboxListener());
				pluginsToPass.add(entry);					
			}
			evalPlugins.put(plug, opts);
		}
		pluginsModel.addAll(pluginsToPass.toArray());			
		pluginsList.setModel(pluginsModel);

		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(combo.getSelectedItem() != null){
					setTargetAnnotation(combo.getSelectedItem().toString());					
				}
				Workbench.update(GenericMetricChecklistPanel.this);
			}
		});
		//combo.setRenderer(new AbbreviatedComboBoxCellRenderer(30));
		JLabel nameLabel = new JLabel("Evaluations to Display:");
		nameLabel.setBorder(BorderFactory.createEmptyBorder(6,0,6,0));
		add("left", nameLabel);
		add("br left", new JLabel("Target:"));
		add("hfill", combo);
		describeScroll = new JScrollPane(pluginsList);
		add("br hfill vfill", describeScroll);
	}

	public void refreshPanel(Recipe recipe)
	{
		
		super.refreshPanel();
		FeatureTable table = null;
		
		if(recipe != null)
		{
			table = recipe.getTrainingTable();
		}
		
		if(recipe != localRecipe){
			localRecipe = recipe;
			Set<String> keysNew = new TreeSet<String>();
			if(table != null)
			{
				for(String s : table.getLabelArray())
				{
					keysNew.add(s);
				}
			}
			Feature.Type activeType = (table == null?null: table.getClassValueType());
			Workbench.reloadComboBoxContent(combo, keysNew, (keysNew.size()>0?keysNew.toArray(new String[0])[0]:null));
			E plug = null;
			for(int i = 0; i < pluginsModel.getSize(); i++)
			{
				if(pluginsModel.get(i) instanceof FeatureMetricPlugin)
					plug = (E)pluginsModel.get(i);
				
				if(plug != null && pluginsModel.get(i) instanceof CheckBoxListEntry)
				{

					CheckBoxListEntry check = ((CheckBoxListEntry)pluginsModel.get(i));
					String label = check.getValue().toString();
					//Map<E, Map<String, Boolean>> evalPlugins = getEvaluationPlugins();
					//for (E plug : evalPlugins.keySet())
					{
						Collection<Feature.Type> types = (Collection<Feature.Type>) plug.getAvailableEvaluations().get(label);
						
						if (types != null && activeType != null && !plug.canEvaluateRecipe(recipe, label))
						{
							check.setSelected(false);
							check.setEnabled(false);
						}
						else
						{
							check.setEnabled(true);
						}

					}
				}
			}
			
			pluginsModel.fireContentsChanged(this, 0, pluginsModel.size());
			revalidate();
			repaint();
		}
	}
	
	public abstract ItemListener getCheckboxListener();

	public abstract Map<E, Map<String, Boolean>> getEvaluationPlugins();
	
	public abstract void setTargetAnnotation(String s);
}
