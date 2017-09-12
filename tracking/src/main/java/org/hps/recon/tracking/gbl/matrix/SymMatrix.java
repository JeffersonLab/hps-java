package org.hps.recon.tracking.gbl.matrix;

/**
 * @author Norman A Graf
 * @version $Id:
 */
public class SymMatrix extends Matrix implements java.io.Serializable {

    public SymMatrix(int n) {
        super(n, n);
    }

    public SymMatrix(SymMatrix smat) {
        super(smat.getRowDimension(), smat.getColumnDimension());
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = smat.get(i, j);
            }
        }
    }

    public SymMatrix(Matrix mat) {
        super(mat.getRowDimension(), mat.getColumnDimension());
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = mat.get(i, j);
            }
        }
    }

    @Override
    public void set(int i, int j, double s) {
        super.set(i, j, s);
        super.set(j, i, s);
    }

    public void setToIdentity() {
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = (i == j ? 1.0 : 0.0);
            }
        }
    }

    // Calculate B * (this) * B^T , final matrix will be (nrowsb x nrowsb)
    // TODO see if this needs to be made more efficient
    public SymMatrix Similarity(Matrix b) {
        SymMatrix X = new SymMatrix(b.times(this.times(b.transpose())));
        return X;
    }
}
