package plugins.restructure.multi.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import plugins.restructure.GrowthModel;
import plugins.restructure.multi.model.CrossedLevel;
import plugins.restructure.multi.model.NestedLevel;
import plugins.restructure.multi.model.SimpleLevel;
import plugins.restructure.multi.model.StructuredLevel;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.FeatureTableModel;
import edu.cmu.side.view.util.RadioButtonListEntry;
import edu.cmu.side.view.util.SIDETable;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class MultilevelDomainsPanel extends AbstractListPanel{

	SIDETable display = new SIDETable();
	FeatureTableModel model = new FeatureTableModel();
	
	Map<FeatureTable, Map<StructuredLevel, Boolean>> levels = new HashMap<FeatureTable, Map<StructuredLevel, Boolean>>();
	
	ButtonGroup groupA = new ButtonGroup();
	ButtonGroup groupB= new ButtonGroup();
	
	JButton cross = new JButton("A*B");
	JButton nest = new JButton("A[B]");
	
	public MultilevelDomainsPanel(){
		setLayout(new RiverLayout());
		add("left", new JLabel("Select Levels:"));
		display.addMouseListener(new ToggleMouseAdapter(display, true){
			@Override
			public void setHighlight(Object row, String col) {}
		});

		cross.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(RestructureTablesControl.hasHighlightedFeatureTable()){
					FeatureTable table = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
					levels.get(table).put(new CrossedLevel(GrowthModel.getLevelA(), GrowthModel.getLevelB()), false);
					refreshPanel();
				}
			}
			
		});

		
		nest.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(RestructureTablesControl.hasHighlightedFeatureTable()){
					FeatureTable table = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
					levels.get(table).put(new NestedLevel(GrowthModel.getLevelA(), GrowthModel.getLevelB()), false);
					refreshPanel();
				}
			}
			
		});
		describeScroll = new JScrollPane(display);
		add("br hfill vfill", describeScroll);
		add("br center", cross);
		add("center", nest);
	}

	public Collection<StructuredLevel> getHighlightedLevels(FeatureTable table){
		Collection<StructuredLevel> result = new HashSet<StructuredLevel>();
		for(StructuredLevel s : levels.get(table).keySet()){
			if(levels.get(table).get(s)) result.add(s);
		}
		return result;
	}
	
	
	
	@Override
	public void refreshPanel(){
		model = new FeatureTableModel();
		model.addColumn("Domain");
		model.addColumn("A");
		model.addColumn("B");
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			FeatureTable table = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
			if(!levels.containsKey(table)){
				Map<StructuredLevel, Boolean> anns = new TreeMap<StructuredLevel, Boolean>();
				for(String ann : table.getDocumentList().getAnnotationNames()){
					if(!table.getAnnotation().equals(ann)){
						anns.put(new SimpleLevel(ann), false);						
					}
				}
				levels.put(table, anns);
			}
			groupA = new ButtonGroup();
			groupB = new ButtonGroup();
			for(StructuredLevel level : levels.get(table).keySet()){
				Object[] row = new Object[3];
				CheckBoxListEntry check = new CheckBoxListEntry(level, false);
				levels.get(table).put(level, false);
				check.addItemListener(new GenesisControl.PluginCheckboxListener<StructuredLevel>(this, levels.get(table)));
				row[0] = check;				
				RadioButtonListEntry buttonA = new RadioButtonListEntry(level, false);
				buttonA.addItemListener(new LevelButtonListener() {		
					@Override
					public void setSelected(StructuredLevel level) {
						GrowthModel.setLevelA(level);
					}
				});
				buttonA.setText("");
				RadioButtonListEntry buttonB = new RadioButtonListEntry(level, false);
				buttonB.addItemListener(new LevelButtonListener() {		
					@Override
					public void setSelected(StructuredLevel level) {
						GrowthModel.setLevelB(level);
						cross.setEnabled(GrowthModel.getLevelA() != null && GrowthModel.getLevelB() != null);
						nest.setEnabled(GrowthModel.getLevelA() != null && GrowthModel.getLevelB() != null);
					}
				});
				buttonB.setText("");
				groupA.add(buttonA);
				groupB.add(buttonB);
				row[1] = buttonA;
				row[2] = buttonB;
				model.addRow(row);
			}
		}
		display.setModel(model);
		display.getColumnModel().getColumn(0).setPreferredWidth(175);
		display.getColumnModel().getColumn(1).setPreferredWidth(25);
		display.getColumnModel().getColumn(2).setPreferredWidth(25);
		display.setDefaultRenderer(Object.class, new MultilevelCellRenderer());
		cross.setEnabled(GrowthModel.getLevelA() != null && GrowthModel.getLevelB() != null);
		nest.setEnabled(GrowthModel.getLevelA() != null && GrowthModel.getLevelB() != null);
		
		revalidate();
	}
	
	public void checkButtons(){
		StructuredLevel a = GrowthModel.getLevelA();
		StructuredLevel b = GrowthModel.getLevelB();
		boolean crossBool = a != null && b != null;
		boolean nestBool = a != null && b != null;
		if(a instanceof NestedLevel || b instanceof NestedLevel){
			crossBool = false;
		}
		
		cross.setEnabled(crossBool);
		nest.setEnabled(nestBool);
	}
}
