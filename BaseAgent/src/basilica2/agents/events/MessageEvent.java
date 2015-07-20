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
package basilica2.agents.events;

import de.fhg.ipsi.chatblocks2.model.IReferenceableDocument;
import de.fhg.ipsi.chatblocks2.model.messagebased.ChatMessage;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import basilica2.agents.components.InputCoordinator;

/**
 * 
 * @author rohitk
 */
public class MessageEvent extends Event implements Serializable, Cloneable
{

	public static String GENERIC_NAME = "MESSAGE_EVENT";
	protected String from;
	protected String text;
	protected Map<String, List<String>> annotations;
	protected boolean ackExpected = false;
	protected long typingDuration;
	protected Event referent; 

	public MessageEvent(Component source, String from, String message)
	{
		super(source);
		this.from = from;
		text = message;
	}

	public MessageEvent(Component source, String from, String message, String... annotations)
	{
		this(source, from, message);
		
		if(annotations.length > 0)
		{
			List<String> wholeMessage = Arrays.asList(message);
			for(String note : annotations)
			{
				this.addAnnotation(note, wholeMessage);
			}
		}
		
	}

	public String getFrom()
	{
		return from;
	}

	public String getText()
	{
		return text;
	}

	@Override
	public String getName()
	{
		return GENERIC_NAME;
	}

	public boolean isAcknowledgementExpected()
	{
		return ackExpected;
	}

	public void setAcknowledgementExpected(boolean a)
	{
		ackExpected = a;
	}

	public void addAnnotation(String a, List<String> ps)
	{
		if (annotations == null)
		{
			annotations = new Hashtable<String, List<String>>();
		}
		annotations.put(a.trim(), ps);
	}

	public void removeAnnotation(String a)
	{
		if (annotations != null)
		{
			annotations.remove(a);
		}
	}

	public String getAnnotationString()
	{
		String ret = "";
		if (annotations != null)
		{
			String[] keys = annotations.keySet().toArray(new String[0]);
			for (int i = 0; i < keys.length; i++)
			{
				if (i != 0)
				{
					ret += "+";
				}
				ret += keys[i];
			}
		}

		return ret;
	}

	public String[] getAllAnnotations()
	{
		String[] strings = new String[0];
		if (annotations != null)
			return annotations.keySet().toArray(strings);
		else
			return strings;
	}

	public boolean hasAnyAnnotations(String... annotations)
	{
		if (this.annotations != null)
		{
			for (String a : annotations)
			{
				if (this.annotations.containsKey(a)) return true;
			}
		}
		return false;
	}
	
	public boolean hasAnnotations(String... annotations)
	{
		if (this.annotations != null)
		{
			for (String a : annotations)
			{
				if (!this.annotations.containsKey(a)) return false;
			}
			return true;
		}
		return false;
	}

	public String[] checkAnnotation(String a)
	{
		if (annotations != null)
		{
			List<String> ps;
			if ((ps = annotations.get(a)) != null)
			{
				return ps.toArray(new String[0]);
			}
			else
			{
				return null;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return "Message from "+from+": "+text+" "+getAnnotationString();
	}

	public void setTypingDuration(long millis)
	{
		this.typingDuration = millis;
	}

	public Object getTypingDuration()
	{
		return typingDuration;
	}

	public void addAnnotations(String... annotations2)
	{
		List<String> wholeMessage = Arrays.asList(text);
		for(String a : annotations2)
		{
			if(!hasAnnotations(a))
			{
				addAnnotation(a, wholeMessage);
			}
		}
	}

	public Event getReference()
	{
		return referent;
	}

	public void setReference(Event referent)
	{
		this.referent = referent;
	}
	
	public MessageEvent cloneMessage(String newText)
	{
		MessageEvent e;
		try
		{
			e = (MessageEvent) this.clone();
			e.text = newText;
			return e;
		}
		catch (CloneNotSupportedException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return this;
	}

	public String[] getParts()
	{
		return this.getText().split("\\|");
	}

	public void setText(String t)
	{
		text = t;
	}

}
