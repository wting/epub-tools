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

package com.adobe.dp.office.conv;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import com.adobe.dp.epub.conv.Version;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.otf.FontEmbeddingReport;
import com.adobe.dp.epub.style.CSSLength;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.Selector;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.office.word.BodyElement;
import com.adobe.dp.office.word.MetadataItem;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.WordDocument;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.xml.util.StringUtil;

public class DOCXConverter {

	WordDocument doc;

	Publication epub;

	NCXResource toc;

	StyleResource styles;

	StyleResource global;

	StyleConverter styleConverter;

	// maps Footnote IDs to Footnote XRef
	Hashtable footnoteMap = new Hashtable();

	Hashtable convResources = new Hashtable();

	ContainerSource wordResources;

	FontLocator fontLocator;

	double defaultFontSize;

	PrintWriter log = new PrintWriter(new OutputStreamWriter(System.out));
	
	public DOCXConverter(WordDocument doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;

		global = epub.createStyleResource("OPS/global.css");
		Stylesheet globalStylesheet = global.getStylesheet();

		styles = epub.createStyleResource("OPS/style.css");
		Stylesheet stylesheet = styles.getStylesheet();
		styleConverter = new StyleConverter(stylesheet, false);
		toc = epub.getTOC();

		// default font size - have to happen early
		RunProperties rp = doc.getDocumentDefaultRunStyle().getRunProperties();
		if (rp != null) {
			Object sz = rp.get("sz");
			if (sz instanceof Number)
				defaultFontSize = ((Number) sz).doubleValue();
			Rule bodyRule = globalStylesheet.getRuleForSelector(stylesheet.getSimpleSelector("body", null));
			styleConverter.addDirectProperties("body", bodyRule, rp, 1);
		}
		if (defaultFontSize < 1)
			defaultFontSize = 20;

		Rule bodyEmbedRule = globalStylesheet.getRuleForSelector(stylesheet.getSimpleSelector("body", "embed"));
		bodyEmbedRule.set("font-size", new CSSLength(defaultFontSize / 2, "px"));

		styleConverter.setDefaultFontSize(defaultFontSize);
		styleConverter.setDocumentDefaultParagraphStyle(doc.getDocumentDefaultParagraphStyle());

		// default table styles
		Rule tableRule = globalStylesheet.getRuleForSelector(stylesheet.getSimpleSelector("table", null));
		tableRule.set("border-collapse", "collapse");
		tableRule.set("border-spacing", "0px");

		// default paragraph styles
		// unlike XHTML, Word's default spacing/margings are zero
		Rule pRule = globalStylesheet.getRuleForSelector(stylesheet.getSimpleSelector("p", null));
		pRule.set("margin-top", "0px");
		pRule.set("margin-bottom", "0px");		
		Rule ulRule = globalStylesheet.getRuleForSelector(stylesheet.getSimpleSelector("ul", null));
		ulRule.set("margin-left", "0px");
	}

	public void setFontLocator(FontLocator fontLocator) {
		this.fontLocator = fontLocator;
	}

	public void setLog(PrintWriter log) {
		this.log = log;
	}
	
	public void convert() {

		OPSResource footnotes = null;
		if (doc.getFootnotes() != null) {
			// process footnotes first to build footnote map
			BodyElement fbody = doc.getFootnotes();
			footnotes = epub.createOPSResource("OPS/footnotes.xhtml");
			WordMLConverter footnoteConv = new WordMLConverter(doc, epub, styleConverter, log);
			footnoteConv.setFootnoteMap(footnoteMap);
			footnoteConv.setWordResources(wordResources);
			footnoteConv.convert(fbody, footnotes, false);
			if (footnoteMap.size() > 0) {
				Stylesheet ss = styles.getStylesheet();
				Selector selector = ss.getSimpleSelector(null, "footnote-ref");
				Rule rule = ss.getRuleForSelector(selector);
				rule.set("font-size", "0.7em");
				rule.set("vertical-align", "super");
				rule.set("line-height", "0.2");
				selector = ss.getSimpleSelector(null, "footnote-title");
				rule = ss.getRuleForSelector(selector);
				rule.set("margin", "0px");
				rule.set("padding", "1em 0px 0.5em 2em");
			} else {
				epub.removeResource(footnotes);
			}
		}

		BodyElement body = doc.getBody();
		WordMLConverter bodyConv = new WordMLConverter(doc, epub, styleConverter, log);
		bodyConv.setFootnoteMap(footnoteMap);
		bodyConv.setWordResources(wordResources);
		bodyConv.findLists(body);
		OPSResource ops = epub.createOPSResource("OPS/document.xhtml");
		bodyConv.convert(body, ops, true);

		if (footnotes != null)
			epub.addToSpine(footnotes);

		if (bodyConv.includeWordMetadata) {
			// add EPUB metadata from Word metadata, do it in the end, so that
			// metadata from commands comes first
			Iterator metadata = doc.metadata();
			while (metadata.hasNext()) {
				MetadataItem item = (MetadataItem) metadata.next();
				epub.addMetadata(item.getNS(), item.getName(), item.getValue());
				if (item.getNS().equals("http://purl.org/dc/terms/") && item.getName().equals("modified")) {
					epub.addDCMetadata("date", item.getValue());
				}
			}
		}

		epub.addMetadata(null, "DOCX2EPUB.version", Version.VERSION);
		epub.addMetadata(null, "DOCX2EPUB.conversionDate", StringUtil.dateToW3CDTF(new Date()));
		
		epub.generateTOCFromHeadings(5);
		epub.splitLargeChapters();
	}

	public FontEmbeddingReport embedFonts() {
		if (fontLocator != null)
			return epub.addFonts(global, fontLocator);
		else
			return epub.addFonts(global); // use system fonts
	}
	
	public void setWordResources(ContainerSource source) {
		wordResources = source;
	}

}
