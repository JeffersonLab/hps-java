#!/usr/bin/env python

"""
Utility classes for automatically extracting meta data from LCIO, EVIO and ROOT files.

In order for these classes to work, some setup needs to be done first ...

LCIO must have been compiled and installed with ROOT dictionary support enabled.

The following environment must be setup for the LCIO and ROOT Python bindings to work:

    export PYTHONPATH=$LCIO/src/python/:$ROOTSYS/lib
    export LCIO=$LCIO/build

There is no Python binding for EVIO, so an external Java class is used from the hps-data-cat module
to extract the meta data.

The jar containing this class must be added to the classpath:

    export CLASSPATH=/path/to/hps/java/sandbox/data-cat/target/hps-datacat-0.1-SNAPSHOT-bin.jar    

The correct 'java' binary must also be present in the shell environment of the Python interpreter.
"""

import sys, os, ast
from util import *

suppress_print()
from pyLCIO import IOIMPL
restore_print()

import ROOT

# Java class used to extract information from EVIO files
evio_extractor_java_class = "org.hps.datacat.EvioMetaDataPythonTool"

"""
Base class for extracting meta data from a file.
"""    
class MetaDataExtractor:
        
    def extract_metadata(self, file_path):
        pass
    
    def handles_extension(self):
        pass
    
    def get_metadata(self):
        return self.metadata
    
    def to_define_string(self):
        #print self.metadata
        define_string = ''
        for key, value in self.metadata.items():
            define_string += ' --define %s=\"%s\"' % (key, value)
        return define_string
    
"""
Extract meta data from an LCIO file.
"""
# TODO: add min and max event numbers by reading through whole file
class LcioMetaDataExtractor(MetaDataExtractor):
        
    def extract_metadata(self, file_path):
        self.metadata = {}
        reader = IOIMPL.LCFactory.getInstance().createLCReader()
        reader.open(file_path)
        runNumber = None
        detectorName = None
        collectionNames = []
        for event in reader:
            runNumber = event.getRunNumber()
            detectorName = event.getDetectorName()
            for collectionName, collection in event:
                #print '\t%s of type %s with %d elements' % ( collectionName, collection.getTypeName(), collection.getNumberOfElements() )
                collectionNames.append(collectionName)
            break        
        reader.close()
        self.metadata['nRun'] = runNumber
        self.metadata['sDetectorName'] = detectorName
        self.metadata['sCollectionNames'] = ",".join(collectionNames)
        
    def handles_extension(self):
        return 'slcio'
    
"""
Extract meta data from a ROOT DST file.
"""    
class RootDstMetaDataExtractor(MetaDataExtractor):
    
    def extract_metadata(self, file_path):
        self.metadata = {}
        suppress_print()
        root_file = ROOT.TFile(file_path)
        tree = root_file.Get("HPS_Event")
        tree.GetEntry(0)
        run_number = tree.GetLeaf("run_number").GetValue(0)
        restore_print()
        self.metadata['nRun'] = int(run_number)        
        
    def handles_extension(self):
        return 'root'

"""
Extract meta data from an EVIO file.
"""    
class EvioMetaDataExtractor(MetaDataExtractor):
        
    def extract_metadata(self, file_path):
        command_line = 'java %s %s' % (evio_extractor_java_class, file_path)
        lines, errors, return_value = run_process(command_line)
        if len(errors) != 0 or return_value != 0:
            raise Exception("Call to %s failed!" % evio_extractor_java_class)
        line = lines[0]
        self.metadata = ast.literal_eval(line)
        
    def handles_extension(self):
        return 'evio'

# list of meta data extractors    
metadata_extractors = (LcioMetaDataExtractor(), RootDstMetaDataExtractor(), EvioMetaDataExtractor())

# get a meta data extractor for a file
def get_metadata_extractor(file_path):
    file_extension = os.path.splitext(file_path)[1][1:]
    for extractor in metadata_extractors:
        if extractor.handles_extension() == file_extension:
            return extractor
    return None

# when run from command line takes a single argument which is a local file path
if __name__ == '__main__':

    if len(sys.argv) < 2:
        raise Exception("File is required argument!")
    file_path = sys.argv[1]
    
    extractor = get_metadata_extractor(file_path)
    if extractor == None:
        raise Exception("No MetaDataExtractor found for %s file." % file_path)
    extractor.extract_metadata(file_path)
    print "Extracted meta data ..."
    print extractor.get_metadata()
    
    # Uncomment to do tests ...
    
    # test on LCIO file
    #lcioFile = '/nfs/slac/g/hps3/data/datacat-test/data/hps_testrun_001351.slcio'    
    #print 'testing LcioMetaDataExtractor on %s' % lcioFile    
    #extractor = LcioMetaDataExtractor()
    #extractor.extract_metadata(lcioFile)
    #print extractor.get_metadata()
    #print
    
    # test on EVIO file
    #evioFile = '/nfs/slac/g/hps3/data/datacat-test/data/hps_001351.evio'
    #print 'testing EvioMetaDataExtractor on %s' % evioFile
    #extractor = EvioMetaDataExtractor()
    #extractor.extract_metadata(evioFile)
    #print extractor.get_metadata()
    #print
    
    # test on ROOT file
    #rootFile = '/nfs/slac/g/hps3/data/testrun/runs/recon_new/hps_001351.evio.0_recon.root'
    #print 'testing RootDstDataExtractor on %s' % rootFile
    #extractor = RootDstMetaDataExtractor()
    #extractor.extract_metadata(rootFile)
    #print extractor.get_metadata()    
    