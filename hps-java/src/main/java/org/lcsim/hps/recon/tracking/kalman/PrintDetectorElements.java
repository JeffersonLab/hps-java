/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lcsim.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.List;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.geometry.Detector;

/**
 *
 * @author ecfine
 */

public class PrintDetectorElements{
    ShapeDispatcher shapeDispatcher = new ShapeDispatcher();
    static int indent = 0;
    List<Point3D> coords = null;
    static ArrayList physicalVolumes = new ArrayList();
    Boolean printing = true;


    public void run(Detector detector, Boolean print) {
       printing = print;
       if (printing) {
           System.out.println();
           System.out.println();
           System.out.println("Detector name is " + detector.getDetectorName());
       }
       ILogicalVolume logical = getLogicalVolume(detector);
       loopThroughDaughters(logical);
    }

    private ILogicalVolume getLogicalVolume(Detector det) {
         ILogicalVolume logical = det.getTrackingVolume().getLogicalVolume();
         return logical;
    }

    private void loopThroughDaughters(ILogicalVolume logical) {
        if (printing) {
            printIndent();
            System.out.println("Logical Volume: " + logical.getName());
            indent ++;
            printIndent();
            System.out.println("Solid: " + logical.getSolid().getName());
            printIndent();
            System.out.println("Material: " + logical.getMaterial().getName());
            printIndent();
            System.out.println("Volume: " + logical.getSolid().getCubicVolume());
            printIndent();
            shapeDispatcher.printShape(logical.getSolid());
            printIndent();
            shapeDispatcher.printLocalCoords(logical.getSolid());
            shapeDispatcher.printGlobalCoords(logical.getSolid());
            System.out.println();
            indent ++;
        }
        for (int n = 0; n < logical.getNumberOfDaughters(); n++) {
            IPhysicalVolume physical = logical.getDaughter(n);
            loopThroughDaughters(physicalToLogical(physical));
            indent = (indent - 2);
            physicalVolumes.remove(physicalVolumes.size() - 1);
        }
    }


    // Prints the information in the physical volume and returns the logical
    // volume associated with the physical volume. Adds the physical volume to
    // the physicalVolumes ArrayList. Obtains the local rotation and caluculates
    // global rotation.
    private ILogicalVolume physicalToLogical(IPhysicalVolume physical) {
        physicalVolumes.add(physical.getTransform());
        if (printing){
            printIndent();
            System.out.println("Physical Volume: " + physical.getName());

            indent ++;
            printIndent();
            System.out.println("Sensitive? " + physical.isSensitive());
            printIndent();
            System.out.println("Local Translation: " + physical.getTranslation());
            printIndent();
            System.out.println("Local Rotation: [" + physical.getRotation().getComponent(0,0) +
                    "  " + physical.getRotation().getComponent(0, 1) +
                    "  " + physical.getRotation().getComponent(0, 2));
            printIndent();
            System.out.println("                 " + physical.getRotation().getComponent(1,0) +
                    "  " + physical.getRotation().getComponent(1, 1) +
                    "  " + physical.getRotation().getComponent(1, 2));
            printIndent();
            System.out.println("                 " + physical.getRotation().getComponent(2,0) +
                    "  " + physical.getRotation().getComponent(2, 1) +
                    "  " + physical.getRotation().getComponent(2, 2) + "]");
        }
            getGlobalTransform(physical);
            indent --;
            return physical.getLogicalVolume();
    }

    private void getGlobalTransform(IPhysicalVolume physical) {
        ITransform3D transform = (ITransform3D) physicalVolumes.get(0);
        for (int i = 1; i < physicalVolumes.size(); i++){
            ITransform3D lastTransform = (ITransform3D) physicalVolumes.get(i);
            transform.multiplyBy(lastTransform);
        }
        printIndent();
        System.out.println("Global Transform: " + transform.getTranslation());
        printIndent();
        System.out.println("                   [" + transform.getRotation().getComponent(0,0) +
                    "  " + transform.getRotation().getComponent(0, 1) +
                    "  " + transform.getRotation().getComponent(0, 2));
            printIndent();
            System.out.println("                    " + transform.getRotation().getComponent(1,0) +
                    "  " + transform.getRotation().getComponent(1, 1) +
                    "  " + transform.getRotation().getComponent(1, 2));
            printIndent();
            System.out.println("                    " + transform.getRotation().getComponent(2,0) +
                    "  " + transform.getRotation().getComponent(2, 1) +
                    "  " + transform.getRotation().getComponent(2, 2) + "]");
    }

    public void printIndent () {
        for (int k = indent; k > 0; k --) {
             System.out.print("     ");
        }
    }
}
