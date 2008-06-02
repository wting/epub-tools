<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns="http://www.daisy.org/z3986/2005/ncx/"
    version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:tei="http://www.tei-c.org/ns/1.0"
    exclude-result-prefixes="tei"
    >
  <xsl:import href="epub-common.xsl" />
  <xsl:output method="xml" doctype-public="-//NISO//DTD ncx 2005-1//EN"
              doctype-system="http://www.daisy.org/z3986/2005/ncx-2005-1.dtd" 
              />

  <xsl:template match="/">
    <ncx version="2005-1">
      <head>
        <meta name="dtb:uid" content="{/tei:TEI/@xml:id}"/>
        <meta name="dtb:depth" content="1"/>
        <!-- TODO; should this be included if it is not DTBook content? -->
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
      </head>      
      <docTitle>
        <text><xsl:apply-templates select="//tei:titleStmt/tei:title" /></text>
      </docTitle>
      <navMap>
        <xsl:choose>
          <!-- If we have parts, we want to call them first and nest their subchapters 'inside' -->
          <xsl:when test="//tei:div[@type='part']">
            <xsl:apply-templates select="//tei:div[@type='part']" />
          </xsl:when>
          <xsl:otherwise>
            <!-- Otherwise just get all the flattened chapters -->
            <xsl:apply-templates select="//tei:div[@type='chapter']" />
          </xsl:otherwise>
        </xsl:choose>
      </navMap>
    </ncx>
  </xsl:template>

  <!-- For each part, render itself and call its subchapters -->
  <xsl:template match="tei:div[@type='part']">
    <xsl:call-template name="create-navpoint" />
    <xsl:apply-templates select="tei:div[@type='chapter']" />
  </xsl:template>

  <xsl:template match="tei:div[@type='chapter']">
    <xsl:call-template name="create-navpoint" />
  </xsl:template>

  <xsl:template name="create-navpoint">
    
    <xsl:variable name="chapter-file">
      <xsl:call-template name="chapter-file" />
    </xsl:variable>

    <xsl:variable name="postemp">
      <xsl:value-of select="./@xml:id" />
    </xsl:variable>

    <xsl:variable name="abspos">
      <xsl:for-each select="//tei:div[@type='chapter' or @type='part']">
        <xsl:if test="./@xml:id=$postemp">
          <xsl:value-of select="position()" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <navPoint id="{concat('navpoint-', $abspos)}" playOrder="{$abspos}">
      <navLabel>
        <text><xsl:apply-templates select="(tei:head)[1]" /></text>
      </navLabel>
      <content src="{$chapter-file}" />
    </navPoint>
  </xsl:template>

  <!-- A bug in Digital Editions means that if multiple tei:heads exist for a given chapter and TEI
   generates a line break between them, that line break will appear in the spine, causing a 
   rendering problem. -->
  <xsl:template match="tei:head">
    <xsl:value-of select="normalize-space(.)" />
    <xsl:if test="following-sibling::*[self=tei:head]">
      <xsl:text>: </xsl:text>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>