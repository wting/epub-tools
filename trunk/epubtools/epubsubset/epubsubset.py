#!/usr/bin/env python
import os, os.path, fnmatch
import logging
import zipfile           
import cssutils
import fontforge                  
import re
import shutil
import epubtools as epubtools
from optparse import OptionParser
from lxml import etree

log = logging.getLogger('epubsubset')


class EpubSubset:

    def __init__(self, epub_file, epub_target = None):
        '''Extract new archive'''            
        epub_zip = zipfile.ZipFile(os.path.abspath(epub_file), 'r')
        self.epub_target = epub_target
        self.font_styles = {}

        # Initialize the new archive
        if not epub_target:
            (root, ext) = os.path.splitext(epub_file)
            self.epub_target = root + ('_subsetted') 
        else:
            (root, ext) = os.path.splitext(epub_target)
            self.epub_target = os.path.abspath(root)

        epub_zip.extractall(self.epub_target)

    def subset(self):
        self.read_manifest()
        self.parse_epub()

    def close(self):
        return self.make_epub()


    def read_manifest(self):
        '''
        Take a look at an EPUB manifest and grab any stylesheets and content.
        '''
        # Manifest could be named anything.
        for file_name in os.listdir(os.path.join(self.epub_target, 'OEBPS')):
                if fnmatch.fnmatch(file_name, '*.opf'):
                    manifest_file = file_name
                    break

        opf = open(os.path.join(self.epub_target, 'OEBPS', manifest_file), 'r')
        opf_tree =  etree.parse(opf)

        css_files = []
        xhtml_files = []
        
        # All the <opf:item> elements are resources.
        for item in opf_tree.xpath('//opf:item',
                                  namespaces= { 'opf': 'http://www.idpf.org/2007/opf' }):

            href = item.attrib['href']

            # We're only interested in CSS and xhtml+xml files.
            # Embedded fonts could also be grabbed here.
            if item.attrib['media-type'] == 'text/css':
                css_files.append(href)
            elif item.attrib['media-type'] == 'application/xhtml+xml':
                xhtml_files.append(href)
                
        # Lists of all CSS and XHTML files listed in the manifest.
        self.css_files = css_files
        self.xhtml_files = xhtml_files

    def parse_epub(self):
        '''Parse through lists from the content manifest and extract useful bits'''
        # A set of all unique characters in all content files.   
        c = set()

        for css_file in self.css_files:
            font_properties = self.parse_css(css_file)
            self.font_styles[css_file] = font_properties
        for xhtml_file in self.xhtml_files:
            s = self.parse_xhtml(xhtml_file)
            c = c.union(s)
        # Convert to sequence for passing to fontforge.
        c = list(c)
        self.process_fonts(c, self.font_styles, self.epub_target)

    def process_fonts(self, chars, font_styles, path):
        '''
        Subset a series of fonts. This will have to modified to handle multiple
        character sets and fonts. At present, all fonts will have the same glyphs.
        '''
        for style_file in font_styles:
            for rule in font_styles[style_file]:
                font_file = os.path.join(path, 'OEBPS', rule['src'])
                self.subset_font(font_file, chars)                               

    def subset_font(self, font_file, font_subset):
        '''
        Create a font subset using the fontforge module. Takes a font and a sequence
        of characters to subset.
        
        '''
        f = fontforge.open(font_file)

        # Lose any whitespace
        font_subset = filter(lambda x: x.isspace() == False, font_subset)

        # Fontforge only takes vanilla strings. Convert utf-8 into 
        # U+0000 format that it takes.
        font_subset = ['U+'+'%04x' % ord(c) for c in font_subset]
        f.selection.select(*font_subset)
        f.selection.invert()
        
        # Deleting all other glyphs preserves other font information.
        for glyph in f.selection.byGlyphs:
            glyph.clear()
        f.generate(font_file)
        f.close()
                
    def make_epub(self):            
        '''Make an epub archive from a directory using epubtools'''
        epub_archive = epubtools.create_archive(self.epub_target)
        log.info("Created epub archive as '%s'" % epub_archive)
        shutil.rmtree(self.epub_target)
        return epub_archive

    def parse_xhtml(self, xhtml_file):
        '''Takes a file. Returns a set of unique text characters from that file.'''
        xhtml_file = os.path.join(self.epub_target, 'OEBPS', xhtml_file)
        
        # Ultimately, the file must be parsed for real.
        parser = etree.XMLParser(remove_blank_text=True)
        xhtml_tree = etree.parse(xhtml_file, parser)
        NAMESPACE = 'http://www.w3.org/1999/xhtml'
        
        # Only interested in the body.
        body = xhtml_tree.find('//{%s}body' % NAMESPACE)
        uni = etree.tostring(body, method="text", encoding='UTF-8')
        uni = uni.decode('utf-8')
        return set(uni)


    def parse_css(self, css_file):
        '''Takes a css file. Returns @font-face properties from that file.'''
        css_file = os.path.join(self.epub_target, 'OEBPS', css_file)
        parser = cssutils.CSSParser()
        sheet = parser.parseFile(css_file)            

        # We're only interested in @font-face rules for now.
        # There can be more than one set of @font-face rules per stylesheet.
        fonts = []
        for rule in sheet:
            if rule.type == rule.FONT_FACE_RULE:
                f = {}
                for property in rule.style:
                    if property.name == 'src':
                        # Get a clean font src.
                        f[property.name] = re.findall(r'\((.*)\)', property.value)[0]
                    else:
                        f[property.name] = property.value
                fonts.append(f)
        return fonts



if __name__ == '__main__':    
    parser = OptionParser(description=' Subset fonts in an EPUB archive.',
                            usage = "%prog source_file.epub [-f TARGET]")

    parser.add_option("-f", "--file", dest="filename",
            help="Write subset EPUB file to FILE", metavar="FILE")

    (options, args) = parser.parse_args()

    if len(args) == 0:
        parser.error("You must supply at least one EPUB file.")
    
    if len(args) == 1:
        for epub_src in args:
            if options.filename:
                epub = EpubSubset(epub_src, options.filename)
            else:
                epub = EpubSubset(epub_src)
            
            epub.subset()
            epub.close()
    else:
        print parser.print_help()
        quit()
