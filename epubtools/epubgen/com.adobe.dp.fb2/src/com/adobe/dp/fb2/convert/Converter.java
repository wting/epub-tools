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

package com.adobe.dp.fb2.convert;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.io.ContainerWriter;
import com.adobe.dp.epub.io.FolderContainerWriter;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.opf.BitmapImageResource;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.HTMLElement;
import com.adobe.dp.epub.ops.HyperlinkElement;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.SVGElement;
import com.adobe.dp.epub.ops.SVGImageElement;
import com.adobe.dp.epub.ops.TableCellElement;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.epub.style.BaseRule;
import com.adobe.dp.epub.style.InlineStyleRule;
import com.adobe.dp.epub.style.QuotedString;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.Selector;
import com.adobe.dp.epub.style.SimpleSelector;
import com.adobe.dp.epub.style.SimpleStylesheetParser;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.epub.style.ValueList;
import com.adobe.dp.epub.util.ImageDimensions;
import com.adobe.dp.fb2.FB2AuthorInfo;
import com.adobe.dp.fb2.FB2Binary;
import com.adobe.dp.fb2.FB2Date;
import com.adobe.dp.fb2.FB2DateInfo;
import com.adobe.dp.fb2.FB2Document;
import com.adobe.dp.fb2.FB2DocumentInfo;
import com.adobe.dp.fb2.FB2Element;
import com.adobe.dp.fb2.FB2EmptyLine;
import com.adobe.dp.fb2.FB2FormatException;
import com.adobe.dp.fb2.FB2GenreInfo;
import com.adobe.dp.fb2.FB2Hyperlink;
import com.adobe.dp.fb2.FB2Image;
import com.adobe.dp.fb2.FB2Line;
import com.adobe.dp.fb2.FB2OtherElement;
import com.adobe.dp.fb2.FB2Paragraph;
import com.adobe.dp.fb2.FB2PublishInfo;
import com.adobe.dp.fb2.FB2Section;
import com.adobe.dp.fb2.FB2SequenceInfo;
import com.adobe.dp.fb2.FB2StyledText;
import com.adobe.dp.fb2.FB2Subtitle;
import com.adobe.dp.fb2.FB2TableCell;
import com.adobe.dp.fb2.FB2Text;
import com.adobe.dp.fb2.FB2TextAuthor;
import com.adobe.dp.fb2.FB2Title;
import com.adobe.dp.fb2.FB2TitleInfo;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;

public class Converter {

	final static private int RESOURCE_THRESHOLD_MAX = 45000;

	final static private int RESOURCE_THRESHOLD_MIN = 10000;

	private static QuotedString defaultSansSerifFont = new QuotedString("Arial");

	private static QuotedString defaultSerifFont = new QuotedString(
			"Times New Roman");

	private static QuotedString defaultMonospaceFont = new QuotedString(
			"Courier New");

	FB2Document doc;

	Publication epub;

	Stylesheet stylesheet;

	StyleResource styles;

	NCXResource toc;

	int nameCount = 0;

	Hashtable idMap = new Hashtable();

	Hashtable docRules = new Hashtable();

	FB2Document templateDoc;
	
	Hashtable templateRules;

	FontLocator fontLocator;

	static Hashtable builtinRules;

	static FontLocator builtInFontLocator = new BuiltInFontLocator();

	static float[] titleFontSizes = { 2.2f, 1.8f, 1.5f, 1.3f, 1.2f, 1.1f, 1.0f };

