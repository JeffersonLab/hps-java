#!/usr/bin/env python

"""
Delete a file from the data catalog by using the 'rm' command.
"""

from util import *

__command = 'rm'

# command line parser
parser = create_base_argparser(__command)
parser.add_argument('-p', '--path', help='path to delete from the data catalog (dataset or folder)', required=True)
args = vars(parser.parse_args())

# process command line arguments
connection, dry_run, mode = handle_standard_arguments(args)
logical_folder = args['path']
    
# build command line
command_line = create_base_command_line(__command, connection, dry_run, mode)
command_line += ' --force %s' % logical_folder

# run command line
lines, errors, return_value = run_process(command_line)

# print the result
print_result(__command, return_value, errors) 