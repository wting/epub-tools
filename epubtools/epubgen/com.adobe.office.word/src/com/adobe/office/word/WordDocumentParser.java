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

package com.adobe.office.word;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.adobe.office.embedded.EmbeddedObject;
import com.adobe.office.types.Border;
import com.adobe.office.types.BorderSide;
import com.adobe.office.types.FontFamily;
import com.adobe.office.types.Paint;
import com.adobe.office.types.RGBColor;
import com.adobe.office.types.Spacing;

public class WordDocumentParser {

	static final String wNS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	static final String wpNS = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing";

	static final String aNS = "http://schemas.openxmlformats.org/drawingml/2006/main";

	static final String picNS = "http://schemas.openxmlformats.org/drawingml/2006/picture";

	static final String vNS = "urn:schemas-microsoft-com:vml";

	static final String rNS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

	static final String rPkNS = "http://schemas.openxmlformats.org/package/2006/relationships";

	static final String hyperlinkRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink";

	static final String imageRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image";

	static final String stylesRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles";

	static final String numberingRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering";

	static final String themeRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme";

	static final String fontsRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable";

	WordDocument doc;

	Hashtable rels;

	File docFile;

	ZipFile zip;

	Stack contextStack;

	SAXParserFactory factory;

	String stylesName;

	String fontsName;

	String themeName;

	String numberingName;

	Hashtable fonts = new Hashtable();

	String majorFontName;

	String minorFontName;

	static final Hashtable propertyParsers = new Hashtable();

	static final Hashtable colorTable = new Hashtable();

	static {
		SimplePropertyParser simpleParser = new SimplePropertyParser("");
		propertyParsers.put("numId", simpleParser);
		propertyParsers.put("vertAlign", simpleParser);
		propertyParsers.put("u", simpleParser);
		propertyParsers.put("jc", simpleParser);
		NumberPropertyParser numberParser = new NumberPropertyParser();
		propertyParsers.put("sz", numberParser);
		propertyParsers.put("spacing-r", numberParser);
		OnOffPropertyParser onOffParser = new OnOffPropertyParser();
		propertyParsers.put("b", onOffParser);
		propertyParsers.put("i", onOffParser);
		propertyParsers.put("webHidden", onOffParser);
		propertyParsers.put("strike", onOffParser);
		propertyParsers.put("keepNext", onOffParser);
		propertyParsers.put("keepLines", onOffParser);
		IntegerPropertyParser integerParser = new IntegerPropertyParser();
		propertyParsers.put("ilvl", integerParser);
		propertyParsers.put("outlineLvl", integerParser);
		SpacingPropertyParser insetsParser = new SpacingPropertyParser();
		propertyParsers.put("spacing", insetsParser);
		PaintPropertyParser paintParser = new PaintPropertyParser();
		propertyParsers.put("color", paintParser);
		propertyParsers.put("highlight", paintParser);
		ShadingPropertyParser shdParser = new ShadingPropertyParser();
		propertyParsers.put("shd", shdParser);
		FontsPropertyParser fontsParser = new FontsPropertyParser();
		propertyParsers.put("rFonts", fontsParser);

		colorTable.put("white", new RGBColor(0xFFFFFF));
		colorTable.put("black", new RGBColor(0x000000));
		colorTable.put("red", new RGBColor(0xFF0000));
		colorTable.put("green", new RGBColor(0x00FF00));
		colorTable.put("blue", new RGBColor(0x0000FF));
		colorTable.put("yellow", new RGBColor(0xFFFF00));
		colorTable.put("magenta", new RGBColor(0xFF00FF));
		colorTable.put("cyan", new RGBColor(0x00FFFF));
	}

	static class ParseContext {
		Element parentElement;

		Style parentStyle;

		BaseProperties properties;

		Border border;

		EmbeddedObject embedded;

		FontFamily font;

		String state;
	}

	static class Relationship {
		String type;

		String target;

		String targetMode;

		Relationship(String type, String target, String targetMode) {
			this.target = target;
			this.type = type;
			this.targetMode = targetMode;
		}
	}