	static {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					Converter.class.getResourceAsStream("stylesheet.css"),
					"UTF-8"));
			SimpleStylesheetParser parser = new SimpleStylesheetParser();
			parser.readRules(reader);
			reader.close();
			builtinRules = parser.getRules();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class LinkRecord {
		Vector sources = new Vector();

		Element target;
	}

	private LinkRecord getLinkRecord(String id) {
		LinkRecord record = (LinkRecord) idMap.get(id);
		if (record == null) {
			record = new LinkRecord();
			idMap.put(id, record);
		}
		return record;
	}

	private OPSResource newCoverPageResource() {
		OPSResource res = epub.createOPSResource("OPS/cover.xhtml");
		res.getDocument().addStyleResource(styles);
		epub.addToSpine(res);
		return res;
	}

	private OPSResource newOPSResource() {
		String name = "OPS/ch" + (++nameCount) + ".xhtml";
		OPSResource res = epub.createOPSResource(name);
		res.getDocument().addStyleResource(styles);
		epub.addToSpine(res);
		return res;
	}

	private BitmapImageResource getBitmapImageResource(String name) {
		FB2Binary bin = doc.getBinaryResource(name);
		if (bin == null)
			return null;
		String path = "OPS/images/" + name;
		BitmapImageResource resource = (BitmapImageResource) epub
				.getResourceByName(path);
		if (resource == null) {
			BufferedDataSource data = new BufferedDataSource();
			try {
				data.getOutputStream().write(bin.getData());
			} catch (IOException e) {
				throw new Error("unexpected exception: " + e);
			}
			resource = epub.createBitmapImageResource(path, bin.getMediaType(),
					data);
		}
		return resource;
	}

	private String trim(String s) {
		StringBuffer sb = new StringBuffer();
		int len = s.length();
		int i = 0;
		while (i < len) {
			char c = s.charAt(i);
			if ((c & 0xFFFF) > ' ')
				break;
			i++;
		}
		boolean hadSpace = false;
		while (i < len) {
			char c = s.charAt(i);
			if ((c & 0xFFFF) > ' ') {
				if (hadSpace) {
					sb.append(' ');
					hadSpace = false;
				}
				sb.append(c);
			} else
				hadSpace = true;
			i++;
		}
		return sb.toString();
	}

	private String getStringValue(Object val) {
		if (val instanceof QuotedString) {
			return ((QuotedString) val).getText();
		}
		return val.toString();
	}

	private InlineStyleRule processInlineStyle(String style) {
		InlineStyleRule rule = new InlineStyleRule();
		SimpleStylesheetParser.readProperties(style, rule);
		return rule;
	}

	private boolean isBuiltIn(String name) {
		return name.equals("serif") || name.equals("sans-serif")
				|| name.equals("monospace");
	}

	private void adjustFontList(BaseRule rule) {
		ValueList fonts = (ValueList) rule.get("font-family");
		if (fonts == null)
			return;
		Iterator it = fonts.values();
		while (it.hasNext()) {
			String family = getStringValue(it.next());
			if (isBuiltIn(family))
				continue;
			FontProperties fp = new FontProperties(family,
					FontProperties.WEIGHT_NORMAL, FontProperties.STYLE_REGULAR);
			if (fontLocator.hasFont(fp))
				return; // found at least one font
		}
		ValueList list = new ValueList();
		it = fonts.values();
		boolean inserted = false;
		while (it.hasNext()) {
			Object fn = it.next();
			String family = getStringValue(fn);
			if (!inserted) {
				if (family.equals("sans-serif")) {
					list.add(defaultSansSerifFont);
					inserted = true;
				} else if (family.equals("serif")) {
					list.add(defaultSerifFont);
					inserted = true;
				} else if (family.equals("monospace")) {
					list.add(defaultMonospaceFont);
					inserted = true;
				}
			}
			list.add(fn);
		}
		if (!inserted)
			list.add(defaultSerifFont);
		rule.set("font-family", list);
	}

	void mergeRuleStyle(Rule rule, Hashtable src, SimpleSelector ss) {
		Rule docRule = (Rule) src.get(ss);
		if (docRule != null) {
			Iterator props = docRule.properties();
			while (props.hasNext()) {
				String prop = (String) props.next();
				rule.set(prop, docRule.get(prop));
			}
		}
	}

	void mergeRuleStyle(Rule rule, Hashtable src, String fbName, String fbClass) {
		if (src == null)
			return;
		SimpleSelector ss;
		if (fbName != null) {
			ss = new SimpleSelector(fbName, null);
			mergeRuleStyle(rule, src, ss);
		}
		ss = new SimpleSelector(null, fbClass);
		mergeRuleStyle(rule, src, ss);
	}

	private void ensureRule(Selector selector, String fbName, String fbClass) {
		if (stylesheet.findRuleForSelector(selector) == null) {
			Rule rule = stylesheet.getRuleForSelector(selector);
			mergeRuleStyle(rule, builtinRules, fbName, fbClass);
			mergeRuleStyle(rule, docRules, fbName, fbClass);
			mergeRuleStyle(rule, templateRules, fbName, fbClass);
			adjustFontList(rule);
		}
	}

	private void convertElement(OPSDocument ops, Element parent, Object fb,
			TOCEntry entry, int level, boolean insideTitle) {
		if (fb instanceof FB2Element) {
			FB2Element fbe = (FB2Element) fb;
			FB2Title title = null;
			if (fbe instanceof FB2Section) {
				title = ((FB2Section) fbe).getTitle();
			}
			String name;
			String className;
			String fbName = fbe.getName();
			String fbClass = fbName;
			if (fbe instanceof FB2Section) {
				name = "div";
				level++;
				className = fbName;
			} else if (fbe instanceof FB2Title) {
				name = "div";
				className = "title" + (level > 6 ? 6 : level);
				fbClass = className;
				insideTitle = true;
			} else if (fbe instanceof FB2Subtitle) {
				name = "p";
				className = "subtitle";
			} else if (fbe instanceof FB2TextAuthor) {
				name = "p";
				className = "text-author";
			} else if (fbe instanceof FB2Date) {
				name = "p";
				className = "date";
			} else if (fbe instanceof FB2Paragraph) {
				name = "p";
				className = (insideTitle ? "title-p" : "p");
				fbClass = className;
			} else if (fbe instanceof FB2Line) {
				name = "p";
				className = "v";
			} else if (fbe instanceof FB2EmptyLine) {
				name = "p";
				className = "empty-line";
			} else if (fbe instanceof FB2Hyperlink) {
				name = "a";
				className = null;
			} else if (fbe instanceof FB2Image) {
				name = "img";
				className = "img";
			} else if (fbe instanceof FB2StyledText) {
				className = ((FB2StyledText) fbe).getStyleName();
				fbClass = className;
				name = "span";
			} else if (fbe instanceof FB2Text) {
				className = null;
				if (fbName.equals("emphasis")) {
					name = "em";
				} else if (fbName.equals("strong")) {
					name = "strong";
				} else if (fbName.equals("sup")) {
					name = "sup";
				} else if (fbName.equals("sub")) {
					name = "sub";
				} else if (fbName.equals("code")) {
					name = "code";
				} else {
					throw new RuntimeException("unexpected element: " + fbName);
				}
			} else if (fbe instanceof FB2OtherElement) {
				className = null;
				name = fbe.getName();
			} else if (fbe instanceof FB2TableCell) {
				className = null;
				name = fbe.getName();
			} else {
				System.err.println("Unknown fb2 element: " + fbe.getName());
				name = null;
				className = null;
			}
			Element self;
			if (name == null) {
				self = parent;
			} else {
				if (name.equals("img")) {
					ImageElement img = null;
					FB2Image image = (FB2Image) fbe;
					String resourceName = image.getResourceName();
					String alt = image.getAlt();
					String caption = image.getTitle();
					if (resourceName != null) {
						BitmapImageResource resource = getBitmapImageResource(resourceName);
						if (resource != null) {
							img = ops.createImageElement(name);
							img.setImageResource(resource);
							if (alt != null)
								img.setAltText(alt);
							SimpleSelector imageContent = stylesheet
									.getSimpleSelector("img", null);
							ensureRule(imageContent, null, "image-content");
						}
					}
					if (img == null)
						return;
					name = "div";
					self = ops.createElement(name);
					self.add(img);
					if (caption != null && caption.length() > 0) {
						HTMLElement captionElement = ops.createElement("p");
						captionElement.setClassName("image-title");
						self.add(captionElement);
						captionElement.add(caption);
						SimpleSelector imageTitle = stylesheet
								.getSimpleSelector(null, "image-title");
						ensureRule(imageTitle, null, "image-title");
					}
				} else if (name.equals("a")) {
					HyperlinkElement a = ops.createHyperlinkElement("a");
					String link = ((FB2Hyperlink) fbe).getLinkedId();
					if (link != null) {
						LinkRecord record = getLinkRecord(link);
						record.sources.add(a);
					}
					self = a;
				} else if (name.equals("td") || name.equals("th")) {
					FB2TableCell fbt = (FB2TableCell) fbe;
					TableCellElement td = ops.createTableCellElement(name, fbt
							.getAlign(), fbt.getColSpan(), fbt.getRowSpan());
					self = td;
				} else {
					self = ops.createElement(name);
				}
				String style = fbe.getStyle();
				if (style != null) {
					InlineStyleRule inlineRule = processInlineStyle(style);
					if (inlineRule != null) {
						adjustFontList(inlineRule);
						self.setStyle(inlineRule);
					}
				}
				if (className != null)
					self.setClassName(className);
				parent.add(self);
				if (fbe.getId() != null) {
					LinkRecord record = getLinkRecord(fbe.getId());
					record.target = self;
				}
				if (title != null && entry != null) {
					TOCEntry subentry = toc.createTOCEntry(trim(title
							.contentAsString()), self.getSelfRef());
					entry.add(subentry);
					entry = subentry;
				}
				SimpleSelector selector = stylesheet.getSimpleSelector(name,
						className);
				ensureRule(selector, fbName, fbClass);
			}
			Object[] children = fbe.getChildren();
			for (int i = 0; i < children.length; i++)
				convertElement(ops, self, children[i], entry, level,
						insideTitle);
		} else {
			parent.add(fb);
		}
	}

	public void setTemplate(InputStream templateStream) throws IOException,
			FB2FormatException {
		BufferedInputStream in = new BufferedInputStream(templateStream);
		byte[] sniff = new byte[4];
		in.mark(4);
		in.read(sniff);
		in.reset();
		SimpleStylesheetParser parser = new SimpleStylesheetParser();
		if ((sniff[0] == 'P' && sniff[1] == 'K' && sniff[2] == 3 && sniff[3] == 4)
				|| sniff[0] == '<'
				|| ((sniff[0] == (byte) 0xef && sniff[1] == (byte) 0xbb
						&& sniff[2] == (byte) 0xbf && sniff[3] == '<'))) {
			// template is FB2 file itself
			templateDoc = new FB2Document(in);
			String[] stylesheets = templateDoc.getStylesheets();
			if (stylesheets != null && stylesheets.length > 0) {
				for (int i = 0; i < stylesheets.length; i++) {
					parser.readRules(stylesheets[i]);
				}
			}
		} else {
			templateDoc = null;
			parser.readRules(new BufferedReader(new InputStreamReader(in,
					"UTF-8")));
		}
		templateRules = parser.getRules();
	}

	public void setTemplateFile(String file) {
		try {
			setTemplate(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FB2FormatException e) {
			e.printStackTrace();
		}
	}

	private void convertToResource(OPSResource resource, FB2Section section,
			TOCEntry entry, int level, int index, int length) {
		OPSDocument ops = resource.getDocument();
		Element body = ops.getBody();
		Object[] children = section.getChildren();
		for (int i = 0; i < length; i++)
			convertElement(ops, body, children[index + i], entry, level, false);
	}

	private boolean isLargeSection(Object child) {
		return child instanceof FB2Section
				&& ((FB2Section) child).getUTF16Size() >= RESOURCE_THRESHOLD_MIN;
	}

	private void convertSection(FB2Section section, TOCEntry entry, int level) {
		int size = section.getUTF16Size();
		FB2Title title = section.getTitle();
		if (section.getName().equals("body")) {
			String sectionName = section.getSectionName();
			if (sectionName != null) {
				if (sectionName.equals("footnotes")
						|| sectionName.equals("notes")) {
					entry = null;
					level += 2;
				}
			}
		}
		OPSResource resource = null;
		if (title != null && level > 1) {
			resource = newOPSResource();
			if (entry != null) {
				TOCEntry subentry = toc.createTOCEntry(trim(title
						.contentAsString()), resource.getDocument()
						.getRootXRef());
				entry.add(subentry);
				entry = subentry;
			}
		}
		Object[] children = section.getChildren();
		if (size <= RESOURCE_THRESHOLD_MAX) {
			if (resource == null)
				resource = newOPSResource();
			convertToResource(resource, section, entry, level, 0,
					children.length);
			resource = null;
		} else {
			size = 0;
			int first = 0;
			int i = 0;
			while (i < children.length) {
				Object child = children[i];
				int childSize = FB2Element.getUTF16Size(child);
				int newSize = size + childSize;
				if (isLargeSection(child) || newSize > RESOURCE_THRESHOLD_MAX) {
					if (first < i) {
						if (resource == null)
							resource = newOPSResource();
						convertToResource(resource, section, entry, level,
								first, i - first);
						resource = null;
					}
					if (isLargeSection(child)) {
						convertSection((FB2Section) child, entry, level + 1);
						i++;
					}
					size = 0;
					first = i;
					continue;
				}
				size = newSize;
				i++;
			}
			if (first < i) {
				if (resource == null)
					resource = newOPSResource();
				convertToResource(resource, section, entry, level, first, i
						- first);
			}
		}
	}

	private boolean isUUID(String id) {
		return id.length() == 36 && id.charAt(8) == '-' && id.charAt(13) == '-'
				&& id.charAt(18) == '-' && id.charAt(23) == '-';
	}

	private void convert() {
		FB2TitleInfo bookInfo = doc.getTitleInfo();
		styles = epub.createStyleResource("OPS/style.css");
		stylesheet = styles.getStylesheet();
		if (bookInfo != null) {
			String title = bookInfo.getBookTitle();
			epub.addDCMetadata("title", title);
			FB2AuthorInfo[] authors = bookInfo.getAuthors();
			if (authors != null) {
				for (int i = 0; i < authors.length; i++) {
					epub.addDCMetadata("creator", authors[i].toString());
				}
			}
			FB2AuthorInfo[] translators = bookInfo.getTranslators();
			if (translators != null) {
				for (int i = 0; i < translators.length; i++) {
					epub.addMetadata(null, "FB2.book-info.translator",
							translators[i].toString());
				}
			}
			epub.addDCMetadata("language", bookInfo.getLanguage());
			FB2Section annot = bookInfo.getAnnotation();
			if (annot != null) {
				epub.addDCMetadata("description", annot.contentAsString());
			}
			FB2DateInfo date = bookInfo.getDate();
			if (date != null) {
				String mr = date.getMachineReadable();
				epub.addDCMetadata("date", (mr == null ? date
						.getHumanReadable() : mr));
			}
			FB2GenreInfo[] genres = bookInfo.getGenres();
			if (genres != null) {
				for (int i = 0; i < genres.length; i++)
					epub.addMetadata(null, "FB2.book-info.genre", genres[i]
							.toString());
			}
			FB2SequenceInfo[] sequences = bookInfo.getSequences();
			if (sequences != null) {
				for (int i = 0; i < sequences.length; i++)
					epub.addMetadata(null, "FB2.book-info.sequence",
							sequences[i].toString());
			}
			String coverpageImage = bookInfo.getCoverpageImage();
			if (coverpageImage != null) {
				FB2Binary binary = doc.getBinaryResource(coverpageImage);
				if (binary != null && binary.getData() != null) {
					int[] dim = ImageDimensions.getImageDimensions(binary
							.getData());
					if (dim != null) {
						OPSResource coverRes = newCoverPageResource();
						OPSDocument coverDoc = coverRes.getDocument();
						Element body = coverDoc.getBody();
						body.setClassName("cover");
						Rule coverBodyRule = stylesheet
								.getRuleForSelector(stylesheet
										.getSimpleSelector("body", "cover"));
						coverBodyRule.set("oeb-column-number", "1");
						coverBodyRule.set("margin", "0px");
						coverBodyRule.set("padding", "0px");
						SVGElement svg = coverDoc.createSVGElement("svg");
						svg.setAttribute("viewBox", "0 0 " + dim[0] + " "
								+ dim[1]);
						svg.setClassName("cover-svg");
						body.add(svg);
						Rule svgRule = stylesheet.getRuleForSelector(stylesheet
								.getSimpleSelector("svg", "cover-svg"));
						svgRule.set("width", "100%");
						svgRule.set("height", "100%");
						SVGImageElement image = coverDoc
								.createSVGImageElement("image");
						image.setAttribute("width", Integer.toString(dim[0]));
						image.setAttribute("height", Integer.toString(dim[1]));
						BitmapImageResource resource = getBitmapImageResource(coverpageImage);
						image.setImageResource(resource);
						svg.add(image);
					}
				}
			}
		}
		String[] stylesheets = doc.getStylesheets();
		if (stylesheets != null && stylesheets.length > 0) {
			SimpleStylesheetParser parser = new SimpleStylesheetParser();
			for (int i = 0; i < stylesheets.length; i++) {
				parser.readRules(stylesheets[i]);
			}
			docRules = parser.getRules();
		}
		fontLocator = new EmbeddedFontLocator(docRules, doc, templateRules,
				templateDoc, builtInFontLocator);
		FB2DocumentInfo docInfo = doc.getDocumentInfo();
		String ident = null;
		if (docInfo != null) {
			FB2AuthorInfo[] authors = docInfo.getAuthors();
			if (authors != null) {
				for (int i = 0; i < authors.length; i++) {
					epub.addMetadata(null, "FB2.document-info.author",
							authors[i].toString());
				}
			}
			epub.addMetadata(null, "FB2.document-info.program-used", docInfo
					.getProgramUsed());
			String[] urls = docInfo.getSrcUrls();
			if (urls != null) {
				for (int i = 0; i < urls.length; i++)
					epub
							.addMetadata(null, "FB2.document-info.src-url",
									urls[i]);
			}
			epub.addMetadata(null, "FB2.document-info.src-ocr", docInfo
					.getSrcOcr());
			FB2Section history = docInfo.getHistory();
			if (history != null)
				epub.addMetadata(null, "FB2.document-info.history", history
						.contentAsString());
			ident = docInfo.getId();
			epub.addMetadata(null, "FB2.document-info.id", ident);
			epub.addMetadata(null, "FB2.document-info.version", docInfo
					.getVersion());
			FB2DateInfo date = bookInfo.getDate();
			if (date != null) {
				String mr = date.getMachineReadable();
				epub.addMetadata(null, "FB2.document-info.date",
						(mr == null ? date.getHumanReadable() : mr));
			}

		}
		if (ident == null || !isUUID(ident)) {
			epub.generateRandomIdentifier();
			if (ident != null)
				epub.addDCMetadata("identifier", ident);
		} else {
			epub.addDCMetadata("identifier", "urn:uuid:" + ident.toLowerCase());
		}
		FB2PublishInfo publishInfo = doc.getPublishInfo();
		if (publishInfo != null) {
			epub.addDCMetadata("publisher", publishInfo.getPublisher());
			epub.addMetadata(null, "FB2.publish-info.book-name", publishInfo
					.getBookName());
			epub.addMetadata(null, "FB2.publish-info.city", publishInfo
					.getCity());
			epub.addMetadata(null, "FB2.publish-info.year", publishInfo
					.getYear());
			String isbn = publishInfo.getISBN();
			if (isbn != null)
				epub.addDCMetadata("identifier", "isbn:" + isbn.toUpperCase());
		}
		Rule bodyRule = stylesheet.getRuleForSelector(stylesheet
				.getSimpleSelector("body", null));
		mergeRuleStyle(bodyRule, builtinRules, "body", "body");
		mergeRuleStyle(bodyRule, docRules, "body", "body");
		mergeRuleStyle(bodyRule, templateRules, "body", "body");
		adjustFontList(bodyRule);
		long t0 = System.currentTimeMillis();
		FB2Section[] bodySections = doc.getBodySections();
		toc = epub.getTOC();
		TOCEntry entry = toc.getRootTOCEntry();
		for (int i = 0; i < bodySections.length; i++) {
			convertSection(bodySections[i], entry, 1);
		}
		long t1 = System.currentTimeMillis();
		Enumeration keys = idMap.keys();
		while (keys.hasMoreElements()) {
			String id = (String) keys.nextElement();
			LinkRecord record = (LinkRecord) idMap.get(id);
			if (record.target != null) {
				XRef ref = record.target.getSelfRef();
				Enumeration sources = record.sources.elements();
				while (sources.hasMoreElements()) {
					HyperlinkElement a = (HyperlinkElement) sources
							.nextElement();
					a.setXRef(ref);
				}
			}
		}
		long t2 = System.currentTimeMillis();
		epub.addFonts(styles, fontLocator);
		long t3 = System.currentTimeMillis();
		if (false) {
			System.out.println("\ttext converted in " + (t1 - t0) / 1000.0
					+ " seconds");
			System.out.println("\txrefs resolved in " + (t2 - t1) / 1000.0
					+ " seconds");
			System.out.println("\tfont embedded in " + (t3 - t2) / 1000.0
					+ " seconds");
		}
	}

	public void convert(FB2Document doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;
		convert();
	}

	private void invoke(File inFile, File outFile) {
		try {
			long t0 = System.currentTimeMillis();
			FB2Document doc = new FB2Document(new FileInputStream(inFile));
			long t1 = System.currentTimeMillis();
			if (false)
				System.out.println("\tparsed in " + (t1 - t0) / 1000.0
						+ " seconds");
			File epubFileOrFolder = outFile;
			ContainerWriter container;
			if (epubFileOrFolder.isDirectory())
				container = new FolderContainerWriter(epubFileOrFolder);
			else
				container = new OCFContainerWriter(new FileOutputStream(
						epubFileOrFolder));
			Publication epub = new Publication();
			epub.setTranslit(false);
			epub.useAdobeFontMangling();
			convert(doc, epub);
			long t2 = System.currentTimeMillis();
			epub.serialize(container);
			long t3 = System.currentTimeMillis();
			if (false) {
				System.out.println("\tserialized in " + (t3 - t2) / 1000.0
						+ " seconds");
				System.out.println("Converted in " + (t3 - t0) / 1000.0
						+ " seconds");
				System.out.println();
			}
		} catch (Exception e) {
			System.err.println("While converting " + inFile.getName());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage:");
			System.err
					.println("  java -jar fb2epub.jar [-style styleset.fb2] file.fb2 file.epub");
			System.err
					.println("  java -jar fb2epub.jar [-style styleset.fb2] -folders fb2-folder epub-folder");
			System.exit(2);
		}
		String inFile = null;
		String outFile = null;
		String styleFile = null;
		String inFolder = null;
		String outFolder = null;
		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-style")) {
				styleFile = args[++i];
			} else if (args[i].equals("-folders")) {
				inFolder = args[++i];
				outFolder = args[++i];
			} else if (inFile == null) {
				inFile = args[i];
			} else if (outFile == null) {
				outFile = args[i];
			} else {
				System.err.println("Too many arguments");
				System.exit(2);
			}
		if (inFile != null && (inFolder != null || outFile == null)) {
			System.err.println("Bad command line syntax");
			System.exit(2);
		}
		if (inFolder != null) {
			String[] list = (new File(inFolder)).list();
			for (int i = 0; i < list.length; i++) {
				if (list[i].endsWith(".fb2") || list[i].endsWith(".fb2.zip")) {
					String epubName = list[i].substring(0, list[i]
							.lastIndexOf(".fb2"))
							+ ".epub";
					Converter c = new Converter();
					if (styleFile != null)
						c.setTemplateFile(styleFile);
					c.invoke(new File(inFolder, list[i]), new File(outFolder,
							epubName));
				}
			}
		} else {
			Converter c = new Converter();
			if (styleFile != null)
				c.setTemplateFile(styleFile);
			c.invoke(new File(inFile), new File(outFile));
		}
	}
}
