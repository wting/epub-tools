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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.epub.opf.FontResource;
import com.adobe.dp.epub.opf.StyleResource;

public class Stylesheet {

	StyleResource owner;

	Hashtable rulesBySelector = new Hashtable();

	Hashtable lockedRulesByProperties = new Hashtable();

	Vector content = new Vector();

	public Stylesheet(StyleResource owner) {
		this.owner = owner;
	}

	public Stylesheet(StyleResource owner, Reader reader) throws IOException {
		this.owner = owner;
		SimpleStylesheetParser parser = new SimpleStylesheetParser();
		parser.readRules(reader);
		initWithParser(parser);		
	}

	public Stylesheet(StyleResource owner, SimpleStylesheetParser parser) throws IOException {
		this.owner = owner;
		initWithParser(parser);
	}

	private void initWithParser(SimpleStylesheetParser parser) {
		rulesBySelector = parser.getRules();
		Enumeration rules = rulesBySelector.elements();
		while (rules.hasMoreElements()) {
			Object robj = rules.nextElement();
			if (robj instanceof Rule) {
				Rule rule = (Rule) robj;
				content.add(rule);
			}
		}
	}

	public SimpleSelector getSimpleSelector(String elementName, String className) {
		return new SimpleSelector(elementName, className);
	}

	public Rule findRuleForSelector(Selector selector) {
		return (Rule) rulesBySelector.get(selector);
	}

	public Rule getRuleForSelector(Selector selector) {
		Rule rule = (Rule) rulesBySelector.get(selector);
		if (rule == null) {
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

	public void lock(Rule rule) {
		rule.lock();
		if (rule.selector instanceof SimpleSelector && ((SimpleSelector) rule.selector).getElementName() == null)
			lockedRulesByProperties.put(rule.properties, rule);
	}

	public PrototypeRule createPrototypeRule() {
		return new PrototypeRule();
	}

	public Rule getClassRuleForPrototype(PrototypeRule props) {
		Rule rule = (Rule) lockedRulesByProperties.get(props.properties);
		return rule;
	}

	public Rule createClassRuleForPrototype(String className, PrototypeRule props) {
		SimpleSelector selector = getSimpleSelector(null, className);
		if (rulesBySelector.get(selector) != null)
			throw new RuntimeException("rule already exists for ." + className);
		Rule rule = new Rule(selector, props);
		content.add(rule);
		rulesBySelector.put(selector, rule);
		props.properties = null;
		lockedRulesByProperties.put(rule.properties, rule);
		return rule;
	}

	public Iterator content() {
		return content.iterator();
	}

	public Hashtable getRules() {
		return rulesBySelector;
	}
	
	public void serialize(PrintWriter pout) {
		Iterator it = content();
		while (it.hasNext()) {
			Object cp = it.next();
			if (cp instanceof Rule) {
				((Rule) cp).serialize(pout);
			} else if (cp instanceof FontFace) {
				((FontFace) cp).serialize(pout);
			}
		}
	}

	public void addDirectStyles( Reader reader ) throws IOException {
		SimpleStylesheetParser parser = new SimpleStylesheetParser();
		parser.readRules(reader);
		initWithParser(parser);				
	}
}
