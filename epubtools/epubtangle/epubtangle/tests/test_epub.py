#!/usr/bin/env python
# encoding: utf-8
"""
test_epub.py

Created by Keith Fahlgren on Tue Mar 17 14:01:45 PDT 2009
"""

import logging
import os.path

from nose.tools import *

from epubtangle.epub import Epub

log = logging.getLogger(__name__)

class TestEpub:
    def setup(self):
        self.testfiles_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'files'))

    def test_epub_sanity(self):
        """Epub objects should be create-able."""
        valid = os.path.join(self.testfiles_dir, 'valid.epub')
        epub = Epub(valid)
        assert(epub)

    def test_epub_filename(self):
        """Epub objects should expose their filename."""
        valid = os.path.join(self.testfiles_dir, 'valid.epub')
        epub = Epub(valid)
        assert_equal(valid, epub.filename)
