package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitKalmanSmoother implements CbmLitTrackFitter {

    public LitStatus Fit(CbmLitTrack track, boolean downstream) {
        int n = track.GetNofHits();

        CbmLitFitNode[] nodes = track.GetFitNodes();
        nodes[n - 1].SetSmoothedParam(nodes[n - 1].GetUpdatedParam());

        // start with the before the last detector plane
        for (int i = n - 1; i > 0; i--) {
//            System.out.println("node " + (i - 1) + " " + nodes[i - 1] + " " + i + " " + nodes[i]);
            Smooth(nodes[i - 1], nodes[i]);
        }

        // Calculate the chi2 of the track
        track.SetChi2(0.);
        for (int i = 0; i < n; i++) {
            double chi2Hit = CbmLitMath.ChiSq(nodes[i].GetSmoothedParam(), track.GetHit(i));
            nodes[i].SetChiSqSmoothed(chi2Hit);
            track.SetChi2(track.GetChi2() + chi2Hit);
        }

        track.SetParamFirst(nodes[0].GetSmoothedParam());
        track.SetFitNodes(nodes);
        track.SetNDF(CbmLitMath.NDF(track));

        return LitStatus.kLITSUCCESS;
    }

    /**
     * \brief Smooth one fit node. \param[out] thisNode Current fit node to be
     * smoothed. \param[in] prevNode Previous fit node.
     */
    void Smooth(
            CbmLitFitNode thisNode,
            CbmLitFitNode prevNode) {
// We are going in the upstream direction
// this Node (k) , prevNode (k+1)

        double[] invPrevPredC = prevNode.GetPredictedParam().GetCovMatrix();
//        System.out.println("CbmLitKalmanSmoother: invPrevPredC");
//        for(int i=0; i<invPrevPredC.length; ++i)
//        {
//            System.out.println(i + ":" + invPrevPredC[i]);
//        }
        CbmLitMatrixMath.InvSym15(invPrevPredC);
//        System.out.println("CbmLitKalmanSmoother: invPrevPredC inverted");
//        for(int i=0; i<invPrevPredC.length; ++i)
//        {
//            System.out.println(i + ":" + invPrevPredC[i]);
//        }

        double[] Ft = prevNode.GetF();
        CbmLitMatrixMath.Transpose25(Ft);

        double[] thisUpdC = thisNode.GetUpdatedParam().GetCovMatrix();

        double[] A = new double[25];
        double[] temp1 = new double[25];
        CbmLitMatrixMath.Mult15On25(thisUpdC, Ft, temp1);
        CbmLitMatrixMath.Mult25On15(temp1, invPrevPredC, A);

        double[] thisUpdX = thisNode.GetUpdatedParam().GetStateVector();
        double[] prevSmoothedX = prevNode.GetSmoothedParam().GetStateVector();
        double[] prevPredX = prevNode.GetPredictedParam().GetStateVector();

        double[] temp2 = new double[5];
        double[] temp3 = new double[5];
        CbmLitMatrixMath.Subtract(prevSmoothedX, prevPredX, temp2);
        CbmLitMatrixMath.Mult25On5(A, temp2, temp3);
        double[] thisSmoothedX = new double[5];
        CbmLitMatrixMath.Add(thisUpdX, temp3, thisSmoothedX);

        double[] prevSmoothedC = prevNode.GetSmoothedParam().GetCovMatrix();
        double[] prevPredC = prevNode.GetPredictedParam().GetCovMatrix();
        double[] temp4 = new double[15];
        CbmLitMatrixMath.Subtract(prevSmoothedC, prevPredC, temp4);

        double[] temp5 = new double[15];
        CbmLitMatrixMath.Similarity(A, temp4, temp5);
        double[] thisSmoothedC = new double[15];
        CbmLitMatrixMath.Add(thisUpdC, temp5, thisSmoothedC);

        CbmLitTrackParam par = new CbmLitTrackParam();

        par.SetStateVector(thisSmoothedX);
        par.SetCovMatrix(thisSmoothedC);
        par.SetZ(thisNode.GetUpdatedParam().GetZ());

        thisNode.SetSmoothedParam(par);
    }
}
