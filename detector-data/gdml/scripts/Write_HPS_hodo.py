#!/usr/bin/env python
#
# Author: Maurik Holtrop (UNH)
# Date: October, 2018

import math
#import MySQLdb
import sys
import argparse
#
sys.path.append(".")    # Allow Python to find the Rotations.py and GeometryEngine.py.
from Rotations import *
from GeometryEngine import Geometry, GeometryEngine, Sensitive_Detector
import ROOT

if __name__ == "__main__":
################################################################################################
#
# Geometry engine setup for local running
#
# Slightly unusually, we need to do this here rather than at the end of the file,
# so that any parameter changes that are put in as arguments can be part of the parameter
# calculation.
# Yes, it would be much clearer to make this whole thing a Python class, but at this point
# I don't want to spend the time to put self. in front of every variable everywhere....
#
#################################################################################################

    parser = argparse.ArgumentParser(
                description="""Master HPS geometry writer.
                This code will create TEXT or GDML tables for the HPS Hodoscope.""",
                epilog="""For more information, or errors, please email: maurik@physics.unh.edu """)
    parser.add_argument('-s','--show',action='store_true',help='show the geometry using the ROOT TGeoManager mechanism.')
    parser.add_argument('-g','--gdml',action='store_true',help='Produce a GDML file for output.')
    parser.add_argument('-t','--txt',action='store_true',help='Produce a TEXT file for output.')
    parser.add_argument('-m','--mysql',action='store_true',help='Produce a MySQL table for output. You must also specify --host --database --user --password')
    parser.add_argument('-H','--host',help='Name of the database host computer',default="localhost")
    parser.add_argument('-D','--database',help='Name of the database',default="hps_2014")
    parser.add_argument('-u','--user',help='User name for the database',default="clasuser")
    parser.add_argument('-p','--passwd',help='Password for connecting to the database',default="")
    parser.add_argument('-d','--debug',action="count",help='Increase debug level by one.')
    parser.add_argument('-z','--extraz',type=float,help='Amount to add to the Z-location of ECAL')
    args = parser.parse_args(sys.argv[1:])


StainlessSteel='StainlessSteel'
Aluminum='G4_Al'
Copper='Cu'


