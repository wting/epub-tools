#!/usr/bin/env python
from lxml import etree
import os
import os.path
import logging
import shutil
import docbook2epub.settings as settings
import epubtools as epub
from optparse import OptionParser

log = logging.getLogger('docbook2epub')

xslt_ac = etree.XSLTAccessControl(read_file=True,write_file=True, create_dir=True, read_network=True, write_network=False)

def convert_docbook(docbook_file, xsl, css=None):
    '''Use DocBook XSL to transform our DocBook book into EPUB'''

    xml = etree.parse(os.path.abspath(docbook_file))
    cwd = os.getcwd()
    
    xsl = os.path.abspath(xsl)

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

    if css:
        kw['html.stylesheet'] = "'%s'" % css
    
    # Do XInclude parsing
    xml.xinclude()    

    transform = etree.XSLT(etree.parse(xsl), access_control=xslt_ac)
    transform(xml, **kw)

    os.chdir(cwd)

    # Return the working directory for the EPUB
    return output_path

def convert(docbook_file, xsl, css=None):
    path = convert_docbook(docbook_file, xsl, css)
    epub.find_resources(path)
    epub.create_mimetype(path)
    epub_archive = epub.create_archive(path)

    log.info("Created epub archive as '%s'" % epub_archive)

    # Validate
    if settings.VALIDATE:
        return epub.validate(epub_archive )

    # Clean up the output directory
    shutil.rmtree(path)



if __name__ == '__main__':
    # Convert any DocBook xml files passed in as arguments

    parser = OptionParser(usage = "%prog docbook1.xml [docbook2.xml]... --xsl [DocBook XSL or customization] --css [css file]",
                          version="1.0.2")

    # Add option for XSL override
    parser.add_option("--xsl",
                      action="store",
                      type="string",
                      dest="xsl")

    # Add option for path to CSS override
    parser.add_option("--css",
                      action="store",
                      type="string",
                      dest="css")

    (options, args) = parser.parse_args()
    if len(args) == 0:
        parser.error("You must supply at least one DocBook XML file")

    if not options.xsl and not DOCBOOK_XSL:
        parser.error("You must either supply a default DOCBOOK_XSL value in setitngs.py, or provide a path to the --xsl argument")

    for db_file in args:
        convert(db_file, options.xsl or DOCBOOK_XSL, options.css)
