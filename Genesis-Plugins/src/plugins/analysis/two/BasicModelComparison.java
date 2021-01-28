package plugins.analysis.two;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import plugins.analysis.two.matrix.DifferenceMatrixPlugin;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.EvaluateTwoModelPlugin;
import edu.cmu.side.view.generic.GenericMatrixPanel;
import edu.cmu.side.view.generic.GenericModelMetricPanel;

public class BasicModelComparison extends EvaluateTwoModelPlugin
{
	JPanel panel = new JPanel(new RiverLayout());
	GenericModelMetricPanel baselineMetrics = new GenericModelMetricPanel();
	GenericModelMetricPanel competingMetrics = new GenericModelMetricPanel();

	DecimalFormat numberFormat = new DecimalFormat("#.###");

	JPanel colorBaseline = new JPanel(new BorderLayout());
	JPanel colorCompeting = new JPanel(new BorderLayout());
	JLabel reportBaseline = new JLabel("No model selected");
	JLabel reportCompeting = new JLabel("No model selected");

	static GenericMatrixPanel baseMatrix = new GenericMatrixPanel("Baseline Confusion Matrix:")
	{

		@Override
		public void refreshPanel()
		{
			if (CompareModelsControl.hasBaselineTrainedModelRecipe())
			{
				refreshPanel(CompareModelsControl.getBaselineTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
				DifferenceMatrixPlugin.setBaseTable(matrixDisplay);
			}
			else
			{
				refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
			}
		}
	};

	static GenericMatrixPanel compMatrix = new GenericMatrixPanel("Competing Confusion Matrix:")
	{

		@Override
		public void refreshPanel()
		{
			if (CompareModelsControl.hasCompetingTrainedModelRecipe())
			{
				refreshPanel(CompareModelsControl.getCompetingTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
				DifferenceMatrixPlugin.setCompTable(matrixDisplay);
			}
			else
			{
				refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
			}
		}
	};

	TTest test = new TTest();
	OneWayAnova anova = new OneWayAnova();

	public BasicModelComparison()
	{

		JPanel top = new JPanel(new GridLayout(1, 2));
		JPanel baselinePanel = new JPanel(new BorderLayout());
		JPanel competingPanel = new JPanel(new BorderLayout());
		baselineMetrics.setLabel("Baseline Model Metrics:");
		competingMetrics.setLabel("Competing Model Metrics:");

		baseMatrix.setPreferredSize(new Dimension(400, 300));
		compMatrix.setPreferredSize(new Dimension(400, 300));
		colorBaseline.add(BorderLayout.CENTER, baseMatrix);
		colorCompeting.add(BorderLayout.CENTER, compMatrix);
		colorBaseline.add(BorderLayout.SOUTH, reportBaseline);
		colorCompeting.add(BorderLayout.SOUTH, reportCompeting);

		baselineMetrics.setPreferredSize(new Dimension(400, 150));
		competingMetrics.setPreferredSize(new Dimension(400, 150));
		baselinePanel.add(BorderLayout.NORTH, baselineMetrics);
		baselinePanel.add(BorderLayout.CENTER, colorBaseline);
		baselinePanel.setPreferredSize(new Dimension(400, 500));
		competingPanel.add(BorderLayout.NORTH, competingMetrics);
		competingPanel.add(BorderLayout.CENTER, colorCompeting);
		competingPanel.setPreferredSize(new Dimension(400, 500));

		top.setPreferredSize(new Dimension(800, 400));
		top.add(baselinePanel);
		top.add(competingPanel);
		panel.add("hfill", top);
	}

	@Override
	public String getOutputName()
	{
		return "basic";
	}

	@Override
	public String toString()
	{
		return "Basic Model Comparison";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return panel;
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

	@Override
	public void refreshPanel()
	{
		Recipe baseline = CompareModelsControl.getBaselineTrainedModelRecipe();
		Recipe competing = CompareModelsControl.getCompetingTrainedModelRecipe();
		baselineMetrics.refreshPanel(baseline);
		competingMetrics.refreshPanel(competing);
		if (baseline == null || baseline.getTrainingResult() == null)
		{
			reportBaseline.setText("No model selected.");
			reportBaseline.setBorder(BorderFactory.createEmptyBorder());
		}
		if (baseline == null || baseline.getTrainingResult() == null)
		{
			reportCompeting.setText("No model selected.");
			reportCompeting.setBorder(BorderFactory.createEmptyBorder());
		}
		baseMatrix.refreshPanel();
		compMatrix.refreshPanel();
		if (baseline != null && competing != null && baseline.getTrainingResult() != null && competing.getTrainingResult() != null)
		{
			Map<String, Double> vals = test(baseline, competing);
			Double better = vals.get("better");
			Double p = vals.get("p");
			String stat = "";
			if (vals.containsKey("t"))
			{
				stat += "t=" + numberFormat.format(vals.get("t"));
			}
			if (vals.containsKey("f"))
			{
				stat += "f=" + numberFormat.format(vals.get("f"));
			}
			if (better == 0.0)
			{
				updatePanels(colorBaseline, colorCompeting, reportBaseline, reportCompeting, p, stat);
			}
			else
			{
				updatePanels(colorCompeting, colorBaseline, reportCompeting, reportBaseline, p, stat);
			}
		}
	}

	public void updatePanels(JPanel winner, JPanel loser, JLabel winnerText, JLabel loserText, Double p, String stat)
	{
		winner.setBorder(BorderFactory.createLineBorder(Color.black));
		loser.setBorder(BorderFactory.createEmptyBorder());
		String form = numberFormat.format(p);

		Color back = UIManager.getColor("Panel.background");

		if (p.isNaN() || p.isInfinite())
		{
			winnerText.setText("Models are identical.");
			loserText.setText(" ");
			winner.setBackground(back);
			loser.setBackground(back);

		}
		else if (p > 0.1)
		{
			winnerText.setText("Insignificant improvement (p=" + form + ", " + stat + ")");
			loserText.setText(" ");
			winner.setBackground(back);
			loser.setBackground(back);
		}
		else if (p > 0.05)
		{
			winnerText.setText("Marginal improvement (p=" + form + ", " + stat + ")");
			loserText.setText(" ");
			winner.setBackground(new Color(238, 221, 130));
			loser.setBackground(new Color(255, 99, 71));
		}
		else if (p > 0.01)
		{
			winnerText.setText("Significant improvement (p=" + form + "*, " + stat + ")");
			loserText.setText(" ");
			winner.setBackground(new Color(143, 188, 143));
			loser.setBackground(new Color(255, 99, 71));
		}
		else if (p < 0.0)
		{
			winnerText.setText("Models cannot be compared.");
			loserText.setText("Models cannot be compared.");
			winner.setBackground(back);
			loser.setBackground(back);
		}
		else
		{
			winnerText.setText("Highly significant improvement (p=" + form + "**, " + stat + ")");
			loserText.setText(" ");
			winner.setBackground(new Color(60, 179, 113));
			loser.setBackground(new Color(255, 99, 71));
		}
	}

	public Map<String, Double> test(Recipe baseline, Recipe competing)
	{
		TrainingResult base = baseline.getTrainingResult();
		TrainingResult comp = competing.getTrainingResult();
		Map<String, Double> out = new TreeMap<String, Double>();
		List<? extends Comparable<?>> predictionsBase = base.getPredictions();
		List<? extends Comparable<?>> predictionsComp = comp.getPredictions();
		double[] basePerf = new double[base.getEvaluationTable().getDocumentList().getSize()];
		double[] compPerf = new double[base.getEvaluationTable().getDocumentList().getSize()];
		int baseSum = 0;
		int compSum = 0;

		Type baseType = base.getEvaluationTable().getClassValueType();
		Type compType = comp.getEvaluationTable().getClassValueType();
		if (predictionsBase.size() == predictionsComp.size() && baseType == compType)
		{

			switch (baseType)
			{
				case STRING:
				case BOOLEAN:
				case NOMINAL:
					List<String> actual = base.getEvaluationTable().getAnnotations();
					for (int i = 0; i < actual.size(); i++)
					{
						if (actual.get(i).equals(predictionsBase.get(i).toString()))
						{
							basePerf[i] = 1;
							baseSum++;
						}
						if (actual.get(i).equals(predictionsComp.get(i).toString()))
						{
							compPerf[i] = 1;
							compSum++;
						}
					}
					out.put("better", baseSum > compSum ? 0.0 : 1.0);
					out.put("t", test.pairedT(basePerf, compPerf));
					out.put("p", test.pairedTTest(basePerf, compPerf));
					break;
				case NUMERIC:
					double[] actualNumeric = base.getEvaluationTable().getNumericClassValues("numeric");
					SimpleRegression regressBase = new SimpleRegression();
					SimpleRegression regressComp = new SimpleRegression();

					for (int i = 0; i < actualNumeric.length; i++)
					{
						try
						{
							Object basePred = predictionsBase.get(i);
							Double baseDbl = null;
							if (basePred instanceof Double)
							{
								baseDbl = (Double) basePred;
							}
							else
							{
								baseDbl = Double.parseDouble(basePred.toString());
							}

							Object compPred = predictionsComp.get(i);
							Double compDbl = null;
							if (compPred instanceof Double)
							{
								compDbl = (Double) compPred;
							}
							else
							{
								compDbl = Double.parseDouble(compPred.toString());
							}

							basePerf[i] = baseDbl;
							compPerf[i] = compDbl;

							regressBase.addData(baseDbl, actualNumeric[i]);
							regressComp.addData(compDbl, actualNumeric[i]);

						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}

					Collection<double[]> twoModels = new ArrayList<double[]>();
					twoModels.add(basePerf);
					twoModels.add(compPerf);
					double f = anova.anovaFValue(twoModels);
					double p = anova.anovaPValue(twoModels);
					out.put("f", f);
					out.put("p", p);
					out.put("better", regressBase.getSumSquaredErrors() < regressComp.getSumSquaredErrors() ? 0.0 : 1.0);
					break;
			}
		}
		else
		{
			out.put("f", 0.0);
			out.put("p", -1.0);
			out.put("better", 0.0);
		}
		return out;
	}
}
