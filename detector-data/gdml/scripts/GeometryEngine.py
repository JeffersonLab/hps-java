#!/usr/bin/python
#
# A Helper library for GEANT4 geometries.
#
# Creator: Maurik Holtrop  2013-3-14  (pi day)
#
# import numpy as np
import re
import sys
import math
import warnings
try:
    import MySQLdb
    import _mysql_exceptions
    warnings.filterwarnings("error",category=MySQLdb.Warning)
except ImportError:
    print("warning mysql not found")


# import Rotations as R
# import ROOT
# We may later add a dependeny on the ROOT engine, and render a geometry directly in ROOT.
# For now, that is too more work than I can handle :-)
#
class Geometry():
    """ A Class for storing the geometry of an objects to be rendered in the output.
    You would typicaly create a list of these Geometry() objects, and then pass this list
    to one of the output engines. """
#
# Note that below are the default values for each of the geometry objects.
# They can be modified when the Geometry class is instantiated.
#
# Implementation question: Would this be cleaner if the all the data was stored in a single list?
# data = {'name':" ",'mother':" ", etc}
    debug=0
    name=""
    mother=""
    description=""
    pos=[0,0,0]
    pos_units="cm"
    rot=[0,0,0]
    rot_units="rad"
    col="000000"
    g4type=""
    dimensions=""
    dims_units="cm"
    material=""
    magfield="no"
    ncopy=1
    pmany=1
    exist=1
    visible=1
    style=1
    sensitivity="no"
    hittype="no"
    identity="no"
    rmin=1
    rmax=10000

    def __init__(self,
                 name="",
                 mother="",
                 description="",
                 pos=[0,0,0],
                 pos_units="cm",
                 rot=[0,0,0],
                 rot_units="rad",
                 col="000000",
                 g4type="",
                 dims_units="cm",
                 dimensions="",
                 material="",
                 magfield="no",
                 ncopy=1,
                 pmany=1,
                 exist=1,
                 visible=1,
                 style=1,
                 sensitivity="no",
                 hittype="",
                 identity="",
                 rmin=1,
                 rmax=10000):

        if type(name) is tuple or type(name) is list:   # If first argument is a tuple, assume the full content.
            self.name=name[0]
            self.mother=name[1]
            self.description=name[2]
            self.pos,self.pos_units=self.parse_gemc_str(name[3],"cm")
            self.rot,self.rot_units=self.parse_gemc_str(name[4], "deg")
            self.col=name[5]
            self.g4type=name[6]
            self.dimensions,self.dims_units=self.parse_gemc_str(name[7],"cm deg")
            self.material=name[8]
            self.magfield=name[9]
            self.ncopy=int(name[10])
            self.pmany=int(name[11])
            self.exist=int(name[12])
            self.visible=int(name[13])
            self.style=int(name[14])
            self.sensitivity=name[15]
            self.hittype=name[16]
            self.identity=name[17]
            if len(name) > 18:
                self.rmin=name[18]
                self.rmax=name[19]
            else:
                self.rmin=1
                self.rmax=100000
            return

        self.name = name
        self.mother = mother
        self.description = description
        self.pos = pos
        self.set_position(pos,pos_units)
        self.set_rotation(rot,rot_units)
        self.col=col
        self.g4type=g4type
        self.set_dimensions(dimensions,dims_units)
        self.material=material
        self.magfield=magfield
        self.ncopy=ncopy
        self.pmany=pmany
        self.exist=exist
        self.visible=visible
        self.style=style
        self.sensitivity=sensitivity
        self.hittype=hittype
        self.identity=identity
        self.rmin=rmin
        self.rmax=rmax

    def unit_convert_dict(self,base_unit):
        """Return a conversion dict and translation dict that provides the correct unit conversions based on base_unit
        base_unit is a string or a list and contains a unit for lenght and one for angles
        The conversion dict contains the numeric constant to covert to the base_unit.
        The translation dict converts the unit name to the base unit name."""
        if type(base_unit) == str:
            base_unit = base_unit.split()
        base_unit = [ u.strip().strip("*") for u in base_unit] # Split the string and remove any *
# This default ensures that there is always a sensible conversion, no matter the contentds of base_unit
        conv_dict={'mm':0.1,'cm':1.,'m':100.,'inch':2.54,'inches':2.54,'rad':1.,'mrad':0.001,'deg':math.radians(1)}
        trans_dict={'mm':'cm','cm':'cm','m':'cm','inch':'cm','inches':'cm','rad':'rad','mrad':'rad','deg':'rad'}
        for u in base_unit:
            if u == "cm":
                trans_dict['mm'] = trans_dict['cm'] = trans_dict['m'] = trans_dict['inch'] = u
                conv_dict['mm']=0.1
                conv_dict['cm']=1.
                conv_dict['m']=100.
                conv_dict['inch']=2.54
            elif u == "mm":
                trans_dict['mm'] = trans_dict['cm'] = trans_dict['m'] = trans_dict['inch'] = u
                conv_dict['mm']=1.
                conv_dict['cm']=10.
                conv_dict['m']=1000.
                conv_dict['inch']=25.4
            elif u == "m":
                trans_dict['mm'] = trans_dict['cm'] = trans_dict['m'] = trans_dict['inch'] = u
                conv_dict['mm']=0.001
                conv_dict['cm']=0.01
                conv_dict['m']=1.
                conv_dict['inch']=0.0254
            elif u == "inch":
                print("Warning: Base Units of Inches is not recommended for GEANT4")
                trans_dict['mm'] = trans_dict['cm'] = trans_dict['m'] = trans_dict['inch'] = u
                conv_dict['mm']=0.1/2.54
                conv_dict['cm']=1./2.54
                conv_dict['m']=100./2.54
                conv_dict['inch']=1.
            elif u == "rad":
                trans_dict['rad'] = trans_dict['mrad'] = trans_dict['deg'] = u
                conv_dict['rad']=1
                conv_dict['mrad']=0.001
                conv_dict['deg']=math.radians(1.)
            elif u == "mrad":
                trans_dict['rad'] = trans_dict['mrad'] = trans_dict['deg'] = u
                conv_dict['rad']=1000.
                conv_dict['mrad']=1.
                conv_dict['deg']=1000.*math.radians(1.)
            elif u == "deg":
                trans_dict['rad'] = trans_dict['mrad'] = trans_dict['deg'] = u
                conv_dict['rad']=math.degrees(1.)
                conv_dict['mrad']=0.001*math.degrees(1.)
                conv_dict['deg']=1

        return(conv_dict,trans_dict)

    def parse_gemc_str(self,string,base_unit):
        """Convert the GEMC MySQL string with units into a list and a units list based on the base_unit """
        conv,trans=self.unit_convert_dict(base_unit)

        tmp_list= string.split()
        ans=0
        dims=[]
        dims_units=[]
        for p in tmp_list:
            if not re.search('\*',p):
                if len(p)>1:
                    print("Warning: expected unit but none found in:"+str(tmp_list))
                ans=float(p)
                unit="";
                dims.append(ans)
                dims_units.append('cm')  # If there was no unit, make it cm.
            else:
