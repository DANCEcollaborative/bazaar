package plugins.restructure;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import plugins.features.ColumnFeaturesPanel;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.RegroupFeatureHit;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.view.generic.GenericLoadCSVPanel;
import edu.cmu.side.view.generic.GenericLoadPanel;

public class RegroupPlugin extends RestructurePlugin{

	JPanel panel = new JPanel(new RiverLayout());
	JComboBox classValue = new JComboBox();
	JComboBox classType = new JComboBox();
	JCheckBox countCheckBox = new JCheckBox("Add merge count as feature");
	String selectedClass;
	Type selectedType = Type.NOMINAL;
	Collection<String> selectedColumns;
	Recipe additionalColumns;
	boolean shouldAddMergeCountFeature;
	
	Collection<String> ignoreColumns = Arrays.asList("timestamp");

	GenericLoadPanel loadNewLabels = new GenericLoadCSVPanel("New Annotations to Add:", true, true, false, false)
	{

		@Override
		public void setHighlight(Recipe r)
		{
			if (r != null)
			{
//				System.out.println("Loaded RP45");
				additionalColumns = r;
				String[] anns = r.getDocumentList().getAnnotationNames();
				Workbench.reloadComboBoxContent(classValue, r.getDocumentList().allAnnotations().keySet(), anns.length > 0 ? anns[0] : null);
			}
		}
		
		@Override
		public Recipe getHighlight()
		{
			return additionalColumns;
		}

		@Override
		public void deleteHighlight()
		{
			additionalColumns = null;
			
		}

//		@Override
//		public void refreshPanel()
//		{
//			ArrayList<Recipe> recipes = new ArrayList<Recipe>();
//			Workbench.reloadComboBoxContent(combo, recipes, getHighlight());
//		}
//
//		@Override
//		public void loadNewItem()
//		{
//			loadNewDocumentsFromCSV();
//		}

	};

	ColumnFeaturesPanel columns = new ColumnFeaturesPanel("Columns Specifying Instances:", true){
		@Override
		public DocumentList updateDocumentList() {
			if(RestructureTablesControl.hasHighlightedFeatureTable()){
				return RestructureTablesControl.getHighlightedFeatureTableRecipe().getDocumentList();				
			}else return null;
		}
	};
	
