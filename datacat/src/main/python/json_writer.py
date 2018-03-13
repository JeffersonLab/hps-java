import os, argparse, json

class Builder:

    def build(self, path):
        datadict = {}
        components = path.split('/')
        if path.startswith("/mss/hallb/hps"):
            components = components[4:]
        else:
            raise Exception("Bad path: " + path)

        name = components[-1]
        datadict['name'] = name
        datadict['resource'] = path
       
        return datadict, components

class LCIOBuilder(Builder):

    def build(self, path):
        datadict,components = Builder.build(self, path)
        datadict['data_format'] = 'LCIO'
        metadata = {}
        if components[0] == 'production':
            if 'slic' in components:
                datadict['data_type'] = 'SIM'
                loc = components.index('slic')
            elif 'recon' in components:
                datadict['data_type'] = 'SIM_RECON'
                loc = components.index('recon')
            elif 'readout' in components:
                datadict['data_type'] = 'SIM_READOUT'
                loc = components.index('readout')
            else:
                raise Exception("Unknown data type for path: " + path)

            # detector
            name = datadict["name"]
            det_name = name[name.find("HPS-"):]
            det_end = det_name.find("_")
            if "_globalAlign" in det_name:
                det_end = det_name.find("_globalAlign") + 12
            elif "-fieldmap" in det_name:
                det_end = det_name.find("-fieldmap") + 9
            metadata['DETECTOR'] = det_name[:det_end]

            # physics event type
            event_type = components[loc+1]
            metadata['EVENT_TYPE'] = event_type

            # run tag
            run_tag = ''
            for rt in ['engrun2015', 'physrun2016']:
                if rt.lower() in path.lower():
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

        datadict['metadata'] = metadata

        return datadict,components 

class JSONWriter:

    BUILDERS = { '.slcio': LCIOBuilder() }

    def parse(self):
        parser = argparse.ArgumentParser("Write datacat JSON from a list of files")
        parser.add_argument("-o", "--output", nargs=1, help="If set then write the output to the specified file, otherwise print the results")
        parser.add_argument("-v", "--verbose", action="store_true")
        parser.add_argument("-f", "--folder", nargs=1, help="Target root folder in the datacat", required=True)
        parser.add_argument("-s", "--site", nargs=1, help="Site of dataset (default is 'JLAB')", default=["JLAB"])
        parser.add_argument("-x", "--strip", nargs=1, help="Strip dir from resource when making folder")
        parser.add_argument("filelist", nargs="?", help="Text files containing datacat files", action="append", default=[])
        cl = parser.parse_args()

        if not len(cl.filelist):
            parser.print_help()
            raise Exception("At least one file list is required.")

        self.output = cl.output[0]
        self.verbose = cl.verbose
        self.folder = cl.folder[0]
        self.site = cl.site[0]
        if cl.strip is not None:
            self.strip_dirs = cl.strip
        else:
            self.strip_dirs = []

        self.filelists = cl.filelist

    def run(self):
        self.datacat = {'datacat': []} 
        for filelist in self.filelists:
            with open(filelist, 'r') as f:
                files = f.readlines()
            files = [line.strip() for line in files] 
        for f in files:
            fname,ext = os.path.splitext(f)
            if fname.endswith('.evio'):
                ext = '.evio'
            builder = JSONWriter.BUILDERS[ext]            
            datadict,components = builder.build(f)
            datadict['folder'] = self.get_folder(datadict['resource'])
            datadict['site'] = self.site
            self.datacat['datacat'].append(datadict)

    def get_folder(self, path):
        f = os.path.dirname(path)
        for s in self.strip_dirs:
            f = f.replace(s, '')
        f = self.folder + "/" + f
        return f

    def write_results(self):
        with open(self.output, 'w') as f:
            json.dump(self.datacat, f, indent=2, ensure_ascii=True)

    def print_results(self):
        parsed = json.loads(self.datacat)
        json.dumps(parsed, indent=2, sort_keys=True)

if __name__ == "__main__":
    writer = JSONWriter()
    writer.parse()
    writer.run()
    if writer.output:
        writer.write_results()
    else:
        writer.print_results()    
