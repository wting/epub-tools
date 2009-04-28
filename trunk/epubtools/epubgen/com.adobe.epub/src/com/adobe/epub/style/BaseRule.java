/*******************************************************************************
* Copyright (c) 2009, Adobe Systems Incorporated
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without 
* modification, are permitted provided that the following conditions are met:
*
* ·        Redistributions of source code must retain the above copyright 
*          notice, this list of conditions and the following disclaimer. 
*
* ·        Redistributions in binary form must reproduce the above copyright 
*		   notice, this list of conditions and the following disclaimer in the
*		   documentation and/or other materials provided with the distribution. 
*
* ·        Neither the name of Adobe Systems Incorporated nor the names of its 
*		   contributors may be used to endorse or promote products derived from
*		   this software without specific prior written permission. 
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
* OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
* THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*******************************************************************************/

package com.adobe.epub.style;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

public abstract class BaseRule {

	Hashtable properties;
	
	BaseRule() {
		properties = new Hashtable();
	}

	BaseRule(Hashtable props) {
		this.properties = props;
	}

	public Object get(String property) {
		return properties.get(property);
	}

	public void set(String property, Object value) {
		if (value == null)
			properties.remove(property);
		else
			properties.put(property, value);
	}
	
	public Iterator properties() {
		return properties.keySet().iterator();
	}
	
	public abstract void serialize(PrintWriter out);

	public void serializeProperties(PrintWriter out, boolean newlines) {
		Enumeration keys = properties.keys();
		while( keys.hasMoreElements() ) {
			Object property = keys.nextElement();
			if( newlines )
				out.print('\t');
			out.print(property);
			out.print(": ");
			out.print(properties.get(property));
			out.print(";");
			if( newlines )
				out.println();
			else
				out.print(' ');
		}		
	}
	
}
