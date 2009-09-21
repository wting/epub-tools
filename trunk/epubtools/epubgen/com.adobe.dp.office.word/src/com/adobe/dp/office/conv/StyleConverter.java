package com.adobe.dp.office.conv;

import java.util.HashSet;
import java.util.Iterator;

import com.adobe.dp.epub.style.BaseRule;
import com.adobe.dp.epub.style.CSSLength;
import com.adobe.dp.epub.style.PrototypeRule;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.SimpleSelector;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.office.types.Border;
import com.adobe.dp.office.types.BorderSide;
import com.adobe.dp.office.types.FontFamily;
import com.adobe.dp.office.types.Frame;
import com.adobe.dp.office.types.Indent;
import com.adobe.dp.office.types.Paint;
import com.adobe.dp.office.types.RGBColor;
import com.adobe.dp.office.types.Spacing;
import com.adobe.dp.office.word.BaseProperties;
import com.adobe.dp.office.word.ParagraphProperties;
import com.adobe.dp.office.word.RunProperties;
import com.adobe.dp.office.word.Style;

public class StyleConverter {

	Stylesheet stylesheet;

	Style documentDefaultParagraphStyle;

	double defaultFontSize;

	// set of classes
	HashSet classNames = new HashSet();

	int styleCount = 1;

	boolean usingPX;

	private static final int RUN_COMPLEX = 1;

	private static final int RUN_STYLE = 2;

	private static final int RUN_BOLD = 4;

	private static final int RUN_ITALIC = 8;

	private static final int RUN_SUPER = 0x10;

	private static final int RUN_SUB = 0x20;

	StyleConverter(Stylesheet stylesheet, boolean usingPX) {
		this.stylesheet = stylesheet;
		this.usingPX = usingPX;
		classNames.add("primary");
		classNames.add("embed");
		classNames.add("footnote");
		classNames.add("footnote-ref");
		classNames.add("footnote-title");
	}

	StyleConverter(StyleConverter conv) {
		this(conv.stylesheet, true);
		this.classNames = conv.classNames;
	}

	void setDocumentDefaultParagraphStyle(Style documentDefaultParagraphStyle) {
		this.documentDefaultParagraphStyle = documentDefaultParagraphStyle;
	}

	boolean usingPX() {
		return usingPX;
	}

	void setDefaultFontSize(double defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
	}

	void addDirectProperties(String elementName, BaseRule rule, BaseProperties prop, float emScale) {
		addDirectPropertiesWithREM(elementName, rule, prop, emScale);
		resolveREM(rule, emScale);
	}

	private String findUniqueClassName(String base, boolean tryBase) {
		if (tryBase) {
			if (!classNames.contains(base))
				return base;
			base = base + "-";
		}
		while (true) {
			String name = base + styleCount;
			if (!classNames.contains(name)) {
				return name;
			}
			styleCount++;
		}
	}

	String getUniqueClassName(String base, boolean tryBase) {
		String cname = findUniqueClassName(base, tryBase);
		classNames.add(cname);
		return cname;
	}

	private String mapToElement(String styleId) {
		if (styleId != null) {
			if (styleId.equals("Title"))
				return "h1";
			if (styleId.equals("Heading1"))
				return "h1";
			if (styleId.equals("Heading2"))
				return "h2";
			if (styleId.equals("Heading3"))
				return "h3";
			if (styleId.equals("Heading4"))
				return "h4";
			if (styleId.equals("Heading5"))
				return "h5";
			if (styleId.equals("Heading6"))
				return "h6";
		}
		return null;
	}

	private int getRunMask(RunProperties rp) {
		int result = 0;
		if (rp == null)
			return 0;
		if (rp.getRunStyle() != null)
			result = RUN_STYLE;
		if (!rp.isEmpty()) {
			Iterator it = rp.properties();
			while (it.hasNext()) {
				String prop = (String) it.next();
				if (prop.equals("b")) {
					Object val = rp.get("b");
					if (val.equals(Boolean.TRUE))
						result |= RUN_BOLD;
				} else if (prop.equals("i")) {
					Object val = rp.get("i");
					if (val.equals(Boolean.TRUE))
						result |= RUN_ITALIC;
				} else if (prop.equals("vertAlign")) {
					Object val = rp.get("vertAlign");
					if (val.equals("superscript"))
						result |= RUN_SUPER;
					else if (val.equals("subscript"))
						result |= RUN_SUB;
				} else {
					result |= RUN_COMPLEX;
				}
			}
		}
		return result;
	}

	static void setIfNotPresent(BaseRule rule, String name, Object value) {
		Object val = rule.get(name);
		// space in front means it was not an implied, not explicitly set value
		if (val == null || (val instanceof String && ((String) val).startsWith(" ")))
			rule.set(name, value);
	}

