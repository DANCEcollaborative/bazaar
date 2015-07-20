/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.listeners.plan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.StepDoneEvent;
import basilica2.agents.events.priority.PrioritySource;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

/**
 * 
 * @author rohitk + frankensteining by dadamson
 */
public class PlanExecutor extends BasilicaAdapter implements TimeoutReceiver
{
	public static final String PROGRESS_TIMER_ID = "_PROGRESS_STAGE_";

	class PlanSource extends PrioritySource
	{
		public PlanSource(String name)
		{
			super(name, false);
		}

		@Override
		public boolean isBlocking()
		{
			return expectingRequestDetection;
		}
	}

	private String plan_steps_file = "plans/plan_steps.xml";
	private String planStatusFilename = "planstatus/plan_status.plan_file";

	Plan currentPlan = null;
	private Timer interStepTimer = null;

	private int default_interstep_timeout = 5; // seconds
	int interstepDelay = default_interstep_timeout;

	private boolean expectingRequestDetection = false;

	InputCoordinator source;
	private Collection<BasilicaAdapter> activeHelpers = new HashSet<BasilicaAdapter>();
	private Map<String, Collection<StepHandler>> handlers = new HashMap<String, Collection<StepHandler>>();
	private boolean launched;

	public PlanExecutor(Agent a)
	{
		super(a, "PlanExecutor");
		// Load the properties from file
		if (properties != null)
		{
			this.default_interstep_timeout = Integer.parseInt(properties.getProperty("planexecutor.interstepseconds", "15"));
			this.setPlanFilename(properties.getProperty("planexecutor.plan_file", plan_steps_file));

			String[] handlerNames = properties.getProperty("planexecutor.step_handlers", "").split("[\\s,]+");
			for (String element : handlerNames)
			{
				try
				{
					String[] typeAndClass = element.split(":");
					Class c = Class.forName(typeAndClass[1]);
					StepHandler mrHandy = (StepHandler) c.newInstance();
					this.addStepHandler(typeAndClass[0], mrHandy);
				}
				catch (Exception e)
				{
					log(getClass().getSimpleName(), Logger.LOG_ERROR, "could not load StepHandler class for " + element);
					e.printStackTrace();
				}
			}

			// this.addStepHandler(PromptStepHandler.getStepType(),
			// new
			// PromptStepHandler((properties.getProperty("planexecutor.plan_prompts",
			// "plans/plan_prompts.xml"))));
		}
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { LaunchEvent.class, StepDoneEvent.class };
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		this.source = source;

		if (event instanceof LaunchEvent)
			this.handleLaunchEvent((LaunchEvent) event);
		else if (event instanceof StepDoneEvent) this.handleStepDoneEvent((StepDoneEvent) event);

	}

	void activateStage(String id)
	{
		Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "Firing Stage '" + id + "'");

		if (currentPlan.currentStage != null)
		{
			this.updatePlanStatusFile(this.currentPlan.currentStage.name);
			currentPlan.currentStage.isRunning = false;
			currentPlan.currentStage.isDone = true;

			if (interStepTimer != null)
			{
				interStepTimer.stopAndQuit();
			}

			// UnblockEvent ube = new UnblockEvent(this);
			// this.dispatchEvent(getAgent().getComponent(moves_controller_name),
			// ube);
		}

