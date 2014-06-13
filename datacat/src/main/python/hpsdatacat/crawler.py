#!/usr/bin/env python

"""
Directory crawler that will find and add new files to the data catalog.
Files with a creation time after the modification time of a timestamp
file are registered.  It uses the register.py script to perform the 
dataset registration.
"""

import os
from util import *
from extract_metadata import *

# file extensions that will be looked at by default
__default_extensions = ('slcio', 'evio', 'root')

# name of command, but does not correspond to anything in the data catalog API
__command = 'crawler'

# default site
site = get_default_site()

# default group
group = get_default_group()

# default data catalog root path for file registration
default_base_folder = '/HPS'

# path to script for registering new files
register_script = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'register.py')

# command line options
parser = create_base_argparser(__command)
parser.add_argument('-d', '--directory', help='starting base_directory to crawl')
parser.add_argument('-t', '--timestamp', help='files newer than the timestamp file will be registered')
parser.add_argument('-e', '--extension', help='only handle files with given extension')
parser.add_argument('-g', '--group', help='dataset group')
parser.add_argument('-s', '--site', help='dataset site e.g. SLAC or JLAB')
parser.add_argument('-p', '--path', help='path in the data catalog')
args = vars(parser.parse_args())

connection_string, dry_run, mode = handle_standard_arguments(args)

# Try to figure out a default connection string if none was supplied.    
if connection_string == None:    
    connection_string = get_ssh_connection_string()    
    if connection_string == None:
        raise Exception("Couldn't figure out a connection_string to use!")    

# base directory to crawl
if args['directory'] != None:
    base_directory = args['directory']
else:
    raise Exception("The directory is required!")

# timestamp file to use
if args['timestamp'] != None:
    timestamp_file_path = args['timestamp']
    if not os.path.isfile(timestamp_file_path):
        raise Exception("The timestamp file %s does not exist!" % timestamp_file_path)
else:
    raise Exception("The timestamp file is a required argument!")

# the timestamp for comparison is the modification time of the timestamp file 
timestamp = os.path.getmtime(timestamp_file_path)
print 'using timestamp ', str(timestamp)

# dataset group
if args['group'] != None:
    group = args['group']

# dataset site    
if args['site'] != None:
    site = args['site']
    check_valid_site(site)

# path in data catalog    
datacat_path = None
if args['path'] != None:
    datacat_path = args['path']

# file extension to process
handle_extensions = __default_extensions
if args['extension'] != None:
    # only look at files with extension matching argument
    handle_extensions = (args['extension'])

# walk the directory tree
for dirname, dirnames, filenames in os.walk(base_directory):
    # ignore directories starting with a '.'
    if dirname[0] == '.':
        continue
    # process files
    for filename in filenames:

        # get file path and extension
        full_path = os.path.join(dirname, filename)
        extension = os.path.splitext(full_path)[1][1:]

        # process file if it is a valid extension                
        if extension in handle_extensions:      
                  
            # get the creation time of the file
            file_ctime = os.path.getctime(full_path)
            
            # register files with creation time greater than modification time of the timestamp file                
            if file_ctime > timestamp:
                
                print 'found file %s with creation time %f' % (full_path, file_ctime)
                
                # extract meta data
                metadata_extractor = get_metadata_extractor(full_path)
                if (metadata_extractor == None):
                    raise Exception("A MetaDataExtractor for %s was not found!" % full_path)
                metadata_extractor.extract_metadata(full_path)                
                metadata = metadata_extractor.to_define_string()              
                
                # relative path
                rel_path = full_path.replace(base_directory, '')
                base_path = os.path.dirname(rel_path)
                
                # figure out the folder to use in the data catalog
                if datacat_path == None:
                    # folder from structure under root directory
                    datacat_folder = default_base_folder
                    datacat_folder += os.path.dirname(rel_path)
                else:
                    # folder from command line argument
                    datacat_folder = datacat_path
                
                # build the command line
                command_line = register_script
                if dry_run == True:
                    command_line += ' --dry-run'
                if mode != None:
                    command_line += ' --mode ' % mode
                command_line += ' -c %s' % connection_string                
                command_line += ' --file %s' % full_path
                command_line += ' --path %s' % datacat_folder
                command_line += ' --group %s' % group
                command_line += ' --site %s' % site
                command_line += '%s' % metadata
                 
                # run the register command and print results
                print "Registering new file with command ..."
                print command_line                                                                        
                lines, errors, return_value = run_process(command_line, False)                
                if len(errors) > 0 or return_value != 0:
                    print "Command returned with an error!"                    
                    # just print the first error
                    print errors[0]
                else:
                    print "File successfully registered!"

# touch the timestamp file to update its modification time
os.utime(timestamp_file_path, None)