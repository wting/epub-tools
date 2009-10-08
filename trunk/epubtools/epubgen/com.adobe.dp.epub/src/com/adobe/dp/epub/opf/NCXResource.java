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

package com.adobe.dp.epub.opf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

public class NCXResource extends Resource {

	Publication owner;

	TOCEntry rootTOCEntry = new TOCEntry("", null);

	int entryCount;

	Vector pages = new Vector();

	private static final String ncxns = "http://www.daisy.org/z3986/2005/ncx/";

	static class Page {
		String name;

		XRef xref;

		Page(String name, XRef xref) {
			this.name = name;
			this.xref = xref;
		}
	}

	NCXResource(Publication owner, String name) {
		super(name, "application/x-dtbncx+xml", null);
		this.owner = owner;
	}

	public TOCEntry getRootTOCEntry() {
		return rootTOCEntry;
	}

	public TOCEntry createTOCEntry(String title, XRef xref) {
		return new TOCEntry(title, xref);
	}

	private static int parseRoman(String roman) {
		int acc = 0;
		int lastDigit = 0;
		int len = roman.length();
		for (int i = 0; i < len; i++) {
			char c = roman.charAt(i);
			int digit = 0;
			switch (c) {
			case 'i':
				digit = 1;
				break;
			case 'v':
				digit = 5;
				break;
			case 'x':
				digit = 10;
				break;
			case 'l':
				digit = 50;
				break;
			case 'c':
				digit = 100;
				break;
			case 'd':
				digit = 500;
				break;
			case 'm':
				digit = 1000;
				break;
			default:
				return 0;
			}
			if (lastDigit >= digit) {
				acc += lastDigit;
			} else {
				acc -= lastDigit;
			}
			lastDigit = digit;
		}
		acc += lastDigit;
		return acc;
	}

	private static String printRoman(int n) {
		switch (n) {
		case 1:
			return "i";
		case 2:
			return "ii";
		case 3:
			return "iii";
		case 4:
			return "iv";
		case 5:
			return "v";
		case 9:
			return "ix";
		case 10:
			return "x";
		case 40:
			return "xl";
		case 50:
			return "l";
		case 90:
			return "xc";
		case 100:
			return "c";
		case 400:
			return "cd";
		case 500:
			return "d";
		case 900:
			return "cm";
		case 1000:
			return "m";
		}
		if (5 < n && n <= 8)
			return "v" + printRoman(n - 5);
		if (10 < n && n <= 20)
			return "x" + printRoman(n - 10);
		if (20 < n && n <= 30)
			return "xx" + printRoman(n - 20);
		if (30 < n && n <= 39)
			return "xxx" + printRoman(n - 30);
		if (40 < n && n <= 49)
			return "xl" + printRoman(n - 40);
		if (50 < n && n <= 89)
			return "l" + printRoman(n - 50);
		if (90 < n && n <= 99)
			return "xc" + printRoman(n - 90);
		if (100 < n && n <= 200)
			return "c" + printRoman(n - 100);
		if (200 < n && n <= 300)
			return "cc" + printRoman(n - 200);
		if (300 < n && n < 400)
			return "ccc" + printRoman(n - 300);
		if (400 < n && n < 500)
			return "cd" + printRoman(n - 400);
		if (500 < n && n < 900)
			return "d" + printRoman(n - 500);
		if (900 < n && n < 1000)
			return "cm" + printRoman(n - 900);
		if (1000 < n && n <= 2000)
			return "m" + printRoman(n - 1000);
		if (2000 < n && n <= 3000)
			return "mm" + printRoman(n - 2000);
		if (3000 < n && n < 4000)
			return "mmm" + printRoman(n - 3000);
		return null;
	}

	private static String increment(String pageName) {
		int len = pageName.length();
		int numberStart = len - 1;
		char c;
		do {
			c = pageName.charAt(numberStart);
			if ('0' > c || c > '9')
				break;
			numberStart--;
		} while (numberStart >= 0);
		numberStart++;
		if (numberStart != len) {
			// ends with number
			int n = Integer.parseInt(pageName.substring(numberStart));
			return pageName.substring(0, numberStart) + (n + 1);
		}
		// hmm, see if it is lowercase roman numeral
		int n = parseRoman(pageName);
		if (n > 0)
			return printRoman(n + 1);
		return null; // don't know what it is
	}