#           exec(strunits+"ans="+p)
                num,unit = p.split('*')
                if unit == "inches":
                    unit = "inch"
                ans = float(num)*conv[unit]

                dims.append(ans)
                dims_units.append(trans[unit])

        return(dims,dims_units)

    def set_position(self,pos,units):
        """ Set the position from a string or a list, in the latter case use units for the units."""
        if type(pos) == str:
# Need to convert the string to a pos vector and a unit. Typical pos="10*mm 12*cm 1*m"
            self.pos,self.pos_units = self.parse_gemc_str(pos,units)

        else:
# No need to convert
            self.pos=pos
            self.pos_units=units

    def set_rotation(self,rot,units):
        """ Set the rotation of the object from a string or a list."""
        if type(rot) == str:
# Need to convert the string to a pos vector and a unit. Typical pos="10*mm 12*cm 1*m"
            self.rot,self.rot_units = self.parse_gemc_str(rot,units)

        else:
# No need to convert
            self.rot=rot
            self.rot_units=units

    def set_dimensions(self,dims,units):
        """ Set the dimensions of the object from a string or a list """
        if type(dims) == str:
# Need to convert the string to a pos vector and a unit. Typical pos="10*mm 12*cm 1*m"
            self.dimensions,self.dims_units = self.parse_gemc_str(dims,units)

        else:
# No need to convert
            self.dimensions=dims
            self.dims_units=units

    def make_string(self,item,units):
        """ Turn a list and units into a Maurizio's MySQL Database style string. """
        strout=""
        if type(units) == str:
            units = [units]
        if self.debug > 0:
            print("inital: item(",len(item),")=",item,"  units(",len(units),")=",units)

        if len(units) == len(item):   # There is one unit for each item. Units may be mixed.
            for i in range(len(item)):
                strout+=str(item[i])+"*"+units[i]+" "
        else:
            if len(units) > 1:
                print("WARNING: There seems to be an issue here, item and units are different length: item="+str(item)+" units="+str(units))
            for i in item:
                strout+=str(i)+"*"+units[0]+" "

        return(strout)

    def MySQL_str(self,Table,variation=0,idn=1):
        """ Return a MySQL statement to insert the geometry into a GEMC Table in a MySQL database"""

        sql="INSERT INTO "+Table +" VALUES ('"
        sql+=self.name+"','"+self.mother+"','"+self.description+"','"
        sql+=self.make_string(self.pos, self.pos_units)+"','"
        sql+=self.make_string(self.rot, self.rot_units)+"','"
        sql+=self.col+"','"+self.g4type+"','"
        sql+=self.make_string(self.dimensions,self.dims_units) +"','"
        sql+=self.material+"','"
        sql+=self.magfield+"',"
        sql+=str(self.ncopy)+","
        sql+=str(self.pmany)+","
        sql+=str(self.exist)+","
        sql+=str(self.visible)+","
        sql+=str(self.style)+",'"
        sql+=self.sensitivity+"','"
        sql+=self.hittype+"','"
        sql+=self.identity+"',"
        sql+=str(self.rmin)+","
        sql+=str(self.rmax)+","
        sql+="now()"

        if variation != 0:       # This is for GEMC 2.0 style tables.
            sql+=",'"+variation+"',"
            sql+= str(idn)
        sql+=");"
        return(sql)

    def Python_str(self,indent=4):
        """Return a string containing a python statement that will render this geometry.
        This can be used to create a template script from the contents of a database.
        Code will be indented by 'indent'
        """
        pstr =' '*indent +"geo = Geometry( \n"
        pstr+=' '*indent +"      name='"+self.name+"',\n"
        pstr+=' '*indent +"      mother='"+self.mother+"',\n"
        pstr+=' '*indent +"      description='"+self.description+"',\n"
        pstr+=' '*indent +"      pos="+str(self.pos)+",\n"
        pstr+=' '*indent +"      pos_units="+str(self.pos_units)+",\n"
        pstr+=' '*indent +"      rot="+str(self.rot)+",\n"
        pstr+=' '*indent +"      rot_units="+str(self.rot_units)+",\n"
        pstr+=' '*indent +"      col='"+self.col+"',\n"
        pstr+=' '*indent +"      g4type='"+self.g4type+"',\n"
        pstr+=' '*indent +"      dimensions="+str(self.dimensions)+",\n"
        pstr+=' '*indent +"      dims_units="+str(self.dims_units)+",\n"
        pstr+=' '*indent +"      material='"+self.material+"',\n"
        pstr+=' '*indent +"      magfield='"+self.magfield+"',\n"
        pstr+=' '*indent +"      ncopy="+str(self.ncopy)+",\n"
        pstr+=' '*indent +"      pmany="+str(self.pmany)+",\n"
        pstr+=' '*indent +"      exist="+str(self.exist)+",\n"
        pstr+=' '*indent +"      visible="+str(self.visible)+",\n"
        pstr+=' '*indent +"      style="+str(self.style)+",\n"
        pstr+=' '*indent +"      sensitivity='"+self.sensitivity+"',\n"
        pstr+=' '*indent +"      hittype='"+self.hittype+"',\n"
        pstr+=' '*indent +"      identity='"+self.identity+"',\n"
        pstr+=' '*indent +"      rmin="+str(self.rmin)+",\n"
        pstr+=' '*indent +"      rmax="+str(self.rmax)+" )\n"
        return(pstr)

    def Validate(self):
        """Attempt to check yourself for valid entries.
          If all is OK, then return a 0.
          If an error is expected, return a integer indicating the field where the first error is expected.
          Clearly this can only catch the most simple errors, such as formatting problems."""

        if not type(self.name) is str:
            return(1)

        if not type(self.mother) is str:
            return(2)

        if not type(self.description) is str:
            return(3)

        if type(self.pos) is list or type(self.pos) is tuple:
            if not len(self.pos) == 3:
                return(4)
            for x in self.pos:
                if not (type(x) is int or type(x) is float):
                    return(4)
        else:
            return(4)

        try:
            self.make_string(self.pos, self.pos_units)
        except:
            return(4)

        if type(self.rot) is list or type(self.rot) is tuple:
            if not len(self.rot) == 3:
                return(5)
            for x in self.rot:
                if not (type(x) is int or type(x) is float):
                    return(5)
        else:
            return(5)

        try:
            self.make_string(self.rot, self.rot_units)
        except:
            return(5)

        if not type(self.col) is str:
            return(6)

        if not type(self.g4type) is str:
            return(7)

        # The dimensions and dims_units are more difficult to check. The Operations: g4type has none!
        # Try to turn to a string, and hope for the best....
        try:
            self.make_string(self.dimensions, self.dims_units)
        except:
            return(8)

        if  not type(self.material) is str:
            return(9)

        if not type(self.magfield) is str:
            return(10)

        if not type(self.ncopy) is int:
            return(11)

        if not type(self.pmany) is int:
            return(12)

        if not (self.exist ==0 or self.exist ==1):
            return(13)

        if not (self.visible ==0 or self.visible == 1):
            return(14)

        if not (self.style ==0 or self.style == 1):
            return(14)

        if not type(self.sensitivity) is str:
            return(15)

        if not type(self.hittype) is str:
            return(16)

        if not type(self.identity) is str:
            return(17)

        if not type(self.rmin) is int:
            return(18)

        if not type(self.rmax) is int:
            return(19)


        return(0)



    def __str__(self):
        """ Return a string with the geometry as a '|' delimited string, as Maurizio's perl scripts """
        outstr =self.name+' | '
        outstr+=self.mother+' | '
        outstr+=self.description+' | '
        outstr+=self.make_string(self.pos,self.pos_units)+' | '
        outstr+=self.make_string(self.rot,self.rot_units)+' | '
        outstr+=self.col+' | '
        outstr+=self.g4type+' | '
        outstr+=self.make_string(self.dimensions,self.dims_units)+' | '
        outstr+=self.material+' | '
        outstr+=self.magfield+' | '
        outstr+=str(self.ncopy)+' | '
        outstr+=str(self.pmany)+' | '
        outstr+=str(self.exist)+' | '
        outstr+=str(self.visible)+' | '
        outstr+=str(self.style)+' | '
        outstr+=self.sensitivity+' | '
        outstr+=self.hittype+' | '
        outstr+=self.identity+' '       # No bar on the end of the line.
