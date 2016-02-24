#!/usr/bin/env python

"""
Insert files into datacat using previously written .metadata files or allow updating
the metadata of an existing dataset if the "-u" option is specified.

The dataset's resource directory and target folder in the datacat must be provided explicitly
with command line arguments. 

author: Jeremy McCormick, SLAC
"""

import os, sys, glob, argparse

from datacat import *
from datacat.error import DcException
from datacat.model import Dataset

# assumes datacat config is in current working dir
client = client_from_config_file(path=os.getcwd() + '/default.cfg')

def get_data_format(name):
    if name.endswith('.aida'):
        return 'AIDA'
    elif name.endswith('.slcio'):
        return 'LCIO'
    elif name.endswith('.root'):
        return 'ROOT' 
    elif '.evio' in os.path.basename(name):
        return 'EVIO'
    raise Exception('Failed to get data format for %s' % name)

def get_data_type(name, format):
    if format == 'EVIO':
        return 'RAW'
    elif format == 'LCIO' and '_recon' in name:
        return 'RECON'
    elif format == 'ROOT' and '_dst' in name:
        return 'DST'
    elif format == 'ROOT' and '_dqm' in name:
        return 'DQM'
    elif format == 'AIDA' and '_dqm' in name:
        return 'DQM'    
    raise Exception('Failed to get data type for %s' % name)

# define CL options
parser = argparse.ArgumentParser(description='insert or update datasets from metadata files')
parser.add_argument('--basedir', '-b', dest='basedir', nargs=1, help='base dir containing metadata files to read', required = True)
parser.add_argument('--folder', '-f', dest='folder', nargs=1, help='target folder in the datacat',  required = True)
parser.add_argument('--resource', '-r', dest='resource', nargs=1, help='actual directory of the files', required = True)
parser.add_argument('--site', '-s', dest='site', nargs=1, help='datacat site (default JLAB)', default='JLAB') # TODO: default to dir of .metadata file
parser.add_argument('--update', '-u', dest='update', help='allow updates to metadata of existing files', action='store_true')
args = parser.parse_args()

basedir = args.basedir[0]
folder = args.folder[0]
if args.resource[0] is not None:
    resource = args.resource[0]
else:
    resource = basedir
site = args.site[0]
allow_update = args.update

metadata_files = glob.glob(basedir + '/*.metadata')

if len(metadata_files) == 0:
    raise Exception("No metadata files found in %s dir." % basedir) 
        
for metadata_file in metadata_files:
    
    metadata = eval(open(metadata_file).read())
    if not isinstance(metadata, dict):
        raise Exception("Input metadata from %s is not a dict." % metadata_file)
    
    locationExtras = {}
    for k, v in metadata.iteritems():
        if k != 'versionMetadata':
           locationExtras[k] = v
        else:
            versionMetadata = v

    if versionMetadata is None:
        versionmetadata = {}

    # TODO: check for empty metadata here (should have some)

    name = os.path.basename(metadata_file).replace('.metadata', '')
    data_format = get_data_format(name)
    data_type = get_data_type(name, data_format)
    
    print "adding dataset ..."
    print "folder = %s" % folder
    print "name = %s" % name
    print "data_format = %s" % data_format
    print "data_type = %s" % data_type
    print "site = %s" % site
    print "resource = %s" % (resource + '/' + name)
    print "versionMetadata = " + repr(versionMetadata)
    print "locationExtras = " + repr(locationExtras)
    print
    
    dataset_exists = False    
    try:
        p = client.path("%s/%s" % (folder, name))
        if isinstance(p, Dataset):
           dataset_exists = True 
    except DcException:
        pass
    
    if not dataset_exists:
        print "Creating new dataset for %s ..." % name
        try:
            client.mkds(folder,
                        name,
                        data_type,
                        data_format, 
                        site=site, 
                        resource=resource + '/' + name,
                        versionMetadata=versionMetadata,
                        locationExtras=locationExtras)
            print "%s was added successfully." % name
        except DcException as e:
            print 'Insert of %s failed!' % name
            print repr(e)
    else:
        if allow_update:
            print "Updating metadata on existing dataset %s ..." % name
            try:
                if metadata['checksum'] is not None:
                    del metadata['checksum']
                client.patchds(folder + '/' + name, metadata)
            except DcException as e:
                print "Update of %s failed!" % name
                print repr(e)                
        else:
            raise Exception("Dataset already exists and updates are not allowed.")                
