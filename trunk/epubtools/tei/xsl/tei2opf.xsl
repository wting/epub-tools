<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:tei="http://www.tei-c.org/ns/1.0"
    xmlns:opf="http://www.idpf.org/2007/opf"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">

  <xsl:import href="epub-common.xsl" />
  <xsl:output method="xml" encoding="utf-8" omit-xml-declaration="no" indent="yes"/>

    <xsl:template match="tei:TEI">
      <opf:package unique-identifier="bookid" version="2.0">
        <opf:metadata>
          <dc:title><xsl:apply-templates select="//tei:titleStmt/tei:title" /></dc:title>
          <dc:creator><xsl:apply-templates select="//tei:titleStmt/tei:author" /></dc:creator>
          <dc:language xsi:type="dcterms:RFC3066">en-US</dc:language> 
          <dc:rights>Public Domain</dc:rights>
          <dc:publisher>threepress.org</dc:publisher>
          <dc:identifier id="bookid">urn:uuid:<xsl:value-of select="/tei:TEI/@xml:id"/></dc:identifier>
        </opf:metadata>
        <opf:manifest>
          <opf:item id="ncx" href="toc.ncx" media-type="text/xml"/>
          <opf:item id="style" href="stylesheet.css" media-type="text/css"/>

          <xsl:choose>
            <!-- If we have parts, we want to call them first and nest their subchapters 'inside' -->
            <xsl:when test="//tei:div[@type='part']">
              <xsl:apply-templates select="//tei:div[@type='part']" mode="item"/>
            </xsl:when>
            <xsl:otherwise>
              <!-- Otherwise just get all the flattened chapters -->
              <xsl:apply-templates select="//tei:div[@type='chapter']" mode="item"/>
            </xsl:otherwise>
          </xsl:choose>

        </opf:manifest>


        <opf:spine toc="ncx">
          <xsl:choose>
            <!-- If we have parts, we want to call them first and nest their subchapters 'inside' -->
            <xsl:when test="//tei:div[@type='part']">
              <xsl:apply-templates select="//tei:div[@type='part']" mode="spine"/>
            </xsl:when>
            <xsl:otherwise>
              <!-- Otherwise just get all the flattened chapters -->
              <xsl:apply-templates select="//tei:div[@type='chapter']" mode="spine"/>
            </xsl:otherwise>
          </xsl:choose>
        </opf:spine>

      </opf:package>
    </xsl:template>

    <!-- Generate parts and their subchapters -->
    <xsl:template match="tei:div[@type='part']" mode="item">
      <xsl:call-template name="create-item" />

      <xsl:apply-templates select="tei:div[@type='chapter']" mode="item" />
    </xsl:template>

    <!-- Generate a chapter -->
    <xsl:template match="tei:div[@type='chapter']" mode="item">
      <xsl:call-template name="create-item" />
    </xsl:template>

    <!-- Generate parts and their subchapters -->    
    <xsl:template match="tei:div[@type='part']" mode="spine">
      <xsl:call-template name="create-spine" />

      <xsl:apply-templates select="tei:div[@type='chapter']" mode="spine" />
    </xsl:template>

    <!-- Generate a chapter -->
    <xsl:template match="tei:div[@type='chapter']" mode="spine">
      <xsl:call-template name="create-spine" />
    </xsl:template>

    <!-- Create an OPF item -->
    <xsl:template name="create-item">
      <xsl:variable name="chapter-name">
        <xsl:call-template name="chapter-name" />
      </xsl:variable>
      <xsl:variable name="chapter-file">
        <xsl:call-template name="chapter-file" />
      </xsl:variable>

      <opf:item id="{$chapter-name}" href="{$chapter-file}" media-type="application/xhtml+xml" />
    </xsl:template>

    <!-- Create an OPF spine -->
    <xsl:template name="create-spine">
      <xsl:variable name="chapter-name">
        <xsl:call-template name="chapter-name" />
      </xsl:variable>
      <opf:itemref idref="{$chapter-name}" />
    </xsl:template>

</xsl:stylesheet>