/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.recon.tracking.kalman;

import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.solids.Trd;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;

/**
 *
 * @author ecfine
 */
public class TrdHelper implements ShapeHelper{
    PrintDetectorElements printDetectorElements = new PrintDetectorElements();

    /* Prints that solid is trapezoid */
    public void printShape(ISolid solid) {
        checkTrapezoid(solid);
        System.out.println("Shape: Trapezoid");
    }

    /* Prints the local coordinates of the trapezoid */
    public void printLocalCoords(ISolid solid){
        checkTrapezoid(solid);
        Trd trap = (Trd) solid;
        System.out.println("Local Coordinates: " + trap.getVertices().get(0));
         for (int n = 1; n < trap.getVertices().size(); n ++) {
            printDetectorElements.printIndent();
            System.out.println("                    " + trap.getVertices().get(n));
        }
    }

    /* Prints the global coordinates of the trapezoid */
    public void printGlobalCoords(ISolid solid){
        checkTrapezoid(solid);
        Trd trap = (Trd) solid;
         for(int i = 0; i < trap.getVertices().size(); i++) {
            Hep3Vector currentPoint = (Hep3Vector) trap.getVertices().get(i);
            Hep3Vector transformedPoint = (Hep3Vector) currentPoint;
            for(int k = PrintDetectorElements.physicalVolumes.size(); k > 0; k--) {
                ITransform3D lastTransform = (ITransform3D) PrintDetectorElements.physicalVolumes.get(k-1);
                currentPoint = lastTransform.transformed(currentPoint);
            }
            transformedPoint = currentPoint;
            printDetectorElements.printIndent();
            if (i == 0){
                System.out.println("Global Coordinates: [    " + transformedPoint.x() +
                        ",      " + transformedPoint.y() + ",        " + transformedPoint.z() +"]");
            } else{
                 System.out.println("                     [     " + transformedPoint.x() +
                        ",       " + transformedPoint.y() + ",      " + transformedPoint.z() +"]");
            }
        }
    }

    public void findIntersection(ISolid solid, Track track) {
        checkTrapezoid(solid);
        Trd trap = (Trd) solid;
        ShapeDispatcher shapedis = new ShapeDispatcher();
        HelicalTrackFit helix = shapedis.trackToHelix(track);
        List<Polygon3D> faces = trap.getFaces();
        
        // For each face, find the vertices
        for (int i = 0; i < faces.size(); i++) {
            Polygon3D face = faces.get(i);
            Hep3Vector localNormal = face.getNormal();
            Hep3Vector normal = getGlobalRot(localNormal); // get the normal in global coords
            List<Hep3Vector> vertices = new ArrayList<Hep3Vector>();

            // For each vertex, find and print the Global coordinates
            for(int k = 0; k < face.getVertices().size(); k++){
                Hep3Vector localVertex = (Hep3Vector) face.getVertices().get(k);
//                System.out.println("local vertex: " + localVertex);
                Hep3Vector vertex = getGlobalCoords(localVertex);
                System.out.println("vertex: " + vertex);
                vertices.add(vertex);
            }
//            System.out.println("local normal: " + localNormal);
            System.out.println("normal: " + normal);
            if (0.01 > normal.z() && normal.z() > -0.01){
                if (org.lcsim.fit.helicaltrack.HelixUtils.isInterceptingBoundedXYPlane(helix, normal, vertices)) {
                    System.out.println("INTERSECTING BOUNDED PLANE!");
                } else {
                    System.out.println("No intersection");
                }
                if (org.lcsim.fit.helicaltrack.HelixUtils.isInterceptingXYPlane(helix, normal, vertices.get(0))) {
                    System.out.println("INTERSECTING PLANE!");
                } else {
                    System.out.println("No intersection");
                }
            } else {
                System.out.println("wrong orientation");
            }
        }
    }


    // This is (clearly) a work in progress...
   public KalmanSurface getKalmanSurf(ISolid solid) {
        KalmanSurface surf = null;
//        checkTrapezoid(solid);
//        Trd trap = (Trd) solid;
//        List<Polygon3D> faces = trap.getFaces();
//        /* This needs to decide what surface the trapezoid should be modeled as.
//         * I suspect xy plane and z plane are the main choices. For now, it's
//         * just going to default to an xy plane, but that should be revisited. */
////        List vertices = trap.getVertices();
//
//        // From a list of 8 vertices, need to match the pairs with the smallest difference
//        // between them. Or alternatively, need to find the two biggest squares. That
//        // may actually be a better way to do things... could do it using faces that way?
//
//        ArrayList areas = new ArrayList();
//        for (int i = 0; i < faces.size(); i++) {
//            Polygon3D face = faces.get(i);
//            double area = findArea(face);
//            areas.add(area);
//        }
//        for (int k )
//
//
//            Hep3Vector localNormal = face.getNormal();
//            Hep3Vector normal = getGlobalRot(localNormal); // get the normal in global coords
//            List<Hep3Vector> vertices = new ArrayList<Hep3Vector>();
//            Hep3Vector planeNormal = null;
//            // Only want to make surface for xy plane faces
//            if (0.01 > normal.z() && normal.z() > -0.01){
//                // For each vertex, find the Global coordinates & add to vertices
//                for(int k = 0; k < face.getVertices().size(); k++){
//                    Hep3Vector localVertex = (Hep3Vector) face.getVertices().get(k);
//                    Hep3Vector vertex = getGlobalCoords(localVertex);
//                    vertices.add(vertex);
//                    planeNormal = normal;
//                }
//             /* From the 8 vertices, we want one origin point, since
//             * the origin and a vector define the plane. Need a single x & y value.
//             * Here, we find the smallest difference in x and y values between two
//             * points. Once we find this pair, we can average the x and y values
//             * for our origin point. */
//                double minxDiff = (vertices.get(0).x() - vertices.get(1).x());
//                double minyDiff = (vertices.get(0).y() - vertices.get(1).y());
//                int secondPoint = 1;
//                for (int j = 2; j < vertices.size(); j++){
//                    double xDiff = vertices.get(0).x() - vertices.get(j).x();
//                    double yDiff = vertices.get(0).y() - vertices.get(j).y();
//                    if (xDiff < minxDiff && yDiff < minyDiff) {
//                        minxDiff = xDiff;
//                        minyDiff = yDiff;
//                        secondPoint = j;
//                    }
//                }
//                // Average the x, y, and z values of the two points to find origin.
//                Hep3Vector doubleMidPoint = VecOp.add(vertices.get(0), vertices.get(secondPoint));
//                Hep3Vector origin = VecOp.mult(.5, doubleMidPoint);
//
//               /* Make a new surface from the origin and plane normal as found
//                * above. */
//
//                double distnorm = (planeNormal.x() * origin.x() + planeNormal.y() * origin.y()
//                        + planeNormal.z() * origin.z())/planeNormal.magnitude();
//                double normphi = Math.atan2(planeNormal.y(), planeNormal.x());
//                if (normphi < 0){
//                    normphi = normphi + TRFMath.TWOPI;
//                }
//                System.out.println("Plane normal = " + planeNormal + ", origin = " + origin);
//                surf = new KalmanSurface("xyplane", distnorm, normphi);
//            }
//
//
//

        return surf;
    }

