import logging

from lxml import etree
import sys, os, os.path, logging, shutil, zipfile
import settings

log = logging.getLogger('epubtools.epub')

def find_resources(path):
    '''Parse the content manifest to find all the resources in this book.'''
    opf = etree.parse(os.path.join(path, 'OEBPS', 'content.opf'))
    # All the <opf:item> elements are resources
    for item in opf.xpath('//opf:item', 
                          namespaces= { 'opf': 'http://www.idpf.org/2007/opf' }):

        # If the resource was not already created by DocBook XSL itself, 
        # copy it into the OEBPS folder
        href = item.attrib['href']
        referenced_file = os.path.join(path, 'OEBPS', href)
        if not os.path.exists(referenced_file):
            log.debug("Copying '%s' into content folder" % href)
            shutil.copy(href, '%s/OEBPS' % path)
    
def create_mimetype(path):
    '''Create the mimetype file'''
    f = os.path.join(path, settings.MIMETYPE)
    f = open(f, 'w')
    f.write(settings.MIMETYPE_CONTENT)
    f.close()

def create_archive(path):
    '''Create the ZIP archive.  The mimetype must be the first file in the archive 
    and it must not be compressed.'''
    cwd = os.getcwd()

    epub_name = '%s.epub' % os.path.basename(path)

    # The EPUB must contain the META-INF and mimetype files at the root, so 
    # we'll create the archive in the working directory first and move it later
    os.chdir(path)    

    # Open a new zipfile for writing
    epub = zipfile.ZipFile(epub_name, 'w')

    # Add the mimetype file first and set it to be uncompressed
    epub.write(settings.MIMETYPE, compress_type=zipfile.ZIP_STORED)
    
    # For the remaining paths in the EPUB, add all of their files using normal ZIP compression
    for p in os.listdir('.'):
        if os.path.isdir(p):
            for f in os.listdir(p):
                log.debug("Writing file '%s/%s'" % (p, f))
                epub.write(os.path.join(p, f), compress_type=zipfile.ZIP_DEFLATED)
    epub.close()
    shutil.move(epub_name, cwd)
    os.chdir(cwd)
    
    return epub_name

def validate(epub):
    '''Validate this using epubcheck'''
    os.system('%s -jar %s %s' % (settings.JAVA, settings.EPUBCHECK, epub))
