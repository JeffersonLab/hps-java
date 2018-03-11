#!/usr/bin/env python

from datacat import *
import os

client = client_from_config_file(path=os.getcwd() + "/default.cfg")

s = client.search('/HPS/mc')

for d in s:
    print "Deleting '%s'" % d.path
    client.rmds(d.path)
