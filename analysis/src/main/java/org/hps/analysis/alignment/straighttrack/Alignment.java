package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Norman Graf
 */
public class Alignment {

    int IDT;
    int NPR;
    double[] WMAT = new double[21];
    double[] WVEC = new double[6];
    int NPARF;
    int[] _MASK = new int[6]; // which parameters to align (3 pos, 3rot)
    int[] MPNT = new int[6];
    List<double[]> DEROTs = new ArrayList<double[]>();
    double[] PARC = new double[6];
    double[] _ROT = new double[9];  // best guess plane rotation matrix
    double[] _R0 = new double[3];   // best guess plane offsets

    boolean _debug = false;
    boolean _debugS = false;

    public Alignment(int layerNumber, int[] MASK, double[] ROT, double[] R0) {
        IDT = layerNumber;
        for (int i = 0; i < 6; ++i) {
            NPARF += MASK[i];
            MPNT[i] = (NPARF * MASK[i]) - 1;
        }
        NPR = NPARF;
        System.arraycopy(MASK, 0, _MASK, 0, 6);
        System.arraycopy(ROT, 0, _ROT, 0, 9);
        System.arraycopy(R0, 0, _R0, 0, 3);
        DEROTs = ROT_DER(ROT);
        if (_debug) {
            System.out.println("Creating Alignment for layer " + IDT);
        }
        if (_debug) {
            System.out.println("NPARF " + NPARF);
        }
        if (_debug) {
            System.out.println("MASK " + Arrays.toString(MASK));
        }
        if (_debug) {
            System.out.println("MPNT " + Arrays.toString(MPNT));
        }
        for (int i = 0; i < 3; ++i) {
            if (_debug) {
                System.out.println("DEROTs[ " + i + "] " + Arrays.toString(DEROTs.get(i)));
            }
        }
    }

    public void setRot(double[] ROT) {
        System.arraycopy(ROT, 0, _ROT, 0, 9);
    }

    public void setR0(double[] R0) {
        System.arraycopy(R0, 0, _R0, 0, 3);
    }

