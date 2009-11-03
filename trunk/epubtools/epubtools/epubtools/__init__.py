import logging

from lxml import etree
import sys, os, os.path, logging, shutil, zipfile
import epubtools.settings

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
        paths = os.path.split(href)
        current_path = os.path.join(path, 'OEBPS')
        if paths[0] != '':
            p = os.path.join(current_path, paths[0])
            if not os.path.exists(p):
                log.debug("Creating subdir %s in content folder %s" % (paths[0], current_path))
                os.makedirs(p)
            shutil.copy(href, p)
        else:
            if not os.path.exists(os.path.join(current_path, paths[1])):
                log.debug("Copying '%s' into content folder" % paths[1])
                shutil.copy(paths[1], current_path)
    
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

    opf = open(os.path.join('OEBPS', 'content.opf')).read()

    # For the remaining paths in the EPUB, add all of their files using normal ZIP compression
    for dirpath, dirnames, filenames in os.walk('.', topdown=False):
        for name in filenames:
            if name == epub_name:
                continue
            if not name in opf:
                continue
            f = os.path.join(dirpath, name) 
            log.debug("Writing file %s" % f)
            epub.write(f, compress_type=zipfile.ZIP_DEFLATED)

    epub.write('META-INF/container.xml', compress_type=zipfile.ZIP_DEFLATED)
    epub.write('OEBPS/content.opf', compress_type=zipfile.ZIP_DEFLATED)

    epub.close()
    try:
        shutil.move(epub_name, cwd)
    except shutil.Error:
        # In Python 2.6 it's an error if this already exists
        os.remove('%s/%s' % (cwd, epub_name))
        shutil.move(epub_name, cwd)
        
    os.chdir(cwd)
    
    return epub_name

def validate(epub):
    '''Validate this using epubcheck'''
    os.system('%s -jar %s %s' % (settings.JAVA, settings.EPUBCHECK, epub))
