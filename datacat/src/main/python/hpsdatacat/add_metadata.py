#!/usr/bin/env python

"""
Set meta data on an existing dataset or group using the 'addMetaData' command.
"""

from util import *

# command this script will use
__command = 'addMetaData'

# create the argparser
parser = create_base_argparser(__command)
parser.add_argument('-p', '--path', help='logical folder in data catalog', required=True)
parser.add_argument('-g', '--group', help='dataset group or group to tag when no physical file is specified', required=True)
parser.add_argument('-d', '--define', nargs='*', help='define one field in key=value format', required=True)
parser.add_argument('-n', '--name', help='dataset name')
parser.add_argument('-v', '--version', help='version ID of the dataset (defaults to latest)')
args = vars(parser.parse_args())

# handle standard arguments
connection, dry_run, mode = handle_standard_arguments(args)
  
# file_path
file_path = args['name']
group = args['group']    
if file_path == None and group == None:
    raise Exception("A dataset name or a group is required.")
logical_folder = args['path']
version = args['version']

# metadata    
if args['define'] == None:
    raise Exception("At least one meta data definition is required.")            
metadata = format_metadata_definition(args['define'])
if metadata == None:
    raise Exception("Bad meta data definition.")    

# build command line
command_line = create_base_command_line(__command, connection, dry_run, mode)    
if file_path != None:
    command_line += ' --dataset %s' % file_path    
if version != None:
    command_line.append += ' --version %s' % version
if group != None:
    command_line += ' --group %s' % group
command_line += ' %s' % metadata
command_line += ' %s' % logical_folder

# run the command
lines, errors, return_value = run_process(command_line)

# print end message
print_result(__command, return_value, errors)