#       outstr+=str(self.rmin)+' | '      # GEMC does NOT want the rmin and rman in text files. Don't know why.
#       outstr+=str(self.rmax)

        return(outstr)

    def __getitem__(self,i):
        """To treat the Geometry as a dictionary or list..."""
        if i == "name" or i==0:
            return(self.name)
        elif i=="mother" or i==1:
            return(self.mother)
        elif i=="description" or i==2:
            return(self.description)
        elif i=="pos" or i == 3:
            return(self.make_string(self.pos, self.pos_units))
        elif i=="rot" or i== 4:
            return(self.make_string(self.rot, self.rot_units))
        elif i=="col" or i == 5:
            return(self.col)
        elif i=="g4type" or i=="type" or i==6:
            return(self.g4type)
        elif i=="dimensions" or i=="dims" or i==7:
            return(self.make_string(self.dimensions, self.dims_units))
        elif i=="material" or i==8:
            return(self.material)
        elif i=="magfield" or i==9:
            return(self.magfield)
        elif i=="ncopy" or i==10:
            return(self.ncopy)
        elif i=="pmany" or i==11:
            return(self.pmany)
        elif i=="exist" or i==12:
            return(self.exist)
        elif i=="visible" or i==13:
            return(self.visible)
        elif i=="style" or i==14:
            return(self.style)
        elif i=="sensitivity" or i==15:
            return(self.sensitivity)
        elif i=="hittype" or i==16:
            return(self.hittype)
        elif i=="identity" or i==17:
            return(self.identity)
        elif i=="rmin" or i==18:
            return(self.rmin)
        elif i=="rmax" or i==19:
            return(self.rmax)


    def CalcG4Trapezoid(self,front,depth,p1x,p1z,theta1,p2x,p2z,theta2):
        # Utility function.
        # This function calculates the parameters for a G4Trap, assuming the front is at z=front, and given the depth (length) of the
        # trapezoid, and a left (p1) and right (p2) point and angle wrt z, for points on the left and right edges of the trapezoid.
        #
        # The function retuns cx,cz,theta,dx1,dx2  the centerpoint (x,z), the skew angle, and the front and back half widths.
        #
        # See HPS_Software Notebook section "G4 Trapezoid"
        #
        z1=front
        z2=front+ depth
        #  dz=depth/2

        dx1= ( (p2x - p1x) - (z1 - p1z)*math.tan(theta1) + (z1 - p2z)*math.tan( theta2)  )/2.
        dx2= ( (p2x - p1x) - (z2 - p1z)*math.tan(theta1) + (z2 - p2z)*math.tan( theta2)  )/2.

        pp1x= p1x + ((z1+z2)/2. - p1z)*math.tan(theta1)     # line through midpoint on low x.
        pp2x= p2x + ((z1+z2)/2. - p2z)*math.tan(theta2)     # line through midpoint high x.

        c1x = p2x + (z1 - p2z)*math.tan(theta2) - dx1
        c2x = p2x + (z2 - p2z)*math.tan(theta2) - dx2

        cx    = (c1x + c2x)/ 2.
        cz    = front + depth/2.
        thetal = math.atan2( (c2x - c1x),depth )

        if dx1 <= 0 or dx2 <= 0 or pp1x > pp2x :
            print("front="+str(front)+"  Depth="+str(depth))
            print("p1x  ="+str(p1x)+"  p1z  ="+str(p1z))
            print("p2x  ="+str(p2x)+"  p2z  ="+str(p2z))
            print("pp1x =",pp1x,    "  pp2x =",pp2x)
            print("dx1  =",dx1,     "  dx2  =",dx2)
            print("theta1=",theta1, " theta2=",theta2)
            print("depth ="+str(depth) +"  z2 - p2z=",(z2-p2z),"  tan(theta2)=",math.tan(theta2)," (z2-p1z)*tan(theta1)=",(z2-p1z)*math.tan(-theta2))
            print("c1x  ="+str(c1x)+"   c2x ="+str(c2x))
            print("cx =  "+str(cx)+"   cz = "+str(cz))
            print("cx'=  ",(pp1x+pp2x)/2.)
            print("thetal=",thetal)
            print("dx1  ="+str(dx1)+"   dx2 ="+str(dx2))

        return( cx,cz,thetal,dx1,dx2)