	public RegroupPlugin(){
		panel.add("br hfill", columns);
		panel.add("br hfill", loadNewLabels);
		panel.add("br left", new JLabel("Predict Annotation:"));
		panel.add("hfill", classValue);
		panel.add("br left", new JLabel("Annotation Type:   "));
		panel.add("hfill", classType);
		panel.add("br fhill", countCheckBox);
		columns.setPreferredSize(new Dimension(200,150));
		classValue.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent ae)
			{	
				if(classValue != null && classValue.getSelectedItem() != null)
				{
					selectedClass = classValue.getSelectedItem().toString();
					
					if(additionalColumns != null)
					{
						selectedType = additionalColumns.getDocumentList().getValueType(selectedClass);
						classType.setSelectedItem(selectedType);
					}
				}
			}
		});
		classType.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent ae){	
				if(classType != null && classType.getSelectedItem() != null){
					selectedType = (Type) classType.getSelectedItem();
				}
			}
		});
		countCheckBox.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				shouldAddMergeCountFeature = countCheckBox.isSelected();
			}});
		
		classType.setModel(new DefaultComboBoxModel(new Type[]{Type.NOMINAL, Type.NUMERIC}));
		classType.setSelectedItem(Type.NOMINAL);
	}
	@Override
	protected FeatureTable restructureWithMaskForSubclass(
			FeatureTable original, boolean[] mask, int threshold, 
			StatusUpdater progressIndicator) {
		return generateFeatureTable(selectedColumns, selectedClass, selectedType, mask, original, threshold, progressIndicator);
	}

	@Override
	protected FeatureTable restructureTestSetForSubclass(FeatureTable original,
			FeatureTable test, int threshold, StatusUpdater progressIndicator) {
		boolean[] mask = new boolean[test.getSize()];
		for(int i = 0; i < mask.length; i++) mask[i] = true;
		return generateFeatureTable(selectedColumns, selectedClass, selectedType, mask, test, threshold, progressIndicator);
	}

	public FeatureTable generateFeatureTable(Collection<String> selCols, String selClass, Type selType, boolean[] mask, FeatureTable original,int threshold, 
			StatusUpdater update)
	{
		Map<Integer, Integer> newIndices = new HashMap<Integer, Integer>(); //from old doclist index to new doclist index
		Map<Integer, Set<Integer>> oldIndices = new TreeMap<Integer, Set<Integer>>(); //from new doclist index to old doclist indices
		Map<String, Integer> labelsDict = new HashMap<String, Integer>(); //from regroup key to new doclist indices 
		Map<Integer, String> labelsReverseDict = new TreeMap<Integer, String>(); //from new doclist index to regroup key

		//from regroup key to instance value to new index
		Map<String, Map<String, Collection<Integer>>> selectedColumnsMapping = new HashMap<String, Map<String, Collection<Integer>>>();

		int indexCount = 0;
		DocumentList originalDocuments = original.getDocumentList();
		for (int i = 0; i < original.getSize(); i++)
		{
			if (!mask[i]) continue;
			update.update("Mapping", i, original.getSize());
			String label = "";
			for (String s : selCols)
			{
				String mappingKey = "";
				String mappingValue = "";
				if (s.equals(ColumnFeaturesPanel.INCLUDE_FILENAMES_FLAG))
				{
					mappingKey = "Filenames";
					mappingValue = originalDocuments.getFilename(i);
				}
				else
				{
					mappingKey = s;
					mappingValue = originalDocuments.getAnnotationArray(s).get(i);
				}
				label += mappingValue + ",";
			}
			if (!labelsDict.containsKey(label))
			{
				labelsDict.put(label, indexCount);
				labelsReverseDict.put(indexCount, label);
				oldIndices.put(indexCount, new HashSet<Integer>());
				//System.out.println(label + " added as label, index count " + indexCount + " RP116");
				indexCount++;
			}

			int index = labelsDict.get(label); //new index
			newIndices.put(i, index);
			oldIndices.get(index).add(i);

			for (String s : selCols)
			{
				String mappingKey = "";
				String mappingValue = "";
				if (s.equals(ColumnFeaturesPanel.INCLUDE_FILENAMES_FLAG))
				{
					mappingKey = "Filenames";
					mappingValue = originalDocuments.getFilename(i).substring(originalDocuments.getFilename(i).lastIndexOf("/") + 1);
				}
				else
				{
					mappingKey = s;
					mappingValue = originalDocuments.getAnnotationArray(s).get(i);
				}
				if (!selectedColumnsMapping.containsKey(mappingKey))
				{
					selectedColumnsMapping.put(mappingKey, new HashMap<String, Collection<Integer>>());
				}
				if (!selectedColumnsMapping.get(mappingKey).containsKey(mappingValue))
				{
					selectedColumnsMapping.get(mappingKey).put(mappingValue, new HashSet<Integer>());
				}
				//System.out.println(mappingKey + " Key " + mappingValue + " Value - Index " + index);
				selectedColumnsMapping.get(mappingKey).get(mappingValue).add(index);
			}
		}

		String classToSelect = null; //new annotation class

		//from new index (i) to annotation key to annotation value (for new regrouped doc)
		Map<Integer, Map<String, Comparable>> newAnnotations = new TreeMap<Integer, Map<String, Comparable>>();
		for (Integer i : oldIndices.keySet())
		{
			update.update("Re-annotating", i, oldIndices.size());
			newAnnotations.put(i, new HashMap<String, Comparable>()); 
			for (String ann : originalDocuments.getAnnotationNames())
			{
				if (!selCols.contains(ann) && !ignoreColumns.contains(ann))
				{
					switch (originalDocuments.getValueType(ann))
					{
						case BOOLEAN:
						case NOMINAL:
						case STRING:
							Map<String, Set<String>> annotations = new HashMap<String, Set<String>>();
							annotations.put(ann, new HashSet<String>());
							for (Integer old : oldIndices.get(i))
							{
								annotations.get(ann).add(originalDocuments.getAnnotationArray(ann).get(old));
							}
							for (String poss : originalDocuments.getPossibleAnn(ann))
							{
								String key = ann + ":" + poss;
								if (classToSelect == null && selClass != null && selClass.equals(ann))
								{
									classToSelect = key;
								}
								newAnnotations.get(i).put(key, annotations.get(ann).contains(poss));
							}
							break;
						case NUMERIC:
							double total = 0.0;
							for (Integer old : oldIndices.get(i))
							{
								total += original.getNumericConvertedClassValue(old, ann);
							}
							String totStr = ann + ":TOTAL";
							newAnnotations.get(i).put(totStr, total);
							double mean = total / (0.0 + oldIndices.size());
							String meanStr = ann + ":MEAN";
							newAnnotations.get(i).put(meanStr, mean);
							if (classToSelect == null && selClass.equals(ann))
							{
								classToSelect = totStr;
							}
							break;
					}
				}
			}
		}

		//from annotation label to per-document list of annotation values
		Map<String, List<String>> finalAnnotations = new HashMap<String, List<String>>();
		
		//per-document list of combined text
		List<String> finalText = new ArrayList<String>();

		//per-document list of originating filenames
		List<String> restructFilenames = new ArrayList<String>();
		//int[][] docTextOffsets = new int[indexCount][];
		
		//per-document list of old-document-per-new-document counts (stored as strings to make doclist's job easier?)
		ArrayList<String> mergeCount = new ArrayList<String>();
		
		for(int i = 0; i < indexCount; i++)
		{
			update.update("Filling regrouped instances",i,indexCount);
			StringBuilder thisText = new StringBuilder();

			for (String key : newAnnotations.get(i).keySet()) //add regrouped annotations
			{
				if (!finalAnnotations.containsKey(key))
				{
					finalAnnotations.put(key, new ArrayList<String>());
				}
				finalAnnotations.get(key).add(newAnnotations.get(i).get(key).toString());
			}

			int numMerged = oldIndices.get(i).size(); //add merged filenames
			if(numMerged > 0)
			{
				restructFilenames.add(originalDocuments.getFilename(oldIndices.get(i).toArray(new Integer[0])[0]));				
			}

			mergeCount.add(""+numMerged);
			
			//System.out.println("RP 256: numMerged = "+numMerged);
			
			//docTextOffsets[i] = new int[numMerged];
			for(Integer old : oldIndices.get(i)) //add merged text
			{
				//System.out.println("RP 261: i="+i+" old="+old);
				//docTextOffsets[i][old] = thisText.length(); //FIXME: make new indices make sense for features
				thisText.append(originalDocuments.getPrintableTextAt(old) + "\n");
			}
			finalText.add(thisText.toString());
		}


		//feature hits for new regrouped documents
		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		Collection<String> bonusColumnFeatureNames = new ArrayList<String>();//Arrays.asList("cluster_weka"); //TODO: give this a UI.
		
		if(additionalColumns != null)
		{
			DocumentList bonusAnnotations = additionalColumns.getDocumentList();
			System.out.println("RP 268 annotations: "+Arrays.toString(bonusAnnotations.getAnnotationNames()));
			//System.out.println("RP 273: selectedColumnsMapping="+selectedColumnsMapping.keySet());
			for(int bonusRow = 0; bonusRow < bonusAnnotations.getSize(); bonusRow++)
			{		
				Set<Integer> remaining = new TreeSet<Integer>(); //indices of regrouped instances matching this bonus row
				boolean start = true;
				for(String sel : selCols)
				{
					//System.out.println("RP 275: additional i="+i+" sel="+sel);
					//System.out.println("RP279: annotationArray = "+moreAnnotations.getAnnotationArray(sel));
					
					String label = bonusAnnotations.getAnnotationArray(sel).get(bonusRow);
					Collection<Integer> potential = selectedColumnsMapping.get(sel).get(label);
					
					//System.out.println("RP 282: potential["+sel+"]["+label+"]="+(potential == null ? "null" : potential.size()));
					if (potential != null)
					{
						if (start)
						{
							remaining.addAll(potential);
							start = false;
						}
						else
						{
							remaining.retainAll(potential);
						}
					}
				}
				
//				for(Integer r : remaining){ System.out.print(r + ", "); }
//				System.out.println();
				for (String ann : bonusAnnotations.getAnnotationNames())
				{
					if (selClass.equals(ann))
					{
						classToSelect = ann;
					}
					if (!finalAnnotations.containsKey(ann)) //pre-populate finalAnnotations with blanks
					{
						ArrayList<String> strings = new ArrayList<String>();
						String blank = "";
						for (int j = 0; j < finalText.size(); j++)
						{
							strings.add(blank);
						}
						finalAnnotations.put(ann, strings);
					}
					
					Feature bonusColumnFeature = Feature.fetchFeature("regroup-bonus", ann, bonusAnnotations.getPossibleAnn(ann), this);
					
					for (Integer rem : remaining) //for instances that actually carry over from the bonus columns
					{
						String bonusAnnotationValue = bonusAnnotations.getAnnotationArray(ann).get(bonusRow);
						finalAnnotations.get(ann).set(rem, bonusAnnotationValue); 
						
						if(bonusColumnFeatureNames.contains(ann))
						{
							FeatureHit bonusColumnFeatureHit = new FeatureHit(bonusColumnFeature, bonusAnnotationValue, rem);
							hits.add(bonusColumnFeatureHit);
						}
					}
				}
			}
		}
		

		DocumentList docs = new DocumentList(finalText, finalAnnotations);
		docs.setFilenames(restructFilenames);

		if(classToSelect != null && docs.allAnnotations().keySet().contains(classToSelect))
		{
			docs.setCurrentAnnotation(classToSelect, selectedType);
		}
		
		Feature mergeCountFeature = Feature.fetchFeature("regroup", "mergeCount", Type.NUMERIC, this);
		
		for(int i = 0; i < original.getSize(); i++)
		{
			if(!mask[i]) continue;
			update.update("Duplicating features",i,original.getSize());
			for(FeatureHit oldHit : original.getHitsForDocument(i))
			{
				FeatureHit newHit = new RegroupFeatureHit(oldHit, newIndices);//, docTextOffsets[i]); //FIXME: update indices for merged documents
				hits.add(newHit);
			}
			
			if(shouldAddMergeCountFeature)
			{
				hits.add(new FeatureHit(mergeCountFeature, mergeCount.get(i), i));
			}
			
		}

		FeatureTable newTable = new FeatureTable(docs, hits, threshold, classToSelect, selType);

		newTable.getDocumentList().addAnnotation("mergeCount", mergeCount, false);
		
		try{
			update.update("Printing merge counts to file");
			BufferedWriter out = new BufferedWriter(new FileWriter("line_counts.csv"));
			StringBuilder sb = new StringBuilder("line,count");
			for(String s : finalAnnotations.keySet()){
				sb.append(","+s);
			}
			sb.append("\n");
			for(int i = 0; i < mergeCount.size(); i++){
				sb.append(i+","+mergeCount.get(i));
				for(String s : finalAnnotations.keySet()){
					sb.append(","+finalAnnotations.get(s).get(i));
				}
				sb.append("\n");
			}
			out.write(sb.toString());
			out.close();
		}catch(Exception e){ e.printStackTrace(); }
		update.update("Merge counts printed.");
		
		
		return newTable;
	}

	@Override
	public String toString(){
		return "Regroup Instances";
	}

	@Override
	public String getOutputName() {
		return "regroup";
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		refreshPanel();
		return panel;
	}

	public void refreshPanel(){
		columns.refreshPanel();
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			FeatureTable table = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
			DocumentList documents = table.getDocumentList();
			if(documents.getAnnotationNames().length>0){
				Workbench.reloadComboBoxContent(classValue, documents.getAnnotationNames(), table.getAnnotation());						
			}
			else{
				Workbench.reloadComboBoxContent(classValue, new ArrayList<String>(), null);						
			}
		}else{
			Workbench.reloadComboBoxContent(classValue, new ArrayList<String>(), null);	
		}
	}

	@Override
	public Map<String, String> generateConfigurationSettings() {
		TreeMap<String, String> highlights = new TreeMap<String, String>();
		String selectedColumns = "";
		for(String s : columns.getSelectedColumns().keySet()){
			selectedColumns += s + ",";
		}
		highlights.put("class", selectedClass);
		highlights.put("type", selectedType.name());
		highlights.put("diff", selectedColumns);
		return highlights;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {
		selectedClass = settings.get("class");
		selectedType = Type.valueOf(settings.get("type"));
		shouldAddMergeCountFeature = Boolean.TRUE.toString().equals(settings.get("shouldAddMergeCountFeature"));
		
		selectedColumns = new ArrayList<String>();
		String[] labs = settings.get("diff").split(",");
		for(String l : labs) selectedColumns.add(l);
			
	}

}
