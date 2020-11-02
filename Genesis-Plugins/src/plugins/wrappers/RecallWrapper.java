package plugins.wrappers;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.FreqMap;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.WrapperPlugin;

public class RecallWrapper extends WrapperPlugin
{

	JCheckBox activeCbx = new JCheckBox("Discount Majority");
	private boolean active = false;
	
	double discount = 0.5;
	String labelToDiscount = "NA";
	
	JSlider discountSlider = new JSlider(0, 100);
	JPanel controlPanel;
	JLabel discountLabel = new JLabel();
	
	public RecallWrapper()
	{
		activeCbx.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent arg0)
			{
				setActive(activeCbx.isSelected());
			}
		});
	}
	
	
	@Override
	public void learnFromTrainingData(FeatureTable train, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{
		//exceptionsMap.clear();
	}

	@Override
	public FeatureTable wrapTableForSubclass(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{	
		return table;
	}

	@Override
	public PredictionResult wrapResultAfter(PredictionResult result, FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{
		if(table.getClassValueType() == Type.NOMINAL)
		{
			List labelList =  result.getPredictions();
			Map<String, List<Double>> distro = result.getDistributions();
			ArrayList<String> labels = new ArrayList<String>(Arrays.asList(table.getLabelArray()));
			
			FreqMap<String> labelCounts = new FreqMap<String>();
			for(String label : table.getDocumentListQuickly().getAnnotationArray(table.getAnnotation()))
			{
				labelCounts.count(label);
			}
			labelToDiscount = labelCounts.getMaxKey();
			System.out.println("discounting majority label "+labelToDiscount);
			
			labels.remove(labelToDiscount);
			labels.add(0, labelToDiscount);
			
			for(int i = 0; i < labelList.size(); i++)
			{
				String best = labelToDiscount;
				double bestScore = 0.0;
				double shift = discount;
				
				for(String label : labels)
				{
					double score =  distro.get(label).get(i);
					if(label.equals(labelToDiscount))
					{
						double newScore = Math.max(0, score-discount);
						shift = newScore - score;
						score = newScore;
					}
					
					if(score > bestScore)
					{
						best = label;
						bestScore = score;
					}

					distro.get(label).set(i, score/(1.0-shift));
				}
				
//				Object originalLabel = labelList.get(i);
//				
//				if(!originalLabel.equals(best))
//					System.out.println(i+": "+ originalLabel+" > " + best);
				
				labelList.set(i, best);
				
			}
			return new PredictionResult(labelList, distro);
		}
		
		return result;
	}

	@Override
	public String getOutputName()
	{
		// TODO Auto-generated method stub
		return "majorityDiscounter";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		if(controlPanel == null)
		{
			controlPanel = new JPanel(new RiverLayout());
			controlPanel.setBackground(null);
			
			discountSlider.setValue(50);
			discountSlider.setMajorTickSpacing(25);
			discountSlider.setPaintLabels(true);
			discountLabel.setText("-"+discount);
			
			discountSlider.addChangeListener(new ChangeListener()
			{
				
				@Override
				public void stateChanged(ChangeEvent arg0)
				{
					discount = discountSlider.getValue()/100.0;
					discountLabel.setText("-"+discount);
				}
			});
			

			controlPanel.add(activeCbx);
//			controlPanel.add(discountSlider);
//			controlPanel.add(discountLabel);
		}
		return controlPanel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("active", "" + isActive());
		settings.put("discount", "" + discount);
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		setActive(settings.containsKey("active") && Boolean.TRUE.toString().equals(settings.get("active")));
		discount = Double.parseDouble(settings.get("discount"));
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
		if(active)
		{
			controlPanel.add("left", discountLabel);
			controlPanel.add("br left", discountSlider);
		}
		else
		{
			controlPanel.remove(discountSlider);
			controlPanel.remove(discountLabel);
		}
		
		controlPanel.revalidate();
		
		BuildModelControl.getWrapperPlugins().put(RecallWrapper.this, active);
	}

	@Override
	public PredictionResult wrapResultForSubclass(PredictionResult result, int fold, Map<Integer, Integer> foldsMap, StatusUpdater update)
	{
		//should never be called
		System.out.println("EMW: somebody's directly calling wrapResultForSubclass! Naughty.");
		return result;
	}

}
