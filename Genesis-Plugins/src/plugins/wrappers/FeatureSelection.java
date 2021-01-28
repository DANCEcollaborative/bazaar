package plugins.wrappers;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import plugins.learning.WekaTools;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.Ranker;
import weka.attributeSelection.ReliefFAttributeEval;
import weka.core.Instances;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.WrapperPlugin;
import edu.cmu.side.view.util.DefaultMap;
import edu.cmu.side.view.util.Refreshable;
import edu.stanford.nlp.util.StringUtils;

public class FeatureSelection extends WrapperPlugin implements Refreshable
{

	private static final String FEATURE_JOINER = "\tFeature_Selected,\t";
	JCheckBox activeCbx = new JCheckBox("Feature Selection");
	JLabel label = new JLabel("#:");
	JTextField num = new JTextField(4);
	boolean active = false;
	int limit = -1;

	AttributeSelection selector = new AttributeSelection();
	Set<Feature> selectedFeatures = new HashSet<Feature>();
	protected JButton warningButton = new JButton();


	public FeatureSelection()
	{
//		super();
//		selector.setEvaluator(new ChiSquaredAttributeEval());
		num.setVisible(false);
		label.setVisible(false);
		//TODO: replace with WarningButton utility class.
		warningButton.setIcon(new ImageIcon("toolkits/icons/error.png"));
		warningButton.setBorderPainted(false);
		warningButton.setOpaque(false);
		warningButton.setContentAreaFilled(false);
		warningButton.setVisible(false);

		warningButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(panel, warningButton.getToolTipText(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		});

		activeCbx.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent arg0) 
			{
				boolean clicked = ((JCheckBox)arg0.getSource()).isSelected();
				num.setVisible(clicked);
				label.setVisible(clicked);
				active=clicked;
				BuildModelControl.getWrapperPlugins().put(FeatureSelection.this, clicked);
				verify();
			}
		});


		num.getDocument().addDocumentListener(new DocumentListener()
		{

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				verify();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				verify();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				verify();
			}

			
		});
		panel.add("left", activeCbx);
		panel.add("left", label);
		panel.add("left", num);
		panel.add("left", warningButton);
		
		GenesisControl.addListenerToMap(Stage.FEATURE_TABLE, this);
	}

	public boolean verify()
	{
		if(!active || BuildModelControl.getHighlightedFeatureTableRecipe() == null)
		{
			warningButton.setVisible(false);
			return true;
		}
		try
		{
			int selectNum = Integer.parseInt(num.getText());
			if(BuildModelControl.hasHighlightedFeatureTableRecipe()){
				int size = BuildModelControl.getHighlightedFeatureTableRecipe().getTrainingTable().getFeatureSet().size();
				if (selectNum > 0 && selectNum <= size)
				{
					warningButton.setVisible(false);
					warningButton.setToolTipText("Select a number of features between 1 and " + size);
					limit = selectNum;
					return true;
				}				
			}else{
				warningButton.setVisible(false);
			}
		}
		catch (NumberFormatException e)
		{
			warningButton.setToolTipText("Input a valid number");
		}

		limit = -1;
		warningButton.setVisible(true);
		return false;
	}
	
