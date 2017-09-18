package kalman;

class Measurement {  // Holds a single silicon-strip measurement (single-sided), to interface with the Kalman fit
	double v;               // Measurement value in detector frame
	double sigma;           // Measurement uncertainty
	double vTrue;           // MC truth measurement value
	Vec rGlobal;            // Global MC truth

	Measurement(double value, double resolution, Vec rGlobal, double vTrue) {
		v = value;
		sigma = resolution;
		this.rGlobal = rGlobal;
		this.vTrue = vTrue;
	}
	
	void print(String s) {
		System.out.format("Measurement %s: Measurement value=%10.6f+-%10.6f;  MC truth=%10.6f\n", s, v, sigma, vTrue);	
		rGlobal.print("global location from MC truth");
	}
}
