package com.adobe.dp.office.vml;

import org.xml.sax.Attributes;

public class VMLLineElement extends VMLElement {

	VMLCoordPair from;
	VMLCoordPair to;
	
	VMLLineElement( Attributes attr ) {
		super(attr);
		this.from = VMLCoordPair.parse(attr.getValue("from"), 0, 0);
		this.to = VMLCoordPair.parse(attr.getValue("to"), 10, 10);
	}

	public VMLCoordPair getFrom() {
		return from;
	}

	public VMLCoordPair getTo() {
		return to;
	}
		
}
