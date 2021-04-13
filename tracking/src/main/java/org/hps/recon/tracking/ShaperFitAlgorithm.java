package org.hps.recon.tracking;

import java.util.Collection;
import org.lcsim.event.RawTrackerHit;

// TODO: Add class documentation.
public interface ShaperFitAlgorithm {

    //===> public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, ChannelConstants constants);
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rawHit, PulseShape shape);

    public void setDebug(boolean debug);
}
