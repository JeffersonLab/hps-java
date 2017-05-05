package org.hps.recon.tracking;

import java.util.Collection;
//import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.event.RawTrackerHit;

public interface ShaperFitAlgorithm {

    //===> public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants);
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rawHit, PulseShape shape);

    public void setDebug(boolean debug);
}
