package edu.cmu.side.view.explore;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.view.generic.GenericMatrixPanel;
import edu.cmu.side.view.util.ToggleMouseAdapter;
import edu.cmu.side.view.util.RadioButtonListEntry;

public class ExploreMatrixPanel extends GenericMatrixPanel{

	private ButtonGroup toggleButtons;

	public ExploreMatrixPanel(){
		super();
		label.setText("Cell Highlight:");
		this.getDisplayTable().setCellSelectionEnabled(false);
		this.getDisplayTable().addMouseListener(new ToggleMouseAdapter(this.getDisplayTable(), true){

			@Override
			public void setHighlight(Object row, String col) {
				ExploreResultsControl.setHighlightedCell(row.toString(), col);
				Workbench.update(ExploreMatrixPanel.this);
			}
			
		});
	}
	@Override
	public Object getCellObject(Object o){
		RadioButtonListEntry tb = new RadioButtonListEntry(o, false);
		toggleButtons.add(tb);
		return tb;
	}


	@Override
	public void refreshPanel() {
		toggleButtons = new ButtonGroup();
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			refreshPanel(ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult().getConfusionMatrix());				
		}else{
			refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
		}
	}

}
