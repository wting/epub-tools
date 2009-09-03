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

package com.adobe.dp.conv.word2epub;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

import com.adobe.dp.epub.io.BufferedDataSource;
import com.adobe.dp.epub.io.ContainerSource;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.StringDataSource;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.Resource;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.HyperlinkElement;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.XRef;
import com.adobe.dp.epub.style.BaseRule;
import com.adobe.dp.epub.style.CSSLength;
import com.adobe.dp.epub.style.PrototypeRule;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.Selector;
import com.adobe.dp.epub.style.SimpleSelector;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.office.conv.GDISVGSurface;
import com.adobe.dp.office.conv.ResourceWriter;
import com.adobe.dp.office.conv.StreamAndName;
import com.adobe.dp.office.drawing.PictureData;
import com.adobe.dp.office.metafile.WMFParser;
import com.adobe.dp.office.types.Border;
import com.adobe.dp.office.types.BorderSide;
import com.adobe.dp.office.types.FontFamily;
import com.adobe.dp.office.types.Indent;
import com.adobe.dp.office.types.Paint;
import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.types.Spacing;
import com.adobe.dp.office.word.BRElement;
import com.adobe.dp.office.word.BaseProperties;
import com.adobe.dp.office.word.BodyElement;
import com.adobe.dp.office.word.ContainerElement;
import com.adobe.dp.office.word.DrawingElement;
import com.adobe.dp.office.word.FootnoteElement;
import com.adobe.dp.office.word.FootnoteReferenceElement;
import com.adobe.dp.office.word.NumberingProperties;
import com.adobe.dp.office.word.ParagraphElement;
import com.adobe.dp.office.word.ParagraphProperties;
import com.adobe.dp.office.word.RunElement;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.Style;
import com.adobe.dp.office.word.TabElement;
import com.adobe.dp.office.word.TableCellElement;
import com.adobe.dp.office.word.TableElement;
import com.adobe.dp.office.word.TableProperties;
import com.adobe.dp.office.word.TableRowElement;
import com.adobe.dp.office.word.TextElement;
import com.adobe.dp.office.word.WordDocument;
import com.adobe.dp.otf.FontLocator;

public class Converter {

	WordDocument doc;

	Publication epub;

	StyleResource styles;

	Stylesheet stylesheet;

	NCXResource toc;

	// maps Footnote IDs to Footnote XRef
	Hashtable footnoteMap = new Hashtable();

	// set of classes
	HashSet classNames = new HashSet();

	HashSet listElements = new HashSet();

	int styleCount = 1;

	Hashtable convResources = new Hashtable();

	ContainerSource wordResources;
	
	FontLocator fontLocator;

	boolean hadSpace = false;

	double defaultFontSize;

	private static String mediaFolder = "OPS/media/";

	public Converter(WordDocument doc, Publication epub) {
		this.doc = doc;
		this.epub = epub;
		styles = epub.createStyleResource("OPS/style.css");
		stylesheet = styles.getStylesheet();
		toc = epub.getTOC();

		// default font size - have to happen early
		RunProperties rp = doc.getDocumentDefaultRunStyle().getRunProperties();
		Object sz = (rp != null ? rp.get("sz") : null);
		if (sz instanceof Number)
			defaultFontSize = ((Number) sz).doubleValue();
		if (defaultFontSize < 1)
			defaultFontSize = 20;

		// default table styles
		Rule tableRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("table", null));
		tableRule.set("border-collapse", "collapse");
		tableRule.set("border-spacing", "0px");