//	FeatureTable lastTable = null;
//	static DefaultMap<Feature, Integer> magics = new DefaultMap<Feature, Integer>(0);
	
	@Override
	synchronized public void learnFromTrainingData(FeatureTable train, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{
		int currentLimit = limit < train.getFeatureSet().size() ? limit : -1;
		Collection<Integer> selectedAttributes = new HashSet<Integer>();
		Instances instances = WekaTools.getInstances(train, fold, foldsMap, true);
		selectedFeatures.clear();

		Map<Feature, Integer> attributeMap = WekaTools.getAttributeMap(train);

		if(train.getClassValueType() == Type.NUMERIC)
		{
			//CfsSubsetEval evaluator = new CfsSubsetEval();
			ReliefFAttributeEval evaluator = new ReliefFAttributeEval();
			int samples = Math.min(1000, instances.size());
			try
			{
				evaluator.setSampleSize(samples);
				evaluator.setSigma(Math.max(2, samples/10));
			}
			catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			selector.setEvaluator(evaluator);
//			GreedyStepwise searcher = new GreedyStepwise();
//			searcher.setNumToSelect(currentLimit);
//			BestFirst searcher = new BestFirst();
//			try
//			{
//				searcher.setSearchTermination(currentLimit);
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//			}

			Ranker searcher = new Ranker();
			searcher.setNumToSelect(currentLimit);
			selector.setSearch(searcher);
			
			logger.info("FS fold "+fold+": using "+searcher.getClass().getSimpleName()+" search with evaluator "+evaluator.getClass().getSimpleName()+" for numeric class attribute selection.");
			
			selector.setSearch(searcher);
		}
		else
		{
//			logger.info("FS: using Ranked Chi-Squared Evaluation for nominal class attribute");
			selector.setEvaluator(new ChiSquaredAttributeEval());
			Ranker ranker = new Ranker();
			ranker.setNumToSelect(currentLimit);
			selector.setSearch(ranker);
		}
		try
		{
			selector.SelectAttributes(instances);
			for (Integer i : selector.selectedAttributes())
			{
				selectedAttributes.add(i);
			}

			// convert back to featuretable space, you can't trust the WekaTools cache very far
			for (Feature f : train.getFeatureSet()) 
			{
				if (selectedAttributes.contains(attributeMap.get(f)))
				{
					selectedFeatures.add(f);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

//		synchronized(FeatureSelection.class)
//		{
//			//FIXME: kill this
//
//			if(lastTable != null && !train.equals(lastTable))
//			{
//				magics.clear();
//			}
//			lastTable = train;
//			
//			for(Feature f : selectedFeatures)
//			{
//				magics.put(f, magics.get(f)+1);
//			}
//	
//			logger.info("COMMON FEATURES:");
//			for(Feature f : magics.keySet())
//			{
//				int count = magics.get(f);
//				if(count > 6)
//				{
//					logger.info(count+"\t"+f);
//				}
//			}
//		}
		
		logger.info("FS 205: selected " + selectedFeatures.size() + " features from table " + train.getName() + ", fold " + fold);
		//logger.info(StringUtils.join(selectedFeatures, "\n"));

	}

	@Override
	synchronized public FeatureTable wrapTableForSubclass(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{
			Collection<FeatureHit> newHits = new HashSet<FeatureHit>();
			
			Set<Feature> featureSet = table.getFeatureSet();
			
			for(Feature f : selectedFeatures)
			{
				if(featureSet.contains(f))
				{
					for (FeatureHit hit : table.getHitsForFeature(f))
					{
						newHits.add(hit);
					}
				}
			}
			FeatureTable newFeatureTable;
			if(table.getAnnotation() != null){
				newFeatureTable = new FeatureTable(table.getDocumentList(), newHits, 0, table.getAnnotation(), table.getClassValueType(), table.getLabelArray());
			}else{
				newFeatureTable = new FeatureTable(table.getDocumentList(), newHits, 0, table.getAnnotation(), table.getClassValueType());
			}
				
			//logger.info("FS 206: old = "+table.getFeatureSet().size()+", new = "+newFeatureTable.getFeatureSet().size());

			newFeatureTable.reconcileFeatures(selectedFeatures);
			
			//logger.info("FS 210: old = "+table.getFeatureSet().size()+", reconciled = "+newFeatureTable.getFeatureSet().size());

			newFeatureTable.setName("FeatureSelection("+table.getName()+", n="+selectedFeatures.size()+", fold="+fold+")");
			return newFeatureTable;

	}

	@Override
	public PredictionResult wrapResultForSubclass(PredictionResult result,
			int fold, Map<Integer, Integer> foldsMap, StatusUpdater update) {
		return result;
	}

	@Override
	public String getOutputName() {
		return "select";
	}
	
	@Override
	public String toString()
	{
		return "Feature Selection";
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		if(BuildModelControl.hasHighlightedFeatureTableRecipe()){
			Feature.Type activeClassType = BuildModelControl.getHighlightedFeatureTableRecipe().getTrainingTable().getClassValueType();
			switch(activeClassType){
			case NOMINAL:
			case BOOLEAN:
			case STRING:
				activeCbx.setSelected(active);
				activeCbx.setEnabled(true);
				num.setEnabled(true);
				break;
			case NUMERIC:
				activeCbx.setSelected(false);
				activeCbx.setEnabled(false);
				num.setText("");
				num.setEnabled(false);
				break;
			}
		}
		return panel;
	}

	@Override
	synchronized public Map<String, String> generateConfigurationSettings() {
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("active", ""+ activeCbx.isSelected());
		settings.put("num", ""+limit);
		
		StringBuilder featureBuilder = new StringBuilder();
		
		for(Feature f : selectedFeatures)
		{
			featureBuilder.append(f.encode());
			featureBuilder.append(FEATURE_JOINER);
		}
		settings.put("features", featureBuilder.toString());
		
		return settings;
	}

	@Override
	synchronized public void configureFromSettings(Map<String, String> settings) {
		active = settings.containsKey("active") && Boolean.TRUE.toString().equals(settings.get("active"));
		//if (active)
		{
			try
			{
				limit = Integer.parseInt(settings.get("num"));
			}
			catch (Exception e)
			{
				limit = -1;
			}
			if(settings.containsKey("features"))
			{
				selectedFeatures.clear();
				for(String feature : settings.get("features").split(FEATURE_JOINER))
				{
					if(!feature.isEmpty())
					try
					{
						selectedFeatures.add(Feature.fetchFeature(feature));
					}
					catch(IllegalStateException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}


	@Override
	public boolean settingsMatch(Map<String, String> settingsA, Map<String, String> settingsB)
	{
		for(String key : settingsA.keySet())
		{
			if(!key.equals("features") && !settingsA.get(key).equals(settingsB.get(key)))
				return false;
		}
		return true;
	}
	
	@Override
	public void refreshPanel()
	{
		verify();
	}

	public Set<Feature> getSelectedFeatures()
	{
		return selectedFeatures;
	}

}
