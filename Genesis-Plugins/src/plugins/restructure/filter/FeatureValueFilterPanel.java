package plugins.restructure.filter;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import plugins.restructure.RestructureFeatureMetricCheckboxPanel;
import plugins.restructure.filter.FeatureValueFilter.FeatureValue;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.view.generic.GenericFeatureMetricPanel;
import edu.cmu.side.view.util.Refreshable;

public class FeatureValueFilterPanel extends JPanel implements Refreshable
{
	private GenericFeatureMetricPanel featurePanel;
	

	JComboBox filterHowBox = new JComboBox(new DefaultComboBoxModel(new String[]{"Selected feature hits", "Documents with features"}));
	JComboBox removeRetainBox = new JComboBox(new DefaultComboBoxModel(new String[]{"Remove", "Retain"}));
	

	static ImageIcon addIcon = new ImageIcon("toolkits/icons/add.png");
	static ImageIcon deleteIcon = new ImageIcon("toolkits/icons/delete.png");




	public FeatureValueFilterPanel(final FeatureValueFilter plugin)
	{
		featurePanel = new RestructureFeatureMetricCheckboxPanel()
		{
			
			@Override
			public void selectedFeaturesChanged()
			{
				plugin.filters = getFilters();
			}
			
			@Override
			public void refreshPanel()
			{
				super.refreshPanel();
				plugin.filters = getFilters();
			}
		};
		
//		filterPanel.setLayout(new RiverLayout(0, 5));
		
//		JButton addButton = new JButton("Filter", addIcon);
//		
//		addButton.addActionListener(new ActionListener()
//		{
//			
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				for(Feature selectedFeature: featurePanel.getSelectedFeatures())
//				{
//					if(selectedFeature != null && !filterItems.containsKey(selectedFeature))
//					{
//						FilterItem filterItem = new FilterItem(selectedFeature);
//						filterPanel.add("br left hfill", filterItem);
//					}
//				}
//
//				filterPanel.revalidate();
//			}
//		});

		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, this);

		featurePanel.setPreferredSize(new Dimension(0, 250));
		
		ActionListener updateFiltersListener = new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				plugin.filters = getFilters();
			}
		};

		removeRetainBox.addActionListener(updateFiltersListener);
		filterHowBox.addActionListener(updateFiltersListener);
		
		this.setLayout(new RiverLayout());
		this.add("br left", removeRetainBox);
		this.add("hfill", filterHowBox);
		this.add("br left hfill vfill", featurePanel);
//		this.add("br left", addButton);
//		this.add("br vfill hfill", filterPanel);
		
		this.refreshPanel();
	}

	@Override
	public void refreshPanel()
	{
		featurePanel.refreshPanel();

	}
	
	public Set<FeatureValue> getFilters()
	{
		Set<FeatureValue> filters = new HashSet<FeatureValue>();
		for(Feature f : featurePanel.getSelectedFeatures())
		{
			FeatureValue filtron = new FeatureValue(f, null, 0, filterHowBox.getSelectedIndex()==0, removeRetainBox.getSelectedIndex() == 1);
			filters.add(filtron);
		}
		
		return filters;
	}

//	class FilterItem extends JPanel //unused for now
//	{
//		JComboBox possibleBox;
//		JComboBox filterHowBox = new JComboBox(new DefaultComboBoxModel(new String[]{"feature hits", "documents"}));
//		JComboBox removeRetainBox = new JComboBox(new DefaultComboBoxModel(new String[]{"Remove", "Retain"}));
//		
//		FeatureValue myValue = null;
//
//		public FilterItem(final Feature f)
//		{
//			setLayout(new RiverLayout(5, 0));
//			
//			filterItems.put(f, this);
//			List possibles = new ArrayList();
//			Component valueChooser = new JLabel("");
//			possibleBox  = new JComboBox();
//
//			Type featureType = f.getFeatureType();
//			if (featureType == Type.NOMINAL)
//			{
//				possibles.addAll(f.getNominalValues());
//				valueChooser = possibleBox;
//			}
//			
//			if (featureType == Type.BOOLEAN)
//			{
//				possibles.addAll(Arrays.asList(Boolean.TRUE));
//			}
//			else
//			{
//				possibles.add(0, "All");
//			}
//
//			possibleBox.setModel(new DefaultComboBoxModel(possibles.toArray()));
//			ItemListener updateFilterListener = new ItemListener()
//			{
//				@Override
//				public void itemStateChanged(ItemEvent arg0)
//				{
//					updateFilterSelection(f);
//				}
//			};
//			possibleBox.addItemListener(updateFilterListener);
//			filterHowBox.addItemListener(updateFilterListener);
//
//			
//			JButton deleteButton = new JButton(deleteIcon);
//			deleteButton.setBorderPainted(false);
//			deleteButton.setContentAreaFilled(false);
//			deleteButton.setOpaque(false);
//			deleteButton.addActionListener(new ActionListener()
//			{
//				@Override
//				public void actionPerformed(ActionEvent arg0)
//				{
//					filterPanel.remove(FilterItem.this);
//					filterItems.remove(f);
//					
//					if(filters.contains(myValue))
//						filters.remove(myValue);
//					
//					filterPanel.revalidate();
//				}});
//
//			add("left", removeRetainBox);
//			add("left", valueChooser);
//			add("hfill", new JLabel("'"+f.getFeatureName()+"'"));
//			add("right", filterHowBox);
//			add("right", deleteButton);
//			
//			updateFilterSelection(f);
//		}
//
//		public void updateFilterSelection(final Feature f)
//		{
//
//			if(filters.contains(myValue))
//				filters.remove(myValue);
//			
//			if (possibleBox.getSelectedIndex() > 0)
//			{
//				myValue = new FeatureValue(f, possibleBox.getSelectedItem(), 0, filterHowBox.getSelectedIndex()==0, removeRetainBox.getSelectedIndex() == 1);
//				filters.add(myValue);
//			}
//			else
//			{
//				myValue = new FeatureValue(f, null, 0, filterHowBox.getSelectedIndex()==0, removeRetainBox.getSelectedIndex() == 1);
//				filters.add(myValue);
//			}
//		}
//	}

}
