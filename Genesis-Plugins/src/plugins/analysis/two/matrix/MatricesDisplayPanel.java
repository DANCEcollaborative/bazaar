package plugins.analysis.two.matrix;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.table.DefaultTableModel;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.generic.GenericMatrixPanel;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.RadioButtonListEntry;
import edu.cmu.side.view.util.SIDETable;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class MatricesDisplayPanel extends AbstractListPanel{

	GenericMatrixPanel baseMatrix = new GenericMatrixPanel("Baseline Confusion Matrix:") {

		@Override
		public void refreshPanel() {
			if(CompareModelsControl.hasBaselineTrainedModelRecipe()){
				refreshPanel(CompareModelsControl.getBaselineTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
				DifferenceMatrixPlugin.setBaseTable(matrixDisplay);
			}else{
				refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
			}
		}
	};

	GenericMatrixPanel compMatrix = new GenericMatrixPanel("Competing Confusion Matrix:") {

		@Override
		public void refreshPanel() {
			if(CompareModelsControl.hasCompetingTrainedModelRecipe()){
				refreshPanel(CompareModelsControl.getCompetingTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
				DifferenceMatrixPlugin.setCompTable(matrixDisplay);
			}else{
				refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
			}
		}
	};

	GenericMatrixPanel diffMatrix = new GenericMatrixPanel("Difference Confusion Matrix:"){

		@Override
		public void refreshPanel(){
			matrixModel = new DefaultTableModel();
			ButtonGroup confusionCells = new ButtonGroup();
			try{
				sum = new Double[]{0.0,0.0};
				SIDETable base = DifferenceMatrixPlugin.getBaseTable();
				SIDETable comp = DifferenceMatrixPlugin.getCompTable();
				if(base != null && comp != null && base.getColumnCount() == comp.getColumnCount()){
					Collection<String> labels = new TreeSet<String>();
					for(int i = 1; i < base.getColumnCount(); i++){
						labels.add(base.getColumnName(i));
					}
					matrixModel.addColumn("Act \\ Pred");
					for(String s : labels){
						matrixModel.addColumn(s);
					}
					List<Object[]> rowsToPass = new ArrayList<Object[]>();
					for(int i = 1; i < base.getColumnCount(); i++){
						Object[] row = new Object[base.getColumnCount()];
						row[0] = base.getColumnName(i).toString();
						for(int j = 1; j < base.getColumnCount(); j++){
							Integer compVal = (Integer)comp.getDeepValue((i-1), j);
							Integer baseVal = (Integer)base.getDeepValue((i-1), j);
							sum[1] += Math.abs(compVal-baseVal);
							RadioButtonListEntry cellButton = new RadioButtonListEntry(compVal-baseVal, false);
							confusionCells.add(cellButton);
							row[j] = cellButton;
						}
						rowsToPass.add(row);
					}
					for(Object[] row : rowsToPass){
						matrixModel.addRow(row);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			matrixDisplay.setModel(matrixModel);
			matrixDisplay.addMouseListener(new ToggleMouseAdapter(matrixDisplay, true){
				@Override
				public void setHighlight(Object row, String col) {
					DifferenceMatrixPlugin.setHighlightedCell(row.toString(), col);
					Workbench.update(MatricesDisplayPanel.this);
				}
			});
		}
	};

	@Override
	public void refreshPanel(){
		Recipe baseline = CompareModelsControl.getBaselineTrainedModelRecipe();
		Recipe competing = CompareModelsControl.getCompetingTrainedModelRecipe();
		if(baseline != null && competing != null && baseline.getTrainingResult() != null && competing.getTrainingResult() != null){
			baseMatrix.refreshPanel();
			compMatrix.refreshPanel();
			diffMatrix.refreshPanel();
		}
	}
	public MatricesDisplayPanel(){
		setLayout(new RiverLayout());
		baseMatrix.setPreferredSize(new Dimension(250, 150));
		compMatrix.setPreferredSize(new Dimension(250, 150));
		diffMatrix.setPreferredSize(new Dimension(250, 150));
		add("br hfill", diffMatrix);
		add("br hfill", baseMatrix);
		add("br hfill", compMatrix);
	}
}
