package plugins.analysis.one.metric;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.ActionBarTask;
import edu.cmu.side.view.generic.GenericMatrixPanel;
import edu.cmu.side.view.util.SwingUpdaterLabel;

public class FeatureMetricAnalysis extends EvaluateOneModelPlugin
{
	Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
	JLabel infoLabel = new JLabel();

	SwingUpdaterLabel update = new SwingUpdaterLabel();
//
//	class MiniActionBar extends ActionBar
//	{
//
//		public MiniActionBar(StatusUpdater update)
//		{
//			super(update);
//			remove(this.actionButton);
//			add("left", progressBar);
//		}
//
//		@Override
//		public void startedTask()
//		{
//		}
//
//		@Override
//		public void endedTask()
//		{
//		}
//	}
//
//	MiniActionBar mini = new MiniActionBar(update);
	JPanel panel = new JPanel();

	@Override
	public String getOutputName()
	{
		return "basic";
	}

	@Override
	public String toString()
	{
		return "Highlighted Feature Details";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		panel = new JPanel(new RiverLayout());
		updateLabelText();
		panel.add("left", infoLabel);
		
//		panel.add("hfill", mini);
//		mini.removeAll();
		PopulateMatricesTask task = new PopulateMatricesTask(ExploreResultsControl.getActionBar());
		task.executeActionBarTask();
//		JScrollPane scroll = new JScrollPane(panel);
//		return scroll;
		return panel;
	}

	protected void updateLabelText()
	{
		Feature feature = ExploreResultsControl.getHighlightedFeature();
		String labelText = "Metric Confusion Matrices"+(feature != null?(" for "+feature):"");
		infoLabel.setText(labelText);
	}

	class PopulateMatricesTask extends ActionBarTask
	{

		ArrayList<GenericMatrixPanel> matrixPanels = new ArrayList<GenericMatrixPanel>();
		
		public PopulateMatricesTask(ActionBar action)
		{
			super(action);
		}

		@Override
		protected void doTask()
		{

			Map<ModelFeatureMetricPlugin, Map<String, Boolean>> metrics = ExploreResultsControl.getFeatureEvaluationPlugins();
			for (ModelFeatureMetricPlugin plugin : metrics.keySet())
			{
				Map<String, Boolean> pluginMap = metrics.get(plugin);
				for (String metric : pluginMap.keySet())
				{
					if (pluginMap.get(metric))
					{
//						System.out.println(metric + ": "+pluginMap.get(metric));
						GenericMatrixPanel matrix = new GenericMatrixPanel(plugin, metric)
						{

							private double minVal = Double.MAX_VALUE;
							private double maxVal = Double.MIN_VALUE;

							@Override
							public Double[] getSum()
							{
								return new Double[] { minVal, maxVal };
							}

							@Override
							protected Vector<Vector<Object>> generateRows(Map<String, Map<String, List<Integer>>> confusion, Collection<String> labels)
							{
								Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
								Recipe recipe = ExploreResultsControl.getHighlightedTrainedModelRecipe();
								TrainingResult result = recipe.getTrainingResult();
								boolean[] mask = new boolean[result.getEvaluationTable().getDocumentList().getSize()];
								for (int i = 0; i < mask.length; i++)
									mask[i] = true;
								Feature highlightedFeature = ExploreResultsControl.getHighlightedFeature();
								if (highlightedFeature != null)
								{
									minVal = Double.MAX_VALUE;
									maxVal = Double.MIN_VALUE;
									try
									{
									for (String act : labels)
									{
										Vector<Object> row = new Vector<Object>();
										row.add(act);
										int index = 1;
										for (String pred : labels)
										{
											Map<Feature, Double> values = plugin.evaluateModelFeatures(recipe, mask, setting, pred, act,
													ExploreResultsControl.getUpdater());
											
											if(values != null && values.containsKey(highlightedFeature))//sparse evaluations might be tricky
											{
												Double val = values.get(highlightedFeature);
												for (Feature f : values.keySet())
												{
													Double test = values.get(f);
													if (test < minVal) minVal = test;
													
													if (test > maxVal) maxVal = test;
												}
												row.add(val);
											}
											else row.add(0.0);//sparse evaluations might be tricky
											index++;
										}
										rows.add(row);
									}
									}
									catch(Exception e)
									{
										e.printStackTrace();
									}
								}
								return rows;
							}

							@Override
							public void refreshPanel()
							{
								if (ExploreResultsControl.hasHighlightedTrainedModelRecipe())
								{
									refreshPanel(ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
								}
								revalidate();
								repaint();
							}
						};
						
						matrixPanels.add(matrix);
					}
				}
			}
		}
		
		@Override 
		public void beginTask()
		{
			
		}
		
		@Override 
		public void finishTask()
		{
			
			panel.removeAll();
			boolean first = true;
			for(GenericMatrixPanel matrix : matrixPanels)
			{
				JPanel metricPanel = new JPanel(new BorderLayout());
				JLabel metricName = new JLabel(matrix.getSetting());
				metricName.setFont(font);
				
				matrix.refreshPanel();
				metricPanel.add(BorderLayout.NORTH, metricName);
				metricPanel.add(BorderLayout.CENTER, matrix);
				int width = 300;
				if (ExploreResultsControl.hasHighlightedTrainedModelRecipe())
				{
					TrainingResult result = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
					width = Math.max(width, 75 + (75 * result.getConfusionMatrix().keySet().size()));
				}
				metricPanel.setPreferredSize(new Dimension(width, 250));
				String align = "left";
				if (first)
				{
					align = "br " + align;
					first = false;
				}
				panel.add(align, new JScrollPane(metricPanel));
			}

			panel.revalidate();
			panel.repaint();
		}

		@Override
		public void requestCancel()
		{
			// TODO Auto-generated method stub

		}

	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		return new TreeMap<String, String>();
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{

	}

}
