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
 * Do a simple 1d linear fit using the 
 * 
 * @author mgraham <mgraham@slac.stanford.edu>
 */
public class StraightTrackAxialFitter implements HPSFitter{

    SlopeInterceptLineFitter _lfitter = new SlopeInterceptLineFitter();
    HelicalTrackFit _fit;

//    /**
//     * Status of the HelicalTrackFit.
//     */
//    public enum FitStatus {
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
        boolean success;
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
        double[] dz = new double[nhit];
        Map<HelicalTrackHit, Double> smap = new HashMap<>();
     
        //  Store the coordinates and errors for the SZ line fit
        for (int i = 0; i < nhit; i++) {
            HelicalTrackHit hit = hits.get(i);
            z[i] = hit.z();
            dz[i] = HitUtils.zres(hit, msmap, oldfit); //MG  this works even in msmap is null
            s[i] = hit.x();  //take as the track length, the distance in x
            smap.put(hit, s[i]);
        }
        //  Call the line fitter and check for success
        success = _lfitter.fit(s, z, dz, nhit);
        if (!success){
            System.out.println("SZLineFitFailed");
            return FitStatus.SZLineFitFailed;
        }
        SlopeInterceptLineFit szFit = _lfitter.getFit();
        par[3] = szFit.intercept();
        par[4] = szFit.slope();
        cov.setElement(3, 3, Math.pow(szFit.interceptUncertainty(), 2));
        cov.setElement(4, 4, Math.pow(szFit.slopeUncertainty(), 2));
        cov.setElement(3, 4, Math.pow(szFit.covariance(), 2));
        chisq[1] = szFit.chisquared();
        ndof[1] = szFit.ndf();        
        //set the other parameters to 0;
        chisq[0] = 0;
        ndof[0] = 0;       
        par[0]=0;
        par[1]=0;
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
