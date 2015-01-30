#!/usr/bin/env python
import sys
import engrun_util as ERU

# WORK IN PROGRESS

# this is a wrapper because we need two-step search
# once on raw files which have all the metadata,
# and again for the reconstructed files

usage='engrun_find.py <GROUP> <PASS> [query]'

if len(sys.argv)<3:
  sys.exit(usage)

group=sys.argv[1]
npass=sys.argv[2]

if group.find(' ') or npass.find(' '):
  sys.exit(usage)

args = ' '.join(sys.argv[3:])

dclistcmd=ERU.__SSH+' "'+ERU.__DATACAT+' find '+ERU.__DCSRCHOPTS+' '+args+' --group RAW /HPS/engrun2014'+'"'

#print dclistcmd
#sys.exit()

rawlist=ERU.ListDataCatalog(dclistcmd)
runlist=ERU.GetRunList(rawlist)

#print rawlist
#print runlist
#sys.exit()

tmp=''
opts=' --filter \'('
for xx in runlist:
  opts += ' %s nRun==%d '%(tmp,xx)
  tmp=' || '
opts+=')\''

dclistcmd=ERU.__SSH+' "'+ERU.__DATACAT+' find '+ERU.__DCSRCHOPTS+' '+opts+' --group '+group+' /HPS/engrun2014/pass'+npass+'"'

reclist=ERU.ListDataCatalog(dclistcmd)

for xx in reclist:
  print xx

