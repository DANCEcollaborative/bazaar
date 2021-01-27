package plugins.features;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.view.util.Refreshable;
import edu.stanford.nlp.util.StringUtils;

public class PredictedAnnotationFeatures extends FeaturePlugin implements Refreshable
{
	JTextArea infoText = new JTextArea();
	JCheckBox usePredictedBox = new JCheckBox("Use labels from trained model");
	JLabel warning = new JLabel("Not suitable for randomized cross-validation.");
	Component configPanel;
	
	
	private boolean usePredictedLabels = false;
	private String annotationName;
	private String featureName;
	private Type featureType;
	private Collection<String> nominalValues;
	
	
	public PredictedAnnotationFeatures()
	{
		configPanel = createUI();
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, this);
	}

	// TODO: support unlabeled data
	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();

		Feature feature;

		if (featureType == Type.NOMINAL)
		{
			feature = Feature.fetchFeature("pre", featureName, nominalValues, this);
		}
		else
			feature = Feature.fetchFeature("pre", featureName, featureType, this);

		List<String> labels = documents.getAnnotationArray(annotationName);
		
		String defaultValue = "";
		if(labels == null)
		{
				System.out.println("PAF 94: No class labels.");
				switch(featureType)
				{
					case NUMERIC:
						defaultValue = "0.0";
						break;
					case BOOLEAN:
						defaultValue = "false";
						break;
					case NOMINAL:
						defaultValue = nominalValues.iterator().next();
						break;
				}
		}
		
		int size = documents.getSize();
		for (int i = 1; i < size; i++)
		{
			update.update("Learning from the past to repeat its mistakes", i, size);
			FeatureHit prevLabelHit;
			Object value;
			
			if(labels != null)
				value = labels.get(i - 1);
			else
				value = defaultValue;
			
			if(featureType == Type.NUMERIC)
			{
				value = Double.parseDouble((String) value);
			}
			
			if (usePredictedLabels)
				prevLabelHit = new PredictingFeatureHit(feature, value, i);
			else
				prevLabelHit = new FeatureHit(feature, value, i);
			hits.add(prevLabelHit);
		}

		return hits;
	}

	@Override
	public String getOutputName()
	{
		return "pre";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return configPanel;
	}

	/**
	 * @return
	 */
	protected Component createUI()
	{
		JPanel panel = new JPanel(new RiverLayout());

		infoText.setText("Previous instance's class value,\nfor sequential data.");
		infoText.setEditable(false);
		infoText.setBackground(null);

		panel.add("hfill", infoText);
		panel.add("br left", usePredictedBox);
		panel.add("br left", warning);
		warning.setIcon(new ImageIcon("toolkits/icons/error.png"));
		warning.setVisible(false);

		usePredictedBox.setToolTipText("<html>Prediction/evaluation on a trained model <br>" + "will use the model's own predictions from the <br>"
				+ "previous instance as a feature, instead of the <br>" + "gold standard labels.</html>");
		usePredictedBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean selected = usePredictedBox.isSelected();
				usePredictedLabels = selected;
				warning.setVisible(selected);
			}
		});

		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("usePredictedLabels", usePredictedLabels + "");
		map.put("annotationName", annotationName);
		map.put("featureName", featureName);
		map.put("featureType", featureType.toString());
		map.put("nominalValues", StringUtils.join(nominalValues, ","));
		return map;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		usePredictedLabels = settings.get("usePredictedLabels").equals("true");
		usePredictedBox.setSelected(usePredictedLabels);
		annotationName = settings.get("annotationName");
		featureName = settings.get("featureName");
		featureType = Type.valueOf(settings.get("featureType"));
		nominalValues = Arrays.asList(settings.get("nominalValues").split(","));
	}

	@Override
	public String toString()
	{
		return "Previous Label Features";
	}

	@Override
	public void refreshPanel()
	{
		Recipe recipe = ExtractFeaturesControl.getHighlightedDocumentListRecipe();
		if (recipe != null)
		{
			DocumentList documents = recipe.getDocumentList();
			String className = ExtractFeaturesControl.getSelectedClassAnnotation();
			infoText.setText("Use the previous instance's '" + className + "' label\nas a feature for sequential data.");
			

			if(featureName == null)
			{
				annotationName = ExtractFeaturesControl.getSelectedClassAnnotation();
				featureType = documents.getValueType(annotationName);
				if (featureType == Type.NOMINAL)
				{
					nominalValues = documents.getPossibleAnn(annotationName);
					String annonationString = StringUtils.join(nominalValues, "_");	
					featureName = "prev_" + annotationName + "_" + annonationString;
				}
				else
				{
					featureName = "prev_" + annotationName;
				}
			}
			// TODO:done? feature names are not unique between documentLists.
		}
		else
		{
			annotationName = null;
			featureName = null;
			featureType = null;
			nominalValues = null;
		}
	}

	public static void main(String[] args)
	{
		JFrame freddy = new JFrame("Test!");
		freddy.setContentPane((JPanel) new PredictedAnnotationFeatures().createUI());
		freddy.setPreferredSize(new Dimension(300, 300));
		freddy.setVisible(true);
		freddy.pack();
	}

	static class PredictingFeatureHit extends FeatureHit
	{
		Object goldValue;
		boolean valid = true;

		public PredictingFeatureHit(Feature feature, Object value, int documentIndex)
		{
			super(feature, value, documentIndex);
			goldValue = value;
		}

		@Override
		public void prepareForTraining(int fold, Map<Integer, Integer> foldsMap, FeatureTable table)
		{
			int i = documentIndex;
			valid = i > 0 && !foldsMap.get(i - 1).equals(fold);
			value = goldValue;

			// System.out.println("preparing "+this+" for training");
		}

		@Override
		public void prepareToPredict(int fold, Map<Integer, Integer> foldsMap, FeatureTable newData, List<? extends Object> predictions)
		{
			int i = documentIndex;
			valid = i > 0 && foldsMap.get(i - 1).equals(fold);
			if (valid)
			{
				value = predictions.get(i - 1);
				// System.out.println("preparing "+this+" for prediction (gold "+goldValue+", using "+value+")");
			}
		}

		@Override
		public boolean isValid()
		{
			return valid;
		}

	}

}
