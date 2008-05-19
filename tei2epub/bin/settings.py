from os.path import realpath, dirname
import sys, logging

# Raise or lower this setting according to the amount of debugging
# output you would like to see.  See http://docs.python.org/lib/module-logging.html
logging.basicConfig(level=logging.WARN)

# Run epubcheck after each build? (Recommended)
VALIDATE = True

# Path of the executable
path = realpath(dirname(sys.argv[0]))

# XSL templates
XSLT_DIR = '%s/../xsl' % path
TEI2OPF_XSLT = '%s/tei2opf.xsl' % XSLT_DIR
TEI2NCX_XSLT = '%s/tei2ncx.xsl' % XSLT_DIR
HTMLFRAG2HTML_XSLT = '%s/htmlfrag2html.xsl' % XSLT_DIR

# Directory where our output will go
DIST = '%s/../dist' % path

# Working directory
BUILD = '%s/../build' % path

# zip command
ZIP = '/usr/bin/zip'

# Arguments to pass to zip (this will specify the order of the files and not to 
# compress the mimetype file
ZIP_ARGS = '-qXr9D'

# Configuration specific to your TEI type
TEI_DIV_TYPE = 'chapter'

# TEI P5 version 5.9 is included in the distribution but could be replaced
# with a different version (e.g. p4)
TEI2XHTML_XSLT = '%s/../xsl/tei/tei-xsl-5.9/p5/xhtml/tei.xsl' % path

# Our Java executable
JAVA = '/usr/bin/java'

# epubcheck location
EPUBCHECK = '%s/../test/epubcheck.jar' % path


# _______________________________________________________________________
# You should not have to change any items below this as they are standard
# OPF filenames

# Name of our OPF mimetype file
MIMETYPE = 'mimetype'
MIMETYPE_CONTENT = 'application/epub+zip'

CSS_STYLESHEET = 'stylesheet.css'
STYLESHEET_CONTENTS = '''
body { font-family: serif }
'''

META = 'META-INF'

CONTENT = 'content.opf'

NAVMAP = 'toc.ncx'

OEBPS = 'OEBPS'

# Top-level folders in our epub directory
FOLDERS = (META, OEBPS)

CONTAINER = 'container.xml'
CONTAINER_CONTENTS = '''<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
'''

# TEI namespace
TEI = 'http://www.tei-c.org/ns/1.0'