		Stage stage = currentPlan.stages.get(id);
		if (stage != null)
		{
			stage.isRunning = true;
			currentPlan.currentStage = stage;
			currentPlan.currentStage.progressStage(source);
		}
	}

	void clearHelpers()
	{
		synchronized (activeHelpers)
		{
			for (BasilicaAdapter helper : activeHelpers)
			{
				helper.stopListening(source);
			}
			activeHelpers.clear();
		}
	}

	public void addHelper(BasilicaAdapter helper)
	{
		synchronized (activeHelpers)
		{
			activeHelpers.add(helper);
			helper.startListening(source);
		}
	}

	public void addStepHandler(String stepType, StepHandler mrHandy)
	{
		if (!handlers.containsKey(stepType))
		{
			handlers.put(stepType, new HashSet<StepHandler>());
		}
		handlers.get(stepType).add(mrHandy);
	}

	private void handleLaunchEvent(LaunchEvent pe)
	{
		if (!isLaunched() && currentPlan != null) currentPlan.launchStages(this);
		setLaunched(true);
	}

	private void handleStepDoneEvent(StepDoneEvent dpe)
	{
		log(Logger.LOG_NORMAL, "received step done event for " + dpe.getName());
		stepDone();

	}

	/**
	 * clear all helpers, and wait *delay* seconds until starting the next step.
	 */
	public void stepDone()
	{

		clearHelpers();
		if (interstepDelay > 0)
		{
			String currentStep = currentPlan.getCurrentStep();
			if(currentStep != null)
			{
				Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL,
						"starting " + interstepDelay + " second delay timer for " + currentStep);
				interStepTimer = new Timer(interstepDelay, currentStep, this);
				interStepTimer.start();
			}
		}
		else
		{
			currentPlan.currentStage.progressStage(source);
		}
	}

	public void timedOut(String id)
	{
		// informObservers("<timedout id=\"" + id + "\" />");
		if (id.equals(PROGRESS_TIMER_ID)
				|| (currentPlan != null && currentPlan.currentStage != null && id.equals(currentPlan.currentStage.currentStep.name)))
		{
			clearHelpers();
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "PLAN: current step done: " + id);
			currentPlan.currentStage.progressStage(source);

		}
		else if (id != null && ((currentPlan.currentStage != null && id.equals(currentPlan.currentStage.nextStage)) || currentPlan.stages.containsKey(id)))
		{
			clearHelpers();
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "PLAN: activiating next stage: " + id);
			activateStage(id);
		}
		else
		{
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING, "PLAN: no idea what to do with timeout key " + id);
		}
	}

	public void log(String from, String level, String msg)
	{
		log(level, from + ": " + msg);
	}

	public String getStatus()
	{
		return currentPlan.name + " > " + currentPlan.currentStage.name + " > " + currentPlan.getCurrentStep();
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] {};
	}

	public Collection<StepHandler> getHandlers(String type)
	{
		return handlers.get(type);
	}

	public void setPlanFilename(String filename)
	{
		plan_steps_file = filename;
		informObservers("<configuration planfile=\"" + plan_steps_file + "\">");
		// Load Plan and Status
		currentPlan = loadPlan(plan_steps_file);
		planStatusFilename = "planstatus" + File.separator + getAgent().getName() + ".planstatus.txt";

		File planStatusFile = new File(planStatusFilename);
		if (planStatusFile.exists() && !System.getProperties().containsKey("basilica2.handsfree"))
		{
			int choice = JOptionPane.showConfirmDialog(null, "There's already a plan status file - Keep going from last time?");
			if (choice == JOptionPane.NO_OPTION) planStatusFile.delete();
		}
		else
		{
			try
			{
				planStatusFile.createNewFile();
			}
			catch (IOException ex)
			{
				log(getClass().getSimpleName(), Logger.LOG_ERROR, "Can't do that..." + ex);
			}
		}

		readPlanStatusFile();
	}

	private Plan loadPlan(String f)
	{
		Plan p = null;
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList planNodes = dom.getElementsByTagName("plan");
			if ((planNodes != null) && (planNodes.getLength() != 0))
			{
				Element planNode = (Element) planNodes.item(0);
				p = new Plan();
				p.name = planNode.getAttribute("name");
				NodeList stageNodes = planNode.getElementsByTagName("stage");
				if ((stageNodes != null) && (stageNodes.getLength() != 0))
				{
					for (int i = 0; i < stageNodes.getLength(); i++)
					{
						Element stageNode = (Element) stageNodes.item(i);
						Stage s = new Stage(this);
						s.name = stageNode.getAttribute("name");
						s.type = stageNode.getAttribute("type");

						if (stageNode.hasAttribute("delay"))
						{
							s.delay = Integer.parseInt(stageNode.getAttribute("delay"));
							if (i == 0) s.timeout = s.delay;
						}
						if (stageNode.hasAttribute("timeout")) s.timeout = Integer.parseInt(stageNode.getAttribute("timeout"));

						if (i + 1 < stageNodes.getLength()) s.nextStage = ((Element) stageNodes.item(i + 1)).getAttribute("name");

						NodeList stepNodes = stageNode.getElementsByTagName("step");
						if ((stepNodes != null) && (stepNodes.getLength() != 0))
						{
							for (int j = 0; j < stepNodes.getLength(); j++)
							{
								Element stepNode = (Element) stepNodes.item(j);
								Step t = new Step(this);
								String type = stepNode.getAttribute("type");
								t.type = type.toLowerCase();
								if (stepNode.hasAttribute("delay")) 
										t.delay = Integer.parseInt(stepNode.getAttribute("delay"));
								if (stepNode.hasAttribute("timeout")) 
										t.timeout = Integer.parseInt(stepNode.getAttribute("timeout"));
								t.name = stepNode.getTextContent();
								s.steps.add(t);

								NamedNodeMap nm = stepNode.getAttributes();
								Map<String, String> attributes = new HashMap<String, String>();
								for (int ix = 0; ix < nm.getLength(); ix++)
								{

									Node item = nm.item(ix);
									attributes.put(item.getNodeName(), item.getTextContent());
								}
								t.setAttributes(attributes);

							}
						}
						p.stages.put(s.name, s);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return p;
	}

	private void readPlanStatusFile()
	{
		try
		{
			BufferedReader fr = new BufferedReader(new FileReader(planStatusFilename));
			String line = fr.readLine();
			int read = 0, removed = 0;
			while (line != null)
			{
				read++;

				if (currentPlan.removeStep(line.trim()))
				{
					removed++;
				}
				line = fr.readLine();
			}
			fr.close();
			log(Logger.LOG_NORMAL, "Read " + read + " plan items and Removed " + removed);
		}
		catch (Exception e)
		{
			log(Logger.LOG_ERROR, "Error while reading Status File (" + e.toString() + ")");
		}
	}

	void updatePlanStatusFile(String doneStep)
	{
		try
		{
			FileWriter fw = new FileWriter(planStatusFilename, true);
			fw.write(doneStep + "\n");
			log(Logger.LOG_NORMAL, "Updated " + doneStep + " to plan status file");
			fw.flush();
			fw.close();
		}
		catch (Exception e)
		{
			log(Logger.LOG_ERROR, "Error while updating Status File (" + e.toString() + ")");
		}
	}

	public boolean isLaunched()
	{
		return launched;
	}

	public void setLaunched(boolean launched)
	{
		this.launched = launched;
	}

}
