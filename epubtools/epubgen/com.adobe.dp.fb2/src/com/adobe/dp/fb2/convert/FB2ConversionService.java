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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import com.adobe.dp.epub.conv.GUIDriver;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.fb2.FB2Document;
import com.adobe.dp.fb2.FB2TitleInfo;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

public class FB2ConversionService implements ConversionService {

	BufferedImage fb2icon;

	public FB2ConversionService() {
		InputStream png = FB2ConversionService.class
				.getResourceAsStream("fb2.png");
		try {
			fb2icon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean canConvert(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".fb2") || name.endsWith("fb2.zip");
	}

	public boolean canUse(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".css") || name.endsWith(".otf")
				|| name.endsWith(".ttf") || name.endsWith("ttc");
	}

	public File convert(File src, File[] aux, ConversionClient client) {
		try {
			InputStream fb2in = new FileInputStream(src);
			FB2Document doc = new FB2Document(fb2in);
			Publication epub = new Publication();
			epub.setTranslit(true);
			epub.useAdobeFontMangling();
			fb2in.close();
			FB2TitleInfo bookInfo = doc.getTitleInfo();
			String title = (bookInfo == null ? null : bookInfo.getBookTitle());
			String fname;
			if (title == null)
				fname = "book";
			else
				fname = Translit.translit(title).replace(' ', '_').replace(
						'\t', '_').replace('\n', '_').replace('\r', '_');
			File outFile = File.createTempFile(fname, ".epub");
			OutputStream out = new FileOutputStream(outFile);
			OCFContainerWriter container = new OCFContainerWriter(out);
			Converter conv = new Converter();
			FontLocator fontLocator = DefaultFontLocator.getInstance();
			conv.setFontLocator(fontLocator);
			conv.convert(doc, epub);
			epub.serialize(container);
			return outFile;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Image getIcon(File src) {
		return fb2icon;
	}

	public static void main(String[] args) {
		GUIDriver.main(args);
	}
}
