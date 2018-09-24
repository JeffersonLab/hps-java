package org.hps.analysis.alignment.straighttrack.vertex;

import java.util.List;

/**
 *
 * @author ngraf
 */
public class StraightLineVertexFitter {

    public static void fitPrimaryVertex(List<Track> tracks, double[] v0, Vertex vtx) {

        double CutChi2 = 3.5 * 3.5;
        int MaxIter = 10;
        double[] r = new double[3];
        double[] C = new double[6];
        //initialize the vertex
        System.arraycopy(v0, 0, r, 0, 3);

        // iterative fit of the vertex
        for (int iteration = 0; iteration < MaxIter; ++iteration) {
//            System.out.println("iteration "+iteration+" vtx: "+Arrays.toString(r));
            // store the vertex from the previous iteration
            double[] r0 = new double[3];
            double[] C0 = new double[6];
            System.arraycopy(r, 0, r0, 0, 3);
            System.arraycopy(C, 0, C0, 0, 6);
            // initialize the vertex covariance, Chi^2 and ndf
            C[0] = 100.;
            C[1] = 0.;
            C[2] = 100.;
            C[3] = 0.;
            C[4] = 0.;
            C[5] = 100.;

            vtx.fNDF = -3;
            vtx.fChi2 = 0.;
            vtx.fNTracks = 0;

            for (Track t : tracks) {
                Track T = new Track(t);
                extrapolate(T, r0[2]);
                double[] m = T.params();
                double[] V = T.cov();

                double a = 0.;
                double b = 0.;
                {
                    double[] zeta = {r0[0] - m[0], r0[1] - m[1]};
                    //* Check the track Chi^2 deviation from the r0 vertex estimate 
                    double[] S = {(C0[2] + V[2]), -(C0[1] + V[1]), (C0[0] + V[0])};
                    double s = S[2] * S[0] - S[1] * S[1];
                    double chi2 = zeta[0] * zeta[0] * S[0] + 2 * zeta[0] * zeta[1] * S[1] + zeta[1] * zeta[1] * S[2];
//                    System.out.println("chi2 "+chi2+" s "+s+" CutChi2 "+CutChi2+" s*CutChi2 "+(s * CutChi2));
//TODO fix this chisq cut
//                    if (chi2 > s * CutChi2) {
//                        continue;
//                    }
//                    System.out.println("pass");

                    //* Fit of the vertex track slopes (a,b) to the r0 vertex estimate 
                    s = V[0] * V[2] - V[1] * V[1];
                    if (s < 1.E-20) {
                        continue;
                    }
                    s = 1. / s;
                    a = m[2] + s * ((V[3] * V[2] - V[4] * V[1]) * zeta[0] + (-V[3] * V[1] + V[4] * V[0]) * zeta[1]);
                    b = m[3] + s * ((V[6] * V[2] - V[7] * V[1]) * zeta[0] + (-V[6] * V[1] + V[7] * V[0]) * zeta[1]);
                }
                //** Update the vertex (r,C) with the track estimate (m,V) : 
                //* Linearized measurement matrix H = { { 1, 0, -a}, { 0, 1, -b} }; 
                //* Residual zeta (measured - estimated) 
                double[] zeta = {m[0] - (r[0] - a * (r[2] - r0[2])), m[1] - (r[1] - b * (r[2] - r0[2]))};
                //* CHt = CH’ 
                double[][] CHt = {
                    {C[0] - a * C[3], C[1] - b * C[3]},
                    {C[1] - a * C[4], C[2] - b * C[4]},
                    {C[3] - a * C[5], C[4] - b * C[5]}};
                //* S = (H*C*H’ + V )^{-1} 
                double[] S = {
                    V[0] + CHt[0][0] - a * CHt[2][0],
                    V[1] + CHt[1][0] - b * CHt[2][0],
                    V[2] + CHt[1][1] - b * CHt[2][1]};
                //* Invert S 
                {
                    double s = S[0] * S[2] - S[1] * S[1];
                    if (s < 1.E-20) {
                        continue;
                    }
                    s = 1. / s;
                    double S0 = S[0];
                    S[0] = s * S[2];
                    S[1] = -s * S[1];
                    S[2] = s * S0;
                }

                //* Calculate Chi^2 
                vtx.fChi2 += zeta[0] * zeta[0] * S[0] + 2 * zeta[0] * zeta[1] * S[1] + zeta[1] * zeta[1] * S[2];
                vtx.fNDF += 2;
                vtx.fNTracks++;

                //* Kalman gain K = CH’*S 
                double[][] K = new double[3][2];
                for (int i = 0; i < 3; ++i) {
                    K[i][0] = CHt[i][0] * S[0] + CHt[i][1] * S[1];
                    K[i][1] = CHt[i][0] * S[1] + CHt[i][1] * S[2];
                }
                //* New estimation of the vertex position r += K*zeta
                for (int i = 0; i < 3; ++i) {
                    r[i] += K[i][0] * zeta[0] + K[i][1] * zeta[1];
                }

                //* New covariance matrix C -= K*(CH’)’ 
                C[0] -= K[0][0] * CHt[0][0] + K[0][1] * CHt[0][1];
                C[1] -= K[1][0] * CHt[0][0] + K[1][1] * CHt[0][1];
                C[2] -= K[1][0] * CHt[1][0] + K[1][1] * CHt[1][1];
                C[3] -= K[2][0] * CHt[0][0] + K[2][1] * CHt[0][1];
                C[4] -= K[2][0] * CHt[1][0] + K[2][1] * CHt[1][1];
                C[5] -= K[2][0] * CHt[2][0] + K[2][1] * CHt[2][1];

            } //end of loop on tracks
        } // end of iterations
        //* Copy the state vector to the output 
        vtx.fX = r[0];
        vtx.fY = r[1];
        vtx.fZ = r[2];
        vtx.setCov(C);
    }

