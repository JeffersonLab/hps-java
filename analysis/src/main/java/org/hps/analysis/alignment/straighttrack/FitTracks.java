package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class FitTracks {

    public static void main(String[] args) throws Exception {

        AIDA aida = AIDA.defaultInstance();
        System.out.println("Generating detector");
        List<DetectorPlane> planes = GENER_DET();
        for (DetectorPlane plane : planes) {
            System.out.println(plane);
        }
        // Testing the track fit code   
        double[] A0 = {0., 0., 0.}; //-2337.1810}; // initial guess for (x,y,z) of track
        double[] B0 = {0., 0., 1.}; // initial guess for the track direction
        List<String> lines = readEvents("D:/work/git/hps-users/hpsHits.txt");
        int NTIME = lines.size();
        System.out.println("found " + NTIME + " events in file");
        for (int i = 0; i < NTIME; ++i) { // loop over events
            double[] parin = new double[4];  // generated track parameters
            List<Hit> hits = nextEvent(lines.get(i), parin);
            // reconstruct using the ideal detector... 
            TrackFit fit = STR_LINFIT(planes, hits, A0, B0);
            double[] pars = fit.pars();
            double[] cov = fit.cov();
            if (debug()) {
                System.out.println("fit: " + fit);
                System.out.println("pars " + Arrays.toString(pars));
                System.out.println("parin " + Arrays.toString(parin));
                System.out.println("fit cov " + Arrays.toString(cov));
            }
//            System.out.println(Arrays.toString(cov));
            aida.cloud1D("fit chisq per ndf").fill(fit.chisq()/fit.ndf());
            aida.cloud1D("fit ndf ").fill(fit.ndf());
            double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
            aida.cloud1D("fit chisq prob ").fill(chisqProb);
            aida.cloud1D("fit niterations").fill(fit.niterations());
            aida.cloud1D("x meas-pred").fill(pars[0] - parin[0]);
            aida.cloud1D("y meas-pred").fill(pars[1] - parin[1]);
            aida.cloud1D("dxdz meas-pred").fill(pars[2] - parin[2]);
            aida.cloud1D("dydz meas-pred").fill(pars[3] - parin[3]);
            aida.cloud1D("x meas-pred pull").fill((pars[0] - parin[0]) / sqrt(cov[0]));
            aida.cloud1D("y meas-pred pull").fill((pars[1] - parin[1]) / sqrt(cov[2]));
            aida.cloud1D("dxdz meas-pred pull").fill((pars[2] - parin[2]) / sqrt(cov[5]));
            aida.cloud1D("dydz meas-pred pull").fill((pars[3] - parin[3]) / sqrt(cov[9]));

        }
        aida.saveAs("FitTracks.aida");
    }

    static TrackFit STR_LINFIT(List<DetectorPlane> planes, List<Hit> hits, double[] A0, double[] B0) {
        double[][] UVW = {{1., 0., 0.}, {0., 1., 0.}, {0., 0., 1.}};
        if (debug()) {
            System.out.println("\n\n\nIN STR_LINFIT\n\n\n");
        }
        double CHI = 1.E10;
        double CHI0 = 0.;
        int NDF = -4;
        int NIT = 0;
//        double wirez = -2337.1810;
        double[] PAR = new double[4];
        double[] COV = new double[10];
        List<ImpactPoint> rx = new ArrayList<ImpactPoint>();
        double[] A = new double[3];//{0., 0., 0.};
        double[] B = new double[3];//{0., 0., 1.};

        //start with initial estimate...
        System.arraycopy(A0, 0, A, 0, 3);
        System.arraycopy(B0, 0, B, 0, 3);
        // LOOP comes here...
        do {
            rx.clear();
            NIT++;

            A[0] = PAR[0];
            A[1] = PAR[1];
            B[0] = PAR[2];
            B[1] = PAR[3];

            A[2] = A0[2];
            B[2] = B0[2];

            Matrix b = new Matrix(B, 1);

            double[] BUVW = new double[3];

            CHI0 = CHI;
            CHI = 0.;
            double DCHI = 0;
            NDF = -4;

            double[] WMT = new double[10];
            double[] WVT = new double[4];
//
// See: http://cmsdoc.cern.ch/documents/99/note99_041.ps.Z
//
            int N = planes.size();
            for (int i = 0; i < N; ++i) {
                if (debug()) {
                    System.out.println("DETECTOR " + (i + 1));
                }
                DetectorPlane dp = planes.get(i);
                Matrix rot = dp.rot();
                if (debug()) {
                    System.out.println("rot " + Arrays.toString(rot.getRowPackedCopy()));
                }

                Hit h = hits.get(i);

                Matrix[] uvwg = new Matrix[3];
                for (int j = 0; j < 3; ++j) {
//                    if (debug()) {
//                        System.out.println("  CALLING VMATR");
//                    }
                    Matrix uvw = new Matrix(UVW[j], 3);
                    if (debug()) {
                        System.out.println("  UVW(" + (j + 1) + ") " + uvw.get(0, 0) + " " + uvw.get(1, 0) + " " + uvw.get(2, 0));
                    }
                    uvwg[j] = rot.transpose().times(uvw);
                    if (debug()) {
                        System.out.println("UVWG(" + (j + 1) + ") " + uvwg[j].get(0, 0) + " " + uvwg[j].get(1, 0) + " " + uvwg[j].get(2, 0) + " ");
                    }
//                    System.out.println("j "+j);
//                    System.out.println("b");
//                    b.print(6,4);
                    BUVW[j] = b.times(uvwg[j]).get(0, 0);
                }
                if (debug()) {
                    System.out.println("   BUVW " + BUVW[0] + " " + BUVW[1] + " " + BUVW[2]);
                }
                ImpactPoint ip = GET_IMPACT(A, B, rot, dp.r0(), uvwg[2], BUVW[2]);

                rx.add(ip);
                int NM = 0;
                double[] wt = h.wt();
                double[] eps = new double[2];
                if (wt[0] > 0.) {
                    NM = NM + 1; // precise measurement
                }
                if (wt[2] > 0.) {
                    NM = NM + 1; // also coarse
                }
                double[] q = ip.q();    //impact point in local coordinates
                double[] uvm = h.uvm();  // hit in local coordinates
                double[][] DER = new double[2][4];
                double ti = ip.ti();
                if (debug()) {
                    System.out.println("ti " + ti);
                }
//                if (debug()) {
//                    System.out.println("uvwg[0]");
//                }
//                if (debug()) {
//                    uvwg[0].print(6, 4);
//                }
//                if (debug()) {
//                    System.out.println("uvwg[1]");
//                }
//                if (debug()) {
//                    uvwg[1].print(6, 4);
//                }
//                if (debug()) {
//                    System.out.println("uvwg[2]");
//                }
//                if (debug()) {
//                    uvwg[2].print(6, 4);
//                }

                for (int j = 0; j < NM; ++j) {
                    eps[j] = q[j] - uvm[j]; //predicted minus measured
                    DER[j][0] = uvwg[j].get(0, 0) - uvwg[2].get(0, 0) * BUVW[j] / BUVW[2];
                    DER[j][1] = uvwg[j].get(1, 0) - uvwg[2].get(1, 0) * BUVW[j] / BUVW[2];
                    DER[j][2] = ti * DER[j][0];
                    DER[j][3] = ti * DER[j][1];
                    //if (debug()) {
                    if (debug()) {
                        System.out.println("j " + (j + 1) + " q " + q[j] + " UVM " + uvm[j] + " eps " + eps[j]);
                    }
                    //}
                    if (debug()) {
                        System.out.println(uvwg[j].get(0, 0) + " " + uvwg[2].get(0, 0) + " " + BUVW[j] + " " + BUVW[2]);
                    }
                    if (debug()) {
                        System.out.println(uvwg[j].get(1, 0) + " " + uvwg[2].get(1, 0) + " " + BUVW[j] + " " + BUVW[2]);
                    }
                    if (debug()) {
                        System.out.println(DER[j][0]);
                    }
                    if (debug()) {
                        System.out.println(DER[j][1]);
                    }
                    if (debug()) {
                        System.out.println(DER[j][2]);
                    }
                    if (debug()) {
                        System.out.println(DER[j][3]);
                    }
                    NDF = NDF + 1;
                }

                if (NM > 0) {
                    if (debug()) {
                        System.out.println("CALLING TRASAT");
                    }
                    DCHI = TRASAT(eps, wt, NM);
                    if (debug()) {
                        System.out.println(" EPS " + eps[0] + " " + eps[1]);
                    }
                    if (debug()) {
                        System.out.println(" WT(" + (i + 1) + ") " + wt[0] + " " + wt[1] + " " + wt[2]);
                    }
                    if (debug()) {
                        System.out.println(" DCHI " + DCHI);
                    }
                    CHI = CHI + DCHI;
                    if (debug()) {
                        System.out.println("CALLING TRATSA");
                    }
                    double[] WP = TRATSA(DER, wt, NM, 4);
                    for (int jj = 0; jj < NM; ++jj) {
                        if (debug()) {
                            System.out.println(" DER " + (jj + 1) + ") " + DER[jj][0] + " " + DER[jj][1] + " " + DER[jj][2] + " " + DER[jj][3] + " ");
                        }
                    }
                    if (debug()) {
                        System.out.println("wt(" + wt[0] + " " + wt[1] + " " + wt[2]);
                    }
                    if (debug()) {
                        System.out.println(" WP " + Arrays.toString(WP));
                    }
                    if (debug()) {
                        System.out.println("CALLING TRATS");
                    }
                    double[][] C = TRATS(DER, wt, NM, 4);
                    for (int jj = 0; jj < NM; ++jj) {
                        if (debug()) {
                            System.out.println(" C(" + (jj + 1) + ") " + C[0][jj] + " " + C[1][jj] + " " + C[2][jj] + " " + C[3][jj]);
                        }
                    }
                    if (debug()) {
                        System.out.println(" EPS " + eps[0] + " " + eps[1]);
                    }
                    if (debug()) {
                        System.out.println("CALLING MXMPY(C, EPS, 4, 2, 1)");
                    }
                    double[] WV = new double[4]; //MXMPY(C, eps, 4, 2, 1);
                    if (debug()) {
                        System.out.println(" WV " + WV[0] + " " + WV[1] + " " + WV[2] + " " + WV[3]);
                    }
                    myMXMPY(C, eps, WV, 4, NM, 1);
                    if (debug()) {
                        System.out.println(" WV " + WV[0] + " " + WV[1] + " " + WV[2] + " " + WV[3]);
                    }
                    if (debug()) {
                        System.out.println("CALLING VADD");
                    }
                    WMT = VADD(WMT, WP);
                    if (debug()) {
                        System.out.println(" WMT " + Arrays.toString(WMT));
                    }
                    if (debug()) {
                        System.out.println("CALLING VADD");
                    }
                    WVT = VADD(WVT, WV);
                    if (debug()) {
                        System.out.println("WVT(" + WVT[0] + " " + WVT[1] + " " + WVT[2] + " " + WVT[3]);
                    }
                }
            } // end of loop over planes
            if (NDF > 0) {
                if (debug()) {
                    System.out.println("CALLING TRSINV");
                }
                COV = TRSINV(WMT, 4);
                if (debug()) {
                    System.out.println(" COV " + Arrays.toString(COV));
                }
                if (debug()) {
                    System.out.println("CALLING TRSA");
                }
                double[] DPAR = TRSA(COV, WVT, 4); // parameters delta
                if (debug()) {
                    System.out.println("DPAR " + Arrays.toString(DPAR));
                }
                if (debug()) {
                    System.out.println("CALLING VSUB");
                }
                if (debug()) {
                    System.out.println(" PAR " + Arrays.toString(PAR));
                }
                PAR = VSUB(PAR, DPAR);
                if (debug()) {
                    System.out.println("PAR-DPAR " + Arrays.toString(PAR));
                }
                if (debug()) {
                    System.out.println(" CHI0 " + CHI0 + " CHI " + CHI + " CHI0-CHI " + (CHI0 - CHI));
                }
            }
            // end of while loop
        } while (CHI0 - CHI > 1);

        if (debug()) {
            System.out.println("PAR " + Arrays.toString(PAR));
        }
        if (debug()) {
            System.out.println("COV " + Arrays.toString(COV));
        }
        int count = 0;
        List<double[]> impactPoints = new ArrayList<double[]>();
        for (ImpactPoint ip : rx) {
            if (debug()) {
                System.out.println("RX( " + (count + 1) + " )" + Arrays.toString(ip.r()));
            }
            impactPoints.add(ip.r());
            count++;
        }
        if (debug()) {
            System.out.println("CHI " + CHI);
            System.out.println("NDF " + NDF);
            System.out.println("NIT " + NIT);
        }
        return new TrackFit(PAR, COV, impactPoints, CHI, NDF, NIT);
    }

    static List<DetectorPlane> GENER_DET() {
        int NN = 12;
        double[] SIGS = {0.006, 0.00};     //  detector resolutions in mm

        List<double[]> angles = new ArrayList<double[]>();
        angles.add(new double[]{3.1406969851087094, 0.03177473972250014, -1.5707096750665268});
        angles.add(new double[]{-8.763843967516362E-4, -0.030857430620294213, -1.6707969967061551});
        angles.add(new double[]{-3.1371868210063276, 0.031982073016205884, -1.570800276546960});
        angles.add(new double[]{-0.008253950195711953, -0.031206900985050606, -1.670332814679196});
        angles.add(new double[]{3.140449458676607, 0.02612204537702204, -1.5708809505162877});
        angles.add(new double[]{-0.005357612619242289, -0.025781003210562113, -1.6704142005768545});
        angles.add(new double[]{-3.135680720223676, 0.03153474000599398, -1.570835921868696});
        angles.add(new double[]{0.004079561821680232, -0.034697901407559295, -1.6205748135045286});
        angles.add(new double[]{-3.1358886943959, 0.030816539774356996, -1.5711458236970879});
        angles.add(new double[]{0.006434838235404464, -0.032235412982587405, -1.6204426401435137});
        angles.add(new double[]{3.1410568732583375, 0.030229266015029423, -1.5709559555513355});
        angles.add(new double[]{-2.1930870918929454E-4, -0.031152570497038165, -1.6201278501297305});

        List<double[]> r0 = new ArrayList<double[]>();
        r0.add(new double[]{3.083251888032933, 20.6939715764943, 87.90907758988494});
        r0.add(new double[]{3.3897589906322203, 20.723713416767932, 96.09347236561139});
        r0.add(new double[]{6.209507497482065, 22.251100764548063, 187.979198401254});
        r0.add(new double[]{6.448495884160572, 22.27056067869049, 195.97435895116467});
        r0.add(new double[]{9.266018127938018, 23.77017055031802, 287.706424933812});
        r0.add(new double[]{9.524206517587587, 23.79411639949488, 295.78541118932907});
        r0.add(new double[]{-35.45499313137205, 26.704165491252628, 489.9250042201181});
        r0.add(new double[]{-35.177966825203924, 29.23243054764656, 496.79862110072156});
        r0.add(new double[]{-29.40714364121509, 29.778421220699943, 689.7885600858451});
        r0.add(new double[]{-29.05999883738231, 32.30110797572307, 697.1004544791742});
        r0.add(new double[]{-23.24754288591039, 32.76458780556233, 889.4575319549996});
        r0.add(new double[]{-22.89425800691335, 35.2745708242259, 896.9124291322955});

        List<Matrix> rot = new ArrayList<Matrix>();

        List<DetectorPlane> planes = new ArrayList<DetectorPlane>();

        {
            System.out.println("GENERATING DETECTOR WITH " + NN + " DETECTOR PLANES");
        }
        for (int i = 0; i < NN; ++i) {
//            if (debug()) {
//                System.out.println("Detector " + (i + 1));
//            }
//            if (debug()) {
//                System.out.println("Generating rotation matrices for detector " + (i + 1));
//            }
            double[] a = angles.get(i);
            Matrix[] mats = new Matrix[3];
            double[][] RW = new double[3][9];
            for (int j = 0; j < 3; ++j) {
                double angl = a[j];
                mats[j] = GEN_ROTMAT(angl, j);
                GEN_ROTMAT(angl, j, RW[j]);
//                if (debug()) {
//                    System.out.println("Rotation matrix for axis " + (j + 1) + " angle " + angl);
//                }
//                if (debug()) {
//                    mats[j].print(6, 4);
//                }
            }
            Matrix prodrot = PROD_ROT(mats[0], mats[1], mats[2]);
//            System.out.println("prodrot ");
//            prodrot.print(10,6);
            // combined rotation
            double[] PRODROT = new double[9];
            PROD_ROT(RW[2], RW[1], RW[0], PRODROT); //delta rotation
            if (debug()) {
                System.out.println("Combined rotation matrix is");
                prodrot.print(10, 6);
            }
            System.out.println("PRODROT "+Arrays.toString(PRODROT));
            rot.add(prodrot);
//            if (debug()) {
//                System.out.println("Translation vector is");
//            }
            double[] off = r0.get(i);
//            if (debug()) {
//                System.out.println(off[0] + " " + off[1] + " " + off[2]);
//                System.out.println("Sigs: " + SIGS[0] + " " + SIGS[1]);
//            }
            planes.add(new DetectorPlane(i, prodrot, off, SIGS));
        }
        return planes;
    }

    static void GEN_ROTMAT(double ANG, int IAX, double[][] ROTM) {
// *DOC  SUBROUTINE GEN_ROTMAT(ANG,IAX,ROTM)
//*DOC  Action: Generate rotation matrix around axis IAX
//*DOC
//*DOC  Input: ANG = rotation angle (rad) around axis IAX
//*DOC         IAX = axis number (1, 2 or 3) of rotation
//*DOC
//*DOC  Output: ROTM = rotation matrix  
        ROTM[IAX][IAX] = 1.;
        double C = cos(ANG);
        double S = sin(ANG);
        if (IAX == 0) {
            ROTM[1][1] = C;
            ROTM[2][1] = S;
            ROTM[1][2] = -S;
            ROTM[2][2] = C;
        } else if (IAX == 1) {
            ROTM[0][0] = C;
            ROTM[2][0] = -S;
            ROTM[0][2] = S;
            ROTM[2][2] = C;
        } else if (IAX == 2) {
            ROTM[0][0] = C;
            ROTM[1][0] = S;
            ROTM[0][1] = -S;
            ROTM[1][1] = C;
        }
    }

    static Matrix PROD_ROT(Matrix rx, Matrix ry, Matrix rz) {
        return rz.times(ry.times(rx));
    }

    static void GEN_ROTMAT(double ANG, int IAX, double[] R) {
        double[][] r = new double[3][3];
        if (debug()) {
            System.out.println("ANG " + ANG + " IAX " + (IAX + 1));
        }
        r[IAX][IAX] = 1;
        double C = cos(ANG);
        double S = sin(ANG);
        switch (IAX) {
            case 0:
                r[1][1] = C;
                r[1][2] = S;
                r[2][1] = -S;
                r[2][2] = C;
                break;
            case 1:
                r[0][0] = C;
                r[0][2] = -S;
                r[2][0] = S;
                r[2][2] = C;
                break;
            case 2:
                r[0][0] = C;
                r[0][1] = S;
                r[1][0] = -S;
                r[1][1] = C;
                break;
            default:
                break;
        }
        int n = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                R[n++] = r[i][j];
            }
        }
    }

    static Matrix GEN_ROTMAT(double ANG, int IAX) {
        Matrix r = Matrix.identity(3, 3);
        if (debug()) {
            System.out.println("ANG " + ANG + " IAX " + (IAX + 1));
        }
        double C = cos(ANG);
        double S = sin(ANG);
        switch (IAX) {
            case 0:
                r.set(1, 1, C);
                r.set(1, 2, S);
                r.set(2, 1, -S);
                r.set(2, 2, C);
                break;
            case 1:
                r.set(0, 0, C);
                r.set(0, 2, -S);
                r.set(2, 0, S);
                r.set(2, 2, C);
                break;
            case 2:
                r.set(0, 0, C);
                r.set(0, 1, S);
                r.set(1, 0, -S);
                r.set(1, 1, C);
                break;
            default:
                break;
        }
        if (debug()) {
            r.print(10, 6);
        }
        return r;
    }

    static void PROD_ROT(double[] RA, double[] RB, double[] RC, double[] RTOT) {
        double[] RAPU = new double[9];
        MXMPY(RA, RB, RAPU, 3, 3, 3);
        MXMPY(RAPU, RC, RTOT, 3, 3, 3);
    }

    static void MXMPY(double[] A, double[] B, double[] C, int I, int J, int K) {
        int IIA = 1;
        int IOA = J;
        int IIB = K;
        int IOB = 1;
        int IA = 0;
        int IC = 0;
        for (int L = 0; L < I; ++L) {
            int IB = 0;
            for (int M = 0; M < K; ++M) {
                C[IC] = 0.;
                if (J > 0) {
                    int JA = IA;
                    int JB = IB;
                    for (int N = 0; N < J; ++N) {
                        C[IC] = C[IC] + A[JA] * B[JB];
                        JA = JA + IIA;
                        JB = JB + IIB;
                    }
                    IB = IB + IOB;
                    IC = IC + 1;
                }
            }
            IA = IA + IOA;
        }
    }

    static ImpactPoint GET_IMPACT(double[] A, double[] B, Matrix ROT, double[] R0, Matrix wg, double BWG) {
        if (debug()) {
            System.out.println("GET_IMPACT A " + Arrays.toString(A));
            System.out.println("GET_IMPACT B " + Arrays.toString(B));
            System.out.println("GET_IMPACT R0 " + Arrays.toString(R0));
        }
        double[] AMR = VSUB(A, R0);
        Matrix amr = new Matrix(AMR, 3);
        if (debug()) {
            System.out.println("GET_IMPACT AMR " + AMR[0] + " " + AMR[1] + " " + AMR[2]);
        }
        double ti = -amr.transpose().times(wg).get(0, 0) / BWG;
        if (debug()) {
            System.out.println("GET_IMPACT PARAMETER AT IMPACT TI " + ti);
        }
        double[] APU = VLINE(AMR, 1., B, ti);
        if (debug()) {
            System.out.println("GET_IMPACT APU " + APU[0] + " " + APU[1] + " " + APU[2]);
        }
        double[] q = VMATL(ROT, APU);
        if (debug()) {
            System.out.println("GET_IMPACT LOCAL U,V,W Q " + q[0] + " " + q[1] + " " + q[2]);
        }
        double[] r = VADD(APU, R0);
        if (debug()) {
            System.out.println("GET_IMPACT GLOBAL X,Y,Z R " + r[0] + " " + r[1] + " " + r[2]);
        }
        return new ImpactPoint(ti, q, r);
    }

    static double[] TRSINV(double[] A, int M) {
        // invert symmetric matrix given by lower diagonal element array...
        Matrix a = new Matrix(M, M);
        a.set(0, 0, A[0]);
        a.set(1, 0, A[1]);
        a.set(1, 1, A[2]);
        a.set(2, 0, A[3]);
        a.set(2, 1, A[4]);
        a.set(2, 2, A[5]);
        a.set(3, 0, A[6]);
        a.set(3, 1, A[7]);
        a.set(3, 2, A[8]);
        a.set(3, 3, A[9]);

        a.set(0, 1, a.get(1, 0));
        a.set(0, 2, a.get(2, 0));
        a.set(0, 3, a.get(3, 0));
        a.set(1, 2, a.get(2, 1));
        a.set(1, 3, a.get(3, 1));
        a.set(2, 3, a.get(3, 2));

        if (debug()) {
            a.print(6, 4);
        }

        Matrix b = a.inverse();
        if (debug()) {
            b.print(6, 4);
        }

        double[] r = new double[10];
        r[0] = b.get(0, 0);
        r[1] = b.get(1, 0);
        r[2] = b.get(1, 1);
        r[3] = b.get(2, 0);
        r[4] = b.get(2, 1);
        r[5] = b.get(2, 2);
        r[6] = b.get(3, 0);
        r[7] = b.get(3, 1);
        r[8] = b.get(3, 2);
        r[9] = b.get(3, 3);
        return r;

    }

    static double[][] TRATS(double[][] B, double[] R, int N, int M) {
        // s is symmetric matrix packed as lower diagonal
        // R = B'S
        // N=2, M=4 
        Matrix b = new Matrix(N, M);
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < M; ++j) {
                b.set(i, j, B[i][j]);
            }
        }
        Matrix s = new Matrix(N, N); // symmetric
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j <= i; ++j) {
                s.set(i, j, R[i * N + j]);
                s.set(j, i, R[i * N + j]);
            }
        }
        if (debug()) {
            s.print(6, 4);
        }
        if (debug()) {
            b.print(6, 4);
        }
        Matrix r = b.transpose().times(s);
        if (debug()) {
            r.print(6, 4);
        }
        return r.getArray();
    }

    static double[] TRATSA(double[][] B, double[] S, int N, int M) {
        // s is symmetric matrix packed as lower(?) diagonal
        // R = B'SB
        // N=2, M=4
        Matrix b = new Matrix(N, M);
        Matrix s = new Matrix(N, N); // symmetric
        s.set(0, 0, S[0]);
        if (debug()) {
            s.print(6, 4);
        }
        if (debug()) {
            b.print(6, 4);
        }
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < M; ++j) {
                b.set(i, j, B[i][j]);
            }
        }
        if (debug()) {
            s.print(6, 4);
        }
        if (debug()) {
            b.print(6, 4);
        }
        Matrix r = b.transpose().times(s.times(b));
        if (debug()) {
            r.print(6, 4);
        }
        //OK, now get the lower diagonal
        //matrix is now MxM
        double[] R = new double[10];
        int n = 0;
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j <= i; ++j) {
                R[n++] = r.get(i, j);
            }
        }
        return R;
    }

    static double TRASAT(double[] a, double[] s, int N) {
        // s is a symmetric (lower diagonal?) matrix
        // a is a vector
        double r = 0.;
        if (N == 1) {
            r = a[0] * s[0] * a[0];
        } else if (N == 2) {
            r = a[0] * (a[0] * s[0] + a[1] * s[1]) + a[1] * (a[0] * s[1] + a[1] * s[2]);
        }
        return r;
    }

    static double[] TRSA(double[] S, double[] A, int N) {
//CNG CALL TRSA (S,A,C,M,N) SA -> C
//CNG A and C are M X N rectangular matrices
//CNG S is an M X M symmetrix matrix
        Matrix s = new Matrix(N, N);
        s.set(0, 0, S[0]);
        s.set(1, 0, S[1]);
        s.set(1, 1, S[2]);
        s.set(2, 0, S[3]);
        s.set(2, 1, S[4]);
        s.set(2, 2, S[5]);
        s.set(3, 0, S[6]);
        s.set(3, 1, S[7]);
        s.set(3, 2, S[8]);
        s.set(3, 3, S[9]);

        s.set(0, 1, s.get(1, 0));
        s.set(0, 2, s.get(2, 0));
        s.set(0, 3, s.get(3, 0));
        s.set(1, 2, s.get(2, 1));
        s.set(1, 3, s.get(3, 1));
        s.set(2, 3, s.get(3, 2));

        if (debug()) {
            s.print(6, 4);
        }
        Matrix a = new Matrix(4, 1);
        a.set(0, 0, A[0]);
        a.set(1, 0, A[1]);
        a.set(2, 0, A[2]);
        a.set(3, 0, A[3]);
        if (debug()) {
            a.print(6, 4);
        }

        Matrix d = s.times(a);
        if (debug()) {
            d.print(6, 4);
        }

        double[] r = new double[4];
        r[0] = d.get(0, 0);
        r[1] = d.get(1, 0);
        r[2] = d.get(2, 0);
        r[3] = d.get(3, 0);

        return r;
    }

    static void myMXMPY(double[][] A, double[] B, double[] C, int I, int J, int K) {
        int rdim = A.length;
        int cdim = A[0].length;
        double[] a = new double[rdim * cdim];
        int c = 0;
        for (int j = 0; j < cdim; ++j) {
            for (int i = 0; i < rdim; ++i) {
                a[c++] = A[i][j];
            }
        }
        myMXMPY(a, B, C, I, J, K);
    }

    static void myMXMPY(double[] A, double[] B, double[] C, int I, int J, int K) {
        int IIA = 1;
        int IOA = J;
        int IIB = K;
        int IOB = 1;
        int IA = 0;
        int IC = 0;
        for (int L = 0; L < I; ++L) {
            int IB = 0;
            for (int M = 0; M < K; ++M) {
                C[IC] = 0.;
                if (J > 0) {
                    int JA = IA;
                    int JB = IB;
                    for (int N = 0; N < J; ++N) {
                        C[IC] = C[IC] + A[JA] * B[JB];
                        JA = JA + IIA;
                        JB = JB + IIB;
                    } //ENDDO
                    IB = IB + IOB;
                    IC = IC + 1;
                }
            }
            IA = IA + IOA;
        }
    }

    static double[] VMATL(Matrix ROT, double[] APU) {
        double[] c = new double[3];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                c[i] += ROT.get(i, j) * APU[j];
            }
        }
        return c;
    }

    static double[] VLINE(double[] A, double F1, double[] B, double F2) {
        double[] c = new double[A.length];
        for (int i = 0; i < A.length; ++i) {
            c[i] = A[i] * F1 + B[i] * F2;
        }
        return c;
    }

    static double[] VSUB(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; ++i) {
            c[i] = a[i] - b[i];
        }
        return c;
    }

    static double[] VADD(double[] a, double[] b) {
        double[] c = new double[a.length];
        for (int i = 0; i < a.length; ++i) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    static List<String> readEvents(String filename) {
        List<String> lines = null;
        try {
            Path path = Paths.get(filename);
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }

    static List<Hit> nextEvent(String line, double[] par) {
        String[] vals = line.trim().split("\\s+");

        for (int i = 0; i < 4; ++i) {
            par[i] = Double.parseDouble(vals[i + 24]);
        }
        List<Hit> hits = new ArrayList<Hit>();
        for (int i = 0; i < 12; ++i) {
            double[] meas = {Double.parseDouble(vals[i]), 0.};     // only looking at 1D strip hits
            double[] cov = {Double.parseDouble(vals[i + 12]), 0., 0.};  // lower diag cov matrix
            hits.add(new Hit(meas, cov));
        }
        return hits;
    }

    static boolean debug() {
        return false;
    }

    static boolean debug2() {
        return false;
    }
}
