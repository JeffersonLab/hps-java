/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.recon.tracking.kalman;

import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;


/**
 *
 * @author ecfine
 */
public class PrintDetectorElementsDriver extends Driver{
    final boolean printing = true; // Determines whether results are printed

    
    public void detectorChanged(Detector detector) {
        PrintDetectorElements loop = new PrintDetectorElements();
        loop.run(detector, printing);
    }
}
