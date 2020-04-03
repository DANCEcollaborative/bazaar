package edu.cmu.side.view.generic;

import java.awt.Color;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.plugin.ModelMetricPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.FeatureTableModel;
import edu.cmu.side.view.util.SIDETable;

public class GenericModelMetricPanel extends AbstractListPanel{

	SIDETable featureTable = new SIDETable();
	FeatureTableModel model = new FeatureTableModel();
	JLabel label = new JLabel("Model Evaluation Metrics:");
	Map<String, String> allKeys = new TreeMap<String, String>();
	public void setLabel(String l){
		label.setText(l);
	}
	
	public Map<String, String> getKeys(){
		return allKeys;
	}
	
	public GenericModelMetricPanel(){
		setLayout(new RiverLayout());
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		add("left", label);
		featureTable.setModel(model);
		featureTable.setBorder(BorderFactory.createLineBorder(Color.gray));
		JScrollPane tableScroll = new JScrollPane(featureTable);
		add("br hfill vfill", tableScroll);
	}

	public void refreshPanel(Recipe recipe){
		model = new FeatureTableModel();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		Vector<Object> header = new Vector<Object>();
		header.add("Metric");
		header.add("Value");
		if(recipe != null && recipe.getTrainingResult() != null){
			allKeys.clear();
			TrainingResult result = recipe.getTrainingResult();
			Collection<ModelMetricPlugin> plugins = BuildModelControl.getModelEvaluationPlugins();
			for(ModelMetricPlugin plugin : plugins){
				Map<String, String> evaluations = plugin.evaluateModel(result, plugin.generateConfigurationSettings());
				result.cacheEvaluations(evaluations);
				for(String s : evaluations.keySet()){
					Vector<Object> row = new Vector<Object>();
					row.add(s);
					try{
						Double d = Double.parseDouble(evaluations.get(s));
						row.add(d);
					}catch(Exception e){
						row.add(evaluations.get(s));
					}
					data.add(row);
				}
				allKeys.putAll(evaluations);
			}			
		}
		model = new FeatureTableModel(data, header);
		featureTable.setModel(model);
	}

}