    boolean pointIsOnSolid(ISolid solid, Point3D hitPoint) {
        boolean onSolid = true;
        checkTrapezoid(solid);
        Trd trap = (Trd) solid;
        List faces = trap.getFaces();
        // Want to find all projections for each face, and see if projections
        // of the hit point are inside them. This is almost certainly not the
        // most efficient way to see whether the point is in the solid, but
        // I'm not really sure how else to deal with it.
        for(int i = 0; i < faces.size(); i++){
            Polygon3D face = (Polygon3D) faces.get(i);
            List vertices = face.getVertices();
            ArrayList xProjVertices = new ArrayList();
            ArrayList yProjVertices = new ArrayList();
            ArrayList zProjVertices = new ArrayList();
            for (int k = 0; k < vertices.size(); k ++){
                Point3D currentPoint = (Point3D) vertices.get(k);
                double[] xProj = new double[2];


            }
        }
        return onSolid;
    }

    // Finds the area of an arbitrary quadrilateral. See http://softsurfer.com/
    // Archive/algorithm_0101/algorithm_0101.htm#Quandrilaterals for more info.
    private double findArea(Polygon3D face) {
        double[] v2_v0 = new double[3];
        v2_v0[0] = face.getVertices().get(2).x() - face.getVertices().get(0).x();
        v2_v0[1] = face.getVertices().get(2).y() - face.getVertices().get(0).y();
        v2_v0[2] = face.getVertices().get(2).z() - face.getVertices().get(0).z();
        double[] v3_v1 = new double[3];
        v3_v1[0] = face.getVertices().get(3).x() - face.getVertices().get(1).x();
        v3_v1[1] = face.getVertices().get(3).y() - face.getVertices().get(1).y();
        v3_v1[2] = face.getVertices().get(3).z() - face.getVertices().get(1).z();
        double area = .5 * magnitude(crossProduct(v2_v0, v3_v1));
        return area;
    }

    // Finds the cross product of two 3d vectors, represented as double[]s.
    private double[] crossProduct(double[] v1, double[] v2){
        double[] cross = new double[3];
        cross[0] = v1[1]*v2[2] - v2[1]*v1[2];
        cross[1] = v1[2]*v2[0] - v2[2]*v1[0];
        cross[2] = v1[0]*v2[1] - v2[0]*v1[1];
        return cross;
    }

    // Finds the magnitude of a 3d vector, represented as a double[]
    private double magnitude(double[] v){
        double mag = Math.sqrt(Math.pow(v[0], 2) + Math.pow(v[1], 2) + Math.pow(v[2], 2));
        return mag;
    }
//
//    public HTrack addIntercept(ISolid solid, HTrack ht) {
//
//    }



    
    /* Finds the global coordinates of a local point */
    private Hep3Vector getGlobalCoords(Hep3Vector local){
        Hep3Vector currentPoint = local;
        for(int k = PrintIntersections.physicalVolumes.size(); k > 0; k--) {
            ITransform3D lastTransform = (ITransform3D) PrintIntersections.physicalVolumes.get(k-1);
            currentPoint = lastTransform.transformed(currentPoint);
        }
        return currentPoint;
    }

    /* Finds the global rotation of a local point. Use this for normals, where
     the translation is irrelevant. */
    private Hep3Vector getGlobalRot(Hep3Vector local){
        Hep3Vector currentPoint = local;
        for(int k = PrintIntersections.physicalVolumes.size(); k > 0; k--) {
            ITransform3D lastTransform = (ITransform3D) PrintIntersections.physicalVolumes.get(k-1);
            IRotation3D lastRotation = lastTransform.getRotation();
            currentPoint = lastRotation.rotated(currentPoint);
        }
        return currentPoint;
    }

    /* Checks that solid is really trapezoid */
    public void checkTrapezoid(ISolid solid){
        if (solid instanceof Trd) {
         } else {
            System.out.println("Error! This shape is not a trapezoid!");
            return;
        }
    }

}
