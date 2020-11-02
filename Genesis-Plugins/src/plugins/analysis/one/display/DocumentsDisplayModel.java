package plugins.analysis.one.display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.side.model.data.TrainingResult;

public abstract class DocumentsDisplayModel {

	//Visible in the explore documentsPanel
	protected Map<TrainingResult, List<Boolean>> displayed = new HashMap<TrainingResult, List<Boolean>>();

	//Visible in the checklist documentsPanel
	protected Map<TrainingResult, List<Boolean>> checkables = new HashMap<TrainingResult, List<Boolean>>();

	public void selectIndex(TrainingResult tr, Integer index, Boolean select){
		displayed.get(tr).set(index, select);
	}

	public abstract void updateHighlights(TrainingResult select);

	public List<Integer> safeGetDisplayList(TrainingResult select){
		return safeGetList(select, displayed);
	}
	 	
	public List<Integer> safeGetChecklistOptions(TrainingResult select){
		return safeGetList(select, checkables);
	}
	
	public List<Integer> safeGetList(TrainingResult select, Map<TrainingResult, List<Boolean>> map){
		if(!map.containsKey(select)){
			generateForTrainingResult(select);
		}
		ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
		for(int i = 0 ; i < map.get(select).size(); i++){
			if(map.get(select).get(i)){
				selectedIndices.add(i);
			}
		}
		return selectedIndices;
	}

	public void generateForTrainingResult(TrainingResult select){
		ArrayList<Boolean> booleans = new ArrayList<Boolean>();
		ArrayList<Boolean> vis = new ArrayList<Boolean>();
		if(select != null){
			for(int i = 0; i < select.getEvaluationTable().getSize(); i++){
				booleans.add(false);
				vis.add(true);
			}			
		}
		displayed.put(select, booleans);
		checkables.put(select, vis);
	}

	public boolean containsTrainingResult(TrainingResult result){
		return displayed.containsKey(result);
	}
	
	
}
