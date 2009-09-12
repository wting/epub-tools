package com.adobe.dp.office.vml;

import java.util.Hashtable;

import org.xml.sax.Attributes;

import com.adobe.dp.office.word.Element;

public class VMLElementFactory {

	public static Element createVMLElement(VMLElement parent, Hashtable vmldefs, String localName, Attributes attr) {
		if (localName.equals("group"))
			return new VMLGroupElement(attr);
		if (localName.equals("line"))
			return new VMLLineElement(attr);
		if (localName.equals("oval"))
			return new VMLOvalElement(attr);
		if (localName.equals("rect"))
			return new VMLRectElement(attr);
		if (localName.equals("shape")) {
			VMLShapeElement e = new VMLShapeElement(attr);
			String ref = attr.getValue("type");
			if( ref != null && ref.startsWith("#")) {
				Object def = vmldefs.get(ref.substring(1));
				if( def instanceof VMLShapeTypeElement ) {
					e.type = (VMLShapeTypeElement)def;
				}
			}
			return e;
		}
		if (localName.equals("shapetype")) {
			VMLShapeTypeElement e = new VMLShapeTypeElement(attr);
			if( e.id != null )
				vmldefs.put(e.id, e);
			return e;
		}
		if (localName.equals("formulas"))
			return new VMLFormulasElement(attr);
		if (localName.equals("f"))
			return new VMLFElement(attr);
		if (localName.equals("textbox"))
			return new VMLTextboxElement(attr);
		if (localName.equals("path")) {
			if( parent instanceof VMLShapeTypeElement ) {
				VMLShapeTypeElement ste = (VMLShapeTypeElement)parent;
				VMLCoordPair limo = VMLCoordPair.parse(attr.getValue("limo"));
				if( limo != null )
					ste.limo = limo;
				String strokeok = attr.getValue("strokeok");
				if( strokeok != null && !strokeok.toLowerCase().startsWith("t"))
					ste.strokeok = false;
				String fillok = attr.getValue("fillok");
				if( fillok != null && !fillok.toLowerCase().startsWith("t"))
					ste.fillok = false;
				String textbox = attr.getValue("textboxrect");
				if( textbox != null )
					ste.setTextBox(textbox);
			}
		}
		else if (localName.equals("stroke")) {
			if( parent instanceof VMLElement ) {
				VMLElement e = (VMLElement)parent;
				e.endArrow = attr.getValue("endarrow");
				e.startArrow = attr.getValue("startarrow");
			}
		}
		else if (localName.equals("fill")) {
			if( parent instanceof VMLElement ) {
				VMLElement e = (VMLElement)parent;
			}
		}
		return null;
	}
}
