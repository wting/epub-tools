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

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.StringDataSource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.HyperlinkElement;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.SVGElement;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.epub.style.CSSLength;
import com.adobe.dp.epub.style.PrototypeRule;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.SimpleSelector;
import com.adobe.dp.office.drawing.PictureData;
import com.adobe.dp.office.metafile.WMFParser;
import com.adobe.dp.office.vml.VMLGroupElement;
import com.adobe.dp.office.vml.VMLPathConverter;
import com.adobe.dp.office.word.BRElement;
import com.adobe.dp.office.word.BodyElement;
import com.adobe.dp.office.word.ContainerElement;
import com.adobe.dp.office.word.DrawingElement;
import com.adobe.dp.office.word.FootnoteElement;
import com.adobe.dp.office.word.FootnoteReferenceElement;
import com.adobe.dp.office.word.NumberingProperties;
import com.adobe.dp.office.word.ParagraphElement;
import com.adobe.dp.office.word.ParagraphProperties;
import com.adobe.dp.office.word.PictElement;
import com.adobe.dp.office.word.RunElement;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.Style;
import com.adobe.dp.office.word.TXBXContentElement;
import com.adobe.dp.office.word.TabElement;
import com.adobe.dp.office.word.TableCellElement;
import com.adobe.dp.office.word.TableElement;
import com.adobe.dp.office.word.TableProperties;
import com.adobe.dp.office.word.TableRowElement;
import com.adobe.dp.office.word.TextElement;
import com.adobe.dp.office.word.WordDocument;

class WordMLConverter {

	private StyleConverter styleConverter;

	private HashSet listElements = new HashSet();

	private Publication epub;

	private WordDocument doc;

	private boolean hadSpace = false;

	private Hashtable footnoteMap;

	private Hashtable convResources = new Hashtable();

	private ContainerSource wordResources;

	private static final String mediaFolder = "OPS/media/";

	WordMLConverter(WordDocument doc, Publication epub, StyleConverter styleConverter) {
		this.doc = doc;
		this.epub = epub;
		this.styleConverter = styleConverter;
	}

	WordMLConverter(WordMLConverter parent) {
		this.doc = parent.doc;
		this.epub = parent.epub;
		this.styleConverter = new StyleConverter(parent.styleConverter);
	}

	WordMLConverter(WordMLConverter parent, StyleConverter styleConverter) {
		this.doc = parent.doc;
		this.epub = parent.epub;
		this.styleConverter = styleConverter;
	}

	class WMFResourceWriter implements ResourceWriter {

		public StreamAndName createResource(String base, String suffix, boolean doNotCompress) throws IOException {
			String name = mediaFolder + "wmf-" + base + suffix;
			name = epub.makeUniqueResourceName(name);
			BufferedDataSource dataSource = new BufferedDataSource();
			epub.createBitmapImageResource(name, "image/png", dataSource);
			return new StreamAndName(name.substring(mediaFolder.length()), dataSource.getOutputStream());
		}

	}

	class ListControl {
		Stack nesting = new Stack();

		Object numId;

		Element process(OPSDocument chapter, Element currentParent, ParagraphElement wordElement) {
			ParagraphElement wp = (ParagraphElement) wordElement;
			ParagraphProperties pp = wp.getParagraphProperties();
			NumberingProperties np = pp.getNumberingProperties();
			Integer levelVal = (Integer) np.get("ilvl");
			Object numId = np.get("numId");
			if (numId == null || (this.numId != null && !this.numId.equals(numId))) {
				currentParent = finish();
				if (numId == null)
					return currentParent;
			}
			this.numId = numId;
			int level = (levelVal == null ? 0 : levelVal.intValue()) + 1;
			int currLevel = nesting.size();
			if (level > currLevel) {
				while (level > nesting.size()) {
					nesting.push(currentParent);
					if (nesting.size() > 1) {
						Object obj = currentParent.getLastChild();
						if (obj != null && obj instanceof Element && ((Element) obj).getElementName().equals("li")) {
							currentParent = (Element) obj;
						} else {
							Element e = chapter.createElement("li");
							e.setClassName("nested");
							currentParent.add(e);
							currentParent = e;
						}
					}
					Element list = chapter.createElement("ul");
					currentParent.add(list);
					currentParent = list;
				}
			} else if (level < currLevel) {
				currentParent = (Element) nesting.elementAt(level);
				nesting.setSize(level);
			}
			return currentParent;
		}

