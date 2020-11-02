package plugins.analysis.two.matrix;

import java.util.List;

import plugins.analysis.one.display.DocumentsDisplayModel;
import edu.cmu.side.model.data.TrainingResult;

public class DifferenceDocumentsModel extends DocumentsDisplayModel{


	@Override
	public void updateHighlights(TrainingResult select) {
		generateForTrainingResult(select);
		String col = DifferenceMatrixPlugin.getHighlightedColumn();
		String row = DifferenceMatrixPlugin.getHighlightedRow();
		if(col != null && row != null){
			List<Integer> indices = select.getConfusionMatrix().get(col).get(row);
			for(int i = 0; i < select.getEvaluationTable().getDocumentList().getSize(); i++){
				selectIndex(select, i, indices.contains(i));
			}			
		}
	}
}
