package kalman;

public class Measurement {  // Holds a single silicon-strip measurement (single-sided), to interface with the Kalman fit
	Plane p;                // Orientation and offset of the measurement plane in global coordinates
	double vTrue;           // MC truth measurement value
	Vec rGlobal;            // Global MC truth
	double v;               // Measurement value in detector frame
	double sigma;           // Measurement uncertainty
	private RotMatrix R;    // Rotation from the detector coordinates to global coordinates (not field coordinates)
	RotMatrix Rinv;         // Rotation from global (not field) coordinates to detector coordinates (transpose of R)
	double stereo;          // Stereo angle of the detectors
	double thickness;       // Silicon thickness in mm
	double B;               // Magnetic field magnitude
	Vec t;                  // Magnetic field direction (should be not far from the z axis direction)

	public Measurement(Plane plane, Vec rGlobal, double B, Vec t, double value, double resolution, double stereo, double thickness) {
		this.B = B;
		p = plane.copy();
		v = value;
		sigma = resolution;
		this.stereo = stereo;
		RotMatrix R1 = new RotMatrix(p.U(), p.V(), p.T());
		RotMatrix R2 = new RotMatrix(stereo);      // Rotation by stereo angle in detector plane
		Rinv = R2.multiply(R1);
		R = Rinv.invert();
		this.rGlobal = rGlobal.copy();
		Vec rLocal = Rinv.rotate(rGlobal.dif(p.X()));
		vTrue = rLocal.v[1];
		this.thickness = thickness;
		this.t = t.copy();
	}
	
	public void print(String s) {
		System.out.format("Measurement instance %s, stereo angle=%9.4f, thickness=%9.4f:\n", s, stereo, thickness);
		rGlobal.print("global location from MC truth");
		p.print("of measurement");
		System.out.format("Measurement %s B field=%10.7f, Measurement value=%10.6f+-%10.6f;  MC truth=%10.6f\n", s, B, v, sigma, vTrue);
		t.print("B field direction");
		R.print("from detector coordinates to global coordinates.");
		measurementGlobal().print("global location of measurement, using MC truth for unmeasured coordinate");		
	}

	public Vec toGlobal(Vec vLocal) {  // Convert a vector from local detector coordinates to global coordinates
		return p.X().sum(R.rotate(vLocal));
	}
	
	public Vec toLocal(Vec vGlobal) {
		return Rinv.rotate(vGlobal.dif(p.X()));
	}
	
	public Vec measurementGlobal() {    // Convert the measurement to global coordinates, using MC truth for the unmeasured coordinate
		Vec rLocal = R.invert().rotate(rGlobal.dif(p.X()));
		//rLocal.print("local MC truth coordinate");
		return toGlobal(new Vec(rLocal.v[0],v,0.));
	}
}
