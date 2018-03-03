#!/usr/bin/env python

import os, sys, json
from datacat import *
from datacat.error import DcException

datafile = 'data.json'
if len(sys.argv) > 1:
    datafile = sys.argv[1]

json_data = open(datafile).read()
data = json.loads(json_data)

client = client_from_config_file(path=os.getcwd() + "/default.cfg")

for entry in data['datacat']:
    ds = client.mkds(
            entry['folder'],
            entry['name'],
            entry['data_type'],
            entry['data_format'],
            site=entry['site'],
            resource=entry['resource'],
            versionMetadata=entry['metadata'])
#,
#            locationExtras={"runMin": 0, "runMax": 0, "eventCount": 0, "scanStatus": "UNSCANNED", "checksum": 0})
    print(repr(ds))
    break # DEBUG
