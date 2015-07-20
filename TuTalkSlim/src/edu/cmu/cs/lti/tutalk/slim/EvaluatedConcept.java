package edu.cmu.cs.lti.tutalk.slim;

import edu.cmu.cs.lti.tutalk.script.Concept;

public class EvaluatedConcept implements Comparable<EvaluatedConcept>
{
	public Concept concept;
	public double value;
	
	public EvaluatedConcept(Concept c, double v)
	{
		concept = c;
		value = v;
	}
	
	@Override
	public int compareTo(EvaluatedConcept ec)
	{
		return Double.compare(value, ec.value);
	}
	
	public String toString()
	{
		return value+":\t"+concept.toString();
	}
	
}