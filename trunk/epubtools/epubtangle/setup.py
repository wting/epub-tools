from setuptools import setup, find_packages
import sys, os

version = '0.0'

setup(name='epubtangle',
      version=version,
      description="EpubTangle combines discrete ePub documents into an anthology",
      long_description="""\
""",
      classifiers=[], # Get strings from http://pypi.python.org/pypi?%3Aaction=list_classifiers
      keywords='epub',
      author='Keith Fahlgren',
      author_email='keith@oreilly.com',
      url='http://code.google.com/p/epub-tools/',
      license='BSD',
      packages=find_packages(exclude=['ez_setup', 
                                      "*.tests", "*.tests.*", "tests.*", "tests"]),
      package_data={
                    'epubtangle.externals': ['README', 'epubcheck', 'epubcheck*/*.*', 'epubcheck*/*/*.*'],
      }
      zip_safe=False,
      install_requires=[
          'lxml>=2.1.2',
      ],
      entry_points="""
      # -*- Entry points: -*-
      """,
      )