	public void addPage(String name, XRef location) {

		if (name == null || name.length() == 0) {
			if (pages.size() > 0) {
				Page lastPage = (Page) pages.lastElement();
				String lastName = lastPage.name;
				name = increment(lastName);
			}
			if (name == null || name.length() == 0)
				name = Integer.toString(pages.size() + 1);
		} else if (name.equals("."))
			name = "";

		location.addUsage(XRef.USAGE_PAGE);
		pages.add(new Page(name, location));
	}

	public void serialize(OutputStream out) throws IOException {
		XMLSerializer ser = new XMLSerializer(out);
		ser.startDocument("1.0", "UTF-8");
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "2005-1");
		String lang = owner.getDCMetadata("language");
		if (lang != null)
			attrs.put(null, "xml:lang", lang);
		ser.startElement(ncxns, "ncx", attrs, true);
		ser.newLine();
		ser.startElement(ncxns, "head", null, false);
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "name", "dtb:uid");
		String uid = owner.getDCMetadata("identifier");
		if (uid == null)
			uid = "";
		attrs.put(null, "content", uid);
		ser.startElement(ncxns, "meta", attrs, false);
		ser.endElement(ncxns, "meta");
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "name", "dtb:depth");
		int depth = calculateDepth(rootTOCEntry);
		attrs.put(null, "content", Integer.toString(depth));
		ser.startElement(ncxns, "meta", attrs, false);
		ser.endElement(ncxns, "meta");
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "name", "dtb:totalPageNumber");
		attrs.put(null, "content", Integer.toString(pages.size()));
		ser.startElement(ncxns, "meta", attrs, false);
		ser.endElement(ncxns, "meta");
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "name", "dtb:maxPageNumber");
		attrs.put(null, "content", Integer.toString(pages.size()));
		ser.startElement(ncxns, "meta", attrs, false);
		ser.endElement(ncxns, "meta");
		ser.newLine();
		ser.endElement(ncxns, "head");
		ser.newLine();
		ser.startElement(ncxns, "docTitle", null, false);
		ser.startElement(ncxns, "text", null, false);
		String title = owner.getDCMetadata("title");
		if (title == null)
			title = "";
		if (owner.isTranslit())
			title = Translit.translit(title);
		char[] arr = title.toCharArray();
		ser.text(arr, 0, arr.length);
		ser.endElement(ncxns, "text");
		ser.endElement(ncxns, "docTitle");
		ser.newLine();
		ser.startElement(ncxns, "navMap", null, false);
		ser.newLine();
		entryCount = 1;
		serializeChildEntries(rootTOCEntry, ser);
		ser.endElement(ncxns, "navMap");
		ser.newLine();
		if (pages.size() > 0) {
			ser.startElement(ncxns, "pageList", null, false);
			ser.startElement(ncxns, "navLabel", null, false);
			ser.startElement(ncxns, "text", null, false);
			ser.endElement(ncxns, "text");
			ser.endElement(ncxns, "navLabel");
			ser.newLine();
			Iterator pages = this.pages.iterator();
			int count = 1;
			while (pages.hasNext()) {
				Page page = (Page) pages.next();
				attrs = new SMapImpl();
				String countStr = Integer.toString(count++);
				attrs.put(null, "value", countStr);
				attrs.put(null, "type", "normal");
				attrs.put(null, "playOrder", Integer.toString(page.xref.getPlayOrder()));
				ser.startElement(ncxns, "pageTarget", attrs, false);
				ser.newLine();
				ser.startElement(ncxns, "navLabel", null, false);
				ser.startElement(ncxns, "text", null, false);
				char[] text = page.name.toCharArray();
				ser.text(text, 0, text.length);
				ser.endElement(ncxns, "text");
				ser.endElement(ncxns, "navLabel");
				ser.newLine();
				attrs = new SMapImpl();
				attrs.put(null, "src", page.xref.makeReference(this));
				ser.startElement(ncxns, "content", attrs, false);
				ser.endElement(ncxns, "content");
				ser.newLine();
				ser.endElement(ncxns, "pageTarget");
				ser.newLine();
			}
			ser.endElement(ncxns, "pageList");
		}
		ser.endElement(ncxns, "ncx");
		ser.newLine();
		ser.endDocument();
	}

	private static int calculateDepth(TOCEntry entry) {
		Iterator it = entry.content();
		int maxDepth = 0;
		while (it.hasNext()) {
			TOCEntry child = (TOCEntry) it.next();
			int depth = calculateDepth(child);
			if (depth > maxDepth)
				maxDepth = depth;
		}
		return maxDepth + 1;
	}

	private void serializeChildEntries(TOCEntry entry, XMLSerializer ser) {
		Iterator it = entry.content();
		while (it.hasNext()) {
			TOCEntry child = (TOCEntry) it.next();
			SMapImpl attrs = new SMapImpl();
			attrs.put(null, "playOrder", Integer.toString(child.getXRef().getPlayOrder()));
			attrs.put(null, "id", "id" + entryCount);
			entryCount++;
			ser.startElement(ncxns, "navPoint", attrs, false);
			ser.newLine();
			ser.startElement(ncxns, "navLabel", null, false);
			ser.newLine();
			ser.startElement(ncxns, "text", null, false);
			String title = child.getTitle();
			if (title == null)
				title = "";
			if (owner.isTranslit())
				title = Translit.translit(title);
			char[] arr = title.toCharArray();
			ser.text(arr, 0, arr.length);
			ser.endElement(ncxns, "text");
			ser.newLine();
			ser.endElement(ncxns, "navLabel");
			ser.newLine();
			attrs = new SMapImpl();
			attrs.put(null, "src", child.getXRef().makeReference(this));
			ser.startElement(ncxns, "content", attrs, false);
			ser.endElement(ncxns, "content");
			ser.newLine();
			serializeChildEntries(child, ser);
			ser.endElement(ncxns, "navPoint");
			ser.newLine();
		}
	}

	void serializePageMap(PageMapResource pageMap, OutputStream out) throws IOException {
		final String pagemapns = "http://www.idpf.org/2007/opf";
		XMLSerializer ser = new XMLSerializer(out);
		ser.startDocument("1.0", "UTF-8");
		ser.startElement(pagemapns, "page-map", null, true);
		ser.newLine();
		Iterator pages = this.pages.iterator();
		while (pages.hasNext()) {
			Page page = (Page) pages.next();
			SMapImpl attrs = new SMapImpl();
			attrs.put(null, "name", page.name);
			attrs.put(null, "href", page.xref.makeReference(pageMap));
			ser.startElement(pagemapns, "page", attrs, false);
			ser.endElement(pagemapns, "page");
			ser.newLine();
		}
		ser.endElement(pagemapns, "page-map");
		ser.endDocument();
	}

	public void prepareTOC() {
		/*
		 * Adobe renderer has a bug that if a page map is used, each chapter
		 * must start on page boundary. This code works around for this bug by
		 * moving the first page break in the chapter to the beginning of the
		 * chapter. Also, mark all xrefs in page map as requiring playOrder
		 */
		Iterator pages = this.pages.iterator();
		OPSResource lastResource = null;
		boolean report = false;
		while (pages.hasNext()) {
			Page page = (Page) pages.next();
			OPSResource resource = page.xref.getTargetResource();
			if (resource == lastResource)
				continue;
			// chapter change: move back to the chapter boundary
			lastResource = resource;
			if (page.xref.getTargetId() != null) {
				if (report) {
					Element p = resource.getDocument().getBody();
					Element t = page.xref.getTagetElement();
					while (p != t) {
						Iterator it = p.content();
						if (!it.hasNext())
							break;
						Object f = it.next();
						if (f instanceof Element)
							p = (Element) f;
						else
							break;
					}
					if (p != t)
						System.out.println("chapter break is moved");
					else
						System.out.println("chapter break is adjusted");
				}
				page.xref = resource.getDocument().getRootXRef();
			} else if (report) {
				System.out.println("chapter break is good as is");
			}
			page.xref.requestPlayOrder();
		}

		/*
		 * Mark all xrefs in TOC as requiring playOrder
		 */
		rootTOCEntry.requestPlayOrder();
	}
}
