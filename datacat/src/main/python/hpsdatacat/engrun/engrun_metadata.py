#!/usr/bin/env python
import os,sys,re
import engrun_util as ERU

# FIXME: MAKE THESE PATHS ABSOLUTE:
__SSFILENAME='./CONFIGINFO/HPS_Runs.csv'
__DAQFILENAMEFORMAT='./CONFIGINFO/EVIODUMPS/hps_%.6d.evio.0.xml'
__METADATAVARFILENAME='./CONFIGINFO/METADATA.txt'

# SPREADSHEET COLUMNS:
__SSCOLUMNS={'run':0,'tgt':5,'cur':6,'x':7,'y':8,'trig':9,
             'fadcmode':10,'fadcthresh':11,'fadcwindow':12,
             'seedthresh':13,'clusterthresh':14,'hitwindow':15,'pairwindow':16,
             'desc1':19,'desc2':20,'nev':3,'runbegin':1,'runend':2}

# DAQ TRIGGER NUMBERING:
__IOSRC={'20':'SINGLES_0_EN','21':'SINGLES_1_EN',
         '22':'PAIRS_0_EN','23':'PAIRS_1_EN',
         '24':'LED_EN','25':'COSMIC_EN',
         '18':'PULSER_EN'}

__LASTCOSMICRUNBEFOREBEAM=3151

__SPREADSHEET=[]
__MISSINGDAQRUNS=[]
__MISSINGSSHEETRUNS=[]


# THIS WILL NEED SOME MINOR UPDATING FOR NEXT RUN
# parse DAQ Configuration bank from xml dump of EVIO files:
def GetMetadataFromDAQ(runno,mtd):

  filepath=__DAQFILENAMEFORMAT%(runno)

  # start with all triggers OFF:
  for xx in __IOSRC.keys():
    mtd['SSP_HPS_'+__IOSRC[xx]]=0

  # missing file:
  if not os.path.isfile(filepath):
    if not runno in __MISSINGDAQRUNS:
      sys.stderr.write('Missing DAQCONFIG For Run '+str(runno)+': '+filepath+'\n')
      __MISSINGDAQRUNS.append(runno)
    return

  prevthresh=0

  for xx in open(filepath):

    xx=xx.split()
    if len(xx)<2:
      continue

    # first column is variable name:
    key=xx.pop(0)

    # is it FADC or SSP config line:
    isADC = 0 if re.match('FADC250',key)==None else 1
    isSSP = 0 if re.match('SSP',key)==None else 1

    if not isSSP and not isADC:
      continue

    if isADC==1:

      # ignore SLOT lines:
      if re.search('SLOT',key)!=None:
        continue

      # may have SVT FADC later, so specify:
      key=key.replace('FADC250','ECALFADC')


    # special case #1, interpret triggers:
    if isSSP and re.match('SSP_HPS_SET_IO_SRC',key)!=None:

      if __IOSRC.has_key(xx[1]):
        mtd['SSP_HPS_'+__IOSRC[xx[1]]]=1

    # special case #2, interpret threshold:
    elif isADC and re.search('ALLCH_PED',key)!=None:

      if prevthresh=='0':
        mtd['ECALFADC_THRESH']=0
      else:
        mtd['ECALFADC_THRESH']=int('%.0f'%(float(prevthresh)-float(xx[0])+1))

    # special case #3, save threshold:
    elif isADC and re.search('ALLCH_TET',key)!=None:
      prevthresh=xx[0]

    # 2-column lines, only one possibility:
    elif len(xx)==1:
      mtd[key]=int(xx[0])

    # ignore ADC lines with more than two columns:
    elif isADC:
      continue

    else:

      # second column is trigger #
      key=key+'_'+xx.pop(0)

      # 3-column lines:
      if len(xx)==1:
        # config line messed up:
        if re.match('SSP_HPS_SINGLES_NMIN',key)!=None:
          continue
        mtd[key]=int(xx[0])

      # 4-column lines, last 2 columns are value and enable bit:
      elif len(xx)==2:
        mtd[key]=int(xx[0])
        mtd[key+'_EN']=int(xx[1])

      # 5-columns lines, last 3 columns are limits and enable bit:
      elif len(xx)==3:

        mtd[key+'_EN']=int(xx[2])
        # another special case, ENERGYDIST has a float:
        if re.match('SSP_HPS_PAIRS_ENERGYDIST',key)!=None:
          mtd[key+'_SLOPE']=float(xx[0])
          mtd[key+'_OFFSET']=int(xx[1])
        else:
          mtd[key+'_HI']=int(xx[0])
          mtd[key+'_LO']=int(xx[1])



