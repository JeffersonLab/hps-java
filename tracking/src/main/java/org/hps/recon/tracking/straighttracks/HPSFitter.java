package org.hps.recon.tracking.straighttracks;

import java.util.List;
import java.util.Map;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.MultipleScatter;

/**
 *
 */
public interface HPSFitter {

//    public void HPSFitter();

    public enum FitStatus {
    
        /**
         * Successful Fit.
         */
        Success,
        /**
         * Inconsistent seed hits
         */
        InconsistentSeed,
        /**
         * s-z line fit failed.
         */
        SZLineFitFailed,
        /**
         * ZSegmentFit failed.
         */
        XYLineFitFailed
    };

    public FitStatus fit(List<HelicalTrackHit> hits);

    public FitStatus fit(List<HelicalTrackHit> hits, Map<HelicalTrackHit, MultipleScatter> msmap, HelicalTrackFit oldfit);

    public HelicalTrackFit getFit();

}
