package com.adobe.dp.office.vml;

import org.xml.sax.Attributes;

import com.adobe.dp.office.types.RGBColor;

public class VMLShapeElement extends VMLElement {

	VMLCoordPair origin;

	VMLCoordPair size;

	VMLCoordPair limo;

	int[] adj;

	VMLShapeTypeElement type;

	VMLShapeElement(Attributes attr) {
		super(attr);
		this.origin = VMLCoordPair.parse(attr.getValue("coordorigin"));
		this.size = VMLCoordPair.parse(attr.getValue("coordsize"));
		this.limo = VMLCoordPair.parse(attr.getValue("limo"));
		this.adj = VMLShapeTypeElement.parseAdj(attr.getValue("adj"));
	}

	public RGBColor getFill() {
		if( type != null && !type.fillok )
			return null;
		return fill;
	}

	public RGBColor getStroke() {
		if( type != null && !type.strokeok )
			return null;
		return stroke;
	}

}