class Sensitive_Detector():
    """ A class to represent the sensitive, or active, detector components.
        Each active detector component has a "sensitivity", a "hitType" ("flux", "Hodo", "SVT",...), and an "identity"
        in the geometry description. Each type of "sensitivity" needs to be further specified for proper readout.
        Note that each type of "sensitivity" also needs to have a corresponding line in HitProcess_MapRegister.cc in Gemc,
        so the contents is not something arbitrary.
        This class stores those values for one type of sensitive detector and how to read those values into EVIO banks.
    """

    name=""
    description=""
    identifiers=""
    signalThreshold=""
    timeWindow=""
    prodThreshold=""
    maxStep=""
    riseTime=""
    fallTime=""
    mvToMeV=""
    pedestal=""
    delay=""
    bankId=""
    _BankRows=0  # must be an array [] Defines the rows of the EVIO bank. Each row contains a tuple ("row name", "Comment", id, "type" )

# Type is a two char string:
# The first char:
#  R for raw integrated variables
#  D for dgt integrated variables
#  S for raw step by step variables
#  M for digitized multi-hit variables
#  V for voltage(time) variables
#
# The second char:
# i for integers
# d for doubles

    def __init__(self,name,description,identifiers,signalThreshold="0*MeV",timeWindow="10*ns",
                 prodThreshold="0*mm",maxStep="1*mm",riseTime="1*ns",fallTime="1*ns",mvToMeV="1",pedestal="0",delay="0*ns",bankId=0):
        """ Initialization of class"""
        self.name = name
        self.description=description
        self.identifiers=identifiers
        self.signalThreshold=signalThreshold
        self.timeWindow=timeWindow
        self.prodThreshold=prodThreshold
        self.maxStep=maxStep
        self.riseTime=riseTime
        self.fallTime=fallTime
        self.mvToMeV=mvToMeV
        self.pedestal=pedestal
        self.delay=delay
        self.bankId=bankId
        self._BankRows=[]

        self.add_bank_row("bankid",name+" bank id", bankId, "Di")

    def add_bank_row(self,name,comment, idn,stype):
        """ Add a row to the EVIO Bank definition
            name = 'rowname'
            comment = 'comment string'
            idn   = 1 'Evio row id number'
            stype = 'Di'
            """
        self._BankRows.append( (name,comment,idn,stype) )

    def hit_str(self):
        """ Return a string for the detector__hits_variation.txt style file. """
        outstr = self.name + " | "
        outstr+= self.description + " | "
        outstr+= self.identifiers + " | "
        outstr+= self.signalThreshold + " | "
        outstr+= self.timeWindow + " | "
        outstr+= self.prodThreshold + " | "
        outstr+= self.maxStep + " | "
        outstr+= self.riseTime + " | "
        outstr+= self.fallTime + " | "
        outstr+= self.mvToMeV + " | "
        outstr+= self.pedestal + " | "
        outstr+= self.delay
        return(outstr)

    def bank_str(self):
        """ Return a multi line string containing the text of the EVIO bank definitions. """
        print("Number of rows:" + str(len(self._BankRows)))
        outstr=""
        for row in self._BankRows:
            outstr += self.name + " | " + " | ".join(map(str,row))+"\n"  # join with | the str(row) components.
        return(outstr)

    def bank_MySQL_str(self,Table,variation,idn=1):
        """Return a MySQL string to fill a banks table."""
        sql="INSERT INTO "+Table+" VALUES "
        for row in self._BankRows:
            sql+= str((self.name,)+row+('now()',variation))+","
        sql = sql[:-1] + ";"
        return(sql)

    def hit_MySQL_str(self,Table,variation,idn=1):
        """ Return a MySQL string to fill the hit table """
        sql="INSERT INTO "+Table+" VALUES ('"
        sql+=self.name+"','"
        sql+=self.description+"','"
        sql+=self.identifiers+"','"
        sql+=self.signalThreshold+"','"
        sql+=self.timeWindow+"','"
        sql+=self.prodThreshold+"','"
        sql+=self.maxStep + "','"
        sql+=self.riseTime+ "','"
        sql+=self.fallTime+ "',"
        sql+=str(self.mvToMeV)+","
        sql+=str(self.pedestal)+",'"
        sql+=self.delay +"',"
        sql+="now(),'"
        sql+=variation+"')"

        return(sql)

    def hitType(self):
        """ Return the correct string for the hitType entry in the Geometry definition. """
        return(self.name)

    def sensitivity(self):
        """ Return the correct sting for the sensitivity entry in the Geometry definition. """
        return(self.name)

    def identity(self,*indexes):   # The * means, add all the arguments into the list called indexes
        """ Given a tuple of indexes i.e. (1,2,3), return the correct string for the indentity entry in the Geometry definition"""
        outstr=""
        ids = self.identifiers.split()
        for i in range(len(ids)):
            outstr += ids[i]+" manual " + str(indexes[i]) + " "

        return(outstr)

    def __str__(self):
        """ Print information of the class to sting. Because GEMC expects 2 files, one for hits and one for banks,
            this mehod cannot be used the write these files in one line."""
        outstr= self.hit_str() + "\n"
        outstr+="-------------------------------------------------------------------------------------------------------\n"
        outstr+= self.bank_str()
        return(outstr)


