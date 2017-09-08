package kalman;

public class LinearHelixFit { // Simultaneous fit to a line in the non-bending plane and a parabola in the bending plane

	private SquareMatrix C;    // Covariance matrix of solution
	private Vec a;             // Solution vector (line coefficients followed by parabola coefficients
	private double chi2;
	
	public LinearHelixFit(int N, double [] y, double [] v, double [] s, double [] theta, boolean verbose) {
		SquareMatrix A = new SquareMatrix(5);
		Vec B = new Vec(5);
		
		for (int i=0; i<N; i++) {
			double w = 1.0/(s[i]*s[i]);
			double ct = Math.cos(theta[i]);
			double st = Math.sin(theta[i]);
			A.M[0][0] += w*ct*ct;
			A.M[0][1] += w*y[i]*ct*ct;
			A.M[0][2] += w*st*ct;
			A.M[0][3] += w*y[i]*ct*st;
			A.M[0][4] += w*y[i]*y[i]*ct*st;
			A.M[1][1] += w*y[i]*y[i]*ct*ct;
			A.M[1][4] += w*y[i]*y[i]*y[i]*ct*st;					
			A.M[2][2] += w*st*st;
			A.M[2][3] += w*y[i]*st*st;
			A.M[2][4] += w*y[i]*y[i]*st*st;
			A.M[3][4] += w*y[i]*y[i]*y[i]*st*st;
			A.M[4][4] += w*y[i]*y[i]*y[i]*y[i]*st*st;
			B.v[0] += v[i]*ct*w;
			B.v[1] += v[i]*y[i]*ct*w;
			B.v[2] += v[i]*st*w;
			B.v[3] += v[i]*y[i]*st*w;
			B.v[4] += v[i]*y[i]*y[i]*st*w;			
		}
		A.M[1][0] = A.M[0][1];
		A.M[1][2] = A.M[0][3];
		A.M[1][3] = A.M[0][4];
		A.M[2][0] = A.M[0][2];
		A.M[2][1] = A.M[1][2];
		A.M[3][0] = A.M[0][3];
		A.M[3][1] = A.M[1][3];
		A.M[3][2] = A.M[2][3];
		A.M[3][3] = A.M[2][4];
		A.M[4][0] = A.M[0][4];
		A.M[4][1] = A.M[1][4];
		A.M[4][2] = A.M[2][4];
		A.M[4][3] = A.M[3][4];
		
		if (verbose) A.print("LinearHelixFit");
		C = A.invert();
		a = B.leftMultiply(C);
		
        chi2 = 0.;
        for (int i=0; i<N; i++) {
    		double vPred = evaluateLine(y[i])*Math.cos(theta[i]) + evaluateParabola(y[i])*Math.sin(theta[i]);
    		double err = (v[i] - vPred)/s[i];
        	chi2 += err*err;
		}
	}
	
	void print(int N, double [] y, double [] v, double [] s, double [] theta) {
		System.out.format("LinearHelixFit: parabola a=%10.7f   b=%10.7f   c=%10.7f\n", a.v[2], a.v[3], a.v[4]);
		System.out.format("LinearHelixFit:     line a=%10.7f   b=%10.7f\n", a.v[0], a.v[1]);
		C.print("LinearHelixFit covariance");
		System.out.format("LinearHelixFit: i   x          y       y predicted     residual     sigmas       chi^2=%8.3f\n", chi2);
		for (int i=0; i<N; i++) {
			double vPred = evaluateLine(y[i])*Math.cos(theta[i]) + evaluateParabola(y[i])*Math.sin(theta[i]);
			System.out.format("        %d   %10.7f   %10.7f   %10.7f   %10.7f   %10.7f\n", i, y[i], v[i], vPred, v[i]-vPred, (v[i]-vPred)/s[i]);
		}
	}
	
	SquareMatrix covariance() {
		return C;
	}
	
    Vec solution() {
    	return a;
    }
    
    double evaluateLine(double y) {
    	return a.v[0] + a.v[1]*y;
    }
    
    double evaluateParabola(double y) {
       	return a.v[2] + (a.v[3] + a.v[4]*y)*y;    	
    }
    
    double chiSquared() {
    	return chi2;
    }
}
