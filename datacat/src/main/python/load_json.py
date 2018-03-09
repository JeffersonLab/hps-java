#!/usr/bin/env python

"""
Load datasets from JSON into the datacat.
"""

import os, sys, json
from datacat import *
from datacat.error import DcException

DEBUG = False

datafile = 'data.json'
if len(sys.argv) > 1:
    datafile = sys.argv[1]

json_data = open(datafile).read()
data = json.loads(json_data)

client = client_from_config_file(path=os.getcwd() + "/default.cfg")

for entry in data['datacat']:
    try: 
        ds = client.mkds(
                entry['folder'],
                entry['name'],
                entry['data_type'],
                entry['data_format'],
                site=entry['site'],
                resource=entry['resource'],
                versionMetadata=entry['metadata'])
        print(repr(ds))
    except Exception as e:
        print e
    if DEBUG:
        break