	String convertBorderSide(BorderSide side) {
		String type = side.getType();
		if (type.equals("single"))
			type = "solid";
		else
			type = "solid";
		Paint paint = side.getColor();
		String color = (paint instanceof RGBColor ? ((RGBColor) paint).toCSSString() : "black");
		double width = side.getWidth() / 8.0;
		return (width > 1 ? width + "px " : "1px ") + type + " " + color;
	}

	private String getFontFamilyString(FontFamily family) {
		String name = family.getName();
		StringBuffer result = new StringBuffer();
		if (name.indexOf(' ') >= 0) {
			result.append('\'');
			result.append(name);
			result.append('\'');
		} else {
			result.append(name);
		}
		String backupName = null;
		String shape = family.getFamily();
		String pitch = family.getPitch();
		if (pitch != null && pitch.equals("fixed")) {
			backupName = "monospace";
		} else if (shape != null) {
			if (shape.equals("roman"))
				backupName = "serif";
			else if (shape.equals("swiss"))
				backupName = "sans-serif";
		}
		if (backupName != null) {
			result.append(',');
			result.append(backupName);
		}
		return result.toString();
	}

	void addCellBorderProperties(BaseRule rule, BaseProperties prop) {
		if (prop == null || prop.isEmpty())
			return;
		Border border = (Border) prop.get("tblBorders");
		if (border == null)
			return;
		if (border.getInsideH() != null) {
			CSSLength paddingDef = new CSSLength(border.getInsideH().getSpace() / 8.0, "px");
			String borderDef = convertBorderSide(border.getInsideH());
			setIfNotPresent(rule, "padding-top", paddingDef);
			setIfNotPresent(rule, "padding-bottom", paddingDef);
			setIfNotPresent(rule, "border-top", borderDef);
			setIfNotPresent(rule, "border-bottom", borderDef);
		}
		if (border.getInsideV() != null) {
			CSSLength paddingDef = new CSSLength(border.getInsideV().getSpace() / 8.0, "px");
			String borderDef = convertBorderSide(border.getInsideV());
			setIfNotPresent(rule, "padding-left", paddingDef);
			setIfNotPresent(rule, "padding-right", paddingDef);
			setIfNotPresent(rule, "border-left", borderDef);
			setIfNotPresent(rule, "border-right", borderDef);
		}
	}

