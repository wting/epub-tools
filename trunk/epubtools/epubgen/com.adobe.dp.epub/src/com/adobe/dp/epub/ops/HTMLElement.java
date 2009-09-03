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

package com.adobe.dp.epub.ops;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.Selector;
import com.adobe.dp.epub.style.SimpleStylesheetParser;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.epub.util.TOCLevel;

public class HTMLElement extends Element {

	static HashSet sectionElements;

	static HashSet nonbreakable;

	static Hashtable peelingBonus;

	static Stylesheet builtInStylesheet;

	static {
		HashSet be = new HashSet();
		be.add("div");
		be.add("body");
		be.add("table");
		be.add("tr");
		be.add("ul");
		be.add("ol");
		be.add("dl");
		sectionElements = be;
		HashSet nb = new HashSet();
		nb.add("table");
		nb.add("ol");
		nb.add("ul");
		nb.add("dl");
		nonbreakable = nb;
		Hashtable pb = new Hashtable();
		pb.put("h1", new Integer(1000));
		pb.put("h2", new Integer(1000));
		pb.put("h3", new Integer(700));
		pb.put("h4", new Integer(500));
		pb.put("h5", new Integer(350));
		pb.put("h6", new Integer(200));
		pb.put("div", new Integer(200));
		pb.put("p", new Integer(50));
		pb.put("dl", new Integer(0));
		pb.put("ul", new Integer(0));
		pb.put("ol", new Integer(0));
		pb.put("p", new Integer(50));
		pb.put("blockquote", new Integer(0));
		pb.put("pre", new Integer(0));
		peelingBonus = pb;
		InputStream in = HTMLElement.class.getResourceAsStream("XHTMLStyles.css");
		if (in != null) {
			try {
				Reader reader = new InputStreamReader(in, "UTF-8");
				builtInStylesheet = new Stylesheet(null, reader);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	HTMLElement(OPSDocument document, String name) {
		super(document, name);
	}

	public String getNamespaceURI() {
		return OPSDocument.xhtmlns;
	}

	Element cloneElementShallow(OPSDocument newDoc) {
		HTMLElement e = new HTMLElement(newDoc, getElementName());
		e.className = className;
		return e;
	}

	protected Object getBuiltInProperty(String propName) {
		if (builtInStylesheet == null)
			return null;
		Selector selector = builtInStylesheet.getSimpleSelector(elementName, null);
		return getValue(builtInStylesheet, selector, propName);
	}

	boolean isSection() {
		return sectionElements.contains(elementName);
	}

	int getPeelingBonus() {
		Integer bonus = (Integer) peelingBonus.get(elementName);
		if (bonus == null)
			return -1;
		return bonus.intValue();
	}

	public void generateTOCFromHeadings(Stack headings, int depth) {
		if (elementName.startsWith("h") && elementName.length() == 2 && Character.isDigit(elementName.charAt(1))) {
			int headingLevel = elementName.charAt(1) - '0';
			TOCLevel level;
			while (true) {
				level = (TOCLevel) headings.pop();
				if (headings.isEmpty() || level.getHeadingLevel() < headingLevel) {
					headings.push(level);
					break;
				}
			}
			if (headings.size() < depth + 1) {
				String title = getText();
				TOCEntry entry = new TOCEntry(title, getSelfRef());
				level.getTOCEntry().add(entry);
				TOCLevel myLevel = new TOCLevel(headingLevel, entry);
				headings.push(myLevel);
			}
		}
		super.generateTOCFromHeadings(headings, depth);
	}

	boolean canPeelChild() {
		return !nonbreakable.contains(elementName);
	}
}
