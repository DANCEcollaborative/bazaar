package plugins.analysis.two.matrix;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import plugins.analysis.one.display.DocumentDisplayExplorePanel;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.util.AbstractListPanel;

public class DifferenceInstancePanel extends AbstractListPanel{

	enum Limiter{
		BASELINE,
		COMPETING,
		BOTH;
		
		public List<Integer> getDisplayedIndices(List<Integer> base, List<Integer> comp){
			ArrayList<Integer> toPass = new ArrayList<Integer>();
			switch(this){
			case BASELINE:
				for(Integer b : base){
					if(!comp.contains(b)){
						toPass.add(b);
					}
				}
				break;
			case COMPETING:
				for(Integer c : comp){
					if(!base.contains(c)){
						toPass.add(c);
					}
				}
				break;
			case BOTH:
				for(Integer b : base){
					if(comp.contains(b)){
						toPass.add(b);
					}
				}
				break;
			}
			return toPass;
		}
	}
	ButtonGroup buttons = new ButtonGroup();
	
	Limiter limit = Limiter.BOTH;
	DifferenceDocumentsModel baselineModel = new DifferenceDocumentsModel();
	DifferenceDocumentsModel competingModel = new DifferenceDocumentsModel();
		
	DocumentDisplayExplorePanel explore = new DocumentDisplayExplorePanel(){
		@Override
		public void refreshPanel(){
			if(CompareModelsControl.hasBaselineTrainedModelRecipe() && CompareModelsControl.hasCompetingTrainedModelRecipe()){
				Recipe rec = CompareModelsControl.getBaselineTrainedModelRecipe();
				Recipe comp = CompareModelsControl.getCompetingTrainedModelRecipe();
				refreshPanel(rec, limit.getDisplayedIndices(baselineModel.safeGetDisplayList(rec.getTrainingResult()), competingModel.safeGetDisplayList(comp.getTrainingResult())));				
			}
		}
	};
	
	public void setLimiter(Limiter l){
		limit = l;
		refreshPanel();
	}
	
	public DifferenceInstancePanel(){
		setLayout(new RiverLayout());
		JRadioButton baselineOnly = new JRadioButton("Only Baseline Model Cell", false);
		JRadioButton competingOnly = new JRadioButton("Only Competing Model Cell", false);
		JRadioButton bothModels = new JRadioButton("Present in Both Cells", true);
		
		buttons.add(baselineOnly);
		baselineOnly.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(((JRadioButton)e.getSource()).isSelected()){
					setLimiter(Limiter.BASELINE);
				}
			}
		});
		buttons.add(competingOnly);
		competingOnly.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(((JRadioButton)e.getSource()).isSelected()){
					setLimiter(Limiter.COMPETING);
				}
			}
		});
		buttons.add(bothModels);
		bothModels.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(((JRadioButton)e.getSource()).isSelected()){
					setLimiter(Limiter.BOTH);
				}
			}
		});
		add("center", baselineOnly);
		add("center", competingOnly);
		add("center", bothModels);
		add("br hfill vfill", explore);
	}
	
	@Override
	public void refreshPanel(){
		if(CompareModelsControl.hasBaselineTrainedModelRecipe()){
			baselineModel.updateHighlights(CompareModelsControl.getBaselineTrainedModelRecipe().getTrainingResult());		
		}else{
			baselineModel.updateHighlights(null);
		}
		if(CompareModelsControl.hasCompetingTrainedModelRecipe()){
			competingModel.updateHighlights(CompareModelsControl.getCompetingTrainedModelRecipe().getTrainingResult());	
		}else{
			competingModel.updateHighlights(null);
		}
		
		explore.refreshPanel();
	}
}
