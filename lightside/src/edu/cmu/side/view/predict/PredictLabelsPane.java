package edu.cmu.side.view.predict;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.PredictLabelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.util.DocumentListTableModel;
import edu.cmu.side.view.util.RecipeExporter;
import edu.cmu.side.view.util.Refreshable;

public class PredictLabelsPane extends JPanel implements Refreshable
{

	JCheckBox useValidationBox = new JCheckBox("Copy Validation Results to Test Data");
	JCheckBox retestBox = new JCheckBox("Re-Evaluate Model on Selected Data");
	GenericLoadPanel load = new GenericLoadPanel("Model to Apply:")
	{

		{
			checkChooser();
			chooser.resetChoosableFileFilters();
			chooser.addChoosableFileFilter(RecipeExporter.getPredictModelFilter());
		}

		@Override
		public void setHighlight(Recipe r)
		{
			PredictLabelsControl.setHighlightedTrainedModelRecipe(r);
			Workbench.update(this);
		}

		@Override
		public Recipe getHighlight()
		{
			return PredictLabelsControl.getHighlightedTrainedModelRecipe();
		}

		@Override
		public void refreshPanel()
		{
			refreshPanel(Workbench.getRecipesByPane(RecipeManager.Stage.TRAINED_MODEL, RecipeManager.Stage.PREDICTION_ONLY));
		}

		@Override
		public Stage getLoadableStage()
		{
			return Stage.TRAINED_MODEL;
		}

		@Override
		public void deleteHighlight()
		{
			PredictLabelsControl.setHighlightedTrainedModelRecipe(null);
		}

	};

	ActionBar actionBar = new PredictActionBar(PredictLabelsControl.getUpdater());

	PredictNewDataPanel newData = new PredictNewDataPanel();
	PredictOutputPanel output = new PredictOutputPanel();
	DocumentListTableModel docTableModel = new DocumentListTableModel(null);
	JTable docDisplay = new JTable(docTableModel);

	public PredictLabelsPane()
	{
		setLayout(new BorderLayout());
		JSplitPane pane = new JSplitPane();

		JPanel newDataPanel = new JPanel(new BorderLayout());
		JPanel widgetPanel = new JPanel(new GridLayout(0,1));
		widgetPanel.add(useValidationBox);
		//widgetPanel.add(retestBox);

		useValidationBox.setEnabled(false);
		retestBox.setEnabled(false);
		
		newDataPanel.add(widgetPanel, BorderLayout.NORTH);
		newDataPanel.add(newData, BorderLayout.CENTER);
		useValidationBox.setToolTipText("<html>Add the predictions made during model evaluation<br>to a copy of the test data.</html>");
		retestBox.setToolTipText("<html>Use thse documents as a test set<br>and re-evaluate the model.</html>");
		retestBox.addActionListener(new ActionListener()
		{
			boolean noted = false;
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(!noted)
				{
					JOptionPane.showMessageDialog(null, "Sorry, this feature isn't hooked up yet. Stay tuned, beta-fans!");
					noted = true;
				}
			}
		});
		
		retestBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean retest = retestBox.isSelected();
				setRetestModel(retest);
			}
			
		});
		
		useValidationBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				boolean validation = useValidationBox.isSelected();
				setCopyValidationResults(validation);

			}
		});

		JPanel left = new JPanel(new GridLayout(2, 1));
		left.add(load);
		left.add(newDataPanel);
		pane.setLeftComponent(left);
		pane.setRightComponent(output);
		Dimension minimumSize = new Dimension(50, 200);
		left.setMinimumSize(minimumSize);
		output.setMinimumSize(minimumSize);
		pane.setDividerLocation(300);
		add(BorderLayout.CENTER, pane);
		add(BorderLayout.SOUTH, actionBar);

		// TODO: why can't these each be (parameterized) in genericLoadPane?
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.PREDICTION_ONLY, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, newData);
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, output);
		GenesisControl.addListenerToMap(RecipeManager.Stage.PREDICTION_RESULT, actionBar);

		GenesisControl.addListenerToMap(load, actionBar);
		GenesisControl.addListenerToMap(newData, actionBar);
		GenesisControl.addListenerToMap(newData, output);
		GenesisControl.addListenerToMap(actionBar, output);

		GenesisControl.addListenerToMap(load, this);
		GenesisControl.addListenerToMap(newData, this);
		
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.PREDICTION_ONLY, this);

	}

	public void refreshPanel()
	{
//		load.refreshPanel();
//		newData.refreshPanel();
//		output.refreshPanel(PredictLabelsControl.getHighlightedUnlabeledData());
//		actionBar.refreshPanel();

		Recipe modelRecipe = PredictLabelsControl.getHighlightedTrainedModelRecipe();
		Recipe documentRecipe = newData.getSelectedItem();//PredictLabelsControl.getHighlightedUnlabeledData();
		if (modelRecipe != null) 
		{
			if (modelRecipe.getStage() == Stage.PREDICTION_ONLY)
			{
				useValidationBox.setEnabled(false);
				setCopyValidationResults(false);
			}
			else
			{
				useValidationBox.setEnabled(true);
			}
			
			if(documentRecipe != null)
			{
				if(documentRecipe.getDocumentList().allAnnotations().keySet().contains(modelRecipe.getAnnotation()))
				{
					retestBox.setEnabled(true);
				}
				else
				{
					retestBox.setEnabled(false);
					setRetestModel(false);
				}
			}
			else
			{
				retestBox.setEnabled(false);
				setRetestModel(false);
			}
		}
		else
		{
			useValidationBox.setEnabled(false);
			retestBox.setEnabled(false);
			setCopyValidationResults(false);
			setRetestModel(false);
		}
		
	}

	protected void setCopyValidationResults(boolean validation)
	{
		newData.setEnabled(!validation);
		PredictLabelsControl.setUseValidationResults(validation);

		useValidationBox.setSelected(validation);
		
		if (validation)
		{
			PredictLabelsControl.setHighlightedUnlabeledData(PredictLabelsControl.getHighlightedTrainedModelRecipe());
			setRetestModel(false);
		}
		else
		{
			newData.refreshPanel();
			PredictLabelsControl.setHighlightedUnlabeledData(newData.getSelectedItem());
		}

		Workbench.update(newData);
	}

	protected void setRetestModel(boolean retest)	
	{
		PredictLabelsControl.setRetestModel(retest);
		retestBox.setSelected(retest);
		if(retest)
		{
			setCopyValidationResults(false);
		}

		Workbench.update(newData);
	}
}