class GeometryEngine():
    """A class for building GEANT4 geometries.
        The initial purpose is to build the geometries for the MySQL database used by Gemc1 or Gemc2.
        It can also write the flat text tables that Gemc2 supports.
        Each GeometryEngine object represents ONE 'detector' (as defined by Gemc).
        Expansion to other geometry formats is possible.
        """

    _DataBase=0
    _cursor=0
    _Detector=""
    _Geometry=0    # Stores all the geometries objects for the current detector part.
    _Parameters=0  # Paraeters to go into __parameter table
    _Sensitive_Detector=0        #
    _Geometry_Table=""
    _Parameters_Table=""
    _Hit_Table=""
    _Banks_Table=""

    table_variation=0
    table_id = 0
    GemcVersion=0
    debug=0

# Special controls. Should not be needed.
    _always_commit = 0 # Set to 1 to commit immediately after each sql_execute()

    def __init__(self,detector,variation="original",iden=1,machine=0,user=0,passwd=0,database=0,gemcversion=2):
        """ Does nothing. Can print a hello """
        print("Init the GeometryEngine ")
        self._Detector = detector
        self.GemcVersion=gemcversion
        self.table_variation=variation
        self.table_id = iden
        self._Geometry=[]  # Must be done here, otherwise [] will leak across instances.
        self._Parameters=[]
        self._Sensitive_Detector=[]
        if machine != 0:
            self.MySQL_OpenDB(machine, user, passwd, database)

    def __del__(self):
        """ Clean up at exit """
        if self._DataBase:
            self._DataBase.commit()
            self._DataBase.close()

    def get_database(self):
        """ Return the database handle. You can use this to initialize other
        GeometryEngine objects with the same database. """
        return(self._DataBase)

    def set_database(self,db):
        """ Set the database handle to db. You can use this to initialize this
        GeometryEngine objects with the same database as another. """
        self._DataBase = db
        self._cursor = self._DataBase.cursor()

    def add(self,geom):
        """ Add a Geometry class object to the list of geometry objects """
        self._Geometry.append(geom)

    def add_sensitivity(self,sens):
        """ Add a Sensitive_Detector object to the hits definitions """
        self._Sensitive_Detector.append(sens)


    def find_volume_regex(self,name,flags=0):
        """ Find a particular geometry with a name that has regex match name from the Detector Geometry table.
            You can use a pattern: .*foo to search for 'foo' anywhere in the name.
            If you want to ignore case, pass flags=re.IGNORECASE (which is 2)"""
# OK, we usually have just one sensitive detector per geometry, so this might be over kill. Still it is handy.
        if not isinstance(self._Geometry,list):   # Are we initialized?
            print("This GeometryEngine appears not to be inialized")
            return(-1)

        prog = re.compile(name,flags)
        found = [x for x in self._Geometry if prog.match(x.name)]
        if len(found)==0:
            return(-1)

        return(found)

    def find_volume(self,name):
        """ Find a particular geometry with name=sens_name from the Detector Geometry table """
# OK, we usually have just one sensitive detector per geometry, so this might be over kill. Still it is handy.
        if not isinstance(self._Geometry,list):   # Are we initialized?
            print("GeometryEngine seems not to be inialized")
            return(-1)

        found = [x for x in self._Geometry if x.name == name]
        if len(found)==0:
            return(-1)

        if len(found)>1:
            print("Warning: More than one Detector Geometry with name:"+ name+" found in GeometryEngine")

        return(found[0])

    def find_children_regex(self,name,flags=0):
        """ Find those geometries that have name for their mother volume, from the Detector Geometry table.
            You can use a pattern: .*foo to search for 'foo' anywhere in the name.
            If you want to ignore case, pass flags=re.IGNORECASE (which is 2)"""
# OK, we usually have just one sensitive detector per geometry, so this might be over kill. Still it is handy.
        if not isinstance(self._Geometry,list):   # Are we initialized?
            print("The GeometryEngine is not inialized, oops.")
            return(-1)

        prog = re.compile(name,flags)
        found = [x for x in self._Geometry if prog.match(x.mother)]
        if len(found)==0:
            return(-1)

        return(found)

    def find_children(self,name):
        """ Find a list of geometries with mother=name from the Detector Geometry table """
# OK, we usually have just one sensitive detector per geometry, so this might be over kill. Still it is handy.
        if not isinstance(self._Geometry,list):   # Are we initialized?
            print("It seems that the GeometryEngine is not inialized")
            return(-1)

        found = [x for x in self._Geometry if x.mother == name]
        if len(found)==0:
            return(-1)

        return(found)

    def find_sensitivity(self,sens_name):
        """ Find a particular sensitivity with name=sens_name from the Sensitive Detector table """
