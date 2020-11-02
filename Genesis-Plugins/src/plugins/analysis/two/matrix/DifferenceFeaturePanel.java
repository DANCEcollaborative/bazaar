package plugins.analysis.two.matrix;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;

import plugins.metrics.features.models.ErrorAnalysisFeatureMetrics;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.SwingUpdaterLabel;

public class DifferenceFeaturePanel extends AbstractListPanel{


	ActionBar action = new ActionBar(new SwingUpdaterLabel()){
		@Override
		public void startedTask() {}
		@Override
		public void endedTask() {}
	};
	
	DifferenceFeatureMetricPanel metrics = new DifferenceFeatureMetricPanel(action);
	
	DifferenceMatrixPanel avgDifference = new DifferenceMatrixPanel(DifferenceMatrixPlugin.getEvaluator(), ErrorAnalysisFeatureMetrics.avg, "Average Difference:");
	DifferenceMatrixPanel freqDifference = new DifferenceMatrixPanel(DifferenceMatrixPlugin.getEvaluator(), ErrorAnalysisFeatureMetrics.freq, "Frequency Difference:");
	

	ButtonGroup subsetSelect = new ButtonGroup();
	JRadioButton onlyBaseline = new JRadioButton("Only Baseline Model Cell");
	JRadioButton onlyCompete = new JRadioButton("Only Competing Model Cell");

	public DifferenceFeaturePanel(){
		setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane();
		JPanel matrixPanel = new JPanel(new GridLayout(2,1));
		JPanel leftPanel = new JPanel(new BorderLayout());
		JPanel radioPanel = new JPanel(new RiverLayout());
		
		GenesisControl.addListenerToMap(metrics, this);
		
		subsetSelect.add(onlyBaseline);
		subsetSelect.add(onlyCompete);
		radioPanel.add("left", onlyBaseline);
		radioPanel.add("br left", onlyCompete);
		avgDifference.setPreferredSize(new Dimension(200,150));
		freqDifference.setPreferredSize(new Dimension(200,150));
		matrixPanel.add(avgDifference);
		matrixPanel.add(freqDifference);
		leftPanel.add(BorderLayout.NORTH, radioPanel);
		leftPanel.add(BorderLayout.CENTER, metrics);
		metrics.setPreferredSize(new Dimension(250,400));
		matrixPanel.setPreferredSize(new Dimension(225,400));
		split.setLeftComponent(leftPanel);
		split.setRightComponent(matrixPanel);
		add(BorderLayout.CENTER, split);
		
	}
	
	@Override
	public void refreshPanel(){
		if(CompareModelsControl.hasBaselineTrainedModelRecipe()){
			String calc = onlyBaseline.isSelected()?"baseline":(onlyCompete.isSelected()?"compete":null);
			DifferenceMatrixPlugin.setCalculateOption(calc);
			if(DifferenceMatrixPlugin.getHighlightedColumn() != null && DifferenceMatrixPlugin.getHighlightedRow() != null){
				metrics.refreshPanel();
				avgDifference.refreshPanel();
				freqDifference.refreshPanel();				
			}
		}
	}
	
}