    static void extrapolate(Track t, double z) {
        double[] t_out = new double[6];
        double[] c_out = new double[15];
        extrapolateLine(t.params(), t.cov(), t_out, c_out, z);
        t.setParams(t_out);
        t.setCov(c_out);
    }

    /**
     *
     * @param T_in input track parameters (x,y,tx,ty,Q/p,z)
     * @param C_in input covariance matrix (packed lower matrix)
     * @param T_out output track parameters
     * @param C_out output covariance matrix
     * @param z_out extrapolate to this z position
     */
    static void extrapolateLine(double[] T_in, double[] C_in, double[] T_out, double[] C_out, double z_out) {
        double dz = z_out - T_in[5];

        T_out[0] = T_in[0] + dz * T_in[2];
        T_out[1] = T_in[1] + dz * T_in[3];
        T_out[2] = T_in[2];
        T_out[3] = T_in[3];
        T_out[4] = T_in[4];
        T_out[5] = z_out;

        double dzC_in8 = dz * C_in[8];

        C_out[4] = C_in[4] + dzC_in8;
        C_out[1] = C_in[1] + dz * (C_out[4] + C_in[6]);

        double C_in3 = C_in[3];

        C_out[3] = C_in3 + dz * C_in[5];
        C_out[0] = C_in[0] + dz * (C_out[3] + C_in3);

        double C_in7 = C_in[7];

        C_out[7] = C_in7 + dz * C_in[9];
        C_out[2] = C_in[2] + dz * (C_out[7] + C_in7);
        C_out[5] = C_in[5];
        C_out[6] = C_in[6] + dzC_in8;
        C_out[8] = C_in[8];
        C_out[9] = C_in[9];

        C_out[10] = C_in[10];
        C_out[11] = C_in[11];
        C_out[12] = C_in[12];
        C_out[13] = C_in[13];
        C_out[14] = C_in[14];

    }
}