    public void accumulate(double[] RX, double[] S, double[] QM, double[] W) {
//*DOC  Input:
//*DOC            IFL = 0/1 for accumulation/solution
//*DOC            IDT = detector plane number
//*DOC            MASK[6] = mask indicating coefficients to be fitted
//                          3 displacements + 3 rotations        
//*DOC                  e.g. 1,1,0,1,0,0  (pars 1,2,4 to be fitted)
//*DOC            RX[3]  = impact point coordinates
//*DOC            S[3]   = track direction at impact point
//*DOC            QM[2]  = measured coordinates in local
//*DOC            W[3]   = weight matrix
//*DOC            ROT[9] = rotation matrix (best known so far)
//*DOC            R0[3]  = coordinates of local origin in global
//*DOC            PAR[6] = local offsets and tilt angles (3+3) 
//*DOC  Output:
//*DOC            For IFL = 1:
//*DOC            ROT[9] = corrected rotation matrix
//*DOC            R0[3]  = corrected coordinates of local origin in global
//*DOC            PAR[6] = local offsets and tilt angles (3+3) 
        // shouldn't need IDT, MASK, PAR or COV as input?
        if (_debug) {
            System.out.println("NPR " + NPR);
        }
        if (_debug) {
            System.out.println("MASK " + Arrays.toString(_MASK));
        }
        if (_debug) {
            System.out.println("RX " + Arrays.toString(RX));
        }
        if (_debug) {
            System.out.println("S  " + Arrays.toString(S));
        }
        if (_debug) {
            System.out.println("QM " + Arrays.toString(QM));
        }
        if (_debug) {
            System.out.println("W " + Arrays.toString(W));
        }
        if (_debug) {
            System.out.println("ROT " + Arrays.toString(_ROT));
        }
        if (_debug) {
            System.out.println("R0 " + Arrays.toString(_R0));
        }
//        if(_debug) System.out.println("PARIN " + Arrays.toString(PAR));

        if (_debug) {
            System.out.println("NPARF " + NPARF);
        }
        if (_debug) {
            System.out.println("MPNT " + Arrays.toString(MPNT));
        }

        double[] SLOC = new double[3];
        VMATL(_ROT, S, SLOC, 3, 3);
        if (_debug) {
            System.out.println("SLOC" + Arrays.toString(SLOC));
        }
        if (_debug) {
            System.out.println("'CALLING VSUB(RX,R0,RXMR0)");
        }
        double[] RXMR0 = new double[3];
        VSUB(RX, _R0, RXMR0, 3);
        if (_debug) {
            System.out.println("RXMR0 " + Arrays.toString(RXMR0));
        }
        double[][] DRA = new double[3][3];
        for (int i = 0; i < 3; ++i) {
            if (_debug) {
                System.out.println("CALLING VMATL(DEROT(1,I,IDT),RXMR0,DRA(1,I),3,3)");
            }
            double[] DEROT = DEROTs.get(i);
            double[] tmp = new double[3];
            if (_debug) {
                System.out.println("DEROT " + Arrays.toString(DEROT));
            }
            VMATL(DEROT, RXMR0, tmp, 3, 3);
            DRA[i] = tmp;
            if (_debug) {
                System.out.println("DRA[ " + i + " ]= " + Arrays.toString(tmp));
            }
        }
        double[][] DER = new double[2][6];
        DER[0][0] = -1.;
        DER[1][1] = -1;
        DER[0][2] = SLOC[0] / SLOC[2];
        DER[0][3] = DRA[0][0] - DRA[0][2] * DER[0][2];
        DER[0][4] = DRA[1][0] - DRA[1][2] * DER[0][2];
        DER[0][5] = DRA[2][0] - DRA[2][2] * DER[0][2];
        DER[1][2] = SLOC[1] / SLOC[2];
        DER[1][3] = DRA[0][1] - DRA[0][2] * DER[1][2];
        DER[1][4] = DRA[1][1] - DRA[1][2] * DER[1][2];
        DER[1][5] = DRA[2][1] - DRA[2][2] * DER[1][2];
        if (_debug) {
            System.out.println("DER[0] " + Arrays.toString(DER[0]));
        }
        if (_debug) {
            System.out.println("DER[1] " + Arrays.toString(DER[1]));
        }
        double[] XJ = new double[12];
        for (int i = 0; i < 6; ++i) {
            if (MPNT[i] > -1) {
//                if(_debug) System.out.println("i "+i+" MPNT["+i+"]= "+MPNT[i]);
                XJ[MPNT[i]] = DER[0][i];
                if (_debug) {
                    System.out.println("XJ[ " + MPNT[i] + " ]= " + DER[0][i]);
                }
//                if(_debug) System.out.println("NPR "+NPR);
//                if(_debug) System.out.println(" "+(MPNT[i] + NPR));
                XJ[MPNT[i] + NPR] = DER[1][i];
                if (_debug) {
                    System.out.println("XJ{ " + (MPNT[i] + NPR) + " ]= " + DER[1][i]);
                }
            }
        }
        if (_debug) {
            System.out.println("CALLING VMATL(ROT,RXMR0,QX,3,3)");
        }
        double[] QX = new double[3];

        VMATL(_ROT, RXMR0, QX, 3, 3);
        if (_debug) {
            System.out.println("QX " + Arrays.toString(QX));
        }
        if (_debug) {
            print("QM", QM);
        }
        double[] EPS = new double[2];
        EPS[0] = QX[0] - QM[0];
        EPS[1] = QX[1] - QM[1];
        if (_debug) {
            print("EPS", EPS);
        }

        if (_debug) {
            System.out.println("CALLING TRASAT(EPS,W,DCHI,1,2)");
        }
        double DCHI = TRASAT(EPS, W, 2);
        if (_debug) {
            System.out.println("DCHI " + DCHI);
        }
        if (_debug) {
            System.out.println(" CALLING TRATSA(XJ,W,WP,NPR,2)");
        }
        if (_debug) {
            System.out.println("NPR " + NPR);
        }
        if (_debug) {
            print("XJ ", XJ);
        }
        if (_debug) {
            print("W ", W);
        }
        double[] WP = TRATSA(XJ, W, NPR, 2);
        if (_debug) {
            print("WP ", WP);
        }

        if (_debug) {
            System.out.println("CALLING TRATS(XJ,W,C,NPR,2)");
        }
        double[] C = TRATS(XJ, W, NPR, 2);
        if (_debug) {
            System.out.println("C " + Arrays.toString(C));
        }
        if (_debug) {
            System.out.println("CALLING MXMPY(C,EPS,WV,NPR,2,1)");
        }
        if (_debug) {
            print("EPS ", EPS);
        }
        if (_debug) {
            System.out.println("NPR " + NPR);
        }
        double[] WV = new double[6];
        MXMPY(C, EPS, WV, NPR, 2, 1);
        if (_debug) {
            print("WV ", WV);
        }
        int NELEMF = NPR * (NPR + 1) / 2;
//        double[] WMAT = new double[21];
        if (_debug) {
            System.out.println("CALL VADD(WMAT(1,IDT),WP,WMAT(1,IDT),NELEMF)");
        }
        if (_debug) {
            print("WMAT ", WMAT);
        }
        if (_debug) {
            print("WP ", WP);
        }
        WMAT = VADD(WMAT, WP, NELEMF);
        if (_debug) {
            print("WMAT ", WMAT);
        }
        //TODO understand what this is and why it's hinky
        if (_debug) {
            System.out.println("CALLING VADD(WVEC(1,IDT),WV,WVEC(1,IDT),NPR)");
        }
        if (_debug) {
            print("WVEC ", WVEC);
        }
        if (_debug) {
            print("WV ", WV);
        }
        WVEC = VADD(WVEC, WV, NPR);
        if (_debug) {
            System.out.println(" *****");
            print("WVEC ", WVEC);
            System.out.println(" *****");
            System.out.println(" ");
        }
    }

