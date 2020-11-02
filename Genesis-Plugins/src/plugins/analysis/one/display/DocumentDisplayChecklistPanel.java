package plugins.analysis.one.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.SIDETable;
import edu.cmu.side.view.util.SIDETableCellRenderer;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class DocumentDisplayChecklistPanel extends AbstractListPanel{

	SIDETable checklist = new SIDETable();
	DefaultTableModel model = new DefaultTableModel();
	protected JButton export = new JButton("");

	JCheckBox constraintFeature = new JCheckBox("Filter documents by selected feature");
	JCheckBox reverseConstraint = new JCheckBox("Reverse document filter");
	JCheckBox constraintCell = new JCheckBox("Documents from selected cell only");
	public DocumentDisplayChecklistPanel(){
		setLayout(new RiverLayout());
		add("left", constraintFeature);
		add("br left", reverseConstraint);
		constraintFeature.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				JCheckBox source = ((JCheckBox)e.getSource());
				DocumentsDisplay.setFilterFeature(source.isSelected());
				DocumentsDisplay.refreshPanel();
				
				reverseConstraint.setEnabled(source.isSelected());
			}
		});
		reverseConstraint.setEnabled(false);

		reverseConstraint.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				JCheckBox source = ((JCheckBox)e.getSource());
				DocumentsDisplay.setReverseFilter(source.isSelected());
				DocumentsDisplay.refreshPanel();
			}
		});

		constraintCell.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent e) {
				JCheckBox source = ((JCheckBox)e.getSource());
				DocumentsDisplay.setFilterCell(source.isSelected());
				DocumentsDisplay.refreshPanel();
			}
		});

		export.setIcon(new ImageIcon("toolkits/icons/note_go.png"));
		export.setToolTipText("Export to CSV...");
		export.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				CSVExporter.exportToCSV(model);
			}});
		export.setEnabled(false);
		add("br hfill", constraintCell);
		add("right", export);
		checklist.setModel(model);
		checklist.setDefaultRenderer(Object.class, new SIDETableCellRenderer());
		checklist.addMouseListener(new ToggleMouseAdapter(checklist, false){

			@Override
			public void setHighlight(Object row, String col) {
				if(row instanceof CheckBoxListEntry){
					TrainingResult tr = ExploreResultsControl.getHighlightedTrainedModelRecipe().getTrainingResult();
					CheckBoxListEntry entry = ((CheckBoxListEntry)row);
					Integer index = (Integer)entry.getValue();
					Boolean selected = !(entry.isSelected());
//					System.out.println(index + ", " + selected + " DDCP65");
					DocumentsDisplay.selectIndex(tr, index, selected);
					DocumentsDisplay.refreshPanel();
				}
			}
		});
		JScrollPane scroll = new JScrollPane(checklist);
		add("br hfill vfill", scroll);
	}

	@Override
	public void refreshPanel(){
		Vector<Object> header = new Vector<Object>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		Recipe selectRecipe = ExploreResultsControl.getHighlightedTrainedModelRecipe();
		if(selectRecipe != null){
			TrainingResult select = selectRecipe.getTrainingResult();
			List<Integer> visible = DocumentsDisplay.getModel().safeGetChecklistOptions(select);
			List<Integer> displayed = DocumentsDisplay.getModel().safeGetDisplayList(select);
			export.setEnabled(selectRecipe != null && !visible.isEmpty());
			header.add("Instance");
			header.add("Predicted");
			header.add("Actual");
			header.add("Text");
			for(Integer row : visible){
				Vector<Object> rowVector = new Vector<Object>();
				boolean selected = displayed.contains(row);
				CheckBoxListEntry entry = new CheckBoxListEntry(row, selected);

				rowVector.add(entry);
				rowVector.add(select.getPredictions().get(row).toString());
				rowVector.add(select.getEvaluationTable().getAnnotations().get(row));
				rowVector.add(select.getEvaluationTable().getDocumentList().getPrintableTextAt(row));
				data.add(rowVector);
			}
		}else{
			export.setEnabled(false);
		}
		model = new DefaultTableModel(data, header);
		checklist.setModel(model);
		revalidate();
		repaint();
	}
}
