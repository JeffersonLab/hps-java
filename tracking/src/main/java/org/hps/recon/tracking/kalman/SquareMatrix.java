package kalman;

class SquareMatrix {    // Simple matrix package strictly for N by N matrices needed by the Kalman fitter
	double [][] M = null;
	int N;
	
	SquareMatrix(int N) {
		M = new double [N][N];
		this.N = N;
	}
	
	SquareMatrix(int N, double [][] m) {
		M = new double [N][N];
		this.N = N;
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				M[i][j] = m[i][j];
			}
		}
	}

	SquareMatrix unit(int N) {            // Create and return a unit matrix
		SquareMatrix R = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			R.M[i][i] = 1.0;
		}
		return R;
	}
	
	void scale(double f) {   // Multiply all matrix elements by a scalar
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				M[i][j] *= f;
			}
		}
	}
	
	SquareMatrix multiply(SquareMatrix M2) {  // Standard matrix multiplication
		SquareMatrix Mp = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				for (int k=0; k<N; k++) {
					Mp.M[i][j] += M[i][k]*M2.M[k][j];
				}
			}
		}
		return Mp;
	}
	
	SquareMatrix sum(SquareMatrix m2) {  // Add two matrices
		SquareMatrix Ms = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				Ms.M[i][j] = m2.M[i][j] + M[i][j]; 
			}
		}
		return Ms;		
	}
	
	SquareMatrix dif(SquareMatrix m2) {  // Subtract two matrices
		SquareMatrix Ms = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				Ms.M[i][j] = M[i][j] - m2.M[i][j]; 
			}
		}
		return Ms;		
	}
	
	SquareMatrix transpose() {
		SquareMatrix Mt = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				Mt.M[i][j] = M[j][i]; 
			}
		}
		return Mt;
	}
	
	SquareMatrix similarity(SquareMatrix F) {  // Similarity transform by matrix F
		SquareMatrix Mp = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				for (int m=0; m<N; m++) {
					for (int n=0; n<N; n++) {
						Mp.M[i][j] += F.M[i][m]*M[m][n]*F.M[j][n];
					}
				}
			}
		}
		return Mp;
	}
	
	void print(String s) {
		System.out.format("Printout of matrix %s  %d\n", s, N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				System.out.format("  %12.4e", M[i][j]);
			}
			System.out.format("\n");
		}
	}
	
	SquareMatrix copy() {
		SquareMatrix Mc = new SquareMatrix(N);
		for (int i=0; i<N; i++) {
			for (int j=0; j<N; j++) {
				Mc.M[i][j] = M[i][j];
			}
		}
		Mc.N = N;
		return Mc;
	}
	
	SquareMatrix invert() {  // Inversion algorithm copied from "Numerical Methods in C"
		SquareMatrix Minv = copy();   // Creates a new matrix; does not overwrite the original one
		
		int icol = 0, irow = 0;

		int [] indxc = new int [N];
		int [] indxr = new int [N];
		int [] ipiv = new int [N];
		for (int i=0; i<N; i++) {
			double big = 0.0;
			for (int j=0; j<N; j++) {
				if (ipiv[j] != 1) {
					for (int k=0; k<N; k++) {
						if (ipiv[k] == 0) {
							if (Math.abs(Minv.M[j][k]) >= big) {
								big = Math.abs(Minv.M[j][k]);
								irow = j;
								icol = k;
							}
						}
					}
				}
					
			}
			++(ipiv[icol]);
			if (irow != icol) {
				for (int l=0; l<N; l++) {
					double c = Minv.M[irow][l];
					Minv.M[irow][l] = Minv.M[icol][l];
					Minv.M[icol][l] = c;
				}
			}
			indxr[i] = irow;
			indxc[i] = icol;
			if (Minv.M[icol][icol] == 0.0) {
				System.out.format("Singular matrix in SquareMatrix.java method invert.\n");
				return Minv;
			}
			double pivinv = 1.0/Minv.M[icol][icol];
			Minv.M[icol][icol] = 1.0;
			for (int l=0; l<N; l++) {
				Minv.M[icol][l] *= pivinv;
			}
			for (int ll=0; ll<N; ll++) {
				if (ll != icol) {
					double dum = Minv.M[ll][icol];
					Minv.M[ll][icol] = 0.0;
					for (int l=0; l<N; l++) {
						Minv.M[ll][l] -= Minv.M[icol][l]*dum;
					}
				}
			}
		}
		for (int l=N-1; l>=0; l--) {
			if (indxr[l] != indxc[l]) {
				for (int k=0; k<N; k++) {
					double dum = Minv.M[k][indxr[l]];
					Minv.M[k][indxr[l]] = Minv.M[k][indxc[l]];
					Minv.M[k][indxc[l]] = dum;
				}
			}
		}
		
		return Minv;
	}
}
