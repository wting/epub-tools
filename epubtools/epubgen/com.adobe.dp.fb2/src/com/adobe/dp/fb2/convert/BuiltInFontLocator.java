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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import com.adobe.dp.otf.ByteArrayFontInputStream;
import com.adobe.dp.otf.FontInputStream;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.otf.FontProperties;
import com.adobe.dp.otf.FontPropertyConstants;

public class BuiltInFontLocator extends FontLocator {

	static Hashtable fontMap;

	static {
		Hashtable map = new Hashtable();
		map.put(new FontProperties("Times New Roman",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "times.ttf");
		map.put(new FontProperties("Times New Roman",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "timesbd.ttf");
		map.put(new FontProperties("Times New Roman",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "timesi.ttf");
		map.put(new FontProperties("Times New Roman",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "timesbi.ttf");
		map.put(new FontProperties("Georgia",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "georgia.ttf");
		map.put(new FontProperties("Georgia",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "georgiab.ttf");
		map.put(new FontProperties("Georgia",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "georgiai.ttf");
		map.put(new FontProperties("Georgia",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "georgiaz.ttf");
		map.put(new FontProperties("Verdana",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "verdana.ttf");
		map.put(new FontProperties("Verdana",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "verdanab.ttf");
		map.put(new FontProperties("Verdana",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "verdanai.ttf");
		map.put(new FontProperties("Verdana",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "verdanaz.ttf");
		map.put(new FontProperties("Arial",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "arial.ttf");
		map.put(new FontProperties("Arial", FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "arialbd.ttf");
		map.put(new FontProperties("Arial",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "ariali.ttf");
		map.put(new FontProperties("Arial", FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "arialbi.ttf");
		map.put(new FontProperties("Trebuchet MS",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "trebuc.ttf");
		map.put(new FontProperties("Trebuchet MS",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "trebucbd.ttf");
		map.put(new FontProperties("Trebuchet MS",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "trebuci.ttf");
		map.put(new FontProperties("Trebuchet MS",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "trebucbi.ttf");
		map.put(new FontProperties("Courier New",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "cour.ttf");
		map.put(new FontProperties("Courier New",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "courbd.ttf");
		map.put(new FontProperties("Courier New",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_ITALIC), "couri.ttf");
		map.put(new FontProperties("Courier New",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_ITALIC), "courbi.ttf");
		map.put(new FontProperties("Comic Sans MS",
				FontPropertyConstants.WEIGHT_NORMAL,
				FontPropertyConstants.STYLE_REGULAR), "comic.ttf");
		map.put(new FontProperties("Comic Sans MS",
				FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "comicbd.ttf");
		map.put(new FontProperties("Impact", FontPropertyConstants.WEIGHT_BOLD,
				FontPropertyConstants.STYLE_REGULAR), "impact.ttf");
		fontMap = map;
	}

	public FontInputStream locateFont(FontProperties key) throws IOException {
		String name = (String) fontMap.get(key);
		if (name != null) {
			InputStream in = BuiltInFontLocator.class
					.getResourceAsStream("fonts/" + name);
			if (in != null) {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				int len;
				while ((len = in.read(buf)) >= 0) {
					buffer.write(buf, 0, len);
				}
				return new ByteArrayFontInputStream(buffer.toByteArray());
			}
		}
		return null;
	}

	public boolean hasFont(FontProperties key) {
		return fontMap.get(key) != null;
	}

}
