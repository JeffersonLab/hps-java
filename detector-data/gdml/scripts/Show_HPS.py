#!/usr/bin/env python
#
# Author: Maurik Holtrop (UNH)
# Date: Oct 11, 2018
#
# This script creates the flange and support structures for the
# HPS hodoscope.
#
import math
#import MySQLdb
import sys
import argparse
#
sys.path.append(".")    # Allow Python to find the Rotations.py and GeometryEngine.py.

import ROOT
from Write_HPS_ecal import *
from Write_HPS_beamline import *
from Write_HPS_hodo import *
from GeometryEngine import Geometry, GeometryEngine, Sensitive_Detector
from GeometryROOT import GeometryROOT

Detector = "ecal_vacuum"
Variation= "original"

geo_en = GeometryEngine(Detector)
geo_en_flux=0

#    Set_Global_Parameters()
#calculate_ecal_mother_geometry(geo_en,mother="world_volume")
#origin=[x_location_crystal_front,0,z_location]
origin=[21.17,0,z_location]
print("Parameters used for the ECal detector:")
print_parameters()
print("Origin = ",origin)

calculate_dipole_geometry(geo_en,origin=[0,0,0],mother="world_volume")
calculate_ps_vacuum(geo_en)
calculate_ecal_geometry(geo_en,origin=origin,mother="world_volume")
calculate_ecal_vacuum_geometry(geo_en,origin=origin,mother="world_volume")
calculate_ecal_crystalbox_geometry(geo_en,origin=origin,mother="world_volume")
calculate_ecal_coolingsys_geometry(geo_en,origin=origin,mother="world_volume")
calculate_hodo_support_geometry(geo_en,origin=[21.17,0,0],mother="world_volume",zloc=Box_Start_z)

rr = GeometryROOT("world_volume")
rr.Create_root_volume()
rr.Build_volumes(geo_en)
rr.Draw("ogl")

if __name__ == "__main__":
    print("Type return to end program.")
    ans=input("...")
