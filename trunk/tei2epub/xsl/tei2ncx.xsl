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
        <meta name="dtb:totalPageCount" content="0"/>
        <meta name="dtb:maxPageNumber" content="0"/>
      </head>      
      <docTitle>
        <text><xsl:apply-templates select="//tei:titleStmt/tei:title" /></text>
      </docTitle>
      <navMap>
        <navPoint id="navpoint-1" playOrder="1">
          <navLabel>
            <text>Title Page</text>
          </navLabel>
          <content src="title_page.html"/>
        </navPoint>  
        <xsl:apply-templates select="//tei:div[@type='chapter']" />
      </navMap>
    </ncx>
  </xsl:template>

  <xsl:template match="tei:div[@type='chapter']">

    <xsl:variable name="chapter-file">
      <xsl:call-template name="chapter-file" />
    </xsl:variable>
    
    <!-- Navpoint needs to be +1 on the chapter, to account for the title page -->
    <navPoint id="{concat('navpoint-', position() + 1)}" playOrder="{position() + 1}">
      <navLabel>
        <text><xsl:apply-templates select="tei:head" /></text>
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