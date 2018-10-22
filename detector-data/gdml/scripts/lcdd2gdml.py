#!/usr/bin/env python
#
# This script strips the LCDD parts off of an LCDD file, leaving you
# a gdml file.
#
#
import re
def remove_lcdd(file):
    """Removes the lcdd components of the file, leaving you with GDML only."""
    print("lcdd code being removed from: ",file)
    new_file = file.replace(".lcdd",".gdml")
    inf = open(file,"r")
    of  = open(new_file,"w")

    lcdd_region=False
    lcdd_region_false_next_line=False
    lcdd_region_true_next_line=False

    for line in inf:
        if lcdd_region and lcdd_region_false_next_line:
            lcdd_region=False
            lcdd_region_false_next_line=False

        if not lcdd_region and lcdd_region_true_next_line:
            lcdd_region=True
            lcdd_region_false_next_line=False

        if re.search(" *<lcdd.*>",line):
            lcdd_region=True

        if re.search(".*<gdml.*>",line):
            lcdd_region=False

        if re.search(".*</gdml.*>",line):
            lcdd_region_true_next_line=True


        if not lcdd_region:
            of.write(line)

    inf.close()
    of.close()

def main():
    import sys

    if len(sys.argv)<2:
        print("Please supply a filename to transform.")
    else:
        remove_lcdd(sys.argv[1])

if __name__ == '__main__':
    main()
