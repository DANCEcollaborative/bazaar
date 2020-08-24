package edu.cmu.side.view.generic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.view.util.AbstractListPanel;

public abstract class ActionBar extends AbstractListPanel{
	
	protected JButton actionButton = new JButton();
	protected JButton cancel = new JButton();
	
	protected JProgressBar progressBar = new JProgressBar();
	protected JTextField name = new JTextField(15);
	protected JLabel nameLabel = new JLabel("Name:");
	protected JPanel settings = new JPanel(new RiverLayout());
	protected JComboBox combo;
	protected JPanel updaters = new JPanel(new RiverLayout());
	
	protected String defaultName;
	protected Stage recipeStage;
	
	protected Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);

	protected JPanel left = new JPanel(new RiverLayout());
	protected JPanel middle = new JPanel(new RiverLayout());
	protected JPanel right = new JPanel(new RiverLayout());
	
	protected StatusUpdater update;
	
	protected boolean nameEdited = false;

	public ActionBar(String def, Stage stage, StatusUpdater update){
		this(update);
		defaultName = def;
		recipeStage = stage;
	}
	public ActionBar(StatusUpdater update)
	{
		this.update = update;
		
		setLayout(new RiverLayout());
		actionButton.setFont(font);
		actionButton.setEnabled(false);
		setBackground(Color.white);
		setBorder(BorderFactory.createLineBorder(Color.gray));
		settings.setBackground(Color.white);
		updaters.setBackground(Color.white);
		settings.add("left", nameLabel);
		settings.add("left", name);
		progressBar.setPreferredSize(new Dimension(50,25));
		updaters.add("hfill", (Component)update);
		updaters.add("right", progressBar);
		progressBar.setVisible(false);
		ImageIcon iconCancel = new ImageIcon("toolkits/icons/cancel.png");
		cancel.setText("");
		cancel.setIcon(iconCancel);
		cancel.setEnabled(false);
		cancel.setToolTipText("Cancel");
		right.add("hfill", updaters);
		right.add("left", cancel);
		right.setBackground(Color.white);
		add("left", actionButton);
		add("hfill", settings);
		add("left", right);
		
		name.addKeyListener(new KeyAdapter()
		{

			@Override
			public void keyReleased(KeyEvent e)
			{
				nameEdited = true;
			}
		});
	}

	

	@Override
	public void refreshPanel()
	{
		if(getDefaultName() != null && !nameEdited || name.getText().trim().isEmpty())
		{
			name.setText(Workbench.getRecipeManager().getAvailableRecipeName(getDefaultName(), getRecipeStage()));		
			name.setCaretPosition(0);
		}
	}

	public void setDefaultName(String def){
		defaultName = def;
	}
	
	public void setRecipeStage(Stage stage){
		recipeStage = stage;
	}
	
	public String getDefaultName(){
		return defaultName;
	}
	
	public Stage getRecipeStage(){
		return recipeStage;
	}
	
	public abstract void startedTask();

	public abstract void endedTask();
	
	public void enable(boolean enabled)
	{
		actionButton.setEnabled(enabled);
	}
	public boolean isEnabled()
	{
		return actionButton.isEnabled();
	}
}