def calculate_hodo_support_geometry(g_en,origin=[0,0,0],mother="root",style=1,zloc=0):
    """Computes the geometry of all the support structures for the 2019 Hodoscope  """

    front_flange_center_z = zloc - 50.0/2.

    print(origin)

    geo = Geometry(
          name='hodo_flange_outer',
          mother=mother,
          description='Vacuum Chamber Shim Flange outer shape',
          pos=[0,0,0],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffff00',
          g4type='Box',
          dimensions=[768.35/2.,457.2/2.,50./2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='hodo_flange_inner',
          mother=mother,
          description='Vacuum Chamber Shim Flange cutout shape',
          pos=[0,0,0.],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ee0000',
          g4type='Box',
          dimensions=[650.350/2.,334.8/2.,50.01/2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='hodo_flange_only',
          mother=mother,
          description='Vacuum Chamber Shim Flange',
          pos=[0,0,0],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Operation: hodo_flange_outer - hodo_flange_inner",
          material='Component')
    g_en.add(geo)

    extrusion_width = 25.
    extrusion_height = 35.
    extrusion_depth = 15.
    # 650.35/2 = inner edge of flange. 118.675 = distance of side to inner edge.
    extrusion_x_1 = 650.350/2.- 118.675 - extrusion_width/2.
    # 95.0 is the distance between sides of the two extrusions.
    extrusion_x_2 = 650.350/2.- 118.675 - extrusion_width- 95.0 -extrusion_width/2.
    # 334.8/2 = inner edge of flange.
    extrusion_y_1 = (334.8-extrusion_height)/2
    geo = Geometry(
          name = 'Flange_extrusion_1',
          mother=mother,
          description='welded on extrusion to bolt sintillator assembly onto',
          pos=[extrusion_x_1,extrusion_y_1,-1.],
          pos_units=['mm','mm','mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Box",
          dimensions=[extrusion_width/2.,extrusion_height/2.,extrusion_depth/2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name = 'Flange_extrusion_2',
          mother=mother,
          description='welded on extrusion to bolt sintillator assembly onto',
          pos=[extrusion_x_1,-extrusion_y_1,-1.],
          pos_units=['mm','mm','mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Box",
          dimensions=[extrusion_width/2.,extrusion_height/2.,extrusion_depth/2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name = 'Flange_extrusion_3',
          mother=mother,
          description='welded on extrusion to bolt sintillator assembly onto',
          pos=[extrusion_x_2,extrusion_y_1,-1.],
          pos_units=['mm','mm','mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Box",
          dimensions=[extrusion_width/2.,extrusion_height/2.,extrusion_depth/2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name = 'Flange_extrusion_4',
          mother=mother,
          description='welded on extrusion to bolt sintillator assembly onto',
          pos=[extrusion_x_2,-extrusion_y_1,-1.],
          pos_units=['mm','mm','mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Box",
          dimensions=[extrusion_width/2.,extrusion_height/2.,extrusion_depth/2.],
          dims_units=['mm', 'mm', 'mm'],
          material='Component')
    g_en.add(geo)


    geo = Geometry(
          name='hodo_flange_ex1',
          mother=mother,
          description='Vacuum Chamber Shim Flange',
          pos=[0,0,0],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Operation: hodo_flange_only + Flange_extrusion_1",
          material='Component')
    g_en.add(geo)


    geo = Geometry(
          name='hodo_flange_ex2',
          mother=mother,
          description='Vacuum Chamber Shim Flange',
          pos=[0,0,0],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Operation: hodo_flange_ex1 + Flange_extrusion_2",
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='hodo_flange_ex3',
          mother=mother,
          description='Vacuum Chamber Shim Flange',
          pos=[0,0,0],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Operation: hodo_flange_ex2 + Flange_extrusion_3",
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='hodo_flange',
          mother=mother,
          description='Vacuum Chamber Shim Flange',
          pos=[origin[0],origin[1],origin[2]+front_flange_center_z],
          pos_units=['mm', 'mm', 'mm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffffbb',
          g4type="Operation: hodo_flange_ex3 + Flange_extrusion_4",
          material='Aluminum')
    g_en.add(geo)

    # The arms (bras) are made of expoxy. Suggested material is G10-FR4: https://en.wikipedia.org/wiki/FR-4
    # https://indico.cern.ch/event/568427/contributions/2336556/attachments/1366767/2070787/8667K43_Properties_4.pdf
    # http://personalpages.to.infn.it/~tosello/EngMeet/ITSmat/SDD/SDD_G10FR4.html
    # from: https://www.phenix.bnl.gov/~suhanov/ncc/geant/rad-source/src/ExN03DetectorConstruction.cc
    # G4Material* SiO2 =
    # new G4Material("quartz",density= 2.200*g/cm3, ncomponents=2);
    # SiO2->AddElement(Si, natoms=1);
    # SiO2->AddElement(O , natoms=2);
    #//from http://www.physi.uni-heidelberg.de/~adler/TRD/TRDunterlagen/RadiatonLength/tgc2.htm
    #//Epoxy (for FR4 )
    #density = 1.2*g/cm3;
    #G4Material* Epoxy = new G4Material("Epoxy" , density, ncomponents=2);
    #Epoxy->AddElement(H, natoms=2);
    #Epoxy->AddElement(C, natoms=2);
    #//FR4 (Glass + Epoxy)
    #density = 1.86*g/cm3;
    #G4Material* FR4 = new G4Material("FR4"  , density, ncomponents=2);
    #FR4->AddMaterial(SiO2, fractionmass=0.528);
    #FR4->AddMaterial(Epoxy, fractionmass=0.472);

    arms_block_dy_pz = 35.     # Epoxy block, delta y, at +z
    arms_block_dy_mz = 37.071  # Epoxy block, delta y, at -z
    arms_block_dz    = 15.     # Depth of block.
    arms_block_dx    = 25.     # Width of block.
    tan_skew_angle = (arms_block_dy_mz -arms_block_dy_pz)/(2*arms_block_dz)
    skew_angle = math.atan(tan_skew_angle)*180./math.pi
    arms_block_y_loc = 334.8/2 - arms_block_dy_pz/2. - arms_block_dz/2 * tan_skew_angle
    # 1/2 Angle of top (bottom) of the block trapezoid

    geo = Geometry(
            name = "arms1_block1",
            mother=mother,
            description="Block at left top flange side of epoxy arm",
            pos=[origin[0]+extrusion_x_1,origin[1]+arms_block_y_loc,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='ff0000',
            g4type="G4Trap",
            dimensions=[arms_block_dz/2.,skew_angle,90.,arms_block_dy_mz /2.,arms_block_dx/2.,arms_block_dx/2.,0.,
                        arms_block_dy_pz /2.,arms_block_dx/2.,arms_block_dx/2.,0.],
            dims_units=['mm', 'deg','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
            name = "arms1_block2",
            mother=mother,
            description="Block right top at flange side of epoxy arm",
            pos=[origin[0]+extrusion_x_2,origin[1]+arms_block_y_loc,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='ff0000',
            g4type="G4Trap",
            dimensions=[arms_block_dz/2.,skew_angle,90.,arms_block_dy_mz /2.,arms_block_dx/2.,arms_block_dx/2.,0.,
                        arms_block_dy_pz /2.,arms_block_dx/2.,arms_block_dx/2.,0.],
            dims_units=['mm', 'deg','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
            name = "arms1_block3",
            mother=mother,
            description="Block left bottom at flange side of epoxy arm",
            pos=[origin[0]+extrusion_x_1,origin[1]-arms_block_y_loc,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='ff0000',
            g4type="G4Trap",
            dimensions=[arms_block_dz/2.,-skew_angle,90.,arms_block_dy_mz /2.,arms_block_dx/2.,arms_block_dx/2.,0.,
                        arms_block_dy_pz /2.,arms_block_dx/2.,arms_block_dx/2.,0.],
            dims_units=['mm', 'deg','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
            name = "arms1_block4",
            mother=mother,
            description="Block right bottom at flange side of epoxy arm",
            pos=[origin[0]+extrusion_x_2,origin[1]-arms_block_y_loc,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='ff0000',
            g4type="G4Trap",
            dimensions=[arms_block_dz/2.,-skew_angle,90.,arms_block_dy_mz /2.,arms_block_dx/2.,arms_block_dx/2.,0.,
                        arms_block_dy_pz /2.,arms_block_dx/2.,arms_block_dx/2.,0.],
            dims_units=['mm', 'deg','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
            name = "cross_bar_top",
            mother=mother,
            description="cross bar block of epoxy arm",
            pos=[origin[0]+(extrusion_x_1+extrusion_x_2)/2.,origin[1]+334.8/2-8./2,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='ff0000',
            g4type="Box",
            dimensions=[95./2.,8./2.,arms_block_dz/2.],
            dims_units=['mm', 'mm', 'mm'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "cross_bar_bottom",
        mother=mother,
        description="cross bar block of epoxy arm",
        pos=[origin[0]+(extrusion_x_1+extrusion_x_2)/2.,origin[1]-334.8/2+8./2,
            origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='ff0000',
        g4type="Box",
        dimensions=[95./2.,8./2.,arms_block_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)

    # Support Arm:
    support_arm_dz = 217.0
    support_arm_dx =   6.0
    support_arm_dy_pz = 32.271

    tmp_top_rise_tan_angle = 30.729/203.
    tmp_top_end_location = support_arm_dy_pz + support_arm_dz * tmp_top_rise_tan_angle  # y location of top at mz
    tmp_bot_rise_tan_angle = 40.608/208.908
    tmp_bot_end_location = support_arm_dz * tmp_bot_rise_tan_angle  # y location of bottom at mz
    support_arm_dy_mz = tmp_top_end_location - tmp_bot_end_location #
    pos_at_block_bottom_left =[origin[0]+extrusion_x_1,origin[1]-334.8/2.,origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz]
    mid_point_z = support_arm_dz/2
    support_arm_skew_angle = (math.atan(tmp_top_rise_tan_angle)+math.atan(tmp_bot_rise_tan_angle))/2
#    support_arm_skew_angle = 0
    mid_point_y = arms_block_dy_mz - support_arm_dy_pz/2. + mid_point_z*math.tan( support_arm_skew_angle)
    pos_support_arm_bot_left = [pos_at_block_bottom_left[0],pos_at_block_bottom_left[1]+mid_point_y,pos_at_block_bottom_left[2]-mid_point_z]

    geo = Geometry(
        name = "support_arm_bottom_left",
        mother=mother,
        description="epoxy support arm on the left bottom",
        pos=pos_support_arm_bot_left,
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='ff0000',
        g4type="G4Trap",
        dimensions=[support_arm_dz/2.,-support_arm_skew_angle,90.,support_arm_dy_mz /2.,support_arm_dx/2.,support_arm_dx/2.,0.,
                    support_arm_dy_pz /2.,support_arm_dx/2.,support_arm_dx/2.,0.],
        dims_units=['mm', 'rad','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
        material='G10_FR4')
    g_en.add(geo)



if __name__ == "__main__":
################################################################################################
#
# Geometry engine setup for local running
#
#################################################################################################

    Detector = "hps_hodo"
    Variation= "original"

    geo_en = GeometryEngine(Detector)

    try:
        z_location
    except NameError:
        z_location = 0
    origin=[21.17,0,z_location]

    calculate_hodo_support_geometry(geo_en,origin=origin,mother="world_volume")
    print("geo_en     length = ",len(geo_en._Geometry))
#    print "geo_en_vac length = ",len(geo_en_vac._Geometry)
    #
    # Now write the tables to the MySQL database
    #

    if args.mysql:
        geo_en.MySQL_OpenDB(args.host,args.user,args.passwd,args.database)
        geo_en.MySQL_New_Table(Detector)
        geo_en.MySQL_Write_Geometry()

    if args.txt:
        geo_en.TXT_Write(Variation)

    if args.gdml or args.show:
        from GeometryROOT import GeometryROOT
        rr = GeometryROOT("world_volume")
        rr.Create_root_volume()
        rr.Build_volumes(geo_en)
        if args.gdml:
            rr._geom.Export(Detector+".gdml")
            post_process_gdml_file(Detector+".gdml")
        if args.show:
            topvol = rr.Draw("ogl")
            print("Type return to end program.")
            ans=input("...")