def LoadRunSpreadsheet(file=__SSFILENAME):
  if os.path.isfile(file):
    print 'Reading Run Spreadsheet: ',file
    nlines=0
    for xx in open(file):
      nlines+=1
      if nlines<=3:
        continue
      xx=xx.replace('"','').split(',')
      zz=[yy.rstrip() for yy in xx]
      __SPREADSHEET.append(zz)
  if len(__SPREADSHEET)==0:
    sys.exit('SpreadSheet Problem:  ',file)


# THIS WILL NEED UPDATING FOR NEXT RUN:
def GetMetadataFromRunSpreadsheet(runno,mtd):

  if len(__SPREADSHEET)==0:
    LoadRunSpreadsheet()

  if runno<=__LASTCOSMICRUNBEFOREBEAM:
    return

  col=__SSCOLUMNS

  found=0
  for xx in __SPREADSHEET:
    if runno == int(xx[col['run']]):

      found=1

      # these come only from the spreadsheet::::::::::::::::

      desc1=xx[col['desc1']]
      desc2=xx[col['desc2']]
      trig=xx[col['trig']].replace('\'','prime')
      mtd['Description']='%s , %s , %s'%(trig,desc1,desc2)

#      if xx[col['runbegin']].find('/'):
#        unix=ERU.ConvertSpreadsheetDate(xx[col['runbegin']])
#        if (not unix==None):
#          mtd['RunBegin']=int(time)
#      if xx[col['runend']].find('/'):
#        unix=ERU.ConvertSpreadsheetDate(xx[col['runend']])
#        if (not unix==None):
#          mtd['RunEnd']=int(time)

      if xx[col['nev']].find('M')>=0:
        mtd['Nevents']=float(xx[col['nev']].replace('M','').rstrip())

      if xx[col['tgt']].lower()=='none':
        mtd['Target']='none'
      else:
        mtd['Target']=xx[col['tgt']]

      if xx[col['cur']]=='?':
        pass
      elif xx[col['cur']].lower()=='off':
        mtd['BeamCurrent']=float(0)
      else:
        mtd['BeamCurrent']=float(xx[col['cur']])

      if xx[col['x']]!='' and xx[col['y']]!='':
        mtd['BeamX']=float(xx[col['x']])
        mtd['BeamY']=float(xx[col['y']])


      # use info if we didn't find it elsewhere ::::::::::::

      if not mtd.has_key('GTP_CLUSTER_PULSE_COIN'):
        match=re.search('\+/- \d+',xx[col['hitwindow']])
        if match!=None:
          # divide by 4ns to resurrect DAQ CONFIG line:
          nsamp=int(match.string[match.start()+4:match.end()])/4
          mtd['GTP_CLUSTER_PULSE_COIN']=str('%d %d'%(nsamp,nsamp))

      if not mtd.has_key('GTP_CLUSTER_PULSE_THRESHOLD'):
        match=re.search('\d+',xx[col['seedthresh']])
        if match!=None:
          mtd['GTP_CLUSTER_PULSE_THRESHOLD']=int(match.string)

      if not mtd.has_key('ECALFADC_MODE'):
        mtd['ECALFADC_MODE']=int(xx[col['fadcmode']])

      if not mtd.has_key('ECALFADC_THRESH'):
        mtd['ECALFADC_THRESH']=int(xx[col['fadcthresh']])

      offwin=xx[col['fadcwindow']].split('/')
      if len(offwin)==2:
        if not mtd.has_key('ECALFADC_W_OFFSET'):
          mtd['ECALFADC_W_OFFSET']=int(offwin[0])
        if not mtd.has_key('ECALFADC_W_WIDTH'):
          mtd['ECALFADC_W_WIDTH']=int(offwin[1])

      if not mtd.has_key('ChicaneOffset'):
        match=re.search('chicane (-\d+)\%',mtd['Description'].lower())
        if match:
          mtd['ChicaneOffset']=int(match.group(1))
        elif mtd.has_key('BeamCurrent') and mtd['BeamCurrent']>0:
          mtd['ChicaneOffset']=int(-25)


