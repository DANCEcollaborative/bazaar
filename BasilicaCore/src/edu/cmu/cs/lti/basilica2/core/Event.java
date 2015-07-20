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
package edu.cmu.cs.lti.basilica2.core;

import edu.cmu.cs.lti.project911.utils.IdGenerator;
import java.io.Serializable;

/**
 *
 * @author rohitk
 */
public abstract class Event implements Serializable {

    transient protected Component sender;
    Long id = null;
	private long timestamp;
	protected boolean validity;
    private Object original;

    public Event()
    {
        sender = null;
        validity = true;
        renew();
    }
    
    public Event(Component s) 
    {
        sender = s;
        validity = true;
        renew();
    }

    public abstract String getName();

    public Component getSender() {
        return sender;
    }

    public Event forwardBy(Component from) {
        sender = from;
        return this;
    }

    /**
     * Its ok to reuse events as long as we renew them
     */
    public void renew() 
    {
        id = IdGenerator.get();
        timestamp = System.currentTimeMillis();
    }

    public void invalidate() {
        validity = false;
    }

    public boolean isValid() {
        return validity;
    }

    public Long getId() {
        return id;
    }

    //Only need to make a string out of the event data (not name, sender, etc.)
    @Override
    public abstract String toString();

	public void setSender(Component retransmitter)
	{
		this.sender = retransmitter;
	}

	public Object getOriginal()
	{
		return original;
	}

	public void setOriginal(Object original)
	{
		this.original = original;
	}

    public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

}
