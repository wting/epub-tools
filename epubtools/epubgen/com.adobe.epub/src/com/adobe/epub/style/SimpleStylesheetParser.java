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

package com.adobe.epub.style;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.adobe.otf.FontProperties;

public class SimpleStylesheetParser {

	Hashtable rules = new Hashtable();

	private static int skipWhitespace(Reader reader) throws IOException {
		while (true) {
			reader.mark(1);
			int ch = reader.read();
			if (ch < 0)
				return ch;
			if (!Character.isWhitespace((char) ch)) {
				reader.reset();
				return ch;
			}
		}
	}

	private static void skipRule(Reader reader) throws IOException {
		boolean started = false;
		int counter = 0;
		while (!started && counter > 0) {
			int ch = reader.read();
			if (ch < 0)
				return;
			if (ch == '{') {
				started = true;
				counter++;
			} else if (ch == '}') {
				counter--;
			}
		}
	}

	private static String readName(Reader reader) throws IOException {
		StringBuffer sb = new StringBuffer();
		while (true) {
			reader.mark(1);
			int ch = reader.read();
			if (ch < 0)
				return null;
			if (ch != '-' && !Character.isLetterOrDigit((char) ch)) {
				reader.reset();
				return sb.toString();
			}
			sb.append((char) ch);
		}
	}

	private static int hexValue(int ch) {
		if ('0' <= ch && ch <= '9')
			return ch - '0';
		if ('a' <= ch && ch <= 'f')
			return ch - ('a' - 10);
		if ('A' <= ch && ch <= 'F')
			return ch - ('A' - 10);
		return -1;
	}

	private static Object readValue(Reader reader, boolean partOfList) throws IOException {
		int ch = skipWhitespace(reader);
		StringBuffer sb = new StringBuffer();
		if (ch == '\'' || ch == '\"') {
			int fin = ch;
			reader.read();
			while (true) {
				ch = reader.read();
				if (ch < 0 || ch == fin)
					break;
				if (ch == '\\') {
					ch = reader.read();
					if (ch == '\n')
						continue;
					if (ch == '\r') {
						reader.mark(1);
						ch = reader.read();
						if (ch != '\n')
							reader.reset();
						continue;
					}
					if (hexValue(ch) >= 0) {
						int unival = hexValue(ch);
						for (int i = 0; i < 6; i++) {
							reader.mark(1);
							ch = reader.read();
							if (ch < 0 || ch == ' ' || ch == '\n')
								break;
							if (ch == '\r') {
								reader.mark(1);
								ch = reader.read();
								if (ch != '\n')
									reader.reset();
								break;
							}
							int hv = hexValue(ch);
							if (hv < 0) {
								reader.reset();
								break;
							}
							unival = (unival << 4) | hv;
						}
						ch = unival;
					}
				}
				sb.append((char) ch);
			}
			return new QuotedString(sb.toString());
		} else {
			int braceCount = 0;
			boolean done = false;
			while (true) {
				reader.mark(1);
				ch = reader.read();
				if (ch < 0)
					break;
				switch (ch) {
				case '{':
					braceCount++;
					break;
				case '}':
					if (braceCount == 0)
						done = true;
					else
						braceCount--;
					break;
				case ';':
					if (braceCount == 0)
						done = true;
					break;
				case ',':
					if (partOfList)
						done = true;
					break;
				}
				if (done) {
					reader.reset();
					break;
				}
				sb.append((char) ch);
			}
			return sb.toString().trim();
		}
	}

	public static ValueList readValueList(Reader reader) throws IOException {
		ValueList list = new ValueList();
		while (true) {
			Object value = readValue(reader, true);
			if (value == null)
				return list;
			list.add(value);
			int ch = skipWhitespace(reader);
			if (ch != ',')
				return list;
			reader.read();
		}
	}

