package org.hps.recon.tracking.straighttracks;

import hep.physics.matrix.SymmetricMatrix;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HitUtils;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.fit.line.SlopeInterceptLineFit;
import org.lcsim.fit.line.SlopeInterceptLineFitter;

/**
 *
 */
public class StraightTrack2DFitter implements HPSFitter {

    SlopeInterceptLineFitter _lfitter = new SlopeInterceptLineFitter();
    HelicalTrackFit _fit;

    /**
     * Status of the HelicalTrackFit.
     */
  //  public enum FitStatus {
//
//        /**
//         * Successful Fit.
//         */
//        Success,
//        /**
//         * Inconsistent seed hits
//         */
//        InconsistentSeed,
//        /**
//         * s-z line fit failed.
//         */
//        SZLineFitFailed,
//        /**
//         * ZSegmentFit failed.
//         */
//        XYLineFitFailed
//    };
    public void StraightTrack2DFitter() {

    }

    @Override
    public FitStatus fit(List<HelicalTrackHit> hits) {
        Map<HelicalTrackHit, MultipleScatter> msmap = new HashMap<HelicalTrackHit, MultipleScatter>();
        return fit(hits, msmap, null);
    }

    @Override
    public FitStatus fit(List<HelicalTrackHit> hits, Map<HelicalTrackHit, MultipleScatter> msmap, HelicalTrackFit oldfit) {
        //  Check that we have at least 3 hits
        boolean success = false;
        int nhit = hits.size();
        if (nhit < 3)
            return FitStatus.InconsistentSeed;

        //  Create the objects that will hold the fit output
        double[] chisq = new double[2];
        int[] ndof = new int[2];
        double[] par = new double[5];
        SymmetricMatrix cov = new SymmetricMatrix(5);

        //  Setup for the line fit
        double[] s = new double[nhit];
        double[] z = new double[nhit];
        double[] y = new double[nhit];
        double[] x = new double[nhit];
        double[] dz = new double[nhit];
        double[] dy = new double[nhit];
        Map<HelicalTrackHit, Double> smap = new HashMap<>();

        //  Store the coordinates and errors for the XY line fit
        for (int i = 0; i < nhit; i++) {
            HelicalTrackHit hit = hits.get(i);
            y[i] = hit.y();
            dy[i] = HitUtils.zres(hit, msmap, oldfit);
            double drphi_ms = 0;
            if (msmap.containsKey(hit))
                drphi_ms = msmap.get(hit).drphi();
            double dyHitSq = hit.getCorrectedCovMatrix().e(1, 1);
            dy[i] = Math.sqrt(dyHitSq + drphi_ms * drphi_ms);
            x[i] = hit.x();
        }
        //  Call the line fitter and check for success
        success = _lfitter.fit(x, y, dy, nhit);
        if (!success)
            return FitStatus.XYLineFitFailed;
        SlopeInterceptLineFit xyFit = _lfitter.getFit();
        par[0] = xyFit.intercept();
        par[1] = xyFit.slope();
        cov.setElement(0, 0, Math.pow(xyFit.interceptUncertainty(), 2));
        cov.setElement(1, 1, Math.pow(xyFit.slopeUncertainty(), 2));
        cov.setElement(0, 1, Math.pow(xyFit.covariance(), 2));
        chisq[0] = xyFit.chisquared();
        ndof[0] = xyFit.ndf();
        //  Store the coordinates and errors for the SZ line fit
        for (int i = 0; i < nhit; i++) {
            HelicalTrackHit hit = hits.get(i);
            z[i] = hit.z();
            dz[i] = HitUtils.zres(hit, msmap, oldfit); //MG  this works even in msmap is null
            s[i] = Math.sqrt(hit.x() * hit.x() + hit.y() * hit.y());
            smap.put(hit, s[i]);
        }
        //  Call the line fitter and check for success
        success = _lfitter.fit(s, z, dz, nhit);
        if (!success)
            return FitStatus.SZLineFitFailed;
        SlopeInterceptLineFit szFit = _lfitter.getFit();
        par[3] = szFit.intercept();
        par[4] = szFit.slope();
        cov.setElement(3, 3, Math.pow(szFit.interceptUncertainty(), 2));
        cov.setElement(4, 4, Math.pow(szFit.slopeUncertainty(), 2));
        cov.setElement(3, 4, Math.pow(szFit.covariance(), 2));
        chisq[1] = szFit.chisquared();
        ndof[1] = szFit.ndf();
        par[2] = 1e-50;
        //  Create the HelicalTrackFit for this helix
        _fit = new HelicalTrackFit(par, cov, chisq, ndof, smap, msmap);
        return FitStatus.Success;
    }

    @Override
    public HelicalTrackFit getFit() {
        return _fit;
    }

}
