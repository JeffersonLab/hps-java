#!/usr/bin/env python
#
# This script uses the ROOT TGeometry system to display an HPS LCDD fileself.
#
# The script will also set some of the visualization properties.
#
import re
import sys

try:
    import ROOT
except:
    print("Could not import the ROOT module, which is needed to display the detector.")
    print("Please make sure ROOT is installed (root.cern.ch) and the PYTHONPATH is set correctly.")
    sys.exit()

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

    return(new_file)

def make_volume_dict(vols):
    '''Traverse the volume tree at level vols and return a dictionary of the daughter volumes.'''
    if vols.GetNdaughters() <= 0:
        vols.dict = None
        return(None)

    out={}
    daughters = vols.GetNodes()
    for i in range(daughters.GetEntries()):
        vol = daughters.At(i)
        name = vol.GetName()
        out[name]=vol
        make_volume_dict(vol)

    vols.dict = out
    return(out)

def find_volume(vols,name):
    '''Recursively search for name in a volume tree that has dictionaries.'''

#    print(vols.GetName())
    if vols.dict is not None:
        if name in vols.dict:
            return(vols.dict[name])  # Found it, so return the object
        else:
            for k in vols.dict:
                found = find_volume(vols.dict[k],name)
                if found is not None:
                     return(found)
    return(None)   # Note found, return None

def find_volumes_regex(vols,reg):
    '''Recursively search for regex reg in a volume tree that has dictionaries.
       Return all list matches, or if no match, an empty list'''

    out=[]
#    print(vols.GetName())
    if vols.dict is not None:
        for name in vols.dict:
            if re.match(reg,name):
                out.append(vols.dict[name])
            rec_list = find_volumes_regex(vols.dict[name],reg)  # Recurse down
            for rec in rec_list:
                out.append(rec)

    return(out)   # Note found, return None

def set_colors(geom):
    ''' Decorate the detector '''

    supports = find_volumes_regex(geom,".*support.*")
    for s in supports:
        s.GetMedium().GetMaterial().SetTransparency(90)

    chamber = find_volumes_regex(geom,"svt_chamber.*")
    for c in chamber:
        c.GetMedium().GetMaterial().SetTransparency(90)

    flange = find_volumes_regex(geom,".*flange.*")
    for f in flange:
        print(f.GetName(),"  ",f.GetMedium().GetMaterial().GetName())
        f.GetMedium().GetMaterial().SetTransparency(93)


    ecal_cham = find_volumes_regex(geom,".*ECAL_chamber.*")
    for ec in ecal_cham:
        print(ec.GetName(),"  ",ec.GetMedium().GetMaterial().GetName())
        ec.GetMedium().GetMaterial().SetTransparency(80)




def main():
    import sys

    if len(sys.argv)<2:
        print("Please supply a filename to show.")
        return()

    if re.match(".*\.lcdd",sys.argv[1]):
        gdml_file = remove_lcdd(sys.argv[1])
    else:
        gdml_file=sys.argv[1]

    geo=ROOT.gGeoManager
    geo.Import(gdml_file)
    print("Checking overlaps:")
    geo.CheckOverlaps(0.00001)
    print("Print the overlaps:")
    geo.PrintOverlaps()
    top_vol = geo.GetTopVolume()

    make_volume_dict(top_vol)

    set_colors(top_vol)
    browser = ROOT.TBrowser()
    top_vol.Draw("ogl")
    print("Type return to end program.")
    if  sys.version_info[0] == 2:
        ans=raw_input("...")
    else:
        ans=input("...")

if __name__ == '__main__':
    main()
