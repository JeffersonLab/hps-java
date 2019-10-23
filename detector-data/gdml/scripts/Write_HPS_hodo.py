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
    parser.add_argument('-r','--root',action='store_true',help='Produce a ROOT file for output.')
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
    front_flange_dz = 50.
    front_flange_inner_dy = 334.8
    front_flange_inner_dx = 650.350
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
          dimensions=[768.35/2,457.2/2,front_flange_dz/2.],
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
          dimensions=[front_flange_inner_dx/2.,front_flange_inner_dy/2.,(front_flange_dz+.01)/2.],
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
          col='cccccc',
          g4type="Operation: hodo_flange_outer - hodo_flange_inner",
          material='Component')
    g_en.add(geo)

    extrusion_width = 25.
    extrusion_height = 35.
    extrusion_depth = 15.
    # 650.35/2 = inner edge of flange. 118.675 = distance of side to inner edge.
    extrusion_x_1 = front_flange_inner_dx/2.- 118.675 - extrusion_width/2.
    # 95.0 is the distance between sides of the two extrusions.
    extrusion_x_2 = front_flange_inner_dx/2.- 118.675 - extrusion_width- 95.0 -extrusion_width/2.
    # front_flange_inner_dy/2 = inner edge of flange.
    extrusion_y_1 = (front_flange_inner_dy-extrusion_height)/2
    extrusions_center = (extrusion_x_1+extrusion_x_2)/2.
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
          col='cccccc',
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
    arms_block_y_loc = front_flange_inner_dy/2 - arms_block_dy_pz/2. - arms_block_dz/2 * tan_skew_angle
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
            col='e5a575',
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
            col='e5a575',
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
            col='e5a575',
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
            col='e5a575',
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
            pos=[origin[0]+extrusions_center,origin[1]+front_flange_inner_dy/2-8./2,
                origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='d6a678',
            g4type="Box",
            dimensions=[95./2.,8./2.,arms_block_dz/2.],
            dims_units=['mm', 'mm', 'mm'],
            material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "cross_bar_bottom",
        mother=mother,
        description="cross bar block of epoxy arm",
        pos=[origin[0]+extrusions_center,origin[1]-front_flange_inner_dy/2+8./2,
            origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz/2.],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
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
    pos_at_block_bottom_left =[origin[0]+extrusion_x_1,origin[1]-front_flange_inner_dy/2.,origin[2]+front_flange_center_z-1.-extrusion_depth/2.-arms_block_dz]
    pos_at_block_top_left =[origin[0]+extrusion_x_1,origin[1]+front_flange_inner_dy/2.,pos_at_block_bottom_left[2]]
    mid_point_z = support_arm_dz/2
    support_arm_skew_angle = (math.atan(tmp_top_rise_tan_angle)+math.atan(tmp_bot_rise_tan_angle))/2
    print("Arm angles: bottom={}   top={}   skew={}".format(math.degrees(math.atan(tmp_bot_rise_tan_angle)),
        math.degrees(math.atan(tmp_top_rise_tan_angle)),math.degrees(support_arm_skew_angle)))
#    support_arm_skew_angle = 0
    mid_point_y = arms_block_dy_mz - support_arm_dy_pz/2. + mid_point_z*math.tan( support_arm_skew_angle)
    pos_support_arm_bot_left = [pos_at_block_bottom_left[0],pos_at_block_bottom_left[1]+mid_point_y,pos_at_block_bottom_left[2]-mid_point_z]
    pos_support_arm_bot_right = [origin[0]+extrusion_x_2,pos_support_arm_bot_left[1],pos_support_arm_bot_left[2]]
    pos_support_arm_top_left = [pos_at_block_bottom_left[0],pos_at_block_top_left[1]-mid_point_y,pos_at_block_top_left[2]-mid_point_z]
    pos_support_arm_top_right = [origin[0]+extrusion_x_2,pos_support_arm_top_left[1],pos_support_arm_top_left[2]]


    geo = Geometry(
        name = "support_arm_bottom_left_arm",
        mother=mother,
        description="epoxy support arm on the left bottom, arm component",
        pos=[0,0,0],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="G4Trap",
        dimensions=[support_arm_dz/2.,-support_arm_skew_angle,90.,support_arm_dy_mz /2.,support_arm_dx/2.,support_arm_dx/2.,0.,
                    support_arm_dy_pz /2.,support_arm_dx/2.,support_arm_dx/2.,0.],
        dims_units=['mm', 'rad','deg','mm', 'mm','mm','deg','mm', 'mm','mm','deg'],
        material='Component')
    g_en.add(geo)

    end_block_dz = 14.
    end_block_dx = 20.
    end_block_dy =  5. + 2.8
    # Position of the block is relative to the *center* of the arm. In z, that is 1/2 the arm length.
    # In y, this is trickier due to the slant. The CAD measured bottom edge at the flange end (+z) of the block
    # to the bottom edge at the flange end of the arm is 55.2 mm in y, 203.0 mm in z.
    # mid_point_z*math.tan( support_arm_skew_angle) is the y distance of the mid point in y at +z of the arm, to
    # the center y of the arm. So 55.2 minus that distance minus the 1/2 height at +z
    # (support_arm_dy_pz /2.) gives the position of the bottom of the block.
    relative_end_block_y = 55.2 -mid_point_z*math.tan( support_arm_skew_angle) -support_arm_dy_pz /2. + end_block_dy/2.
    relative_end_block_z =  -mid_point_z + end_block_dz/2.

    geo = Geometry(
        name = "support_arm_bottom_left_notch",
        mother=mother,
        description="notch at the end of the epoxy support arm, bottom left.",
        pos=[0,relative_end_block_y+5.,(relative_end_block_z-0.01)],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="Box",
        dimensions=[end_block_dx/2,(end_block_dy+10.)/2,(end_block_dz+0.01)/2],
        dims_units=['mm', 'mm', 'mm'],
        material='Component')
    g_en.add(geo)

    geo = Geometry(
        name = "support_arm_bottom_left_with_notch",
        mother=mother,
        description="epoxy support arm with notch subtracted, bottom left",
        pos=[0,0,0],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='ffff00',
        g4type="Operation: support_arm_bottom_left_arm - support_arm_bottom_left_notch",
        material='Component')
    g_en.add(geo)


    geo = Geometry(
        name = "end_block_bottom_left_block",
        mother=mother,
        description="end block of epoxy arm bottom left",
        pos=[0,0,0],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="Box",
        dimensions=[end_block_dx/2,end_block_dy/2,end_block_dz/2],
        dims_units=['mm', 'mm', 'mm'],
        material='Component')
    g_en.add(geo)

    end_block_ridge_dz = 3.0   # 3 mm ridge at +z side of block
    # relative_end_block_y-end_block_dy/2 = The bottom of the end block.
    # Thickness of block at subtracted part is 5 (since we do not have the 1mm notch in the support bar.)
    geo = Geometry(
        name = "end_block_bottom_left_subs",
        mother=mother,
        description="end block of epoxy arm bottom left subtract",
        pos=[0,5.- end_block_dy/2+10./2,-end_block_ridge_dz-0.01], # 3 mm ridge at the +z side of block.
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='ffff00',
        g4type="Box",
        dimensions=[(end_block_dx+0.01)/2,10./2,(end_block_dz+0.01)/2],
        dims_units=['mm', 'mm', 'mm'],
        material='Component')
    g_en.add(geo)

    geo = Geometry(
        name = "end_block_bottom_left",
        mother=mother,
        description="epoxy support arm on the left bottom",
        pos=[0,relative_end_block_y,relative_end_block_z],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='ffff00',
        g4type="Operation: end_block_bottom_left_block - end_block_bottom_left_subs",
        material='Component')
    g_en.add(geo)


    geo = Geometry(
        name = "support_arm_bottom_left_plus_block",
        mother=mother,
        description="epoxy support arm on the left bottom",
        pos=pos_support_arm_bot_left,
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="Operation: support_arm_bottom_left_with_notch + end_block_bottom_left",
        material='G10_FR4')
    g_en.add(geo)

    # geo = Geometry(
    #     name = "support_arm_bottom_left",
    #     mother=mother,
    #     description="epoxy support arm with block on the left bottom",
    #     pos=pos_support_arm_bot_left,
    #     pos_units=['mm', 'mm', 'mm'],
    #     rot=[0.0, 0.0, 0.0],
    #     rot_units=['deg', 'deg', 'deg'],
    #     col='d6a678',
    #     g4type="Operation: support_arm_bottom_left_plus_block - end_block_bottom_left_subs",
    #     material='G10_FR4')
    # g_en.add(geo)

    geo = Geometry(                               # Duplicate on the right
        name = "support_arm_bottom_right",
        mother=mother,
        description="epoxy support arm with block on the left bottom",
        pos=pos_support_arm_bot_right,
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="Operation: support_arm_bottom_left_with_notch + end_block_bottom_left",
        material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(                               # Duplicate rotated on the top left
        name = "support_arm_top_left",
        mother=mother,
        description="epoxy support arm with block on the left bottom",
        pos=pos_support_arm_top_left,
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 180.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d60000',
        g4type="Operation: support_arm_bottom_left_with_notch + end_block_bottom_left",
        material='G10_FR4')
    g_en.add(geo)


    geo = Geometry(                               # Duplicate rotated on the top right
        name = "support_arm_top_right",
        mother=mother,
        description="epoxy support arm with block on the left bottom",
        pos=pos_support_arm_top_right,
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 180.0],
        rot_units=['deg', 'deg', 'deg'],
        col='d6a678',
        g4type="Operation: support_arm_bottom_left_with_notch + end_block_bottom_left",
        material='G10_FR4')
    g_en.add(geo)

    usupport_bot_dz = 9.0
    usupport_bot_dy = 8.0
    usupport_dx = 160.0
    usupport_top_dz = 4.0
    usupport_top2_dz = 3.0
    usupport_top_dy = 18.0
#   usupport_x = origin[0] + extrusions_center
    # (pos_at_block_bottom_left[1]+ arms_block_dy_mz - support_arm_dy_pz) is the flange side lower edge of the arm.
    # The CAD measurement is 55.2 mm from that point to the bottom of the support block.
    # The support block is 5.0 mm thick at that point (5+2.8 - subtraction)
    #
    usupport_y = pos_at_block_bottom_left[1]+ arms_block_dy_mz - support_arm_dy_pz + 55.2 + 5. + usupport_bot_dy/2.
    # pos_at_block_bottom_left[2] + relative_end_block_z is z-center of end block in global coords.
    usupport_z = pos_at_block_bottom_left[2] - support_arm_dz + end_block_dz - end_block_ridge_dz - usupport_bot_dz/2.
    geo = Geometry(
        name = "u_support_bar_bottom",
        mother=mother,
        description="U support bar for hodoscope, bottom",
        pos=[origin[0]+extrusions_center,usupport_y,usupport_z-.01],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='e5a555',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_bot_dy/2.,usupport_bot_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "u_support_bar_upper1",
        mother=mother,
        description="U support bar for hodoscope plate1, bottom",
        pos=[origin[0]+extrusions_center,usupport_y+usupport_bot_dy/2+usupport_top_dy/2,usupport_z-usupport_bot_dz/2+usupport_top_dz/2],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='e5a555',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_top_dy/2.,usupport_top_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "u_support_bar_upper2",
        mother=mother,
        description="U support bar for hodoscope plate2, bottom",
        pos=[origin[0]+extrusions_center,usupport_y+usupport_bot_dy/2+usupport_top_dy/2,usupport_z+usupport_bot_dz/2-usupport_top2_dz/2],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='e5a555',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_top_dy/2.,usupport_top2_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)



    geo = Geometry(
        name = "u_support_bar_top",
        mother=mother,
        description="U support bar for hodoscope, bottom",
        pos=[origin[0]+extrusions_center,-usupport_y,usupport_z-0.01],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='00FF00',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_bot_dy/2.,usupport_bot_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "u_support_bar_upper3",
        mother=mother,
        description="U support bar for hodoscope plate1, top",
        pos=[origin[0]+extrusions_center,-(usupport_y+usupport_bot_dy/2+usupport_top_dy/2),usupport_z-usupport_bot_dz/2+usupport_top_dz/2],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='e5a555',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_top_dy/2.,usupport_top_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)

    geo = Geometry(
        name = "u_support_bar_upper4",
        mother=mother,
        description="U support bar for hodoscope plate2, top",
        pos=[origin[0]+extrusions_center,-(usupport_y+usupport_bot_dy/2+usupport_top_dy/2),usupport_z+usupport_bot_dz/2-usupport_top2_dz/2],
        pos_units=['mm', 'mm', 'mm'],
        rot=[0.0, 0.0, 0.0],
        rot_units=['deg', 'deg', 'deg'],
        col='e5a555',
        g4type="Box",
        dimensions=[usupport_dx/2.,usupport_top_dy/2.,usupport_top2_dz/2.],
        dims_units=['mm', 'mm', 'mm'],
        material='G10_FR4')
    g_en.add(geo)





def post_process_gdml_file(file,mother):
    """The LCSIM LCDD schema cannot handle the full GDML written by ROOT.
    This routine processes the file so that it can be included into an LCDD geometry. """
    import os
    import re
    os.rename(file,file+".bak")
    inf = open(file+".bak","r")
    of  = open(file,"w")
    for line in inf:
        if re.search('copynumber=',line):
            wr_line=re.sub('copynumber=".*"','',line)
        elif re.search('<solidref ref="'+mother+'"/>',line) or  re.search('.*<box name="'+mother+'"',line):
            # world_volume is the name of both the solidref and the volume, LCDD doesn't like that.
            wr_line=re.sub(mother,mother+'_solid',line)
        elif re.search('<box *name="'+mother+'.*"/>',line) or  re.search('.*<box name="'+mother+'"',line):
            wr_line= '' # Remove    <box name="tracking_volume" x="2000" y="2000" z="2000" lunit="cm"/>

        else:
            wr_line=line

        of.write(wr_line)


if __name__ == "__main__":
################################################################################################
#
# Geometry engine setup for local running
#
#################################################################################################

    Detector = "hps_hodoscope_assembly"
    Variation= "original"

    geo_en = GeometryEngine(Detector)

    try:
        from Write_HPS_ecal import Box_Start_z
        print("Box_Start_z = ",Box_Start_z)
    except NameError:
        print("Warning: z_location is set to zero.")
        z_location = 0
    origin=[21.17,0,Box_Start_z]

    calculate_hodo_support_geometry(geo_en,origin=origin,mother="tracking_volume")
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

    if args.gdml or args.show or args.root:
        from GeometryROOT import GeometryROOT
        rr = GeometryROOT("tracking_volume")
        rr.Create_root_volume()
        rr.Build_volumes(geo_en)
        if args.gdml:
            rr._geom.Export(Detector+".gdml")
            post_process_gdml_file(Detector+".gdml",mother="tracking_volume")
        if args.show:
            rr.CheckOverlaps(0.00001)
            topvol = rr.Draw("ogl")
            print("Type return to end program.")
            ans=input("...")
        if args.root:
            rr._geom.Export(Detector+".root")
