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

package com.adobe.dp.epub.style;

import java.util.Iterator;
import java.util.Vector;

public class ValueList {

	Vector values = new Vector();
	
	public ValueList() {
	}
	
	public ValueList(Object value) {
		values.add(value);
	}
	
	public ValueList(Object v1, Object v2) {
		values.add(v1);
		values.add(v2);
	}
	
	public ValueList(Object v1, Object v2, Object v3) {
		values.add(v1);
		values.add(v2);
		values.add(v3);
	}
	
	public Iterator values() {
		return values.iterator();
	}
		
	public void add(Object value) {
		values.add(value);
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		String sep = "";
		Iterator vals = values();
		while( vals.hasNext() ) {
			sb.append(sep);
			sb.append(vals.next());
			sep = ", ";
		}
		return sb.toString();
	}
	
	public boolean equals(Object other) {
		if (other.getClass() != getClass())
			return false;
		ValueList o = (ValueList) other;
		return o.values.equals(values);		
	}
	
	public int hashCode() {
		return values.hashCode();
	}
}
