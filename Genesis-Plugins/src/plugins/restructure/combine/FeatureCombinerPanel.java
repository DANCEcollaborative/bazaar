package plugins.restructure.combine;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import plugins.restructure.RestructureFeatureMetricCheckboxPanel;
import plugins.restructure.combine.FeatureCombiner.AndCombiner;
import plugins.restructure.combine.FeatureCombiner.NotCombiner;
import plugins.restructure.combine.FeatureCombiner.OrCombiner;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.view.util.Refreshable;

public class FeatureCombinerPanel extends JPanel implements Refreshable
{
	protected FeatureCombinerPlugin plugin;
	protected RestructureFeatureMetricCheckboxPanel featurePanel;

	Collection<FeatureCombiner> selectedCombiners = new ArrayList<FeatureCombiner>();
	Recipe comboRecipe = null;
	Recipe currentRecipe = null;
	boolean changed = true;
	
	protected RestructureFeatureMetricCheckboxPanel combinationPanel = new RestructureFeatureMetricCheckboxPanel()
	{
		
		{
			this.remove(filterPanel);	
			this.remove(targetLabelLabel);
			this.remove(targetLabelBox);		
		}
		
		@Override
		public void selectedFeaturesChanged()
		{
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public String getTargetAnnotation()
		{
			return featurePanel.getTargetAnnotation();
		}

		@Override
		protected Recipe getFeatureTableRecipe()
		{
			Recipe selectedRecipe = RestructureTablesControl.getHighlightedFeatureTableRecipe();
			if(comboRecipe == null && selectedRecipe != null)
			{	
				comboRecipe = Recipe.fetchRecipe();
				DocumentList docs = selectedRecipe.getDocumentList();
				comboRecipe.setDocumentList(docs);
				FeatureTable features = selectedRecipe.getFeatureTable();
				

//				System.out.println("FCP 60: getting recipe for combiner hits: "+selectedCombiners +" - "+docs.getFilename(0)+" "+features.getSize());
				
				
				Collection<FeatureHit> newHits = plugin.getNewHits(features);
				comboRecipe.setFeatureTable(new FeatureTable(docs, newHits, 0, features.getAnnotation(), features.getClassValueType()));
//				plugin.comboHits = newHits;
			}
			
			return comboRecipe;
		}
		
	};

	public FeatureCombinerPanel(final FeatureCombinerPlugin plugin)
	{
		this.plugin = plugin;
		featurePanel = new RestructureFeatureMetricCheckboxPanel()
		{

			{
				targetLabelBox.addActionListener(new ActionListener()
				{

					@Override
					public void actionPerformed(ActionEvent arg0)
					{
						combinationPanel.refreshPanel();
					}
				});
			}
			
			@Override
			public void selectedFeaturesChanged()
			{
				selectedCombiners = getCombiners();
			}
			
			@Override
			public void refreshPanel()
			{
//				System.out.println("FCP 77: refreshing combo feature panel");
				super.refreshPanel();
				selectedCombiners = getCombiners();
			}
		};
		
//		filterPanel.setLayout(new RiverLayout(0, 5));
		
		JButton andButton = new JButton("AND");
		JButton orButton = new JButton("OR");
		JButton notButton = new JButton("NOT");
		JButton deleteButton = new JButton("Delete");
		
		
		andButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Collection<Feature> selected = getAllSelectedFeatures();
				if(selected.size() > 1)
				{
				comboRecipe = null;
				clearSelections();
				AndCombiner and = new AndCombiner(new ArrayList<Feature>(selected));
				plugin.addCombiner(and);
				combinationPanel.refreshPanel();
				changed = true;
				}
			}
		});


		notButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{

				List<Feature> selected = getAllSelectedFeatures();
				if(selected.size() >= 1)
				{
					comboRecipe = null;
					clearSelections();
					NotCombiner not = new NotCombiner(selected);
					plugin.addCombiner(not);
					combinationPanel.refreshPanel();
					changed = true;
				}
				
			}
		});
		
		orButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{

				Collection<Feature> selected = getAllSelectedFeatures();
				if(selected.size() > 1)
				{
					comboRecipe = null;
					clearSelections();
					OrCombiner or = new OrCombiner(selected);
					plugin.addCombiner(or);
					combinationPanel.refreshPanel();
					changed = true;
				}
			}
		});

		deleteButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				comboRecipe = null;
				clearSelections();
				Collection<Feature> deleteMe = combinationPanel.getSelectedFeatures();
				Iterator<FeatureCombiner> combIt = selectedCombiners.iterator();
				
				while(combIt.hasNext())
				{
					FeatureCombiner combiner = combIt.next();
					if(deleteMe.contains(combiner.getFeature()))
					{
						//hide instead of deleting outright in case there's dependencies
						combiner.setHidden(true); 
					}
				}
				changed = true;
				combinationPanel.refreshPanel();
			}
		});



		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, this);

		JSplitPane featureSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		featurePanel.setPreferredSize(new Dimension(0, 200));
		featurePanel.setMinimumSize(new Dimension(0, 100));
		combinationPanel.setPreferredSize(new Dimension(0, 50));

		featureSplitPane.setTopComponent(featurePanel);
		featureSplitPane.setBottomComponent(combinationPanel);
		featureSplitPane.setBorder(BorderFactory.createEmptyBorder());
//		featureSplitPane.setDividerLocation(100);
		
		this.setLayout(new BorderLayout());
		this.add(featureSplitPane, BorderLayout.CENTER);
		JPanel controlPanel = new JPanel(new RiverLayout());
		controlPanel.setBorder(BorderFactory.createEmptyBorder());
		
		controlPanel.add("br left", andButton);
		controlPanel.add("left", orButton);
		controlPanel.add("left", notButton);
		controlPanel.add("hfill", new JPanel());
		controlPanel.add("right", deleteButton);
		
		this.add(controlPanel, BorderLayout.SOUTH);
		
		this.refreshPanel();
	}

	@Override 
	public void refreshPanel()
	{

		//TODO: move to plugin, maybe
		Recipe selectedRecipe = RestructureTablesControl.getHighlightedFeatureTableRecipe();
		if(currentRecipe != selectedRecipe || changed)
		{
			if(plugin.combinerCache.containsKey(selectedRecipe))
			{
				if(selectedCombiners != plugin.combinerCache.get(selectedRecipe))
				{
					selectedCombiners = plugin.combinerCache.get(selectedRecipe);
					plugin.comboHits = plugin.comboHitCache.get(selectedRecipe);
					comboRecipe = null;
				}
			}
			else
			{
				selectedCombiners = new ArrayList<FeatureCombiner>();
				plugin.comboHits = new ArrayList<FeatureHit>();
				plugin.combinerCache.put(selectedRecipe, selectedCombiners);
				plugin.comboHitCache.put(selectedRecipe, plugin.comboHits);
				comboRecipe = null;
			}
		}
//		
//		if(changed)
//		{
//			featurePanel.clearSelection();
//		}
		
		featurePanel.refreshPanel();
		combinationPanel.refreshPanel();
		
		currentRecipe = selectedRecipe;
		changed = false;
	}

	
	public Collection<FeatureCombiner> getCombiners()
	{
		return plugin.getCombiners();
	}
 
	
	private void clearSelections()
	{
		featurePanel.clearSelection();
		//combinationPanel.clearSelection();
	}
	
	private List<Feature> getAllSelectedFeatures()
	{
		List<Feature> allSelected = new ArrayList<Feature>(featurePanel.getSelectedFeatures());
		allSelected.addAll(combinationPanel.getSelectedFeatures());
		
		return allSelected;
	}
}
