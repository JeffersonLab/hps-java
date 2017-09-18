package kalman;

public class FieldMap {
	private double B;
	String FileName;
	int nX, nY, nZ;
	double [][][] bX, bY, bZ;

	public FieldMap(String FileName) {
		B = 1.0;
		this.FileName = FileName;
		// Open and read the text file into the internal arrays
	}
	
	Vec getField(Vec r) {
		return new Vec(0.,0.,B);
	}

}
