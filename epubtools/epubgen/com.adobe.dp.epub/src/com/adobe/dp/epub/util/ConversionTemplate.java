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

package com.adobe.dp.epub.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.adobe.dp.epub.style.SimpleStylesheetParser;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.otf.ByteArrayFontInputStream;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import com.adobe.dp.otf.FontPropertyConstants;
import com.adobe.dp.otf.OpenTypeFont;

public class ConversionTemplate {

	ZipFile zip;

	Stylesheet stylesheet;

	TemplateFontLocator fontLocator;

	static Hashtable sharedTemplates = new Hashtable();

	class TemplateFontLocator extends FontLocator {

		Hashtable fontMap;

		TemplateFontLocator(Hashtable fontMap) {
			this.fontMap = fontMap;
		}

		FontProperties substitute(FontProperties key) {
			
			/*
			if ( key.getFamilyName().equals("Tahoma")) {
				// substitute Tahoma with Calibri
				key = new FontProperties("Calibri", key.getWeight(), key.getStyle());
			}
			*/
			
			if (key.getStyle() == FontPropertyConstants.STYLE_ITALIC
					&& key.getFamilyName().equals("Tahoma")) {
				// workaround: Tahoma does not have italic, replace with Verdana
				key = new FontProperties("Verdana", key.getWeight(), key.getStyle());
			}
			return key;
		}
		
		String getFontSource(FontProperties key) {
			key = substitute(key);
			String fileName = (String) fontMap.get(key);
			if (fileName == null) {
				// try a bit bolder...
				FontProperties key1 = new FontProperties(key.getFamilyName(), key.getWeight() + 100, key.getStyle());
				fileName = (String) fontMap.get(key1);
				if (fileName == null) {
					// ...and a bit lighter
					key1 = new FontProperties(key.getFamilyName(), key.getWeight() - 100, key.getStyle());
					fileName = (String) fontMap.get(key);
					if (fileName == null)
						return null;
				}
			}
			return fileName;
		}

		public FontInputStream locateFont(FontProperties key) throws IOException {
			String src = getFontSource(key);
			if (src == null)
				return null;
			return fontStreamForName(src);
		}

		public boolean hasFont(FontProperties key) {
			return getFontSource(key) != null;
		}

	}

	public ConversionTemplate(File templateFile) throws IOException {
		zip = new ZipFile(templateFile);
		Enumeration entries = zip.entries();
		HashSet fonts = new HashSet();
		Hashtable fontMap = new Hashtable();
		while (entries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) entries.nextElement();
			String name = entry.getName().toLowerCase();
			if (name.endsWith(".css")) {
				if (stylesheet != null) {
					Reader reader = new InputStreamReader(zip.getInputStream(entry), "UTF-8");
					SimpleStylesheetParser parser = new SimpleStylesheetParser();
					parser.readRules(reader);
					stylesheet = new Stylesheet(null, parser);
					fontMap = parser.getRules();
				}
			} else if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc")) {
				fonts.add(entry.getName());
			}
		}

		Enumeration srcs = fontMap.elements();
		while (srcs.hasMoreElements()) {
			Object srcobj = srcs.nextElement();
			if (srcobj instanceof String) {
				String src = (String) srcobj;
				fonts.remove(src);
			}
		}

		Iterator nakedFonts = fonts.iterator();
		while (nakedFonts.hasNext()) {
			try {
				String name = (String) nakedFonts.next();
				FontInputStream fin = fontStreamForName(name);
				OpenTypeFont font = new OpenTypeFont(fin, true);
				if (!font.canEmbedForReading())
					continue;
				FontProperties key = new FontProperties(font.getFamilyName(), font.getWeight(), font.getStyle());
				fontMap.put(key, name);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		fontLocator = new TemplateFontLocator(fontMap);
	}

	FontInputStream fontStreamForName(String src) throws IOException {
		ZipEntry entry = zip.getEntry(src);
		if (entry == null)
			throw new IOException("Entry " + src + ": not found");
		InputStream in = zip.getInputStream(entry);
		if (in == null)
			throw new IOException("Entry " + src + ": cannot read");
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) >= 0) {
			buffer.write(buf, 0, len);
		}
		return new ByteArrayFontInputStream(buffer.toByteArray());
	}

	public FontLocator getFontLocator() {
		return fontLocator;
	}

	public Stylesheet getStylesheet() {
		return stylesheet;
	}

	public static ConversionTemplate getConversionTemplate(String path) throws IOException {
		synchronized (sharedTemplates) {
			ConversionTemplate result = (ConversionTemplate) sharedTemplates.get(path);
			if (result == null) {
				result = new ConversionTemplate(new File(path));
				sharedTemplates.put(path, result);
			}
			return result;
		}
	}
}