# OK, we usually have just one sensitive detector per geometry, so this might be over kill. Still it is handy.
        if not isinstance(self._Sensitive_Detector,list):   # Are we initialized?
            print("Sensitive_Detector is not inialized")
            return(-1)

        found = [x for x in self._Sensitive_Detector if x.name == sens_name]
        if len(found)==0:
            if self.debug>1:
                print("Sensitive detector "+ sens_name+" not found in GeometryEngine")
            return(-1)

        if len(found)>1:
            print("Warning: More than one Sensitive detector with name:"+ sens_name+" found in GeometryEngine")

        return(found[0])


    def quick_add_cube(self,position):
        """Quick way to add a cube of 1mm size at position pos (in cm) relative to root, for testing
        purposes, color=red, material=vacuum"""
        cube = Geometry(
                        name="Test_Cube",
                        mother="root",
                        description="Test Cube",
                        pos= position,
                        pos_units="cm",
                        rot=[0,0,90],
                        rot_units="deg",
                        col="#ff0000",
                        g4type="Box",
                        dimensions=[0.1,0.1,0.1],
                        dims_units= "cm",
                        material="Vacuum",
                        sensitivity="no",
                        hittype="no",
                        identity="no"
                        )
        self._Geometry.append(cube)


    def MySQL_OpenDB(self,machine,user,passwd,database):
        """Open a MySQL database 'database' on host 'machine', with credentials 'user','passwd' """
        self._DataBase = MySQLdb.connect(machine,user,passwd,database)
        self._cursor = self._DataBase.cursor()
        self._DataBase.raise_on_warnings = True
        self._Geometry_Table = self._Detector + "__geometry"
        self._Parameters_Table = self._Detector + "__parameters"
        self._Hit_Table = self._Detector + "__hit"
        self._Banks_Table = self._Detector + "__bank"
        if self.debug:
            print("Database "+database+" opened for "+user+" on "+machine)
            print(self._DataBase)

    def MySQL_Table_Exists(self,table):
        """Returns True if the table exists, False if it does not. """
        if self.debug>2:
            print("Looking if table exists with name:'"+table+"'")

        sql = "show tables like '"+table+"'"
        n=self.sql_execute(sql)
        if self.debug>2:
            print("Search returns:'"+str(n)+"' = "+str(n!=0))

        return(n!=0)

    def MySQL_Get_Latest_Id(self,table,variation):
        """Find the lastest Id in the MySQL table for variation."""

        if self.debug>2:
            print("Looking up the max(id) for table '"+table+"'")
        if not self.MySQL_Table_Exists(table):
            if self.debug>0:
                print("ERROR: Table not found: '"+table+"'")
            return(None)

        sql = "select max(id) from "+table+" where variation = '"+variation+"';"
        n=self.sql_execute(sql)
        f = self._cursor.fetchone()
        if self.debug>2:
            print("From ID search we get '"+str(n)+"' with fetch: "+str(f))

        if f:
            if f[0]:
                return(f[0])
            else:
                return(0)
        else:
            return(0)

    def MySQL_Clean_Table(self,table,variation=-1,idn=-1):
        """Clean a table by deleting all entries in that table with variaion and id"""

        if not self.MySQL_Table_Exists(table):
            return(None)

        if variation == -1:
            variation = self.table_variation
        if idn == -1:
            idn = self.table_id

        if idn == -1:   # It is is still -1, then get the latest id from table for variation.
            idn = self.MySQL_Get_Latest_Id(table,variation)
            if not idn:
                return(0)

        if re.match('.*__hit',table) or re.match('.*__bank',table):
            if self.debug > 2:
                print("Cleaning a hit or bank table: "+table)
            sql = "delete from "+table+" where variation = '"+variation+"'"

        else:
            if self.debug > 2:
                print("Cleaning a geometry table: "+table)
            sql = "delete from "+table+" where variation = '"+variation+"' and id="+str(idn)

        n=self.sql_execute(sql)
        return(n)

    def MySQL_New_Tables(self):
        """ Create a new set of tables for this part of the detector.
            For GEMC1 - Only a geometery table is created with the name of the table
            For GEMC2 - A geometry table is created with the name table__geometry,
                        plus a tables with the name table__parameters, table__hit and table__bank

            Note that an existing Geometry table will be deleted first, while a Parameters table is preseved."""

        if self._Detector ==0:
            print("ERROR -- The detector does not appear to be correctly initialized.")
            return()

        self.MySQL_New_Geometry_Table()

        if self.GemcVersion == 2:
            # This will test to see if the parameters table exists, and if not, create one.
            self.MySQL_New_Parameters_Table()
            self.MySQL_New_Hit_Table()
            self.MySQL_New_Bank_Table()


    def MySQL_New_Geometry_Table(self,table=0):
        """Create a new Geometry Table in the database. If the table already exists, it is cleared first"""
                    # For GEMC 2, we need to add __geomtery to the name, if not already there.

        if table:
            self._Geometry_Table = table

        if self.MySQL_Table_Exists(self._Geometry_Table):   # Table already exists
            if self.debug:
                print("Geometry table: "+self._Geometry_Table+" already exists, no need to create.")
            return(False)

        sql="""CREATE TABLE `"""+ self._Geometry_Table +"""` (
            `name` varchar(40) DEFAULT NULL,
            `mother` varchar(100) DEFAULT NULL,
            `description` varchar(200) DEFAULT NULL,
            `pos` varchar(100) DEFAULT NULL,
            `rot` varchar(100) DEFAULT NULL,
            `col` varchar(10) DEFAULT NULL,
            `type` varchar(100) DEFAULT NULL,
            `dimensions` text,
            `material` varchar(60) DEFAULT NULL,
            `magfield` varchar(40) DEFAULT NULL,
            `ncopy` int(11) DEFAULT NULL,
            `pMany` int(11) DEFAULT NULL,
            `exist` int(11) DEFAULT NULL,
            `visible` int(11) DEFAULT NULL,
            `style` int(11) DEFAULT NULL,
            `sensitivity` varchar(40) DEFAULT NULL,
            `hitType` varchar(100) DEFAULT NULL,
            `identity` varchar(200) DEFAULT NULL,
            `rmin` int(11) DEFAULT NULL,
            `rmax` int(11) DEFAULT NULL,
            `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"""
        if self.GemcVersion == 2:
            sql += """ `variation` varchar(200) DEFAULT 'original',
                       `id`   int(11)  DEFAULT 0, """
        sql+= """UNIQUE KEY (`variation`,`id`,`name`)) ENGINE=MyISAM DEFAULT CHARSET=latin1;"""
        if self.debug:
            print("Creating Geometry table: "+self._Geometry_Table+".")

        n=self.sql_execute(sql)
        return(n)

    def MySQL_New_Parameters_Table(self):
        """Create a new Parameters table if it does not already exists.
           If it does already exist, keep the old one."""

        self._Parameters_Table = self._Detector + "__parameters"

        if self.MySQL_Table_Exists(self._Parameters_Table):   # Table already exists
            if self.debug:
                print("Parameters table: "+self._Parameters_Table+" already exists, no need to create.")

            return(False)

        sql="""create table IF NOT EXISTS """+ self._Parameters_Table +"""(
                       name              VARCHAR(250),
                       value             FLOAT,
                       units             VARCHAR(50),
                       description       VARCHAR(250),
                       author            VARCHAR(250),
                       author_email      VARCHAR(250),
                       pdf_drawing_link  VARCHAR(250),
                       drawing_varname   VARCHAR(250),
                       drawing_authors   VARCHAR(250),
                       drawing_date      VARCHAR(250),
                       upload_date       TIMESTAMP,
                       variation         VARCHAR(250),
                       rmin              INT,
                       rmax              INT,
                       id                INT,
                       PRIMARY KEY (variation, id, name) );"""
        if self.debug:
            print("Creating Parameters table: "+self._Parameters_Table+".")
        n=self.sql_execute(sql)
        return(n)

    def MySQL_New_Hit_Table(self):
        """Create a new hit table """

        if self.MySQL_Table_Exists(self._Hit_Table):   # Table already exists
            if self.debug:
                print("Hit table: "+self._Hit_Table+" already exists, no need to create.")
            return(False)

        sql="""create table IF NOT EXISTS """+ self._Hit_Table +"""( \
                        name            VARCHAR(100),        \
                        description     VARCHAR(200),        \
                        identifiers     TEXT,                \
                        signalThreshold VARCHAR(30),         \
                        timeWindow      VARCHAR(30),         \
                        prodThreshold   VARCHAR(30),         \
                        maxStep         VARCHAR(30),         \
                        riseTime        VARCHAR(30),         \
                        fallTime        VARCHAR(30),         \
                        mvToMeV               FLOAT,         \
                        pedestal              FLOAT,         \
                        delay           VARCHAR(30),         \
                        time              TIMESTAMP,         \
                        variation       VARCHAR(200),        \
                        PRIMARY KEY (variation, name)        );"""
        if self.debug:
            print("Creating Hit table: "+self._Hit_Table+".")
        n=self.sql_execute(sql)
        return(n)

    def MySQL_New_Bank_Table(self):
        """Create a new bank table """

        if self.MySQL_Table_Exists(self._Banks_Table):   # Table already exist
            if self.debug:
                print("Bank table: "+self._Banks_Table+" already exists, no need to create.")
            return(False)

        sql = """create table IF NOT EXISTS """ + self._Banks_Table + """ ( \
                        bankname        VARCHAR(100),            \
                        name            VARCHAR(100),            \
                        description     VARCHAR(200),            \
                        num             int,                     \
                        type            VARCHAR(10),             \
                        time            TIMESTAMP,               \
                        variation       VARCHAR(200),            \
                        PRIMARY KEY (bankname, name, variation)  );"""
        if self.debug:
            print("Creating Bank table: "+self._Banks_Table+".")

        n=self.sql_execute(sql)
        return(n)

    def MySQL_Write_Volume_RAW(self,name,mother,description,pos,rot,col,g4type,dimensions,material,magfield="no",ncopy=1,pmany=1,exist=1,visible=1,style=1,sensitivity="no",hittype="",identity="",rmin=1,rmax=10000):
        """
        You should not really need to use this call. Build geometries instead, then push them out to a DB or TXT.
        Write a line to the database table, for a volume described by args.
        Note that the database must have been initialized with MySQL_OpenDB and the table must exist.
        If the table does not exist or needs to be overwritten, call MySQL_New_Table first."""
