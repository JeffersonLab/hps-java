#!/usr/bin/env python
#
# Author: Maurik Holtrop (UNH)
# Date: June 2, 2011
# Update: Feb 11, 2012
# Update: Feb 12, 2014  -- GEMC 2.0 compatibility using GeometryEngine
# Update: Aug 1, 2018   -- Change to Python3 compatibility. Make GDML compatible output for LCDD.
#
# This script places the crystals for the Electromagnetic Calorimeter for the HPS experiment.
#
# Notes:
# 1.  The rotations in GEANT4 are not *perfectly* accurate, so to avoid detector overlaps a tiny air gap is needed.
#     The size of the gap is set with "tolerance = 0.001", so 1 micron gap.
#     This is somewhat realistic. The actual detector will have some tolerance as well.
#
# 2.  The actual detector will have 180 microns of foil between the crystals. This is not implemented here, so it is just a gap.
#
# 3.  The front face is supposed to be more or less flat, which it is in this simulation.
#
# 4.  To simplify the geometry, everything is in one big vacuum. This was different for versions before 5 (i.e. 4h), where
#     there was an explicit vacuum box with vacuum.
#
# 5. The latest version no longer includes the Vacuum enclosure calculations, since the values now come from the CAD drawings by
#    Emmanuel Rindel and incorporated into GEMC by Chad Civello of UNH.
#    The Vacuum enclosure is expected to be in a table called ecal_vacuum, which gets inserted into the table created by this script.
#
# Geometry Notes:
# Theta_x is the angle w.r.t the z-axis in the x direction, so it is a rotation around y by - the angle.
# Theta_y is the angle w.r.t the z-axis in the y direction, so it is a rotation around x by + the angle.
#
#
# Different approach from the ecal version 4:
#
# 1 Everything is in one big vacuum. This saves the vacuum box, which got rather complicated.
#
import math
#import MySQLdb
import sys
import argparse
#
sys.path.append(".")    # Allow Python to find the Rotations.py and GeometryEngine.py.
from Rotations import *
from GeometryEngine import Geometry, GeometryEngine, Sensitive_Detector

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
                This code will create TEXT or GDML tables for the HPS ECal.""",
                epilog="""For more information, or errors, please email: maurik@physics.unh.edu """)
    parser.add_argument('-N','--nocrystals',action='store_true',help='Do NOT write out the geometry for the crystals.')
    parser.add_argument('-s','--show',action='store_true',help='show the geometry using the ROOT TGeoManager mechanism.')
    parser.add_argument('-g','--gdml',action='store_true',help='Produce a GDML file for output.')
    parser.add_argument('-t','--txt',action='store_true',help='Produce a TEXT file for output.')
    parser.add_argument('-m','--mysql',action='store_true',help='Produce a MySQL table for output. You must also specify --host --database --user --password')
    parser.add_argument('-H','--host',help='Name of the database host computer',default="localhost")
    parser.add_argument('-D','--database',help='Name of the database',default="hps_2014")
    parser.add_argument('-u','--user',help='User name for the database',default="clasuser")
    parser.add_argument('-p','--passwd',help='Password for connecting to the database',default="")
    parser.add_argument('-d','--debug',action="count",help='Increase debug level by one.')
    parser.add_argument('-z','--extraz',type=float,help='Amount by which to shift ECAL in z direction')
    args = parser.parse_args(sys.argv[1:])

#
# Table Names.
#
Standard_Table_Name="hps_ecal"

#################################################################################################
#
# Lots of Global parameters.
#
# This is kind of ugly, but also the most convenient way to control the geometry of the ECAL.
#
# We use a set of calculation to compute the parameters for the geometry. Note that
# there are many parameters that depend on earlier defined variables.
#
#################################################################################################
#
# Z Location of entire ECAL system
#
# From Stepan: Drawings of Vacuum has the box length = 63.93" and it sticks out upstream by 12" == 51.93" = 1319 mm
#
# Box_Start_z = 51.93 * 25.4;  # Location of the FRONT of the box, in the root system. Determined by the exit flange of magnet.
Box_Start_z = 1318.0 + 50.0  # Value from Holly was 1316, but this causes a 2mm overlap with Sho's SVT vacuum box.
#
#  50 mm extra to make space for the hodoscope flange.
#
x_location_crystal_front = 21.38 # From CAD drawing 2012/01/31
# Location at front of crystals. The entire array of crystals is centered on the photon line at entrance point.
## OLD VALUE = 20.77

if __name__ == "__main__" and args.extraz:
    Box_Start_z += args.extraz


z_crystal_added = 23. # From the CAD drawing, the crystals are moved back from origin by 23mm. CAD drawings 2012/01/31

#
# Materials Used
#
StainlessSteel='StainlessSteel'
Aluminum='G4_Al'
Copper='G4_Cu'

z_location_crystal_front = Box_Start_z + 55. + z_crystal_added # Location of the FRONT of the row 1 crystals, from TARGET.
# The additional space fits the vacuum front flange and space for the light fibers.
#
Box_depth = 450.            # Size of the calorimeter box in the z-direction. From ACAD drawing.            2011/6/15
Box_height = 457.2          # Size of the front flange height (y)         Corresponds with ACAD drawing 2011/6/15
Box_width_front = 768.35   # Size of the front flange width  (x)         Corresponds with ACAD drawing 2011/6/15
Box_width_back = 820.     # Size of the rear of the box.  (x)
Box_plate_material=Aluminum

# The half sizes are useful due to G4 using 1/2 widths.
Box_Half_depth = Box_depth / 2.
Box_Half_width_front = Box_width_front / 2.
Box_Half_width_back = Box_width_back / 2.

#Include the honeycomb block?
Include_Honeycomb_block=True

#
# The y "stack" from the beam is made up of:
#    Vacuum_gap_y
#    Vacuum_plate_thickness
#    Vacuum_foils_thickness
#
Vacuum_gap_y = 8.                  # mm size of the vaccum gap for main chaimber.
Vacuum_plate_thickness = 3.        # Thickness of the main vacuum chamber plate wall
Vacuum_foils_thickness = 3.        # Thickness of the additional insulation foils and cooling pipes and stuff (equivalent thickness).
Vacuum_plate_material = StainlessSteel # Aternative is 'StainlessSteel' or 'Aluminum'

y_offset = 20.                 # mm Opening in Y of CRYSTALS. This determines the y location of the crystals.

if Vacuum_gap_y + Vacuum_plate_thickness + Vacuum_foils_thickness > y_offset:
    print("WARNING: Stack of material and vacuum gap is larger than crystal offset. ")
    exit

#
# Control Parameters for placement of the crystals
#
Cut_Crystals = 2  # Cut some crystals in half or if set to 2, eliminate these crystals
CUT_CRYSTAL_MIN = -9 # lowest index crystal to eliminate (on the -x side, so beam right looking down the beam tube)
CUT_CRYSTAL_MAX = -1 # highest index crystal to eliminate.

CUT_CRYSTAL_SPACE = 11 # mm additional spacing between last cut crystal and the aluminum of the vacuum bulge.
Bulge_Crystal_Space = 2

# Visual - Determine the colors of the crystals, ((1,2 alternate y), (3,4 aleternate y) alternate x)
crys_col1 = "BBBBFF"
crys_col2 = "AAAAE0"
crys_col3 = "BBA7FF"
crys_col4 = "AAA7E5"

# Determine parameters for the vacuum plates.
#
Bulge_Plate_thickness = 3.
Plate_depth = 240 / 2;           # (z_location_crystal_front - Box_Start_z + 200.)/2;

#
# See HPS_Experiment notebook.
#
#Photon_pipe_loc = 18.0570  # Location at the FRONT.
Photon_pipe_loc = 19.09  # Location at the FRONT of ECAL, i.e. at 1319 mm from target.
Photon_pipe_dy = 11 # Inner radius
Photon_pipe_dy2 = 13 # Outer radius
Photon_pipe_angle = 0.0305 ##
Photon_pipe_flare_angle = 0

#
# Side support
#
Plate_side_thickness = 10.
#
# Front flange
#
Front_flange_half_thickness = 10. # 2cm

#
# Fundamental parameters which are unlikely to ever change.
#
# Geometry of a Inner Calorimeter PbWO4 crystal is:
#  front face = 2*6.665 x 2*6.665 mm
#  read face  = 2*8.    x 2*8     mm
#  length     = 2*80mm
#
# So the point of convergence is: 2*80 *6.665/(8-6.665) = 798.80149 mm from the front face.
#
foils_thickness = 0.162  #0.180                   ## 180 micrometers of foil between crystals.
spacing_foil_thickness = 0.000
x_gap_center = 0.001  # Setting this creates a center gap in between the the two halves
#
# Tolerance is set so that crystals do not overlap adjacent volumes.
# When the space in between the crystals is empty, and foils_thickness > 0, it can be set to zero.
tolerance = .000 # A 1 micrometer gap between volumes.
#
# Number of PbWO4 crystals in the y and x (1/2 front) direction.
#
max_y = 5
max_x = 23
#
# PbWO4 crystals dimensions.
#
crystal_front_half_width = 6.665
crystal_front_half_height = 6.665
crystal_back_half_width = 8.
crystal_back_half_height = 8.
crystal_half_length = 80.

#################################################################################
#
# Useful computed quantities.
#
#################################################################################

Vacuum_bulge = y_offset - (Vacuum_gap_y + Vacuum_plate_thickness) + 2 * (crystal_front_half_width + foils_thickness / 2) - Bulge_Crystal_Space
Electron_pipe_dy = Vacuum_gap_y + Vacuum_plate_thickness + Vacuum_bulge - Bulge_Plate_thickness #
Electron_pipe_dy2 = Vacuum_gap_y + Vacuum_plate_thickness + Vacuum_bulge
Electron_pipe_dx = Electron_pipe_dy #
Electron_pipe_dx2 = Electron_pipe_dy2

Electron_pipe_loc = x_location_crystal_front + (CUT_CRYSTAL_MIN) * 2 * (crystal_front_half_width + foils_thickness / 2) + Electron_pipe_dx + CUT_CRYSTAL_SPACE
Electron_pipe2_loc = x_location_crystal_front + (CUT_CRYSTAL_MAX) * 2 * (crystal_front_half_width + foils_thickness / 2) - Electron_pipe_dx - CUT_CRYSTAL_SPACE

Electron_pipe_angle = CUT_CRYSTAL_MIN * math.atan((crystal_back_half_width - (crystal_front_half_width + foils_thickness / 2)) / crystal_half_length)
Electron_pipe2_angle = CUT_CRYSTAL_MAX * math.atan((crystal_back_half_width - (crystal_front_half_width + foils_thickness / 2)) / crystal_half_length)

Box_Front_space = z_location_crystal_front - Box_Start_z # Space from front of box to start of crystals.

########################################
#
# Useful calculated quantities:
#
ff = 2 * crystal_half_length * (crystal_front_half_width + foils_thickness / 2) / (crystal_back_half_width - crystal_front_half_width)   # distance to Focal point of trapezoids to front face.
theta = math.atan((crystal_back_half_width - crystal_front_half_width) / (2 * crystal_half_length)) # Angle of slope of crystal = 1/2 angle beteen adjacent crystals.
mid = ff + crystal_half_length

xcent_shift = (crystal_back_half_width + (crystal_front_half_width + foils_thickness / 2)) / 2
ycent_shift = (crystal_back_half_height + (crystal_front_half_height + foils_thickness / 2)) / 2
inter_layer_space = 0.2
#
# Offsets - these define the gap between the 4 groups of crystals.
#

z_location = z_location_crystal_front + Box_Half_depth - Box_Front_space; # 1350  # Location of the CENTER of the calorimeter box.
# crystal_front_half_width is added later to place the central crystals against each other fo offset=0
z_location_crystal_offset = -Box_Half_depth + Box_Front_space   # Location of the front of the crystals inside the IC box.

z_location_crystalbox_offset = -Box_Half_depth + Box_Front_space + crystal_half_length + 2 #Location of the crystal box

crystalbox_xpos = 2.05 #will need to change if crystals move,2.1
crystalbox_ypos = 8.1 #this is where layer 1 of top level is, all layers rel to this
crystalbox_ypos_bottom = crystalbox_ypos-12.2 #top full layer of the bottom tray stack

cu_pipe_ir = 0.3 #inner radius of cu cooling pipes
cu_pipe_or = 0.4 #outer radius of cu cooling pipes

#
# Placement of the IC volume determines the location of the ecal in the experiment.
# The front face of the ecal is at -150*mm so 1350*mm in z means that the front face is at 1200*mm in global coords.
# The AnaMagnet is at +450*mm, so it's pole face is at 450+500= 950*mm. The front face is then 25 cm from the ecal.
#
def print_parameters():

#
    print("ff          = " + str(ff))
    print("theta       = " + str(theta))
    print("xcent_shift = " + str(xcent_shift))
    print("ycent_shift = " + str(ycent_shift))

    print("KEY Dimensions \n")
    print("Box_start_z               = " + str(Box_Start_z))
    print("Box_Front_space           = " + str(Box_Front_space))
    print("z_location of box center  = " + str(z_location))
    print("z_location_crystal_front  = " + str(z_location_crystal_front))
    print("x_location_crystal_front  = " + str(x_location_crystal_front))
    print("y_offset (Opening crystals)=" + str(y_offset))
    print("")
    print("Below numbers are actual values in mm")
    print("Vacuum gap                = " + str(Vacuum_gap_y * 2))
    print("Angle to edge of crystal  = " + str(math.atan(y_offset / z_location_crystal_front)))
    print("Vacuum Plate_thickness    = " + str(Vacuum_plate_thickness))
    print("Plate Material            = " + Vacuum_plate_material)
    print("Photon hole bore radius   = " + str(Photon_pipe_dy));
    print("Electron hole bore radius = " + str(Electron_pipe_dy) + ", " + str(Electron_pipe_dx))
    print("Electron hole bore outer  = " + str(Electron_pipe_dy2) + ", " + str(Electron_pipe_dx2))
    print("")

    print("Electron pipe  angle      = " + str(Electron_pipe_angle))
    print("Electron pipe  location   = " + str(Electron_pipe_loc))
    print("Electron pipe2 angle      = " + str(Electron_pipe2_angle))
    print("Electron pipe2 location   = " + str(Electron_pipe2_loc))

    print("Photon pipe angle         = " + str(Photon_pipe_angle))
    print("Photon pipe location      = " + str(Photon_pipe_loc))
    print("")
    print("Vacuum bulge: ")
    print("Plate thickness           = " + str(Electron_pipe_dy2 - Electron_pipe_dy))
    print("")
    print("")



def create_ecal_sensitive_detector():
    """ Create the definitions for the ECAL sensitive volume.
        This is used by calculate_ecal_geometry() to define the parameters for the sensitivity and identity of the crystals.
        A Sensitive_Detector object is returned and must be placed onto the GeometryEngine object."""
#
# Add the definitions for the Crystal sensitive detector.
#
    ecal_sens = Sensitive_Detector(name="ECAL",
                                   description="ECAL crystals",
                                   identifiers="idx idy",
                                   signalThreshold="1*MeV",
                                   timeWindow="80*ns",
                                   prodThreshold="1000*eV",
                                   maxStep="1*mm",
                                   riseTime="5*ns",
                                   fallTime="20*ns",
                                   mvToMeV="1",
                                   pedestal="0",
                                   delay="0*ns",
                                   bankId=600)

    ecal_sens.add_bank_row("idx","Horizontal index for crystal",1,"Di")
    ecal_sens.add_bank_row("idy","Vertical index for crystal",2,"Di")
    ecal_sens.add_bank_row("adc","ADC value",3,"Di")
    ecal_sens.add_bank_row("tdc","TDC value ",4,"Di")
    ecal_sens.add_bank_row("hitn","Hit number",99,"Di")


    return(ecal_sens)

def calculate_ecal_mother_geometry(g_en,origin=[0,0,0],mother="ps_field",style=0,visible=0):
    """Create the ecal mother volume, which is a simple box."""

    if not isinstance(g_en,GeometryEngine):
        print("ERROR, I expected a GeometryEngine object as argument to create_ecal_sensitive_detector.")
        return()


    g_en.add(geom = Geometry(
                         name ="ECAL",
                         mother=mother,
                         description="ECAL mother volume",
                         pos=[origin[0],origin[1],z_location+origin[2]],
                         pos_units="mm",
                         rot=[0,0,0],
                         rot_units="rad",
                         col="ffff559",
                         g4type="Box",
                         dimensions=[Box_Half_width_back + 2 * Plate_side_thickness,
                                     Box_height / 2.,
                                     Box_Half_depth ],
                         dims_units="mm",
                         material="Vacuum",
                         style=style,
                         visible=visible
                         ))
#

def calculate_ecal_geometry(g_en,origin=[0,0,0],mother="ECAL",style=1):
    """ Calculate the geometry of the crystals of the ECAL.
        The crystals are placed in the ECAL mother volume, which must be created with calculate_ecal_mother_geometry(g_en) """

    if not isinstance(g_en,GeometryEngine):
        print("ERROR, I expected a GeometryEngine object as argument to create_ecal_sensitive_detector.")
        return()

################################################################################
#
# Find or setup the sensitive detector components.
#
################################################################################

    ecal_sens = g_en.find_sensitivity("ECAL")  # First see if it exists already.
    if ecal_sens == -1:
        ecal_sens = create_ecal_sensitive_detector()
        g_en.add_sensitivity(ecal_sens)

    ######################################################################################################

    def FindCrystalMinMax(coords, updown=1, backfront=1, leftright= -1):
        """Returns the x,y,z coordinates of the corner of a crystal.
           :param coords    =(x,y,z,thtx,thty,thtz) are coordinates and rotations of the crystal.
           :param updown    Set updown=-1 for lower corner or updown=+1 for upper corner,
           :param backfront and backfront=-1 for the front, and +1 for back,
           :param leftright leftright= 1 for left and -1 for right as seen from the front. So left is in +x direction!
        """
        (x_mid, y_mid, z_mid, thtx, thty, thtz) = coords
        lvec = 0
        if backfront > 0:
            lvec = np.matrix((leftright * crystal_back_half_width, updown * crystal_back_half_height, crystal_half_length)).T
        else:
            lvec = np.matrix((leftright * crystal_front_half_width, updown * crystal_front_half_height, -crystal_half_length)).T

        rot = Rotation().rotateG4XYZ((thtx, thty, thtz))
        lvec = rot * lvec
        lx = x_mid + lvec.item(0)
        ly = y_mid + lvec.item(1)
        lz = z_mid + lvec.item(2)

        #  print "mid   (x,y,z)=(%6.4f,%6.4f,%6.4f)"%(x_mid,y_mid,z_mid)
        #  print "vec   (x,y,z)=(%6.4f,%6.4f,%6.4f)"%(lvec.item(0),lvec.item(1),lvec.item(2))
        #  print "point (x,y,z)=(%6.4f,%6.4f,%6.4f)"%(lx,ly,lz)

        return(lx, ly, lz)

    def CosineOddSum(n, tht):
        """Returns the sum of cosines: cos(tht)/cos(tht-tht) + 2*cos(tht)/cos(2*tht) + 2*cos(tht)/cos(4*tht) + ... + cos(tht)/cos( (2n)*tht)
        This formula comes from cos(tht)+sin(tht) tan(tht+alhpa) = cos(alpha)/cos(tht+alhpa). Here tht is the angle of the trapezoid face
        with respect to the vertical (or horizontal) line disecting the midpoint of the face, alpha is the flare out angle of the trapezoid
        and fw times this formula would be the length of the vertical line. See HPS Experiment Notebook 'Ecal Crystal Placement - NEW' page 41."""

        cc = math.cos(tht)  # /cos(0) which is 1.
        for i in range(1, n + 1):
            cc = cc + 2 * math.cos(tht) / math.cos(2 * i * tht)

        return(cc)


    def CrystalLocation(nx, ny):
        """Find the location of crystal number nx,ny for the first quadrand (+x,+y). Returns (x,y,z,thetax,thetay,thetaz)
        The crystals are placed so that the first crystal has a corner at (0,0,0) of the mother volume (verified graphically
        The PlaceCrystal(nx,ny,xsign,ysign) will then put the correct offset on the positions."""

    # X-preferred stacking
    #
    # Rotate crystals so they sit on the x-y plane. Then rotate around y to place them side by side on this plane.
    # Finally, rotate the whole x-y place around x to the correct level.
    #
        theta_mod_x = math.atan((crystal_back_half_height - crystal_front_half_height) / (2 * crystal_half_length))
        rot = Rotation().rotateX(-theta_mod_x)
        #
        # Correct the step size of rotations around the y axis for the x placement (theta_mod_x) to account for rotation of top face of crystal
        #
        thtx = theta_mod_x * 2
        theta_mod_y = math.atan((crystal_back_half_width - crystal_front_half_width) / (2 * crystal_half_length * math.cos(thtx * 0.98)))
        #
        # Th3 0.97 factor is a minor correction for not being quite at the top. This will depend some on the size of the crystals.
        # Same for the small factors below.
        #
        thty = theta_mod_y * (2 * nx + 1)

        x = (crystal_front_half_width + foils_thickness / 2) * CosineOddSum(nx, theta_mod_y * 1.001) * 1.001 + crystal_half_length * math.sin(thty)
        y = (crystal_front_half_width + crystal_back_half_width) / 2
        z = crystal_half_length * math.cos(thtx) * math.cos(thty)

        rot = rot.rotateY(thty)

        #
        # Now rotate the entire stack around the x axis and increase the y by the correct amount.
        #
        thtx = theta_mod_x * 2 * ny
        #  y = y + math.sin(thty) * (ff+crystal_half_length)
        #
        rot = rot.rotateX(-thtx)
        #
        #
        prot = Rotation().rotateX(-thtx)
        pos = np.matrix((x, y, z)).T
        pos = prot * pos
        #
        #
        x = pos.item(0)
        y = pos.item(1)
        z = pos.item(2)

        if ny == 0:            # The first row needs the y_offset.
            y = y
        else:                  # All the next rows are referenced to the previous row.
            (mx, my, mz) = FindCrystalMinMax(CrystalLocation(max_x - 1, ny - 1), 1, 1, 1)
            y = y + my + spacing_foil_thickness / (math.cos(2 * ny * theta)) - mz * math.tan(2 * ny * theta)

        (thtx, thty, thtz) = rot.GetG4XYZ()

        x = x

        return(x, y, z, thtx, thty, thtz)

    def PlaceCrystal(nx, ny, lxsign, lysign):
        """ Compute the actual location of the crystal, including all the offsets and the correct quadrant. """

        (lx, ly, lz, ltht_x, ltht_y, ltht_z) = CrystalLocation(nx, ny)
        lx = lxsign * (lx + x_gap_center) + x_location_crystal_front
        ly = lysign * (ly + y_offset)
        lz = lz + z_location_crystal_offset
        ltht_x = lysign * ltht_x
        ltht_y = lxsign * ltht_y

        return(lx, ly, lz, ltht_x, ltht_y, ltht_z)

####################################################################################################################################
#
# Crystal placement
#
#####################################################################################################################################

    count = 0
#
# POS ranges over the 4 quadrants: (x,y) = ++, +-, --, -+
#

    for pos in range(4):

        for ny in range(max_y):

            if pos == 0 or pos == 3:
                iny = ny + 1
                ysign = 1
            else:
                iny = -ny - 1
                ysign = -1

            for nx in range(max_x):

                n = pos * max_y * max_x + ny * max_x + nx

                if pos == 0 or pos == 1:
                    inx = nx + 1  # identity
                    xsign = 1
                else:
                    inx = -nx  # identity
                    xsign = -1

                (crys_pos_x, crys_pos_y, crys_pos_z, crys_theta_x, crys_theta_y, crys_theta_z) = PlaceCrystal(nx, ny, xsign, ysign)
                if Cut_Crystals and ny == 0 and inx >= CUT_CRYSTAL_MIN and inx <= CUT_CRYSTAL_MAX:  # Row 1 top and bottom, 8 hottest crystals
                    crys_pos_y = crys_pos_y + ysign * ((crystal_front_half_width + foils_thickness / 2) - 2 * tolerance) / 2

                if ((xsign + 1) / 2 + nx) % 2 == 0:  # Color and Trans: CCCCCCT  T=0 -> 5 (max transparency)
                    if ny % 2 == 0:
                        crys_col = crys_col1
                    else:
                        crys_col = crys_col2
                else:
                    if ny % 2 == 0:
                        crys_col = crys_col3
                    else:
                        crys_col = crys_col4

                c_fhh = crystal_front_half_height
                c_bhh = crystal_back_half_height

                if Cut_Crystals==1 and ny == 0 and inx >= CUT_CRYSTAL_MIN and inx <= CUT_CRYSTAL_MAX:
#                    print "Cutting crystals, are ya?"  # Row 1 top and bottom, 8 hottest crystals, cut in 1/2
                    c_fhh = crystal_front_half_height / 2
                    c_bhh = crystal_back_half_height / 2

                if not (Cut_Crystals == 2 and ny == 0 and inx >= CUT_CRYSTAL_MIN and inx <= CUT_CRYSTAL_MAX):

                    g_en.add(geom=Geometry(
                                         name="ECAL_" + str(n),
                                         mother=mother,
                                         description="ECAL Crystal (" + str(inx) + "," + str(iny) + ")",
                                         pos=[crys_pos_x+origin[0], crys_pos_y+origin[1], crys_pos_z+origin[2]],
                                         pos_units="mm",
                                         rot=[crys_theta_x, crys_theta_y, crys_theta_z],
                                         rot_units="rad",
                                         col=crys_col,
                                         g4type="Trd",
                                         dimensions=[
                                                     crystal_front_half_width,
                                                     crystal_back_half_width,
                                                     c_fhh,
                                                     c_bhh,
                                                     crystal_half_length
                                                  ],
                                         dims_units="mm",
                                         material="G4_PbWO4",
                                         sensitivity=ecal_sens.sensitivity(),
                                         hittype=ecal_sens.hitType(),
                                         identity=ecal_sens.identity(inx,iny),
                                         style=style
                                         ))


                count += 1

def calculate_ecal_vacuum_geometry_minimal(g_en,origin=[0,0,0],mother="ECAL",style=1):
    """Transcribed from GDML. This creates the detailed vacuum system for the ECAL, with flanges."""
    ecal_vac_material=Aluminum

    # front_x = 0.0
    # front_z = 10.0
    back_x  = -147.505
    # back_z  = 440.0
    chamber_x = -140.828
    # chamber_z = 225.0
    # honeycomb_x = -210.0
    # honeycomb_z = 190.0
    # ecal_flange_x = 21.17
    # ecal_flange_z = 1318

    fl_visible=1
    fl_style=style

    g_en.add(Geometry(
                      name="front_flange",
                      description="The Front Flange",
                      mother=mother,
                      pos=[origin[0],origin[1],origin[2]-215.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[768.35/2.,457.2/2.,20./2.],
                      dims_units=["mm","mm","mm"],
                      col="ccccdd",
                      material=Aluminum,
                      style=fl_style,
                      visible=fl_visible
                      ))


def calculate_ecal_vacuum_geometry(g_en,origin=[0,0,0],mother="ECAL",style=1):
    """Transcribed from GDML. This creates the detailed vacuum system for the ECAL, with flanges."""
    ecal_vac_material=Aluminum

    # front_x = 0.0
    # front_z = 10.0
    back_x  = -147.505
    # back_z  = 440.0
    chamber_x = -140.828
    # chamber_z = 225.0
    # honeycomb_x = -210.0
    # honeycomb_z = 190.0
    # ecal_flange_x = 21.17
    # ecal_flange_z = 1318

    fl_visible=1
    fl_style=style

    g_en.add(Geometry(
                      name="front_flange_box",
                      description="The Front Flange of ECAL vacuum system",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[768.35/2.,457.2/2.,20./2.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="front_chamber_trap",
                      description="Trapezoid cutout of ECAL front flange",
                      mother=mother,
                      pos=[-146.309,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[30./2., -0.988, 0, 16/2., 331.198/2., 331.198/2., 0, 16/2., 334.064/2., 334.064/2., 0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="front_minus_chamber",
                      description="The Front Flange with chamber subtracted",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: front_flange_box - front_chamber_trap",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="front_egap_trap",
                      description="Trapezoid cutout of ECAL front flange",
                      mother=mother,
                      pos=[-44.683,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[30./2, -4.796, 0, 52.66/2., 25.683/2., 25.683/2.,0.,52.66/2.,29.716/2.,29.716/2.,0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="front_minus_egap",
                      description="The Front Flange with egap subtracted",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: front_minus_photontube - front_egap_trap",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="flange_photontube_inside",
                      description="Tube cutout of ECAL front flange",
                      mother=mother,
                      pos=[20.007,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,-1.748,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[11.,11.,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="front_minus_photontube",
                      description="The Front Flange with chamber and tube subtracted",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: front_minus_chamber - flange_photontube_inside",
                      material="Component"
                      ))


    g_en.add(Geometry(
                      name="flange_egap_inside_tube",
                      description="Tube cutout of ECAL front flange",
                      mother=mother,
                      pos=[-30.833,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0.956,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[26.33,26.33,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="front_minus_egapleft",
                      description="The Front Flange",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: front_minus_egap - flange_egap_inside_tube",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="flange_egap_inside_tube2",
                      description="Tube cutout of ECAL front flange",
                      mother=mother,
                      pos=[-58.532,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,8.594,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[26.33,26.33,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="front_flange",
                      description="The Front Flange",
                      mother=mother,
                      pos=[origin[0],origin[1],origin[2]-215.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: front_minus_egapleft - flange_egap_inside_tube2",
                      col="ccccdd",
                      material=Aluminum,
                      style=fl_style,
                      visible=fl_visible
                      ))

    g_en.add(Geometry(
                      name="back_flange_box",
                      description="The Back Flange of ECAL vacuum system",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[505./2.,160./2.,20./2.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="back_chamber_trap",
                      description="Trapezoid cutout of ECAL back flange",
                      mother=mother,
                      pos=[-153.726-back_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[30/2., -0.988, 0, 16/2., 372.279/2., 372.279/2., 0, 16./2., 375.145/2, 375.145/ 2.,0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="back_minus_chamber",
                      description="The Back Flange",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: back_flange_box - back_chamber_trap",
                      material="Component",
                      ))


    g_en.add(Geometry(
                      name="flange_photontube_inside2",
                      description="Tube cutout of ECAL front flange 2",
                      mother=mother,
                      pos=[33.130 - back_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,-1.748,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[11.,11.,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="back_minus_photontube",
                      description="The Back Flange",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: back_minus_chamber - flange_photontube_inside2",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="back_egap_trap",
                      description="Trapezoid cutout of ECAL back flange",
                      mother=mother,
                      pos=[-80.763-back_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[30./2., -4.796, 0, 52.66/2., 83.493/2., 83.493/2., 0, 52.66/2., 87.526/2., 87.526/2., 0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="back_minus_egap",
                      description="The Back Flange",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: back_minus_photontube - back_egap_trap",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="flange_egap_inside_tube3",
                      description="Tube cutout of ECAL front flange",
                      mother=mother,
                      pos=[-38.008-back_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0.956,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[26.33,26.33,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))


    g_en.add(Geometry(
                      name="back_minus_egapleft",
                      description="The Back Flange",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: back_minus_egap - flange_egap_inside_tube3",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="flange_egap_inside_tube4",
                      description="Tube cutout of ECAL front flange",
                      mother=mother,
                      pos=[-123.518-back_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,8.594,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[26.33,26.33,30.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="back_flange",
                      description="The Back Flange",
                      mother=mother,
                      pos=[origin[0]+back_x,origin[1],origin[2]+215.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: back_minus_egapleft - flange_egap_inside_tube4",
                      #material="Component",
                      col="ccccdd",
                      material=Aluminum,
                      style=fl_style,
                      visible=fl_visible
                      ))

    g_en.add(Geometry(
                      name="chamber_trap",
                      description="Trapezoid for ECAL vacuum chamber, slot part",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[450/2.,-1.864,0,28./2.,377./2.,377./2.,0,28./2.,406.29/2.,406.29/2.,0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="chamber_cutaway_box",
                      description="Ecal - box that makes front end thinner.",
                      mother=mother,
                      pos=[0,16.,-90.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[500./2.,10./2.,300./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_trim1",
                      description="ECAL vacuum with thinner front top",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_trap - chamber_cutaway_box",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="chamber_cutaway_box2",
                      description="Ecal - box that makes front end thinner",
                      mother=mother,
                      pos=[0,-16.,-90.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[500./2.,10./2.,300./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_trim2",
                      description="ECAL vacuum chamber with thinner front.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_trim1 - chamber_cutaway_box2",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="photontube_outside",
                      description="Tube bump up of chamber",
                      mother=mother,
                      pos=[26.569-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,-1.748,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[13.,13.,235.],
                      dims_units=["mm","mm","mm"],
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="chamber_with_photontube",
                      description="ECAL vacuum chamber with thinner front.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_trim2 + photontube_outside",
                      material="Component"
                     ))

    g_en.add(Geometry(
                      name="egap_outside_trap_upper",
                      description="Trapezoid top of ECAL chamber extension",
                      mother=mother,
                      pos=[-63.810-chamber_x,16.165,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[450./2.,-4.796,0.,32.33/2.,106.912/2.,52.344/2.,0.269,32.33/2.,167.411/2.,112.843/2.,0.269],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="chamber_with_egap_upper",
                      description="ECAL vacuum chamber with upper egap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_with_photontube + egap_outside_trap_upper",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="egap_outside_trap_lower",
                      description="Trapezoid top of ECAL chamber extension",
                      mother=mother,
                      pos=[-63.810-chamber_x,-16.165,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[450./2.,-4.796,0,32.33/2.,52.344/2.,106.912/2., -0.269,32.33/2.,112.843/2.,167.411/2., -0.269],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="chamber_with_egap_lower",
                      description="ECAL vacuum chamber with upper egap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_with_egap_upper + egap_outside_trap_lower",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="chamber_cutaway_box3",
                      description="Ecal - box that makes front end thinner",
                      mother=mother,
                      pos=[0,34.33,-90.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[500./2.,10./2.,300./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_with_egap_trimtop",
                      description="ECAL vacuum chamber with upper egap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_with_egap_lower -  chamber_cutaway_box3",
                      material="Component"
                      ))


    g_en.add(Geometry(
                      name="chamber_cutaway_box4",
                      description="Ecal - box that makes front end thinner",
                      mother=mother,
                      pos=[0,-34.33,-90.],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[500./2.,10./2.,300./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_with_egap_trimbot",
                      description="ECAL vacuum chamber with upper egap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_with_egap_trimtop -  chamber_cutaway_box4",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="back_end_box",
                      description="End box to cut clean end.",
                      mother=mother,
                      pos=[0,0,-230],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[768.35/2.,457.2/2.,50./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_outside_trim1",
                      description="ECAL vacuum chamber with clean front slice.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_with_egap_trimbot -  back_end_box",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="back_end_box2",
                      description="End box to cut clean end.",
                      mother=mother,
                      pos=[0,0,+230],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Box",
                      dimensions=[768.35/2.,457.2/2.,50./2.],
                      dims_units="mm",
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_outside_trim2",
                      description="ECAL vacuum chamber with clean front slice.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_outside_trim1 -  back_end_box2",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="photontube_inside",
                      description="Tube cutout of chamber",
                      mother=mother,
                      pos=[26.569-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,-1.748,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[11.,11.,235.],
                      dims_units=["mm","mm","mm"],
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="chamber_minus_photontube",
                      description="ECAL vacuum chamber with clean front slice.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_outside_trim2 -  photontube_inside",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="chamber_inside_trap",
                      description="Trapezoid cutout for ECAL chamber",
                      mother=mother,
                      pos=[-150.017-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[450.001/2.,-0.988,0,16./2.,331.676/2.,331.676/2.,0,16./2.,374.667/2.,374.667/2.,0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="chamber_minus_inside",
                      description="ECAL vacuum chamber with vac trap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_minus_photontube -  chamber_inside_trap",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="egap_inside_trap",
                      description="Trapezoid cutout for ECAL chamber",
                      mother=mother,
                      pos=[-62.723-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="G4Trap",
                      dimensions=[450.001/2.,-4.796 ,0 , 52.66/2. , 26.355/2. , 26.355/2., 0. ,52.66/2.,86.854/2.,86.854/2.,0],
                      dims_units=["mm","deg","deg","mm","mm","mm","deg","mm","mm","mm","deg"],
                      material="Component"))

    g_en.add(Geometry(
                      name="chamber_minus_egapinside",
                      description="ECAL vacuum chamber with vac trap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_minus_inside -  egap_inside_trap",
                      material="Component"
                      ))

    g_en.add(Geometry(
                      name="egap_inside_tube",
                      description="Tube cutout of ecal chamber",
                      mother=mother,
                      pos=[-34.4205-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0.956,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Eltu",
                      dimensions=[26.33,26.33,240.],
                      dims_units=["mm","mm","mm"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="chamber_minus_egap_left",
                      description="ECAL vacuum chamber with vac trap.",
                      mother=mother,
                      pos=[0,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_minus_egapinside -  egap_inside_tube",
                      material="Component"
                      ))


    g_en.add(Geometry(
                      name="egap_inside_tube2",
                      description="Tube cutout of ecal chamber",
                      mother=mother,
                      pos=[-91.025-chamber_x,0,0],
                      pos_units=["mm","mm","mm"],
                      rot=[0,8.594,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Tube",
                      dimensions=[0.,26.33,240.,0.,360.0001],
                      dims_units=["mm","mm","mm","deg","deg"],
                      material="Component",
                      ))

    g_en.add(Geometry(
                      name="ECAL_chamber",
                      description="ECAL vacuum chamber with vac trap.",
                      mother=mother,
                      pos=[origin[0]+chamber_x,origin[1],origin[2]],
                      pos_units=["mm","mm","mm"],
                      rot=[0,0,0],
                      rot_units=["deg","deg","deg"],
                      g4type="Operation: chamber_minus_egap_left -  egap_inside_tube2",
                      col="ccccdd",
                      material=Aluminum,
                      style=style,
                      visible=1
                        ))

    if Include_Honeycomb_block:
        geo = Geometry(
            name='al_honeycomb',
            mother=mother,
            description='Al honeycomb in vacuum box for support',
            pos=[origin[0]-210.0,origin[1], origin[2] -35.0],
            pos_units=['mm', 'mm', 'mm'],
            rot=[0.0, 0.0, 0.0],
            rot_units=['deg', 'deg', 'deg'],
            col='aaaaff',
            g4type='Box',
            dimensions=[3.0, 0.8, 3.0],
            dims_units=['cm', 'cm', 'cm'],
            material='AlHoneycomb',
            style=style)
        g_en.add(geo)


###############Crystal Box encasing ECal Crystals###############################
def calculate_ecal_crystalbox_geometry(g_en,mother="ECAL",origin=[0,0,0]):
    """This creates the frames that hold the crystals, including the structures on the sides. """

    geo = Geometry(
          name='ecal_box_outer1',
          mother=mother,
          description='Outer shell of rack holding crystals',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='ffbbcc',
          g4type='Box',
          dimensions=[40.0, 0.70, 10.05],
          dims_units=['cm', 'cm', 'cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ecal_box_inner1',
          mother=mother,
          description='Cut out inside for 1-5 stack',
          pos=[0.0, 0.16, 1.85],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbFFcc',
          g4type='Box',
          dimensions=[39.0, 1.0, 10.5],#z=8.2
          dims_units=['cm', 'cm', 'cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ecal_box_inner2',
          mother=mother,
          description='Cut out front of 1-5 crystal stack',
          pos=[0.0, 0.16, -10.05],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbFF',
          g4type='Box',
          dimensions=[34.0, 0.6, 3.71],
          dims_units=['cm', 'cm', 'cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ecal_box_minus_inner1',
          mother=mother,
          description='Cut out for 1-4 stack',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: ecal_box_outer1 - ecal_box_inner1',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ecal_box_minus_inner2',
          mother=mother,
          description='Cut out for 1-4 stack',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: ecal_box_minus_inner1 - ecal_box_inner2',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_0',
          mother=mother,
          description='Center ppd',
          pos=[0.0, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 0.0, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_1',
          mother=mother,
          description='1st ppd on right',
          pos=[-5.24, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, -3.87, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_2',
          mother=mother,
          description='2nd ppd on right',
          pos=[-10.5, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, -7.74, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_3',
          mother=mother,
          description='3rd ppd on right',
          pos=[-15.8, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, -11.62, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_4',
          mother=mother,
          description='4th ppd on right',
          pos=[-21.17, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, -15.49, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_5',
          mother=mother,
          description='5th ppd on right',
          pos=[-26.64, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, -19.36, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_6',
          mother=mother,
          description='1st ppd on left',
          pos=[5.24, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 3.87, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_7',
          mother=mother,
          description='2nd ppd on left',
          pos=[10.5, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 7.74, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_8',
          mother=mother,
          description='3rd ppd on left',
          pos=[15.8, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 11.62, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_9',
          mother=mother,
          description='4th ppd on left',
          pos=[21.17, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 15.49, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_10',
          mother=mother,
          description='5th ppd on left',
          pos=[26.64, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.3, 0.7, 0.75, 0.0, 19.36, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd',
          mother=mother,
          description='Add ppd0 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: ecal_box_minus_inner2 + ppd_0',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd1',
          mother=mother,
          description='Add ppd1 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd + ppd_1',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd2',
          mother=mother,
          description='Add ppd 2 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd1 + ppd_2',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd3',
          mother=mother,
          description='Add ppd3 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd2 + ppd_3',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd4',
          mother=mother,
          description='Add ppd4 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd3 + ppd_4',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd5',
          mother=mother,
          description='Add ppd5 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd4 + ppd_5',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd6',
          mother=mother,
          description='Add ppd6 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd5 + ppd_6',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd7',
          mother=mother,
          description='Add ppd7 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd6 + ppd_7',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd8',
          mother=mother,
          description='Add ppd8 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd7 + ppd_8',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='box_with_ppd9',
          mother=mother,
          description='Add ppd9 to box frame',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd8 + ppd_9',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_1_top',
          mother=mother,
          description='Adds ppd10 and makes layer 1',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_2_top',
          mother=mother,
          description='Adds ppd10 and makes layer 2',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos -1.41, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_3_top',
          mother=mother,
          description='Adds ppd10 and makes layer 3',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos -2.82, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_4_top',
          mother=mother,
          description='Adds ppd10 and makes layer 4',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos -4.23, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_1_bottom',
          mother=mother,
          description='Adds ppd10 and makes layer 1',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_2_bottom',
          mother=mother,
          description='Adds ppd10 and makes layer 2',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom -1.41, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_3_bottom',
          mother=mother,
          description='Adds ppd10 and makes layer 3',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom -2.82, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_4_bottom',
          mother=mother,
          description='Adds ppd10 and makes layer 4',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom -4.23, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: box_with_ppd9 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='ppd_left1',
          mother=mother,
          description='Bar along left of electron hole, front part',
          pos=[-1.65, 0.0, -9.3],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.251, 0.7, 0.75, 0.0, 0.97, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='ppd_left2',
          mother=mother,
          description='Bar along left of electron hole, back part',
          pos=[-1.903, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.2, 0.7, 10.05, 0.0, 0.97, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)


    geo = Geometry(
          name='ppd_right',
          mother=mother,
          description='Bar along right of electron hole',
          pos=[-14.3853, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[0.45, 0.7, 10.02, 0.0, -9.68, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a1_1',
          mother=mother,
          description='Adding left side of hole to 5th layer',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: ecal_box_minus_inner2 + ppd_left2',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a1',
          mother=mother,
          description='Adding left side of hole to 5th layer',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a1_1 + ppd_left1',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='electron_hole_left',
          mother=mother,
          description='Cut out on right to leave left side of 5th layer',
          pos=[-21.3615, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Trd',
          dimensions=[19.0885, 19.4285, 2.0, 2.0, 10.1],
          dims_units=['cm', 'cm', 'cm', 'cm', 'cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a2',
          mother=mother,
          description='Remove right side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a1 -electron_hole_left',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a3',
          mother=mother,
          description='Add ppd6 to left side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a2 + ppd_6',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a4',
          mother=mother,
          description='Add ppd7 to left side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a3 + ppd_7',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a5',
          mother=mother,
          description='Add ppd8 to left side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a4 + ppd_8',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a6',
          mother=mother,
          description='Add ppd9 to left side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a5 + ppd_9',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5a7',
          mother=mother,
          description='Add ppd10 to left side of 5th layer',
          pos=[19.1375, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a6 + ppd_10',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5T_left',
          mother=mother,
          description='Add ppd0 to left side of 5th layer for top stack',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos -5.64, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a7 + ppd_0',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='layer_5B_left',
          mother=mother,
          description='Add ppd0 to left side of 5th layer for top stack',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom +1.41, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5a7 + ppd_0',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
    g_en.add(geo)

    geo = Geometry(
          name='electron_hole_right',
          mother=mother,
          description='Hole to subtract on the left to leave right side',
          pos=[19.2095, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Parallelepiped',
          dimensions=[33.1325, 1.5, 10.1, 0.0, -9.68, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5b1',
          mother=mother,
          description='Subtract hole on left to leave right side, 5th layer',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: ecal_box_minus_inner2 - electron_hole_right',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5b2',
          mother=mother,
          description='Add bar on right side of electron hole',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5b1 + ppd_right',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5b3',
          mother=mother,
          description='Add ppd3 to right side 5th layer',
          pos=[0.0, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5b2 + ppd_3',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)

    geo = Geometry(
          name='layer_5b4',
          mother=mother,
          description='Add ppd4 to right side 5th layer',
          pos=[-27.194, 0.0, 0.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbcc',
          g4type='Operation: layer_5b3 + ppd_4',
          dimensions=[0.0],
          dims_units=['cm'],
          material='Component')
    g_en.add(geo)


    geo = Geometry(
          name='layer_5T_right',
          mother=mother,
          description='Add ppd5 to right side 5th layer, make for bottom stack',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos -5.64, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='FF0000',
          g4type='Operation: layer_5b4 + ppd_5',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
#    g_en.add(geo)

    geo = Geometry(
          name='layer_5B_right',
          mother=mother,
          description='Add ppd5 to right side 5th layer, make for bottom stack',
          pos=[origin[0]/10. +crystalbox_xpos, origin[1]/10. +crystalbox_ypos_bottom +1.41, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='FF0000',
          g4type='Operation: layer_5b4 + ppd_5',
          dimensions=[0.0],
          dims_units=['cm'],
          material=Aluminum)
#    g_en.add(geo)

    geo = Geometry(
          name='steel_bar',
          mother=mother,
          description='Steel Bar on bottom of right top stack',
          pos=[origin[0]/10. -37.5, origin[1]/10. +0.9, origin[2]/10. +z_location_crystalbox_offset/10. ],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='aabbee',
          g4type='Box',
          dimensions=[1.5, 0.75, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=StainlessSteel,
          style=1)

    g_en.add(geo)


def calculate_ecal_coolingsys_geometry_minimal(g_en,mother="ECAL",origin=[0,0,0]):

    copper_color='cc9900'
    copper_material=Copper
    geo = Geometry(
          name='cu_Tpipe_inner_left',
          mother=mother,
          description='Copper pipe in electron hole, top inner left',
          pos=[origin[0]/10. -0.5,origin[1]/10. +2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, -0.956, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=StainlessSteel,
          style=1)
    g_en.add(geo)

def dummy(g_en,mother,origin):
    geo = Geometry(
          name='cu_Tpipe_inner_right',
          mother=mother,
          description='Copper pipe in electron hole, top inner right',
          pos=[origin[0]/10. -11.3, origin[1]/10. +2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 9.68, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_inner_left',
          mother=mother,
          description='Copper pipe in electron hole, bottom inner left',
          pos=[origin[0]/10. -0.5, origin[1]/10. -2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, -0.956, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_inner_right',
          mother=mother,
          description='Copper pipe in electron hole, bottom inner right',
          pos=[origin[0]/10. -11.3, origin[1]/10. -2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 9.68, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right1',
          mother=mother,
          description='Copper pipe in electron hole, top outer right1',
          pos=[origin[0]/10. -13.0, origin[1]/10. +1.4, origin[2]/10.-5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 10.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right2',
          mother=mother,
          description='Copper pipe in electron hole, top outer right2',
          pos=[origin[0]/10. -30.0,origin[1]/10. + 1.4, origin[2]/10. -5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right3',
          mother=mother,
          description='Copper pipe in electron hole, top outer right3',
          pos=[origin[0]/10. -37.0, origin[1]/10. +1.5, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 90.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 4.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_outer_right',
          mother=mother,
          description='Copper pipe in electron hole, bottom outer right',
          pos=[origin[0]/10. -37.0, origin[1]/10. -1.5, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 90.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 4.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)


    geo = Geometry(
          name='cu_Bpipe_outer_right1',
          mother=mother,
          description='Copper pipe in electron hole, bottom outer right1',
          pos=[origin[0]/10. -13.0, origin[1]/10. -1.4, origin[2]/10. -5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 10.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)




##################Copper Pipes###################################################
def calculate_ecal_coolingsys_geometry(g_en,mother="ECAL",origin=[0,0,0]):

    copper_color='cc9900'
    copper_material=Copper

    geo = Geometry(
          name='cu_Tpipe_inner_left',
          mother=mother,
          description='Copper pipe in electron hole, top inner left',
          pos=[origin[0]/10. -0.5,origin[1]/10. +2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, -0.956, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_inner_right',
          mother=mother,
          description='Copper pipe in electron hole, top inner right',
          pos=[origin[0]/10. -11.3, origin[1]/10. +2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 9.68, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_inner_left',
          mother=mother,
          description='Copper pipe in electron hole, bottom inner left',
          pos=[origin[0]/10. -0.5, origin[1]/10. -2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, -0.956, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_inner_right',
          mother=mother,
          description='Copper pipe in electron hole, bottom inner right',
          pos=[origin[0]/10. -11.3, origin[1]/10. -2.7, origin[2]/10. +z_location_crystalbox_offset/10.],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 9.68, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 10.05, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right1',
          mother=mother,
          description='Copper pipe in electron hole, top outer right1',
          pos=[origin[0]/10. -13.0, origin[1]/10. +1.4, origin[2]/10.-5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 10.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right2',
          mother=mother,
          description='Copper pipe in electron hole, top outer right2',
          pos=[origin[0]/10. -30.0,origin[1]/10. + 1.4, origin[2]/10. -5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Tpipe_outer_right3',
          mother=mother,
          description='Copper pipe in electron hole, top outer right3',
          pos=[origin[0]/10. -37.0, origin[1]/10. +1.5, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 90.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 4.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_outer_right',
          mother=mother,
          description='Copper pipe in electron hole, bottom outer right',
          pos=[origin[0]/10. -37.0, origin[1]/10. -1.5, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 90.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 4.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)


    geo = Geometry(
          name='cu_Bpipe_outer_right1',
          mother=mother,
          description='Copper pipe in electron hole, bottom outer right1',
          pos=[origin[0]/10. -13.0, origin[1]/10. -1.4, origin[2]/10. -5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 10.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_Bpipe_outer_right2',
          mother=mother,
          description='Copper pipe in electron hole, bottom outer right2',
          pos=[origin[0]/10. -30.0, origin[1]/10. -1.4, origin[2]/10. -5.0],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='EllipticalTube',
          dimensions=[0.5, 0.1, 10.0],
          dims_units=['cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='al_pipe_across_top1',
          mother=mother,
          description='Aluminum pipe across top of ecal crystal front',
          pos=[origin[0]/10. , origin[1]/10. +9.0, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[90.0, 93.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbaa',
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 33.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=Aluminum,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='al_pipe_across_top2',
          mother=mother,
          description='Aluminum pipe across top of ecal crystal front2',
          pos=[origin[0]/10. , origin[1]/10. +4.0, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[90.0, 91.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbaa',
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 37.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=Aluminum,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='al_pipe_across_bottom1',
          mother=mother,
          description='Aluminum pipe across bottom of ecal crystal front',
          pos=[origin[0]/10. , origin[1]/10. -9.0, origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[90.0, 87.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbaa',
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 35.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=Aluminum,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='al_pipe_across_bottom2',
          mother=mother,
          description='Aluminum pipe across top of ecal crystal bottom2',
          pos=[origin[0]/10. , origin[1]/10. -4.0,origin[2]/10. -17.2],
          pos_units=['cm', 'cm', 'cm'],
          rot=[90.0, 89.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='bbbbaa',
          g4type='Tube',
          dimensions=[cu_pipe_ir, cu_pipe_or, 39.0, 0.0, 360.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg'],
          material=Aluminum,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_top_left',
          mother=mother,
          description='copper plate under top left of crystal stucture',
          pos=[origin[0]/10. +20.0, origin[1]/10. +1.6, origin[2]/10. -6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Parallelepiped',
          dimensions=[20.0, 0.05, 10.0, 0.0, 0.97, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_bottom_left',
          mother=mother,
          description='copper plate above bottom left of crystal stucture',
          pos=[origin[0]/10. +20.0, origin[1]/10. -1.6, origin[2]/10. -6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Parallelepiped',
          dimensions=[20.0, 0.05, 10.0, 0.0, 0.97, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_top_right',
          mother=mother,
          description='copper plate under top right of crystal stucture',
          pos=[origin[0]/10.-23.0, origin[1]/10.+1.6, origin[2]/10.-6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Parallelepiped',
          dimensions=[11, 0.05, 10.0, 0.0, 9.68, 180.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_bottom_right',
          mother=mother,
          description='copper plate on bottom right of crystal stucture',
          pos=[origin[0]/10.-23.0, origin[1]/10.-1.6, origin[2]/10.-6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Parallelepiped',
          dimensions=[11, 0.05, 10.0, 0.0, -9.68, 0.0],
          dims_units=['cm', 'cm', 'cm', 'deg', 'deg', 'deg'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_top_middle',
          mother=mother,
          description='copper plate on top middle of crystal stucture',
          pos=[origin[0]/10.-5.5, origin[1]/10.+3.1, origin[2]/10.-6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Trd',
          dimensions=[3.5, 4.9, 0.05, 0.05, 10.0],
          dims_units=['cm', 'cm', 'cm', 'cm', 'cm'],
          material=copper_material,
          style=1)
    g_en.add(geo)

    geo = Geometry(
          name='cu_plate_bottom_middle',
          mother=mother,
          description='copper plate on bottom middle of crystal stucture',
          pos=[origin[0]/10.-5.5, origin[1]/10.-3.1, origin[2]/10.-6.5],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col=copper_color,
          g4type='Trd',
          dimensions=[3.5, 4.9, 0.05, 0.05, 10.0],
          dims_units=['cm', 'cm', 'cm', 'cm', 'cm'],
          material=copper_material,
          style=1)

    g_en.add(geo)


def calculate_ecal_flux(g_en,mother="ps_field",origin=[0,0,0]):
    """This creates a flux plane at the face of the ECal. """
    g_en.add(Geometry(
          name='ecal_plane',
          mother=mother,
          description='Ecal flux plane at face of ecal',
          pos=[2.0,0.0,139.6],
          pos_units=['cm', 'cm', 'cm'],
          rot=[0.0, 0.0, 0.0],
          rot_units=['deg', 'deg', 'deg'],
          col='005500',
          g4type='Box',
          dimensions=[33.0, 10.0, 0.01],
          dims_units=['cm', 'cm', 'cm'],
          material="Vacuum",
          visible=1,
          style=1,
          sensitivity="flux",
          hittype="flux",
          identity="id manual 150"
          ))

def post_process_gdml_file(file):
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
        elif re.search('<solidref ref="world_volume"/>',line) or  re.search('.*<box name="world_volume"',line):
            # world_volume is the name of both the solidref and the volume, LCDD doesn't like that.
            wr_line=re.sub('world_volume','world_volume_solid',line)
        else:
            wr_line=line

        of.write(wr_line)

if __name__ == "__main__":
################################################################################################
#
# Geometry engine setup for local running
#
#################################################################################################

    Detector = "ecal_vacuum_flange_complete_v3"

    Variation= "original"


    print("Parameters used for the ECal detector:")
    print_parameters()

    geo_en = GeometryEngine(Detector)

#    geo_en_vac = GeometryEngine(Detector_Vac)

    geo_en_flux=0

#    Set_Global_Parameters()
    #calculate_ecal_mother_geometry(geo_en,mother="world_volume")
    #origin=[x_location_crystal_front,0,z_location]
#    origin=[21.17,0,z_location]   $ OLD valueself.
    origin=[21.42,0,z_location]  # To correspond to v1 GDML version.

    if not args.nocrystals:
        calculate_ecal_geometry(geo_en,origin=origin,mother="world_volume")
    calculate_ecal_vacuum_geometry(geo_en,origin=origin,mother="world_volume")
    calculate_ecal_crystalbox_geometry(geo_en,origin=origin,mother="world_volume")
    calculate_ecal_coolingsys_geometry(geo_en,origin=origin,mother="world_volume")

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
