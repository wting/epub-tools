from setuptools import setup, find_packages
import sys, os

version = '1.0.0'

setup(name='epubtools',
      version=version,
      description="Library for common epub file handling",
      long_description="""\
""",
      classifiers=[], # Get strings from http://pypi.python.org/pypi?%3Aaction=list_classifiers
      keywords='epub',
      author='Liza Daly',
      author_email='liza@threepress.org',
      url='http://code.google.com/p/epub-tools/',
      license='New BSD',
      packages=find_packages(exclude=['ez_setup', 
                                      "*.tests", "*.tests.*", "tests.*", "tests"]),
      package_data={
                    'epubtools.externals': ['README', 'epubcheck', 'epubcheck*/*.*', 'epubcheck*/*/*.*'],
      },
      zip_safe=False,
      install_requires=[
          'lxml>=2.1.2',
      ],
      entry_points=""" 
      # -*- Entry points: -*-
      """,
      )
