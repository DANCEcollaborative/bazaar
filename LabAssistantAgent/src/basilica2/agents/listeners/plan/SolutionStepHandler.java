package basilica2.agents.listeners.plan;

import java.util.Properties;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.OutputCoordinator;

class SolutionStepHandler implements StepHandler
{
    private static final String DEFAULT_MESSAGE = "The wizard's solution is not configured.";
    private final Properties planProperties;

    SolutionStepHandler()
    {
        planProperties = PropertiesLoader.loadProperties("PlanExecutor.properties");
    }

    public static String getStepType()
    {
        return "solution";
    }

    @Override
    public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
    {
        String propertyKey = currentStep.attributes.get("property");
        String messageText = lookupProperty(propertyKey);

        Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL,
                "Sending solution message for " + currentStep.name + " using property " + propertyKey);

        MessageEvent solutionMessage = new MessageEvent(source, overmind.getAgent().getUsername(), messageText, propertyKey);
        source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "SolutionStep", solutionMessage,
                OutputCoordinator.HIGH_PRIORITY, 5.0, 2));

        overmind.stepDone();
    }

    private String lookupProperty(String key)
    {
        if (key == null || key.isEmpty() || planProperties == null)
        {
            return DEFAULT_MESSAGE;
        }
        String value = planProperties.getProperty(key);
        if (value == null || value.trim().isEmpty())
        {
            return DEFAULT_MESSAGE;
        }
        return value.trim();
    }
}
