#!/usr/bin/env python                                                                
# encoding: utf-8  


import logging
import os.path
import shutil
import fontforge
import time

from nose.tools import *
from nose.plugins.attrib import attr

from epubsubset import *

log = logging.getLogger(__name__)

class TestEpubSubset:

    FILENAME = 'test-tmp.epub'
    FONTFILE = 'test-tmp/OEBPS/fonts/LinLibertine_Re-4.7.5.otf'

    def setup(self):                                                                 
        self.testfiles_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'files'))

    def teardown(self):
        os.remove(self.FILENAME)                                                              
        pass       

    def test_no_embed(self):
        '''
        An epub file without any embedded fonts shouldn't be changed.
        '''
        epub_file = os.path.join(self.testfiles_dir, 'test-no-embed.epub')  
        epub = EpubSubset(epub_file, self.FILENAME)
        epub.subset()
        new_epub_file = epub.close()
        assert self._file_is_equal(new_epub_file, epub_file)
    
    def test_file_size_reduction(self):
        '''
        An epub file that contains many characters should output a font that is 
        smaller than the source font. This just checks file size.
        '''
        epub_file = os.path.join(self.testfiles_dir, 'test.epub')
        epub = EpubSubset(epub_file, self.FILENAME)
        epub.subset()
        new_epub_file = epub.close()
        assert self._file_is_smaller(new_epub_file, epub_file)
           
    def test_file_ascii_char(self):
        '''
        A file with one ascii character should have one glyph in the subset font
        '''
        epub_file = os.path.join(self.testfiles_dir, 'test-ascii-char.epub')  
        epub = EpubSubset(epub_file, self.FILENAME)
        epub.subset()

        char_count = self._count_font_glyphs(self.FONTFILE)
        epub.close()

        assert char_count == 2

    def test_file_unicode_char(self):
        '''
        A file with one unicode character should have one glyph in the subset font
        '''
        epub_file = os.path.join(self.testfiles_dir, 'test-utf8-char.epub')  
        epub = EpubSubset(epub_file, self.FILENAME)
        epub.subset()

        (root, ext) = os.path.splitext(self.FILENAME)
        char_count = self._count_font_glyphs(self.FONTFILE)
        epub.close()
        print char_count
        assert char_count == 2

    def _count_font_glyphs(self, font_file):
        f = fontforge.open(font_file)
        i = 0
        for glyph in f.glyphs():
            i += 1
        f.close()
        return i


    def _file_is_equal(self, new_file, orig_file):
        return os.path.getsize(new_file) == os.path.getsize(orig_file)

    def _file_is_smaller(self, new_file, orig_file):
        return os.path.getsize(new_file) < os.path.getsize(orig_file) 
