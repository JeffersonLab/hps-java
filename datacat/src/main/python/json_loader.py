"""
Load datasets from JSON into the datacat.
"""

import os, sys, json, argparse
from datacat import *
from datacat.error import DcException

class JSONLoader:

    def parse(self):
        parser = argparse.ArgumentParser("Load entries into datacat from JSON file")
        parser.add_argument("-c", "--config", nargs=1, help="Datacat config file", default=os.getcwd()+"/default.cfg")
        parser.add_argument("-s", "--start", nargs=1, type=int, default=0, help="Start index in JSON datacat list")
        parser.add_argument("-n", "--entries", nargs=1, type=int, default=sys.maxint, help="Number of entries to load")   
        parser.add_argument("json_file", nargs=1, help="JSON file")
        cl = parser.parse_args()

        if cl.json_file is None:
            parser.print_help()
            raise Exception("Name of JSON file is required.")

        self.json_file = cl.json_file[0]
        self.config = cl.config[0]
        self.start = cl.start[0]
        self.nentries = cl.entries[0]

    def load(self):

        json_data = open(self.json_file).read()
        data = json.loads(json_data)

        client = client_from_config_file(self.config)

        entries = data['datacat'][self.start:self.start+self.nentries]
        for entry in entries:
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

if __name__ == '__main__':
    loader = JSONLoader()
    loader.parse()
    loader.load()
