/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.recon.tracking.kalman;

import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.ISolid;
import org.lcsim.event.Track;

/**
 *
 * @author ecfine
 */
public class BoxHelper implements ShapeHelper{
    public void printShape(ISolid solid) {
        checkBox(solid);
        System.out.println("Shape: Box");
    }
    
    public void printLocalCoords(ISolid solid) {
        
    }
    
    public void printGlobalCoords(ISolid solid) {
        
    }
    
    private void checkBox(ISolid solid){
        if (solid instanceof Box) {
        } else {
            System.out.println("Error! This is not a box! ");
            return;
        }
    }

    public void findIntersection(ISolid solid, Track track) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public KalmanSurface getKalmanSurf(ISolid solid) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
