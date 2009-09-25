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

package com.adobe.dp.xml.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {

	public static String replace(String src, String olds, String news) {
		int index = src.indexOf(olds);
		if (index < 0)
			return src;
		StringBuffer sb = new StringBuffer(src.substring(0, index));
		int olen = olds.length();
		while (true) {
			sb.append(news);
			index += olen;
			int newIndex = src.indexOf(olds, index);
			if (newIndex < 0) {
				sb.append(src.substring(index));
				break;
			}
			sb.append(src.substring(index, newIndex));
			index = newIndex;
		}
		String result = sb.toString();
		return result;
	}
	
	public static String dateToW3CDTF(Date date) {
		SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		String s = w3cdtf.format(date);
		int index = s.length() - 2;
		return s.substring(0, index) + ":" + s.substring(index);
	}
	
	
}
