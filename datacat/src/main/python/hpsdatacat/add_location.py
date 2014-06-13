#!/usr/bin/env python

"""
Add an additional location for a dataset using the 'addLocation' command.  
""" 

from util import *

__command = 'addLocation'

# create the parser
parser = create_base_argparser(__command)
parser.add_argument('-p', '--path', help='logical folder in data catalog where dataset is located', required=True)
parser.add_argument('-n', '--name', help='original dataset name (with no file extension)', required=True)
parser.add_argument('-f', '--file', help='new physical file location', required=True)
parser.add_argument('-g', '--group', help='dataset group', default=get_default_group())
parser.add_argument('-s', '--site', help='new dataset site', default=get_default_site())
parser.add_argument('-v', '--version', help='dataset version')
args = vars(parser.parse_args())

# process command line arguments
connection, dry_run, mode = handle_standard_arguments(args)
if connection == None:    
    connection = get_ssh_connection_string()    
    if connection == None:
        raise Exception("Couldn't figure out a connection to use!")
logical_folder = args['path']
dataset_name = args['name']
file_path = args['file']
group = args['group']
site = args['site']
check_valid_site(site)
version = None    
if args['version'] != None:
    version = args['version']

# build command line
command_line = create_base_command_line(__command, connection, dry_run, mode)
if group != None:
    command_line += ' --group %s' % group
if site != None:
    command_line += ' --site %s' % site
if version != None:    
    command_line += ' --version %s' % version    
command_line += ' %s %s %s' % (dataset_name, logical_folder, file_path)

# run the command
lines, errors, return_value = run_process(command_line)

# print results
print_result(__command, return_value, errors)