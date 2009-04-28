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

package com.adobe.epub.ops;

import java.util.Iterator;
import java.util.Vector;

import com.adobe.epub.otf.FontSubsetter;
import com.adobe.epub.style.InlineStyleRule;
import com.adobe.xml.util.SMapImpl;
import com.adobe.xml.util.XMLSerializer;

abstract public class Element {

	OPSDocument document;

	String className;
	
	InlineStyleRule style;

	String elementName;

	String id;

	Vector children = new Vector();

	XRef selfRef;
	
	Element(OPSDocument document, String name) {
		this.document = document;
		elementName = name;
	}

	abstract public String getNamespaceURI();
	
	public String getElementName() {
		return elementName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public void add(Object child) {
		children.add(child);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		document.setElementId(this, id);
	}

	public Iterator content() {
		return children.iterator();
	}

	public Object getLastChild() {
		if (children.isEmpty())
			return null;
		return children.lastElement();
	}

	public XRef getSelfRef() {
		if (selfRef == null) {
			document.assignId(this);
			selfRef = new XRef(document.resource, this);
		}
		return selfRef;
	}

	SMapImpl getAttributes() {
		SMapImpl attrs = new SMapImpl();
		if (className != null)
			attrs.put(null, "class", className);
		if (id != null)
			attrs.put(null, "id", id);
		if (style != null)
			attrs.put(null, "style", style);
		return attrs;
	}

	public void addFonts(FontSubsetter subsetter) {
		subsetter.push(this);
		try {
			Iterator children = content();
			if (children != null) {
				while (children.hasNext()) {
					Object child = children.next();
					if( child instanceof Element ) {
						((Element)child).addFonts(subsetter);
					} else if( child instanceof String ) {
						subsetter.play((String)child);
					}
				}
			}
		} finally {
			subsetter.pop(this);
		}
	}
	
	boolean isSection() {
		return false;
	}
	
	void serialize(XMLSerializer ser) {
		boolean section = isSection();
		String ns = getNamespaceURI();
		ser.startElement(ns, elementName, getAttributes(), false);
		if( section )
			ser.newLine();
		Iterator it = content();
		while (it.hasNext()) {
			Object next = it.next();
			if (next instanceof Element)
				((Element) next).serialize(ser);
			else if (next instanceof String) {
				char[] arr = ((String) next).toCharArray();
				ser.text(arr, 0, arr.length);
			}
			if( section )
				ser.newLine();
		}
		ser.endElement(ns, elementName);
	}

	public InlineStyleRule getStyle() {
		return style;
	}

	public void setStyle(InlineStyleRule style) {
		this.style = style;
	}
}
