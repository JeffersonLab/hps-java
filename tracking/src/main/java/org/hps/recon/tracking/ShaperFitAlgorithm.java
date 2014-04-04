package org.hps.recon.tracking;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.event.RawTrackerHit;

/**
 * 
 * @author Matt Graham
 */
// TODO: Add class documentation.
public interface ShaperFitAlgorithm {

    public ShapeFitParameters fitShape(RawTrackerHit rth, ChannelConstants constants);

}
