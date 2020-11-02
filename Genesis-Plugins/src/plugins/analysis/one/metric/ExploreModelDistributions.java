package plugins.analysis.one.metric;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;
import edu.cmu.side.view.generic.GenericTableDisplayPanel;
import edu.cmu.side.view.util.MapOfListsTableModel;

public class ExploreModelDistributions extends EvaluateOneModelPlugin
{

	protected static final class PredictedLabelRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			boolean highlight = table.getSelectedRow() == row;
			if(row != 1 && !highlight)
			{
				if(value.equals(table.getValueAt(row, 1)))
				{
					cell.setBackground(new Color(192, 192, 255));
				}
				else
				{
					cell.setBackground(new Color(255, 224, 192));
				}
			}
			
			return cell;

		}
	}
	
	protected static final class LabelDistributionRenderer extends DefaultTableCellRenderer
	{
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			
			int notBlue = Math.max(Math.min((int) (255*(0.2 + 0.8*(1.0 - Math.pow((Double)value, 2)))), 255), 0);
			
			boolean highlight = table.getSelectedRow() == row;
			
			if(!highlight)
			{

				int actualColumn = table.getColumn("Label").getModelIndex();
				
				if(table.getColumnName(column).startsWith(table.getValueAt(row, actualColumn).toString()))
				{
					cell.setBackground(new Color(notBlue, notBlue, 255));
					
					if(notBlue < 80)
						cell.setForeground(Color.white);
					else
						cell.setForeground(Color.black);
				}
				else
				{
					cell.setBackground(new Color(255, 128+notBlue/2, notBlue));
					cell.setForeground(Color.black);
				}
			}
	
			return cell;

		}
	}

	GenericTableDisplayPanel display = new GenericTableDisplayPanel()
	{
		MapOfListsTableModel model;

		@Override
		public void updateTableModel()
		{
			Recipe r = ExploreResultsControl.getHighlightedTrainedModelRecipe();
			if (r == null)
			{
				model.setMap(null);
			}
			else if(r.getTrainingTable().getClassValueType() == Type.NUMERIC)
			{
				model.setMap(null);
				setLabel("Numeric classes don't have label distributions.");
				
			}
			else
			{
				TrainingResult trainingResult = r.getTrainingResult();
				Map<String, List<Double>> distributions = trainingResult.getDistributions();
				FeatureTable evaluationTable = trainingResult.getEvaluationTable();

				HashMap<String, List> displayMap = new HashMap<String, List>();

				List<String> labels = new ArrayList<String>();
				String docIndexLabelName = "Document Index";
				String actualLabelName = "Label";
				String predictedLabelName = "Predicted";
				String textLabelName = "Text";

				labels.add(docIndexLabelName);
				labels.add(actualLabelName);
				labels.add(predictedLabelName);

				displayMap.put(actualLabelName, evaluationTable.getNominalClassValues());
				displayMap.put(predictedLabelName, trainingResult.getPredictions());

				for (String key : evaluationTable.getLabelArray())
				{
					String predictedLabel = key + " Score";
					displayMap.put(predictedLabel, distributions.get(key));
					labels.add(predictedLabel);
				}
				labels.add(textLabelName);

				List<String> text = new ArrayList<String>();
				List<Integer> index = new ArrayList<Integer>();

				for (int i = 0; i < evaluationTable.getSize(); i++)
				{
					index.add(i);
					String printable = evaluationTable.getDocumentList().getPrintableTextAt(i);
					text.add(printable.substring(0, Math.min(printable.length(), 200)));
					// FIXME: this could be large if not truncated. reference
					// doctable directly instead?
				}

				displayMap.put(textLabelName, text);
				displayMap.put(docIndexLabelName, index);

				model.setMap(displayMap, labels);
				setLabel(trainingResult.getName() + " Label Distributions");
				
				table.getColumn(predictedLabelName).setCellRenderer(new PredictedLabelRenderer());
			}
		}

		@Override
		public AbstractTableModel getTableModel()
		{
			if (model == null) 
			{
				model = new MapOfListsTableModel(null);

				table.setDefaultRenderer(Double.class, new LabelDistributionRenderer());
				table.setDefaultRenderer(String.class, new DefaultTableCellRenderer());
			}
			return model;
		}
	};

	public ExploreModelDistributions()
	{
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, display);
	}

	@Override
	public String toString()
	{
		return "Label Distributions";
	}

	@Override
	public String getOutputName()
	{
		return "distro";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		display.refreshPanel();
		return display;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		// TODO Auto-generated method stub

	}

}