#
# Note: Future update could check for the existence of table and the existence of "name" in the table.
#       If name already exists, print warning and update name instead of insert.
#
        sql="INSERT INTO "+self._Detector +" VALUES ('"
        sql+=name+"','"+mother+"','"+description+"','"
        sql+=pos+"','"+rot+"','"+col+"','"+g4type+"','"
        sql+=dimensions+"','"
        sql+=material+"','"
        sql+=magfield+"',"
        sql+=str(ncopy)+","
        sql+=str(pmany)+","
        sql+=str(exist)+","
        sql+=str(visible)+","
        sql+=str(style)+",'"
        sql+=sensitivity+"','"
        sql+=hittype+"','"
        sql+=identity+"',"
        sql+=str(rmin)+","
        sql+=str(rmax)+",now())"
        self._cursor.execute(sql)

    def sql_execute(self,sql):
        """Utility to wrap the SQL executing in a nice way. """
        n=0
#        n=self._cursor.execute(sql)

        if self.debug>3:
            print("SQL = " + sql)

        try:
            n=self._cursor.execute(sql)
            if self.debug>3:
                print("n = " + str(n))
                print("#rows = "+str( self._DataBase.affected_rows() ))
        except _mysql_exceptions.Warning as e:
            if self.debug:
                print("MySQL Warning: "+str(e))
                print("For SQL: "+sql)
        except Exception as e:
            print(e)
            print("Unexpected error:", sys.exc_info()[0])
            print("For SQL: "+sql)
            raise

        if self._always_commit:
            self._DataBase.commit()  # Make sure the transaction is completed.
        return(n)

    def MySQL_Write(self):
        """ Write out all the tables for the detector to the database.
            Note that the database must be initialized first with MySQL_OpenDB """

        if not self._DataBase:
            print("ERROR -- Database was not initialized.")
            return()

        self.MySQL_New_Tables()
        self.MySQL_Write_Geometry()
        self.MySQL_Write_Hit()
        self.MySQL_Write_Bank()


    def MySQL_Write_Geometry(self):
        """ Write the entire geometry to the MySQL table _Detector. """

        if self.table_id <= 0:
            self.table_id = self.MySQL_Get_Latest_Id(self._Geometry_Table,self.table_variation) +1

        self.MySQL_Clean_Table(self._Geometry_Table, self.table_variation,self.table_id)

        if self.debug:
            print("Writing out the geometry MySQL table for "+self._Detector+" with variation="+self.table_variation+" and id="+str(self.table_id))

        for geo in self._Geometry:
            self.MySQL_Write_Volume(geo)


    def MySQL_Write_Volume(self,volume):
        """ Write the Geometry class object 'volume' to the MySQL table _Detector with 'variation' and 'iden' """

        if not isinstance(volume,Geometry):
            print("ERROR: Asked to write an object that is not a Geometry to the MySQL tables.")
            return(1)

        if self.debug >3:
            print("Writing the geometry: "+volume.name)
        sql = volume.MySQL_str(self._Geometry_Table,self.table_variation,self.table_id)
