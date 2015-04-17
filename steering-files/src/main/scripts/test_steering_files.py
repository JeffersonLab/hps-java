#!/usr/bin/env python

""" This script performs a dry run test on all the steering files in this module. """

# imports
import re, fnmatch, os, sys, subprocess
from sets import Set

# the jar that will be run which can optionally be set as the command line argument 
jar = "~/.m2/repository/org/hps/hps-distribution/${project.version}/hps-distribution-${project.version}-bin.jar"
if len(sys.argv) > 1:
  jar = sys.argv[1]

# verbosity
verbose = True

# basic command to run the steering file
run_cmd = "java -jar %s -x -v" % jar    
print "base run command: %s" % run_cmd 
print

# make a list of steering files to check
steering_files = []
for root, dirnames, filenames in os.walk('src/main/resources'):
  for filename in fnmatch.filter(filenames, '*.lcsim'):
    steering_files.append(os.path.join(root, filename))

# regex for finding variables in the steering files
regex = re.compile(r'\$\{\S*\}')

# totals of good and bad files
total_run = 0
total_errors = 0
total_okay = 0

# list of bad files
bad_files = []

# loop over steering files that were found
for steering_file in steering_files:
  print ">>>> testing steering file:", steering_file
  
  # find variables that will need to be set with dummy values
  vars = Set()
  for line in open(steering_file):
    varsrch = regex.findall(line)
    if (len(varsrch) > 0):
      for var in varsrch:
        varname = var[2:len(var)-1]
        vars.add(varname)
  
  # setup the run command for this file
  shell_cmd = run_cmd[:]
  for var in vars:
    val = 'dummy'
    # this variable needs to be a number
    if var == 'runNumber':
      val = '1'
    shell_cmd += " -D%s=%s" % (var, val)
  shell_cmd += " %s" % steering_file
  
  print "  %s" % shell_cmd

  # run the command
  process = subprocess.Popen(shell_cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
  printLine = False
  for line in process.stdout.readlines():
    # print out lines with exceptions and all subsequent output
    if 'Exception' in line:
      printLine = True
    if printLine or verbose:
      print "  %s" % line,
      
  # print the return value
  retval = process.wait()
  print
  print "  return value: %d" % retval
  print
  
  # increment totals etc.
  if retval == 0:
    total_okay += 1
  else:
    total_errors += 1
    bad_files.append(steering_file)
  total_run += 1

# print summary
print "Summary:"
print "  Ran %d steering files of which %d had errors and %d were okay." % (total_run, total_errors, total_okay)
print 
print "Files with errors: "
for bad_file in bad_files:
  print "  %s" % bad_file
