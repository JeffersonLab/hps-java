#!/usr/bin/env python
import re,os,sys
import engrun_util as ERU
import engrun_metadata as ERM

USAGE='Usage:  engrun_register.py filepath [outputfile]'

DEBUG=0
#DEBUG=1

SSHREG=0
#SSHREG=1

if len(sys.argv)!=3 and len(sys.argv)!=2:
  sys.exit(USAGE)

FILEPATH=sys.argv[1].rstrip()

OUTFILE=None
if len(sys.argv)==3:
  OUTFILE=sys.argv[2].rstrip()
  if os.path.exists(OUTFILE):
    sys.exit('Output File Already Exists:  '+OUTFILE)
  else:
    OUTFILE=open(OUTFILE, 'w')


# DATA CATALOG PATH MUST ALREADY EXIST (MAKE IT MANUALLY)
DCPATH=ERU.GetDCPath(FILEPATH)

DCOPTS=' --site '+ERU.GetSite(FILEPATH)

# THIS WILL NEED CHANGING TO RUN LOCALLY AT SLAC
# WE CAN CHOOSE WHETHER TO SSH BASED ON FILEPATH, or HOSTNAME (see Jeremy's util.py)
DCLISTCMD=ERU.__SSH+' '+ERU.__DATACAT+' find '+DCOPTS+' '+ERU.__DCSRCHOPTS+' '+DCPATH

DCREGCMD=ERU.__DATACAT+' registerDataset '+DCOPTS

# Get list of files already in the catalog:
DCLIST=ERU.ListDataCatalog(DCLISTCMD)

# Get list of files to register:
FILELIST=ERU.ListRealFiles(FILEPATH)

if len(FILELIST)==0:
  sys.exit('No Files:  '+FILEPATH)


for filename in FILELIST:

  filename=filename.rstrip()

  if filename.find("led")>=0:
    continue

  if filename in DCLIST:
    sys.stderr.write('File already cataloged, Ignoring: '+filename+'\n')
    continue

  [fileformat,datatype]=ERU.GetDataType(filename)
  [runno,filno]=ERU.GetRunNumber(filename)

  if runno<0:
    if DEBUG>1:
      sys.stderr.write('Invalid Run Number for '+filename+'\n')
    continue
  if filno<0:
    if DEBUG>1:
      sys.stderr.write('Invalid File Number for '+filename+'\n')
    continue

  metadata={}
  metadata['Run']=runno
  metadata['FileNumber']=filno

  # Only assignigning full metadata to the raw EVIO files: 
  if fileformat.lower() == 'evio':

    # FOR THE NEXT RUN, JUST USE DAQ and SPREADSHEET, not LOGBOOK
    # Determine metadata (order is crucial):
    ERM.GetMetadataFromDAQ(runno,metadata)
    ERM.GetMetadataFromRunSpreadsheet(runno,metadata)
    ERM.GetMetadataFromLogbook(runno,metadata)

    # remove quotes:
    for xx in metadata.keys():
      if not type(metadata[xx]) is str:
        continue
      metadata[xx]=metadata[xx].replace('\'','prime')

    # remove some useless metadata:
    rm=['SSP_BERR','SSP_HPS_COSMIC_PATTERNCOINCIDENCE','SSP_HPS_PAIRS_CLUSTERDELAY']
    for xx in rm:
      for yy in metadata.keys():
        if yy.find(xx)>=0:
          del metadata[yy]

  # store stuff for later:
  ERU.AppendMetaDataVarNames(metadata)
  if not runno in ERU.__ALLRUNS:
    ERU.__ALLMETADATA.append(metadata)
    ERU.__ALLRUNS.append(runno)

  # ASSIGNING THIS TO THE GROUP, NOT INDIVIDUAL FILES, DO IT MANUALLY FOR NOW
  # add Pass-specific metadata:
  #ERM.GetPassMetadata(filename,metadata)

  if DEBUG>0:
    continue

  # create command-line options for metadata:
  mtdopts=''
  for key in ERU.SortMetadataNamesForCatalog(metadata.keys()):
    if type(metadata[key]) is str:
      mtdopts += ' --define s%s=\"%s\"'%(key,metadata[key])
    elif type(metadata[key]) is int:
      mtdopts += ' --define n%s=%d'%(key,metadata[key])
    elif type(metadata[key]) is float:
      mtdopts += ' --define n%s=%.3f'%(key,metadata[key])
    else:
      sys.exit('Wierd Metadata:  '+metadata[key])

  # more command-line options:
  opts=''
  opts += ' --format '+fileformat
  opts += ' --name '+os.path.basename(filename)
  opts += ' --group '+ERU.GetGroup(filename)

  ############################################################################

  if DEBUG<0:
    if filno!=0: #10 or filno%2!=0 or filno%4!=0:
      continue

  if SSHREG:
    cmd = '%s \'%s %s %s %s %s %s\''%(ERU.__SSH,DCREGCMD,mtdopts,opts,datatype,DCPATH,filename)
  else:
    cmd = '%s %s %s %s %s %s'%(DCREGCMD,mtdopts,opts,datatype,DCPATH,filename)

  if OUTFILE==None:
    print cmd
  else:
    print >> OUTFILE, cmd


if DEBUG!=0:
  ERU.DumpTable()
  ERU.DumpNames()




#ERU.CloneMetadata(3216,0,ERM.__METADATAVARFILENAME)
#DCMKDCMD=ERU.__DATACAT+' mkdir '+ERU.__DCGROUP
#DCMETCMD=ERU.__SSH+' '+ERU.__DATACAT+' find '+DCOPTS+' '+DCSRCHOPTS+' '+ERU.__HPSDCRAWPATH

