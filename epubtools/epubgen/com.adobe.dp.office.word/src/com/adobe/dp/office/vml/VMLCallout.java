package com.adobe.dp.office.vml;

public class VMLCallout {
	
	public final char code; // '@' or '#'
	public final int index;
	
	public VMLCallout(char code, int index) {
		this.code = code;
		this.index = index;
	}
}
