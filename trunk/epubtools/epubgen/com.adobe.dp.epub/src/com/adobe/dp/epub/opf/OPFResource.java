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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

public class OPFResource extends Resource {

	private static final String opfns = "http://www.idpf.org/2007/opf";

	private static final String dcns = "http://purl.org/dc/elements/1.1/";

	private static final String dctermsns = "http://purl.org/dc/terms/";

	private static final String xsins = "http://www.w3.org/2001/XMLSchema-instance";

	Publication owner;

	OPFResource(Publication owner, String name) {
		super(name, "application/oebps-package+xml", null);
		this.owner = owner;
	}

	public void parse(InputStream in) throws IOException {
		throw new IOException("not yet implemented");
	}
	
	public void serialize(OutputStream out) throws IOException {
		XMLSerializer ser = new XMLSerializer(out);
		ser.startDocument("1.0", "UTF-8");
		SMapImpl nsmap = new SMapImpl();
		nsmap.put(null, "opf", opfns);
		nsmap.put(null, "dc", dcns);
		nsmap.put(null, "dcterms", dctermsns);
		nsmap.put(null, "xsi", xsins);
		ser.setPreferredPrefixMap(nsmap);
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "2.0");
		attrs.put(null, "unique-identifier", "bookid");
		ser.startElement(opfns, "package", attrs, true);
		ser.newLine();
		ser.startElement(opfns, "metadata", null, false);
		ser.newLine();
		Iterator it = owner.metadata.iterator();
		int identifierCount = 0;
		while (it.hasNext()) {
			Publication.SimpleMetadata item = (Publication.SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier")) {
				attrs = new SMapImpl();
				attrs.put(null, "id", (identifierCount == 0 ? "bookid" : "bookid" + identifierCount));
				identifierCount++;
			} else {
				attrs = null;
			}
			String value = item.value;
			if (owner.isTranslit())
				value = Translit.translit(value);
			if (item.ns == null) {
				attrs = new SMapImpl();
				attrs.put(null, "name", item.name);
				attrs.put(null, "content", value);
				ser.startElement(opfns, "meta", attrs, false);
				ser.endElement(opfns, "meta");
				ser.newLine();
			} else {
				ser.startElement(item.ns, item.name, attrs, false);
				char[] arr = value.toCharArray();
				ser.text(arr, 0, arr.length);
				ser.endElement(item.ns, item.name);
				ser.newLine();
			}
		}
		ser.endElement(opfns, "metadata");
		ser.newLine();
		ser.startElement(opfns, "manifest", null, false);
		ser.newLine();
		it = owner.resources();
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			if (r != this) {
				attrs = new SMapImpl();
				attrs.put(null, "id", owner.assignId(r));
				attrs.put(null, "href", makeReference(r, null));
				attrs.put(null, "media-type", r.mediaType);
				ser.startElement(opfns, "item", attrs, false);
				ser.endElement(opfns, "item");
				ser.newLine();
			}
		}
		ser.endElement(opfns, "manifest");
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "toc", owner.assignId(owner.toc));
		if( owner.pageMap != null )
			attrs.put(null, "page-map", owner.assignId(owner.pageMap));			
		ser.startElement(opfns, "spine", attrs, false);
		ser.newLine();
		it = owner.spine();
		while (it.hasNext()) {
			Resource r = (Resource) it.next();
			attrs = new SMapImpl();
			attrs.put(null, "idref", owner.assignId(r));
			ser.startElement(opfns, "itemref", attrs, false);
			ser.endElement(opfns, "itemref");
			ser.newLine();
		}
		ser.endElement(opfns, "spine");
		ser.newLine();
		ser.endElement(opfns, "package");
		ser.newLine();
		ser.endDocument();
	}
}
