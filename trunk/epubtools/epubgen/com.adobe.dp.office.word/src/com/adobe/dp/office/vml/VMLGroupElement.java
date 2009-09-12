package com.adobe.dp.office.vml;

import java.util.Hashtable;

import org.xml.sax.Attributes;

public class VMLGroupElement extends VMLElement {

	VMLCoordPair origin;
	VMLCoordPair size;
	
	VMLGroupElement( Attributes attr ) {
		super(attr);
		this.origin = VMLCoordPair.parse(attr.getValue("coordorigin"), 0, 0);
		this.size = VMLCoordPair.parse(attr.getValue("coordsize"), 1000, 1000);
	}
	
	public Hashtable getStyle() {
		return style;
	}

	public VMLCoordPair getOrigin() {
		return origin;
	}

	public VMLCoordPair getSize() {
		return size;
	}
}