		// default paragraph styles
		Rule pRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("p", null));
		ParagraphProperties pp = doc.getDocumentDefaultParagraphStyle().getParagraphProperties();
		addDirectProperties(pRule, pp, 1);
		// unlike XHTML, Word's default spacing is zero
		setIfNotPresent(pRule, "margin-top", "0px");
		setIfNotPresent(pRule, "margin-bottom", "0px");
		if (rp != null) {
			Rule bodyRule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("body", null));
			addDirectProperties(bodyRule, rp, 1);
		}
	}

	public void setFontLocator(FontLocator fontLocator) {
		this.fontLocator = fontLocator;
	}
	
	private void findLists(ContainerElement ce) {
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

	public void convert() {

		OPSResource footnotes = null;
		if (doc.getFootnotes() != null) {
			// process footnotes first to build footnote map
			BodyElement fbody = doc.getFootnotes();
			footnotes = epub.createOPSResource("OPS/footnotes.xhtml");
			OPSDocument fdoc = footnotes.getDocument();
			fdoc.addStyleResource(styles);
			addChildren(fdoc, fdoc.getBody(), fbody, 1);
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
		findLists(body);
		OPSResource ops = epub.createOPSResource("OPS/document.xhtml");
		epub.addToSpine(ops);
		OPSDocument doc = ops.getDocument();
		doc.addStyleResource(styles);
		addChildren(doc, doc.getBody(), body, 1);

		if (footnotes != null)
			epub.addToSpine(footnotes);

		epub.generateTOCFromHeadings(5);
		epub.splitLargeChapters();
		if( fontLocator != null )
			epub.addFonts(styles, fontLocator);
		else
			epub.addFonts(styles); // use system fonts
	}

	private static final int RUN_COMPLEX = 1;

	private static final int RUN_STYLE = 2;

	private static final int RUN_BOLD = 4;

	private static final int RUN_ITALIC = 8;

	private static final int RUN_SUPER = 0x10;

	private static final int RUN_SUB = 0x20;

	private int getRunMask(RunProperties rp) {
		int result = 0;
		if (rp == null)
			return 0;
		if (rp.getRunStyle() != null)
			result = RUN_STYLE;
		if (!rp.isEmpty()) {
			Iterator it = rp.properties();
			while (it.hasNext()) {
				String prop = (String) it.next();
				if (prop.equals("b")) {
					Object val = rp.get("b");
					if (val.equals(Boolean.TRUE))
						result |= RUN_BOLD;
				} else if (prop.equals("i")) {
					Object val = rp.get("i");
					if (val.equals(Boolean.TRUE))
						result |= RUN_ITALIC;
				} else if (prop.equals("vertAlign")) {
					Object val = rp.get("vertAlign");
					if (val.equals("superscript"))
						result |= RUN_SUPER;
					else if (val.equals("subscript"))
						result |= RUN_SUB;
				} else {
					result |= RUN_COMPLEX;
				}
			}
		}
		return result;
	}

	private String mapToElement(String styleId) {
		if (styleId != null) {
			if (styleId.equals("Title"))
				return "h1";
			if (styleId.equals("Heading1"))
				return "h1";
			if (styleId.equals("Heading2"))
				return "h2";
			if (styleId.equals("Heading3"))
				return "h3";
			if (styleId.equals("Heading4"))
				return "h4";
			if (styleId.equals("Heading5"))
				return "h5";
			if (styleId.equals("Heading6"))
				return "h6";
		}
		return null;
	}

	private String findUniqueClassName(String base, boolean tryBase) {
		if (tryBase) {
			if (!classNames.contains(base))
				return base;
			base = base + "-";
		}
		while (true) {
			String name = base + (styleCount++);
			if (!classNames.contains(name)) {
				return name;
			}
		}
	}

	void setIfNotPresent(BaseRule rule, String name, Object value) {
		if (rule.get(name) == null)
			rule.set(name, value);
	}

	String convertBorderSide(BorderSide side) {
		String type = side.getType();
		if (type.equals("single"))
			type = "solid";
		else
			type = "solid";
		Paint paint = side.getColor();
		String color = (paint instanceof RGBColor ? ((RGBColor) paint).toCSSString() : "black");
		double width = side.getWidth() / 8.0;
		return (width > 0.7 ? width + "pt " : "1px ") + type + " " + color;
	}

	private String getFontFamilyString(FontFamily family) {
		String name = family.getName();
		StringBuffer result = new StringBuffer();
		if (name.indexOf(' ') >= 0) {
			result.append('\'');
			result.append(name);
			result.append('\'');
		} else {
			result.append(name);
		}
		String backupName = null;
		String shape = family.getFamily();
		String pitch = family.getPitch();
		if (pitch != null && pitch.equals("fixed")) {
			backupName = "monospace";
		} else if (shape != null) {
			if (shape.equals("roman"))
				backupName = "serif";
			else if (shape.equals("swiss"))
				backupName = "sans-serif";
		}
		if (backupName != null) {
			result.append(',');
			result.append(backupName);
		}
		return result.toString();
	}

	private void addDirectProperties(BaseRule rule, BaseProperties prop, float emScale) {
		if (prop == null || prop.isEmpty())
			return;
		Iterator props = prop.properties();
		while (props.hasNext()) {
			String name = (String) props.next();
			Object value = prop.get(name);
			if (name.equals("b")) {
				boolean bold = value.equals(Boolean.TRUE);
				setIfNotPresent(rule, "font-weight", (bold ? "bold" : "normal"));
			} else if (name.equals("i")) {
				boolean italic = value.equals(Boolean.TRUE);
				setIfNotPresent(rule, "font-style", (italic ? "italic" : "normal"));
			} else if (name.equals("rFonts")) {
				FontFamily fontFamily = (FontFamily) value;
				setIfNotPresent(rule, "font-family", getFontFamilyString(fontFamily));
			} else if (name.equals("sz")) {
				double fontSize = ((Number) value).doubleValue() / (emScale * defaultFontSize);
				setIfNotPresent(rule, "font-size", new CSSLength(fontSize,"em") );
			} else if (name.equals("u")) {
				Object val = rule.get("text-decoration");
				if (val == null || !val.equals("line-through"))
					rule.set("text-decoration", "underline");
				else
					rule.set("text-decoration", "underline, line-through");
			} else if (name.equals("strike")) {
				Object val = rule.get("text-decoration");
				if (val == null || !val.equals("underline"))
					rule.set("text-decoration", "line-through");
				else
					rule.set("text-decoration", "underline, line-through");
			} else if (name.equals("color")) {
				setIfNotPresent(rule, "color", ((RGBColor) value).toCSSString());
			} else if (name.equals("highlight")) {
				setIfNotPresent(rule, "background-color", ((RGBColor) value).toCSSString());
			} else if (name.equals("shd")) {
				if (value instanceof RGBColor)
					setIfNotPresent(rule, "background-color", ((RGBColor) value).toCSSString());
			} else if (name.equals("pBdr") || name.equals("tblBorders")) {
				Border border = (Border) value;
				if (border.getTop() != null) {
					setIfNotPresent(rule, "padding-top", border.getTop().getSpace() / 8.0 + "pt");
					setIfNotPresent(rule, "border-top", convertBorderSide(border.getTop()));
				}
				if (border.getBottom() != null) {
					setIfNotPresent(rule, "padding-bottom", border.getBottom().getSpace() / 8.0 + "pt");
					setIfNotPresent(rule, "border-bottom", convertBorderSide(border.getBottom()));
				}
				if (border.getLeft() != null) {
					setIfNotPresent(rule, "padding-left", border.getLeft().getSpace() / 8.0 + "pt");
					setIfNotPresent(rule, "border-left", convertBorderSide(border.getLeft()));
				}
				if (border.getRight() != null) {
					setIfNotPresent(rule, "padding-right", border.getRight().getSpace() / 8.0 + "pt");
					setIfNotPresent(rule, "border-right", convertBorderSide(border.getRight()));
				}
			} else if (name.equals("jc")) {
				String css = "left";
				if (value.equals("both"))
					css = "justify";
				else if (value.equals("right") || value.equals("center"))
					css = value.toString();
				setIfNotPresent(rule, "text-align", css);
			} else if (name.equals("webHidden")) {
				boolean hidden = value.equals(Boolean.TRUE);
				if (hidden)
					setIfNotPresent(rule, "display", "none");
			} else if (name.equals("spacing")) {
				Spacing insets = (Spacing) value;
				setIfNotPresent(rule, "margin-top", insets.getBefore() / 20.0 + "pt");
				setIfNotPresent(rule, "margin-bottom", insets.getAfter() / 20.0 + "pt");
				if (insets.getLine() > 0) {
					Object fv = prop.get("sz");
					double fontSize = (fv == null ? 11.0 : ((Number) value).doubleValue() / 2);
					double lineHeight = insets.getLine() / (fontSize * 20.0);
					setIfNotPresent(rule, "line-height", Double.toString(lineHeight));
				}
			} else if (name.equals("ind")) {
				Indent ind = (Indent) value;
				final float normalWidth = 612; // convert to percentages of this
				if (ind.getLeft() > 0) {
					float pts = ind.getLeft() / 20;
					float percent = 100 * pts / normalWidth;
					setIfNotPresent(rule, "margin-left", new CSSLength(percent, "%"));
				}
				if (ind.getRight() > 0) {
					float pts = ind.getRight() / 20;
					float percent = 100 * pts / normalWidth;
					setIfNotPresent(rule, "margin-right", new CSSLength(percent, "%"));
				}
				if (ind.getFirstLine() > 0) {
					prop.get("font-size");
					setIfNotPresent(rule, "text-indent", new CSSLength(ind.getFirstLine() / 20.0, "pt"));
				}
			}
		}
	}

	private void addCellBorderProperties(BaseRule rule, BaseProperties prop) {
		if (prop == null || prop.isEmpty())
			return;
		Border border = (Border) prop.get("tblBorders");
		if (border == null)
			return;
		if (border.getInsideH() != null) {
			String paddingDef = border.getInsideH().getSpace() / 8.0 + "pt";
			String borderDef = convertBorderSide(border.getInsideH());
			setIfNotPresent(rule, "padding-top", paddingDef);
			setIfNotPresent(rule, "padding-bottom", paddingDef);
			setIfNotPresent(rule, "border-top", borderDef);
			setIfNotPresent(rule, "border-bottom", borderDef);
		}
		if (border.getInsideV() != null) {
			String paddingDef = border.getInsideV().getSpace() / 8.0 + "pt";
			String borderDef = convertBorderSide(border.getInsideV());
			setIfNotPresent(rule, "padding-left", paddingDef);
			setIfNotPresent(rule, "padding-right", paddingDef);
			setIfNotPresent(rule, "border-left", borderDef);
			setIfNotPresent(rule, "border-right", borderDef);
		}
	}

	private void convertStylingRule(BaseRule rule, BaseProperties prop, String elementName, float emScale) {
		boolean runOnly = prop instanceof RunProperties;
		Style style;
		if (runOnly) {
			RunProperties rp = (RunProperties) prop;
			addDirectProperties(rule, prop, emScale);
			style = rp.getRunStyle();
		} else {
			ParagraphProperties pp = (ParagraphProperties) prop;
			addDirectProperties(rule, prop, emScale);
			style = pp.getParagraphStyle();
		}
		while (style != null) {
			addDirectProperties(rule, style.getRunProperties(), emScale);
			if (!runOnly)
				addDirectProperties(rule, style.getParagraphProperties(), emScale);
			style = style.getParent();
		}
		if (runOnly)
			addDirectProperties(rule, doc.getDocumentDefaultRunStyle().getRunProperties(), emScale);
		else
			addDirectProperties(rule, doc.getDocumentDefaultParagraphStyle().getParagraphProperties(), emScale);
		if (elementName != null && elementName.startsWith("h")) {
			if (rule.get("font-weight") == null)
				rule.set("font-weight", "normal");
		}
	}

	private SimpleSelector mapPropertiesToSelector(BaseProperties prop, boolean isListElement, float emScale) {
		if (prop == null)
			return null;
		String elementName = null;
		String className = null;
		if (prop instanceof ParagraphProperties) {
			ParagraphProperties pp = (ParagraphProperties) prop;
			Style style = pp.getParagraphStyle();
			boolean noInlineStyling = pp.isEmpty() && pp.getNumberingProperties() == null
					&& pp.getRunProperties() == null;
			if (style == null) {
				if (noInlineStyling)
					return stylesheet.getSimpleSelector("p", null);
			} else if (isListElement) {
				elementName = "li";
			} else {
				elementName = mapToElement(style.getStyleId());
			}
			if (elementName == null && style != null) {
				className = findUniqueClassName(style.getStyleId(), pp.isEmpty());
			} else {
				if (!noInlineStyling || elementName.equals("h1") || elementName.equals("li"))
					className = findUniqueClassName("p", false);
			}
		} else {
			RunProperties rp = (RunProperties) prop;
			int runMask = getRunMask(rp);
			switch (runMask) {
			case 0:
				// don't create unneeded span element
				return null;
			case RUN_BOLD:
				// bold only
				return stylesheet.getSimpleSelector("b", null);
			case RUN_ITALIC:
				// italic only
				return stylesheet.getSimpleSelector("i", null);
			case RUN_SUB:
				// subscript only
				return stylesheet.getSimpleSelector("sub", null);
			case RUN_SUPER:
				// superscript only
				return stylesheet.getSimpleSelector("sup", null);
			}
			if ((runMask & RUN_STYLE) != 0) {
				className = findUniqueClassName(rp.getRunStyle().getStyleId(), rp.isEmpty());
			} else {
				className = findUniqueClassName("r", false);
			}
		}
		PrototypeRule pr = stylesheet.createPrototypeRule();
		convertStylingRule(pr, prop, null, emScale);
		Rule rule = stylesheet.getClassRuleForPrototype(pr);
		if (rule == null) {
			rule = stylesheet.createClassRuleForPrototype(className, pr);
			classNames.add(className);
		} else {
			className = ((SimpleSelector) rule.getSelector()).getClassName();
		}
		return stylesheet.getSimpleSelector(elementName, className);
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

	private void appendConvertedElement(com.adobe.dp.office.word.Element we, Element parent, OPSDocument chapter,
			float emScale) {
		Element conv = null;
		boolean addToParent = true;
		boolean resetSpaceProcessing = false;
		if (we instanceof ParagraphElement) {
			ParagraphElement wp = (ParagraphElement) we;
			ParagraphProperties pp = wp.getParagraphProperties();
			SimpleSelector selector = mapPropertiesToSelector(pp, listElements.contains(we), emScale);
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
			SimpleSelector selector = mapPropertiesToSelector(rp, false, emScale);
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
			PrototypeRule prule = stylesheet.createPrototypeRule();
			addDirectProperties(prule, tp, emScale);
			addCellBorderProperties(prule, tp);
			Rule rule = stylesheet.getClassRuleForPrototype(prule);
			if (rule == null) {
				String className = findUniqueClassName("tb", false);
				classNames.add(className);
				rule = stylesheet.createClassRuleForPrototype(className, prule);
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
							String className = findUniqueClassName("im", false);
							classNames.add(className);
							img.setClassName(className);
							Rule rule = stylesheet.getRuleForSelector(stylesheet.getSimpleSelector("img", className));
							rule.set("width", picture.getWidth() + "pt");
							rule.set("height", picture.getHeight() + "pt");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
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
		if (fontSize != null) {
			String fontSizeStr = fontSize.toString();
			if (fontSizeStr.endsWith("em")) {
				try {
					float scale = Float.parseFloat(fontSizeStr.substring(0, fontSizeStr.length() - 2));
					if (scale > 0)
						emScale *= scale;
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	public void setWordResources(ContainerSource source) {
		wordResources = source;
	}

}
