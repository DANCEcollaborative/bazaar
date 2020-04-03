package edu.cmu.side.view.predict;

import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.PredictLabelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.DocumentListTableModel;
import edu.cmu.side.view.util.SIDETable;

public class PredictOutputPanel extends AbstractListPanel
{

	SIDETable docTable = new SIDETable();
	DocumentListTableModel model = new DocumentListTableModel(null);
	JLabel label = new JLabel("Selected Dataset");
	JScrollPane tableScroll;
	JButton exportToCSVButton = new JButton("", new ImageIcon("toolkits/icons/note_go.png"));
	JButton exportToDBButton = new JButton("Upload to DB", new ImageIcon("toolkits/icons/database_go.png"));

	public void setLabel(String l)
	{
		label.setText(l);
	}

	public PredictOutputPanel()
	{
		exportToCSVButton.setToolTipText("Export to CSV...");
		exportToCSVButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				Recipe recipe = PredictLabelsControl.getHighlightedUnlabeledData();
				if(recipe != null)
				CSVExporter.exportToCSV(model, recipe.getDocumentList().getName());
			}
		});
		exportToCSVButton.setEnabled(false);

		boolean showDB = false;
		
		try
		{
			// Test to see if the magic is there.
			Class.forName("fr.emse.tatiana.corpus.COMMSDBUploader");
			showDB = true;
			exportToDBButton.setEnabled(false);
			exportToDBButton.setToolTipText("Upload selected rows to the database");
			exportToDBButton.addActionListener(new ActionListener()
			{

				@Override
				public void actionPerformed(ActionEvent arg0)
				{
					List<Integer> selection = new ArrayList<Integer>();
					for (int i : docTable.getSelectedRows())
					{
						selection.add(docTable.convertRowIndexToModel(i));
					}
					if (selection.isEmpty()) selection = null;
				}
			});

		}
		catch (ClassNotFoundException e)
		{
			// no worries
		}

		setLayout(new RiverLayout());
		add("left", label);
		add("hfill", new JPanel());
		if(showDB)
			add("right", exportToDBButton);
		add("right", exportToCSVButton);
		docTable.setModel(model);
		docTable.setBorder(BorderFactory.createLineBorder(Color.gray));
		docTable.setRowSorter(new TableRowSorter<TableModel>(model));
		docTable.setAutoCreateColumnsFromModel(true);
		tableScroll = new JScrollPane(docTable);
		add("br hfill vfill", tableScroll);
	}

	@Override
	public void refreshPanel()
	{
		refreshPanel(PredictLabelsControl.getHighlightedUnlabeledData());
	}

	public void refreshPanel(Recipe recipe)
	{
		model.setDocumentList(null);

		if (recipe == null)
			model.setDocumentList(null);
		else
		{
			model.setDocumentList(recipe.getDocumentList());
			setLabel("Selected Dataset: " + recipe.getDocumentList().getName());
		}

		exportToCSVButton.setEnabled(recipe != null);
		exportToDBButton.setEnabled(recipe != null && recipe.getDocumentList().allAnnotations().containsKey("src-anchor"));

		// table.setModel(new DocumentListTableModel(recipe.getDocumentList()));
	}

}
