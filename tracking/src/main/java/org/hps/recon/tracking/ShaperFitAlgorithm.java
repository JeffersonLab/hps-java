package org.hps.recon.tracking;

import java.util.Collection;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author Matt Graham
 */
// TODO: Add class documentation.
public interface ShaperFitAlgorithm {

    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants);

    public void setDebug(boolean debug);
}