	void addDirectPropertiesWithREM(String elementName, BaseRule rule, BaseProperties prop, float emScale) {
		final float normalWidth = 612; // convert to percentages of this
		if (prop == null || prop.isEmpty())
			return;
		Iterator props = prop.properties();
		while (props.hasNext()) {
			String name = (String) props.next();
			Object value = prop.get(name);
			if (name.equals("b")) {
				boolean bold = value.equals(Boolean.TRUE);
				setIfNotPresent(rule, "font-weight", (bold ? "bold" : "normal"));
			} else if (name.equals("i")) {
				boolean italic = value.equals(Boolean.TRUE);
				setIfNotPresent(rule, "font-style", (italic ? "italic" : "normal"));
			} else if (name.equals("rFonts")) {
				FontFamily fontFamily = (FontFamily) value;
				setIfNotPresent(rule, "font-family", getFontFamilyString(fontFamily));
			} else if (name.equals("sz")) {
				if (usingPX) {
					double fontSize = ((Number) value).doubleValue() / 2;
					setIfNotPresent(rule, "font-size", new CSSLength(fontSize, "px"));
				} else {
					double fontSize = ((Number) value).doubleValue() / (emScale * defaultFontSize);
					setIfNotPresent(rule, "font-size", new CSSLength(fontSize, "em"));
				}
			} else if (name.equals("u")) {
				Object val = rule.get("text-decoration");
				if (val == null || !val.equals("line-through"))
					rule.set("text-decoration", "underline");
				else
					rule.set("text-decoration", "underline, line-through");
			} else if (name.equals("strike")) {
				Object val = rule.get("text-decoration");
				if (val == null || !val.equals("underline"))
					rule.set("text-decoration", "line-through");
				else
					rule.set("text-decoration", "underline, line-through");
			} else if (name.equals("color")) {
				setIfNotPresent(rule, "color", ((RGBColor) value).toCSSString());
			} else if (name.equals("highlight")) {
				setIfNotPresent(rule, "background-color", ((RGBColor) value).toCSSString());
			} else if (name.equals("shd")) {
				if (value instanceof RGBColor)
					setIfNotPresent(rule, "background-color", ((RGBColor) value).toCSSString());
			} else if (name.equals("pBdr") || name.equals("tblBorders")) {
				Border border = (Border) value;
				if (border.getTop() != null) {
					setIfNotPresent(rule, "padding-top", new CSSLength(border.getTop().getSpace() / 8.0, "px"));
					setIfNotPresent(rule, "border-top", convertBorderSide(border.getTop()));
				}
				if (border.getBottom() != null) {
					setIfNotPresent(rule, "padding-bottom", new CSSLength(border.getBottom().getSpace() / 8.0, "px"));
					setIfNotPresent(rule, "border-bottom", convertBorderSide(border.getBottom()));
				}
				if (border.getLeft() != null) {
					setIfNotPresent(rule, "padding-left", new CSSLength(border.getLeft().getSpace() / 8.0, "px"));
					setIfNotPresent(rule, "border-left", convertBorderSide(border.getLeft()));
				}
				if (border.getRight() != null) {
					setIfNotPresent(rule, "padding-right", new CSSLength(border.getRight().getSpace() / 8.0, "px"));
					setIfNotPresent(rule, "border-right", convertBorderSide(border.getRight()));
				}
			} else if (name.equals("jc")) {
				String css = "left";
				if (value.equals("both"))
					css = "justify";
				else if (value.equals("right") || value.equals("center"))
					css = value.toString();
				setIfNotPresent(rule, "text-align", css);
			} else if (name.equals("webHidden")) {
				boolean hidden = value.equals(Boolean.TRUE);
				if (hidden)
					setIfNotPresent(rule, "display", "none");
			} else if (name.equals("spacing")) {
				Spacing insets = (Spacing) value;
				double halfPtSize = insets.getBefore() / 10.0;
				if (usingPX) {
					double pxSize = halfPtSize / 2;
					setIfNotPresent(rule, "margin-top", new CSSLength(pxSize, "px"));
				} else {
					double remSize = halfPtSize / defaultFontSize;
					setIfNotPresent(rule, "margin-top", new CSSLength(remSize, "rem"));
				}
				halfPtSize = insets.getAfter() / 10.0;
				if (usingPX) {
					double pxSize = halfPtSize / 2;
					setIfNotPresent(rule, "margin-bottom", new CSSLength(pxSize, "px"));
				} else {
					double remSize = halfPtSize / defaultFontSize;
					setIfNotPresent(rule, "margin-bottom", new CSSLength(remSize, "rem"));
				}
				if (insets.getLine() > 0) {
					halfPtSize = insets.getLine() / 10.0;
					if (usingPX) {
						double pxSize = halfPtSize / 2;
						setIfNotPresent(rule, "line-height", new CSSLength(pxSize, "px"));
					} else {
						double remSize = halfPtSize / defaultFontSize;
						setIfNotPresent(rule, "line-height", new CSSLength(remSize, "rem"));
					}
				}
			} else if (name.equals("ind")) {
				Indent ind = (Indent) value;
				if (ind.getLeft() > 0) {
					float pts = ind.getLeft() / 20;
					if (pts > 0) {
						float percent = 100 * pts / normalWidth;
						setIfNotPresent(rule, "margin-left", new CSSLength(percent, "%"));
					}
				}
				if (ind.getRight() > 0) {
					float pts = ind.getRight() / 20;
					float percent = 100 * pts / normalWidth;
					setIfNotPresent(rule, "margin-right", new CSSLength(percent, "%"));
				}
				if (ind.getFirstLine() > 0) {
					double halfPtSize = ind.getFirstLine() / 10.0;
					if (usingPX) {
						double pxSize = halfPtSize / 2;
						setIfNotPresent(rule, "text-indent", new CSSLength(pxSize, "px"));
					} else {
						double remSize = halfPtSize / defaultFontSize;
						setIfNotPresent(rule, "text-indent", new CSSLength(remSize, "rem"));
					}
				}
			} else if (name.equals("framePr")) {
				Frame f = (Frame) value;
				String align = f.getAlign();
				if (align != null) {
					setIfNotPresent(rule, "float", align);
				} else {
					// extra space indicates it was not explicitly set
					align = "left";
					setIfNotPresent(rule, "float", " left");
				}
				float width = f.getWidth();
				if (width > 0) {
					float pts = width / 20;
					float percent = 100 * pts / normalWidth;
					setIfNotPresent(rule, "width", new CSSLength(percent, "%"));
				}
				float hSpace = f.getHSpace();
				if (hSpace > 0) {
					float pts = hSpace / 20;
					CSSLength margin = new CSSLength(pts, "px");
					if (align == null || align.equals("left"))
						setIfNotPresent(rule, "margin-right", margin);
					else
						setIfNotPresent(rule, "margin-left", margin);
				}
				float vSpace = f.getVSpace();
				if (vSpace > 0) {
					float pts = vSpace / 20;
					setIfNotPresent(rule, "margin-bottom", new CSSLength(pts, "px"));
				}
			}
		}
	}

