#!/usr/bin/env python
import subprocess,os,re,sys

__HPSDCPATH='/HPS'
__HPSMSSPATH='/mss/hallb/hps'

__SSH='ssh hpscat@rhel6-64d.slac.stanford.edu'
__DATACAT='~srs/datacat/prod/datacat-hps'
__DCSRCHOPTS='--search-groups --show-non-ok-locations'

__RUNMIN=2000
__RUNMAX=4000

__ALLRUNS=[]
__ALLMETADATA=[]
__ALLMETADATANAMES=[]

__RAWFILENAMEFORMAT='hps_%.6d.evio.%d'


##################################
# CATALOG STRUCTURE (terminal directories are "GROUPS")
#HPS
#- -EngRun2014
#- - - - RAW
#- - - - pass0
#- - - - - - RECON
#- - - - - - DST
#- - - - - - DQM_AIDA
#- - - - - - DQM_ROOT
#- - - - pass1
#- - - - - - RECON
#- - - - - - ....
##################################

def GetRunList(filelist):
  runlist=[]
  for xx in filelist:
    runno=GetRunNumber(xx)[0]
    if not runno in runlist:
      runlist.append(runno)
  return runlist


# THIS NEEDS FIXING FOR OTHER TYPES (e.g. DQM):
def GetGroup(filename):
  dt=GetDataType(filename)
  if dt==None:
    sys.exit('Bad Group:  '+filename)
  elif dt[1]=='EVIO':
    return 'RAW'
  elif dt[1]=='SLCIO':
    return 'RECON'
  elif dt[1]=='TEST':
    return 'DST'
  else:
    sys.exit('GetGroup:  Not Ready for this DataType:  '+filename)

# THESE WILL HAVE TO CHANGE FOR THE NEXT RUN
# SHOULD DO IT AUTOMATICALLY BASED ON RUN NUMBER
def GetDCRunPeriod(filepath):
#  runno=GetRunNumber(filepath)
  return 'engrun2014'
def GetMSSRunPeriod(filepath):
  return 'engrun'

# ONLY READY FOR FILES AT JLAB, UPDATE FOR SLAC:
def GetDCPath(filepath):
  if re.match(__HPSMSSPATH,filepath)==None:
    sys.exit('GetDCPath:  Must start with '+__HPSMSSPATH+':  '+filepath)
  if not os.path.isdir(filepath):
    sys.exit('Directory does not exist:  '+filepath)
  filepath=filepath.lstrip(__HPSMSSPATH).lstrip('/').rstrip('/')
  prefix=__HPSDCPATH+'/'+GetDCRunPeriod(filepath)
  if filepath=='data':
    return prefix
  else:
    subdirs=filepath.split('/')
    subdirs.reverse()
    if subdirs.pop().find('engrun')>=0:
      npass=GetPass(filepath)
      if npass==None:
        sys.exit('GetDCPath:  Unresolved Pass:  '+filepath)
      return prefix+'/pass%d'%(npass)
  sys.exit('GetDCPath: Not ready for this:  '+filepath)

# UPDATE THIS IF WE GET NEW DATATYPES AVAILABLE IN THE CATALOG:
# Returns [fileformat,datatype] REQUIRED by the catalog
def GetDataType(filename):
  if re.search('evio',os.path.basename(filename))!=None:
    return ['evio','EVIO']
  elif re.search('slcio',os.path.basename(filename))!=None:
    return ['slcio','SLCIO']
  elif re.search('dst',os.path.basename(filename))!=None:
    return ['root','TEST']
  else:
    return ['unspecified','TEST']

def GetPass(filename):
  match=re.search('pass\d+',filename)
  if match==None:
    return None
  return int(match.string[match.start()+4:match.end()])

def GetSite(filepath):
  if re.match('/mss',filepath)!=None:
    return 'JLAB'
  else:
    return 'SLAC'

