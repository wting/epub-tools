#!/usr/bin/env python
import os, os.path, sys, logging, shutil, subprocess
from lxml import etree

import settings

def create_html(directory, tree):
    '''Generate the HTML files that make up each chapter in the TEI document.'''
    xslt = etree.parse(settings.TEI2XHTML_XSLT)

    transform = etree.XSLT(xslt)
    for (i, element) in enumerate(tree.xpath('//tei:div[@type="%s"]' % settings.TEI_DIV_TYPE, namespaces={'tei': settings.TEI})):
        processed = transform(element, xhtml="'true'", generateParagraphIDs="'true'")
        f = '%s/%s/chapter-%d.html' % (directory, settings.OEBPS, i + 1)
        _output_html(f, processed) 

    # Create the title page
    _output_html('%s/%s/title_page.html' % (directory, settings.OEBPS), '<p>Title page</p>', False)

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
    logging.debug('Outputting file %s' % f)
    content = open(f, 'w')    
    content.write(html)
    content.close()

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
    logging.debug('Outputting file %s' % f)
    content = open(f, 'w')
    content.write(etree.tostring(xml, encoding='utf-8', pretty_print=True, xml_declaration=True))
    content.close()

def create_mimetype(directory):
    '''Create the mimetype file'''
    f = '%s/%s' % (directory, settings.MIMETYPE)
    logging.debug('Creating mimetype file %s' % f)
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
    logging.debug('Creating container file %s' % f)
    f = open(f, 'w')
    f.write(settings.CONTAINER_CONTENTS)
    f.close()

def create_stylesheet(directory):
    '''Create the stylesheet file'''
    f = '%s/%s/%s' % (directory, settings.OEBPS, settings.CSS_STYLESHEET)
    logging.debug('Creating CSS file %s' % f)
    f = open(f, 'w')
    f.write(settings.STYLESHEET_CONTENTS)
    f.close()

def create_package(directory):
    '''Archive the entire package using our zip utility'''
    epub_file = '%s.epub' % directory
    
    subprocess.check_call([settings.ZIP, settings.ZIP_ARGS, epub_file, settings.MIMETYPE, settings.META, settings.OEBPS], cwd=directory)
    shutil.move(epub_file, settings.DIST)

def validate(directory):
    '''Validate this using epubcheck'''
    output = directory.replace(settings.BUILD, settings.DIST)
    os.system('%s -jar %s %s.epub' % (settings.JAVA, settings.EPUBCHECK, output))
        
                  
def main(*args):
    '''Create an epub-format zip file given a source XML file.
       Based on the tutorial from: http://www.jedisaber.com/eBooks/tutorial.asp
    '''

    if len(args) < 2:
        print 'Usage: create-epub.py tei-source-file [alternate output directory]'
        return 1

    source = args[1]

    if len(args) > 2:
        directory = '%s/%s' % (settings.BUILD, args[2])
    else:
        if not '.xml' in source:
            logging.error('Source file must have a .xml extension')
            return 1
        directory = '%s/%s' % (settings.BUILD, os.path.basename(source).replace('.xml', ''))

    tree = etree.parse(source)

    if not os.path.exists(settings.BUILD):
        os.mkdir(settings.BUILD)

    if not os.path.exists(settings.DIST):
        os.mkdir(settings.DIST)

    if os.path.exists(directory):
        logging.debug('Removing previous output directory %s' % directory)
        shutil.rmtree(directory)

    logging.debug('Creating directory %s' % directory)
    os.mkdir(directory)

    # Create the epub content
    create_folders(directory)
    create_mimetype(directory)
    create_container(directory)
    create_stylesheet(directory)
    create_navmap(directory, tree)
    create_content(directory, tree)
    create_html(directory, tree)

    # Create the epub format
    create_package(directory)

    # Validate
    if settings.VALIDATE:
        return validate(directory)

    logging.warn('Skipping validation step')
    return 0



if __name__ == '__main__':
    sys.exit(main(*sys.argv))
