#!/usr/bin/env python

import os, sys, json

nrecords = 1000
start = 0

datafile = 'data.json'
if len(sys.argv) > 1:
    datafile = sys.argv[1]

basename,ext = os.path.splitext(datafile)

json_data = open(datafile).read()
data = json.loads(json_data)

file_num=0
for r in range(0, len(data['datacat']), nrecords):
    records = data['datacat'][r:r+nrecords]
    datadict = {'datacat':[]}
    datadict['datacat'].append(records)
    outfile = basename + "_" + str(file_num).zfill(2) + ".json"
    print "Writing '%s'" % outfile
    with open(outfile, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=True)
    file_num += 1
