/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.tracking.kalman;

import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Tube;
import org.lcsim.event.Track;

/**
 *
 * @author ecfine
 */
public class TubeHelper implements ShapeHelper {

    public void printShape(ISolid solid) {
        checkTube(solid);
        System.out.println("Shape: Tube");

    }

    public void printLocalCoords(ISolid solid) {
    }

    public void printGlobalCoords(ISolid solid) {
    }

    public void checkTube(ISolid solid) {
        if (solid instanceof Tube) {
        } else {
            System.out.print("Error! This isn't a tube!");
            return;
        }
    }

    public void findIntersection(ISolid solid, Track track) {
    }

    public KalmanSurface getKalmanSurf(ISolid solid) {
         KalmanSurface surf = null;
         return surf;
    }
}
