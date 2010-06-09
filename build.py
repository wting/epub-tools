#!/usr/bin/env python

import os,sys,logging

from lib import googlecode_upload, settings

logging.basicConfig(level=logging.INFO)

def main(*args):
    if len(args) < 4:
        logging.info("Builds zip and uploads it to Google Code under the epub-tools project.\n\nUsage: build.py <sub-project> <version> <summary>")
        return 1
    project = args[1]
    project = project.replace('/', '') # Strip any path info

    version = args[2]
    package = create_package(project, version)
    
    summary = args[3]

    (http_status, http_reason, file_url) =  googlecode_upload.upload(package, settings.PROJECT_NAME, settings.USER_NAME, settings.PASSWORD, summary, labels=[settings.LABELS[project]])
    if http_status != 201:
        logging.error('File did not upload correctly: %s (%d)' % (http_reason, http_status) )
        return 1
    logging.info('Uploaded file with URL %s ' % file_url)

def create_package(project, version):

    dist = 'dist/%s' % project

    if os.path.exists(dist):
        logging.info('Removing previous build at %s' % dist)
        os.system('rm -rf %s' % dist)
    project_folder = project
    os.system('svn export epubtools/%s %s' % (project_folder, dist))
    os.system('svn export epubtools/epubtools %s/epubtools' % (dist))
    os.chdir('dist')
    filename = '%s-%s.zip' % (project, version)
    os.system('zip -rq %s %s' % (filename, project))
    
    logging.info('Built %s' % filename)
    return filename

if __name__ == '__main__':
    sys.exit(main(*sys.argv))

