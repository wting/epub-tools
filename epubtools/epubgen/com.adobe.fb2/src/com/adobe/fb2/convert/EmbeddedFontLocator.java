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

package com.adobe.fb2.convert;

import java.io.IOException;
import java.util.Hashtable;

import com.adobe.fb2.FB2Binary;
import com.adobe.fb2.FB2Document;
import com.adobe.otf.ByteArrayFontInputStream;
import com.adobe.otf.FontInputStream;
import com.adobe.otf.FontLocator;
import com.adobe.otf.FontProperties;

public class EmbeddedFontLocator extends FontLocator {

	Hashtable stylesheet1;

	FB2Document doc1;
	
	Hashtable stylesheet2;

	FB2Document doc2;

	FontLocator chained;

	public EmbeddedFontLocator(Hashtable stylesheet1, FB2Document doc1,
			Hashtable stylesheet2, FB2Document doc2, FontLocator chained) {
		if (stylesheet1 == null)
			stylesheet1 = new Hashtable();
		if (stylesheet2 == null)
			stylesheet2 = new Hashtable();
		this.stylesheet1 = stylesheet1;
		this.stylesheet2 = stylesheet2;
		this.doc1 = doc1;
		this.doc2 = doc2;
		this.chained = chained;
	}

	public FontInputStream locateFont(FontProperties key) throws IOException {
		FB2Document doc = doc1;
		String src = (String) stylesheet1.get(key);
		if (src == null) {
			src = (String) stylesheet2.get(key);
			doc = doc2;
		}
		if (src != null && src.startsWith("#") && doc != null) {
			FB2Binary res = doc.getBinaryResource(src.substring(1));
			if (res != null) {
				return new ByteArrayFontInputStream(res.getData());
			}
		}
		return chained.locateFont(key);
	}

	public boolean hasFont(FontProperties key) {
		FB2Document doc = doc1;
		String src = (String) stylesheet1.get(key);
		if (src == null) {
			src = (String) stylesheet2.get(key);
			doc = doc2;
		}
		if (src != null && src.startsWith("#") && doc != null) {
			FB2Binary res = doc.getBinaryResource(src.substring(1));
			if (res != null) {
				return true;
			}
		}
		return chained.hasFont(key);
	}

}
