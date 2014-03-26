/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.recon.tracking.kalman;

import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Trd;
import org.lcsim.detector.solids.Tube;
import org.lcsim.event.Track;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.recon.tracking.trffit.HTrack;

/**
 * This class takes a solid and dispatches it to its Helper class,
 * for any of the possible Helper methods.
 *
 * @author ecfine
 */
public class ShapeDispatcher implements ShapeHelper{

    public void printShape(ISolid solid) {
         if (solid instanceof Trd) {
            TrdHelper trapHelper = new TrdHelper();
            trapHelper.printShape(solid);
        } else if (solid instanceof Tube) {
//            TubeHelper tubeHelper = new TubeHelper();
//            tubeHelper.printShape(solid);
        } else if (solid instanceof Box) {
            BoxHelper boxHelper = new BoxHelper();
            boxHelper.printShape(solid);
        } else {
             System.out.println("Error! I don't recognize this shape!");
        }
    }

    public void printLocalCoords(ISolid solid) {
         if (solid instanceof Trd) {
            TrdHelper trapHelper = new TrdHelper();
            trapHelper.printLocalCoords(solid);
        } else if (solid instanceof Tube) {
//            TubeHelper tubeHelper = new TubeHelper();
//            tubeHelper.printLocalCoords(solid);
        } else if (solid instanceof Box) {
            BoxHelper boxHelper = new BoxHelper();
            boxHelper.printLocalCoords(solid);
        } else {
             System.out.println("Error! I don't recognize this shape!");
        }
    }

    public void printGlobalCoords(ISolid solid) {
        if (solid instanceof Trd) {
            TrdHelper trapHelper = new TrdHelper();
            trapHelper.printGlobalCoords(solid);
        } else if (solid instanceof Tube) {
//            TubeHelper tubeHelper = new TubeHelper();
//            tubeHelper.printGlobalCoords(solid);
        } else if (solid instanceof Box) {
            BoxHelper boxHelper = new BoxHelper();
            boxHelper.printGlobalCoords(solid);
        } else {
             System.out.println("Error! I don't recognize this shape!");
        }
    }

    public HTrack addIntercept(ISolid solid, HTrack ht) {
        HTrack track = null;
        if (solid instanceof Trd) {
            TrdHelper trapHelper = new TrdHelper();
//            track = trapHelper.addIntercept(solid, ht);
        } else {
            System.out.println("This shape is not supported yet");

        }
        return track;
    }

    public HelicalTrackFit trackToHelix(Track track) {
        SeedTrack stEle = (SeedTrack) track;
        SeedCandidate seedEle = stEle.getSeedCandidate();
        HelicalTrackFit ht = seedEle.getHelix();
        return ht;
    }

    public KalmanSurface getKalmanSurf(ISolid solid) {
        KalmanSurface surf = null;
        if (solid instanceof Trd) {
            TrdHelper trapHelper = new TrdHelper();
            surf = trapHelper.getKalmanSurf(solid);
        } else {
            System.out.println("This solid is not supported yet");
        }
        return surf;
    }

    public void findIntersection(ISolid solid, Track track) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean pointIsOnSolid(ISolid solid, Point3D hitPoint) {
        boolean pointOnSolid = false;
        if (solid instanceof Trd){
            TrdHelper trapHelper = new TrdHelper();
            pointOnSolid = trapHelper.pointIsOnSolid(solid, hitPoint);
        } else {
            System.out.print("This solid is not supported yet");
        }
        return pointOnSolid;

   }
}
