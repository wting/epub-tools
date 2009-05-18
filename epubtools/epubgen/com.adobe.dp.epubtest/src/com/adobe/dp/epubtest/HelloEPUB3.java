package com.adobe.dp.epubtest;

import java.io.FileOutputStream;

import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.io.ResourceDataSource;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.opf.BitmapImageResource;
import com.adobe.dp.epub.opf.NCXResource;
import com.adobe.dp.epub.opf.OPSResource;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.opf.StyleResource;
import com.adobe.dp.epub.ops.Element;
import com.adobe.dp.epub.ops.ImageElement;
import com.adobe.dp.epub.ops.OPSDocument;
import com.adobe.dp.epub.ops.SVGElement;
import com.adobe.dp.epub.style.Rule;
import com.adobe.dp.epub.style.Selector;
import com.adobe.dp.epub.style.Stylesheet;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

public class HelloEPUB3 {

	public static void main(String[] args) {

		try {

			// create new EPUB document
			Publication epub = new Publication();

			// set up title and author
			epub.addDCMetadata("title", "My Third EPUB");
			epub.addDCMetadata("creator", System.getProperty("user.name"));
			epub.addDCMetadata("language", "en");

			// prepare table of contents
			NCXResource toc = epub.getTOC();
			TOCEntry rootTOCEntry = toc.getRootTOCEntry();

			// create a stylesheet
			StyleResource style = epub.createStyleResource("OPS/styles.css");
			Stylesheet stylesheet = style.getStylesheet();

			// style h1 element
			Selector h1Selector = stylesheet.getSimpleSelector("h1", null);
			Rule h1Rule = stylesheet.getRuleForSelector(h1Selector);
			h1Rule.set("color", "gray");
			h1Rule.set("border-bottom", "2px solid gray");
			h1Rule.set("text-align", "right");
			h1Rule.set("margin", "2em 8px 1em 0px");
			h1Rule.set("font-family", "'Adobe Garamond Pro'");

			// style p element
			Selector pSelector = stylesheet.getSimpleSelector("p", null);
			Rule pRule = stylesheet.getRuleForSelector(pSelector);
			pRule.set("margin", "0px");
			pRule.set("text-indent", "1em");
			pRule.set("text-align", "justify");
			pRule.set("font-family", "'Chaparral Pro'");

			// style bitmap class (JPEG image)
			Selector bitmapSelector = stylesheet.getSimpleSelector(null,
					"bitmap");
			Rule bitmapRule = stylesheet.getRuleForSelector(bitmapSelector);
			bitmapRule.set("width", "80%");
			bitmapRule.set("max-width", "553px");

			// style container class (container for JPEG image)
			Selector containerSelector = stylesheet.getSimpleSelector("p",
					"container");
			Rule containerRule = stylesheet
					.getRuleForSelector(containerSelector);
			containerRule.set("text-align", "center");
			containerRule.set("text-indent", "0px");
			containerRule.set("padding", "0.5em 0px");

			// style svgimage class (embedded SVG)
			Selector svgimageSelector = stylesheet.getSimpleSelector(null,
					"svgimage");
			Rule svgimageRule = stylesheet.getRuleForSelector(svgimageSelector);
			svgimageRule.set("width", "80%");
			svgimageRule.set("padding", "0.5em 10%");

			// style label1 class (text in embedded SVG)
			Selector label1Selector = stylesheet.getSimpleSelector(null,
					"label1");
			Rule label1Rule = stylesheet.getRuleForSelector(label1Selector);
			label1Rule.set("font-size", "36");
			label1Rule.set("font-family", "'Chaparral Pro'");

			// style label2 class (text in embedded SVG)
			Selector label2Selector = stylesheet.getSimpleSelector(null,
					"label2");
			Rule label2Rule = stylesheet.getRuleForSelector(label2Selector);
			label2Rule.set("font-size", "48");
			label2Rule.set("font-family", "'Comic Sans MS'");

			// create first chapter resource
			OPSResource chapter1 = epub
					.createOPSResource("OPS/chapter1.html");
			epub.addToSpine(chapter1);

			// get chapter document
			OPSDocument chapter1Doc = chapter1.getDocument();

			// link our stylesheet
			chapter1Doc.addStyleResource(style);

			// add chapter to the table of contents
			TOCEntry chapter1TOCEntry = toc.createTOCEntry("Chapter 1",
					chapter1Doc.getRootXRef());
			rootTOCEntry.add(chapter1TOCEntry);

			// chapter XHTML body element
			Element body1 = chapter1Doc.getBody();

			// add a header
			Element header1 = chapter1Doc.createElement("h1");
			header1.add("One");
			body1.add(header1);

			// add a paragraph
			Element paragraph1 = chapter1Doc.createElement("p");
			StringBuffer sb1 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb1.append("This is sentence " + i
						+ " of the first chapter's first paragraph. ");
			paragraph1.add(sb1.toString());
			body1.add(paragraph1);

			// add SVG graphics area
			SVGElement svg = chapter1Doc.createSVGElement("svg");
			svg.setAttribute("viewBox", "0 0 400 200");
			svg.setClassName("svgimage");
			body1.add(svg);

			// draw background
			SVGElement bg = chapter1Doc.createSVGElement("rect");
			bg.setAttribute("x", "1");
			bg.setAttribute("y", "1");
			bg.setAttribute("width", "398");
			bg.setAttribute("height", "198");
			bg.setAttribute("stroke", "black");
			bg.setAttribute("stroke-width", "2");
			bg.setAttribute("fill", "#FFF8EE");
			svg.add(bg);

			// draw a shape
			SVGElement path = chapter1Doc.createSVGElement("path");
			path.setAttribute("fill", "none");
			path.setAttribute("stroke", "black");
			path.setAttribute("stroke-width", "4");
			path
					.setAttribute("d",
							"M90 120h50l10 20l20-40l20 40l20-40l20 40l20-40l20 40l20-40l20 40l10-20h50");
			svg.add(path);

			// draw a label
			SVGElement label1 = chapter1Doc.createSVGElement("text");
			label1.setClassName("label1");
			label1.setAttribute("x", "150");
			label1.setAttribute("y", "60");
			label1.add("R = 30\u03A9");
			svg.add(label1);

			// draw rotated label
			SVGElement label2 = chapter1Doc.createSVGElement("text");
			label2.setClassName("label2");
			label2.setAttribute("transform", "translate(40 110)rotate(-75)");
			SVGElement t1 = chapter1Doc.createSVGElement("tspan");
			t1.setAttribute("fill", "red");
			t1.add("S");
			label2.add(t1);
			SVGElement t2 = chapter1Doc.createSVGElement("tspan");
			t2.setAttribute("fill", "green");
			t2.add("V");
			label2.add(t2);
			SVGElement t3 = chapter1Doc.createSVGElement("tspan");
			t3.setAttribute("fill", "blue");
			t3.add("G");
			label2.add(t3);
			svg.add(label2);

			// create second chapter resource
			OPSResource chapter2 = epub
					.createOPSResource("OPS/chapter2.html");
			epub.addToSpine(chapter2);

			// get chapter document
			OPSDocument chapter2Doc = chapter2.getDocument();

			// link our stylesheet
			chapter2Doc.addStyleResource(style);

			// add chapter to the table of contents
			TOCEntry chapter2TOCEntry = toc.createTOCEntry("Chapter 2",
					chapter2Doc.getRootXRef());
			rootTOCEntry.add(chapter2TOCEntry);

			// chapter XHTML body element
			Element body2 = chapter2Doc.getBody();

			// add a header
			Element header2 = chapter1Doc.createElement("h1");
			header2.add("Two");
			body2.add(header2);

			// add a paragraph
			Element paragraph2 = chapter2Doc.createElement("p");
			StringBuffer sb2 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb2.append("This is sentence " + i
						+ " of the second chapter's first paragraph. ");
			paragraph2.add(sb2.toString());
			body2.add(paragraph2);

			// add a bitmap resource
			DataSource dataSource = new ResourceDataSource(HelloEPUB3.class,
					"cassini.jpg");
			BitmapImageResource imageResource = epub.createBitmapImageResource(
					"OPS/images/cassini.jpg", "image/jpeg", dataSource);

			// add a bitmap image
			Element container = chapter2Doc.createElement("p");
			container.setClassName("container");
			body2.add(container);
			ImageElement bitmap = chapter2Doc.createImageElement("img");
			bitmap.setClassName("bitmap");
			bitmap.setImageResource(imageResource);
			container.add(bitmap);

			// and another paragraph
			Element paragraph3 = chapter2Doc.createElement("p");
			StringBuffer sb3 = new StringBuffer();
			for (int i = 1; i <= 6; i++)
				sb3.append("This is sentence " + i
						+ " of the second chapter's second paragraph. ");
			paragraph3.add(sb3.toString());
			body2.add(paragraph3);
			
			// embed fonts
			// NB: on non-Windows platforms you need to supply your own
			// FontLocator implementation
			FontLocator fontLocator = DefaultFontLocator.getInstance();
			epub.useAdobeFontMangling();
			epub.addFonts(style, fontLocator);

			// save EPUB to an OCF container
			OCFContainerWriter writer = new OCFContainerWriter(
					new FileOutputStream("hello.epub"));
			epub.serialize(writer);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
