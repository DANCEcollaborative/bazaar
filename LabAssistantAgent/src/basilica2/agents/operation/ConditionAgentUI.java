package basilica2.agents.operation;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.cmu.cs.lti.project911.utils.log.Logger;


public class ConditionAgentUI extends BaseAgentUI
{

	private Map<String, JCheckBox> conditionBoxes;
	public String conditionKey;
	private String[] defaultConditions;

	public ConditionAgentUI(BaseAgentOperation o, String roomName, String... allConditions)
	{
		super(o, roomName);
		initConditionComponents(allConditions);
		this.setSize(350, 250);
	}

	public void setDefaultConditions(String... defaults)
	{
		defaultConditions = defaults;
		for(String c : defaults)
		{
			if(conditionBoxes.containsKey(c))
			{
				conditionBoxes.get(c).setSelected(true);
			}
		}
		System.setProperty(conditionKey, getConditionString());
	}
	
	protected void initConditionComponents(String... allConditions)
	{
		defaultConditions = new String[0];
		conditionBoxes = new HashMap<String, JCheckBox>();
		conditionKey = "basilica2.agents.condition";
		JPanel checkBoxPanel = new JPanel(new GridLayout(0, 3));
		JPanel conditionPanel = new JPanel(new BorderLayout());
		conditionPanel.add(new JLabel("Conditions:"), BorderLayout.NORTH);
		
		ActionListener conditionListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String conditions = getConditionString();
				System.setProperty(conditionKey, conditions);
				Logger.commonLog("ConditionAgentUI", Logger.LOG_NORMAL, "Conditions set to " + conditions);
			}

		};

		for (String condition : allConditions)
		{
			JCheckBox box = new JCheckBox(condition, false);
			conditionBoxes.put(condition, box);
			
			checkBoxPanel.add(box);
			box.addActionListener(conditionListener);
		}
		
		setCheckboxesFromProperty(conditionKey);

		conditionPanel.add(checkBoxPanel, BorderLayout.CENTER);
		this.launchPanel.add(conditionPanel, BorderLayout.NORTH);
		this.pack();
	}


	public String getConditionString()
	{
		String conditions = "";
		for(Entry<String, JCheckBox> pair : conditionBoxes.entrySet())
		{
			if(pair.getValue().isSelected())
				conditions += pair.getKey()+" ";
		}
		if (conditions.isEmpty()) return "none";
		else return conditions.trim();
	}
	
	public String getDefaultConditionString()
	{
		if(defaultConditions.length == 0) return "none";
		
		String conditions = "";
		for(String c : defaultConditions)
		{
			conditions += c + " ";
		}
		return conditions.trim();
		
	}
	
	public void setCheckboxesFromProperty(String key)
	{
		conditionKey = key;
		String[] conditions;
		if(System.getProperty(key) != null)
		{
			conditions = System.getProperty(key).split("\\s+");
		}
		else
		{
			conditions = defaultConditions;
			System.setProperty(key, getDefaultConditionString());
		}

		for(String c : conditions)
		{
			if(conditionBoxes.containsKey(c))
			{
				conditionBoxes.get(c).setSelected(true);
			}
		}
		
	}

	public void enableAgentConfiguration(boolean enabled)
	{
		super.enableAgentConfiguration(enabled);
		setCheckboxesFromProperty(conditionKey);
		for(JCheckBox box : conditionBoxes.values())
		{
			box.setEnabled(enabled);
		}
	}

	public void selectAllConditions()
	{
		setDefaultConditions(conditionBoxes.keySet().toArray(new String[0]));
	}
}
