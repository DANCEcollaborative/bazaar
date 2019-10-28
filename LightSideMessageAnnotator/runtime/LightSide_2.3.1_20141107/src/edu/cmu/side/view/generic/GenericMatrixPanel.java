package edu.cmu.side.view.generic;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.ConfusionCellRenderer;
import edu.cmu.side.view.util.SIDETable;

public abstract class GenericMatrixPanel extends AbstractListPanel{
	protected SIDETable matrixDisplay = new SIDETable();
	protected DefaultTableModel matrixModel = new DefaultTableModel();

	protected JLabel label;

	protected ModelFeatureMetricPlugin plugin;
	protected String setting;

	protected Double[] sum = new Double[]{0.0,0.0};
	protected JButton export = new JButton("");

	public Double[] getSum(){
		return sum;
	}

	public SIDETable getDisplayTable(){
		return matrixDisplay;
	}

	public GenericMatrixPanel(ModelFeatureMetricPlugin p, String s){
		this();
		plugin = p;
		setting = s;
	}

	public GenericMatrixPanel(String l){
		this();
		label.setText(l);
	}

	public GenericMatrixPanel(){
		

		export.setIcon(new ImageIcon("toolkits/icons/note_go.png"));
		export.setToolTipText("Export to CSV...");
		export.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				CSVExporter.exportToCSV(matrixModel);
			}});
		export.setEnabled(false);
		
		setLayout(new RiverLayout());
		label = new JLabel("Model Confusion Matrix:");
		add("hfill", label);
		add("right", export);
		matrixDisplay.setModel(matrixModel);
		matrixDisplay.setBorder(BorderFactory.createLineBorder(Color.gray));
		matrixDisplay.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e){
				int row = matrixDisplay.getSelectedRow();
				int col = matrixDisplay.getSelectedColumn();
				if(row<0||col<=0) return;
			}
		});
		matrixDisplay.setDefaultRenderer(java.lang.Object.class, new ConfusionCellRenderer(this));
		describeScroll = new JScrollPane(matrixDisplay);
		add("br hfill vfill", describeScroll);
	}

	@Override
	public abstract void refreshPanel();

	public void refreshPanel(Map<String, Map<String, List<Integer>>> confusion){

		Collection<String> labels = new TreeSet<String>();
		Vector<Object> header = new Vector<Object>();
		for(String s : confusion.keySet()){
			labels.add(s);
			for(String p : confusion.get(s).keySet()){
				labels.add(p);
			}
		}
		header.add("Act \\ Pred");

		for(String s : labels){
			header.add(s);
		}

		sum = new Double[]{0.0,0.0};
		Vector<Vector<Object>> data = generateRows(confusion, labels);
		
		matrixModel = new DefaultTableModel(data, header);
		matrixDisplay.setModel(matrixModel);
		export.setEnabled(confusion != null && !confusion.isEmpty());

	}

	protected Vector<Vector<Object>> generateRows(Map<String, Map<String, List<Integer>>> confusion, Collection<String> labels) {
		Vector<Vector<Object>> rowsToPass = new Vector<Vector<Object>>();
		double localSum = 0;
		for(String act : labels){
			Vector<Object> row = new Vector<Object>();
			row.add(act);
			int index = 1;
			for(String pred : labels){
				if(confusion.containsKey(pred) && confusion.get(pred).containsKey(act)){
					List<Integer> cellIndices = confusion.get(pred).get(act);
					localSum += confusion.get(pred).get(act).size();
					row.add(getCellObject(cellIndices.size()));		
				}else{
					row.add(getCellObject(0));
				}
				index++;
			}
			rowsToPass.add(row);
		}
		sum = new Double[]{0.0,localSum};
		return rowsToPass;
	}

	public Object getCellObject(Object o){
		return o;
	}

	public String getSetting()
	{
		return setting;
	}

	public void setSetting(String setting)
	{
		this.setting = setting;
	}
}
