package basilica2.chatter.components;

import edu.cmu.cs.lti.basilica2.core.Agent;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;

public class MultiActorInputCoordinator extends InputCoordinator
{
	private String[] agentNames;
	
	public MultiActorInputCoordinator(Agent a, String n, String pf)
	{
		super(a, n, pf);
		if(myProperties != null)
			agentNames = System.getProperty("chatterbox.users", "Tutor").split(",");
	}
	
	
	public String[] getAgentNames()
	{
		return agentNames;
	}


	public void setAgentNames(String... agentNames)
	{
		this.agentNames = agentNames;
	}


	@Override
	public boolean isAgentName(String from)
	{
		for(String name : agentNames)
		{
			if(from.contains(name))
				return true;
		}
		return false;
	}

}
