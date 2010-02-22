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
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.adobe.dp.epub.io.ContainerWriter;
import com.adobe.dp.epub.io.DataSource;
import com.adobe.dp.epub.ncx.TOCEntry;
import com.adobe.dp.epub.otf.FontEmbeddingReport;
import com.adobe.dp.epub.otf.FontSubsetter;
import com.adobe.dp.epub.util.TOCLevel;
import com.adobe.dp.otf.DefaultFontLocator;
import com.adobe.dp.otf.FontLocator;
import com.adobe.dp.xml.util.SMapImpl;
import com.adobe.dp.xml.util.XMLSerializer;

/**
 * Publication represents an EPUB document. At a minimum, Publication must have
 * OPF resource (that contains metadata, manifest and a spine), NCX resource
 * (that contains table of contents) and some content resources (also called
 * chapters).
 */
public class Publication {

	/**
	 * Dublin Core namespace
	 */
	public static final String dcns = "http://purl.org/dc/elements/1.1/";

	/**
	 * OCF namespace
	 */
	public static final String ocfns = "urn:oasis:names:tc:opendocument:xmlns:container";

	/**
	 * XML encryption namespace
	 */
	public static final String encns = "http://www.w3.org/2001/04/xmlenc#";

	/**
	 * Adobe Digital Edition encoding namespace (for font embedding)
	 */
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

	private byte[] idpfMask;

	private byte[] adobeMask;

	PageMapResource pageMap;

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

	public void splitLargeChapters(int sizeToSplit) {
		for (int i = 0; i < spine.size(); i++) {
			Resource item = (Resource) spine.elementAt(i);
			if (item instanceof OPSResource) {
				OPSResource[] split = ((OPSResource) item).splitLargeChapter(this, sizeToSplit);
				if (split != null) {
					spine.remove(i);
					for (int j = 0; j < split.length; j++) {
						spine.insertElementAt(split[j], i);
						i++;
					}
					i--;
				}
			}
		}
	}

	public void splitLargeChapters() {
		splitLargeChapters(100000);
	}

	public void generateTOCFromHeadings(int depth) {
		TOCEntry entry = getTOC().getRootTOCEntry();
		entry.removeAll();
		Iterator spine = spine();
		Stack headings = new Stack();
		headings.push(new TOCLevel(0, entry));
		while (spine.hasNext()) {
			Resource r = (Resource) spine.next();
			if (r instanceof OPSResource)
				((OPSResource) r).generateTOCFromHeadings(headings, depth);
		}
	}

