#!/usr/bin/env python
# encoding: utf-8
"""
epub.py

Created by Keith Fahlgren on Tue Mar 17 20:55:01 PDT 2009
"""

import logging

log = logging.getLogger(__name__)

class Epub:
    """A class that manages ePub documents."""
    def __init__(self, filename):
        self.filename = filename
