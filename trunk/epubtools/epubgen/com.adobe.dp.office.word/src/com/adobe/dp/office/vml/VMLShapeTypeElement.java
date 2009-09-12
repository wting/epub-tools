package com.adobe.dp.office.vml;

import java.util.StringTokenizer;

import org.xml.sax.Attributes;

public class VMLShapeTypeElement extends VMLElement {

	VMLCoordPair size;

	VMLCoordPair origin;

	VMLCoordPair limo;

	String id;

	VMLPathSegment[] path;

	VMLFormulasElement formulas;

	int[] adj;

	Object[] textbox;

	boolean strokeok = true;

	boolean fillok = true;

	VMLShapeTypeElement(Attributes attr) {
		super(attr);
		this.id = attr.getValue("id");
		this.origin = VMLCoordPair.parse(attr.getValue("coordorigin"), 0, 0);
		this.size = VMLCoordPair.parse(attr.getValue("coordsize"), 1000, 1000);
		this.limo = VMLCoordPair.parse(attr.getValue("limo"));
		this.path = VMLPathSegment.parse(attr.getValue("path"));
		this.adj = parseAdj(attr.getValue("adj"));
	}

	void setTextBox(String textbox) {
		StringTokenizer tok = new StringTokenizer(textbox, ", ");
		int n = tok.countTokens();
		if (n != 4)
			return;
		Object[] tb = new Object[4];
		for (int i = 0; i < 4; i++) {
			String str = tok.nextToken();
			if (str.startsWith("@") || str.startsWith("#")) {
				int index = Integer.parseInt(str.substring(1));
				tb[i] = new VMLCallout(str.charAt(0), index);
			} else {
				tb[i] = new Integer(Integer.parseInt(str));
			}
		}
		this.textbox = tb;
	}
	
	static int[] parseAdj(String adjs) {
		if (adjs == null)
			return null;
		StringTokenizer tok = new StringTokenizer(adjs, ", ");
		int n = tok.countTokens();
		int[] adj = new int[n];
		for (int i = 0; i < n; i++) {
			adj[i] = Integer.parseInt(tok.nextToken());
		}
		return adj;
	}

	public VMLFormulasElement getFormulas() {
		return formulas;
	}

	public void setFormulas(VMLFormulasElement formulas) {
		this.formulas = formulas;
	}

}
