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

package com.adobe.dp.epub.style;

import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.epub.opf.FontResource;
import com.adobe.dp.epub.opf.StyleResource;

public class Stylesheet {

	StyleResource owner;
	
	Hashtable rulesBySelector = new Hashtable();
	
	Vector content = new Vector();
	
	public Stylesheet(StyleResource owner) {
		this.owner = owner;
	}
	
	public SimpleSelector getSimpleSelector(String elementName, String className) {
		return new SimpleSelector(elementName, className);
	}
	
	public Rule findRuleForSelector(Selector selector) {
		return (Rule)rulesBySelector.get(selector);
	}
	
	public Rule getRuleForSelector(Selector selector) {
		Rule rule = (Rule)rulesBySelector.get(selector);
		if( rule == null ) {
			rule = new Rule(selector);
			content.add(rule);
			rulesBySelector.put(selector, rule);
		}
		return rule;
	}
	
	public FontFace createFontFace(FontResource fontResource) {
		FontFace fontFace = new FontFace(this, fontResource);
		content.add(fontFace);
		return fontFace;
	}
	
	public Iterator content() {
		return content.iterator();
	}
	
	public void serialize(PrintWriter pout) {
		Iterator it = content();
		while( it.hasNext() ) {
			Object cp = it.next();
			if( cp instanceof Rule ) {
				((Rule)cp).serialize(pout);
			} else if( cp instanceof FontFace ) {
				((FontFace)cp).serialize(pout);
			}
		}
	}
	
}
