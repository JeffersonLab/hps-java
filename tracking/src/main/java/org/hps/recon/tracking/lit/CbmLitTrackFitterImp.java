package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitTrackFitterImp implements CbmLitTrackFitter {

    CbmLitTrackPropagator fPropagator; // Track propagation tool
    CbmLitTrackUpdate fUpdate; // Track update tool

    public CbmLitTrackFitterImp(
            CbmLitTrackPropagator propagator,
            CbmLitTrackUpdate update) {
        fPropagator = propagator;
        fUpdate = update;
    }

    public LitStatus Fit(CbmLitTrack track, boolean downstream) {
        track.SortHits(downstream);
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

        for (int iHit = 0; iHit < nofHits; iHit++) {
            // pseudo return argument
            double[] length = new double[1];
            CbmLitHit hit = track.GetHit(iHit);
            if (hit instanceof CbmLitDetPlaneStripHit) {
                if (fPropagator.Propagate(par, ((CbmLitDetPlaneStripHit) hit).GetPlane(), track.GetPDG(), F, length) == LitStatus.kLITERROR) {
                    track.SetQuality(LitTrackQa.kLITBAD);
                    return LitStatus.kLITERROR;
                }
            } else {
                double Ze = hit.GetZ();

                if (fPropagator.Propagate(par, Ze, track.GetPDG(), F, length) == LitStatus.kLITERROR) {
                    track.SetQuality(LitTrackQa.kLITBAD);
                    return LitStatus.kLITERROR;
                }
            }
            totalLength += length[0];
//            System.out.println(Arrays.toString(nodes));
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