	public static ValueList readValueList(String value) {
		try {
			return readValueList(new StringReader(value));
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void readProperties(Reader reader, BaseRule rule) throws IOException {
		while (true) {
			int ch = skipWhitespace(reader);
			if (!Character.isLetter((char) ch) && ch != '-')
				break;
			String prop = readName(reader);
			ch = skipWhitespace(reader);
			if (ch != ':')
				break;
			reader.read();
			skipWhitespace(reader);
			Object value;
			if (prop.equals("font-family"))
				value = readValueList(reader);
			else
				value = readValue(reader, false);
			if (value == null)
				break;
			rule.set(prop, value);
			reader.mark(1);
			ch = reader.read();
			if (ch != ';') {
				reader.reset();
				break;
			}
		}
	}

	public static void readProperties(String value, BaseRule rule) {
		try {
			readProperties(new StringReader(value), rule);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static FontProperties makeFontProperties(Rule rule) {
		Object family = rule.get("font-family");
		if (family == null)
			return null;
		String familyText;
		if (family instanceof QuotedString)
			familyText = ((QuotedString) family).getText();
		else
			familyText = family.toString();

		int weight = FontProperties.WEIGHT_NORMAL;
		Object w = rule.get("font-weight");
		if (w != null) {
			String ws = w.toString();
			if (ws.equals("bold"))
				weight = FontProperties.WEIGHT_BOLD;
			else if (ws.endsWith("00")) {
				try {
					weight = Integer.parseInt(ws);
				} catch (NumberFormatException e) {
				}
			}
		}

		int style = FontProperties.STYLE_REGULAR;
		Object s = rule.get("font-style");
		if (s != null) {
			if (s.equals("italic"))
				style = FontProperties.STYLE_ITALIC;
			else if (s.equals("oblique"))
				style = FontProperties.STYLE_OBLIQUE;
		}
		return new FontProperties(familyText, weight, style);
	}

	private static String getFontSrcURL(Rule rule) {
		Object srcObj = rule.get("src");
		if (srcObj != null) {
			String src = srcObj.toString().trim();
			if (src.startsWith("url(") && src.endsWith(")")) {
				return src.substring(4, src.length() - 1).trim();
			}
		}
		return null;
	}

	public void readRules(Reader reader) throws IOException {
		Vector selectors = new Vector();
		while (true) {
			int ch = skipWhitespace(reader);
			if (ch < 0)
				return;
			String name = null;
			String cls = null;
			boolean fontFace = false;
			if (Character.isLetter((char) ch) || ch == '-') {
				name = readName(reader);
				if (name == null)
					return;
				ch = skipWhitespace(reader);
			}
			if (ch == '.') {
				reader.read();
				cls = readName(reader);
				if (cls == null)
					return;
				ch = skipWhitespace(reader);
			} else if (ch == '@') {
				reader.read();
				String atRule = readName(reader);
				if (atRule != null && atRule.equals("font-face")) {
					fontFace = true;
					ch = skipWhitespace(reader);
				} else {
					skipRule(reader);
					continue;
				}
			}
			SimpleSelector selector = new SimpleSelector(name, cls);
			selectors.add(selector);
			if (ch == ',') {
				reader.read();
				continue;
			}
			if (ch != '{')
				return;
			reader.read();
			Rule rule = null;
			if (!fontFace && selectors.size() == 1) {
				rule = (Rule) rules.get(selector);
				if (rule == null) {
					rule = new Rule(selector);
					rules.put(selector, rule);
				}
			} else {
				rule = new Rule(selector);
			}
			readProperties(reader, rule);
			if (fontFace) {
				String src = getFontSrcURL(rule);
				if (src != null) {
					FontProperties props = makeFontProperties(rule);
					if (props != null)
						rules.put(props, src);
				}
			} else if (selectors.size() != 1) {
				Iterator it = selectors.iterator();
				while (it.hasNext()) {
					selector = (SimpleSelector) it.next();
					Rule tr = (Rule) rules.get(selector);
					if (tr == null) {
						tr = new Rule(selector);
						rules.put(selector, tr);
					}
					Iterator props = rule.properties();
					while (props.hasNext()) {
						String prop = (String) props.next();
						tr.set(prop, rule.get(prop));
					}
				}
			}
			selectors.clear();
			reader.mark(1);
			ch = reader.read();
			if (ch != '}')
				reader.reset();

		}
	}

	public void readRules(String value) {
		try {
			readRules(new StringReader(value));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Hashtable getRules() {
		return rules;
	}

}
