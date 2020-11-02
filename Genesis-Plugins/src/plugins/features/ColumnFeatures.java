package plugins.features;

import java.awt.Component;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.ParallelFeaturePlugin;

public class ColumnFeatures extends ParallelFeaturePlugin
{

	public static final String CONVERT_TO_BINARY = "EXPAND TO BOOLEAN";

	public static final String COLUMN_FEATURE_SUFFIX = "__column";

	ColumnFeaturesPanel panel = new ColumnFeaturesPanel()
	{
		@Override
		public DocumentList updateDocumentList()
		{
			if (ExtractFeaturesControl.hasHighlightedDocumentList())
			{
				return ExtractFeaturesControl.getHighlightedDocumentListRecipe().getDocumentList();
			}
			else
				return null;
		}
	};

	protected Map<String, String> selectedColumns = new TreeMap<String, String>();

	public void selectColumn(String s, String type)
	{
		selectedColumns.put(s, type);
	}

	public void clearSelections()
	{
		selectedColumns.clear();
	}

	@Override
	public String toString()
	{
		return "Column Features";
	}

	@Override
	public String getOutputName()
	{
		return "columns";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		panel.refreshPanel();
		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> selected = panel.getSelectedColumns();
		Map<String, String> settings = new TreeMap<String, String>();
		for (String s : selected.keySet())
		{
			settings.put(s, selected.get(s));
		}
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		selectedColumns.clear();
		for (String s : settings.keySet())
		{
			selectColumn(s, settings.get(s));
		}
	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i)
	{
		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		for (String selected : selectedColumns.keySet())
		{
			String featureName = selected + COLUMN_FEATURE_SUFFIX;
			
			Feature f ;
			Object value;
			if(selected.equals(ColumnFeaturesPanel.INCLUDE_FILENAMES_FLAG))
				value = documents.getFilenameList().get(i);
			else
				value = getValue(documents.getAnnotationArray(selected).get(i));
			String valString = value.toString();
			
			String typeString = selectedColumns.get(selected); 
			Type type;
			if(typeString.equals(CONVERT_TO_BINARY))
			{
				type = Type.BOOLEAN;
				f = getFeature(type, featureName+"="+valString, selected, documents);
				
				value = true;
			}
			else
			{
				type = Type.valueOf(typeString);

				if(type == Type.NOMINAL && selected.equals(ColumnFeaturesPanel.INCLUDE_FILENAMES_FLAG))
					f = Feature.fetchFeature(getOutputName(), featureName, documents.getFilenames(), this);
				else
					f = getFeature(type, featureName, selected, documents);
			}

			if (!valString.equals(documents.getEmptyAnnotationString())) switch (type)
			{
				case NOMINAL:
					FeatureHit nom = new FeatureHit(f, valString, i);
					hits.add(nom);
					break;

				case BOOLEAN:
					if (!Boolean.FALSE.equals(value))
					{
						FeatureHit bol = new FeatureHit(f, value, i);
						hits.add(bol);
					}
					break;

				case NUMERIC:
					double val;
					try
					{
						val = Double.parseDouble(valString);
						FeatureHit num = new FeatureHit(f, val, i);
						hits.add(num);
					}
					catch(NumberFormatException e)
					{
						logger.warning("Unparseable numeric feature for document "+i+": '"+valString+"'");
					}
					break;

			}
		}
		return hits;
	}

	public Feature.Type columnFeatureType(Feature.Type type)
	{
		return type;
	}

	
	public Feature getFeature(Feature.Type type, String featureName, String selected, DocumentList documents)
	{
		Feature f = null;
		switch (type)
		{
			case NOMINAL:
				f=Feature.fetchFeature(getOutputName(), featureName, type);
				
				if(f == null)
				{
					f = Feature.fetchFeature(getOutputName(), featureName,  documents.getPossibleAnn(selected), this);
				}
				break;
			case BOOLEAN:
				f = Feature.fetchFeature(getOutputName(), featureName, Feature.Type.BOOLEAN, this);
				break;
			case NUMERIC:
				f = Feature.fetchFeature(getOutputName(), featureName, Feature.Type.NUMERIC, this);
				break;
		}
		return f;
	}

	public Object getValue(String val)
	{
		return val;
	}
}
