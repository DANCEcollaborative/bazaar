package plugins.features;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.FeatureTableModel;
import edu.cmu.side.view.util.SIDETable;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public abstract class ColumnFeaturesPanel extends AbstractListPanel
{

	SIDETable display = new SIDETable()
	{

		@Override
		public boolean isCellEditable(int row, int col)
		{
			return col > 0;
		}
	};
	FeatureTableModel model = new FeatureTableModel();
	DocumentList storedDocuments = null;

	public static String INCLUDE_FILENAMES_FLAG = "Filenames";
	boolean includeFilenames;

	Map<String, Boolean> selectedOptions = new HashMap<String, Boolean>();

	public ColumnFeaturesPanel()
	{
		this("Columns to Add as Features:", true);
	}

	public ColumnFeaturesPanel(String label, boolean filenames)
	{
		includeFilenames = filenames;
		setLayout(new RiverLayout());
		add("left", new JLabel());
		describeScroll = new JScrollPane(display);
		describeScroll.setPreferredSize(new Dimension(275, 200));
		add("br hfill vfill", describeScroll);

		add.setText("All");
		add.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				for (int i = 1; i < model.getRowCount(); i++)
				{
					selectedOptions.put(getKeyObject(((CheckBoxListEntry) model.getValueAt(i, 0)).getValue().toString()), true);
				}
				refreshPanel(includeFilenames);
			}
		});

		clear.setText("None");
		clear.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				for (int i = 0; i < model.getRowCount(); i++)
				{
					selectedOptions.put(getKeyObject(((CheckBoxListEntry) model.getValueAt(i, 0)).getValue().toString()), false);
				}
				refreshPanel(includeFilenames);
			}
		});
		display.addMouseListener(new ToggleMouseAdapter(display, true)
		{

			@Override
			public void setHighlight(Object rowObj, String col)
			{
				CheckBoxListEntry check = ((CheckBoxListEntry) rowObj);
				selectedOptions.put(getKeyObject(check.getValue().toString()), !check.isSelected());
			}

		});

		add("br center", add);
		add("center", clear);
	}

	public String getKeyObject(String s)
	{
		return s.equals(INCLUDE_FILENAMES_FLAG) ? "Filenames" : s;
	}

	public Map<String, String> getSelectedColumns()
	{
		Map<String, String> select = new TreeMap<String, String>();
		for (int i = 0; i < model.getRowCount(); i++)
		{
			CheckBoxListEntry entry = (CheckBoxListEntry) model.getValueAt(i, 0);
			if (entry.isSelected())
			{
				select.put(entry.getValue().toString(),  model.getValueAt(i, 1).toString());
			}
		}

		// for(String s : selectedOptions.keySet())
		// {
		// select.add(s);
		// }

		return select;
	}

	@Override
	public void refreshPanel()
	{
		refreshPanel(includeFilenames);
	}

	public void refreshPanel(boolean filenames)
	{

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		Vector<Object> header = new Vector<Object>();
		header.add("Column Name");
		header.add("Feature Type");
		header.add("Values");
		DocumentList docs = updateDocumentList();

		final Map<String, Type> guessedTypes = new TreeMap<String, Type>();
		
		if (docs != null)
		{
			if (filenames)
			{
				if (!selectedOptions.containsKey(INCLUDE_FILENAMES_FLAG))
				{
					selectedOptions.put(INCLUDE_FILENAMES_FLAG, false);
				}
				CheckBoxListEntry checkbox = new CheckBoxListEntry(INCLUDE_FILENAMES_FLAG, selectedOptions.get(INCLUDE_FILENAMES_FLAG));
				Vector<Object> row = new Vector<Object>();
				row.add(checkbox);
				row.add(Type.NOMINAL.toString());
				row.add(docs.getFilenames().toString());
				guessedTypes.put(INCLUDE_FILENAMES_FLAG, Type.NOMINAL);
				data.add(row);
			}

			// remove old columns that don't match the new doclist
			// for(String s : new ArrayList<String>(selectedOptions.keySet()))
			// {
			// if(docs.allAnnotations().keySet().contains(s) &&
			// !s.equals(ExtractFeaturesControl.getSelectedClassAnnotation()) &&
			// !docs.getTextColumns().contains(s))
			// {
			// selectedOptions.remove(s);
			// }
			// }

			// add new columns from the new doclist
			for (String s : docs.getAnnotationNames())
			{
				if (!s.equals(ExtractFeaturesControl.getSelectedClassAnnotation()) && !docs.getTextColumns().contains(s))
				{
					if (!selectedOptions.containsKey(s))
					{
						selectedOptions.put(s, false);
					}

					CheckBoxListEntry checkbox = new CheckBoxListEntry(s, selectedOptions.get(s));
					Vector<Object> row = new Vector<Object>();
					row.add(checkbox);
					Type guessedType = docs.getValueType(s);
					guessedTypes.put(s, guessedType);
					
					Set<String> possible = docs.getPossibleAnn(s);
					if(guessedType == Type.NOMINAL)
					{
						row.add(Type.NOMINAL.toString());
					}
					else
					{
						row.add(Type.NUMERIC.toString());
					}
					if(possible.size() <= 5)
					{
						row.add(possible.size()+" unique values: "+possible);
					}
					else
					{
						row.add(possible.size()+" unique values");
					}
					data.add(row);
				}
			}
		}
		model = new FeatureTableModel(data, header);
		display.setModel(model);

		final TableColumn tableColumn = display.getColumnModel().getColumn(1);
		final JComboBox<String> combo = new JComboBox<String>();
		
		final ComboBoxModel<String> numericModel = new DefaultComboBoxModel<String>(new String[]{Type.NUMERIC.toString(), Type.NOMINAL.toString(), ColumnFeatures.CONVERT_TO_BINARY});
		final ComboBoxModel<String> nominalModel = new DefaultComboBoxModel<String>(new String[]{Type.NOMINAL.toString(), ColumnFeatures.CONVERT_TO_BINARY});
		
		tableColumn.setCellEditor(new DefaultCellEditor(combo)
		{
			public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
			{
				if(guessedTypes.get(((CheckBoxListEntry)table.getValueAt(row, 0)).getValue()) == Type.NUMERIC)
				{
					combo.setModel(numericModel);
				}
				else
				{
					combo.setModel(nominalModel);
				}
				combo.setSelectedItem(value);
				return combo;
			}
		});
		
		
//		tableColumn.setCellRenderer(new TableCellRenderer()
//		{
//			
//			@Override
//			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//			{
//				return new JComboBox(new String[]{value.toString()});
//				//return tableColumn.getCellEditor().getTableCellEditorComponent(table, value, isSelected, row, column);
//			}
//		});

		revalidate();
		repaint();

		storedDocuments = docs;
	}

	public abstract DocumentList updateDocumentList();

	public void setSelectedColumns(Collection<String> selectedColumns)
	{
		for (String s : selectedColumns)
		{
			selectedOptions.put(s, true);
		}
	}
}
