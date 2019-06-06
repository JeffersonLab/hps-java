package org.hps.recon.tracking.lit;

import java.util.Set;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsLitTrackFitter {

    HpsTrackPropagator fPropagator; // Track propagation tool
    HpsLitKalmanFilter fUpdate; // Track update tool

    public HpsLitTrackFitter(HpsTrackPropagator propagator, HpsLitKalmanFilter filter) {
        fPropagator = propagator;
        fUpdate = filter;
    }

    public LitStatus Fit(HpsLitTrack track, boolean downstream) {
        // get the hits
        Set<HpsStripHit> hits = track.GetHits();
        if (!downstream) {
            hits = track.GetReverseHits();
        }

//        track.SortHits(downstream);
        track.SetChi2(0.0);
        int nofHits = track.GetNofHits();
        CbmLitFitNode[] nodes = new CbmLitFitNode[nofHits];
        for (int i = 0; i < nofHits; ++i) {
            nodes[i] = new CbmLitFitNode();
        }
        CbmLitTrackParam par;
        double[] F = new double[25];

        if (downstream) {
            CbmLitTrackParam p = track.GetParamFirst();
            track.SetParamLast(track.GetParamFirst());
            par = track.GetParamLast();
        } else {
            track.SetParamFirst(track.GetParamLast());
            par = track.GetParamFirst();
        }

        double totalLength = 0.;

        int iHit = 0;
        for (HpsStripHit hit : hits) {
            //double Ze = hit.GetZ();
            DetectorPlane p = hit.plane();
            double[] length = new double[1];
            if (fPropagator.Propagate(par, p, track.GetPDG(), F, length) == LitStatus.kLITERROR) {
                track.SetQuality(LitTrackQa.kLITBAD);
                return LitStatus.kLITERROR;
            }
            totalLength += length[0];
            nodes[iHit].SetPredictedParam(par);
            nodes[iHit].SetF(F);
            double[] chi2Hit = new double[1];
            if (fUpdate.Update(par, hit, chi2Hit) == LitStatus.kLITERROR) {
                track.SetQuality(LitTrackQa.kLITBAD);
                return LitStatus.kLITERROR;
            }
            nodes[iHit].SetUpdatedParam(par);
            nodes[iHit].SetChiSqFiltered(chi2Hit[0]);
            track.SetChi2(track.GetChi2() + chi2Hit[0]);
            iHit++;
        }
        if (downstream) {
            track.SetParamLast(par);
        } else {
            track.SetParamFirst(par);
        }

        track.SetFitNodes(nodes);
        track.SetNDF(CbmLitMath.NDF(track));
        track.SetLength(totalLength);

        return LitStatus.kLITSUCCESS;
    }
}