		Element finish() {
			Element init = (Element) nesting.get(0);
			nesting.clear();
			return init;
		}
	}

	void setFootnoteMap(Hashtable footnoteMap) {
		this.footnoteMap = footnoteMap;
	}

	Publication getPublication() {
		return epub;
	}

	private Resource getImageResource(PictureData data) {
		String src = data.getSrc();
		if (src == null)
			return null;
		String epubName = (String) convResources.get(src);
		if (epubName == null) {
			DataSource dataSource = wordResources.getDataSource(src);
			int index = src.lastIndexOf('/');
			String shortName = src.substring(index + 1);
			String mediaType = doc.getResourceMediaType(src);
			if (mediaType.equals("image/x-wmf")) {
				WMFResourceWriter dw = new WMFResourceWriter();
				GDISVGSurface svg = new GDISVGSurface(dw);
				try {
					WMFParser parser = new WMFParser(dataSource.getInputStream(), svg);
					parser.readAll();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				dataSource = new StringDataSource(svg.getSVG());
				mediaType = "image/svg+xml";
				shortName = shortName + ".svg";
			}
			epubName = mediaFolder + shortName;
			if (dataSource == null)
				return null;
			epub.createBitmapImageResource(epubName, mediaType, dataSource);
			convResources.put(src, epubName);
		}
		return epub.getResourceByName(epubName);
	}

	private boolean shouldMergeParagraphs(ParagraphProperties pp) {
		return pp != null
				&& (pp.getWithInheritance("shd") != null || pp.getWithInheritance("pBdr") != null || pp
						.getWithInheritance("keepLines") != null);
	}

	private void resetSpaceProcessing() {
		hadSpace = false;
	}

	private void treatAsSpace() {
		hadSpace = true;
	}

	private String processSpaces(String s) {
		int len = s.length();
		StringBuffer result = null;
		int last = 0;
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if (c == ' ') {
				if (hadSpace) {
					if (result == null)
						result = new StringBuffer();
					result.append(s.substring(last, i));
					result.append('\u00A0'); // nbsp
					last = i + 1;
				} else {
					hadSpace = true;
				}
			} else {
				hadSpace = false;
			}
		}
		if (result != null) {
			result.append(s.substring(last));
			return result.toString();
		}
		return s;
	}

	void setImageWidth(Element img, String baseClassName, double widthPt, float emScale) {
		String className = styleConverter.getUniqueClassName(baseClassName, false);
		img.setClassName(className);
		SimpleSelector selector = styleConverter.stylesheet.getSimpleSelector(null, className);
		Rule rule = styleConverter.stylesheet.getRuleForSelector(selector);
		if (styleConverter.usingPX()) {
			rule.set("width", new CSSLength(widthPt, "px"));
		} else {
			double defaultFontSize = styleConverter.defaultFontSize;
			double widthEM = widthPt / (emScale * defaultFontSize / 2);
			rule.set("width", new CSSLength(widthEM, "em"));
		}
		rule.set("max-width", "100%");
	}

