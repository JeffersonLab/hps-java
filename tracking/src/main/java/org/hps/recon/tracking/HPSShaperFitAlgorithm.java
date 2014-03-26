/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author mgraham
 */
public interface HPSShaperFitAlgorithm {
    
    public HPSShapeFitParameters fitShape(RawTrackerHit rth, ChannelConstants constants);

}