    public void setWMAT(double[] mat) {
        WMAT = mat;
    }

    public void setWVEC(double[] vec) {
        WVEC = vec;
    }

    public void setPARC(double[] p) {
        PARC = p;
    }

    public void solve(double[] PAR, double[] COV, double[] rot, double[] r0) {//(int IDT, int[] MASK, double[] RX, double[] S, double[] QM, double[] W, double[] ROT, double[] R0, double[] PAR, double[] COV) {
//*DOC  Input:
//*DOC            IFL = 0/1 for accumulation/solution
//*DOC            IDT = detector plane number
//*DOC            MASK[6] = mask indicating coefficients to be fitted
//                          3 displacements + 3 rotations        
//*DOC                  e.g. 1,1,0,1,0,0  (pars 1,2,4 to be fitted)
//*DOC            RX[3]  = impact point coordinates
//*DOC            S[3]   = track direction at impact point
//*DOC            QM[2]  = measured coordinates in local
//*DOC            W[3]   = weight matrix
//*DOC            ROT[9] = rotation matrix (best known so far)
//*DOC            R0[3]  = coordinates of local origin in global
//*DOC            PAR[6] = local offsets and tilt angles (3+3) 
//*DOC  Output:
//*DOC            For IFL = 1:
//*DOC            ROT[9] = corrected rotation matrix
//*DOC            R0[3]  = corrected coordinates of local origin in global
//*DOC            PAR[6] = local offsets and tilt angles (3+3) 
        if (_debugS) {
            System.out.println("Alignment solve");
        }
        if (_debugS) {
            System.out.println("NPR " + NPR);
        }
        if (_debugS) {
            print("WMAT ", WMAT);
        }
        TRSINV(WMAT, COV, NPR);      // covariance matrix
        if (_debugS) {
            print("COV ", COV);
        }
        if (_debugS) {
            System.out.println("CALLING TRSA(COV,WVEC(1,IDT),PAR,NPR,1)");
        }
        if (_debugS) {
            print("WVEC ", WVEC);
        }
        TRSA(COV, WVEC, PAR, NPR, 1);
        if (_debugS) {
            print("PAR ", PAR);
        }
        double[] CORR = new double[6];
        for (int i = 0; i < 6; ++i) {
            if (MPNT[i] > -1) {
                CORR[i] = PAR[MPNT[i]];
            }
        }
        if (_debugS) {
            print("CORR ", CORR);
        }
        if (_debugS) {
            System.out.println("CALLING VSUB(PARC(1,IDT),CORR,PARC(1,IDT),6)");
        }
        if (_debugS) {
            print("PARC ", PARC);
        }
        VSUB(PARC, CORR, PARC, 6);
        if (_debugS) {
            print("PARC ", PARC);
        }
        if (_debugS) {
            System.out.println("CALLING VMATR(CORR,ROT,R0CORR,3,3)");
        }
        if (_debugS) {
            print("ROT ", _ROT);
        }
        double[] R0CORR = new double[3];
        VMATR(CORR, _ROT, R0CORR, 3, 3);
        if (_debugS) {
            print("R0CORR ", R0CORR);
        }
        if (_debugS) {
            System.out.println("CALLING VSUB(R0,R0CORR,R0,3)");
        }
        if (_debugS) {
            print("R0 ", _R0);
        }
        VSUB(_R0, R0CORR, _R0, 3);
        if (_debugS) {
            print("R0 ", _R0);
        }
        if (_debugS) {
            System.out.println("CALLING  CALL VSCALE(CORR(4),-1.,CORR(4),3)");
        }
        CORR[3] = -CORR[3];
        CORR[4] = -CORR[4];
        CORR[5] = -CORR[5];
        if (_debugS) {
            print("CORR ", CORR);
        }
        double[][] ROC = new double[3][9];
        for (int i = 0; i < 3; ++i) {
            if (_debugS) {
                System.out.println("CALLING GEN_ROTMAT(CORR(3+I),I,ROC(1,I)) " + i);
            }
            GEN_ROTMAT(CORR[3 + i], i, ROC[i]);
            if (_debugS) {
                print("ROC ", ROC[i]);
            }
        }
        if (_debugS) {
            System.out.println("CALLING PROD_ROT(ROC(1,3),ROC(1,2),ROC(1,1), DROT)");
        }
        double[] DROT = new double[9];
        PROD_ROT(ROC[2], ROC[1], ROC[0], DROT);     // delta rot
        if (_debugS) {
            print("DROT ", DROT);
        }
        if (_debugS) {
            System.out.println("CALLING CALL MXMPY (ROT,DROT,ROTN,3,3,3)");
        }
        if (_debugS) {
            print("ROT ", _ROT);
        }
        double[] ROTN = new double[9];
        MXMPY(_ROT, DROT, ROTN, 3, 3, 3);             // corr rot
        if (_debugS) {
            print("ROTN ", ROTN);
        }
        System.arraycopy(ROTN, 0, _ROT, 0, 9);
        if (_debugS) {
            print("ROT ", _ROT);
        }
//        NPARF = 0;
        System.out.println("Fitted alignment parameters for plane " + IDT);
        for (int i = 0; i < 6; ++i) {
            int j = MPNT[i];
            if (j > -1) {
                int jj = j + 1;
//                System.out.println("i "+i+" j "+j+" j*(j+1) "+(j*(j+1)/2));
//                System.out.println("COV[j * (j + 1)/2] = COV("+(j * (j + 1)/2 )+")= "+COV[j * (j + 1) / 2]);
                System.out.println("PAR " + (i + 1) + ": " + PARC[i] + " +/- " + sqrt(COV[jj * (jj + 1) / 2 - 1]));
            } else {
                System.out.println("PAR " + (i + 1) + " fixed");
            }
        }
//        System.out.println(" *****");
//        System.out.println("_R0 " + Arrays.toString(_R0));
//        System.out.println("_ROT " + Arrays.toString(_ROT));
//        System.out.println("PAR " + Arrays.toString(PAR));
//        System.out.println("COV " + Arrays.toString(COV));
//        System.out.println(" *****");
//        System.out.println(" ");
        System.arraycopy(_ROT, 0, rot, 0, 9);
        System.arraycopy(_R0, 0, r0, 0, 3);

        // clear WMAT
        Arrays.fill(WMAT, 0.);
        // clear WVEC
        Arrays.fill(WVEC, 0.);
        // derivatives?
        DEROTs = ROT_DER(_ROT);
    }

