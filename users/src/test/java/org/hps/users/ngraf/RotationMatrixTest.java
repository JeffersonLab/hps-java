package org.hps.users.ngraf;

import static java.lang.Math.PI;
import java.util.Arrays;
import junit.framework.TestCase;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * This class shows how to manipulate (create, extract, use) the rotation 
 * matrices used in our geometry system.
 * Geant4 employs the RotationOrder.XYZ
 * rotate about x by alpha1
 * rotate about y' by alpha2
 * rotate about z" by alpha3
 * Common names for these are:
 * 
 * o Tait-Bryan angles
 * o Cardan angles
 * o Heading, Elevation and Bank
 * o Yaw, Pitch and Roll
 * 
 * Note that the angles are not necessarily unique.
 * In principle this class can throw the following exception
 * 
 * CardanEulerSingularityException - if the rotation is singular with respect to the angles set specified
 * 
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class RotationMatrixTest extends TestCase
{

    private boolean debug = true;

    public void testRotationMatrix()
    {
        Vector3D axisX = new Vector3D(1., 0., 0.);
        Vector3D axisY = new Vector3D(0., 1., 0.);
        Vector3D axisZ = new Vector3D(0., 0., 1.);

        double alpha1 = PI / 4.;
        double alpha2 = PI / 4.;
        double alpha3 = 0.;

        //set up a rotation by alpha1 about the X axis
        Rotation r1 = new Rotation(axisX, alpha1);

        // find y' and z'
        Vector3D axisYPrime = r1.applyTo(axisY);
        Vector3D axisZPrime = r1.applyTo(axisZ);

        if(debug) System.out.println("axisYPrime: " + axisYPrime);
        if(debug) System.out.println("axisZPrime: " + axisZPrime);

        //set up a rotation by alpha2 about the Y' axis
        Rotation r2 = new Rotation(axisYPrime, alpha2);

        //find z''
        Vector3D axisZPrimePrime = r2.applyTo(axisZPrime);
        if(debug) System.out.println("axisZPrimePrime: " + axisZPrimePrime);

        //set up a rotation by alpha3 about the Z'' axis
        Rotation r3 = new Rotation(axisZPrimePrime, alpha3);

        //create a test vector u
        Vector3D u = new Vector3D(1., 1., 0.);

        //rotate u by angle alpha 1 about X
        Vector3D u1 = r1.applyTo(u);
        //rotate by angle alpha 2 about Y'
        Vector3D u2 = r2.applyTo(u1);
        //rotate by angle alpha 3 about Z''
        Vector3D u3 = r3.applyTo(u2);

        // now create the rotation matrix in one go...
        Rotation active = new Rotation(RotationOrder.XYZ, alpha1, alpha2, alpha3);

        // apply to u
        Vector3D v = active.applyTo(u);

        //compare u3 to v
        if(debug) System.out.println("u3: " + u3);
        if(debug) System.out.println("v : " + v);

        // Build one of the rotations that transforms one vector u into another one v.
        Rotation r = new Rotation(u, v);

        // retrieve the Cardan angles (recall that these are not necessarily unique
        double[] angles = r.getAngles(RotationOrder.XYZ);
        if(debug) System.out.println(Arrays.toString(angles));

        // create a rotation matrix from these angles
        Rotation passive = new Rotation(RotationOrder.XYZ, angles[0], angles[1], angles[2]);

        // rotate u
        Vector3D w = passive.applyTo(u);
        
        // unrotate v
        
        Vector3D vPrime = passive.applyInverseTo(v);
        
        // compare v to w
        if(debug) System.out.println("v: " + v);
        if(debug) System.out.println("w: " + w);
        
        // compare u to vPrime
        if(debug) System.out.println("u: " + u);
        if(debug) System.out.println("v': " + vPrime);
    }
}
