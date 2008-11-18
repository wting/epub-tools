#!/usr/bin/env python
from lxml import etree
import sys, os, os.path, logging, shutil
import settings

sys.path.append('../..')
import epub

log = logging.getLogger('docbook2epub')

xslt_ac = etree.XSLTAccessControl(read_file=True,write_file=True, create_dir=True, read_network=True, write_network=False)
transform = etree.XSLT(etree.parse(settings.DOCBOOK_XSL), access_control=xslt_ac)

def convert_docbook(docbook_file):
    '''Use DocBook XSL to transform our DocBook book into EPUB'''
    cwd = os.getcwd()
    # Create a temporary working directory for the output files
    output_path = os.path.basename(os.path.splitext(docbook_file)[0])
    if not os.path.exists(output_path):
        os.mkdir(output_path)

    # DocBook needs the source file in the current working directory to output correctly
    shutil.copy(docbook_file, output_path)
    os.chdir(output_path)

    # Call the transform
    kw = {} 
    if settings.COVER_IS_LINEAR:
        kw['epub.cover.linear'] = '1'
    transform(etree.parse(docbook_file),**kw)
    os.chdir(cwd)

    # Return the working directory for the EPUB
    return output_path

def convert(docbook_file):
    path = convert_docbook(docbook_file)
    epub.find_resources(path)
    epub.create_mimetype(path)
    epub_archive = epub.create_archive(path)
    # Validate
    if settings.VALIDATE:
        return epub.validate(path)

    # Clean up the output directory
    shutil.rmtree(path)

    log.info("Created epub archive as '%s'" % epub_archive)

if __name__ == '__main__':
    '''Convert any DocBook xml files passed in as arguments'''
    for db_file in sys.argv[1:]:
        convert(db_file)
