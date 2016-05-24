/*
 * To change this license header, choose License Headers in Project Properties. To change this template file, choose
 * Tools | Templates and open the template in the editor.
 */
package org.hps.detector.ecal;


/**
 * Calculates Euler (actually Tait-Bryan) angles for a rotation which transfers the crystal from it's initial
 * state parallel to the Z axis into the center position defined by the center points of the front and back faces.
 * <p>
 * For information about Tait-Bryan angles <a href="http://sedris.org/wg8home/Documents/WG80485.pdf">look here</a>. 
 * 
 * @author Annie Simonyan
 */
class CrystalTaitBryanAngleCalculator {
    
    private double phi = 0;  
    private double theta = 0;
    private final double psi = 0;

    /* Vector representing final position. */
    private GVector fin; 

    private double sinTheta = 0;
    private double cosPhi = 0;
    private double sinPhi = 0;
    
    /* constructor:
    * @param vecFin final state unit vector, 
    * if not unit, it will be redefined as unitary,
    * initial state is defined as (0,0,1) vector, which is parallel to Z(beam direction)
    */
    public CrystalTaitBryanAngleCalculator(GVector vecFin) {
        fin = vecFin;
        if (vecFin.isUnitary() == false) {
            fin = vecFin.getUnitVector();
        }
        this.phi = this.calculatePhi();
        this.theta = this.calculateTheta();
    }

    /*
     * calculates phi angle
     * @return phi angle, rotation around X [rad]
     */
    private double calculatePhi() {
        sinPhi = -fin.y();
        cosPhi = Math.sqrt(1 - this.sinPhi * this.sinPhi);
        return Math.asin(this.sinPhi);
    }
    
    /*
     * calculates theta angle after phi is calculated
     * @return theta angle, rotation around Y [rad]
     *  
     */
    private double calculateTheta() {
        if (this.cosPhi == 0) {
            this.sinTheta = Double.NaN;
        } else {
            this.sinTheta = fin.x() / this.cosPhi;
        }
        return Math.asin(this.sinTheta);
    }
    
    /*
     * @return value of theta [rad],  rotation around Y
     */
    public double getTheta() {
        return this.theta;
    }

    /*
     * @return value of phi [rad],  rotation around X
     * /
     */
    public double getPhi() {
        return this.phi;
    }
    
    /*
     * @return value of psi [rad],  rotation around Z
     */
    public double getPsi() {
        return this.psi;
    }
    
    /*
     * @return string of Tait-Bryan angles-(phi, theta, psi) [rad]
     */
    public String toString(){
        return ("("+this.phi+","+this.theta+","+this.psi+")");
    }
}
