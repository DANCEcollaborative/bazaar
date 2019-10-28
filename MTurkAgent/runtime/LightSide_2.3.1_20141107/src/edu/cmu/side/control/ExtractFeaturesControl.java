package edu.cmu.side.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.TableFeatureMetricPlugin;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.view.extract.ExtractCombinedLoadPanel;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.ActionBarTask;
import edu.cmu.side.view.util.ParallelTaskUpdater;
import edu.cmu.side.view.util.Refreshable;
import edu.cmu.side.view.util.SwingUpdaterLabel;

public class ExtractFeaturesControl extends GenesisControl{

	private static Recipe highlightedDocumentList;
	private static Recipe highlightedFeatureTable;
	private static StatusUpdater update = new SwingUpdaterLabel();//new ParallelTaskUpdater(8);
	private static Map<FeaturePlugin, Boolean> featurePlugins;
	private static Map<TableFeatureMetricPlugin, Map<String, Boolean>> tableEvaluationPlugins;
	private static String targetAnnotation;
	private static String selectedClassAnnotation;
	private static Type selectedClassType;
	protected static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	static{
		featurePlugins = new HashMap<FeaturePlugin, Boolean>();
		SIDEPlugin[] featureExtractors = PluginManager.getSIDEPluginArrayByType("feature_hit_extractor");
		boolean selected = true;
		for(SIDEPlugin fe : featureExtractors){
			featurePlugins.put((FeaturePlugin)fe, selected);
			selected = false;
		}
		tableEvaluationPlugins = new HashMap<TableFeatureMetricPlugin, Map<String, Boolean>>();
		SIDEPlugin[] tableEvaluations = PluginManager.getSIDEPluginArrayByType("table_feature_evaluation");
		for(SIDEPlugin fe : tableEvaluations){
			tableEvaluationPlugins.put((TableFeatureMetricPlugin)fe, new TreeMap<String, Boolean>());
		}
	}

//	public static class AddFilesListener implements ActionListener{
//		private AbstractListPanel parentComponent;
//		private FastListModel model;
//		private JFileChooser chooser = new JFileChooser(Workbench.csvFolder);
//
//		public AddFilesListener(AbstractListPanel parentComponent){
//			this.parentComponent = parentComponent;
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) 
//		{
//			chooser.setFileFilter(FileToolkit
//					.createExtensionListFileFilter(new String[] { "csv" }, true));
//			chooser.setMultiSelectionEnabled(true);
//			int result = chooser.showOpenDialog(parentComponent);
//			if (result != JFileChooser.APPROVE_OPTION) {
//				return;
//			}
//			Recipe plan = generateDocumentListRecipe(chooser.getSelectedFiles());
//			
//			DocumentList newDocs = plan.getDocumentList();
//			String label = newDocs.guessTextAndAnnotationColumns();
//			setSelectedClassAnnotation(label);
//			setSelectedClassType(newDocs.guessValueType(label));
//			
//			Workbench.update(RecipeManager.Stage.DOCUMENT_LIST);
//			setHighlightedDocumentListRecipe(plan);
//			Workbench.update(parentComponent);
//		}
//	}
	

	public static Recipe generateDocumentListRecipe(File[] files){
		TreeSet<String> pass = new TreeSet<String>();
		for(File f : files){
			pass.add(f.getAbsolutePath());
		}
		return generateDocumentListRecipe(pass);
	}
	
	public static Recipe generateDocumentListRecipe(File file){
		TreeSet<String> pass = new TreeSet<String>();
		pass.add(file.getAbsolutePath());
		return generateDocumentListRecipe(pass);
	}
	
	public static Recipe generateDocumentListRecipe(Set<String> files){
		DocumentList sdl = new DocumentList(files);
		Recipe plan = Workbench.getRecipeManager().fetchDocumentListRecipe(sdl);
		return plan;
	}
	
	public static Map<FeaturePlugin, Boolean> getFeaturePlugins(){
		return featurePlugins;
	}
	
	public static EvalCheckboxListener getEvalCheckboxListener(Refreshable source){
		return new EvalCheckboxListener(source, tableEvaluationPlugins, ExtractFeaturesControl.getUpdater());
	}
	
	public static Map<TableFeatureMetricPlugin, Map<String, Boolean>> getTableEvaluationPlugins(){
		return tableEvaluationPlugins;
	}
	
	public static void clearTableEvaluationPlugins(){
		for(TableFeatureMetricPlugin p : tableEvaluationPlugins.keySet()){
			tableEvaluationPlugins.put(p, new TreeMap<String, Boolean>());
		}
	}
	
	public static void setTargetAnnotation(String s)
	{
		targetAnnotation = s;
	}
	
	public static String getTargetAnnotation(){
		return targetAnnotation;
	}
	
	public static final ActionListener differentiateTextColumnsListener = new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			boolean differentiate = ((JCheckBox)event.getSource()).isSelected();
			if(highlightedDocumentList != null)
			{
				highlightedDocumentList.getDocumentList().setDifferentiateTextColumns(differentiate);
			}
			Workbench.update(Stage.DOCUMENT_LIST);
		}
		
	};
	
	public static class AnnotationComboListener implements ActionListener
	{
		private ExtractCombinedLoadPanel parentComponent;

		public AnnotationComboListener(ExtractCombinedLoadPanel parentComponent)
		{
			this.parentComponent = parentComponent;
		}

		static boolean updatingCombos = false;
		@Override
		public void actionPerformed(ActionEvent ae)
		{
			if (!updatingCombos)
			{
				updatingCombos = true;
				JComboBox annotationFieldCombo = parentComponent.getAnnotationFieldCombo();
				Object selectedClass = annotationFieldCombo.getSelectedItem();

				if (ExtractFeaturesControl.hasHighlightedDocumentList() && selectedClass != null
						&& ExtractFeaturesControl.getHighlightedDocumentListRecipe().getDocumentList().allAnnotations().containsKey(selectedClass.toString()))
				{
					DocumentList sdl = ExtractFeaturesControl.getHighlightedDocumentListRecipe().getDocumentList();

					JComboBox classTypeCombo = parentComponent.getClassTypeCombo();
					if (ae.getSource() == annotationFieldCombo)
					{
						String annot = selectedClass.toString();
						if (sdl.getTextColumns().contains(annot))
						{
							sdl.setTextColumn(annot, false);
						}

						if(sdl.getPossibleAnn(annot).contains(sdl.getEmptyAnnotationString()))
						{
							parentComponent.setWarning("The column you've picked for the class has missing values.\nPlease correct your data file or pick another column.");
						}
						else
						{
							parentComponent.clearWarning();
						}
						
						setSelectedClassAnnotation(annot);
						Type valueType = sdl.getValueType(annot);
						classTypeCombo.setSelectedItem(valueType);
						setSelectedClassType(valueType);

						Map<String, Boolean> columns = new TreeMap<String, Boolean>();
						for (String s : sdl.allAnnotations().keySet())
						{
							if (!getSelectedClassAnnotation().equals(s)) columns.put(s, false);
						}
						for (String s : sdl.getTextColumns())
						{
							columns.put(s, true);
						}
						parentComponent.reloadCheckBoxList(columns);
						Workbench.update(RecipeManager.Stage.DOCUMENT_LIST);
						Workbench.update(parentComponent);
					}
					else if (ae.getSource() == classTypeCombo)
					{
						Type classType = (Type) classTypeCombo.getSelectedItem();
//						sdl.setClassValueType(classType);
						setSelectedClassType(classType);
					}
					
				}
			}
			updatingCombos = false;
		}
	}
	
	
	public static class BuildTableListener implements ActionListener{
		
		private ActionBar actionBar;
		private JTextField threshold;
		private JTextField name;
		
		public BuildTableListener(ActionBar action, JTextField thr, JTextField n){
			actionBar = action;
			threshold = thr;
			name = n;
		}
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			Collection<FeaturePlugin> plugins = new TreeSet<FeaturePlugin>();
			for (FeaturePlugin plugin : ExtractFeaturesControl.getFeaturePlugins().keySet())
			{
				if (ExtractFeaturesControl.getFeaturePlugins().get(plugin))
				{
					plugins.add(plugin);
				}
			}
			int thresh = 1;
			try
			{
				thresh = Integer.parseInt(threshold.getText());
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(threshold, "Threshold value is not an integer!", "Warning", JOptionPane.WARNING_MESSAGE);
			}

			Recipe newRecipe = Recipe.addPluginsToRecipe(getHighlightedDocumentListRecipe(), plugins);
			ExtractFeaturesControl.BuildTableTask task = new ExtractFeaturesControl.BuildTableTask(actionBar, newRecipe, name.getText(), thresh);
			task.executeActionBarTask();
		}
		
	}

	private static class BuildTableTask extends ActionBarTask
	{
		
		Recipe plan;
		String name;
		Integer threshold;
		FeaturePlugin activeExtractor =  null;
		Exception ex;
		
		public BuildTableTask(ActionBar action, Recipe newRecipe, String n, int t){
			super(action);
			plan = newRecipe;
			name = n;
			threshold = t;

		}

		@Override
		protected void finishTask()
		{
			super.finishTask();

			if(ex == null && !halt)
			{
				setHighlightedFeatureTableRecipe(plan);
				RestructureTablesControl.setHighlightedFeatureTableRecipe(plan);
				BuildModelControl.setHighlightedFeatureTableRecipe(plan);
				Workbench.getRecipeManager().addRecipe(plan);
			}
			else if(ex != null && ex.getMessage() != null && ex.getMessage().equals("User Canceled"))
			{
				JOptionPane.showMessageDialog(null, "Feature Extraction Canceled.", "User Cancelled", JOptionPane.INFORMATION_MESSAGE);
			}
			else
			{
				JOptionPane.showMessageDialog(null, "Feature Extraction Stopped.\n"+((ex==null)?"":ex.getLocalizedMessage()), "User Cancelled", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		@Override
		protected void doTask()
		{	
			try
			{
				// System.out.println("EFC 289: extracting features for new feature table. Annotation "+selectedClassAnnotation+", type "+selectedClassType);
				Collection<FeatureHit> hits = new HashSet<FeatureHit>();
				for (SIDEPlugin plug : plan.getExtractors().keySet())
				{
					if (!halt)
					{
						activeExtractor = (FeaturePlugin) plug;
						hits.addAll(activeExtractor.extractFeatureHits(plan.getDocumentList(), plan.getExtractors().get(plug), update));
					}

				}
				if (!halt)
				{
					update.update("Building Feature Table");
					FeatureTable ft = new FeatureTable(plan.getDocumentList(), hits, threshold, selectedClassAnnotation, selectedClassType);
					ft.setName(name);
					plan.setFeatureTable(ft);
				}
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, "Couldn't finish the feature table.\nSee lightside_log for more details.\n"+e.getLocalizedMessage(),"Feature Failure",JOptionPane.ERROR_MESSAGE);
				System.err.println("Feature Extraction Failed");
				e.printStackTrace();
				ex = e;
			}
		}

		@Override
		public void requestCancel()
		{
			logger.warning("stopping extraction...");
			if(activeExtractor != null && !activeExtractor.isStopped())
				activeExtractor.stopWhenPossible();
		}
	}

	public static Recipe getHighlightedDocumentListRecipe(){
		return highlightedDocumentList;
	}
	
	public static boolean hasHighlightedDocumentList(){
		return highlightedDocumentList != null;
	}
	
	public static boolean hasHighlightedFeatureTable(){
		return highlightedFeatureTable != null;
	}
	
	public static Recipe getHighlightedFeatureTableRecipe(){
		return highlightedFeatureTable;
	}

	public static void setUpdater(StatusUpdater up){
		update = up;
	}
	
	public static StatusUpdater getUpdater(){
		return update;
	}
	
	public static void setHighlightedDocumentListRecipe(Recipe highlight){
		highlightedDocumentList = highlight;
	}


	
	public static void setHighlightedFeatureTableRecipe(Recipe highlight){
		highlightedFeatureTable = highlight;
	}

	public static Type getSelectedClassType()
	{
		return selectedClassType;
	}

	public static void setSelectedClassType(Type targetType)
	{
		ExtractFeaturesControl.selectedClassType = targetType;
	}

	public static String getSelectedClassAnnotation()
	{
		return selectedClassAnnotation;
	}

	public static void setSelectedClassAnnotation(String selectedClassAnnotation)
	{
		ExtractFeaturesControl.selectedClassAnnotation = selectedClassAnnotation;
	}
}