	void appendConvertedElement(com.adobe.dp.office.word.Element we, Element parent, OPSDocument chapter, float emScale) {
		Element conv = null;
		boolean addToParent = true;
		boolean resetSpaceProcessing = false;
		if (we instanceof ParagraphElement) {
			ParagraphElement wp = (ParagraphElement) we;
			ParagraphProperties pp = wp.getParagraphProperties();
			SimpleSelector selector = styleConverter.mapPropertiesToSelector(pp, listElements.contains(we), emScale);
			String className = null;
			String elementName;
			if (selector != null) {
				elementName = selector.getElementName();
				if (elementName == null)
					elementName = "p";
				className = selector.getClassName();
			} else {
				elementName = "p";
			}
			Object lastChild = parent.getLastChild();
			if (lastChild instanceof Element) {
				Element lastElement = (Element) lastChild;
				String lastClass = lastElement.getClassName();
				String lastName = lastElement.getElementName();
				if (elementName.equals(lastName)
						&& (lastClass == null ? className == null : className != null && lastClass.equals(className))) {
					if (shouldMergeParagraphs(pp)) {
						addToParent = false;
						conv = lastElement;
						lastElement.add(chapter.createElement("br"));
					}
				}
			}
			if (conv == null) {
				conv = chapter.createElement(elementName);
				if (className != null)
					conv.setClassName(className);
				resetSpaceProcessing = true;
			} else {
				treatAsSpace();
			}
		} else if (we instanceof RunElement) {
			RunElement wr = (RunElement) we;
			RunProperties rp = wr.getRunProperties();
			SimpleSelector selector = styleConverter.mapPropertiesToSelector(rp, false, emScale);
			if (selector != null) {
				String className = null;
				String elementName;
				if (selector != null) {
					elementName = selector.getElementName();
					if (elementName == null)
						elementName = "span";
					className = selector.getClassName();
				} else {
					elementName = "span";
				}
				conv = chapter.createElement(elementName);
				if (className != null)
					conv.setClassName(className);
			}
		} else if (we instanceof com.adobe.dp.office.word.HyperlinkElement) {
			com.adobe.dp.office.word.HyperlinkElement wa = (com.adobe.dp.office.word.HyperlinkElement) we;
			HyperlinkElement a = chapter.createHyperlinkElement("a");
			String href = wa.getHRef();
			if (href != null)
				a.setExternalHRef(href);
			conv = a;
		} else if (we instanceof FootnoteReferenceElement) {
			FootnoteReferenceElement wf = (FootnoteReferenceElement) we;
			String fid = wf.getID();
			if (fid != null) {
				XRef xref = (XRef) footnoteMap.get(fid);
				if (xref != null) {
					HyperlinkElement a = chapter.createHyperlinkElement("a");
					a.setClassName("footnote-ref");
					a.setXRef(xref);
					a.add("[" + fid + "]");
					conv = a;
				}
			}
			resetSpaceProcessing = true;
		} else if (we instanceof FootnoteElement) {
			FootnoteElement wf = (FootnoteElement) we;
			String fid = wf.getID();
			if (fid != null) {
				conv = chapter.createElement("div");
				footnoteMap.put(fid, conv.getSelfRef());
				conv.setClassName("footnote");
				Element ft = chapter.createElement("h6");
				ft.setClassName("footnote-title");
				conv.add(ft);
				ft.add(fid);
			}
			resetSpaceProcessing = true;
		} else if (we instanceof TableElement) {
			TableElement wt = (TableElement) we;
			TableProperties tp = wt.getTableProperties();
			conv = chapter.createElement("table");
			PrototypeRule prule = styleConverter.stylesheet.createPrototypeRule();
			styleConverter.addDirectPropertiesWithREM(prule, tp, emScale);
			styleConverter.addCellBorderProperties(prule, tp);
			styleConverter.resolveREM(prule, emScale);
			Rule rule = styleConverter.stylesheet.getClassRuleForPrototype(prule);
			if (rule == null) {
				String className = styleConverter.getUniqueClassName("tb", false);
				rule = styleConverter.stylesheet.createClassRuleForPrototype(className, prule);
			}
			String className = ((SimpleSelector) rule.getSelector()).getClassName();
			conv.setClassName(className);
			resetSpaceProcessing = true;
		} else if (we instanceof TableRowElement) {
			conv = chapter.createElement("tr");
			conv.setClassName(parent.getClassName());
			resetSpaceProcessing = true;
		} else if (we instanceof TableCellElement) {
			conv = chapter.createElement("td");
			conv.setClassName(parent.getClassName());
			resetSpaceProcessing = true;
		} else if (we instanceof TextElement) {
			parent.add(processSpaces(((TextElement) we).getText()));
			return;
		} else if (we instanceof TabElement) {
			parent.add(processSpaces("\t"));
			return;
		} else if (we instanceof BRElement) {
			conv = chapter.createElement("br");
			resetSpaceProcessing = true;
		} else if (we instanceof DrawingElement) {
			DrawingElement wd = (DrawingElement) we;
			PictureData picture = wd.getPictureData();
			if (picture != null) {
				try {
					Resource imageResource = getImageResource(picture);
					if (imageResource != null) {
						ImageElement img = chapter.createImageElement("img");
						img.setImageResource(imageResource);
						conv = img;
						if (picture.getWidth() > 0 && picture.getHeight() > 0) {
							double widthPt = picture.getWidth();
							setImageWidth(img, "img", widthPt, emScale);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			resetSpaceProcessing = true;
		} else if (we instanceof PictElement) {
			Iterator it = we.content();
			VMLGroupElement group = null;
			while (it.hasNext()) {
				Object child = it.next();
				if (child instanceof VMLGroupElement) {
					group = (VMLGroupElement) child;
					break;
				}
			}
			if (group != null) {
				Hashtable style = group.getStyle();
				if (style != null) {
					String widthStr = (String) style.get("width");
					if (widthStr != null) {
						float widthPt = VMLPathConverter.readCSSLength(widthStr, 0);
						boolean embed = true;
						VMLConverter vmlConv = new VMLConverter(this, embed);
						if (embed) {
							SVGElement svg = chapter.createSVGElement("svg");
							vmlConv.convertVML(chapter, svg, group);
							parent.add(svg);
							setImageWidth(svg, "svg", widthPt, emScale);
						} else {
							String name = epub.makeUniqueResourceName(mediaFolder + "vml-embed.svg");
							OPSResource svgResource = epub.createOPSResource(name, "image/svg+xml");
							OPSDocument svgDoc = svgResource.getDocument();
							SVGElement svg = (SVGElement)svgDoc.getBody();
							vmlConv.convertVML(svgDoc, svg, group);
							ImageElement img = chapter.createImageElement("img");
							parent.add(img);
							img.setImageResource(svgResource);
							setImageWidth(img, "img", widthPt, emScale);
						}
					}
				}
			}
			return;
		} else if (we instanceof TXBXContentElement) {
			conv = chapter.createElement("body");
			conv.setClassName("embed");
			resetSpaceProcessing = true;
		} else {
			// unknown element
			return;
		}
		if (conv != null) {
			if (addToParent)
				parent.add(conv);
			parent = conv;
		}
		if (resetSpaceProcessing)
			resetSpaceProcessing();
		Object fontSize = parent.getCascadedProperty("font-size");
		if (fontSize != null && fontSize instanceof CSSLength) {
			CSSLength fs = (CSSLength) fontSize;
			if (fs.getUnit().equals("em")) {
				double scale = fs.getValue();
				if (scale > 0)
					emScale *= (float) scale;
			}
		}
		addChildren(chapter, parent, we, emScale);
		if (resetSpaceProcessing)
			resetSpaceProcessing();
	}

	private void addChildren(OPSDocument chapter, Element parent, com.adobe.dp.office.word.Element we, float emScale) {
		Iterator children = we.content();
		ListControl listControl = null;
		while (children.hasNext()) {
			com.adobe.dp.office.word.Element child = (com.adobe.dp.office.word.Element) children.next();
			if (listElements.contains(child)) {
				if (listControl == null)
					listControl = new ListControl();
				parent = listControl.process(chapter, parent, (ParagraphElement) child);
			} else {
				if (listControl != null) {
					parent = listControl.finish();
					listControl = null;
				}
			}
			appendConvertedElement(child, parent, chapter, emScale);
		}
	}

	void findLists(ContainerElement ce) {
		Iterator it = ce.content();
		ParagraphElement prevElement = null;
		Object prevNumId = null;
		while (it.hasNext()) {
			Object n = it.next();
			if (n instanceof ParagraphElement) {
				ParagraphElement pe = (ParagraphElement) n;
				ParagraphProperties pp = pe.getParagraphProperties();
				if (pp != null) {
					Style style = pp.getParagraphStyle();
					if (style != null && !style.getStyleId().startsWith("Heading")) {
						NumberingProperties np = pp.getNumberingProperties();
						if (np != null) {
							Object numId = np.get("numId");
							if (numId != null) {
								if (prevNumId != null && numId.equals(prevNumId)) {
									if (prevElement != null) {
										listElements.add(prevElement);
										prevElement = null;
									}
									listElements.add(pe);
								} else {
									prevElement = pe;
									prevNumId = numId;
								}
								continue;
							}
						}
					}
				}
			} else if (n instanceof ContainerElement) {
				findLists((ContainerElement) n);
			}
			prevNumId = null;
		}
	}

	void convert(BodyElement wbody, OPSResource ops) {
		OPSDocument doc = ops.getDocument();
		doc.getBody().setClassName("primary");
		addChildren(doc, doc.getBody(), wbody, 1);
	}

	void setWordResources(ContainerSource source) {
		wordResources = source;
	}

}
