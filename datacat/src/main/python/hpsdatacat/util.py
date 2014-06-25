#!/usr/bin/env python

"""
Miscellaneous global utility functions.
"""

import sys, os, getpass, socket, subprocess, argparse

# location of data catalog script at SLAC
__datacat_script = '~srs/datacat/prod/datacat-hps'

# valid commands
__valid_commands = ('rm', 'registerDataset', 'addLocation', 'addMetaData', 'find', 'crawler')

# valid mode settings
__valid_modes = ('PROD', 'DEV', 'TEST')

# default site for data catalog search
__default_site = 'SLAC'

# valid sites
__valid_sites = ('SLAC', 'JLAB')

# default base path for datacatalog search
__default_search_path = '/HPS'

# default dataset group
__default_group = 'HPS'

"""
Get the default site.
"""
def get_default_site():
    return __default_site

"""
Get the default search path.
"""
def get_default_search_path():
    return __default_search_path

"""
Get the default dataset group.
"""
def get_default_group():
    return __default_group
                     
"""
Simple utility to return the full script command.
This function will check if the command is valid.
"""
def get_datacat_command(command):
    if command not in __valid_commands:
        raise Exception("Unknown command: " % command)
    return '%s %s' % (__datacat_script, command)

"""
Get an SSH connection string for the SLAC or JLAB sites.
This function will return null if not running at those sites,
in which case the caller needs to provide their own (usually
through a command line argument to one of the scripts).
"""
def get_ssh_connection_string():

    # setup default connection
    connection = None
    domainname = socket.getfqdn()    

    if 'slac' in domainname:
        username = getpass.getuser()
    elif 'jlab' in domainname and getpass.getuser() == 'clashps':
        username = 'hpscat'
    else:
        username = None
        
    if username != None:         
        connection = '%s@rhel6-64d.slac.stanford.edu' % username
    
    return connection

"""
Run a process in a shell and return the output lines, errors, and return value (in that order). 
"""
def run_process(command, verbose=True, printOutput=True):
    
    if verbose:
        print "Executing command ..."
        print command
    
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    errors = []
    lines = []
    for line in process.stdout.readlines():
        if printOutput:
            print line,
        if 'Exception' in line:
            errors.append(line)
        lines.append(line)
        
    return_value = process.wait()
    
    return lines, errors, return_value

"""
Escape characters for the SSH command.
These include double quotes, spaces, and ampersands.
"""
def escape_characters(raw_string):
    return raw_string.replace('"', '\\"').replace(' ', '\\ ').replace('&', '\\&')

"""
Format meta data string for the 'registerDataset' or 'addMetaData' command line arguments 
from a supplied list of 'key=value' strings.  
This function will return None if the raw_metadata has length zero.
"""
def format_metadata_definition(raw_metadata):
    metadata = ''
    for var in raw_metadata:
        equals = var.find('=')
        if (len(var) < 3 or equals < 0):
            raise Exception("Bad meta data variable format.")
        metadata += '--define %s ' % var
    if len(metadata) == 0:
        metadata = None
    return metadata

"""
Create the basic argparser for data catalog commands which includes handling
of dry run, mode, verbosity and connection settings.  These are all optional.
"""
def create_base_argparser(command):
    if command not in __valid_commands:
        raise Exception("Unknown command: %s" % command)
    parser = argparse.ArgumentParser(description='Execute the %s command on the data catalog' % command)
    parser.add_argument('-D', '--dry-run', help='perform dry run only with no database commits', action='store_true')
    parser.add_argument('-M', '--mode', help='set data source as PROD, DEV, or TEST')
    parser.add_argument('-c', '--connection', help='SSH connection string in form user@host', default=get_ssh_connection_string())
    parser.add_argument('-v', '--verbose', help='run in verbose mode', action='store_true')
    return parser

"""
Parse and return standard arguments from the base parser.
"""
def handle_standard_arguments(args):    
    if args['connection'] != None:
        connection = args['connection']
    else:
        raise Exception("Could not figure out an SSH connection!")
    dry_run = args['dry_run']        
    mode = args['mode']
    verbose = args['verbose']
    return connection, dry_run, mode, verbose

"""
Print the results of running a command.
"""
def print_result(command, return_value, errors, printSuccessful=True):
    if return_value != 0 or len(errors) != 0:
        print "Command %s returned with errors ..." % command
        for error in errors:
            print error
    else:
        if printSuccessful:
            print "Command %s was successful!" % command
    if (return_value != 0) or printSuccessful:
        print "return_value: %s" % str(return_value)

"""
Create the basic SSH command from common arguments.
"""
def create_base_command_line(command, connection, dry_run, mode):        
    command_line = 'ssh %s' % (connection)
    command_line += ' %s' % (get_datacat_command(command))
    if mode != None:
        command_line += ' --mode %s' % mode
    if dry_run:
        command_line += ' --nocommit'
    return command_line

"""
Check if a site looks valid.
"""
def check_valid_site(site):
    if site not in __valid_sites:
        raise Exception("Site is not valid: " + site)
    
"""
Send stdout and stderr to /dev/null e.g. to suppress messages.
"""
def suppress_print():
    sys.stdout = open(os.devnull, 'w')
    sys.stderr = open(os.devnull, 'w')

"""
Restore stdout and stderr after supressing them.
"""    
def restore_print():    
    sys.stdout = sys.__stdout__
    sys.stderr = sys.__stderr__
    
"""
Raise a fatal error.
"""
def fatal_error(message, return_value=1):
    print "Fatal error: %s" % message
    sys.exit(return_value)
    