	void resolveREM(BaseRule rule, float emScale) {
		Object fontSize = rule.get("font-size");
		if (fontSize instanceof CSSLength) {
			CSSLength len = (CSSLength) fontSize;
			if (len.getUnit().equals("em")) {
				emScale *= len.getValue();
			}
		}
		if (emScale == 0) {
			// bad, bad, bad
			emScale = 1;
		}
		Iterator props = rule.properties();
		while (props.hasNext()) {
			String prop = (String) props.next();
			Object val = rule.get(prop);
			if (val instanceof CSSLength) {
				CSSLength len = (CSSLength) val;
				if (len.getUnit().equals("rem")) {
					if (prop.equals("line-height"))
						val = new Double(len.getValue());
					else
						val = new CSSLength(len.getValue() / emScale, "em");
					rule.set(prop, val); // hopefully won't screw up iterator
				}
			}
		}
	}

	private void convertStylingRule(String elementName, BaseRule rule, BaseProperties prop, float emScale) {
		boolean runOnly = prop instanceof RunProperties;
		Style style;
		if (runOnly) {
			RunProperties rp = (RunProperties) prop;
			addDirectPropertiesWithREM(elementName, rule, prop, emScale);
			style = rp.getRunStyle();
		} else {
			ParagraphProperties pp = (ParagraphProperties) prop;
			addDirectPropertiesWithREM(elementName, rule, prop, emScale);
			style = pp.getParagraphStyle();
		}
		while (style != null) {
			addDirectPropertiesWithREM(elementName, rule, style.getRunProperties(), emScale);
			if (!runOnly)
				addDirectPropertiesWithREM(elementName,rule, style.getParagraphProperties(), emScale);
			style = style.getParent();
		}
		if (!runOnly && documentDefaultParagraphStyle != null)
			addDirectPropertiesWithREM(elementName, rule, documentDefaultParagraphStyle.getParagraphProperties(), emScale);
		if (elementName != null && elementName.startsWith("h")) {
			if (rule.get("font-weight") == null)
				rule.set("font-weight", "normal");
		}
		resolveREM(rule, emScale);
	}

	SimpleSelector mapPropertiesToSelector(BaseProperties prop, boolean isListElement, float emScale) {
		if (prop == null)
			return null;
		String elementName = null;
		String className = null;
		if (prop instanceof ParagraphProperties) {
			ParagraphProperties pp = (ParagraphProperties) prop;
			Style style = pp.getParagraphStyle();
			boolean noInlineStyling = pp.isEmpty() && pp.getNumberingProperties() == null
					&& pp.getRunProperties() == null;
			if (style == null) {
				if (noInlineStyling)
					return stylesheet.getSimpleSelector("p", null);
			} else if (isListElement) {
				elementName = "li";
			} else {
				elementName = mapToElement(style.getStyleId());
			}
			if (elementName == null && style != null) {
				className = findUniqueClassName(style.getStyleId(), pp.isEmpty());
			} else {
				if (!noInlineStyling || elementName.equals("h1") || elementName.equals("li"))
					className = findUniqueClassName("p", false);
			}
		} else {
			RunProperties rp = (RunProperties) prop;
			int runMask = getRunMask(rp);
			switch (runMask) {
			case 0:
				// don't create unneeded span element
				return null;
			case RUN_BOLD:
				// bold only
				return stylesheet.getSimpleSelector("b", null);
			case RUN_ITALIC:
				// italic only
				return stylesheet.getSimpleSelector("i", null);
			case RUN_SUB:
				// subscript only
				return stylesheet.getSimpleSelector("sub", null);
			case RUN_SUPER:
				// superscript only
				return stylesheet.getSimpleSelector("sup", null);
			}
			if ((runMask & RUN_STYLE) != 0) {
				className = findUniqueClassName(rp.getRunStyle().getStyleId(), rp.isEmpty());
			} else {
				className = findUniqueClassName("r", false);
			}
		}
		PrototypeRule pr = stylesheet.createPrototypeRule();
		convertStylingRule((elementName==null?"p":elementName), pr, prop, emScale);
		if (elementName != null && elementName.equals("li")) {
			// leading space indicates implicit value
			pr.set("margin-top", " 0px");
			pr.set("margin-bottom", " 0px");
		}
		Rule rule = stylesheet.getClassRuleForPrototype(pr);
		if (rule == null) {
			if (className == null)
				className = findUniqueClassName("p", false);
			rule = stylesheet.createClassRuleForPrototype(className, pr);
			classNames.add(className);
		} else {
			className = ((SimpleSelector) rule.getSelector()).getClassName();
		}
		return stylesheet.getSimpleSelector(elementName, className);
	}
}
