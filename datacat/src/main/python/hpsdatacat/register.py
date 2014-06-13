#!/usr/bin/env python

""" 
Register new files in the data catalog using the 'registerDataset' command. 
"""

import os.path, subprocess, socket, getpass

from util import *

__command = 'registerDataset'
    
# create command line parser
parser = create_base_argparser(__command)
parser.add_argument('-p', '--path', help='destination logical folder in the data catalog', required=True)
parser.add_argument('-f', '--file', help='input physical file to register', required=True)
parser.add_argument('-d', '--define', help='define a field with format key=value', action='append')
parser.add_argument('-g', '--group', help='dataset group', default=get_default_group())
parser.add_argument('-s', '--site', help='site of the physical file', default=get_default_site())
args = vars(parser.parse_args())

# process command line arguments
connection, dry_run, mode = handle_standard_arguments(args)            
logical_folder = args['path']
file_path = args['file']
file_extension = os.path.splitext(file_path)[1][1:]
group = args['group']
site = args['site']
check_valid_site(site)
    
# build meta data definitions
metadata = None
raw_metadata = args['define']
if args['define'] != None:
    metadata = format_metadata_definition(raw_metadata)

# build command line
command_line = create_base_command_line(__command, connection, dry_run, mode)
command_line += ' --group %s --site %s' % (group, site)
if metadata != None:
    command_line += ' %s' % metadata    
command_line += ' %s %s %s' % (file_extension, logical_folder, file_path)

# run the command
lines, errors, return_value = run_process(command_line)

# print file_path information for new dataset
if return_value == 0:
    print 'Added dataset to catalog ...'
    print '  file: %s' % file_path
    print '  folder: %s' % logical_folder
    print '  group: %s' % group
    print '  site: %s' % site
    print '  metadata: %s' % str(raw_metadata)

# print command result
print_result(__command, return_value, errors)