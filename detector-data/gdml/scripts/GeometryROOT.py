#!/usr/bin/python
#
# Class to facilitate the rendering of GeometryEngine objects using ROOT
# This is in a separate file, because not everyone will have PyROOT enabled.
#
import ROOT
import re
import sys
import atexit

#
# The following lines are work-arounds.
#
ROOT.PyConfig.StartGuiThread = 'inputhook'    # Allow the OpenGL display to work without a crash.
#ROOT.TGeoMaterial.__init__._creates = False   # This is supposed to prevent python from crashing on exit,
#ROOT.TGeoMedium.__init__._creates = False     # but it doesn't.

from GeometryEngine import Geometry,GeometryEngine
from Rotations import Rotation,Vector

class GeometryROOT():
    """A class for construction a PyROOT geometry tree from the geometry data in a GeometryEngine class.
    """
    _geom=0
    _geo_engine=0
    _geo_engine_current=0
    _materials=0    # Dictionary of available materials
    _mats_table=0
    _mediums = 0    # Dictionary of available mediums.
    _shapes = 0     # The shapes used to create the volumes. In priciple, these could be recycled.
    _volumes = 0    # Dictionary of volumes.
    _translations=0 # Dictionary of the translation with which volumes are placed in mother. (not needed, but handy)
    debug = 0
    _conv_dict=0
    _trans_dict=0

    def __init__(self,mother='root'):
        """ Initialize the GeometryEngine class, starting up the ROOT.TGeoManager and define materials.
        :param  mother   - the name of the top level volume."""

        self._geo_engine=[]  # Make sure this is a clean list, not a global list.
        self._materials={}
        self._mediums={}
        self._shapes={}
        self._volumes={}
        self._translations={}

        self._mother = mother   # The volume considered to be the top, usually "root"


        self._geom = ROOT.TGeoManager("GEMC","Python ROOT Geometry Engine for GEMC")
        self._mats_table = self._geom.GetElementTable()
        self._geom.SetVisOption(0)
        self._conv_dict,self._trans_dict = Geometry().unit_convert_dict("cm deg")
        #
        # Setup the materials to be used.
        #
        # self.Create_Mats()
        atexit.register(self.__atexit__)

    def __atexit__(self):
        """ Try to clean up after ourselves. """
        print("Calling cleanup crew.")
        del self._geom  # -- This doesn't work. ROOT has already badly cleaned this with the hook.

    def CloseGeometry(self):
        """Close the ROOT GeometryManager:
        Closing geometry implies checking the geometry validity, fixing shapes
        with negative parameters (run-time shapes)building the cache manager,
        voxelizing all volumes, counting the total number of physical nodes and
        registering the manager class to the browser. """
        self._geom.CloseGeometry()


    def FindMaterial(self,material,trans=0):
        """ Find the material (actually medium in ROOT speak) for a volume.
            Because transparency is linked with the material in ROOT, a new
            material needs to be made for each transparency level (0-9).
        """
        g = 1
        cm3=1
        med_index=len(self._mediums)

        if material=="Component":
            return(0)


        if trans:
            matname= material+"_"+str(trans/10)
        else:
            matname= material

        if matname in self._mediums:
            return(self._mediums[matname])

        #
        # We didn't find it, so let's build it.
        #
        if self.debug > 1:
            print("Creating material: "+ matname)

        if material == "Vacuum":

            self._materials[matname] = ROOT.TGeoMaterial(matname, 0,0,0)

        elif material == "Air":

            self._materials[matname] = ROOT.TGeoMixture(matname,2,1.29*g/cm3)
            self._materials[matname].AddElement(self._mats_table.FindElement("N"), 0.7)
            self._materials[matname].AddElement(self._mats_table.FindElement("O"), 0.3)

        elif material == "Fe" or material == "Iron" or material == "G4_Fe":

            self._materials[matname] =  ROOT.TGeoMaterial(matname,self._mats_table.FindElement("Fe"),7.87);

        elif material == "Cu" or material == "Copper" or material == "G4_Cu":

        #    self._materials[matname] =  ROOT.TGeoMaterial("Cu",63.546,29,8.96);
        #    self._materials[matname].AddElement(self._mats_table.FindElement("Cu"),1.0)
            cu63=ROOT.TGeoIsotope("Cu63",29,63,62.9296)
            cu65=ROOT.TGeoIsotope("Cu65",29,65,64.9278)
            Cu = ROOT.TGeoElement("Cu","Copper",2)
            self._materials[matname] = ROOT.TGeoMaterial(matname,Cu,8.96*g/cm3)

        elif material == "StainlessSteel":

                self._materials[matname]=ROOT.TGeoMixture(matname,5, 8.02*g/cm3)
                self._materials[matname].AddElement(self._mats_table.FindElement("Mn"), 0.02)
                self._materials[matname].AddElement(self._mats_table.FindElement("Si"), 0.01)
                self._materials[matname].AddElement(self._mats_table.FindElement("Cr"), 0.19)
                self._materials[matname].AddElement(self._mats_table.FindElement("Ni"), 0.10)
                self._materials[matname].AddElement(self._mats_table.FindElement("Fe"), 0.68)

        elif material == "G4_W" or material == "Tungsten" or material=="W":

            self._materials[matname] =  ROOT.TGeoMaterial("W",self._mats_table.FindElement("W"),19.25*g/cm3);

        elif material == "LeadTungsten":

            self._materials[matname]= ROOT.TGeoMixture(matname, 3, 8.28*g/cm3)
            self._materials[matname].AddElement(self._mats_table.FindElement("Pb"),1)
            self._materials[matname].AddElement(self._mats_table.FindElement("W"), 1)
            self._materials[matname].AddElement(self._mats_table.FindElement("O"), 4)

        elif material == "Scintillator" or material=="ScintillatorB":

            self._materials[matname]= ROOT.TGeoMixture(matname, 2, 1.032*g/cm3)
            self._materials[matname].AddElement(self._mats_table.FindElement("C"),9)
            self._materials[matname].AddElement(self._mats_table.FindElement("H"), 10)


        elif material == "Aluminum" or material=="G4_Al":

            self._materials[matname]= ROOT.TGeoMaterial(matname,self._mats_table.FindElement("Al"),2.699)     # A,Z,rho

        elif material == "AlHoneycomb":

            self._materials[matname]= ROOT.TGeoMaterial(matname,self._mats_table.FindElement("Al"),0.13)    # A,Z,rho

        elif material == "Lead" or material == "Pb":
            self._materials[matname] = ROOT.TGeoMaterial(matname,self._mats_table.FindElement("Pb"),11.34)

         # G4Material *LeadTungsten = new G4Material("LeadTungsten",   density = 8.28*g/cm3, nel=3);
         #  LeadTungsten->AddElement(Pb,1);
         #  LeadTungsten->AddElement(W, 1);
         #  LeadTungsten->AddElement(O, 4);

        elif material == "LeadTungsten" or material == "PbWO" or material == "G4_PbWO4":
             self._materials[matname]= ROOT.TGeoMixture(matname, 3, 8.28*g/cm3)
             self._materials[matname].AddElement(self._mats_table.FindElement("Pb"),1)
             self._materials[matname].AddElement(self._mats_table.FindElement("W"),1)
             self._materials[matname].AddElement(self._mats_table.FindElement("O"),1)

        else:
            print("OOPS, the material: " + material + " has not yet been defined in Python world.")
            print("I'll pretend it is Aluminum...")
            return(self.FindMaterial("Aluminum", trans))

        self._mediums[matname] = ROOT.TGeoMedium(matname,med_index, self._materials[matname])
        self._materials[matname].SetTransparency(trans)
        return( self._mediums[matname])


    def Create_root_volume(self,rmaterial="Vacuum",size=[1000,1000,1000]):
        """Setup the mother of all volumes, the hall or 'root' volume everything else goes into.
        The name is that of _mother which is initiated with the mother parameter at initialization."""
        vacmat = self.FindMaterial("Vacuum")
        self._volumes[self._mother]=self._geom.MakeBox(self._mother,vacmat,size[0],size[1],size[2])
        self._geom.SetTopVolume(self._volumes[self._mother])
        self._geom.SetTopVisible(0)

    def CreateColorForRoot(self,color):
        """Root has a 'color index', the rest of the world has RGB values. Create a new color with
        the specified RGBa value string. """

        # The following regex captures the 3 color groups + the optional alpha channel.
        m = re.match("#?([A-F0-9][A-F0-9])([A-F0-9][A-F0-9])([A-F0-9][A-F0-9])([0-9]?)",color,re.I)
        if not m:
            print("ERROR, I could not conver the color: '"+color+"' to an RGBa group")
            return(21)

        R = int(m.group(1),16)
        G = int(m.group(2),16)
        B = int(m.group(3),16)
        Alpha = int(m.group(4))

        newcol=ROOT.TColor(self._color_idx,R/255.,G/255.,B/255.)
        newcol.SetAlpha(Alpha)

        return(self._color_idx)

    def FindRootColor(self,color):
        """Return the ROOT color, or create it if not found. color is an #rgb string. """

        if not color[0]=="#":
            color="#"+color

        col_found= ROOT.TColor.GetColor(color)
        return(col_found)

    def GetColorAlpha(self,color):
        """ Extract the color and the alpha (0-9) for the color. """
                # The following regex captures the 3 color groups + the optional alpha channel.
        m = re.match("#?([A-F0-9][A-F0-9][A-F0-9][A-F0-9][A-F0-9][A-F0-9])([0-9]?)",color,re.I)
        if not m:
            print("ERROR, I could not convert the color: '"+color+"' to an RGBa group")
            return(21,0)
        color = self.FindRootColor(m.group(1))
        if m.group(2):
            alpha = int(m.group(2))
        else:
            alpha=0

        return color,alpha

    def C(self,value,unit,new_unit):
        """Conversion of a value with unit to new_unit """
        if new_unit == "cm" or new_unit == "deg":    # What we expect
            new_value = value*self._conv_dict[unit.strip()]
            return new_value
        else:
            conv_dict,_  = Geometry().unit_convert_dict(new_unit)
            new_value = value*conv_dict[unit]
            return new_value

    def Build_volumes(self,geo,mother=None):
        """Take the GeometryEngine geo argument and build a tree of ROOT volumes from it.
           The tree starts at the geo volume mother"""
        if not isinstance(geo,GeometryEngine):
            print("The argument to Build_root_volumes MUST be a GeometryEngine object")
            return

        if mother is None:
            mother=self._mother

        self._geo_engine.append(geo)  # Store the GeometryEngine in case needed later.
        self._geo_engine_current = geo

        #
        # We need to go through the geometry and first place all the geometries that are
        # in the mother of geo in our mother.
        # By GEMC definition the geo mother = "root"

        objl = geo.find_children(mother) # Return list of Geometry object with mother as mother.

        if type(objl) is int:
            if self.debug>9:
                print("I could not find volumes with '"+mother+"' for mother volume. ")
            return

        if self.debug > 4:
            sys.stdout.write("Build_volumes: placing in '"+mother+"' volumes: ['")
            for x in objl:
                sys.stdout.write(x.name+"','")
            print("']")

        for vol in objl:
            self.Place_volume(vol,mother)
            self.Build_volumes(geo,vol.name)   # Recurse to find the children of this volume and build them.

    def ComputeCombiTrans(self,name,vector,rotation):
        """Compute the TGeoCombiTrans for the geometry in geo_vol
           A TGeoCombiTrans is considered to be a rotation of the object, followed by a translation.
           The actual rotation we store is an inverse rotation....
           The arguments here are a Vector (expected in 'cm') and a Rotation (in 'radians') """

        if not type(vector) is Vector:
            print("ComputeCombiTrans expected a Vector. Other types not yet implemented")
            return
        if not type(rotation) is Rotation:
            print("ComputeCombiTrans expected a Rotation. Other types not yet implemented")
            return

        rotate  =  ROOT.TGeoRotation(name+"_rot")

        (theta,phi,psi)= rotation.GetXYZ()

        rotate.RotateZ(-self.C(psi  ,"rad","deg"))
        rotate.RotateY(-self.C(phi  ,"rad","deg"))
        rotate.RotateX(-self.C(theta,"rad","deg"))

        transrot = ROOT.TGeoCombiTrans(vector.x(),
                                       vector.y(),
                                       vector.z(),
                                       rotate)
        return(transrot)

    def ComputeTransVector(self,geo_vol):
        """ Return a Rotations.Vector object from the position """

        if type(geo_vol.pos_units) is str:
            unit=geo_vol.pos_units
            geo_vol.pos_units=[unit,unit,unit]

        vec = Vector([self.C(geo_vol.pos[0],geo_vol.pos_units[0],"cm"),
                      self.C(geo_vol.pos[1],geo_vol.pos_units[1],"cm"),
                      self.C(geo_vol.pos[2],geo_vol.pos_units[2],"cm")])
        return vec


    def ComputeTransRotation(self,geo_vol):
        """ Compute the Rotation from the geo_vol rot """

        if type(geo_vol.rot_units) is str:
            unit=geo_vol.rot_units
            geo_vol.rot_units=[unit,unit,unit]

        rotate = Rotation()
        rotate = rotate.rotateX(self.C(geo_vol.rot[0],geo_vol.rot_units[0],"rad"))
        rotate = rotate.rotateY(self.C(geo_vol.rot[1],geo_vol.rot_units[1],"rad"))
        rotate = rotate.rotateZ(self.C(geo_vol.rot[2],geo_vol.rot_units[2],"rad"))

        return(rotate)


    def ComputeCombiTrans2(self,geo_vol):
        """Compute the TGeoCombiTrans for the geometry in geo_vol
           A TGeoCombiTrans is considered to be a rotation of the object, followed by a rotation."""
        rotate  =  ROOT.TGeoRotation(geo_vol.name+"_rot")
        if type(geo_vol.rot_units) is str:
            unit=geo_vol.rot_units
            geo_vol.rot_units=[unit,unit,unit]

        rotate.RotateX(-self.C(geo_vol.rot[0],geo_vol.rot_units[0],"deg"))
        rotate.RotateY(-self.C(geo_vol.rot[1],geo_vol.rot_units[1],"deg"))
        rotate.RotateZ(-self.C(geo_vol.rot[2],geo_vol.rot_units[2],"deg"))

        if type(geo_vol.pos_units) is str:
            unit=geo_vol.pos_units
            geo_vol.pos_units=[unit,unit,unit]

        transrot = ROOT.TGeoCombiTrans(self.C(geo_vol.pos[0],geo_vol.pos_units[0],"cm"),
                                       self.C(geo_vol.pos[1],geo_vol.pos_units[1],"cm"),
                                       self.C(geo_vol.pos[2],geo_vol.pos_units[2],"cm"),
                                       rotate)
        return(transrot)

    def Get_volume_shape(self,geo_vol):
        """From the geo_vol description strings, return a new TGeoVolume shape.
           For Operations, the shape is computed from previously stored shapes and translation."""

        if geo_vol.g4type == "Box":
            if type(geo_vol.dims_units) is str:
                unit = geo_vol.dims_units
                geo_vol.dims_units=[unit,unit,unit]

            newgeo_shape = ROOT.TGeoBBox(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"))
        elif geo_vol.g4type == "Tube":
            if type(geo_vol.dims_units) is str:
                unit = geo_vol.dims_units
                geo_vol.dims_units=[unit,unit,unit]

            if len(geo_vol.dimensions) == 3 or geo_vol.dimensions[3]<=0. and self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"deg")>=360. :
                newgeo_shape = ROOT.TGeoTube(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"))
            else:
                newgeo_shape = ROOT.TGeoTubeSeg(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"),
                                        self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"deg"),
                                        self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"deg"))

        elif geo_vol.g4type == "Sphere":
            if type(geo_vol.dims_units) is str:
                print("We have a problem. Parallelepiped "+ geo_vol.name+" has bad units = string.")

            start_phi = self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"deg")
            delta_phi = self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"deg")
            end_phi = start_phi + delta_phi
            start_tht = self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"deg")
            delta_tht = self.C(geo_vol.dimensions[5],geo_vol.dims_units[5],"deg")
            end_tht = start_tht + delta_tht

            newgeo_shape = ROOT.TGeoSphere(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        start_phi,end_phi,
                                        start_tht,end_tht)


        elif geo_vol.g4type == "Parallelepiped":
            if type(geo_vol.dims_units) is str:
                print("We have a problem. Parallelepiped "+ geo_vol.name+" has bad units = string.")

            newgeo_shape = ROOT.TGeoPara(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"),
                                        self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"deg"),
                                        self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"deg"),
                                        self.C(geo_vol.dimensions[5],geo_vol.dims_units[5],"deg"))

        elif geo_vol.g4type == "Trd":
            if type(geo_vol.dims_units) is str:
                unit = geo_vol.dims_units
                geo_vol.dims_units=[unit,unit,unit,unit,unit]

            newgeo_shape= ROOT.TGeoTrd2(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"),
                                        self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"cm"),
                                        self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"cm"))
        elif geo_vol.g4type == "G4Trap":
            if type(geo_vol.dims_units) is str:
                print("We have a problem. G4Trap "+ geo_vol.name+" has bad units = string.")

            newgeo_shape= ROOT.TGeoTrap(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"deg"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"deg"),
                                        self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"cm"),
                                        self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"cm"),
                                        self.C(geo_vol.dimensions[5],geo_vol.dims_units[5],"cm"),
                                        self.C(geo_vol.dimensions[6],geo_vol.dims_units[6],"deg"),
                                        self.C(geo_vol.dimensions[7],geo_vol.dims_units[7],"cm"),
                                        self.C(geo_vol.dimensions[8],geo_vol.dims_units[8],"cm"),
                                        self.C(geo_vol.dimensions[9],geo_vol.dims_units[9],"cm"),
                                        self.C(geo_vol.dimensions[10],geo_vol.dims_units[10],"deg"))
