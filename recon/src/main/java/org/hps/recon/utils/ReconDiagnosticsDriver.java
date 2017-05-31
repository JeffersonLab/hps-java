package org.hps.recon.utils;

import java.util.List;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class ReconDiagnosticsDriver extends Driver {

    private long _startTime;

    private AIDA aida = AIDA.defaultInstance();

    @Override
    protected void detectorChanged(Detector detector) {
        _startTime = System.nanoTime();
    }

    @Override
    protected void process(EventHeader event) {
        long deltaTime = System.nanoTime() - _startTime;
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        double svtsize = rawHits.size();
        aida.cloud2D("elapsed time vs SVT size").fill(svtsize, deltaTime);
        _startTime = System.nanoTime();
        System.out.println(event.getRunNumber()+" "+event.getEventNumber()+" "+svtsize+" "+deltaTime);
    }

}