def ListDataCatalog(dclistcmd):
  print 'Getting List of Cataloged Files with ',dclistcmd
  ll=subprocess.Popen(dclistcmd,shell=True,stdout=subprocess.PIPE)
  ll=[ff.rstrip() for ff in ll.stdout]
  ll.sort()
  return ll

def ListRealFiles(path):
  print 'Getting List of Real Files at ',path
  ll=[]
  for (dpath, dnames, fnames) in os.walk(path):
    ll=[dpath+'/'+xx.rstrip() for xx in fnames]
  ll.sort()
  return ll

def GetRunNumber(string):
  # since filename convention is different for raw and recon files, a hack:
  # assume the first integer between __RUNMIN and __RUNMAX is the run number
  # assume the first integer after that is the file number
  [runno,filno]=[-1,-1]
  integers=re.findall('\d+',os.path.basename(string))
  for ii in range(len(integers)):
    if integers[ii].lstrip('0')=='':
      continue
    xx=int(integers[ii].lstrip('0'))
    if xx>__RUNMIN and xx<__RUNMAX:
      runno=xx
      if ii<len(integers)-1:
        if integers[ii+1].lstrip('0')=='':
          filno=0
        else:
          filno=int(integers[ii+1].lstrip('0'))
      break
  return [runno,filno]

def AppendMetaDataVarNames(mtd):
  for key in mtd.keys():
    if not key in __ALLMETADATANAMES:
      __ALLMETADATANAMES.append(key)

def SortMetadataNamesForCatalog(metadatanames):
  mtdn=metadatanames
  mtdn.sort()
  first=['Run','FileNumber','Trigger','Description',
         'BeamCurrent','Target','Nevents']
  first.reverse()
  for xx in first:
    if not xx in mtdn:
      continue
    mtdn.remove(xx)
    mtdn.insert(0,xx)
  return mtdn

def SortMetadataNamesForTable(metadatanames):
  mtdn=metadatanames
  mtdn.sort()
  first=['Run','FileNumber']
  last=['Trigger','Description']
  first.reverse()
  for xx in first:
    if not xx in mtdn:
      continue
    mtdn.remove(xx)
    mtdn.insert(0,xx)
  for xx in last:
    if not xx in mtdn:
      continue
    mtdn.remove(xx)
    mtdn.append(xx)
  return mtdn

def DumpTable():
  mtdn=SortMetadataNamesForTable(__ALLMETADATANAMES)
  for key in mtdn:
    print '%100s ,'%(key),
  print
  for xx in __ALLMETADATA:
    for key in mtdn:
      if xx.has_key(key):
        print '%100s ,'%(str(xx[key]).replace(',','/')),
      else:
        print '%100s ,'%'',
    print

def DumpNames():
  for key in __ALLMETADATANAMES:
    print key

#def GetDCOpts(filepath):
#  return ' --site '+GetSite(filepath)
#
#def GetDCRegisterCmd(filepath):
#  return ERU.__SSH+' '+ERU.__DATACAT+' find '+
#         GetDCOpts(filepath)+
#
#def GetDCListCmd(filepath):



#def CloneMetadata(runno,fileno,mdtvarfile):
#  mtd={}
#  if len(__ALLMETADATANAMES)==0:
#    if not os.path.isfile(mdtvarfile):
#      sys.exit('Missing Metadata File:  '+mdtvarfile)
#    for xx in open(mdtvarfile):
#      xx=xx.split()
#      if len(xx)>0:
#        __ALLMETADATANAMES.append(xx.pop(0))
#    if len(__ALLMETADATANAMES)==0:
#      sys.exit('CloneMetaData Failed.')
#  stub=__RAWFILENAMEFORMAT%(runno,fileno)
#  dcmdtcmd=__SSH+' '+__DCMDTCMD+' find --site JLAB --group HPS --recurse --show-non-ok-locations'
#  ll=subprocess.Popen(dcmdtcmd,shell=True,stdout=subprocess.PIPE)
#  for ff in ll.stdout:
#    print ff
#  return mtd