    static void print(String s, double[] A) {
        System.out.println(s + Arrays.toString(A));
    }

    static void PROD_ROT(double[] RA, double[] RB, double[] RC, double[] RTOT) {
        double[] RAPU = new double[9];
        MXMPY(RA, RB, RAPU, 3, 3, 3);
        MXMPY(RAPU, RC, RTOT, 3, 3, 3);
    }

    static void GEN_ROTMAT(double ANG, int IAX, double[] R) {
        double[][] r = new double[3][3];
        //System.out.println("ANG " + ANG + " IAX " + (IAX + 1));
        r[IAX][IAX] = 1;
        double C = cos(ANG);
        double S = sin(ANG);
        if (IAX == 0) {
            r[1][1] = C;
            r[1][2] = S;
            r[2][1] = -S;
            r[2][2] = C;
        } else if (IAX == 1) {
            r[0][0] = C;
            r[0][2] = -S;
            r[2][0] = S;
            r[2][2] = C;
        } else if (IAX == 2) {
            r[0][0] = C;
            r[0][1] = S;
            r[1][0] = -S;
            r[1][1] = C;
        }
        int n = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                R[n++] = r[i][j];
            }
        }
    }

    static double[] VADD(double[] a, double[] b, int NELEMF) {
        double[] c = new double[NELEMF];
        for (int i = 0; i < NELEMF; ++i) {
            c[i] = a[i] + b[i];
        }
        return c;
    }

    static void VMATL(double[] H, double[] A, double[] X, int K, int N) {
        for (int i = 0; i < K; ++i) {
            for (int j = 0; j < N; ++j) {
                X[i] += A[j] * H[i * N + j];
            }
        }
    }

    static void VSUB(double[] A, double[] B, double[] X, int N) {
        for (int i = 0; i < N; ++i) {
            X[i] = A[i] - B[i];
        }
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

    static double[] TRATSA(double[] B, double[] S, int N, int M) {
        double[][] r = new double[M][N];
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < N; ++j) {
                r[i][j] = B[i * N + j];
            }
        }
        return TRATSA(r, S, N, M);
    }

    static double[] TRATSA(double[][] B, double[] S, int N, int M) {
        // s is symmetric matrix packed as lower(?) diagonal
        // R = B'SB
        // N=2, M=4
        Matrix b = new Matrix(B);
        Matrix s = new Matrix(M, M); // symmetric
        s.set(0, 0, S[0]);
        s.set(0, 1, S[1]);
        s.set(1, 0, S[1]);
        s.set(1, 1, S[2]);
        Matrix r = b.transpose().times(s.times(b));

        //OK, now get the lower diagonal
        //matrix is now MxM
        double[] R = new double[N * (N + 1) / 2];
        int n = 0;
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j <= i; ++j) {
                R[n++] = r.get(i, j);
            }
        }
        return R;
    }

    static double[] TRATS(double[] B, double[] S, int N, int M) {
        double[][] r = new double[M][N];
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < N; ++j) {
                r[i][j] = B[i * N + j];
            }
        }
        return TRATS(r, S, N, M);
    }

    static double[] TRATS(double[][] B, double[] S, int N, int M) {
        // s is symmetric matrix packed as lower diagonal
        // R = B'S
        // N=2, M=4 
        Matrix b = new Matrix(B);
        Matrix s = new Matrix(M, M); // symmetric
        //TODO fix this
        s.set(0, 0, S[0]);
        s.set(0, 1, S[1]);
        s.set(1, 0, S[1]);
        s.set(1, 1, S[2]);
        //s.print(6, 4);

        Matrix r = s.times(b);
        // if(debug())System.out.println("r");
        //r.print(6, 4);
        return r.getColumnPackedCopy();
    }

    static void TRSINV(double[] A, double[] r, int M) {

        // invert symmetric matrix given by lower diagonal element array...
        Matrix a = new Matrix(M, M);
        int k = 0;
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j <= i; ++j) {
                a.set(i, j, A[k]);
                a.set(j, i, A[k]);
                k++;
            }
        }
