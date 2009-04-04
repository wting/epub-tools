from setuptools import setup, find_packages
import sys, os

version = '1.0.1'

setup(name='docbook2epub',
      version=version,
      description="Converts DocBook 4/5 documents into ePub. Requires the DocBook XSL",
      long_description="""\
""",
      classifiers=[], # Get strings from http://pypi.python.org/pypi?%3Aaction=list_classifiers
      keywords='epub',
      author='Liza Daly',
      author_email='liza@threepress.org',
      url='http://code.google.com/p/epub-tools/',
      license='New BSD',
      scripts=['docbook2epub/bin/db2epub.py'],
      packages=find_packages(exclude=['ez_setup', 
                                      "*.tests", "*.tests.*", "tests.*", "tests"]),
      package_data={
                    'docbook2epub.externals': ['README', 'epubcheck', 'epubcheck*/*.*', 'epubcheck*/*/*.*'],
      },
      zip_safe=False,
      install_requires=[
          'lxml>=2.1.2',
          'epubtools'
      ],
      entry_points="""
      # -*- Entry points: -*-
      """,
      )
