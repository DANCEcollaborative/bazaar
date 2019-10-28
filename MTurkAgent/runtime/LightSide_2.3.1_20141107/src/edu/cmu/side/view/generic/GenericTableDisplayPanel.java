package edu.cmu.side.view.generic;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.SIDETable;

public abstract class GenericTableDisplayPanel extends AbstractListPanel
{

	protected SIDETable table = new SIDETable();
	protected JLabel label = new JLabel("A Very Nice Table");
	protected JScrollPane tableScroll;
	protected JButton export = new JButton("");

	public void setLabel(String l)
	{
		label.setText(l);
	}

	public GenericTableDisplayPanel()
	{
		AbstractTableModel model = getTableModel();
		export.setIcon(new ImageIcon("toolkits/icons/note_go.png"));
		export.setToolTipText("Export to CSV...");
		export.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				CSVExporter.exportToCSV(getTableModel());
			}});
		export.setEnabled(false);
		
		setLayout(new RiverLayout());
		add("left", label);
		add("hfill", new JPanel());
		add("right", export);
		table.setModel(model);
		table.setBorder(BorderFactory.createLineBorder(Color.gray));
		table.setRowSorter(new TableRowSorter<TableModel>(model));
		table.setAutoCreateColumnsFromModel(true);
		tableScroll = new JScrollPane(table);
		add("br hfill vfill", tableScroll);
	}
	
	@Override
	public void refreshPanel()
	{
		updateTableModel();
		AbstractTableModel model = getTableModel();
		export.setEnabled(model.getRowCount() > 0);
	}

	public abstract void updateTableModel();

	public abstract AbstractTableModel getTableModel();

}
