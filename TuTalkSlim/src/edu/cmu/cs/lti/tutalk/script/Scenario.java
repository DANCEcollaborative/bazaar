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
package edu.cmu.cs.lti.tutalk.script;

import edu.cmu.cs.lti.tutalk.module.ModelConcept;
import edu.cmu.cs.lti.tutalk.module.ModelConcept.Predictor;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author rohitk
 */
public class Scenario
{

	private static String start_goal_name = "start";
	private String myName;
	private ConceptLibrary myConceptLibrary;
	private List<Goal> myGoals;

	public Scenario(String name)
	{
		myName = name;
		myConceptLibrary = new ConceptLibrary();
		myGoals = new ArrayList<Goal>();
	}

	public ConceptLibrary getConceptLibrary()
	{
		return myConceptLibrary;
	}

	public Goal getStartGoal()
	{
		return getGoal(start_goal_name);
	}

	public Goal getGoal(String gname)
	{
		for (int i = 0; i < myGoals.size(); i++)
		{
			if (myGoals.get(i).getName().equalsIgnoreCase(gname)) { return myGoals.get(i); }
		}
		return null;
	}

	public String getName()
	{
		return myName;
	}

	public void addGoal(Goal g)
	{
		myGoals.add(g);
	}

	public static Scenario loadScenario(String filename)
	{
		Scenario sc = null;
		Map<String, Predictor> predictors = new HashMap<String, Predictor>();

		try
		{
			DOMParser parser = new DOMParser();
			System.err.println("parsing...");
			parser.parse(filename);
			System.err.println("done parsing!");
			Document dom = parser.getDocument();
			NodeList ns1 = dom.getElementsByTagName("scenario");
			if ((ns1 != null) && (ns1.getLength() != 0))
			{
				Element scenarioElement = (Element) ns1.item(0);
				sc = new Scenario(filename.replace(".xml", ""));

				// Load and configure any NLP components

				NodeList configElement = scenarioElement.getElementsByTagName("configuration");
				if (configElement != null && configElement.getLength() > 0)
				{
					NodeList moduleNodes = ((Element) configElement.item(0)).getElementsByTagName("module");
					if (moduleNodes != null && moduleNodes.getLength() > 0)
					{
						for (int i = 0; i < moduleNodes.getLength(); i++)
						{
							Element module = (Element) moduleNodes.item(i);
							if (module.getAttribute("kind").equals("model"))
							{
								String classname = module.getAttribute("classname");
								String name = module.getAttribute("name");

								HashMap<String, String> paramDict = new HashMap<String, String>();

								NodeList params = module.getElementsByTagName("param");
								if (params != null)
								{
									for (int j = 0; j < params.getLength(); j++)
									{
										Element param = (Element) params.item(j);
										if (param.hasAttribute("key") && param.hasAttribute("value"))
										{
											String key = param.getAttribute("key");
											String value = param.getAttribute("value");
											paramDict.put(key, value);
										}

									}
								}

								Constructor moduleMaker = Class.forName(classname).getConstructor(String.class, Map.class);
								Predictor pete = (Predictor) moduleMaker.newInstance(name, paramDict);
								predictors.put(pete.getName(), pete);
							}
						}
					}
				}

				// Read the concepts

				NodeList ns2 = scenarioElement.getElementsByTagName("concepts");
				if ((ns2 != null) && (ns2.getLength() != 0))
				{
					Element conceptsElement = (Element) ns2.item(0);
					NodeList ns3 = conceptsElement.getElementsByTagName("concept");
					if ((ns3 != null) && (ns3.getLength() != 0))
					{
						for (int i = 0; i < ns3.getLength(); i++)
						{
							Element conceptElement = (Element) ns3.item(i);
							String label = conceptElement.getAttribute("label");

							Concept c;
							if (conceptElement.hasAttribute("model"))
							{
								// use the given model to predict this label
								c = new ModelConcept(label, predictors.get(conceptElement.getAttribute("model")));
							}
							if (conceptElement.hasAttribute("type") && conceptElement.getAttribute("type").equals("regex"))
							{
								c = new RegExConcept(label);
								NodeList ns4 = conceptElement.getElementsByTagName("phrase");
								if ((ns4 != null) && (ns4.getLength() != 0))
								{
									for (int j = 0; j < ns4.getLength(); j++)
									{
										Element phraseElement = (Element) ns4.item(j);
										((RegExConcept) c).addPattern(phraseElement.getTextContent().trim());
									}
								}
							}
							else if (conceptElement.hasAttribute("type") && conceptElement.getAttribute("type").equals("annotation"))
							{
								c = new AnnotationConcept(label);
								NodeList ns4 = conceptElement.getElementsByTagName("phrase");
								if ((ns4 != null) && (ns4.getLength() != 0))
								{
									for (int j = 0; j < ns4.getLength(); j++)
									{
										Element phraseElement = (Element) ns4.item(j);
										((AnnotationConcept) c).addAnnotation(phraseElement.getTextContent().trim());
									}
								}
							}
							else
							{
								c = new DictionaryConcept(label);
								NodeList ns4 = conceptElement.getElementsByTagName("phrase");
								if ((ns4 != null) && (ns4.getLength() != 0))
								{
									for (int j = 0; j < ns4.getLength(); j++)
									{
										Element phraseElement = (Element) ns4.item(j);
										((DictionaryConcept) c).addPhrase(phraseElement.getTextContent().trim());
									}
								}
							}
							sc.getConceptLibrary().addConcept(c);
						}
					}

				}

				// Add some default concepts
				// dont_know
				DictionaryConcept cdk = new DictionaryConcept("_dont_know_");
				cdk.addPhrase("dont know");
				cdk.addPhrase("not sure");
				cdk.addPhrase("what");
				cdk.addPhrase("you tell me");
				cdk.addPhrase("how should i know");
				cdk.addPhrase("confused");
				sc.getConceptLibrary().addConcept(cdk);

				Map<String, Goal> goals = new Hashtable<String, Goal>();

				// Read the Script
				NodeList ns5 = scenarioElement.getElementsByTagName("script");
				if ((ns5 != null) && (ns5.getLength() != 0))
				{
					Element scriptElement = (Element) ns5.item(0);
					NodeList ns6 = scriptElement.getElementsByTagName("goal");
					if ((ns6 != null) && (ns6.getLength() != 0))
					{
						for (int i = 0; i < ns6.getLength(); i++)
						{
							Element goalElement = (Element) ns6.item(i);
							String name = goalElement.getAttribute("name");
							Goal g = new Goal(name);
							goals.put(name, g);
						}
						for (int i = 0; i < ns6.getLength(); i++)
						{
							Element goalElement = (Element) ns6.item(i);
							String name = goalElement.getAttribute("name");
							Goal g = goals.get(name);
							NodeList ns7 = goalElement.getElementsByTagName("step");
							if ((ns7 != null) && (ns7.getLength() != 0))
							{
								for (int j = 0; j < ns7.getLength(); j++)
								{
									Element stepElement = (Element) ns7.item(j);
									Step s = null;
									// Detemine the type of step it is &
									// intantiate it
									NodeList ns8 = stepElement.getElementsByTagName("subgoal");
									if ((ns8 != null) && (ns8.getLength() != 0))
									{
										Element subGoalStepElement = (Element) ns8.item(0);
										String subgoalname = subGoalStepElement.getTextContent();
										s = new SubGoalStep(goals.get(subgoalname));
									}
									else
									{
										NodeList ns10 = stepElement.getElementsByTagName("initiation");
										if ((ns10 != null) && (ns10.getLength() != 0))
										{
											Element initiationElement = (Element) ns10.item(0);
											String initConceptName = initiationElement.getTextContent();
											Concept initiator = sc.getConceptLibrary().getConcept(initConceptName);
											if(initiator == null)
											{
												System.err.println("WARNING: No concept defined for say=\""+initConceptName+"\" in "+filename);
												initiator = makeDummyConcept(initConceptName);
											}
											Initiation init = new Initiation(initiator);
											NodeList ns9 = stepElement.getElementsByTagName("response");
											if ((ns9 != null) && (ns9.getLength() != 0))
											{
												s = new InitiationResponseStep(init);
												for (int k = 0; k < ns9.getLength(); k++)
												{
													Element responseElement = (Element) ns9.item(k);
													String respConceptName = responseElement.getTextContent();
													Response r = null;
													String sayConceptName = responseElement.getAttribute("say");
													String pushGoalName = responseElement.getAttribute("push");
													
													Concept conceptToMatch = sc.getConceptLibrary().getConcept(respConceptName);
													if(conceptToMatch == null)
													{
														System.err.println("WARNING: No concept defined for <response>\""+respConceptName+"\"</response> in "+filename);
														conceptToMatch = makeDummyConcept(respConceptName);
													}

													Feedback sayFeedback = null;
													if (sayConceptName != null)
													{
														Concept feedbackConcept = sc.getConceptLibrary().getConcept(sayConceptName);
														if(feedbackConcept == null)
														{
															System.err.println("WARNING: No concept defined for say=\""+sayConceptName+"\" in "+filename);
															feedbackConcept = makeDummyConcept(sayConceptName);
														}
														sayFeedback = new Feedback(feedbackConcept);
													}

													if (pushGoalName != null && pushGoalName.length() != 0)
													{
														Goal subgoal = goals.get(pushGoalName);
														if(subgoal == null)
															System.err.println("WARNING: No goal defined for push=\""+pushGoalName+"\" in "+filename);
														else
															r = new SubGoalResponse(conceptToMatch, subgoal, sayFeedback);
													}
													else
													{
														r = new FeedbackResponse(conceptToMatch, sayFeedback);
													}
													((InitiationResponseStep) s).addResponse(r);
												}
											}
											else
											{
												s = new InitiationStep(init);
											}
										}
									}
									g.addStep(s);
								}
							}
							sc.addGoal(g);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return sc;
	}

	protected static Concept makeDummyConcept(String respConceptName)
	{
		Concept conceptToMatch;
		conceptToMatch = new DictionaryConcept(respConceptName);
		((DictionaryConcept)conceptToMatch).addPhrase(respConceptName);
		return conceptToMatch;
	}
}
