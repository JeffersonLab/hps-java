"""
Load datasets from JSON into the datacat.
"""

import os, sys, json, argparse
from datacat import *
from datacat.error import DcException

import logging
logging.basicConfig(level=logging.DEBUG)

class JSONLoader:

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

    def load(self):

        json_data = open(self.json_file).read()
        data = json.loads(json_data)

        if self.nentries is None:
            self.nentries = len(data['datacat'])

        print self.config
        client = client_from_config_file(self.config)

        if self.checkpoint:
            checkpoint_file = open(self.checkpoint, 'w')
        else:
            checkpoint_file = None

        for i in range(self.start, self.start + self.nentries, 1):
            entry = data['datacat'][i]
            try: 
                print "Loading entry: " + str(entry)
                ds = client.mkds(
                        entry['folder'],
                        entry['name'],
                        entry['data_type'],
                        entry['data_format'],
                        site=entry['site'],
                        resource=entry['resource'],
                        versionMetadata=entry['metadata'])
                print "Wrote entry %d" % i
                print(repr(ds))
                print
                if self.checkpoint:
                    checkpoint_file.seek(0, 0)
                    checkpoint_file.write(str(i) + '\n')
            except Exception as e:
                print e
        if checkpoint_file:
            checkpoint_file.close()

if __name__ == '__main__':
    loader = JSONLoader()
    loader.parse()
    loader.load()
