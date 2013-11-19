/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.recon.tracking;

import org.lcsim.event.RawTrackerHit;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants.ChannelConstants;

/**
 *
 * @author mgraham
 */
public interface HPSShaperFitAlgorithm {
    
    public HPSShapeFitParameters fitShape(RawTrackerHit rth, ChannelConstants constants);

}
