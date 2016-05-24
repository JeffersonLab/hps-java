package org.hps.detector.ecal;

import hep.physics.vec.BasicHep3Vector;

/**
 * Define a vector in (XYZ) coordinate system as a geometrical object.
 * This class is similar to HepVector 
 * 
 * @author Annie Simonyan
 */
class GVector {

    private double x = 0, y = 0, z = 0;
    private double[] vec;

    /**
     * default constructor
     */
    public GVector() {
    }

    /**
     * constructor
     * @param double[3]={x,y,z} array for vector coordinates
     */
    public GVector(double[] array) {
        vec = array;
        this.x = array[0];
        this.y = array[1];
        this.z = array[2];
    }

    /**
     * convert this GVector to BasicHep3Vector
     * @return BasicHep3Vector object
     */
    public BasicHep3Vector getHepVector(){
        return new BasicHep3Vector(this.x, this.y, this.z);
    }
    
    /**
     * constructor: 
     * @param x - x coordinates of vector
     * @param y - y coordinates of vector
     * @param z - z coordinates of vector
     * 
     */
    public GVector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        vec = new double[3];
        vec[0] = x;
        vec[1] = y;
        vec[2] = z;
    }
    
    public double x() {
        return x;
    }
    
    public double y() {
        return y;
    }
    
    public double z() {
        return z;
    }

    /**
     * set vector x coordinate
     * @param x vector x coordinate
     */ 
    public void SetX(double x) {
        this.x = x;
    }

    
    /**
     * set vector y coordinate
     * @param y vector y coordinate
     */ 
    public void SetY(double y) {
        this.y = y;
    }

    
    /**
     * set vector z coordinate
     *@param z vector z coordinate
     */ 
    public void SetZ(double z) {
        this.z = z;
    }

    
    /**
     * set vector (x,y,z) coordinate
     * @param x vector x coordinate
     * @param y vector y coordinate
     * @param z vector z coordinate
     */ 
    public void SetXYZ(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * check if the vector is unit vector
     * @return true if the vector is unit vector
     */ 
    public boolean isUnitary() {
        return (Double.compare(StatFunUtils.round(this.Module(), 12), 1.0) == 0);
    }

    /**
     * @return the module of the vector
     */     
    public double Module() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /**
     * multiplication by coefficient k
     * @param k - multiplication coefficient
     * @return new GVector=this*k
     */ 
    public GVector Multiply(double k) {

        return (new GVector(k * this.x, k * this.y, k * this.z));
    }

    
    /**
     * sum of this vector and argument vec2
     * @param vec2 GVector to add
     * @return new GVector = this+vec2
     */ 
    public GVector Add(GVector vec2) {

        return (new GVector(this.x + vec2.x, this.y + vec2.y, this.z + vec2.z));

    }

    /**
     *
     * @return new vector opposite to this vector
     */ 
    public GVector getOpposite() {
        return (new GVector(-this.x, -this.y, -this.z));
    }

    /**
     * substract GVectors
     * @param vec2 GVector to substract  
     * @return new vector = this - vec2
     *
     */
    public GVector Substract(GVector vec2) {

        return (new GVector(this.x - vec2.x, this.y - vec2.y, this.z - vec2.z));

    }

    /**
     * @param trvec translation vector
     * @return new GVector defined as translation of this vector by argument vector
     * newVector = this+trvec
     */ 
    public GVector TranslateBy(GVector trvec) {
        
        return this.Add(trvec);
    }

    /**
     * Rotate this vector by (alpha, betta, gamma) rotation angles
     * (Rz-alpha,Ry-beta,Rx-gamma)
     *   
     * @param alpha rotation around Z axis
     * @param beta rotation around Y axis
     * @param gamma rotation around X axis
     * @return new GVector=this vector rotated by (alpha, betta, gamma)
     */
    public GVector RotateBy(double alpha, double betta, double gamma) {

        double cos_alpha = Math.cos(alpha);
        double sin_alpha = Math.sin(alpha);

        double cos_betta = Math.cos(betta);
        double sin_betta = Math.sin(betta);

        double cos_gamma = Math.cos(gamma);
        double sin_gamma = Math.sin(gamma);

        double xx = this.x * cos_alpha * cos_betta + this.y
                * (-sin_alpha * cos_gamma + cos_alpha * sin_betta * sin_gamma) + this.z
                * (sin_alpha * sin_gamma + cos_alpha * sin_betta * sin_gamma);

        double yy = this.x * sin_alpha * cos_betta + this.y
                * (cos_alpha * cos_gamma + sin_alpha * sin_betta * sin_gamma) + this.z
                * (-cos_alpha * sin_gamma + sin_alpha * sin_betta * sin_gamma);

        double zz = this.x * (-sin_betta) + this.y * (cos_betta * sin_gamma) + this.z * (cos_betta * cos_gamma);

        return (new GVector(xx, yy, zz));
    }

    /**
     * scalar multiplication of this vector by vec2
     * @param vec2 GVector
     * @return double = (this vector * vec2 vector)
     * 
     */ 
    public double ScalarM(GVector vec2) {
        return this.x * vec2.x + this.y * vec2.y + this.z * vec2.z;
    }

    /**
     * Calculate angle between vectors
     * @param vec2 GVector to calc. angle with this
     * @return angle between this and vec2 vectors [rad] 
     */ 
    public double Angle(GVector vec2) {
        // System.out.print("Scaliar= "+this.ScaliarM(vec2) +" modN1 = "+this.Module()+"  modN2 = "+vec2.Module()+"\n");
        // System.out.print(this.ScaliarM(vec2)/(this.Module()*vec2.Module())+ "\n");
        // System.out.println(Math.acos(this.ScaliarM(vec2)/(this.Module()*vec2.Module())));
        double cosAlpha = this.ScalarM(vec2) / (this.Module() * vec2.Module());

        if (Double.compare(StatFunUtils.round(cosAlpha, 12), 1.0) == 0) {
            cosAlpha = 1.0;
        } else if (Double.compare(StatFunUtils.round(cosAlpha, 12), -1.0) == 0) {
            cosAlpha = -1.0;
        }

        return Math.acos(cosAlpha);
    }

    /**
     * 
     * @return coordinates of this vector as an array 
     */
    public double[] getVector() {
        return this.vec;
    }

    /**
     * Calculate unit vector of this vector
     * @return new GVector, which is unit vector of this vector
     * 
     */ 
    public GVector getUnitVector() {
        return new GVector(this.x / this.Module(), this.y / this.Module(), this.z / this.Module());
    }

    
    /**
     * 
     * @return new GVector = coordinates of center of this vector as a GVector object
     * 
     */ 
    public GVector getCenter() {
        return (this.Multiply(0.5));
    }

    /**
     * Print the coordinates of the GVector as (x,y,z)
     */
    public void Print() {
        System.out.println("(" + this.x + ", " + this.y + ", " + this.z + ")");
    }
    
    /**
     * @return String of coordinates of this vector
     */ 
    public String toString(){
        return ("(" + this.x + ", " + this.y + ", " + this.z + ")");
    }
}
