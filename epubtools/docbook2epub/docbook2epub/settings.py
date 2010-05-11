import logging

logging.basicConfig(level=logging.DEBUG)

MIMETYPE = 'mimetype'
MIMETYPE_CONTENT = 'application/epub+zip'
COVER_IS_LINEAR = True
VALIDATE = True

# If present, this can be the default location of your DocBook XSL.  Often
# there will be a customization layer which can be provided via the --xsl
# option on the command line.
DOCBOOK_XSL = None

