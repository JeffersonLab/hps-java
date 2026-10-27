package org.hps.recon.vertexing;

import java.util.ArrayList;
import java.util.List;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.Matrix;
import hep.physics.matrix.SymmetricMatrix;

import junit.framework.TestCase;

import org.lcsim.fit.helicaltrack.HelicalTrackFit;

/**
 * Sanity checks on the vertex-momentum cross-covariance exposed by BilliorVertexer/BilliorVertex,
 * used to build a full (non-block-diagonal) 6x6 joint covariance for a V0 candidate.
 */
public class BilliorVertexerTest extends TestCase {

    private static final double B_FIELD = 0.5;

    public void testVertexMomentumJointCovarianceIsSymmetricPSD() {
        BilliorTrack track1 = makeTrack(0.05, 0.15, 0.0012, 0.02, 0.06);
        BilliorTrack track2 = makeTrack(-0.04, -0.12, -0.0011, -0.01, -0.05);

        List<BilliorTrack> tracks = new ArrayList<BilliorTrack>();
        tracks.add(track1);
        tracks.add(track2);

        BilliorVertexer vertexer = new BilliorVertexer(B_FIELD);
        vertexer.doBeamSpotConstraint(false);
        BilliorVertex vertex = vertexer.fitVertex(tracks);

        Matrix covVV = vertex.getCovMatrix();
        List<Matrix> covTrkMom = vertex.getFittedMomentumCovariance();
        Matrix covPP = MatrixOpAdd(MatrixOpAdd(covTrkMom.get(0), covTrkMom.get(1)),
                MatrixOpAdd(covTrkMom.get(2), transposed(covTrkMom.get(2))));
        Matrix covVP = vertex.getVertexV0MomentumCovariance();

        assertNotNull(covVV);
        assertNotNull(covVP);
        assertEquals(3, covVP.getNRows());
        assertEquals(3, covVP.getNColumns());

        double[][] joint = new double[6][6];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                joint[i][j] = covVV.e(i, j);
                joint[3 + i][3 + j] = covPP.e(i, j);
                joint[i][3 + j] = covVP.e(i, j);
                joint[3 + j][i] = covVP.e(i, j);
            }
        }

        // symmetric by construction; check numerically anyway
        for (int i = 0; i < 6; i++)
            for (int j = 0; j < 6; j++)
                assertEquals("joint covariance not symmetric at (" + i + "," + j + ")",
                        joint[i][j], joint[j][i], 1e-12 * (1 + Math.abs(joint[i][j])));

        assertTrue("assembled 6x6 vertex-momentum joint covariance is not PSD", isPositiveSemiDefinite(joint, 1e-9));
    }

    private static BilliorTrack makeTrack(double dca, double phi0, double curvature, double z0, double slope) {
        double[] par = {dca, phi0, curvature, z0, slope};
        SymmetricMatrix cov = new SymmetricMatrix(5);
        cov.setElement(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex, 1e-6);
        cov.setElement(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index, 1e-7);
        cov.setElement(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex, 1e-10);
        cov.setElement(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index, 1e-6);
        cov.setElement(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex, 1e-7);
        HelicalTrackFit htf = new HelicalTrackFit(par, cov, new double[2], new int[2], null, null);
        return new BilliorTrack(htf);
    }

    private static Matrix MatrixOpAdd(Matrix a, Matrix b) {
        return hep.physics.matrix.MatrixOp.add(a, b);
    }

    private static Matrix transposed(Matrix a) {
        return hep.physics.matrix.MatrixOp.transposed(a);
    }

    /**
     * Attempts an LDL^T (Cholesky-like) decomposition of a symmetric matrix; the matrix is PSD
     * iff no pivot is negative beyond tolerance.
     */
    private static boolean isPositiveSemiDefinite(double[][] m, double tol) {
        int n = m.length;
        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++)
            a[i] = m[i].clone();
        for (int k = 0; k < n; k++) {
            if (a[k][k] < -tol)
                return false;
            if (a[k][k] < tol)
                continue;
            for (int i = k + 1; i < n; i++) {
                double factor = a[i][k] / a[k][k];
                for (int j = k; j < n; j++)
                    a[i][j] -= factor * a[k][j];
            }
        }
        return true;
    }
}
