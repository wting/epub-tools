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

import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

public class NCXResource extends Resource {

	Publication owner;

	TOCEntry rootTOCEntry = new TOCEntry("", null);

	int entryCount;

	private static final String ncxns = "http://www.daisy.org/z3986/2005/ncx/";

	public NCXResource(Publication owner, String name) {
		super(name, "application/x-dtbncx+xml", null);
		this.owner = owner;
	}

	public TOCEntry getRootTOCEntry() {
		return rootTOCEntry;
	}

	public TOCEntry createTOCEntry(String title, XRef xref) {
		return new TOCEntry(title, xref);
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
		attrs.put(null, "content", "0");
		ser.startElement(ncxns, "meta", attrs, false);
		ser.endElement(ncxns, "meta");
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "name", "dtb:maxPageNumber");
		attrs.put(null, "content", "0");
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
			attrs.put(null, "playOrder", Integer.toString(entryCount));
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
}
