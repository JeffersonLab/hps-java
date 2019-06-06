package org.hps.recon.tracking.lit;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CbmLitMatrixMath {

    /*
     * Matrix operations
     */

// SMij are indices for a 5x5 matrix.
    static final int SM00 = 0;
    static final int SM01 = 1;
    static final int SM02 = 2;
    static final int SM03 = 3;
    static final int SM04 = 4;
    static final int SM10 = 1;
    static final int SM11 = 5;
    static final int SM12 = 6;
    static final int SM13 = 7;
    static final int SM14 = 8;
    static final int SM20 = 2;
    static final int SM21 = 6;
    static final int SM22 = 9;
    static final int SM23 = 10;
    static final int SM24 = 11;
    static final int SM30 = 3;
    static final int SM31 = 7;
    static final int SM32 = 10;
    static final int SM33 = 12;
    static final int SM34 = 13;
    static final int SM40 = 4;
    static final int SM41 = 8;
    static final int SM42 = 11;
    static final int SM43 = 13;
    static final int SM44 = 14;

    public static boolean InvSym15(
            double[] a) {
        if (a.length != 15) {
            throw new RuntimeException("-E- InvSym15: size is not correct");
        }

//        double[] pM = a;
        double[] pM = new double[a.length];
        System.arraycopy(a, 0, pM, 0, a.length);

        // Find all NECESSARY 2x2 dets:  (25 of them)
        final double mDet2_23_01 = pM[SM20] * pM[SM31] - pM[SM21] * pM[SM30];
        final double mDet2_23_02 = pM[SM20] * pM[SM32] - pM[SM22] * pM[SM30];
        final double mDet2_23_03 = pM[SM20] * pM[SM33] - pM[SM23] * pM[SM30];
        final double mDet2_23_12 = pM[SM21] * pM[SM32] - pM[SM22] * pM[SM31];
        final double mDet2_23_13 = pM[SM21] * pM[SM33] - pM[SM23] * pM[SM31];
        final double mDet2_23_23 = pM[SM22] * pM[SM33] - pM[SM23] * pM[SM32];
        final double mDet2_24_01 = pM[SM20] * pM[SM41] - pM[SM21] * pM[SM40];
        final double mDet2_24_02 = pM[SM20] * pM[SM42] - pM[SM22] * pM[SM40];
        final double mDet2_24_03 = pM[SM20] * pM[SM43] - pM[SM23] * pM[SM40];
        final double mDet2_24_04 = pM[SM20] * pM[SM44] - pM[SM24] * pM[SM40];
        final double mDet2_24_12 = pM[SM21] * pM[SM42] - pM[SM22] * pM[SM41];
        final double mDet2_24_13 = pM[SM21] * pM[SM43] - pM[SM23] * pM[SM41];
        final double mDet2_24_14 = pM[SM21] * pM[SM44] - pM[SM24] * pM[SM41];
        final double mDet2_24_23 = pM[SM22] * pM[SM43] - pM[SM23] * pM[SM42];
        final double mDet2_24_24 = pM[SM22] * pM[SM44] - pM[SM24] * pM[SM42];
        final double mDet2_34_01 = pM[SM30] * pM[SM41] - pM[SM31] * pM[SM40];
        final double mDet2_34_02 = pM[SM30] * pM[SM42] - pM[SM32] * pM[SM40];
        final double mDet2_34_03 = pM[SM30] * pM[SM43] - pM[SM33] * pM[SM40];
        final double mDet2_34_04 = pM[SM30] * pM[SM44] - pM[SM34] * pM[SM40];
        final double mDet2_34_12 = pM[SM31] * pM[SM42] - pM[SM32] * pM[SM41];
        final double mDet2_34_13 = pM[SM31] * pM[SM43] - pM[SM33] * pM[SM41];
        final double mDet2_34_14 = pM[SM31] * pM[SM44] - pM[SM34] * pM[SM41];
        final double mDet2_34_23 = pM[SM32] * pM[SM43] - pM[SM33] * pM[SM42];
        final double mDet2_34_24 = pM[SM32] * pM[SM44] - pM[SM34] * pM[SM42];
        final double mDet2_34_34 = pM[SM33] * pM[SM44] - pM[SM34] * pM[SM43];

        // Find all NECESSARY 3x3 dets:   (30 of them)
        final double mDet3_123_012 = pM[SM10] * mDet2_23_12 - pM[SM11] * mDet2_23_02 + pM[SM12] * mDet2_23_01;
        final double mDet3_123_013 = pM[SM10] * mDet2_23_13 - pM[SM11] * mDet2_23_03 + pM[SM13] * mDet2_23_01;
        final double mDet3_123_023 = pM[SM10] * mDet2_23_23 - pM[SM12] * mDet2_23_03 + pM[SM13] * mDet2_23_02;
        final double mDet3_123_123 = pM[SM11] * mDet2_23_23 - pM[SM12] * mDet2_23_13 + pM[SM13] * mDet2_23_12;
        final double mDet3_124_012 = pM[SM10] * mDet2_24_12 - pM[SM11] * mDet2_24_02 + pM[SM12] * mDet2_24_01;
        final double mDet3_124_013 = pM[SM10] * mDet2_24_13 - pM[SM11] * mDet2_24_03 + pM[SM13] * mDet2_24_01;
        final double mDet3_124_014 = pM[SM10] * mDet2_24_14 - pM[SM11] * mDet2_24_04 + pM[SM14] * mDet2_24_01;
        final double mDet3_124_023 = pM[SM10] * mDet2_24_23 - pM[SM12] * mDet2_24_03 + pM[SM13] * mDet2_24_02;
        final double mDet3_124_024 = pM[SM10] * mDet2_24_24 - pM[SM12] * mDet2_24_04 + pM[SM14] * mDet2_24_02;
        final double mDet3_124_123 = pM[SM11] * mDet2_24_23 - pM[SM12] * mDet2_24_13 + pM[SM13] * mDet2_24_12;
        final double mDet3_124_124 = pM[SM11] * mDet2_24_24 - pM[SM12] * mDet2_24_14 + pM[SM14] * mDet2_24_12;
        final double mDet3_134_012 = pM[SM10] * mDet2_34_12 - pM[SM11] * mDet2_34_02 + pM[SM12] * mDet2_34_01;
        final double mDet3_134_013 = pM[SM10] * mDet2_34_13 - pM[SM11] * mDet2_34_03 + pM[SM13] * mDet2_34_01;
        final double mDet3_134_014 = pM[SM10] * mDet2_34_14 - pM[SM11] * mDet2_34_04 + pM[SM14] * mDet2_34_01;
        final double mDet3_134_023 = pM[SM10] * mDet2_34_23 - pM[SM12] * mDet2_34_03 + pM[SM13] * mDet2_34_02;
        final double mDet3_134_024 = pM[SM10] * mDet2_34_24 - pM[SM12] * mDet2_34_04 + pM[SM14] * mDet2_34_02;
        final double mDet3_134_034 = pM[SM10] * mDet2_34_34 - pM[SM13] * mDet2_34_04 + pM[SM14] * mDet2_34_03;
        final double mDet3_134_123 = pM[SM11] * mDet2_34_23 - pM[SM12] * mDet2_34_13 + pM[SM13] * mDet2_34_12;
        final double mDet3_134_124 = pM[SM11] * mDet2_34_24 - pM[SM12] * mDet2_34_14 + pM[SM14] * mDet2_34_12;
        final double mDet3_134_134 = pM[SM11] * mDet2_34_34 - pM[SM13] * mDet2_34_14 + pM[SM14] * mDet2_34_13;
        final double mDet3_234_012 = pM[SM20] * mDet2_34_12 - pM[SM21] * mDet2_34_02 + pM[SM22] * mDet2_34_01;
        final double mDet3_234_013 = pM[SM20] * mDet2_34_13 - pM[SM21] * mDet2_34_03 + pM[SM23] * mDet2_34_01;
        final double mDet3_234_014 = pM[SM20] * mDet2_34_14 - pM[SM21] * mDet2_34_04 + pM[SM24] * mDet2_34_01;
        final double mDet3_234_023 = pM[SM20] * mDet2_34_23 - pM[SM22] * mDet2_34_03 + pM[SM23] * mDet2_34_02;
        final double mDet3_234_024 = pM[SM20] * mDet2_34_24 - pM[SM22] * mDet2_34_04 + pM[SM24] * mDet2_34_02;
        final double mDet3_234_034 = pM[SM20] * mDet2_34_34 - pM[SM23] * mDet2_34_04 + pM[SM24] * mDet2_34_03;
        final double mDet3_234_123 = pM[SM21] * mDet2_34_23 - pM[SM22] * mDet2_34_13 + pM[SM23] * mDet2_34_12;
        final double mDet3_234_124 = pM[SM21] * mDet2_34_24 - pM[SM22] * mDet2_34_14 + pM[SM24] * mDet2_34_12;
        final double mDet3_234_134 = pM[SM21] * mDet2_34_34 - pM[SM23] * mDet2_34_14 + pM[SM24] * mDet2_34_13;
        final double mDet3_234_234 = pM[SM22] * mDet2_34_34 - pM[SM23] * mDet2_34_24 + pM[SM24] * mDet2_34_23;

        // Find all NECESSARY 4x4 dets:   (15 of them)
        final double mDet4_0123_0123 = pM[SM00] * mDet3_123_123 - pM[SM01] * mDet3_123_023
                + pM[SM02] * mDet3_123_013 - pM[SM03] * mDet3_123_012;
        final double mDet4_0124_0123 = pM[SM00] * mDet3_124_123 - pM[SM01] * mDet3_124_023
                + pM[SM02] * mDet3_124_013 - pM[SM03] * mDet3_124_012;
        final double mDet4_0124_0124 = pM[SM00] * mDet3_124_124 - pM[SM01] * mDet3_124_024
                + pM[SM02] * mDet3_124_014 - pM[SM04] * mDet3_124_012;
        final double mDet4_0134_0123 = pM[SM00] * mDet3_134_123 - pM[SM01] * mDet3_134_023
                + pM[SM02] * mDet3_134_013 - pM[SM03] * mDet3_134_012;
        final double mDet4_0134_0124 = pM[SM00] * mDet3_134_124 - pM[SM01] * mDet3_134_024
                + pM[SM02] * mDet3_134_014 - pM[SM04] * mDet3_134_012;
        final double mDet4_0134_0134 = pM[SM00] * mDet3_134_134 - pM[SM01] * mDet3_134_034
                + pM[SM03] * mDet3_134_014 - pM[SM04] * mDet3_134_013;
        final double mDet4_0234_0123 = pM[SM00] * mDet3_234_123 - pM[SM01] * mDet3_234_023
                + pM[SM02] * mDet3_234_013 - pM[SM03] * mDet3_234_012;
        final double mDet4_0234_0124 = pM[SM00] * mDet3_234_124 - pM[SM01] * mDet3_234_024
                + pM[SM02] * mDet3_234_014 - pM[SM04] * mDet3_234_012;
        final double mDet4_0234_0134 = pM[SM00] * mDet3_234_134 - pM[SM01] * mDet3_234_034
                + pM[SM03] * mDet3_234_014 - pM[SM04] * mDet3_234_013;
        final double mDet4_0234_0234 = pM[SM00] * mDet3_234_234 - pM[SM02] * mDet3_234_034
                + pM[SM03] * mDet3_234_024 - pM[SM04] * mDet3_234_023;
        final double mDet4_1234_0123 = pM[SM10] * mDet3_234_123 - pM[SM11] * mDet3_234_023
                + pM[SM12] * mDet3_234_013 - pM[SM13] * mDet3_234_012;
        final double mDet4_1234_0124 = pM[SM10] * mDet3_234_124 - pM[SM11] * mDet3_234_024
                + pM[SM12] * mDet3_234_014 - pM[SM14] * mDet3_234_012;
        final double mDet4_1234_0134 = pM[SM10] * mDet3_234_134 - pM[SM11] * mDet3_234_034
                + pM[SM13] * mDet3_234_014 - pM[SM14] * mDet3_234_013;
        final double mDet4_1234_0234 = pM[SM10] * mDet3_234_234 - pM[SM12] * mDet3_234_034
                + pM[SM13] * mDet3_234_024 - pM[SM14] * mDet3_234_023;
        final double mDet4_1234_1234 = pM[SM11] * mDet3_234_234 - pM[SM12] * mDet3_234_134
                + pM[SM13] * mDet3_234_124 - pM[SM14] * mDet3_234_123;

        // Find the 5x5 det:
        final double det = pM[SM00] * mDet4_1234_1234 - pM[SM01] * mDet4_1234_0234 + pM[SM02] * mDet4_1234_0134
                - pM[SM03] * mDet4_1234_0124 + pM[SM04] * mDet4_1234_0123;

        if (det == 0) {
            throw new RuntimeException("-E- InvSym15: zero determinant");
        }

        final double oneOverDet = 1.0 / det;
        final double mn1OverDet = -oneOverDet;

        pM[SM00] = mDet4_1234_1234 * oneOverDet;
        pM[SM01] = mDet4_1234_0234 * mn1OverDet;
        pM[SM02] = mDet4_1234_0134 * oneOverDet;
        pM[SM03] = mDet4_1234_0124 * mn1OverDet;
        pM[SM04] = mDet4_1234_0123 * oneOverDet;

        pM[SM11] = mDet4_0234_0234 * oneOverDet;
        pM[SM12] = mDet4_0234_0134 * mn1OverDet;
        pM[SM13] = mDet4_0234_0124 * oneOverDet;
        pM[SM14] = mDet4_0234_0123 * mn1OverDet;

        pM[SM22] = mDet4_0134_0134 * oneOverDet;
        pM[SM23] = mDet4_0134_0124 * mn1OverDet;
        pM[SM24] = mDet4_0134_0123 * oneOverDet;

        pM[SM33] = mDet4_0124_0124 * oneOverDet;
        pM[SM34] = mDet4_0124_0123 * mn1OverDet;

        pM[SM44] = mDet4_0123_0123 * oneOverDet;

        System.arraycopy(pM, 0, a, 0, a.length);
        return true;
    }

    public static boolean Mult25(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 25 || b.length != 25 || c.length != 25) {
            throw new RuntimeException("-E- Mult25: size is not correct");
        }

        c[0] = a[0] * b[0] + a[1] * b[5] + a[2] * b[10] + a[3] * b[15] + a[4] * b[20];
        c[1] = a[0] * b[1] + a[1] * b[6] + a[2] * b[11] + a[3] * b[16] + a[4] * b[21];
        c[2] = a[0] * b[2] + a[1] * b[7] + a[2] * b[12] + a[3] * b[17] + a[4] * b[22];
        c[3] = a[0] * b[3] + a[1] * b[8] + a[2] * b[13] + a[3] * b[18] + a[4] * b[23];
        c[4] = a[0] * b[4] + a[1] * b[9] + a[2] * b[14] + a[3] * b[19] + a[4] * b[24];
        c[5] = a[5] * b[0] + a[6] * b[5] + a[7] * b[10] + a[8] * b[15] + a[9] * b[20];
        c[6] = a[5] * b[1] + a[6] * b[6] + a[7] * b[11] + a[8] * b[16] + a[9] * b[21];
        c[7] = a[5] * b[2] + a[6] * b[7] + a[7] * b[12] + a[8] * b[17] + a[9] * b[22];
        c[8] = a[5] * b[3] + a[6] * b[8] + a[7] * b[13] + a[8] * b[18] + a[9] * b[23];
        c[9] = a[5] * b[4] + a[6] * b[9] + a[7] * b[14] + a[8] * b[19] + a[9] * b[24];
        c[10] = a[10] * b[0] + a[11] * b[5] + a[12] * b[10] + a[13] * b[15] + a[14] * b[20];
        c[11] = a[10] * b[1] + a[11] * b[6] + a[12] * b[11] + a[13] * b[16] + a[14] * b[21];
        c[12] = a[10] * b[2] + a[11] * b[7] + a[12] * b[12] + a[13] * b[17] + a[14] * b[22];
        c[13] = a[10] * b[3] + a[11] * b[8] + a[12] * b[13] + a[13] * b[18] + a[14] * b[23];
        c[14] = a[10] * b[4] + a[11] * b[9] + a[12] * b[14] + a[13] * b[19] + a[14] * b[24];
        c[15] = a[15] * b[0] + a[16] * b[5] + a[17] * b[10] + a[18] * b[15] + a[19] * b[20];
        c[16] = a[15] * b[1] + a[16] * b[6] + a[17] * b[11] + a[18] * b[16] + a[19] * b[21];
        c[17] = a[15] * b[2] + a[16] * b[7] + a[17] * b[12] + a[18] * b[17] + a[19] * b[22];
        c[18] = a[15] * b[3] + a[16] * b[8] + a[17] * b[13] + a[18] * b[18] + a[19] * b[23];
        c[19] = a[15] * b[4] + a[16] * b[9] + a[17] * b[14] + a[18] * b[19] + a[19] * b[24];
        c[20] = a[20] * b[0] + a[21] * b[5] + a[22] * b[10] + a[23] * b[15] + a[24] * b[20];
        c[21] = a[20] * b[1] + a[21] * b[6] + a[22] * b[11] + a[23] * b[16] + a[24] * b[21];
        c[22] = a[20] * b[2] + a[21] * b[7] + a[22] * b[12] + a[23] * b[17] + a[24] * b[22];
        c[23] = a[20] * b[3] + a[21] * b[8] + a[22] * b[13] + a[23] * b[18] + a[24] * b[23];
        c[24] = a[20] * b[4] + a[21] * b[9] + a[22] * b[14] + a[23] * b[19] + a[24] * b[24];

        return true;
    }

    public static boolean Transpose25(
            double[] a) {
        if (a.length != 25) {
            throw new RuntimeException("-E- Transpose25: size is not correct");
        }
        double[] b = new double[a.length];
        System.arraycopy(a, 0, b, 0, a.length);
        a[0] = b[0];
        a[1] = b[5];
        a[2] = b[10];
        a[3] = b[15];
        a[4] = b[20];
        a[5] = b[1];
        a[6] = b[6];
        a[7] = b[11];
        a[8] = b[16];
        a[9] = b[21];
        a[10] = b[2];
        a[11] = b[7];
        a[12] = b[12];
        a[13] = b[17];
        a[14] = b[22];
        a[15] = b[3];
        a[16] = b[8];
        a[17] = b[13];
        a[18] = b[18];
        a[19] = b[23];
        a[20] = b[4];
        a[21] = b[9];
        a[22] = b[14];
        a[23] = b[19];
        a[24] = b[24];
        return true;
    }

    public static boolean Mult25On5(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 25 || b.length != 5 || c.length != 5) {
            throw new RuntimeException("-E- Mult25On5: size is not correct");
        }
        c[0] = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] + a[4] * b[4];
        c[1] = a[5] * b[0] + a[6] * b[1] + a[7] * b[2] + a[8] * b[3] + a[9] * b[4];
        c[2] = a[10] * b[0] + a[11] * b[1] + a[12] * b[2] + a[13] * b[3] + a[14] * b[4];
        c[3] = a[15] * b[0] + a[16] * b[1] + a[17] * b[2] + a[18] * b[3] + a[19] * b[4];
        c[4] = a[20] * b[0] + a[21] * b[1] + a[22] * b[2] + a[23] * b[3] + a[24] * b[4];
        return true;
    }

    public static boolean Mult15On5(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 15 || b.length != 5 || c.length != 5) {
            throw new RuntimeException("-E- Mult15On5: size is not correct");
        }
        c[0] = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] + a[4] * b[4];
        c[1] = a[1] * b[0] + a[5] * b[1] + a[6] * b[2] + a[7] * b[3] + a[8] * b[4];
        c[2] = a[2] * b[0] + a[6] * b[1] + a[9] * b[2] + a[10] * b[3] + a[11] * b[4];
        c[3] = a[3] * b[0] + a[7] * b[1] + a[10] * b[2] + a[12] * b[3] + a[13] * b[4];
        c[4] = a[4] * b[0] + a[8] * b[1] + a[11] * b[2] + a[13] * b[3] + a[14] * b[4];
        return true;
    }

    public static boolean Subtract(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != b.length || a.length != c.length) {
            throw new RuntimeException("-E- Subtract: size is not correct");

        }
        for (int i = 0; i < a.length; ++i) {
            c[i] = a[i] - b[i];
        }
        return true;
    }

    public static boolean Add(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != b.length || a.length != c.length) {
            throw new RuntimeException("-E- Add: size is not correct");

        }
        for (int i = 0; i < a.length; ++i) {
            c[i] = a[i] + b[i];
        }
        return true;
    }


    /*
     * a*b*a^T
     */
    public static boolean Similarity(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 25 || b.length != 15 || c.length != 15) {
            throw new RuntimeException("-E- Similarity: size is not correct");

        }

        double A = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] + a[4] * b[4];
        double B = a[0] * b[1] + a[1] * b[5] + a[2] * b[6] + a[3] * b[7] + a[4] * b[8];
        double C = a[0] * b[2] + a[1] * b[6] + a[2] * b[9] + a[3] * b[10] + a[4] * b[11];
        double D = a[0] * b[3] + a[1] * b[7] + a[2] * b[10] + a[3] * b[12] + a[4] * b[13];
        double E = a[0] * b[4] + a[1] * b[8] + a[2] * b[11] + a[3] * b[13] + a[4] * b[14];

        double F = a[5] * b[0] + a[6] * b[1] + a[7] * b[2] + a[8] * b[3] + a[9] * b[4];
        double G = a[5] * b[1] + a[6] * b[5] + a[7] * b[6] + a[8] * b[7] + a[9] * b[8];
        double H = a[5] * b[2] + a[6] * b[6] + a[7] * b[9] + a[8] * b[10] + a[9] * b[11];
        double I = a[5] * b[3] + a[6] * b[7] + a[7] * b[10] + a[8] * b[12] + a[9] * b[13];
        double J = a[5] * b[4] + a[6] * b[8] + a[7] * b[11] + a[8] * b[13] + a[9] * b[14];

        double K = a[10] * b[0] + a[11] * b[1] + a[12] * b[2] + a[13] * b[3] + a[14] * b[4];
        double L = a[10] * b[1] + a[11] * b[5] + a[12] * b[6] + a[13] * b[7] + a[14] * b[8];
        double M = a[10] * b[2] + a[11] * b[6] + a[12] * b[9] + a[13] * b[10] + a[14] * b[11];
        double N = a[10] * b[3] + a[11] * b[7] + a[12] * b[10] + a[13] * b[12] + a[14] * b[13];
        double O = a[10] * b[4] + a[11] * b[8] + a[12] * b[11] + a[13] * b[13] + a[14] * b[14];

        double P = a[15] * b[0] + a[16] * b[1] + a[17] * b[2] + a[18] * b[3] + a[19] * b[4];
        double Q = a[15] * b[1] + a[16] * b[5] + a[17] * b[6] + a[18] * b[7] + a[19] * b[8];
        double R = a[15] * b[2] + a[16] * b[6] + a[17] * b[9] + a[18] * b[10] + a[19] * b[11];
        double S = a[15] * b[3] + a[16] * b[7] + a[17] * b[10] + a[18] * b[12] + a[19] * b[13];
        double T = a[15] * b[4] + a[16] * b[8] + a[17] * b[11] + a[18] * b[13] + a[19] * b[14];

        c[0] = A * a[0] + B * a[1] + C * a[2] + D * a[3] + E * a[4];
        c[1] = A * a[5] + B * a[6] + C * a[7] + D * a[8] + E * a[9];
        c[2] = A * a[10] + B * a[11] + C * a[12] + D * a[13] + E * a[14];
        c[3] = A * a[15] + B * a[16] + C * a[17] + D * a[18] + E * a[19];
        c[4] = A * a[20] + B * a[21] + C * a[22] + D * a[23] + E * a[24];

        c[5] = F * a[5] + G * a[6] + H * a[7] + I * a[8] + J * a[9];
        c[6] = F * a[10] + G * a[11] + H * a[12] + I * a[13] + J * a[14];
        c[7] = F * a[15] + G * a[16] + H * a[17] + I * a[18] + J * a[19];
        c[8] = F * a[20] + G * a[21] + H * a[22] + I * a[23] + J * a[24];

        c[9] = K * a[10] + L * a[11] + M * a[12] + N * a[13] + O * a[14];
        c[10] = K * a[15] + L * a[16] + M * a[17] + N * a[18] + O * a[19];
        c[11] = K * a[20] + L * a[21] + M * a[22] + N * a[23] + O * a[24];

        c[12] = P * a[15] + Q * a[16] + R * a[17] + S * a[18] + T * a[19];
        c[13] = P * a[20] + Q * a[21] + R * a[22] + S * a[23] + T * a[24];

        c[14] = (a[20] * b[0] + a[21] * b[1] + a[22] * b[2] + a[23] * b[3] + a[24] * b[4]) * a[20]
                + (a[20] * b[1] + a[21] * b[5] + a[22] * b[6] + a[23] * b[7] + a[24] * b[8]) * a[21]
                + (a[20] * b[2] + a[21] * b[6] + a[22] * b[9] + a[23] * b[10] + a[24] * b[11]) * a[22]
                + (a[20] * b[3] + a[21] * b[7] + a[22] * b[10] + a[23] * b[12] + a[24] * b[13]) * a[23]
                + (a[20] * b[4] + a[21] * b[8] + a[22] * b[11] + a[23] * b[13] + a[24] * b[14]) * a[24];
        return true;
    }

    public static boolean Mult15On25(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 15 || b.length != 25 || c.length != 25) {
            throw new RuntimeException("-E- Mult15On25: size is not correct");

        }
        c[0] = a[0] * b[0] + a[1] * b[5] + a[2] * b[10] + a[3] * b[15] + a[4] * b[20];
        c[1] = a[0] * b[1] + a[1] * b[6] + a[2] * b[11] + a[3] * b[16] + a[4] * b[21];
        c[2] = a[0] * b[2] + a[1] * b[7] + a[2] * b[12] + a[3] * b[17] + a[4] * b[22];
        c[3] = a[0] * b[3] + a[1] * b[8] + a[2] * b[13] + a[3] * b[18] + a[4] * b[23];
        c[4] = a[0] * b[4] + a[1] * b[9] + a[2] * b[14] + a[3] * b[19] + a[4] * b[24];
        c[5] = a[1] * b[0] + a[5] * b[5] + a[6] * b[10] + a[7] * b[15] + a[8] * b[20];
        c[6] = a[1] * b[1] + a[5] * b[6] + a[6] * b[11] + a[7] * b[16] + a[8] * b[21];
        c[7] = a[1] * b[2] + a[5] * b[7] + a[6] * b[12] + a[7] * b[17] + a[8] * b[22];
        c[8] = a[1] * b[3] + a[5] * b[8] + a[6] * b[13] + a[7] * b[18] + a[8] * b[23];
        c[9] = a[1] * b[4] + a[5] * b[9] + a[6] * b[14] + a[7] * b[19] + a[8] * b[24];
        c[10] = a[2] * b[0] + a[6] * b[5] + a[9] * b[10] + a[10] * b[15] + a[11] * b[20];
        c[11] = a[2] * b[1] + a[6] * b[6] + a[9] * b[11] + a[10] * b[16] + a[11] * b[21];
        c[12] = a[2] * b[2] + a[6] * b[7] + a[9] * b[12] + a[10] * b[17] + a[11] * b[22];
        c[13] = a[2] * b[3] + a[6] * b[8] + a[9] * b[13] + a[10] * b[18] + a[11] * b[23];
        c[14] = a[2] * b[4] + a[6] * b[9] + a[9] * b[14] + a[10] * b[19] + a[11] * b[24];
        c[15] = a[3] * b[0] + a[7] * b[5] + a[10] * b[10] + a[12] * b[15] + a[13] * b[20];
        c[16] = a[3] * b[1] + a[7] * b[6] + a[10] * b[11] + a[12] * b[16] + a[13] * b[21];
        c[17] = a[3] * b[2] + a[7] * b[7] + a[10] * b[12] + a[12] * b[17] + a[13] * b[22];
        c[18] = a[3] * b[3] + a[7] * b[8] + a[10] * b[13] + a[12] * b[18] + a[13] * b[23];
        c[19] = a[3] * b[4] + a[7] * b[9] + a[10] * b[14] + a[12] * b[19] + a[13] * b[24];
        c[20] = a[4] * b[0] + a[8] * b[5] + a[11] * b[10] + a[13] * b[15] + a[14] * b[20];
        c[21] = a[4] * b[1] + a[8] * b[6] + a[11] * b[11] + a[13] * b[16] + a[14] * b[21];
        c[22] = a[4] * b[2] + a[8] * b[7] + a[11] * b[12] + a[13] * b[17] + a[14] * b[22];
        c[23] = a[4] * b[3] + a[8] * b[8] + a[11] * b[13] + a[13] * b[18] + a[14] * b[23];
        c[24] = a[4] * b[4] + a[8] * b[9] + a[11] * b[14] + a[13] * b[19] + a[14] * b[24];

        return true;
    }

    public static boolean Mult25On15(
            double[] a,
            double[] b,
            double[] c) {
        if (a.length != 25 || b.length != 15 || c.length != 25) {
            throw new RuntimeException("-E- Mult15On25: size is not correct");

        }
        c[0] = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3] + a[4] * b[4];
        c[1] = a[0] * b[1] + a[1] * b[5] + a[2] * b[6] + a[3] * b[7] + a[4] * b[8];
        c[2] = a[0] * b[2] + a[1] * b[6] + a[2] * b[9] + a[3] * b[10] + a[4] * b[11];
        c[3] = a[0] * b[3] + a[1] * b[7] + a[2] * b[10] + a[3] * b[12] + a[4] * b[13];
        c[4] = a[0] * b[4] + a[1] * b[8] + a[2] * b[11] + a[3] * b[13] + a[4] * b[14];
        c[5] = a[5] * b[0] + a[6] * b[1] + a[7] * b[2] + a[8] * b[3] + a[9] * b[4];
        c[6] = a[5] * b[1] + a[6] * b[5] + a[7] * b[6] + a[8] * b[7] + a[9] * b[8];
        c[7] = a[5] * b[2] + a[6] * b[6] + a[7] * b[9] + a[8] * b[10] + a[9] * b[11];
        c[8] = a[5] * b[3] + a[6] * b[7] + a[7] * b[10] + a[8] * b[12] + a[9] * b[13];
        c[9] = a[5] * b[4] + a[6] * b[8] + a[7] * b[11] + a[8] * b[13] + a[9] * b[14];
        c[10] = a[10] * b[0] + a[11] * b[1] + a[12] * b[2] + a[13] * b[3] + a[14] * b[4];
        c[11] = a[10] * b[1] + a[11] * b[5] + a[12] * b[6] + a[13] * b[7] + a[14] * b[8];
        c[12] = a[10] * b[2] + a[11] * b[6] + a[12] * b[9] + a[13] * b[10] + a[14] * b[11];
        c[13] = a[10] * b[3] + a[11] * b[7] + a[12] * b[10] + a[13] * b[12] + a[14] * b[13];
        c[14] = a[10] * b[4] + a[11] * b[8] + a[12] * b[11] + a[13] * b[13] + a[14] * b[14];
        c[15] = a[15] * b[0] + a[16] * b[1] + a[17] * b[2] + a[18] * b[3] + a[19] * b[4];
        c[16] = a[15] * b[1] + a[16] * b[5] + a[17] * b[6] + a[18] * b[7] + a[19] * b[8];
        c[17] = a[15] * b[2] + a[16] * b[6] + a[17] * b[9] + a[18] * b[10] + a[19] * b[11];
        c[18] = a[15] * b[3] + a[16] * b[7] + a[17] * b[10] + a[18] * b[12] + a[19] * b[13];
        c[19] = a[15] * b[4] + a[16] * b[8] + a[17] * b[11] + a[18] * b[13] + a[19] * b[14];
        c[20] = a[20] * b[0] + a[21] * b[1] + a[22] * b[2] + a[23] * b[3] + a[24] * b[4];
        c[21] = a[20] * b[1] + a[21] * b[5] + a[22] * b[6] + a[23] * b[7] + a[24] * b[8];
        c[22] = a[20] * b[2] + a[21] * b[6] + a[22] * b[9] + a[23] * b[10] + a[24] * b[11];
        c[23] = a[20] * b[3] + a[21] * b[7] + a[22] * b[10] + a[23] * b[12] + a[24] * b[13];
        c[24] = a[20] * b[4] + a[21] * b[8] + a[22] * b[11] + a[23] * b[13] + a[24] * b[14];

        return true;
    }
}
