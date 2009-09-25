package com.adobe.dp.office.conv;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import javax.imageio.ImageIO;

import com.adobe.dp.epub.conv.ConversionClient;
import com.adobe.dp.epub.conv.ConversionService;
import com.adobe.dp.epub.io.OCFContainerWriter;
import com.adobe.dp.epub.opf.Publication;
import com.adobe.dp.epub.util.ConversionTemplate;
import com.adobe.dp.epub.util.Translit;
import com.adobe.dp.office.rtf.RTFDocument;
import com.adobe.dp.otf.ChainedFontLocator;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;

public class RTFConversionService extends ConversionService {

	BufferedImage rtficon;

	boolean embedFonts = true;

	boolean adobeMangling = true;

	boolean translit = true;

	boolean getBooleanProperty(Properties prop, String name, boolean def) {
		String s = prop.getProperty(name);
		if (s == null)
			return def;
		return s.toLowerCase().startsWith("t");
	}

	public RTFConversionService() {
		InputStream png = DOCXConversionService.class.getResourceAsStream("docx.png");
		try {
			rtficon = ImageIO.read(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean canConvert(File src) {
		String name = src.getName().toLowerCase();
		return name.endsWith(".rtf");
	}

	public boolean canUse(File src) {
		return false;
	}

	public File convert(File src, File[] aux, ConversionClient client, PrintWriter log) {
		try {
			RTFDocument doc = new RTFDocument(src);
			Publication epub = new Publication();
			epub.setTranslit(translit);
			epub.useAdobeFontMangling();
			RTFConverter conv = new RTFConverter(doc, epub);
			epub.setTranslit(translit);
			if (adobeMangling)
				epub.useAdobeFontMangling();
			else
				epub.useIDPFFontMangling();
			FontLocator fontLocator = DefaultFontLocator.getInstance();
			if (aux != null && aux.length > 0) {
				ConversionTemplate template = new ConversionTemplate(aux);
				FontLocator customLocator = template.getFontLocator();
				fontLocator = new ChainedFontLocator(customLocator, fontLocator);
			}
			conv.convert();
			if (embedFonts)
				conv.embedFonts(fontLocator);

			String title = epub.getDCMetadata("title");
			String fname;
			if (title == null) {
				fname = src.getName();
				epub.addDCMetadata("title", fname);
				if (fname.endsWith(".rtf"))
					fname = fname.substring(0, fname.length() - 5);
			} else {
				fname = Translit.translit(title).replace(' ', '_').replace('\t', '_').replace('\n', '_').replace('\r',
						'_').replace('/', '_').replace('\\', '_').replace('\"', '_');
			}

			File outFile = client.makeFile(fname + ".epub");
			OutputStream out = new FileOutputStream(outFile);
			OCFContainerWriter container = new OCFContainerWriter(out);
			epub.serialize(container);
			return outFile;
		} catch (Exception e) {
			e.printStackTrace(log);
		}
		return null;
	}

	public Image getIcon(File src) {
		return rtficon;
	}

	public void setProperties(Properties prop) {
		embedFonts = getBooleanProperty(prop, "embedFonts", embedFonts);
		adobeMangling = getBooleanProperty(prop, "adobeMangling", adobeMangling);
		translit = getBooleanProperty(prop, "translit", translit);
	}
}
