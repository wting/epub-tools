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

# Configuration specific to your TEI types (div1 = top-level division, div2 = 2nd level division)
# @todo Note that if you modify these values you will need to make corresponding changes in the XSL
#       as well; they are not yet hooked up 
TEI_DIV1_TYPE = 'part'
TEI_DIV2_TYPE = 'chapter'

# TEI P5 version 5.9 is included in the distribution but could be replaced
# with a different version (e.g. p4)
TEI2XHTML_XSLT = '%s/../xsl/tei/tei-xsl-5.12/p5/xhtml/tei.xsl' % path

# @todo Change this to read in from a file
STYLESHEET_CONTENTS = open('%s/../sample/sample.css' % path).read()

# _______________________________________________________________________
# You should not have to change any items below this as they are standard
# OPF filenames

# Name of our OPF mimetype file
MIMETYPE = 'mimetype'
MIMETYPE_CONTENT = 'application/epub+zip'

CSS_STYLESHEET = 'stylesheet.css'

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

