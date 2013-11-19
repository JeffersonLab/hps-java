#!/usr/bin/python

import os,sys,glob


test = True
pat = []
notpat = []
for w in range(1,len(sys.argv)):
    if not sys.argv[w]=='run':
        if '!' in sys.argv[w]:
            notpat.append(sys.argv[w].replace('!',''))
        else:
            pat.append(sys.argv[w])


for w in sys.argv:
    if w=='run':
        test = False


if test:
    print 'TESTING'
else:
    print 'RUNNING'
print 'pattern \"',pat,'\"'
print 'no pattern \"',notpat,'\"'


cmd = []
steering =  'steering/users/phansson/TwoTrackAnalysis.lcsim'
conv_script = 'makeTTreeFromTxtFile.C'
jar = 'target/hps-java-1.8-SNAPSHOT-bin.jar'
tag = '_%s' % os.path.splitext(os.path.basename(jar))[0]


################################# twotrkfilt #################################


path = '../data/mc/HPS-TestRun-v5/1.8-SNAPSHOT-gauss-101013'
run = 1351
infiles = glob.glob('%s/pairs*0.016x0*gauss*twotrkfilt*slcio' % path)
outfile = 'twotrackAnlysisTuple_gauss_0.016x0_500mb_90na_%dfiles_twotrkfilt%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))

path = '../data/mc/HPS-TestRun-v5/1.8-SNAPSHOT-recoil-101013'
run = 1351
infiles = glob.glob('%s/egs*0.016x0*recoil*twotrkfilt*slcio' % path)
outfile = 'twotrackAnlysisTuple_recoil_0.016x0_500mb_90na_%dfiles_twotrkfilt%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))



path = '../data/mc/HPS-TestRun-v6/1.8-SNAPSHOT-recoil-102413'
run = 1351
infiles = glob.glob('%s/egs*0.016x0*recoil*twotrkfilt*slcio' % path)
outfile = 'twotrackAnlysisTuple_recoil_v6_0.016x0_500mb_90na_%dfiles_twotrkfilt%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))

path = '../data/data/lcio/HPS-TestRun-v5/101613'
for run in [1351,1353,1354]:
    infiles = glob.glob('%s/hps_00%d.evio.*_recon_twotrkfilt.slcio' % (path, run) )
    outfile = 'twotrackAnlysisTuple_pair%s_twotrkfilt%s.txt' % (run,tag)
    fstr = ''
    for f in infiles:
        fstr = fstr + ' -i ' + f
    cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=false' % (jar,steering,fstr,run,outfile))
    cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))

path = '../data/data/lcio/HPS-TestRun-v6/102413'
for run in [1351,1353,1354,1358]:
    infiles = glob.glob('%s/hps_00%d.evio.*_recon_twotrkfilt.slcio' % (path, run) )
    outfile = 'twotrackAnlysisTuple_pair%s_v6_twotrkfilt%s.txt' % (run,tag)
    fstr = ''
    for f in infiles:
        fstr = fstr + ' -i ' + f
    cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=false' % (jar,steering,fstr,run,outfile))
    cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))




################################# single #################################


path = '../data/mc/HPS-TestRun-v6/1.8-SNAPSHOT-default-102413'
run = 1351
infiles = glob.glob('%s/egs*0.016x0*readout*slcio' % path)
outfile = 'twotrackAnlysisTuple_default_readout_v6_0.016x0_500mb_90na_%dfiles%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))


path = '../data/mc/HPS-TestRun-v6/1.8-SNAPSHOT-default-102413'
run = 1351
infiles = glob.glob('%s/g4*0.016x0*readout*slcio' % path)
outfile = 'twotrackAnlysisTuple_default_readout_v6_g4_0.016x0_500mb_90na_%dfiles%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))


path = '../data/data/lcio/HPS-TestRun-v6/102413'
for run in [1358]:
    infiles = glob.glob('%s/hps_00%d.evio.*_recon.slcio' % (path, run) )
    outfile = 'twotrackAnlysisTuple_%s_recon_v6%s.txt' % (run,tag)
    fstr = ''
    for f in infiles:
        fstr = fstr + ' -i ' + f
    cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=false' % (jar,steering,fstr,run,outfile))
    cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))

path = '../data/mc/HPS-TestRun-v6/1.8-SNAPSHOT-recoil-102413'
run = 1351
infiles = glob.glob('%s/egs*0.016x0*recoil_readout*slcio' % path)
outfile = 'twotrackAnlysisTuple_recoil_readout_v6_0.016x0_500mb_90na_%dfiles%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))



path = '../data/mc/HPS-TestRun-v5/1.8-SNAPSHOT-recoil-101013'
run = 1351
infiles = glob.glob('%s/egs*0.016x0*recoil_readout*slcio' % path)
outfile = 'twotrackAnlysisTuple_recoil_readout_0.016x0_500mb_90na_%dfiles%s.txt' % (len(infiles),tag)
fstr = ''
for f in infiles:
    fstr = fstr + ' -i ' + f
cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=true' % (jar,steering,fstr,run,outfile))
cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))



path = '../data/data/lcio/HPS-TestRun-v5/101613'
for run in [1351]:
    infiles = glob.glob('%s/hps_00%d.evio.*_recon.slcio' % (path, run) )
    outfile = 'twotrackAnlysisTuple_%s_recon%s.txt' % (run,tag)
    fstr = ''
    for f in infiles:
        fstr = fstr + ' -i ' + f
    cmd.append('java -jar %s %s %s -DrunNumber=%d -DoutputFile=%s -DisMC=false' % (jar,steering,fstr,run,outfile))
    cmd.append('root -l -q %s\(\\"%s\\"\)'%(conv_script,outfile))



for c in cmd:
    ok = True
    for w in pat:
        if not w in c:
            ok = False
    for w in notpat:
        if w in c:
            ok = False
    if ok:
        print c
        if not test:
            print 'execute command'
            os.system(c)
