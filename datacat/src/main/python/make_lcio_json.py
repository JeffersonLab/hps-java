#!/usr/bin/env python

"""
Creates a JSON file with datacat entries from a list of LCIO files.
"""

import os, sys, json
#hashlib
from datacat import *
from datacat.error import DcException

# target folder in the datacat
TARGET_FOLDER="/HPS/test/mc"

# file where errors are logged
ERROR_LOG = 'err.log'

# paths to exclude
EXCLUDES = ['/mss/hallb/hps/production/PhysicsRun2016/',
        '/mss/hallb/hps/production/postTriSummitFixes/stdhep/wab/1pt05/',
        '/mss/hallb/hps/production/electronGunG4/',
        '/mss/hallb/hps/production/MG_alphaFix/slic/targetz_0pt5mm/beam/2pt3/',
        '/mss/hallb/hps/production/ngraf/',
        '/mss/hallb/hps/production/MG_alphaFix/engrun2015/tpass7/recon/tritrig-pulserData/']
#'/mss/hallb/hps/production/MG_alphaFix/engrun2015/tpass7/',

# run tags
RUN_TAGS = ['engrun2015', 'physrun2016']

def exclude(datafile):
    for exclude in EXCLUDES:
        if exclude in datafile:
            return True
    return False

def make_dict(datafile):
    d = {}
    metadata = {}

    components = datafile.split('/')
    components.pop(0)

    # basic info
    name = components[-1]
    d['name'] = name
    d['folder'] = TARGET_FOLDER
    d['resource'] = datafile
    d['data_format'] = 'LCIO'
    d['site'] = 'JLAB'
    #d['checksum'] = hashlib.md5(open(datafile,'rb').read()).hexdigest()

    # data type and set base dir for subsequent searches
    loc = -1
    if 'slic' in components:
        d['data_type'] = 'SIM'
        loc = components.index('slic')
    elif 'recon' in components:
        d['data_type'] = 'SIM_RECON'
        loc = components.index('recon')
    elif 'readout' in components:
        d['data_type'] = 'SIM_READOUT'
        loc = components.index('readout')
    else:
        raise Exception("Unknown data type.")
    if loc == -1:
        raise Exception("Could not find 'slic', 'recon' or 'readout' in path components.")

    # physics event type
    event_type = components[loc+1]
    metadata['EVENT_TYPE'] = event_type

    # run tag
    run_tag = ''
    for rt in RUN_TAGS:
        if rt.lower() in datafile.lower():
            run_tag = rt
            break
    metadata['RUN_TAG'] = run_tag

    # A-prime mass
    if event_type == 'ap':
        ap_mass = None
        for c in components[loc+3:]:
            try:
                ap_mass = int(c)
                break
            except:
                pass
        if ap_mass is not None:
            metadata['APRIME_MASS'] = ap_mass
        else:
            raise Exception("A-prime mass not found.")

    # beam energy
    beam_val = components[loc+2].replace('pt','.')
    try:
        metadata['BEAM_ENERGY'] = float(beam_val)
    except:
        raise Exception("Invalid value for beam energy: %s" % beam_val)

    # detector
    metadata['DETECTOR'] = name[name.find("HPS-"):name.rfind("_")]

    # file number
    file_val = name[name.rfind("_")+1:name.rfind(".")]
    try:
        metadata['FILE'] = int(file_val)
    except:
        raise Exception("Invalid value for file number: %s" % file_val)

    # extra event info from the front of the file name
    metadata['EVENT_EXTRA'] = name[0:name.find("HPS-")-1]

    # corresponding reconstruction pass
    metadata['PASS'] = ""
    for c in components:
        if 'pass' in c:
            metadata['PASS'] = c
    
    d['metadata'] = metadata
    
    return d

data = {}
data['datacat'] = []

infile = "mc_files_lcio.txt"
if len(sys.argv) > 1:
    infile = sys.argv[1]
else:
    print "Using default input file '%s'" % infile

outfile = "data.json"
if len(sys.argv) > 2:
    outfile = sys.argv[2]
else:
    print "Using default output file '%s'" % outfile

if os.path.exists(outfile):
    try:
        os.remove(outfile)
        print "Removed existing output file '%s'" % outfile
    except:
        raise Exception("Unable to delete existing output file '%s'" % outfile)

if os.path.exists(ERROR_LOG):
    try:
        os.remove(ERROR_LOG)
        print "Removed existing error log '%s'." % ERROR_LOG
    except:
        raise Exception("Unable to delete existing error log '%s'" % ERROR_LOG)

print "Reading file list from '%s' ..." % (infile)

flist = open(infile, 'r').readlines()
print "Read %d files" % len(flist)

num_written = 0
num_errors = 0
num_excludes = 0
error_log = open(ERROR_LOG, 'w')
for i in range(len(flist)):
    datafile = flist[i].strip()
    if exclude(datafile):
        print "Excluding '%s'" % datafile
        num_excludes +=1 
        continue            
    try:
        datadict = make_dict(datafile) 
        data['datacat'].append(datadict)
        num_written += 1
    except Exception as e:
        error_log.write('%s\n' % datafile)
        error_log.write('%s\n' % str(e))
        error_log.write('\n')
        num_errors += 1
        continue
    if i != 0 and i % 1000 == 0:
        print "Processed %d files" % i

print "Writing JSON file ..."
with open(outfile, 'w') as f:
    json.dump(data, f, indent=2, ensure_ascii=True)

print "Wrote '%s' with %d records, %d errors and %d excluded" % (outfile, num_written, num_errors, num_excludes)