	public void generateTOCFromHeadings() {
		generateTOCFromHeadings(6);
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

	/**
	 * Get table-of-content resource. Each Publication has one.
	 * 
	 * @return table-of-content resource
	 */
	public NCXResource getTOC() {
		return toc;
	}

	/**
	 * Get OPF resource. Each Publication has one. OPF resource lists other
	 * resources in the Publication, defines their types and determines chapter
	 * order.
	 * 
	 * @return OPF resource
	 */
	public OPFResource getOPF() {
		return opf;
	}

	/**
	 * Get Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed and this method returns only the first one.
	 * 
	 * @param name
	 *            type of Dublin Core metadata, such as "creator" or "title"
	 * @return metadata element value; null if no such value exists
	 */
	public String getDCMetadata(String name) {
		return getMetadata(dcns, name, 0);
	}

	/**
	 * Add Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed.
	 * 
	 * @param name
	 *            type of Dublin Core metadata, such as "creator" or "title"
	 * @param value
	 *            metadata element value
	 */
	public void addDCMetadata(String name, String value) {
		if (value != null) {
			addMetadata(dcns, name, value);
		}
	}

	/**
	 * Get metadata value. Note that multiple metadata values of the same type
	 * are allowed. This method can be used to iterate over
	 * 
	 * @param ns
	 *            metadata element namespace, i.e Publication.dcns
	 * @param name
	 *            type of the metadata element
	 * @return metadata element value; null if no such value exists
	 */
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

	/**
	 * Add Dublin Core metadata value. Note that multiple metadata values of the
	 * same type are allowed.
	 * 
	 * @param ns
	 *            metadata element namespace, i.e Publication.dcns
	 * @param name
	 *            type of the metadata element
	 * @param value
	 *            metadata element value
	 */
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

	/**
	 * Return Publication unique identifier; create one (in the form
	 * "urn:uuid:UUID") if it does not exist
	 * 
	 * @return unique identifier
	 */
	public String getPrimaryIdentifier() {
		Iterator it = metadata.iterator();
		while (it.hasNext()) {
			SimpleMetadata item = (SimpleMetadata) it.next();
			if (item.ns != null && item.ns.equals(dcns) && item.name.equals("identifier"))
				return item.value;
		}
		return generateRandomIdentifier();
	}

	/**
	 * Create a unique resource name using baseName as a template. If baseName
	 * looks like "name.foo", "name.foo" name will be tried first. If it already
	 * exists, names like "name-1.foo", "name-2.foo" will be tried until unused
	 * resource name is found.
	 * 
	 * @param baseName
	 *            desired resource name
	 * @return unique resource name based on the desired one
	 */
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

	/**
	 * Create a new XHTML OPS resource and insert it into this Publication (but
	 * not spine!).
	 * 
	 * @param name
	 *            OPS resource name
	 * @return new OPSResource
	 */
	public OPSResource createOPSResource(String name) {
		return createOPSResource(name, "application/xhtml+xml");
	}

	public OPSResource createOPSResource(String name, String mediaType) {
		OPSResource resource = new OPSResource(name, mediaType);
		resourcesByName.put(name, resource);
		return resource;
	}

	public void removeResource(Resource r) {
		resourcesByName.remove(r.name);
	}

	public void renameResource(Resource r, String newName) {
		resourcesByName.remove(r.name);
		r.name = newName;
		resourcesByName.put(newName, r);
	}

	/**
	 * Create new bitmap image resource and insert it into this Publication.
	 * 
	 * @param name
	 *            resource name
	 * @param mediaType
	 *            resource MIME type, i.g. "image/jpeg"
	 * @param data
	 *            resource data
	 * @return new BitmapImageResource
	 */
	public BitmapImageResource createBitmapImageResource(String name, String mediaType, DataSource data) {
		BitmapImageResource resource = new BitmapImageResource(name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new CSS resource and insert it into this Publication
	 * 
	 * @param name
	 *            resource name
	 * @return new StyleResource
	 */
	public StyleResource createStyleResource(String name) {
		StyleResource resource = new StyleResource(name);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new generic resource and insert it into this Publication.
	 * 
	 * @param name
	 *            resource name
	 * @param mediaType
	 *            resource MIME type
	 * @param data
	 *            resource data
	 * @return new Resource
	 */
	public Resource createResource(String name, String mediaType, DataSource data) {
		Resource resource = new Resource(name, mediaType, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Create new embedded font resource and insert it into this Publication.
	 * Font resources differ from genertic binary resources in that they get
	 * "mangled" during serialization.
	 * 
	 * @param name
	 *            resource name
	 * @param data
	 *            resource data
	 * @return new FontResource
	 */
	public FontResource createFontResource(String name, DataSource data) {
		FontResource resource = new FontResource(this, name, data);
		resourcesByName.put(name, resource);
		return resource;
	}

	/**
	 * Add another resource to the spine (chapter list)
	 * 
	 * @param resource
	 *            resource to add; must be one of the existing OPS resources in
	 *            this Publication
	 */
	public void addToSpine(Resource resource) {
		spine.add(resource);
	}

	/**
	 * Iterate all resources in this Publication.
	 * 
	 * @return resource iterator
	 */
	public Iterator resources() {
		return resourcesByName.values().iterator();
	}

	/**
	 * Iterate resources in the spine (chapter list).
	 * 
	 * @return spine iterator
	 */
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

	public void usePageMap() {
		if (pageMap == null) {
			String name = makeUniqueResourceName("OPS/pageMap.xml");
			pageMap = new PageMapResource(this, name);
			resourcesByName.put(name, pageMap);
		}
	}

	/**
	 * Add embedded font resources to this Publication and add corresponding
	 * &#064;font-face rules for the embedded fonts. Fonts are taken from the
	 * default font locator (see {@link DefaultFontLocator.getInstance}).
	 * 
	 * @param styleResource
	 *            style resource where &#064;font-face rules will be added
	 */
	public FontEmbeddingReport addFonts(StyleResource styleResource) {
		return addFonts(styleResource, DefaultFontLocator.getInstance());
	}

	/**
	 * Add embedded font resources to this Publication and add corresponding
	 * &#064;font-face rules for the embedded fonts. Fonts are taken from the
	 * supplied font locator.
	 * 
	 * @param styleResource
	 *            style resource where &#064;font-face rules will be added
	 * @param fontLocator
	 *            FontLocator object to lookup font files
	 */
	public FontEmbeddingReport addFonts(StyleResource styleResource, FontLocator locator) {
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
		return subsetter;
	}

	static String strip(String source) {
		return source.replaceAll("\\s+", "");
	}

	/*
	 * This starts with the "unique-identifier", strips the whitespace, and
	 * applies SHA1 hash giving a 20 byte key that we can apply to the font
	 * file.
	 * 
	 * See:
	 * http://www.openebook.org/doc_library/informationaldocs/FontManglingSpec
	 * .html
	 */
	byte[] makeIDPFXORMask() {
		if (idpfMask == null) {
			try {
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				String temp = strip(getPrimaryIdentifier());
				sha.update(temp.getBytes("UTF-8"), 0, temp.length());
				idpfMask = sha.digest();
			} catch (NoSuchAlgorithmException e) {
				System.err.println("No such Algorithm (really, did I misspell SHA-1?");
				System.err.println(e.toString());
				return null;
			} catch (IOException e) {
				System.err.println("IO Exception. check out mask.write...");
				System.err.println(e.toString());
				return null;
			}
		}
		return idpfMask;
	}

	byte[] makeAdobeXORMask() {
		if (adobeMask != null)
			return adobeMask;
		ByteArrayOutputStream mask = new ByteArrayOutputStream();
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
		adobeMask = mask.toByteArray();
		return adobeMask;
	}

	/**
	 * Serialize Publication into a container such as OCF container or folder.
	 * 
	 * @param container
	 *            container writing interface
	 * @throws IOException
	 *             if I/O error occurs while writing
	 */
	public void serialize(ContainerWriter container) throws IOException {
		getPrimaryIdentifier(); // if no unique id, make one
		toc.prepareTOC();
		Iterator spine = spine();
		int playOrder = 0;
		while (spine.hasNext()) {
			Object sp = spine.next();
			if (sp instanceof OPSResource) {
				playOrder = ((OPSResource) sp).getDocument().assignPlayOrder(playOrder);
			}
		}
		Enumeration names = resourcesByName.keys();
		boolean needEnc = false;
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if (res instanceof FontResource) {
				needEnc = true;
			}
			OutputStream out = container.getOutputStream(name, res.canCompress());
			res.serialize(out);
		}
		if (needEnc) {
			XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/encryption.xml"));
			ser.startDocument("1.0", "UTF-8");
			ser.startElement(ocfns, "encryption", null, true);
			ser.newLine();
			names = resourcesByName.keys();
			while (names.hasMoreElements()) {
				String name = (String) names.nextElement();
				Resource res = (Resource) resourcesByName.get(name);
				if (res instanceof FontResource) {
					SMapImpl attrs = new SMapImpl();
					ser.startElement(encns, "EncryptedData", null, true);
					ser.newLine();
					if (useIDPFFontMangling)
						attrs.put(null, "Algorithm", "http://www.idpf.org/2008/embedding");
					else
						attrs.put(null, "Algorithm", "http://ns.adobe.com/pdf/enc#RC");
					ser.startElement(encns, "EncryptionMethod", attrs, false);
					ser.endElement(encns, "EncryptionMethod");
					ser.newLine();
					ser.startElement(encns, "CipherData", null, false);
					ser.newLine();
					attrs = new SMapImpl();
					attrs.put(null, "URI", name);
					ser.startElement(encns, "CipherReference", attrs, false);
					ser.endElement(encns, "CipherReference");
					ser.newLine();
					ser.endElement(encns, "CipherData");
					ser.newLine();
					ser.endElement(encns, "EncryptedData");
					ser.newLine();
				}
			}
			ser.endElement(ocfns, "encryption");
			ser.newLine();
			ser.endDocument();
		}
		XMLSerializer ser = new XMLSerializer(container.getOutputStream("META-INF/container.xml"));
		ser.startDocument("1.0", "UTF-8");
		SMapImpl attrs = new SMapImpl();
		attrs.put(null, "version", "1.0");
		ser.startElement(ocfns, "container", attrs, true);
		ser.newLine();
		ser.startElement(ocfns, "rootfiles", null, false);
		ser.newLine();
		attrs = new SMapImpl();
		attrs.put(null, "full-path", opf.name);
		attrs.put(null, "media-type", opf.mediaType);
		ser.startElement(ocfns, "rootfile", attrs, true);
		ser.endElement(ocfns, "rootfile");
		ser.newLine();
		ser.endElement(ocfns, "rootfiles");
		ser.newLine();
		ser.endElement(ocfns, "container");
		ser.newLine();
		ser.endDocument();
		container.close();
	}
	
	public void cascadeStyles() {
		Enumeration names = resourcesByName.keys();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if( res instanceof OPSResource ) {
				((OPSResource)res).getDocument().cascadeStyles();
			}
		}		
	}
	
	public void generateStyles(StyleResource styleResource) {
		Enumeration names = resourcesByName.keys();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			Resource res = (Resource) resourcesByName.get(name);
			if( res instanceof OPSResource ) {
				((OPSResource)res).getDocument().generateStyles(styleResource.getStylesheet());
			}
		}		
	}
}
