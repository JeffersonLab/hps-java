/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.lcsim.hps.recon.tracking.kalman;

import org.lcsim.detector.solids.ISolid;
import org.lcsim.event.Track;

/**
 * Interface for dealing with lcsim solids and converting them to trf surfaces.
 * Most of the classes which extend this are basically empty, with the exception
 * of the TrdHelper, which has some geometry things, and ShapeDispatcher, which
 * dispatches solids to the correct helper.
 * 
 * @author ecfine
 */
public interface ShapeHelper {

    /* Prints the shape of the solid */
    public void printShape(ISolid solid);

    /* Prints the local coordinates of the solid */
    public void printLocalCoords(ISolid solid);

    /* Prints the global coordinates of the solid */
    public void printGlobalCoords(ISolid solid);

    /* Finds the intersection of a solid and a track */
    public void findIntersection(ISolid solid, Track track);

    public KalmanSurface getKalmanSurf(ISolid solid);

}
