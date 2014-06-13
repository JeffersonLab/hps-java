#!/usr/bin/env python

""" 
Find files in the data catalog using the 'find' command.
"""

import os.path, subprocess, socket, getpass
from util import *

# data catalog command to be executed
__command = 'find'

# default options for search command
__script_options = '--search-groups --recurse'

# command line parser
parser = create_base_argparser(__command)
parser.add_argument('-p', '--path', help='root logical folder for search', default=get_default_search_path())
parser.add_argument('-s', '--site', help='dataset site', default=get_default_site())
parser.add_argument('-o', '--output', help='save results to output file')
parser.add_argument('-q', '--query', help='data query for filtering results')
args = vars(parser.parse_args())

# get standard arguments
connection, dry_run, mode = handle_standard_arguments(args)    
logical_folder = args['path']
site = args['site']
check_valid_site(site)
    
# meta data query            
query = None
if args['query'] != None:
    query = '--filter \'%s\'' % args['query']
    query = escape_characters(query)
    print query

# build the command line
command_line = create_base_command_line(__command, connection, dry_run, mode)
command_line += ' %s' % __script_options 
command_line += ' --site %s' % site
if query != None:
    command_line += ' %s' % query
command_line += ' %s' % logical_folder     
 
# setup the output file if specified
output = None
if args['output'] != None:
    output = args['output']
    if os.path.isfile(output):
        raise Exception('The output file already exists!')
    output_file = open(output, 'w')

# run the command
lines, errors, return_value = run_process(command_line)

# print or save the output if command was successful
if (return_value == 0 and len(errors) == 0):
    if output != None:
        for line in lines:
            output_file.write(line)
        output_file.close()
        print 'Output saved to file: %s' % output

# print command result
print_result(__command, return_value, errors, False)