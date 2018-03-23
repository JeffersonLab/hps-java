"""
Load datasets from JSON into the datacat.
"""

import os, sys, json, argparse
from datacat import *
from datacat.error import DcException

from multiprocessing import Process

#import logging
#logging.basicConfig(level=logging.DEBUG)

MAX_PROC = 10

ENTRIES = 10

def load(client, entries):
    try:
        for entry in entries:
            #print "Loading entry: " + str(entry)
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
        print (e)

class JSONLoader:

    def __init__(self, json_file=None, config=os.getcwd()+"/default.cfg", start=0, nentries=None, checkpoint=None):
        self.json_file = json_file
        self.config = config
        self.start = start
        self.nentries = nentries
        self.checkpoint = checkpoint

    def parse(self):
        parser = argparse.ArgumentParser("Load entries into datacat from JSON file")
        parser.add_argument("-c", "--config", nargs=1, help="Datacat config file", default=[os.getcwd()+"/default.cfg"])
        parser.add_argument("-s", "--start", nargs=1, type=int, default=0, help="Start index in JSON datacat list")
        parser.add_argument("-n", "--entries", nargs=1, type=int, help="Number of entries to load")   
        parser.add_argument("-p", "--checkpoint", nargs=1, help="File for checkpointing index in JSON file")
        parser.add_argument("json_file", nargs=1, help="JSON file")
        cl = parser.parse_args()

        if cl.json_file is None:
            parser.print_help()
            raise Exception("Name of JSON file is required.")

        self.json_file = cl.json_file[0]
        self.config = cl.config[0]
        self.start = cl.start
        self.nentries = cl.entries
        if cl.checkpoint is not None:
            self.checkpoint = cl.checkpoint[0]
        else:
            self.checkpoint = None

        self.client = client_from_config_file(self.config)

        json_data = open(self.json_file).read()
        self.data = json.loads(json_data)

        if self.nentries is None:
            self.nentries = len(self.data['datacat'])

    def multi_load(self):
        for i in range(0, self.nentries, MAX_PROC*ENTRIES):
            processes = []
            for j in range(i, i + MAX_PROC * ENTRIES, ENTRIES):
                entries = self.data['datacat'][j:j+ENTRIES]
                p = Process(target=load, args=(self.client, entries))
                processes.append(p)
            for p in processes:
                p.start()
            for p in processes:
                p.join()

if __name__ == '__main__':
    loader = JSONLoader()
    loader.parse()
    loader.multi_load()
