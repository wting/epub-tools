#!/usr/bin/env python
# encoding: utf-8
"""
test_epubcheck.py

Created by Keith Fahlgren on Tue Mar 17 14:01:45 PDT 2009
"""

import logging
import os.path

from nose.tools import *

from epubtangle.epub import Epub
from epubtangle.epubcheck import EpubCheck

log = logging.getLogger(__name__)

class TestEpubCheck:
    def setup(self):
        self.testfiles_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), 'files'))

    def test_epubcheck_sanity(self):
        """EpubCheck objects should be create-able."""
        epubcheck = EpubCheck()
        assert(epubcheck)

    def test_epubcheck_validity(self):
        """Valid ePub documents should pass EpubCheck's validity tests."""
        epubcheck = EpubCheck()
        valid = Epub(os.path.join(self.testfiles_dir, 'valid.epub'))
        assert(epubcheck.is_valid(valid))

    def test_epubcheck_invalidity(self):
        """Invalid ePub documents should not pass EpubCheck's validity tests."""
        epubcheck = EpubCheck()
        invalid = Epub(os.path.join(self.testfiles_dir, 'invalid.epub'))
        assert_false(epubcheck.is_valid(invalid))