	abstract static class PropertyParser {

		abstract boolean parse(String localName, Attributes attributes, WordDocumentParser self);

		String propertyName;

		Object propertyValue;
	}

	static class SimplePropertyParser extends PropertyParser {

		SimplePropertyParser(Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			propertyValue = attributes.getValue(wNS, "val");
			if (propertyValue == null)
				propertyValue = defaultValue;
			return true;
		}

		Object defaultValue;
	}

	static class OnOffPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String val = attributes.getValue(wNS, "val");
			if (val == null || val.equals("on"))
				propertyValue = Boolean.TRUE;
			else
				propertyValue = Boolean.FALSE;
			return true;
		}
	}

	static class NumberPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String val = attributes.getValue(wNS, "val");
			if (val != null) {
				try {
					propertyValue = new Double(val);
					return true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	static class IntegerPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String val = attributes.getValue(wNS, "val");
			if (val != null) {
				try {
					propertyValue = new Integer(val);
					return true;
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}

	static class SpacingPropertyParser extends PropertyParser {
		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String before = attributes.getValue(wNS, "before");
			String after = attributes.getValue(wNS, "after");
			String line = attributes.getValue(wNS, "line");
			float beforeVal = 0;
			float afterVal = 0;
			float lineVal = 0;
			try {
				if (before != null)
					beforeVal = Float.parseFloat(before);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			try {
				if (after != null)
					afterVal = Float.parseFloat(after);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			try {
				if (line != null)
					lineVal = Float.parseFloat(line);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			propertyValue = new Spacing(beforeVal, afterVal, lineVal);
			return true;
		}
	}

	static class PaintPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String val = attributes.getValue(wNS, "val");
			propertyValue = parsePaint(val);
			return propertyValue != null;
		}
	}

	static class ShadingPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String val = attributes.getValue(wNS, "fill");
			if (val == null)
				return false;
			propertyValue = parsePaint(val);
			return propertyValue != null;
		}
	}

	static class FontsPropertyParser extends PropertyParser {

		boolean parse(String localName, Attributes attributes, WordDocumentParser self) {
			propertyName = localName;
			String name = attributes.getValue(wNS, "ascii");
			if (name == null) {
				String themeName = attributes.getValue(wNS, "asciiTheme");
				if (themeName != null) {
					if (themeName.equals("majorHAnsi"))
						name = self.majorFontName;
					else if (themeName.equals("minorHAnsi"))
						name = self.minorFontName;
				}
			}
			if (name != null)
				propertyValue = self.fonts.get(name);
			return propertyValue != null;
		}
	}

	static Paint parsePaint(String val) {
		if (val == null)
			return null;
		if (val.equals("auto"))
			val = "black";
		Paint propertyValue = (Paint) colorTable.get(val);
		if (propertyValue != null)
			return propertyValue;
		try {
			int ival = Integer.parseInt(val, 16);
			return new RGBColor(ival);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	static PropertyParser getPropertyParser(BaseProperties properties, String localName) {
		return (PropertyParser) propertyParsers.get(localName);
	}

	private BorderSide parseBorderSide(Attributes attributes) {
		String val = attributes.getValue(wNS, "val");
		String szVal = attributes.getValue(wNS, "sz");
		float sz = 0;
		if (szVal != null)
			try {
				sz = Float.parseFloat(szVal);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		String spaceVal = attributes.getValue(wNS, "space");
		float space = 0;
		if (spaceVal != null)
			try {
				space = Float.parseFloat(spaceVal);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		String colorVal = attributes.getValue(wNS, "color");
		Paint color = parsePaint(colorVal);
		return new BorderSide(val, sz, space, color);
	}

	class XMLHandler extends DefaultHandler implements DrawingElement.Context {

		XMLHandler(String prefix) {
			xrefPrefix = prefix;
		}

		String xrefPrefix;

		public void characters(char[] ch, int start, int length) throws SAXException {
			ParseContext context = (ParseContext) contextStack.peek();
			Element p = context.parentElement;
			if (p instanceof TextElement) {
				((TextElement) p).text += new String(ch, start, length);
			}
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			ParseContext context = (ParseContext) contextStack.pop();
			EmbeddedObject embedded = context.embedded;
			if (embedded != null) {
				embedded.finish(this);
				if (!contextStack.isEmpty()) {
					ParseContext parentContext = (ParseContext) contextStack.peek();
					if (parentContext.embedded != null)
						parentContext.embedded.finishChild(this, embedded);
				}
			}
			if (context.parentStyle != null) {
				Style style = context.parentStyle;
				if (style.parent == null) {
					if (style != doc.defaultParagraphStyle && style != doc.defaultRunStyle) {
						if (style.type != null) {
							if (style.type.equals("character"))
								style.parent = doc.defaultRunStyle;
							else if (style.type.equals("paragraph"))
								style.parent = doc.defaultParagraphStyle;
						}
					}
				}
			}
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			ParseContext newContext = new ParseContext();
			if (contextStack.isEmpty()) {

			} else {
				ParseContext parentContext = (ParseContext) contextStack.peek();
				if (uri != null) {
					if (uri.equals(wNS)) {
						if (localName.equals("style")) {
							newContext.parentStyle = new Style();
							String styleId = attributes.getValue(wNS, "styleId");
							if (styleId != null) {
								newContext.parentStyle.styleId = styleId;
								doc.stylesById.put(styleId, newContext.parentStyle);
							}
							newContext.parentStyle.type = attributes.getValue(wNS, "type");
						} else if (localName.equals("font")) {
							String name = attributes.getValue(wNS, "name");
							if (name != null) {
								FontFamily font = new FontFamily(name);
								newContext.font = font;
								fonts.put(name, font);
							}
						} else if (localName.endsWith("Pr")) {
							BaseProperties prop = createWordProperties(localName);
							newContext.properties = prop;
							if (parentContext.parentStyle != null)
								assignStyleProperties(parentContext.parentStyle, prop);
							else if (parentContext.parentElement != null)
								assignElementProperties(parentContext.parentElement, prop);
							else if (parentContext.properties != null)
								assignInnerProperties(parentContext.properties, prop);
						} else if (parentContext.parentStyle != null) {
							if (localName.equals("name")) {
								parentContext.parentStyle.name = attributes.getValue(wNS, "val");
							} else if (localName.equals("basedOn")) {
								String parentStyle = attributes.getValue(wNS, "val");
								if (parentStyle != null)
									parentContext.parentStyle.parent = doc.getStyleById(parentStyle);
							}
						} else if (parentContext.font != null) {
							if (localName.equals("panose1")) {
								parentContext.font.setPanose(attributes.getValue(wNS, "val"));
							} else if (localName.equals("family")) {
								parentContext.font.setFamily(attributes.getValue(wNS, "val"));
							} else if (localName.equals("pitch")) {
								parentContext.font.setPitch(attributes.getValue(wNS, "val"));
							}
						} else if (parentContext.border != null) {
							if (localName.equals("top") || localName.equals("bottom") || localName.equals("left")
									|| localName.equals("right") || localName.equals("insideH")
									|| localName.equals("insideV")) {
								BorderSide side = parseBorderSide(attributes);
								if (localName.equals("top"))
									parentContext.border.setTop(side);
								else if (localName.equals("bottom"))
									parentContext.border.setBottom(side);
								else if (localName.equals("left"))
									parentContext.border.setLeft(side);
								else if (localName.equals("right"))
									parentContext.border.setRight(side);
								else if (localName.equals("insideH"))
									parentContext.border.setInsideH(side);
								else if (localName.equals("insideV"))
									parentContext.border.setInsideV(side);
							}
						} else if (parentContext.properties != null) {
							BaseProperties prop = parentContext.properties;
							if (localName.equals("rStyle") || localName.equals("pStyle")) {
								String val = attributes.getValue(wNS, "val");
								if (val != null) {
									Style style = doc.getStyleById(val);
									if (prop instanceof ParagraphProperties && localName.equals("pStyle"))
										((ParagraphProperties) prop).paragraphStyle = style;
									else if (prop instanceof RunProperties && localName.equals("rStyle"))
										((RunProperties) prop).runStyle = style;
								}
							} else if (localName.equals("pBdr") || localName.equals("tblBorders")) {
								newContext.border = new Border();
								prop.put(localName, newContext.border);
							} else {
								if (localName.equals("spacing") && parentContext.properties instanceof RunProperties)
									localName = "spacing-r";
								PropertyParser propertyParser = getPropertyParser(parentContext.properties, localName);
								if (propertyParser != null) {
									if (propertyParser.parse(localName, attributes, WordDocumentParser.this))
										parentContext.properties.put(propertyParser.propertyName,
												propertyParser.propertyValue);
								}
							}
						} else if (localName.equals("rPrDefault")) {
							newContext.parentStyle = doc.defaultRunStyle;
						} else if (localName.equals("pPrDefault")) {
							newContext.parentStyle = doc.defaultParagraphStyle;
						} else {
							Element p = createWordElement(localName, attributes);
							if (p != null) {
								Element parent = parentContext.parentElement;
								if (parent instanceof ContainerElement) {
									((ContainerElement) parent).add(p);
								}
								if (contextStack.size() == 1 && localName.equals("body"))
									doc.body = (BodyElement) p;
								newContext.parentElement = p;
								if (p instanceof EmbeddedObject) {
									newContext.embedded = (EmbeddedObject) p;
								}
							}
						}
					} else if (uri.equals(rPkNS)) {
						if (localName.equals("Relationship")) {
							String id = attributes.getValue("Id");
							String type = attributes.getValue("Type");
							String target = attributes.getValue("Target");
							String targetMode = attributes.getValue("TargetMode");
							if (id != null)
								rels.put(id, new Relationship(type, target, targetMode));
							if (type.equals(numberingRel)) {
								if (numberingName == null)
									numberingName = target;
							} else if (type.equals(stylesRel)) {
								if (stylesName == null)
									stylesName = target;
							} else if (type.equals(fontsRel)) {
								if (fontsName == null)
									fontsName = target;
							} else if (type.equals(themeRel)) {
								if (themeName == null)
									themeName = target;
							}
						}
					} else {
						if (uri.equals(aNS)) {
							if (localName.equals("majorFont"))
								newContext.state = "majorFont";
							else if (localName.equals("minorFont"))
								newContext.state = "minorFont";
							else if (localName.equals("latin")) {
								String typeface = attributes.getValue("typeface");
								if (parentContext.state == "majorFont")
									majorFontName = typeface;
								else if (parentContext.state == "minorFont")
									minorFontName = typeface;
							}
						}
						if (parentContext.embedded != null)
							newContext.embedded = parentContext.embedded.newChild(this, uri, localName, attributes);
					}
				}
			}
			contextStack.push(newContext);
		}

		public String getPictureURL(String resId) {
			Relationship rel = (Relationship) rels.get(resId);
			if (rel != null && rel.type.equals(imageRel))
				return xrefPrefix + rel.target;
			return null;
		}

	}

	public WordDocumentParser(File docFile) {
		this.docFile = docFile;
	}

	public WordDocument parse() throws IOException {
		doc = new WordDocument();
		parseInternal();
		return doc;
	}

	void parseInternal() throws IOException {
		doc.body = null;
		doc.defaultParagraphStyle = new Style("__p");
		doc.defaultRunStyle = new Style("__r");
		doc.stylesById = new Hashtable();
		zip = new ZipFile(docFile);
		factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		rels = new Hashtable();
		contextStack = new Stack();
		numberingName = null;
		stylesName = null;
		fontsName = null;
		themeName = null;
		parseXML("word/_rels/document.xml.rels");
		contextStack.clear();
		if (themeName != null) {
			parseXML("word/" + themeName);
			contextStack.clear();
		}
		if (fontsName != null) {
			parseXML("word/" + fontsName);
			contextStack.clear();
		}
		if (stylesName != null) {
			parseXML("word/" + stylesName);
			contextStack.clear();
		}
		if (numberingName != null) {
			parseXML("word/" + numberingName);
			contextStack.clear();
		}
		parseXML("word/document.xml");
		contextStack.clear();
	}

	private void parseXML(String entryName) throws IOException {
		ZipEntry entry = zip.getEntry(entryName);
		if (entry == null)
			return;
		try {
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			int index = entryName.lastIndexOf('/');
			String xrefPrefix = entryName.substring(0, index + 1);
			XMLHandler handler = new XMLHandler(xrefPrefix);
			reader.setContentHandler(handler);
			InputStream in = zip.getInputStream(entry);
			InputSource source = new InputSource(in);
			source.setSystemId(entryName);
			reader.parse(source);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	private void assignStyleProperties(Style style, BaseProperties prop) {
		if (prop instanceof ParagraphProperties) {
			style.paragraphProperties = (ParagraphProperties) prop;
		} else if (prop instanceof RunProperties) {
			style.runProperties = (RunProperties) prop;
		}
	}

	private void assignElementProperties(Element element, BaseProperties prop) {
		if (prop instanceof ParagraphProperties) {
			if (element instanceof ParagraphElement) {
				((ParagraphElement) element).paragraphProperties = (ParagraphProperties) prop;
			}
		} else if (prop instanceof RunProperties) {
			if (element instanceof RunElement) {
				((RunElement) element).runProperties = (RunProperties) prop;
			}
		} else if (prop instanceof TableProperties) {
			if (element instanceof TableElement) {
				((TableElement) element).tableProperties = (TableProperties) prop;
			}
		} else if (prop instanceof TableRowProperties) {
			if (element instanceof TableRowElement) {
				((TableRowElement) element).tableRowProperties = (TableRowProperties) prop;
			}
		} else if (prop instanceof TableCellProperties) {
			if (element instanceof TableCellElement) {
				((TableCellElement) element).tableCellProperties = (TableCellProperties) prop;
			}
		}
	}

	private void assignInnerProperties(BaseProperties parent, BaseProperties prop) {
		if (parent instanceof ParagraphProperties) {
			ParagraphProperties p = (ParagraphProperties) parent;
			if (prop instanceof RunProperties)
				p.runProperties = (RunProperties) prop;
			else if (prop instanceof NumberingProperties)
				p.numberingProperties = (NumberingProperties) prop;
		}
	}

	private BaseProperties createWordProperties(String localName) {
		if (localName.equals("pPr"))
			return new ParagraphProperties();
		if (localName.equals("rPr"))
			return new RunProperties();
		if (localName.equals("numPr"))
			return new NumberingProperties();
		if (localName.equals("tblPr"))
			return new TableProperties();
		if (localName.equals("trPr"))
			return new TableRowProperties();
		if (localName.equals("tcPr"))
			return new TableCellProperties();
		return null;
	}

	private Element createWordElement(String localName, Attributes attributes) {
		if (localName.equals("t")) {
			TextElement te = new TextElement();
			String val = attributes.getValue("http://www.w3.org/XML/1998/namespace", "space");
			te.preserveSpace = val != null && val.equals("preserve");
			return te;
		}
		if (localName.equals("p"))
			return new ParagraphElement();
		if (localName.equals("body"))
			return new BodyElement();
		if (localName.equals("r"))
			return new RunElement();
		if (localName.equals("tab"))
			return new TabElement();
		if (localName.equals("br"))
			return new BRElement();
		if (localName.equals("drawing"))
			return new DrawingElement();
		if (localName.equals("tbl"))
			return new TableElement();
		if (localName.equals("tr"))
			return new TableRowElement();
		if (localName.equals("tc"))
			return new TableCellElement();
		if (localName.equals("drawing"))
			return new DrawingElement();
		if (localName.equals("hyperlink")) {
			HyperlinkElement he = new HyperlinkElement();
			String rid = attributes.getValue(rNS, "id");
			if (rid != null) {
				Relationship rel = (Relationship) rels.get(rid);
				if (rel != null)
					he.href = rel.target;
			}
			return he;
		}
		return null;
	}
}
