#!/usr/bin/python
#
# Python Rotations Library.
# Based on CLHEP HepRotation
#
# Notes on the use of np.matrix
#
# np.matrix.item(y,x) returns the y row and the x column.
import numpy as np
import math

class Rotation(np.matrix):
    """A class for manipulating rotation matrixes.
    Based on the HepRotation from CLHEP, without
    requiring an import of the C++ CLHEP classes.
    The latter would be more efficient, computationally,
    but requires the whole Geant4 g4py, which is unusual."""

    def __new__(subtype, data=None, dtype=None, copy=False):
        #print "New Rotation called"
        # make the matrix array, copy if needed.
        submatrix=1
        if data is not None:
            if isinstance(data,float) or isinstance(data,int):
                submatrix = Rotation().rotateX(data)
            elif isinstance(data,Rotation) or isinstance(data,np.matrix) or isinstance(data,np.ndarray) or isinstance(data,list):
                submatrix = np.matrix(data,dtype=dtype,copy=copy)
                if submatrix.shape != (3,3):
                    print("Must initialize with a 3x3 matrix")
                    raise ValueError
            else:
                print("Unexpected data type to Rotation")
                raise TypeError


        else:
            submatrix = np.matrix(np.identity(3))

        # Transform the matrix in to a Rotation
        submatrix = submatrix.view(subtype)

        return submatrix

    def rotateX(self,theta):
        """Rotate around the X axis by theta in radians."""
        si = math.sin(theta)
        co = math.cos(theta)
        rm = Rotation([[1,0,0],[0,co,-si],[0,si,co]])
        new = rm * self
        return new


    def rotateY(self,theta):
        """Rotate around the X axis by theta in radians."""
        si = math.sin(theta)
        co = math.cos(theta)
        rm = Rotation([[co,0,si],[0,1,0],[-si,0,co]])
        new = rm * self
        return new

    def rotateZ(self,theta):
        """Rotate around the X axis by theta in radians."""
        si = math.sin(theta)
        co = math.cos(theta)
        rm = Rotation([[co,-si,0],[si,co,0],[0,0,1]])
        new = rm * self
        return new

    def rotateXYZ(self,angles):
        """Rotate around the angles list [x,y,z] as an X, then Y then Z rotation"""
        new = Rotation().rotateX(angles[0]).rotateY(angles[1]).rotateZ(angles[2])
        return new

    def rotateG4XYZ(self,angles):
        """Rotate around the angles list [x,y,z] as an -Z, then -Y then -X rotation"""
        new = Rotation().rotateZ(-angles[2]).rotateY(-angles[1]).rotateX(-angles[0])
        return new

    def Test(self):
        """Test if the matrix (still) represents a rotation"""
        test= np.linalg.det(self)

        if test > 0.999999999999:
            return True
        else:
            return False


    def GetXYZ(self):
        """Returns angles (theta,phi,psi) for:
        an x-rotation by theta followed by a y-rotation by phi followed by a z-rotation by psi.
        Active rotations are considered here.
        This works in all quadrants
        Note that if you construct a rotation matrix using three angles, you may not get back the same angles from this function
        (i.e. the mapping between xyz-angle-triples and SO(3) is not 1-1)"""

        phi=math.asin(-self.item(2,0))
        if math.fabs(1.-math.fabs(self.item(2,0)))>.0000001:
# if the phi rotation is not by pi/2 or -pi/2 (two solutions, return one of them)
            psi=math.atan2(self.item(1,0),self.item(0,0))
            theta=math.atan2(self.item(2,1),self.item(2,2))
        else:
# if the phi rotation was in fact by pi/2 or -pi/2 (infinite solutions- pick 0 for psi and solve for theta)
            psi=0
            if math.fabs(self.item(1,1))>.0000001 and math.fabs(self.item(0,2))>.0000001: #as long as these matrix elements aren't 0
                if math.copysign(1,self.item(1,1))==math.copysign(1,self.item(0,2)):
# relative sign of these matrix elements determines theta's quadrant
                    theta=math.atan2(self.item(0,1),self.item(1,1))
                else:
                    theta=math.atan2(-self.item(0,1),self.item(1,1))
            else:
# if one of them was zero then use these matrix elements to test for theta's quadrant instead
                if not math.copysign(1,self.item(0,1))==math.copysign(1,self.item(1,2)):
# relative sign of these matrix elements determines theta's quadrant
                    theta=math.atan2(self.item(0,1),self.item(1,1))
                else:
                    theta=math.atan2(-self.item(0,1),self.item(1,1))

        return (theta,phi,psi)

    def GetG4XYZ(self):
        """Returns angles (theta,phi,psi) for:
        an x-rotation by theta followed by a y-rotation by phi followed by a z-rotation by psi.
        Passive rotations are considered here, as expected for a GEANT4 rotation. That is equivalent to
        an Active rotation by -psi,-phi,-theta in the ZYX order.
        Note that this is the same algorithm as GetXYZ(), for the INVERSE matrix.
        This works in all quadrants
        Note that if you construct a rotation matrix using three angles, you may not get back the same angles from this function
        (i.e. the mapping between xyz-angle-triples and SO(3) is not 1-1)"""

        phi= -math.asin(self.item(0,2))

        if math.fabs(1.-math.fabs(self.item(0,2))) >  0.0000001:
            psi  = math.atan2(self.item(0,1),self.item(0,0))
            theta= math.atan2(self.item(1,2),self.item(2,2))
        else:
            psi = 0
            if math.fabs(self.item(1,1)) > 0.0000001 and math.fabs(self.item(2,0))>0.0000001:
                if math.copysign(1,self.item(1,1)) == math.copysign(1,self.item(2,0)):
                    theta=math.atan2(self.item(1,0),self.item(1,1))
                else:
                    theta=math.atan2(-self.item(1,0),self.item(1,1))
            else:
                if not math.copysign(1,self.item(1,0)) == math.copysign(1,self.item(2,1)):
                    theta=math.atan2(self.item(1,0),self.item(1,1))
                else:
                    theta=math.atan2(-self.item(1,0),self.item(1,1))

        return(theta,phi,psi)


    def __mul__(self,other):
        """Redefined multiply calls np.matrix.__mult__ and then casts to Vector """
        out = self.view(np.matrix)*other

        if isinstance(other,Vector):
            out = out.view(Vector)
        elif isinstance(other,Rotation):
            out = out.view(Rotation)

        return out


