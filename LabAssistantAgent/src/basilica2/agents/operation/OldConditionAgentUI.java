package basilica2.agents.operation;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import edu.cmu.cs.lti.project911.utils.log.Logger;


public class OldConditionAgentUI extends BaseAgentUI
{

	private JCheckBox revoiceBox;
	private JCheckBox feedbackBox;
	private JCheckBox socialBox;
	public String conditionKey;
	private String defaultConditions;

	public OldConditionAgentUI(BaseAgentOperation o, String roomName)
	{
		super(o, roomName);
		this.setSize(300, 180);
	}

	protected void initComponents(String roomName)
	{
		super.initComponents(roomName);
		
		revoiceBox = new JCheckBox("Revoice", true);
		 feedbackBox = new JCheckBox("Feedback", false);
		 socialBox = new JCheckBox("Social", true);
		JPanel conditionPanel = new JPanel();
		
		conditionKey = "basilica2.agents.condition";
		defaultConditions =  "social revoice feedback";
		
		setCheckboxesFromProperty(conditionKey);

		conditionPanel.add(revoiceBox);
		conditionPanel.add(feedbackBox);
		conditionPanel.add(socialBox);

		ActionListener conditionListener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String conditions = (revoiceBox.isSelected() ? "revoice " : "") + (feedbackBox.isSelected() ? "feedback " : "")
						+ (socialBox.isSelected() ? "social " : "");
				if (conditions.isEmpty()) conditions = "none";
				System.setProperty(conditionKey, conditions);
				Logger.commonLog("ConditionAgentUI", Logger.LOG_NORMAL, "Conditions set to " + conditions);
			}

		};

		revoiceBox.addActionListener(conditionListener);
		feedbackBox.addActionListener(conditionListener);
		socialBox.addActionListener(conditionListener);
		this.launchPanel.add(conditionPanel, BorderLayout.NORTH);
		this.pack();
	}


	public void setCheckboxesFromProperty(String key)
	{
		conditionKey = key;
		String conditions;
		if(System.getProperty(key) != null)
		{
			conditions = System.getProperty(key);
		}
		else
		{
			conditions = defaultConditions;
			System.setProperty(key, conditions);
		}
		socialBox.setSelected(conditions.contains("social"));
		revoiceBox.setSelected(conditions.contains("revoice"));
		feedbackBox.setSelected(conditions.contains("feedback"));
		
	}

	public void enableAgentConfiguration(boolean enabled)
	{
		super.enableAgentConfiguration(enabled);
		setCheckboxesFromProperty(conditionKey);
		revoiceBox.setEnabled(enabled);
		feedbackBox.setEnabled(enabled);
		socialBox.setEnabled(enabled);
	}
}
