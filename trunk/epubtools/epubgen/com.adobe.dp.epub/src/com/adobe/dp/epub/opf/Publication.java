/*******************************************************************************
 * Copyright (c) 2009, Adobe Systems Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * ·        Redistributions of source code must retain the above copyright 
 *          notice, this list of conditions and the following disclaimer. 
 *
 * ·        Redistributions in binary form must reproduce the above copyright 
 *		   notice, this list of conditions and the following disclaimer in the
 *		   documentation and/or other materials provided with the distribution. 
 *
 * ·        Neither the name of Adobe Systems Incorporated nor the names of its 
 *		   contributors may be used to endorse or promote products derived from
 *		   this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.adobe.dp.epub.opf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.adobe.dp.epub.io.ContainerWriter;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.otf.FontSubsetter;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

public class Publication {

	public static final String dcns = "http://purl.org/dc/elements/1.1/";

	public static final String ocfns = "urn:oasis:names:tc:opendocument:xmlns:container";

	public static final String encns = "http://www.w3.org/2001/04/xmlenc#";

	public static final String deencns = "http://ns.adobe.com/digitaleditions/enc";

	boolean translit;

	boolean useIDPFFontMangling = true;

	Hashtable resourcesByName = new Hashtable();

	Hashtable resourcesById = new Hashtable();

	Hashtable namingIndices = new Hashtable();

	Vector spine = new Vector();

	Vector metadata = new Vector();

	NCXResource toc;

	OPFResource opf;

	int idCount = 1;

	private String contentFolder;

	static class SimpleMetadata {

		String ns;

		String name;

		String value;

		SimpleMetadata(String ns, String name, String value) {
			this.name = name;
			this.ns = ns;
			this.value = value;
		}
	}

	/**
	 * Create a new empty EPUB document. Content folder is set to "OPS".
	 */
	public Publication() {
		this("OPS");
	}

	/**
	 * Create a new empty EPUB document with the given content folder.
	 */
	public Publication(String contentFolder) {
		this.contentFolder = contentFolder;
		toc = new NCXResource(this, contentFolder + "/toc.ncx");
		opf = new OPFResource(this, contentFolder + "/content.opf");
		resourcesByName.put(toc.name, toc);
		resourcesByName.put(opf.name, opf);
	}

	/**
	 * Create a new empty EPUB document by reading it from a file. This method
	 * is under development and not functional yet.
	 */
	public Publication(File file) throws IOException {
		ZipFile zip = new ZipFile(file);
		ZipEntry container = zip.getEntry("META-INF/container.xml");
		if (container == null)
			throw new IOException("Not an EPUB file: META-INF/container.xml missing");
		String opfName = processOCF(zip.getInputStream(container));
		opf = new OPFResource(this, opfName);
		opf.parse(zip.getInputStream(zip.getEntry(opfName)));
	}

	/**
	 * Return this EPUB package content folder. OPF file is located in that
	 * folder; typically all other content is stored in this folder or its
	 * subfolders as well.
	 */
	public String getContentFolder() {
		return contentFolder;
	}

	/**
	 * Make this EPUB document use Adobe proprietary font mangling method when
	 * serializing. This method works in all versions of Digital Editions and
	 * Adobe Reader Mobile SDK-based devices
	 * 
	 * @see <a href=
	 *      "http://www.adobe.com/devnet/digitalpublishing/pdfs/content_protection.pdf"
	 *      >Adobe&#32;documentation</a>
	 */
	public void useAdobeFontMangling() {
		useIDPFFontMangling = false;
	}

	/**
	 * Make this EPUB document use standard font mangling method when
	 * serializing. This method is not supported in any reading system as of
	 * today (May 2009), but that is expected to change.
	 * 
	 * @see <a href=
	 *      "http://www.openebook.org/doc_library/informationaldocs/FontManglingSpec.html"
	 *      >IDPF&#32;documentation</a>
	 */
	public void useIDPFFontMangling() {
		useIDPFFontMangling = true;
	}

	private static String processOCF(InputStream ocfStream) throws IOException {
		throw new IOException("Not yet implemented");
	}

	/**
	 * Transliterate cyrillic metadata when serializing. This is useful when
	 * formatting books written in cyrillic for reading on non-localized reading
	 * devices. This setting should be avoided.
	 * 
	 * @param translit
	 *            true if cyrillic metadata should be transliterated
	 */
	public void setTranslit(boolean translit) {
		this.translit = translit;
	}

	/**
	 * Determine if cyrillic metadata should be transliterated when serializing.
	 * 
	 * @see #setTranslit
	 * @return true if cyrillic metadata should be transliterated
	 */
	public boolean isTranslit() {
		return translit;
	}

	private static void addRandomHexDigit(StringBuffer sb, Random r, int count, int mask, int value) {
		for (int i = 0; i < count; i++) {
			int v = (r.nextInt(16) & mask) | value;
			if (v < 10)
				sb.append((char) ('0' + v));
			else
				sb.append((char) (('a' - 10) + v));
		}
	}

	/**
	 * Generate random UUID and add it as a publication's identifier (in URN
	 * format, i.e. prefixed with "urn:uuid:")
	 * 
	 * @return generated identifier
	 */
	public String generateRandomIdentifier() {
		// generate v4 UUID
		StringBuffer sb = new StringBuffer("urn:uuid:");
		SecureRandom sr = new SecureRandom();
		addRandomHexDigit(sb, sr, 8, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 4, 0xF, 0);
		sb.append('-');
		sb.append('4');
		addRandomHexDigit(sb, sr, 3, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 1, 0x3, 0x8);
		addRandomHexDigit(sb, sr, 3, 0xF, 0);
		sb.append('-');
		addRandomHexDigit(sb, sr, 12, 0xF, 0);
		String id = sb.toString();
		this.addDCMetadata("identifier", id);
		return id;
	}

	public NCXResource getTOC() {
		return toc;
	}

	public OPFResource getOPF() {
		return opf;
	}

	public String getDCMetadata(String name) {
		return getMetadata(dcns, name, 0);
	}

	public void addDCMetadata(String name, String value) {
		if (value != null) {
			addMetadata(dcns, name, value);
		}
	}

	public String getMetadata(String ns, String name, int index) {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata md = (SimpleMetadata) it.next();
			if ((ns == null ? md.ns == null : md.ns != null && md.ns.equals(ns)) && md.name.equals(name)) {
				if (index == 0)
					return md.value;
				index--;
			}
		}
		return null;
	}

	public void addMetadata(String ns, String name, String value) {
		if (value == null)
			return;
		metadata.add(new SimpleMetadata(ns, name, value));
	}

	private String getAdobePrimaryUUID() {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata item = (SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier")
					&& item.value.startsWith("urn:uuid:"))
				return item.value.substring(9);
		}
		String value = generateRandomIdentifier();
		return value.substring(9);
	}

	public String getPrimaryIdentifier() {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata item = (SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier"))
				return item.value;
		}
		return generateRandomIdentifier();
	}

	public String makeUniqueResourceName(String baseName) {
		if (resourcesByName.get(baseName) == null)
			return baseName;
		int index = baseName.lastIndexOf('.');
		String suffix;
		if (index < 0) {
			suffix = "";
		} else {
			suffix = baseName.substring(index);
			baseName = baseName.substring(0, index);
		}
		Integer lastIndex = (Integer) namingIndices.get(baseName);
		if (lastIndex == null)
			index = 1;
		else
			index = lastIndex.intValue() + 1;
		String name;
		while (true) {
			name = baseName + "-" + index + suffix;
			if (resourcesByName.get(name) == null)
				break;
			index++;
		}
		namingIndices.put(baseName, new Integer(index));
		return name;
	}

	public OPSResource createOPSResource(String name) {
		OPSResource resource = new OPSResource(name);
		resourcesByName.put(name, resource);
		return resource;
	}

	public BitmapImageResource createBitmapImageResource(String name, String mediaType, DataSource data) {
		BitmapImageResource resource = new BitmapImageResource(name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	public StyleResource createStyleResource(String name) {
		StyleResource resource = new StyleResource(name);
		resourcesByName.put(name, resource);
		return resource;
	}

	public Resource createResource(String name, String mediaType, DataSource data) {
		Resource resource = new Resource(name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	public FontResource createFontResource(String name, DataSource data) {
		FontResource resource;
		if (this.useIDPFFontMangling) {
			resource = new IDPFFontResource(name, data);
		} else {
			resource = new AdobeFontResource(name, data);
		}
		resourcesByName.put(name, resource);
		return resource;
	}

	public void addToSpine(Resource resource) {
		spine.add(resource);
	}

	public Iterator resources() {
		return resourcesByName.values().iterator();
	}

	public Iterator spine() {
		return spine.iterator();
	}

	public Resource getResourceByName(String name) {
		return (Resource) resourcesByName.get(name);
	}

	String assignId(Resource res) {
		if (res.id == null) {
			res.id = makeId();
			resourcesById.put(res.id, res);
		}
		return res.id;
	}

	String makeId() {
		while (true) {
			String id = "id" + (idCount++);
			if (resourcesById.get(id) == null)
				return id;
		}
	}

	public void addFonts(StyleResource styleResource) {
		addFonts(styleResource, DefaultFontLocator.getInstance());
	}

	public void addFonts(StyleResource styleResource, FontLocator locator) {
		FontSubsetter subsetter = new FontSubsetter(this, styleResource, locator);
		Iterator res = resources();
		while (res.hasNext()) {
			Object next = res.next();
			if (next instanceof OPSResource) {
				OPSResource ops = (OPSResource) next;
				ops.getDocument().addFonts(subsetter, styleResource);
			}
		}
		subsetter.addFonts(this);
	}

	public static String strip(String source) {
		return source.replaceAll("\\s+", "");
	}

	private byte[] makeXORMask() {
		ByteArrayOutputStream mask = new ByteArrayOutputStream();
		if (useIDPFFontMangling) {
			/*
			 * This starts with the "unique-identifier", strips the whitespace,
			 * and applies SHA1 hash giving a 20 byte key that we can apply to
			 * the font file.
			 * 
			 * See:http://www.openebook.org/doc_library/informationaldocs/
			 * FontManglingSpec.html
			 */
			try {
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				String temp = strip(getPrimaryIdentifier());
				sha.update(temp.getBytes("UTF-8"), 0, temp.length());
				mask.write(sha.digest());
			} catch (NoSuchAlgorithmException e) {
				System.err.println("No such Algorithm (really, did I misspell SHA-1?");
				System.err.println(e.toString());
				return null;
			} catch (IOException e) {
				System.err.println("IO Exception. check out mask.write...");
				System.err.println(e.toString());
				return null;
			}
			if (mask.size() != 20) {
				System.err.println("makeXORMask should give 20 byte mask, but isn't");
				return null;
			}
		} else {
			String opfUID = getAdobePrimaryUUID();
			int acc = 0;
			int len = opfUID.length();
			for (int i = 0; i < len; i++) {
				char c = opfUID.charAt(i);
				int n;
				if ('0' <= c && c <= '9')
					n = c - '0';
				else if ('a' <= c && c <= 'f')
					n = c - ('a' - 10);
				else if ('A' <= c && c <= 'F')
					n = c - ('A' - 10);
				else
					continue;
				if (acc == 0) {
					acc = 0x100 | (n << 4);
				} else {
					mask.write(acc | n);
					acc = 0;
				}
			}
			if (mask.size() != 16)
				return null;
		}
		return mask.toByteArray();
	}

	public void serialize(ContainerWriter container) throws IOException {
		Enumeration names = resourcesByName.keys();
		byte[] mask = makeXORMask();
		boolean needEnc = false;
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if (mask != null && res instanceof AdobeFontResource) {
				((AdobeFontResource) res).setXORMask(mask);
				needEnc = true;
			}
			if (mask != null && res instanceof IDPFFontResource) {
				((IDPFFontResource) res).setXORMask(mask);
				needEnc = true;
			}
			OutputStream out = container.getOutputStream(name, res.canCompress());
			res.serialize(out);
		}
		if (needEnc) {
			XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/encryption.xml"));
			ser.startDocument("1.0", "UTF-8");
			ser.startElement(ocfns, "encryption", null, true);
			names = resourcesByName.keys();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				Resource res = (Resource) resourcesByName.get(name);
				if ((res instanceof FontResource) && (useIDPFFontMangling)) {
					SMapImpl attrs = new SMapImpl();
					ser.startElement(encns, "EncryptedData", null, true);
					attrs.put(null, "Algorithm", "http://www.idpf.org/2008/embedding");
					ser.startElement(encns, "EncryptionMethod", attrs, false);
					ser.endElement(encns, "EncryptionMethod");
					ser.startElement(encns, "CipherData", null, false);
					attrs = new SMapImpl();
					attrs.put(null, "URI", name);
					ser.startElement(encns, "CipherReference", attrs, false);
					ser.endElement(encns, "CipherReference");
					ser.endElement(encns, "CipherData");
					ser.endElement(encns, "EncryptedData");
				} else if (res instanceof FontResource) {
					SMapImpl attrs;
					ser.startElement(encns, "EncryptedData", null, true);
					attrs = new SMapImpl();
					attrs.put(null, "Algorithm", "http://ns.adobe.com/pdf/enc#RC");
					ser.startElement(encns, "EncryptionMethod", attrs, false);
					ser.endElement(encns, "EncryptionMethod");
					ser.startElement(encns, "CipherData", null, false);
					attrs = new SMapImpl();
					attrs.put(null, "URI", name);
					ser.startElement(encns, "CipherReference", attrs, false);
					ser.endElement(encns, "CipherReference");
					ser.endElement(encns, "CipherData");
					ser.endElement(encns, "EncryptedData");
				}
			}
			ser.endElement(ocfns, "encryption");
			ser.endDocument();
		}
		XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/container.xml"));
		ser.startDocument("1.0", "UTF-8");
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "1.0");
		ser.startElement(ocfns, "container", attrs, true);
		ser.startElement(ocfns, "rootfiles", null, false);
		attrs = new SMapImpl();
		attrs.put(null, "full-path", opf.name);
		attrs.put(null, "media-type", opf.mediaType);
		ser.startElement(ocfns, "rootfile", attrs, true);
		ser.endElement(ocfns, "rootfile");
		ser.endElement(ocfns, "rootfiles");
		ser.endElement(ocfns, "container");
		ser.endDocument();
		container.close();
	}
}