//        a.print(6, 4);

        int[] ptr = new int[M]; // pointers for shifted row/col
        int ctr = 0; // reduced dimensionality og matrix
        for (int i = 0; i < M; ++i) {
            if (a.get(i, i) != 0.) {
                ptr[ctr++] = i;
            }
        }
//        System.out.println("reducedDim " + ctr);
//        System.out.println("ptr " + Arrays.toString(ptr));
        Matrix b = new Matrix(M, M);
        if (ctr == M) {
            b = a.inverse();
        } else {
            Matrix c = new Matrix(ctr, ctr);
            for (int i = 0; i < ctr; ++i) {
                for (int j = 0; j <= i; ++j) {
                    c.set(i, j, a.get(ptr[i], ptr[j]));
                    c.set(j, i, a.get(ptr[j], ptr[i]));
                }
            }
//            c.print(6, 4);
            Matrix d = c.inverse();
//            d.print(6, 4);
            // re-expand
            for (int i = 0; i < ctr; ++i) {
                for (int j = 0; j <= i; ++j) {
                    b.set(ptr[i], ptr[j], d.get(i, j));
                    b.set(ptr[j], ptr[i], d.get(j, i));
                }
            }
//            b.print(6,4);
        }
        k = 0;
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j <= i; ++j) {
                r[k] = b.get(i, j);
                ++k;
            }
        }
    }

    static void TRSA(double[] S, double[] A, double[] C, int M, int N) {
//CNG CALL TRSA (S,A,C,M,N) SA -> C
//CNG A and C are M X N rectangular matrices
//CNG S is an M X M symmetrix matrix
        Matrix s = new Matrix(M, M);
        int k = 0;
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j <= i; ++j) {
                s.set(i, j, S[k]);
                s.set(j, i, S[k]);
                k++;
            }
        }
        //s.print(6, 4);

        Matrix a = new Matrix(M, N);
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < N; ++j) {
                a.set(i, j, A[i * N + j]);
            }
        }

        //a.print(6, 4);
        Matrix c = s.times(a);

        //c.print(6, 4);
        System.out.println(Arrays.toString(c.getColumnPackedCopy()));
        System.arraycopy(c.getColumnPackedCopy(), 0, C, 0, M);
    }

    static void VMATR(double[] A, double[] G, double[] X, int M, int N) {
        // A,X One-dimensional arrays of length X
        // G Two-dimensional array of dimension (M,N), stored row-wise
        // GA = X
        for (int i = 0; i < M; ++i) {
            X[i] = 0;
            for (int j = 0; j < N; ++j) {
                X[i] = X[i] + G[i + j * M] * A[j];
            }
        }
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
                    } //ENDDO
                    IB = IB + IOB;
                    IC = IC + 1;
                }
            }
            IA = IA + IOA;
        }
    }

    public int getDetId() {
        return IDT;
    }

    public double[] WMAT() {
        return WMAT;
    }

    public double[] WVEC() {
        return WVEC;
    }

    public int NPARF() {
        return NPARF;
    }

    public int[] MPNT() {
        return MPNT;
    }

    public double[] DEROT(int i) {
        return DEROTs.get(i);
    }

    public double[] PARC() {
        return PARC;
    }

    static List<double[]> ROT_DER(double[] ROT) {
        double[][] DA = {
            {0.0, 0.0, 0.0,
                0.0, 0.0, 1.0,
                0.0, -1.0, 0.0},
            {0.0, 0.0, -1.0,
                0.0, 0.0, 0.0,
                1.0, 0.0, 0.0},
            {0.0, 1.0, 0.0,
                -1.0, 0.0, 0.0,
                0.0, 0.0, 0.0}};
        List<double[]> l = new ArrayList<double[]>();
        for (int i = 0; i < 3; ++i) {
            double[] c = new double[9];
//            System.out.println("ROT "+Arrays.toString(ROT));
//            System.out.println("DA("+i+") "+Arrays.toString(DA[i]));

            myMXMPY(ROT, DA[i], c, 3, 3, 3);
//            System.out.println("DROT "+Arrays.toString(c));
            l.add(c);
        }
        return l;
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
}
