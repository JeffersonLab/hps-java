#!/usr/bin/env python
import subprocess,os,re,sys

__HPSDCPATH='/HPS'
__HPSMSSPATH='/mss/hallb/hps'

__SSH='ssh hpscat@rhel6-64d.slac.stanford.edu'
__DATACAT='~srs/datacat/prod/datacat-hps'
__DCSRCHOPTS='--recurse --search-groups --show-non-ok-locations'

__ALLRUNS=[]
__ALLMETADATA=[]
__ALLMETADATANAMES=[]

__RAWFILENAMEFORMAT='hps_%.6d.evio.%d'

__HPSRUNPERIOD=None

__HPSRUNPERIODS={'simulation':[-9999,100],
                 'testrun2012':[100,2000],
                 'engrun2014':[2000,4000],
                 'engrun2015':[4000,9999]}

def SetRunPeriod(runperiod):
  global __HPSRUNPERIOD
  if not __HPSRUNPERIODS.has_key(runperiod):
    sys.exit('SetRunPeriod:  Missing run period:  '+runperiod)
  __HPSRUNPERIOD=''.join(runperiod)

#
# For Reconstructed Files on Tape/Disk, path must match /pass\d+/.*
# Everything before that will be ignored.
#

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


# Data Catalog Groups (RAW,RECON,DQM,DST,...):
def GetGroup(filename):
  dt=GetDataType(filename)
  if dt==None:
    sys.exit('Bad Group:  '+filename)
  elif dt[1]=='EVIO':
    return 'RAW'
  elif dt[1]=='SLCIO':
    return 'RECON'
  elif filename.lower().endswith('.root'):
    if filename.lower().find('dqm')>=0:
      return 'DQM'
    elif filename.lower().find('dst')>=0:
      return 'DST'
  else:
    sys.exit('GetGroup:  Not Ready for this DataType:  '+filename)


## path stub on tape at JLab:
#def GetMSSRunPeriod(filepath):
#  runno=GetRunNumber(filepath)[0]
#  if runno<10:
#    return 'simulation'
#  if runno<2500:
#    return 'testrun2012'
#  elif runno<4000:
#    return 'engrun'
#  else:
#    return 'engrun2015'

## path stub for the Data Catalog
#def GetDCRunPeriod(filepath):
#  runno=GetRunNumber(filepath)[0]
#  hpsrp=__HPSRUNPERIODS
#  for xx in hpsrp.keys():
#    if runno>=hpsrp[xx][0] and runno<hpsrp[xx][1]:
#      return xx
#  return None

# full path for the Data Catalog:
def GetDCPath(filepath):

  print __HPSRUNPERIOD
  prefix=__HPSDCPATH+'/'+__HPSRUNPERIOD #GetDCRunPeriod(filepath)

  if not os.path.isdir(filepath):
    sys.exit('Directory does not exist:  '+filepath)

  # it's got to be a raw EVIO file:
  if filepath.rstrip().rstrip('/') == __HPSMSSPATH+'/data':
    # fix name for 2014 run:
    if prefix.endswith('engrun2014'):
      return __HPSDCPATH+'/engrun'

  # throw away everything before '/pass#/'
  tmp=re.search('/(pass\d+)/(.*)',filepath)
  if (tmp == None):
    sys.exit('GetDCPath:  Not raw data on tape, Not a pass#:\n'+filepath)

  passN=tmp.group(1)

  # No longer used:
  #pathstub=tmp.group(2).lstrip('/').rstrip('/')
  #subdirs=pathstub.split('/').reverse()
  #if subdirs.pop()=='engrun':
  #  return prefix+'/'+passN
  #sys.exit('GetDCPath: Not ready for this:  '+filepath)

  return prefix+'/'+passN+'/'



# UPDATE THIS IF WE GET NEW DATATYPES AVAILABLE IN THE CATALOG:
# Returns [fileformat,datatype] REQUIRED by the catalog
def GetDataType(filename):
  basename=os.path.basename(filename).lower()
  if basename.find('.evio')>0:
    return ['evio','EVIO']
  elif basename.endswith('.slcio')>0 or basename.endswith('.lcio')>0:
    return ['slcio','SLCIO']
  elif basename.endswith('.root'):
    return ['root','TEST']
  return ['unspecified','TEST']

def GetPass(filename):
  match=re.search('pass\d+',filename)
  if match==None:
    return None
  return int(match.string[match.start()+4:match.end()])

def GetSite(filepath):
  if re.match('/mss',filepath)!=None:
    return 'JLAB'
  elif re.match('/work',filepath)!=None:
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
    if xx>=__HPSRUNPERIODS[__HPSRUNPERIOD][0] and \
       xx< __HPSRUNPERIODS[__HPSRUNPERIOD][1]:
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
         'BeamCurrent','Target','Nevents','RunBegin','RunEnd']
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
  first=['Run','FileNumber','RunBegin','RunEnd']
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

def ConvertSpreadsheetDate(ssdate):
  unix=None
  yy=ssdate.rstrip().lstrip().split(' ')
  if len(yy) == 3:
    date = yy[0]
    time = yy[1]
    ampm = yy[2]
    time = time.split(':')
    date = date.split('/')
    if len(date)==3 and time==2 and (ampm=='AM' or ampm=='PM'):
      month=int(date[0])
      day=int(date[1])
      year=2000+int(date[2])
      hour=int(time[0])
      minu=int(time[1])
      unix=(datetime.datetime(year,month,day,hour,minu)).strftime('%s')
  return unix


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