#        if self.debug > 30:
#            print "Insertion SQL: "+ sql
        n=self.sql_execute(sql)
        return(n)


    def MySQL_Write_Hit(self):
        """ Write the MySQL detector__hit table. """

        self.MySQL_Clean_Table(self._Hit_Table, self.table_variation,self.table_id)

        for sens in self._Sensitive_Detector:
            sql = sens.hit_MySQL_str(self._Hit_Table,self.table_variation,self.table_id)
            if self.debug>2:
                print("Writing the hit:"+sens.name)
                print("SQL: "+sql)
            self.sql_execute(sql)

    def MySQL_Write_Bank(self):
        """ Write the MySQL detector__bank table. """

        self.MySQL_Clean_Table(self._Banks_Table, self.table_variation,self.table_id)

        for sens in self._Sensitive_Detector:
            sql = sens.bank_MySQL_str(self._Banks_Table,self.table_variation,self.table_id)
            if self.debug>2:
                print("Writing the hit:"+sens.name)
                print("SQL: "+sql)
            self.sql_execute(sql)



    def MySQL_Read_Geometry(self,table,variation='original',idn=0):
        """Read the geometry from the MySQL table and store the result in this GeometryEngine.
           For GEMC 2 tables, specify which variation ('original' is default) and
           and which id (idn=0 is default) you want."""
#
# We first determine if this is a GEMC 1 or GEMC 2 table.
# If it has a column called "variation" then assume it is GEMC 2
# We find out by having MySQL describe the table.
        sql = "describe "+table
        err = self._cursor.execute(sql)
        if err <= 0:
            print("Error executing MySQL command: describe "+table)
            return()

        ltable = self._cursor.fetchall()
        vari = [x for x in ltable if x[0] == "variation"]
        table_version = 2
        if len(vari) == 0:  # variation not found
            table_version = 1
            print("Note: GEMC version 1 table found. Variation and id ignored")

        sql = "select name,mother,description,pos,rot,col,type,dimensions,"
        sql+= "material,magfield,ncopy,pMany,exist,visible,style,sensitivity,hitType,identity,"
        sql+= "rmin,rmax,time from "+table
        if table_version == 2:
            sql+=" where variation='"+variation+"' and id="+idn

        self._cursor.execute(sql)
        result = self._cursor.fetchall() # Get the whole table in one call

        for x in result:
            self.add(Geometry(x))   # Store each line in a Geometry, place Geometry in GeometryEngine

        return

    def Python_Write(self,variation=0,idn=0,indent=0):
        """Write the Geometries out as a template Python method: calculate_<detector>_geometry
           into a file called Template_<detector>.py
           The code will be indented by 'indent' spaces.
            """
        fname = "Template_"+self._Detector+".py"
        ff= open(fname,"w")

        s = ' '*indent+"from GeometryEngine import Geometry\n\n"
        ff.write(s)
        s = ' '*indent+"def calculate_"+self._Detector+"_geometry(g_en):\n"
        ff.write(s)
        s = ' '*(indent+4)+'"""Auto generated method. """\n\n'
        ff.write(s)

        for geo in self._Geometry:
            s = geo.Python_str(indent+4)
            ff.write(s+'\n')
            ff.write(' '*(indent+4)+"g_en.add(geo)\n\n")

        return

    def TXT_Read_Geometry(self,file=0):
        """Read in a GEMC TXT file format geometry.
           If you specify 'file=' then exactly that file will be read.
           If you specify nothing, then the detectorname__geometry_original.txt will be read. """
        if not file:
            file= self._Detector+"__geometry_original.txt"

        fin = open(file,"r")

        for line in fin:
            obs = line.strip().split("|")   # Split line on the |
            obs = [x.strip() for x in obs]  # Squeeze leading and trailing whitespace for each term.
            if self.debug > 1:
                print(obs)
            geo = Geometry(obs)
            nerr = geo.Validate()
            if nerr:
                print("Validation error number="+str(nerr)+" for line:")
                print(line)
            self.add(geo)         # Create a Geometry based on line and put in GeometryEngine

        return

    def TXT_Write(self,variation=0):
        """ Write all the TXT files"""
        if variation == 0:
            variation=self.table_variation

        self.TXT_Write_Geometry(variation)
        self.TXT_Write_Banks(variation)
        self.TXT_Write_Hits(variation)

    def TXT_Write_Geometry(self,variation="original"):
        """ Write the entire geometry to a TXT file called detector__geometry_variation.txt """

        fname = self._Detector + "__geometry_" + variation + ".txt"
        ff = open(fname,"w")

        for geo in self._Geometry:
                ff.write(geo.__str__()+"\n")

        ff.close()

    def TXT_Write_Hits(self,variation="original"):
        """ Write the hit definitions to a TXT file called detector__hits_variation.txt """

        fname = self._Detector + "__hit_" + variation + ".txt"
        ff = open(fname,"w")

        if self._Sensitive_Detector == 0:
            print("ERROR:  No hits were defined!")
            return()

        for hit  in self._Sensitive_Detector:
                ff.write(hit.hit_str()+"\n")

        ff.close()

    def TXT_Write_Banks(self,variation="original"):
        """ Write the bank definitions to a TXT file called detector__banks_variation.txt """

        fname = self._Detector + "__bank.txt"
        ff = open(fname,"w")

        if self._Sensitive_Detector == 0:
            print("ERROR:  No hits were defined!")
            return()

        for hit  in self._Sensitive_Detector:
                ff.write(hit.bank_str()+"\n")

        ff.close()

    def __str__(self):
        """ Return string with list of what the geometry currently contains. """
        s_out=""
        if isinstance(self._DataBase,MySQLdb.connection):
            s_out = "Database: " + self._DataBase.get_host_info() + "\n"
        else:
            s_out = "Database:  NOT OPENED \n"
        s_out+= "Table   : " + self._Detector + "\n"
        s_out+= "Geometry: \n"
        for i in self._Geometry:
            s_out+= "     "+i.name+" in "+i.mother+ " ::"+i.description+"\n"

        return(s_out)


def testsuite():
    """Test the GeometryEngine class, currently does nothing, sorry"""
    print("I am not really implemented yet. Sorry")

if __name__ == "__main__":
    testsuite()
