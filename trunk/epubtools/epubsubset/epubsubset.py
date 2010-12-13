#!/usr/bin/env python
import os, os.path, fnmatch
import logging
import zipfile           
import cssutils
import fontforge                  
import re
import shutil
import epubtools as epub
from optparse import OptionParser
from lxml import etree

log = logging.getLogger('epubsubset')

def subset_font(font_file, font_subset):
    '''Create a font subset using the fontforge module.'''
    
    f = fontforge.open(font_file)
    f.selection.select(*font_subset)
    f.selection.invert()
    # Deleting all other glyphs preserves other font information.
    for glyph in f.selection.byGlyphs:
        glyph.clear()
    f.generate(font_file)

def read_epub(epub_file, epub_target = None):
    '''Open an EPUB archive and take a look at its manifest,
        grabbing any stylesheets or content'''

    # Open up epub zip archive 
    epub_zip = zipfile.ZipFile(os.path.abspath(epub_file), 'r')

    # Initialize the new archive
    if not epub_target:
        (root, ext) = os.path.splitext(epub_file)
        new_path = root + ('_subsetted') 
    else:
        (root, ext) = os.path.splitext(epub_target)
        new_path = os.path.abspath(root)

    epub_zip.extractall(new_path)
                     
    for file_name in os.listdir(os.path.join(new_path, 'OEBPS')):
            if fnmatch.fnmatch(file_name, '*.opf'):
                manifest_file = file_name
                break

    opf = open(os.path.join(new_path, 'OEBPS', manifest_file), 'r')
    opf_xml =  etree.parse(opf)

    # A dictionary of all stylesheets and font rules
    fonts = {} 
    # A set of all unique characters    
    chars = set() 
    
    # All the <opf:item> elements are resources.
    for item in opf_xml.xpath('//opf:item',
                              namespaces= { 'opf': 'http://www.idpf.org/2007/opf' }):

        href = item.attrib['href']

        # We're only interested in CSS and xhtml+xml files.
        # Embedded fonts could also be grabbed here.
        if item.attrib['media-type'] == 'text/css':
            css_file = os.path.join(new_path, 'OEBPS', href)
            font_properties = parse_css(css_file)
            # href should be unique, and there can be multiple CSS files.
            fonts[href] = font_properties
        elif item.attrib['media-type'] == 'application/xhtml+xml':
            xhtml_file = open(os.path.join(new_path, 'OEBPS', href), 'r')
            # Ultimately, the file must be parsed for real.
            xhtml_str = xhtml_file.read()
            s = set(xhtml_str)
            chars = chars.union(s)

    # Convert to sequence for passing to fontforge.
    chars = list(chars)
    
    # Do the subsetting.
    for style_file in fonts:
        for rule in fonts[style_file]:
            font_src = re.findall(r'\((.*)\)', rule['src']) 
            font_file = os.path.join(new_path, 'OEBPS', font_src[0])
            subset_font(font_file, chars)

    # Rezip the new EPUB.
    make_epub(new_path)
            
def make_epub(path):            
    '''Make an epub archive using epubtools'''
    epub_archive = epub.create_archive(path)
    log.info("Created epub archive as '%s'" % epub_archive)
    shutil.rmtree(path)

def parse_css(css_file):
    '''Takes a file. Returns @font-face properties.'''
    parser = cssutils.CSSParser()
    sheet = parser.parseFile(css_file)            

    # We're only interested in @font-face rules for now.
    # There can be more than one set of @font-face rules.
    fonts = []
    for rule in sheet:
        if rule.type == rule.FONT_FACE_RULE:
            f = {}
            for property in rule.style:
                f[property.name] = property.value
            fonts.append(f)
    return fonts

if __name__ == '__main__':
    parser = OptionParser(description=' Subset fonts in an EPUB archive.',
                            usage = "%prog source_file.epub [-f TARGET]")

    # Add option for cusom file name
    parser.add_option("-f", "--file", dest="filename",
            help="Write subset EPUB file to FILE", metavar="FILE")

    (options, args) = parser.parse_args()

    if len(args) == 0:
        parser.error("You must supply at least one EPUB file.")
    
    if len(args) == 1:
            for epub_src in args:
                if options.filename:
                    read_epub(epub_src, options.filename)
                else:
                    read_epub(epub_src)
    else:
        print parser.format_help()
        quit()
