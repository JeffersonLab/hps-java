package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitTrackFitterIter implements CbmLitTrackFitter {

    /*
     * Kalman filter track fitting tool
     */
    CbmLitTrackFitter fFitter;
    /*
     * Kalman smoother tool
     */
    CbmLitTrackFitter fSmoother;
    /*
     * Number of iterations
     */
    int fNofIterations;
    /*
     * Cut on chi square for single hit
     */
    double fChiSqCut;
    /*
     * Minimum number of hits in track
     */
    int fMinNofHits;

    public CbmLitTrackFitterIter(CbmLitTrackFitter fitter, CbmLitTrackFitter smoother) {
        fFitter = fitter;
        fSmoother = smoother;
        fChiSqCut = 15.;
        fNofIterations = 2;
        fMinNofHits = 3;
    }

    public CbmLitTrackFitterIter(CbmLitTrackFitter fitter, CbmLitTrackFitter smoother, int maxIterations, int minHits, double chiSqCut) {
        fFitter = fitter;
        fSmoother = smoother;
        fChiSqCut = chiSqCut;
        fNofIterations = maxIterations;
        fMinNofHits = minHits;
    }

    public LitStatus Fit(CbmLitTrack track) {
        return Fit(track, true);
    }

    public LitStatus Fit(CbmLitTrack track, boolean downstream) {
        for (int iter = 0; iter < fNofIterations; iter++) {
            boolean isRefit = false;
//            System.out.println("fitting downstream...");

            if (fFitter.Fit(track, downstream) == LitStatus.kLITERROR) {
                return LitStatus.kLITERROR;
            }
            System.out.println("track :" + track);
            CbmLitTrackParam first = track.GetParamFirst();
            System.out.println("first: " + first);
            CbmLitTrackParam last = track.GetParamLast();
            System.out.println("last: " + last);

//            System.out.println("smoothing...");
            if (fSmoother.Fit(track, downstream) == LitStatus.kLITERROR) {
                return LitStatus.kLITERROR;
            }

            if (iter < fNofIterations - 1) {
                for (int i = 0; i < track.GetNofHits(); i++) {
                    double chiSq = track.GetFitNode(i).GetChiSqSmoothed();
                    if (chiSq > fChiSqCut) {
                        track.RemoveHit(i);
                        isRefit = true;
                    }
                }
            }

            if (track.GetNofHits() < fMinNofHits) {
                return LitStatus.kLITERROR;
            }
            if (!isRefit) {
                return LitStatus.kLITSUCCESS;
            }
        }
        return LitStatus.kLITSUCCESS;
    }
}
