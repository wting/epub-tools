<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:tei="http://www.tei-c.org/ns/1.0"
    xmlns:opf="http://www.idpf.org/2007/opf"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">

  
  <!-- Expects a tei:div and returns its absolute position in the document 
       (different from position(), which may be taking nesting into consideration) -->
  <xsl:template name="absolute-position">
    <xsl:variable name="temp">
      <xsl:value-of select="./@xml:id" />    
    </xsl:variable>
    
    <xsl:for-each select="//tei:div[@type='chapter']">
      <xsl:if test="./@xml:id=$temp">
        <xsl:value-of select="position()" />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- Expects a tei:div node -->
  <xsl:template name="chapter-name">

    <xsl:variable name="absolute-position">
      <xsl:choose>
        <xsl:when test="@type='part'">
          <xsl:value-of select="position()" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="absolute-position" /> 
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:value-of select="concat(@type, '-', $absolute-position)" />
  </xsl:template>


  <!-- Expects a tei:div node -->  
  <xsl:template name="chapter-file">
    <xsl:variable name="chapter-name">
      <xsl:call-template name="chapter-name" />
    </xsl:variable>
    <xsl:value-of select="concat($chapter-name, '.html')" />
  </xsl:template>


</xsl:stylesheet>