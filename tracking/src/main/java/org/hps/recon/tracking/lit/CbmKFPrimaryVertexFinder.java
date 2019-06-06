package org.hps.recon.tracking.lit;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmKFPrimaryVertexFinder {

    List<CbmLitTrack> Tracks = new ArrayList<CbmLitTrack>();
    CbmLitTrackExtrapolator _extrap;

    public CbmKFPrimaryVertexFinder(CbmLitTrackExtrapolator extrap) {
        _extrap = extrap;
    }

    public void Clear() {
        Tracks.clear();
    }

    public void AddTrack(CbmLitTrack Track) {
        Tracks.add(Track);
    }

    public void Fit(CbmVertex vtx) {
        //* Constants 

        final double CutChi2 = 3.5 * 3.5;
        final int MaxIter = 3;

        //* Vertex state vector and the covariance matrix
        double[] r = new double[3];
        double[] C = new double[6];

        //* Initialize the vertex 
        r[0] = r[1] = r[2] = 0.;
        C[0] = C[2] = 5.;
        C[5] = 0.25;

        //* Iteratively fit vertex - number of iterations fixed at MaxIter
        for (int iteration = 0; iteration < MaxIter; ++iteration) {

            //* Store vertex from previous iteration
            double[] r0 = new double[3];
            double[] C0 = new double[6];

            for (int i = 0; i < 3; i++) {
                r0[i] = r[i];
            }
            for (int i = 0; i < 6; i++) {
                C0[i] = C[i];
            }

            //* Initialize the vertex covariance, Chi^2 & NDF
            double large = 100.;
            C[0] = large;
            C[1] = 0.;
            C[2] = large;
            C[3] = 0.;
            C[4] = 0.;
            C[5] = large;

            vtx.setfNDF(-3);
            vtx.setChi2(0.);
            vtx.setNTracks(0);

            // loop over the list of tracks
            for (CbmLitTrack T : Tracks) {
                // extrapolate track to z=r0[2]
                //Bool_t err = T.Extrapolate( r0[2] );
                //if( err ) continue;

                // propagate to z = r0[2]
                CbmLitTrackParam par = T.GetParamFirst();
                _extrap.Extrapolate(par, r0[2], null);

                //fetch the track parameters and cov matrix as arrays...
                double[] m = par.GetStateVector();
                double[] V = par.GetCovMatrix();

                double a = 0, b = 0;
                {
                    double[] zeta = {r0[0] - m[0], r0[1] - m[1]};

                    //* Check track Chi^2 deviation from the r0 vertex estimate
                    double[] S = {(C0[2] + V[2]), -(C0[1] + V[1]), (C0[0] + V[0])};
                    double s = S[2] * S[0] - S[1] * S[1];
                    double chi2 = zeta[0] * zeta[0] * S[0] + 2 * zeta[0] * zeta[1] * S[1]
                            + zeta[1] * zeta[1] * S[2];
                    if (chi2 > s * CutChi2) {
                        continue;
                    }

                    //* Fit of vertex track slopes (a,b) to r0 vertex
                    s = V[0] * V[2] - V[1] * V[1];
                    if (s < 1.E-20) {
                        continue;
                    }
                    s = 1. / s;
                    a = m[2] + s * ((V[3] * V[2] - V[4] * V[1]) * zeta[0]
                            + (-V[3] * V[1] + V[4] * V[0]) * zeta[1]);
                    b = m[3] + s * ((V[6] * V[2] - V[7] * V[1]) * zeta[0]
                            + (-V[6] * V[1] + V[7] * V[0]) * zeta[1]);
                }

                //** Update the vertex (r,C) with the track estimate (m,V) :
                //* Linearized measurement matrix H = { { 1, 0, -a}, { 0, 1, -b} };      
                //* Residual (measured - estimated) 
                double[] zeta = {m[0] - (r[0] - a * (r[2] - r0[2])),
                    m[1] - (r[1] - b * (r[2] - r0[2]))};

                //* CHt = CH'        
                double[][] CHt = {{C[0] - a * C[3], C[1] - b * C[3]}, {C[1] - a * C[4], C[2] - b * C[4]}, {C[3] - a * C[5], C[4] - b * C[5]}};

                //* S = (H*C*H' + V )^{-1} 
                double[] S = {V[0] + CHt[0][0] - a * CHt[2][0],
                    V[1] + CHt[1][0] - b * CHt[2][0],
                    V[2] + CHt[1][1] - b * CHt[2][1]};

                //* Invert S
                {
                    double w = S[0] * S[2] - S[1] * S[1];
                    if (w < 1.E-20) {
                        continue;
                    }
                    w = 1. / w;
                    double S0 = S[0];
                    S[0] = w * S[2];
                    S[1] = -w * S[1];
                    S[2] = w * S0;
                }

                //* Calculate Chi^2
                vtx.incrementChi2(zeta[0] * zeta[0] * S[0] + 2 * zeta[0] * zeta[1] * S[1]
                        + zeta[1] * zeta[1] * S[2] + T.GetChi2());
                vtx.incrementfNDF(2 + T.GetNDF());
                vtx.incrementNTracks(1);

                //* Kalman gain K = CH'*S
                double[][] K = new double[3][2];

                for (int i = 0; i < 3; ++i) {
                    K[i][0] = CHt[i][0] * S[0] + CHt[i][1] * S[1];
                    K[i][1] = CHt[i][0] * S[1] + CHt[i][1] * S[2];
                }

                //* New estimation of the vertex position r += K*zeta
                for (int i = 0; i < 3; ++i) {
                    r[i] += K[i][0] * zeta[0] + K[i][1] * zeta[1];
                }

                //* New covariance matrix C -= K*(CH')'
                C[0] -= K[0][0] * CHt[0][0] + K[0][1] * CHt[0][1];
                C[1] -= K[1][0] * CHt[0][0] + K[1][1] * CHt[0][1];
                C[2] -= K[1][0] * CHt[1][0] + K[1][1] * CHt[1][1];
                C[3] -= K[2][0] * CHt[0][0] + K[2][1] * CHt[0][1];
                C[4] -= K[2][0] * CHt[1][0] + K[2][1] * CHt[1][1];
                C[5] -= K[2][0] * CHt[2][0] + K[2][1] * CHt[2][1];

            }//* itr        

        }//* finished iterations

        vtx.setX(r[0]);
        vtx.setY(r[1]);
        vtx.setZ(r[2]);

        vtx.setCovMatrix(C);
    }
}