#      print '-----------------------'
#      for key in mtd.keys():
#        print key,mtd[key]
#      print '-----------------------'

      break

  if not found:
    if not runno in __MISSINGSSHEETRUNS:
      sys.stderr.write('Missing Run in Spreadsheet: '+str(runno)+'\n')
      __MISSINGSSHEETRUNS.append(runno)
    return None


# THIS SHOULD NOT BE NEEDED FOR NEXT RUN, EXCEPT TRIGGER NAMING SCHEME:
def GetMetadataFromLogbook(runno,mtd):

  # everything before beam was cosmic:
  if runno<=__LASTCOSMICRUNBEFOREBEAM:

    mtd['ECALFADC_MODE']=1
    mtd['ECALFADC_THRESH']=0
    mtd['ECALFADC_W_OFFSET']=3180
    mtd['ECALFADC_W_WIDTH']=400
    mtd['SSP_HPS_PULSER']=1
    mtd['SSP_HPS_PULSER_EN']=1
    mtd['SSP_HPS_COSMIC_EN']=1
    mtd['BeamCurrent']=0
    mtd['Description']='Cosmics + 1Hz Pulser'
    mtd['Target']='none'
    mtd['Trigger']='Cosmic + 1Hz Pulser'

  else:

    if runno==3183:
      mtd['Description']='First Beam, No Target'
      mtd['Target']='none'
      mtd['SSP_HPS_SINGLES_0_EN']=1

    # 3304 was first run with full trigger info in EVIO: 
    if runno<3304:

      # add pulser frequence and trigger enableds:
      if mtd.has_key('Description'):
        match=re.match('(\d+)hz pulser',mtd['Description'].lower())
        if match:
          mtd['SSP_HPS_PULSER_EN']=1
          mtd['SSP_HPS_PULSER']=int(match.group(1))
        if mtd['Description'].lower().find('single')>=0:
          mtd['SSP_HPS_SINGLES_0_EN']=1
        if mtd['Description'].lower().find('pair')>=0:
          mtd['SSP_HPS_PAIRS_0_EN']=1
        if mtd['Description'].lower().find('performance')>=0:
          mtd['SSP_HPS_PAIRS_0_EN']=1

      mtd['SSP_HPS_PAIRS_SUMMAX_MIN_0_LO']=0
      mtd['SSP_HPS_PAIRS_SUMMAX_MIN_1_LO']=0

      # reformat minimum pair sums (before 3304 there was no minimum):
      if mtd.has_key('SSP_HPS_PAIRS_SUMMAX_0'):
        mtd['SSP_HPS_PAIRS_SUMMAX_MIN_0_HI']=mtd['SSP_HPS_PAIRS_SUMMAX_0']
        del mtd['SSP_HPS_PAIRS_SUMMAX_0']

      if mtd.has_key('SSP_HPS_PAIRS_SUMMAX_0_EN'):
        mtd['SSP_HPS_PAIRS_SUMMAX_MIN_0_EN']=mtd['SSP_HPS_PAIRS_SUMMAX_0_EN']
        del mtd['SSP_HPS_PAIRS_SUMMAX_0_EN']

      if mtd.has_key('SSP_HPS_PAIRS_SUMMAX_1'):
        mtd['SSP_HPS_PAIRS_SUMMAX_MIN_1_HI']=mtd['SSP_HPS_PAIRS_SUMMAX_1']
        del mtd['SSP_HPS_PAIRS_SUMMAX_1']

      if mtd.has_key('SSP_HPS_PAIRS_SUMMAX_1_EN'):
        mtd['SSP_HPS_PAIRS_SUMMAX_MIN_1_EN']=mtd['SSP_HPS_PAIRS_SUMMAX_1_EN']
        del mtd['SSP_HPS_PAIRS_SUMMAX_1_EN']


    # Singles Hits Cuts:
    # (before run 3426, the EVIO file was wrong)
    if runno<3399:
      mtd['SSP_HPS_SINGLES_NMIN_0_EN']=1
      mtd['SSP_HPS_SINGLES_NMIN_1_EN']=1
      mtd['SSP_HPS_SINGLES_NMIN_0']=0
      mtd['SSP_HPS_SINGLES_NMIN_1']=0
    elif runno<3426:
      mtd['SSP_HPS_SINGLES_NMIN_0_EN']=1
      mtd['SSP_HPS_SINGLES_NMIN_1_EN']=1
      mtd['SSP_HPS_SINGLES_NMIN_0']=4
      mtd['SSP_HPS_SINGLES_NMIN_1']=0

      mtd['SSP_HPS_SINGLES_EMAX_0_EN']=0
      mtd['SSP_HPS_SINGLES_EMAX_1_EN']=0
      mtd['SSP_HPS_SINGLES_EMIN_0_EN']=1
      mtd['SSP_HPS_SINGLES_EMIN_1_EN']=1

    # Prescale Factors:
    # (not available in EVIO file nor spreadsheet):
    mtd['TI_INPUT_PRESCALE_SINGLES_0']=0
    mtd['TI_INPUT_PRESCALE_SINGLES_1']=0
    mtd['TI_INPUT_PRESCALE_PAIRS_0']=0
    mtd['TI_INPUT_PRESCALE_PAIRS_1']=0
    if runno<3395:
      pass
    elif runno<3430:
      mtd['TI_INPUT_PRESCALE_SINGLES_0']=15
    elif runno==3430:
      mtd['TI_INPUT_PRESCALE_SINGLES_0']=3
      mtd['TI_INPUT_PRESCALE_SINGLES_1']=2
    elif runno<3450:
      mtd['TI_INPUT_PRESCALE_SINGLES_0']=8
      mtd['TI_INPUT_PRESCALE_SINGLES_1']=7

    # Trigger Masks:
    # (difficult to read from EVIO files)
    mtd['ECALFADC_MASK']=0
    if runno==3395 or runno==3396 or runno==3401 or runno==3402:
      mtd['ECALFADC_MASK']=1
    elif runno==3398 or runno==3399:
      mtd['ECALFADC_MASK']=2

  if mtd.has_key('SSP_HPS_PULSER'):
    if mtd['SSP_HPS_PULSER']>0:
      mtd['SSP_HPS_PULSER_EN']=1

  # set trigger:
  if mtd.has_key('Description'):
    dd=mtd['Description'].lower()

    match=re.match('(\d+)hz pulser',dd)
    if match:
      mtd['SSP_HPS_PULSER_EN']=1
      mtd['SSP_HPS_PULSER']=int(match.group(1))

    # order is important:

    if dd.find('loosest 2 cluster trigger')>=0:
      mtd['Trigger']='LoosestPair'

    elif dd.find('performance')>=0:
      mtd['Trigger']='LoosestPair'

    elif dd.find('single + pair')>=0:
      mtd['Trigger']='Single+Pair'

    elif dd.find('loose pair')>=0 and dd.find('tight pair')>=0:
      mtd['Trigger']='LoosePair+TightPair'

    elif dd.find('loose pair')>=0:
      mtd['Trigger']='LoosePair'

    elif dd.find('loose single')>=0:
      mtd['Trigger']='LooseSingle'

    elif dd.find('single')>=0:
      mtd['Trigger']='Single'

    elif dd.find('FirstBeam')>=0:
      mtd['Trigger']='LooseSingle'

    elif dd.find('aprime')>=0:
      mtd['Trigger']='Aprime'

    elif dd.find('cosmic')>=0:
      mtd['Trigger']='Cosmic'
      mtd['Target']='none'
      mtd['BeamCurrent']=0
      mtd['SSP_HPS_COSMIC_EN']=1
      mtd['SSP_HPS_PULSER_EN']=1
      mtd['SSP_HPS_PULSER']=1
      mtd['ECALFADC_MODE']=1
      mtd['ECALFADC_THRESH']=0
      mtd['ECALFADC_W_OFFSET']=3180
      mtd['ECALFADC_W_WIDTH']=400

    elif dd.find('pedestal')>=0 or dd.find('pulser')>=0:
      mtd['Trigger']='PulserOnly'


def GetPassMetadata(filename,mtd):
  npass=ERU.GetPass(filename)
  if npass==None:
    return
#  mtd['Pass']=npass
  if npass==0:
    pass
#    mtd['Collections']='TriggerBank,EcalReadoutHits,FADCGenericHits,EcalCalHits,EcalClusters,EcalClustersIC,RejectedHits,EcalClustersGTP'
#    mtd['Release']='/home/hps/hps-group/hps_soft/hps-java/hps-distribution-3.1-SNAPSHOT-20141225-bin.jar'
#    mtd['Steering']='org.hps.steering.recon.EngineerinfRun2014ECalRecon.lcsim'
#    mtd['Detector']='HPS-ECalCommissioning'
#  else:
    sys.exit('GetPassMetadata:  Not Ready for this Pass:  '+npass)