##############################################################################################################
#
#  Useful methods for working with vectors = column matrixes, or row matrizes.
#
##############################################################################################################

class Vector(np.matrix):
    """ A class to simplify the matrix objects that behave as 3 vectors, i.e. a 1-d column matrix.
        This is a type of vector that you can multiply by a Rotation at Rotarion*Vector"""

    def __new__(subtype, data=None, dtype=None, copy=False):
        submatrix=1
        if data is not None:
            if isinstance(data,float) or isinstance(data,int):
                if data == 0:
                    submatrix = np.matrix((0,0,0)).T
                elif data == 1:
                    submatrix = np.matrix((1,0,0)).T
                elif data == 2:
                    submatrix = np.matrix((0,1,0)).T
                elif data == 3:
                    submatrix = np.matrix((0,0,1)).T
                else:
                    print("Not understanding initializiation parameter: ",data)
                    raise ValueError

            elif isinstance(data,Rotation) or isinstance(data,Vector) or isinstance(data,np.matrix) or isinstance(data,np.ndarray) or isinstance(data,list) or isinstance(data,tuple):
                submatrix= np.matrix(data,dtype=dtype,copy=copy)
                if submatrix.shape == (1,3):
                    submatrix = submatrix.T
                elif submatrix.shape != (3,1):
                    print("A vector must be a (3,1) shape column matrix type. This is ",submatrix.shape)
                    raise ValueError
                else:
                    print("Unexpected type to a Vector")
                    raise TypeError
        else:
            submatrix = np.matrix((0,0,0))

        submatrix = submatrix.view(subtype)   # Transform matrix to Vector type.
        return submatrix

    def length(self):
        """Return the length of the vector """
        return math.sqrt(self.item(0)*self.item(0) + self.item(1)*self.item(1) + self.item(2)*self.item(2) )

    def rho(self):
        """Return the lenght of the vector in the x-y plane"""
        return math.sqrt(self.item(0)*self.item(0) + self.item(1)*self.item(1) )

    def theta(self):
        """Return the polar angle of the vector in radians """
        z = self.item(2)
        rho = self.rho()
        return math.atan2(rho,z)

    def phi(self):
        """Return the azimuthal angle of the vector, i.e. angle wrt to the x axis around the z axis"""
        return math.atan2( self.item(1),self.item(0))

    def x(self):
        """ Return the x component, or actually self.item(0) """
        return self.item(0)

    def y(self):
        """ Return the x component, or actually self.item(1) """
        return self.item(1)

    def z(self):
        """ Return the x component, or actually self.item(2) """
        return self.item(2)

    def list(self):
        """ Return the list [x,y,z] """
        l = [self.item(i) for i in range(3)]
        return l


    def __mul__(self,other):
        """Redefined multiply calls np.matrix.__mult__ and then casts to Vector """
        out = self.view(np.matrix)*other
        if isinstance(other,Rotation) or isinstance(other,np.matrix):

            out = out.view(Vector)



        return out

def testsuite():
    """Test the Rotation class"""

    import random
    maxdif = 0
    for i in range(100000):
        alpha = random.uniform(-math.pi,math.pi);
        beta  = random.uniform(-math.pi,math.pi);
        gamma = random.uniform(-math.pi,math.pi);
        r = Rotation().rotateXYZ((alpha,beta,gamma))
        r2 = Rotation().rotateXYZ(r.GetXYZ())

        if ( (r - r2)> 1.e-4).any() :  # If any element differs more than one in 10^6
            if maxdif < (r-r2).max():
                maxdif = (r-r2).max()

            print("Difference for: ("+str(alpha)+","+str(beta)+","+str(gamma)+" != "+str(r.GetXYZ()))
            print(" r = \n"+str(r))
            print(" r2= \n"+str(r2))
            print("----------------------")
            print(" r-r2 = " + str(r-r2))
            print("----------------------")
            print("")

        r3 = Rotation().rotateG4XYZ((alpha,beta,gamma))
        r4 = Rotation().rotateG4XYZ(r3.GetG4XYZ())

        if ( (r3 - r4)> 1.e-4).any() :  # If any element differs more than one in 10^6
            if maxdif < (r3-r4).max():
                maxdif = (r3-r4).max()

            print("Difference for: ("+str(alpha)+","+str(beta)+","+str(gamma)+" != "+str(r3.GetG4XYZ()))
            print(" r3 = \n"+str(r3))
            print(" r4 = \n"+str(r4))
            print("----------------------")
            print(" r3-r4 = " + str(r3-r4))
            print("----------------------")
            print("")

    print("Max difference found: "+str(maxdif))

if __name__ == "__main__":
    testsuite()
