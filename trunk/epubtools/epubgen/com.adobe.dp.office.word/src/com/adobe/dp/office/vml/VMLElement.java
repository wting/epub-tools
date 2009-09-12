package com.adobe.dp.office.vml;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;

import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.word.ContainerElement;
import com.adobe.dp.office.word.TXBXContentElement;

public class VMLElement extends ContainerElement {

	Hashtable style;

	RGBColor fill;

	RGBColor stroke;

	String strokeWeight;

	float opacity = 1;
	
	String startArrow;
	
	String endArrow;
		
	VMLElement(Attributes attr) {
		this.style = parseStyle(attr.getValue("style"));
		String f = attr.getValue("filled");
		if ((f == null || f.startsWith("t")) && isDrawable()) {
			this.fill = parseColor(attr.getValue("fillcolor"));
			if (this.fill == null)
				this.fill = new RGBColor(0xFFFFFF); // default value: white
		}
		f = attr.getValue("stroked");
		if ((f == null || f.startsWith("t")) && isDrawable()) {
			this.stroke = parseColor(attr.getValue("strokecolor"));
			if (this.stroke == null)
				this.stroke = new RGBColor(0); // default value: black
			this.strokeWeight = attr.getValue("strokeweight");
			if( this.strokeWeight == null )
				this.strokeWeight = "0.75pt"; // default value
		}
		f = attr.getValue("opacity");
		if( f != null ) {
			if( f.endsWith("f") ) {
				opacity = Integer.parseInt(f.substring(0,f.length()-1))/65536.0f;
			} else {
				opacity = Float.parseFloat(f);
			}
		}
	}

	public float getOpacity() {
		return opacity;
	}
	
	protected boolean isDrawable() {
		return true;
	}

	public Hashtable getStyle() {
		return style;
	}

	public RGBColor getFill() {
		return fill;
	}

	public RGBColor getStroke() {
		return stroke;
	}

	public String getStrokeWeight() {
		return strokeWeight;
	}

	static Hashtable parseStyle(String style) {
		if (style == null)
			return null;
		Hashtable result = new Hashtable();
		StringTokenizer tok = new StringTokenizer(style, ";");
		while (tok.hasMoreTokens()) {
			String t = tok.nextToken();
			int i = t.indexOf(':');
			if (i > 0) {
				String prop = t.substring(0, i).trim();
				String val = t.substring(i + 1).trim();
				result.put(prop, val);
			}
		}
		return result;
	}

	static RGBColor parseColor(String color) {
		if (color == null || !color.startsWith("#") || color.length() < 7)
			return null;
		try {
			int ival = Integer.parseInt(color.substring(1, 7), 16);
			return new RGBColor(ival);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public TXBXContentElement getTextBoxContentElement() {
		Iterator it = content();
		while( it.hasNext() ) {
			Object child = it.next();
			if( child instanceof VMLTextboxElement ) {
				Iterator cit = ((VMLTextboxElement)child).content();
				while( cit.hasNext() ) {
					child = cit.next();
					if( child instanceof TXBXContentElement )
						return (TXBXContentElement)child;
				}
			}
		}
		return null;
	}
}
