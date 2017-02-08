package org.hps.detector.ecal;


/**
 * Calculate G4 position for a crystal, by it's front and back face coordinates calculates the position for the center
 * of the crystal calculates the rotation of the crystal in convention of Tait-Bryan angles, {phi, theta, psi} 
 * 
 * phi-rotation around X
 * theta-rotation around Y
 * psi-rotation around Z
 */
public final class Geant4Position {
    
    /* center coordinates */
    private GVector centerPoint; 
    /* crystal vector formed by front face center point and back face center point*/
    private GVector crysvec;
    /* { phi,theta,psi=0}; Tait-Bryan angles to define crystal orientation in the space */
    private double[] taitbriangles;
    
    /* crystal center coordinates as a double[3] array */      
    private double[] centerArr; 
      
    /**
     * constructor
     * @param point1  (x,y,z) of crystal front face center point as GVector
     * @param point2 (x,y,z) of crystal back face center point as GVector
     */
        public Geant4Position(GVector point1, GVector point2) {       
        centerPoint = point2.Add(point1).Multiply(0.5);
        centerArr = new double[3];
        centerArr[0] = centerPoint.x();
        centerArr[1] = centerPoint.y();
        centerArr[2] = centerPoint.z();
        crysvec = (point2.Substract(centerPoint)).getUnitVector();
        taitbriangles = new double[3];
        this.calculateTaitBryanAngles();
    }

   
    /**
     *
     * @return center coordinates as a GVector
     */
        public GVector getCenter() {
        return this.centerPoint;
    }

    

    /**
     *
     * @return center coordinates as an array double[x_center, y_center, z_center]
     */
        public double[] getCenterArr() {
        return centerArr;
    }


    /**
     *
     * @return Tait-Bryan angles as an array doubles [phi, theta, psi]
     */
        public double[] getTaitBryanAngles() {
        return this.taitbriangles;
    }

    /*
     * calculates Tait-Bryan angles for the object orientation  defined by initial and final state vectors
     * throws arithmetical exception if the phi angle is calculated wrong or 0
     */ 
    private void calculateTaitBryanAngles() throws ArithmeticException{
        CrystalTaitBryanAngleCalculator crysTBang = new CrystalTaitBryanAngleCalculator(crysvec);
        if (crysTBang.getPhi() == Double.NaN) {
            throw new ArithmeticException("Calculated Tait-Bryan phi angle is NaN.");
        }
        this.taitbriangles[0] = -crysTBang.getPhi();
        this.taitbriangles[1] = -crysTBang.getTheta();
        this.taitbriangles[2] = -crysTBang.getPsi();
    }


    /**
     * Print crystal center coordinates(X,Y,Z) and Tait-Bryan angles
     */
        public void Print() {
        this.centerPoint.Print();
    }
    
    

    /**
     *
     * @return String of Geant4Position object attributes
     */
        public String toString(){
        return ("\"Crys Center coordinates:\\t\""+this.centerPoint.toString()+"Tait Brian angles phi = " + Math.toDegrees(this.taitbriangles[0]) + "\t theta = "
                + Math.toDegrees(this.taitbriangles[1]) + "\t psi = " + Math.toDegrees(this.taitbriangles[2]) + "\n");
    }

}
