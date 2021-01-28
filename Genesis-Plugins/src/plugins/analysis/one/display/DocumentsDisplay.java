package plugins.analysis.one.display;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JSplitPane;

import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;

public class DocumentsDisplay extends EvaluateOneModelPlugin{

	static JSplitPane split = new JSplitPane();
	static DocumentDisplayChecklistPanel checklist = new DocumentDisplayChecklistPanel();

	static DocumentsDisplayModel model = new DocumentsDisplayModel(){
		@Override
		public void updateHighlights(TrainingResult select){
			String pred = ExploreResultsControl.getHighlightedColumn();
			String act = ExploreResultsControl.getHighlightedRow();
			Feature f = ExploreResultsControl.getHighlightedFeature();

			ArrayList<Boolean> highlights = new ArrayList<Boolean>();
			ArrayList<Boolean> fHits = (f != null)?getFeatureHighlights(f, select, getFilterFeature(), getReverseFilter()):null;
			ArrayList<Boolean> cHits = (pred != null && act != null)?getCellHighlights(pred, act, select, getFilterCell()):null;
			if(fHits != null && cHits != null){
				for(int i = 0; i < fHits.size(); i++){
					highlights.add(fHits.get(i) && cHits.get(i));
				}
			}else if(fHits != null){
				highlights = fHits;
			}else if(cHits != null){
				highlights = cHits;
			}else{
				for(int i = 0; i < select.getEvaluationTable().getDocumentList().getSize(); i++){
					highlights.add(true);
				}
			}
			checkables.put(select, highlights);
		}
		


		private boolean getReverseFilter()
		{
			return filterFeatureReverse;
		}



		public ArrayList<Boolean> getFeatureHighlights(Feature f, TrainingResult select, boolean filter, boolean reverse)
		{
			ArrayList<Boolean> featureSelect = new ArrayList<Boolean>();
			for (int i = 0; i < checkables.get(select).size(); i++)
			{
				if (reverse && filter)
					featureSelect.add(true);
				else
					featureSelect.add(!filter);
			}
			int update = 0;
			if (filter)
			{
				FeatureTable evaluationTable = select.getEvaluationTable();
				if (f != null && evaluationTable.getHitsForFeature(f) != null)
				{
					Collection<FeatureHit> hits = evaluationTable.getHitsForFeature(f);
					for (FeatureHit hit : hits)
					{
						featureSelect.set(hit.getDocumentIndex(), !reverse);
						update++;
					}
				}
			}
			return featureSelect;
		}
		
		public ArrayList<Boolean> getCellHighlights(String pred, String act, TrainingResult select, boolean filter){
			ArrayList<Boolean> cellSelect = new ArrayList<Boolean>();
			for(int i = 0; i < checkables.get(select).size(); i++){
				cellSelect.add(!filter);
			}
			int update = 0;
			if(filter){
				if(pred != null && act != null){
					List<Integer> cell = select.getConfusionMatrix().get(pred).get(act);
					for(Integer i : cell){
						update++;
						cellSelect.set(i, true);
					}
				}			
			}
			return cellSelect;
		}
	};
	
	static DocumentDisplayExplorePanel explore = new DocumentDisplayExplorePanel(){
		@Override
		public void refreshPanel(){
			TreeSet<Integer> toPass = new TreeSet<Integer>();
			Recipe rec = ExploreResultsControl.getHighlightedTrainedModelRecipe();
			if(rec != null){
				TrainingResult select = rec.getTrainingResult();
				List<Integer> displayed = model.safeGetDisplayList(select);
				List<Integer> options = model.safeGetChecklistOptions(select);
				for(Integer d : displayed){
					if(options.contains(d)){
						toPass.add(d);
					}
				}
			}
			refreshPanel(rec, toPass);
		}
	};

	public static DocumentsDisplayModel getModel(){
		return model;
	}

	static boolean filterFeatureReverse = false;
	static boolean filterFeature = false;
	static boolean filterCell = false;

	public DocumentsDisplay(){
		checklist.setPreferredSize(new Dimension(0,0));
		explore.setPreferredSize(new Dimension(0,0));
		split.setLeftComponent(checklist);
		split.setRightComponent(explore);
		split.setDividerLocation(300);
	}
	@Override
	public String getOutputName() {
		return "docs";
	}

	public static void setFilterFeature(boolean f){
		filterFeature = f;
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			TrainingResult select = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
			model.updateHighlights(select);
		}
	}
	
	public static void setFilterCell(boolean f){
		filterCell = f;
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			TrainingResult select = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
			model.updateHighlights(select);
		}
	}

	public static void setReverseFilter(boolean selected)
	{
		filterFeatureReverse = selected;
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			TrainingResult select = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
			model.updateHighlights(select);
		}
	}
	
	public static boolean getFilterFeature(){
		return filterFeature;
	}
	
	public static boolean getFilterCell(){
		return filterCell;
	}
	
	@Override
	public String toString(){
		return "Documents Display";
	}

	public static void refreshPanel(){
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			TrainingResult select = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
			if(select != null && !model.containsTrainingResult(select)){
				model.generateForTrainingResult(select);
			}
			model.updateHighlights(select);
		}
		checklist.refreshPanel();
		checklist.revalidate();
		explore.refreshPanel();
		explore.revalidate();
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		refreshPanel();
		return split;
	}


	public static void selectIndex(TrainingResult tr, Integer index, Boolean select){
		model.selectIndex(tr, index, select);
	}

	@Override
	public Map<String, String> generateConfigurationSettings() {
		return new TreeMap<String, String>();
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {

	}

	
}