#            Double_t dz, Double_t theta, Double_t phi, Double_t h1, Double_t bl1, Double_t tl1, Double_t alpha1, Double_t h2, Double_t bl2, Double_t tl2, Double_t alpha2)
        elif geo_vol.g4type == "EllipticalTube" or geo_vol.g4type == "Eltu":
            if type(geo_vol.dims_units) is str:
                unit = geo_vol.dims_units
                geo_vol.dims_units=[unit,unit,unit]

            newgeo_shape= ROOT.TGeoEltu(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"))

        elif geo_vol.g4type == "Cons":
            if type(geo_vol.dims_units) is str:
                print("We have a problem. Cons "+ geo_vol.name+" has bad units = string.")
#
# Note the arguments have different order from GEANT4.
#
            start_phi = self.C(geo_vol.dimensions[5],geo_vol.dims_units[5],"deg")
            delta_phi = self.C(geo_vol.dimensions[6],geo_vol.dims_units[6],"deg")
            end_phi = start_phi + delta_phi

            newgeo_shape= ROOT.TGeoConeSeg(geo_vol.name+"_shape",
                                        self.C(geo_vol.dimensions[4],geo_vol.dims_units[4],"cm"),
                                        self.C(geo_vol.dimensions[0],geo_vol.dims_units[0],"cm"),
                                        self.C(geo_vol.dimensions[1],geo_vol.dims_units[1],"cm"),
                                        self.C(geo_vol.dimensions[2],geo_vol.dims_units[2],"cm"),
                                        self.C(geo_vol.dimensions[3],geo_vol.dims_units[3],"cm"),
                                        start_phi,end_phi)


        elif re.match("Operation:.*",geo_vol.g4type):
            #
            # Operations combine two shapes with either +, - or *
            # Important Note:
            # ROOT and GEANT4 behave slightly different when it comes to combining volumes.
            # For the ROOT Geo system, object1 and object2 are first located relative to the mother volume,
            # then they are added or subtracted, and then the result is translated to the final location with
            # the translation parameters of the final volume.
            # For GEANT4, object1, is in it's own coordinate system at the origin. Object2 is then moved and/or
            # rotated relative to this origin, and then the addition or subtraction takes place. Then the
            # final result is translated to the final location + rotation.
            # The upshot is that the positioning of the FIRST object must be ignored, if we follow the
            # GEANT4 rules, which we are bound to here.
            # GEMC has the added feature that you can have BOTH volumes specified relative to the coordinates
            # of the mother volume. It achieves this with a calculation. You can tell GEMC to do this with
            # an @ right after the "Operation:" statement (i.e. "Operation:@")
            # This is almost what ROOT does. The difference, is that for ROOT the final positioning is
            # then relative to the origin of the MOTHER volume, i.e. the original center. For GEMC the
            # final positioning is relative to the center of the FIRST object.
            # Because the combo object can be put into another combo, we cannot get the same behavior
            # by simply placing the new shape at trans = -trans_1 + trans_final
            # We need to follow the GEMC calculation.

            match=re.match("Operation:([~@])?\W*(\w*)\W*([+-])\W*(\w*)\W*",geo_vol.g4type)
            special = match.group(1)
            shape1_name = match.group(2)
            operation = match.group(3)
            shape2_name = match.group(4)
            if self.debug >4:
                print("Operation found: " + geo_vol.g4type + " = '" + shape1_name + "' " + operation + " '"+ shape2_name+"'")

#
# The following is effectively a recursive call. If the lines for placing the required shapes had not
# been processed yet, then process them now.
# Find the volume description line from the GeometryEngine, and place the volume.
# Since these are most likely "Component" volumes, they will not actually be placed, but the
# shape and translation will be created in "Place_Volume()
#
            if not shape1_name in self._shapes.keys():
                f_geo = self._geo_engine_current.find_volume(shape1_name) # The volume isn't there yet. Find it and place it.
                self.Place_volume(f_geo) # place in same mother.

            if not shape2_name in self._shapes.keys():
                f_geo = self._geo_engine_current.find_volume(shape2_name) # The volume isn't there yet. Find it and place it.
                self.Place_volume(f_geo) # place in same mother.

            shape1 = self._shapes[shape1_name]
            shape2 = self._shapes[shape2_name]
#            transrot2 = self.ComputeCombiTrans(self._geo_engine_current.find_volume(shape2_name))
            trans2,rot2 = self._translations[shape2_name]
            transrot2 = self.ComputeCombiTrans(shape2_name,trans2,rot2)
            transrot1=0

            if special == "@":
#               transrot1 = self.ComputeCombiTrans(self._geo_engine_current.find_volume(shape1_name))
# Can get the translation-rotation from storage:
                trans1,rot1 = self._translations[shape1_name]

                net_trans = trans2 - trans1
                net_trans_rot = rot2*net_trans

                rot1inv = rot1.I
                net_rot = rot1inv*rot2

                transrot2 = self.ComputeCombiTrans(geo_vol.name, net_trans_rot, net_rot)

            if operation == "-":
                opshape = ROOT.TGeoSubtraction(shape1,shape2,transrot1,transrot2)
            elif operation == "+":
                opshape = ROOT.TGeoUnion(shape1,shape2,transrot1,transrot2)
            elif operation == "*":
                opshape = ROOT.TGeoIntersection(shape1,shape2,transrot1,transrot2)
            else:
                print("WARNING: Operation "+operation+" is not implemented.")

            newgeo_shape = ROOT.TGeoCompositeShape(geo_vol.name+"_shape",opshape)

        else:
            print("The geometry shape: "+ geo_vol.g4type +" is not yet defined.")
            return(0)

        self._shapes[geo_vol.name] = newgeo_shape

        return(newgeo_shape)


    def Place_volume(self,geo_vol,mother=0):
        """ Place the Geometry object geo_vol onto the ROOT geometry tree under the volume 'mother' """

        if not isinstance(geo_vol,Geometry):
            print("We can only build Geometry objects and argument is not a Geometry")
            return

        if not mother:
            mother=geo_vol.mother    # If mother is not explicit, get it from the geometry line.

        try:
            mother_vol = self._volumes[mother]
        except KeyError:
            print("Error -- Mother volume: "+mother+" is not found, so cannot build "+geo_vol.name)
            return

        if geo_vol.name in self._shapes.keys():
            if self.debug > 7:
                print("We have done the shape for '"+geo_vol.name+"' already. Skip.")
            return

        color,transp  = self.GetColorAlpha(geo_vol.col)
        transp= transp*10
        if geo_vol.style == 0 and transp<70:     # Outline only, instead make semi transparent.
            transp= 70

        medium = self.FindMaterial(geo_vol.material,transp)

        # Make the translation and the rotation.
        translate = self.ComputeTransVector(geo_vol)
        rotate    = self.ComputeTransRotation(geo_vol)

        self._translations[geo_vol.name]=(translate,rotate)  # Each shape has a translation in GEMC, store for combos.
        transrot = self.ComputeCombiTrans(geo_vol.name,translate, rotate)

        if geo_vol.exist == 0:  # I don't exist, so don't bother.
            return

        if self.debug > 1:
            print("Mother: "+ mother+ " Volume: "+ geo_vol.name + "  Type:" + geo_vol.g4type + "  Material:"+geo_vol.material + " Vis:"+str(geo_vol.visible))
            print("    dimension:"+str(geo_vol.dimensions) + " units:" + str(geo_vol.dims_units))
            print("    position :"+str(geo_vol.pos)+"  pos units:"+str(geo_vol.pos_units))
            print("    rotation :"+str(geo_vol.rot)+"  rot units:"+str(geo_vol.rot_units))
            print("")

        newgeo_shape = self.Get_volume_shape(geo_vol)

        if self.debug > 6:
            print("Put '"+geo_vol.name+"' put on shapes table ")
        if medium == 0:
            if self.debug > 5:
                print("Component volume '"+geo_vol.name+"' put on shapes table only")
        else:
            newgeo = ROOT.TGeoVolume(geo_vol.name,newgeo_shape,medium)
            newgeo.SetLineColor(color)
            newgeo.SetVisibility(geo_vol.visible)
            self._volumes[geo_vol.name] = newgeo
            mother_vol.AddNode(newgeo,1,transrot)

    def Draw(self,option=""):
        """ Draw an wireframe version of the objects """
        topvol = self._geom.GetTopVolume()
        topvol.Draw(option)
        self._geom.SetVisOption(0)

    def __str__(self):
        """ Print information about this class """
        strout = "GeometryROOT object, containing:\n"
        for name,vol in self._volumes.iteritems():
            match = re.match('<ROOT\.(.*) .*',str(vol.GetShape()))
            shape_name = match.group(1)

            match = re.match('.*\(\"(.*)\"\) .*',str(vol.GetMedium()))
            medium_name = match.group(1)
            strout+= name+" rname:" + vol.GetName() + " type: " + shape_name + " medium:" + str(vol.GetMedium())+"\n"

        return(strout)

if __name__ == "__main__":
    ################################################################################################################################
    print("Sorry, no test suite build in. Run the TestRoot.py script from the python interpreter instead:")
    print("> python")
    print("> execfile('TestRoot.py')")
