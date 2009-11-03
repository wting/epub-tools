#!/usr/bin/env python
import os, os.path, sys, logging, shutil, subprocess
from lxml import etree
import settings

sys.path.append('../..')
sys.path.append('..')
import epubtools


log = logging.getLogger('tei2epub')

def create_html(directory, tree):
    '''Generate the HTML files that make up each chapter in the TEI document.'''
    xslt = etree.parse(settings.TEI2XHTML_XSLT)

    transform = etree.XSLT(xslt)

    _enumerate_div_type(directory, transform, tree, settings.TEI_DIV1_TYPE, 'part', generate_title_page=True)
    _enumerate_div_type(directory, transform, tree, settings.TEI_DIV2_TYPE, 'chapter')

def _enumerate_div_type(directory, transform, tree, div_type, div_name, generate_title_page = False):
    '''Accepts a directory path, an XSLT transform object, an XML tree to operate on,
       the div/@type value to split on,
       the label for the div_name, and an optional parameter to generate a title page containing the
       div's head rather than rendering content (for use in pages like Part or Book breaks.)'''

    for (i, element) in enumerate(tree.xpath('//tei:div[@type="%s"]' % div_type, namespaces={'tei': settings.TEI})):
        f = '%s/%s/%s-%d.html' % (directory, settings.OEBPS, div_name, i + 1)
        if generate_title_page:
           
            processed = "<h1 class='part'>Part %d</h1>" % (i + 1)
            title = element.xpath('(tei:head/text())[1]', namespaces={'tei': settings.TEI})
            if title:
                processed += "<h2 class='title'>%s</h2>" % title[0].encode('utf-8')
            _output_html(f, processed, False)
        else:
            processed = transform(element, xhtml="'true'", generateParagraphIDs="'true'")
            _output_html(f, processed)    

def _output_html(f, content, xml=True):
    if xml:                    
        xslt = etree.parse(settings.HTMLFRAG2HTML_XSLT)        
        processed = content.xslt(xslt)
        html = etree.tostring(processed, encoding='utf-8', pretty_print=True, xml_declaration=False)                    
    else:
        html = '''<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>%s</title>
  </head>
  <body>
%s
  </body>
</html>
''' % ('test', content)
    output = open(f, 'w')    
    output.write(html)
    output.close()

def create_content(directory, tree):
    '''Create the content file based on our TEI source'''
    xslt = etree.parse(settings.TEI2OPF_XSLT)
    processed = tree.xslt(xslt)
    f = '%s/%s/%s' % (directory, settings.OEBPS, settings.CONTENT)
    _output_xml(f, processed)

def create_navmap(directory, tree):
    '''Create the navmap file based on our TEI source'''
    xslt = etree.parse(settings.TEI2NCX_XSLT)
    processed = tree.xslt(xslt)
    f = '%s/%s/%s' % (directory, settings.OEBPS, settings.NAVMAP)
    _output_xml(f, processed)

def _output_xml(f, xml):
    log.debug('Outputting file %s' % f)
    content = open(f, 'w')
    content.write(etree.tostring(xml, encoding='utf-8', pretty_print=True, xml_declaration=True))
    content.close()

def create_mimetype(directory):
    '''Create the mimetype file'''
    f = '%s/%s' % (directory, settings.MIMETYPE)
    log.debug('Creating mimetype file %s' % f)
    f = open(f, 'w')
    f.write(settings.MIMETYPE_CONTENT)
    f.close()

def create_folders(directory):
    '''Create all the top-level directories in our package'''
    for f in settings.FOLDERS:
        d = '%s/%s' % (directory, f)
        if not os.path.exists(d):
            os.mkdir(d)

def create_container(directory):
    '''Create the OPF container file'''
    f = '%s/%s/%s' % (directory, settings.META, settings.CONTAINER)
    log.debug('Creating container file %s' % f)
    f = open(f, 'w')
    f.write(settings.CONTAINER_CONTENTS)
    f.close()

def create_stylesheet(directory):
    '''Create the stylesheet file'''
    f = '%s/%s/%s' % (directory, settings.OEBPS, settings.CSS_STYLESHEET)
    log.debug('Creating CSS file %s' % f)
    f = open(f, 'w')
    f.write(settings.STYLESHEET_CONTENTS)
    f.close()

                 
def convert(*args):
    '''Create an epub-format zip file given a source XML file.
       Based on the tutorial from: http://www.jedisaber.com/eBooks/tutorial.asp
    '''

    if len(args) < 2:
        print 'Usage: create-epub.py tei-source-file [alternate output path]'
        return 1

    source = args[1]

    if len(args) > 2:
        path = '%s/%s' % (settings.BUILD, args[2])
    else:
        if not '.xml' in source:
            log.error('Source file must have a .xml extension')
            return 1
        path = '%s/%s' % (settings.BUILD, os.path.basename(source).replace('.xml', ''))

    tree = etree.parse(source)

    if not os.path.exists(settings.BUILD):
        os.mkdir(settings.BUILD)

    if not os.path.exists(settings.DIST):
        os.mkdir(settings.DIST)

    if os.path.exists(path):
        log.debug('Removing previous output path %s' % path)
        shutil.rmtree(path)

    log.debug('Creating path %s' % path)
    os.mkdir(path)

    # Create the epub content
    create_folders(path)
    create_mimetype(path)
    create_container(path)
    create_stylesheet(path)
    create_navmap(path, tree)
    create_content(path, tree)
    create_html(path, tree)

    # Create the epub format
    epubtools.find_resources(path)
    epubtools.create_mimetype(path)
    epub_archive = epubtools.create_archive(path)

    # Validate
    if settings.VALIDATE:
        return epub.validate(epub_archive)
    log.warn('Skipping validation step')
    return 0

if __name__ == '__main__':
    sys.exit(convert(*sys.argv))
