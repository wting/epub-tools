#!/usr/bin/env python
# encoding: utf-8
"""
epubcheck.py

Created by Keith Fahlgren on Tue Sep  9 13:40:59 PDT 2008
"""

import commands
import logging
import os.path

log = logging.getLogger(__name__)

class EpubCheck:
    """A class that manages checking the validity of Epub objects."""
    EPUBCHECK_COMMAND = os.path.abspath(os.path.join(os.path.dirname(__file__), 'externals', 'epubcheck'))

    def is_valid(self, epub):
        """Tests the validity of the provided Epub."""
        return self._check_validity(epub)

    def _check_validity(self, epub):
        checking_cmd = '%s %s' % (self.EPUBCHECK_COMMAND, epub.filename)
        status, output = commands.getstatusoutput(checking_cmd)
        if status == 0:
            log.info("Epubcheck output: %s" % output)
            return True
        else:
            log.warn("Epubcheck output: %s" % output)
            return False

