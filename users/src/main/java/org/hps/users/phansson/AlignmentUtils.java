/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.users.phansson;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.*;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelixUtils;

/**
 *
 * @author phansson
 */
public class AlignmentUtils {
    
    
    private int _debug;
    

    public AlignmentUtils() {
        _debug = 0;
    }
    public AlignmentUtils(boolean debug) {
        _debug = debug ? 1 : 0;
    }
    public void setDebug(boolean debug) {
        _debug = debug ? 1 : 0;
    }
    public void setDebug(int debug) {
        _debug = debug;
    }

    public Hep3Matrix getRotationMatrix(String axis, double angle) {
        Hep3Matrix m;
        if(axis.equalsIgnoreCase("x")) m = new BasicHep3Matrix(1,0,0,0,Math.cos(angle),Math.sin(angle),0,-Math.sin(angle),Math.cos(angle));
        else if(axis.equalsIgnoreCase("y")) m = new BasicHep3Matrix(Math.cos(angle),0,-Math.sin(angle),0,1,0,Math.sin(angle),0,Math.cos(angle));
        else if (axis.equalsIgnoreCase("z")) m = new BasicHep3Matrix(Math.cos(angle),Math.sin(angle),0,-Math.sin(angle),Math.cos(angle),0,0,0,1);
        else throw new RuntimeException(this.getClass().getSimpleName()+": axis " + axis + " is not valid!!");
        return m;
    }
    
    
    public Hep3Matrix getRotationMatrixAroundX(double angle) {
        return  new BasicHep3Matrix(1,0,0,0,Math.cos(angle),-Math.sin(angle),0,Math.sin(angle),Math.cos(angle));
    }

    
    
    public BasicMatrix calculateLocalHelixDerivatives(HelicalTrackFit trk, double xint) {
        
        // Calculate the derivative w.r.t. to the track parameters (in order/index):
        // d0, z0, slope, phi0, R
        // for a change in x,y,z
        //
        // All the below is in the tracking coordonates:
        // x-axis is beamline direction
        // y-axis is bend direction
        // z-axis is non-bend direction
        //
        // The return object is a 3-by-5 matrix with the derivatives.
        //
        // Input:
        double xr = 0.0; // reference position - typically set top PCA i.e. (x0,y0)
        double yr = 0.0; // reference position - typically set top PCA i.e. (x0,y0)
        double d0 = trk.dca();
        double phi0 = trk.phi0();
        double R = trk.R();
        double slope = trk.slope();
        double s = HelixUtils.PathToXPlane(trk, xint, 0, 0).get(0);
        double phi = -s/R + phi0;
        BasicMatrix dfdq = new BasicMatrix(3,5); //3-dim,ntrackparams
        
        
        dfdq.setElement(0, 0, this.dx_dd0(xint, d0, phi0, R, phi));
        dfdq.setElement(0, 1, this.dx_dz0(R, phi));
        dfdq.setElement(0, 2, this.dx_dslope(R, phi));
        dfdq.setElement(0, 3, this.dx_dphi0(xint, d0, phi0, R, phi));
        dfdq.setElement(0, 4, this.dx_dR(xint, xr, yr, d0, phi0, R, phi));
        
        
        dfdq.setElement(1, 0, this.dy_dd0(xint, d0, phi0, R, phi));
        dfdq.setElement(1, 1, this.dy_dz0(R, phi));
        dfdq.setElement(1, 2, this.dy_dslope(R, phi));
        dfdq.setElement(1, 3, this.dy_dphi0(xint, d0, phi0, R, phi));
        dfdq.setElement(1, 4, this.dy_dR(xint, d0, phi0, R, phi));
        
        
        dfdq.setElement(2, 0, this.dz_dd0(xint, d0, phi0, slope, R));
        dfdq.setElement(2, 1, this.dz_dz0());
        dfdq.setElement(2, 2, this.dz_dslope(phi0, R, phi));
        dfdq.setElement(2, 3, this.dz_ddphi0(s, d0, phi0, slope, R));
        dfdq.setElement(2, 4, this.dz_dR(xint, d0, phi0, slope, R, phi));
        
        
        return dfdq;
        
    }

    
    private BasicMatrix FillMatrix(double[][] array, int nrow, int ncol) {
        BasicMatrix retMat = new BasicMatrix(nrow, ncol);
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                retMat.setElement(i, j, array[i][j]);
            }
        }
        return retMat;
    }

    
    
    
    //-----------------------------------
    // Local derivatives of f in the x-direction
    
    
    public double dx_dd0(double x, double d0, double phi0, double R, double phi) {
        return -Math.sin(phi0) - R * Math.cos(phi)*dphi_dd0(x,d0,phi0,R);
    }
    public double dx_dz0(double R, double phi) {
        return -R*Math.cos(phi)*dphi_dz0();
    }
    public double dx_dslope(double R, double phi) {
        return R*Math.cos(phi)*dphi_dslope();
    }
    public double dx_dphi0(double x, double d0, double phi0, double R, double phi) {
        return (R-d0)*Math.cos(phi0) - R*Math.cos(phi)*dphi_dphi0(x,d0,phi0,R);
    }
    public double dx_dR(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        return Math.sin(phi0) - R*Math.cos(phi)*dphi_dR(x,d0,phi0,R) - Math.sin(phi);
    }
    
    //-----------------------------------
    
    //-----------------------------------
    // Local derivatives of f in the y-direction
    
    public double dy_dd0(double x, double d0, double phi0, double R, double phi) {
        return Math.cos(phi0) - R*Math.sin(phi)*dphi_dd0(x, d0, phi0, R);
    }
    public double dy_dz0(double R, double phi) {
        return -R*Math.sin(phi)*this.dphi_dz0();
    }
    public double dy_dslope(double R, double phi) {
        return -R*Math.sin(phi)*this.dphi_dslope();
    }
    
    public double dy_dphi0(double x, double d0, double phi0, double R, double phi) {
        return (R-d0)*Math.sin(phi0) - R*Math.sin(phi)*this.dphi_dphi0(x, d0, phi0, R);
    }
    public double dy_dR(double x, double d0, double phi0, double R, double phi) {
        return -Math.cos(phi0) - R*Math.sin(phi)*this.dphi_dR(x, d0, phi0, R) + Math.cos(phi);
    }
    
    
     //-----------------------------------
    // Local derivatives of f in the z-direction

    
    public double dz_dd0(double x, double d0, double phi0, double slope, double R) {                
        return -R*slope*this.dphi_dd0(x, d0, phi0, R);
    }
        
    public double dz_dz0() {
        return 1.0;
    }

    public double dz_dslope(double phi0, double R, double phi) {        
        return -R*(phi-phi0);
    }

    public double dz_ddphi0(double x, double d0, double phi0, double slope, double R) {                
        return -R*slope*(this.dphi_dphi0(x, d0, phi0, R)-1);
    }
    
    public double dz_dR(double x, double d0, double phi0, double slope, double R, double phi) {
        return -slope*(phi-phi0+R*this.dphi_dR(x, d0, phi0, R));
    }

    
    //-----------------------------------
    // Derivatives of phi w.r.t. track parameters

    public double dphi_dd0(double x,double d0, double phi0, double R) {
        double num = -1*Math.sin(phi0);
        double den = R*Math.sqrt( 1 - Math.pow(x+(d0-R)*Math.sin(phi0), 2)/Math.pow(R,2));
        return num/den;
    }
    
    public double dphi_dz0() {
        return 0;
    }
    public double dphi_dslope() {
        return 0;
    }
    public double dphi_dphi0(double x,double d0, double phi0, double R) {
        double num = -1*(d0-R)*Math.cos(phi0);
        double den = R*Math.sqrt( 1 - Math.pow(x+(d0-R)*Math.sin(phi0),2)/Math.pow(R, 2));
        return num/den;
    }
    
    public double dphi_dR(double x,double d0, double phi0, double R) {
        double num = x + d0*Math.sin(phi0);
        double den = Math.pow(R, 2)*Math.sqrt( 1 - Math.pow(x+(d0-R)*Math.sin(phi0),2)/Math.pow(R, 2));
        return num/den;
    }
    
    
    
    
    
    
    
    
    
    public double dphi_dx(double xint, double d0, double phi0, double R) {
        double num = -1.0;
        double A_x = -xint + (-d0+R)*Math.sin(phi0);
        double den = R*Math.sqrt(1-(A_x*A_x)/(R*R));
        return num/den;
//        double num = -Math.pow(R, 2)*sign(R);
//        double den1 = Math.sqrt( Math.pow(R, 2) - Math.pow(xint+(d0-R)*Math.sin(phi0), 2)   );
//        double den2 = -Math.pow(xint+(d0-R)*Math.sin(phi0),2);
//        double den3 = sign(R)*sign(R)*(-Math.pow(R, 2)+Math.pow(xint, 2)+2*(d0-R)*xint*Math.sin(phi0)+Math.pow(d0-R,2)*Math.pow(Math.sin(phi0),2) );
//        return num/(den1*(den2+den3));
    }

    public double dphi_dy(double y, double d0, double phi0, double R, double phi) {    
        double A_y = y + (-d0+R)*Math.cos(phi0);
        double num = A_y * Math.signum(R);
        double den = R*Math.sqrt(R*R-A_y*A_y)*Math.sqrt( 1 - (R*R-A_y*A_y)/(R*R));
        return num/den; 
//        double c0 = Math.cos(phi0);
//        double num = Math.pow(R,2)*sign(R)*sign(R);
//        double den1 = Math.sqrt(Math.pow(R, 2)-Math.pow(y+(d0-R)*c0, 2));
//        double den2 = -Math.pow(y+(d0-R)*c0,2);
//        double den3 =  (-Math.pow(R,2) + Math.pow(y, 2) + 2*(d0-R)*y*c0 + Math.pow(d0-R, 2)*Math.pow(c0, 2))*sign(R)*sign(R);        
//        return num/(den1*(den2+den3));
    }

    public double dphi_dz(double slope, double R) {
        return -1/(R*slope);
    }

    
    
    
    
    
    //-----------------------------------
    // Derivatives for translation
    
    public double dx_dx() {
        return 1;
    }
    
    public double dy_dx(double xint, double d0, double phi0, double R, double phi) { 
        return -R*Math.sin(phi)*this.dphi_dx(xint, d0, phi0, R);
    }
    
    public double dz_dx(double xint, double xr,double yr,double d0, double phi0, double slope, double R) {   
        return -R*slope*this.dphi_dx(xint, d0, phi0, R);
    }
    
    public double dx_dy(double y, double d0, double phi0, double R, double phi) {
        return -R*Math.cos(phi)*this.dphi_dy(y, d0, phi0, R, phi);
    }
     
    public double dy_dy() {
        return 1;
    }
    
    public double dz_dy(double y,double d0, double phi0, double slope, double R, double phi) {
        return -R*slope*this.dphi_dy(y, d0, phi0, R, phi);
    }
     

    public double dx_dz(double slope, double R, double phi) {
        return -R*Math.cos(phi)*this.dphi_dz(slope, R);
        //return sign(R)*R*Math.cos(phi)*this.dphi_dz(slope, R);
    }   
    
    public double dy_dz(double slope, double R, double phi) {     
        return -R*Math.sin(phi)*this.dphi_dz(slope, R);
        //return -sign(R)*R*Math.sin(phi)*this.dphi_dz(slope, R);
    }
    
    public double dz_dz() {
        return 1;
    }
    
    
   
    
    public Hep3Matrix rotationDer_da() {
        /*
         * Derivative w.r.t. a of a rotation matrix with small Euler angles a,b,c around axis x,y,z
         * Small angle limit
         */
        return new BasicHep3Matrix(0,0,0,0,0,1,0,-1,0);
    }
    public Hep3Matrix rotationDer_db() {
        /*
         * Derivative w.r.t. b of a rotation matrix with Euler angles a,b,c around axis x,y,z
         * Small angle limit
         */
        return new BasicHep3Matrix(0,0,-1,0,0,0,1,0,0);
    }
    public Hep3Matrix rotationDer_dc() {
        /*
         * Derivative w.r.t. c of a rotation matrix with Euler angles a,b,c around axis x,y,z
         * Small angle limit
         */
        return new BasicHep3Matrix(0,1,0,-1,0,0,0,0,0);
    }
    
    
    
    public BasicMatrix calculateJacobian(Hep3Vector x_vec, Hep3Matrix T) {
        /*
         * Calculate jacobian da/db
         * Arguments:
         * Hep3Matrix T is the 3x3 rotation matrix from the global/tracking to local frame
         * Hep3Vector x_vec is the position of the track on the sensor
         */
        

        //Derivatives of the rotation matrix deltaR' w.r.t. rotations a,b,c around axis x,y,z
        Hep3Matrix ddeltaRprime_da = rotationDer_da();
        Hep3Matrix ddeltaRprime_db = rotationDer_db();
        Hep3Matrix ddeltaRprime_dc = rotationDer_dc();

        if(_debug>1) {
            System.out.printf("%s: Rotation matrx from tracking/global to local frame T:\n %s\n",this.getClass().getSimpleName(),T.toString());
            System.out.printf("%s: Derivatives of the rotation matrix deltaR' w.r.t. rotation a,b,c around x,y,z axis:\n",this.getClass().getSimpleName());
            System.out.printf("%s: ddeltaRprime_da:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_da.toString());
            System.out.printf("%s: ddeltaRprime_db:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_db.toString());
            System.out.printf("%s: ddeltaRprime_dc:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_dc.toString());
        }
        
        
        
        
        // Upper left 3x3
        Hep3Vector deltaX_gl =  new BasicHep3Vector(1,0,0);    
        Hep3Vector deltaY_gl =  new BasicHep3Vector(0,1,0);    
        Hep3Vector deltaZ_gl =  new BasicHep3Vector(0,0,1);    
        Hep3Vector dq_dx = VecOp.mult(T, deltaX_gl);
        Hep3Vector dq_dy = VecOp.mult(T, deltaY_gl);
        Hep3Vector dq_dz = VecOp.mult(T, deltaZ_gl);
        
        if(_debug>1) {
            System.out.printf("%s: - Upper left 3x3 of Jacobian da/db: dq_dx,dq_dy,dq_dz\n",this.getClass().getSimpleName());
            System.out.printf("%s: dq_dx: %s\n",this.getClass().getSimpleName(),dq_dx.toString());
            System.out.printf("%s: dq_dy: %s\n",this.getClass().getSimpleName(),dq_dy.toString());
            System.out.printf("%s: dq_dz: %s\n",this.getClass().getSimpleName(),dq_dz.toString());
        }
        
       
        
        if(_debug>1) {
            System.out.printf("%s: - Upper right 3x3 of Jacobian da/db: dq_dx,dq_dy,dq_dz\n",this.getClass().getSimpleName());
        }
        
        
        // Upper right 3x3
        
        Hep3Vector x_vec_tmp = VecOp.mult(ddeltaRprime_da, x_vec); // derivative of the position
        Hep3Vector x_vec_tmp2 = VecOp.sub(x_vec_tmp, x_vec); // subtract position
        Hep3Vector dq_da = VecOp.mult(T,x_vec_tmp2); //rotated into local frame
        
        if(_debug>1) {
            System.out.printf("%s: position   %s rotation derivative w.r.t. a ddeltaR'/da %s\n",this.getClass().getSimpleName(),x_vec.toString(),x_vec_tmp.toString());
            System.out.printf("%s: subtracted %s and rotated to local %s \n",this.getClass().getSimpleName(),x_vec_tmp2.toString(),dq_da.toString());
        }
        
        
        x_vec_tmp = VecOp.mult(ddeltaRprime_db, x_vec);
        x_vec_tmp2 = VecOp.sub(x_vec_tmp, x_vec); 
        Hep3Vector dq_db = VecOp.mult(T,x_vec_tmp2);
        
        if(_debug>1) {
            System.out.printf("%s: position   %s rotation derivative w.r.t. a ddeltaR'/db %s\n",this.getClass().getSimpleName(),x_vec.toString(),x_vec_tmp.toString());
            System.out.printf("%s: subtracted %s and rotated to local %s \n",this.getClass().getSimpleName(),x_vec_tmp2.toString(),dq_db.toString());
        }
        
        x_vec_tmp = VecOp.mult(ddeltaRprime_dc, x_vec);
        x_vec_tmp2 = VecOp.sub(x_vec_tmp, x_vec); 
        Hep3Vector dq_dc = VecOp.mult(T,x_vec_tmp2);

        if(_debug>1) {
            System.out.printf("%s: position   %s rotation derivative w.r.t. a ddeltaR'/dc %s\n",this.getClass().getSimpleName(),x_vec.toString(),x_vec_tmp.toString());
            System.out.printf("%s: subtracted %s and rotated to local %s \n",this.getClass().getSimpleName(),x_vec_tmp2.toString(),dq_dc.toString());
        }
        
    
        if(_debug>1) {
            System.out.printf("%s: Summary:\n",this.getClass().getSimpleName());
            System.out.printf("%s: dq_da: %s\n",this.getClass().getSimpleName(),dq_da.toString());
            System.out.printf("%s: dq_db: %s\n",this.getClass().getSimpleName(),dq_db.toString());
            System.out.printf("%s: dq_dc: %s\n",this.getClass().getSimpleName(),dq_dc.toString());
        }
        
        
       
        
        // Lower left 3x3
        //deltaR = (alpha,beta,gamma) the rotation alignment parameters in the local frame
        Hep3Vector ddeltaR_dx = new BasicHep3Vector(0,0,0);
        Hep3Vector ddeltaR_dy = new BasicHep3Vector(0,0,0);
        Hep3Vector ddeltaR_dz = new BasicHep3Vector(0,0,0);

        
        if(_debug>1) {
            System.out.printf("%s: - Lower left 3x3 of Jacobian ddeltaR/d(x,y,z): dDeltaR_dx,dDeltaR_dy,dDeltaR_dz\n",this.getClass().getSimpleName());
            System.out.printf("%s: ddeltaR_dx: %s\n",this.getClass().getSimpleName(),ddeltaR_dx.toString());
            System.out.printf("%s: ddeltaR_dy: %s\n",this.getClass().getSimpleName(),ddeltaR_dy.toString());
            System.out.printf("%s: ddeltaR_dz: %s\n",this.getClass().getSimpleName(),ddeltaR_dz.toString());
        }
        
        
     
        
        // Lower right 3x3
/*
        //deltaR = (alpha,beta,gamma) the rotation alignment parameters in the local frame
        //Expressing T in Euler angles i,j,k
        double i = 0;
        double j = 0;
        double k = 0;
        double dalpha_da = Math.cos(k)*Math.sin(i)*Math.sin(j) + Math.cos(i)*Math.sin(k);
        double dbeta_da = Math.cos(i)*Math.cos(k) - Math.sin(i)*Math.sin(j)*Math.sin(k);
        double dgamma_da = -Math.cos(j)*Math.sin(i);
        double dalpha_db = -Math.cos(i)*Math.cos(k)*Math.sin(j) + Math.sin(i)*Math.sin(k);
        double dbeta_db = Math.cos(k)*Math.sin(i) + Math.cos(i)*Math.sin(j)*Math.sin(k);
        double dgamma_db = Math.cos(i)*Math.cos(j);
        double dalpha_dc = -Math.cos(i)*Math.cos(k)*Math.sin(j) + Math.sin(i)*Math.sin(k);
        double dbeta_dc = Math.cos(k)*Math.sin(i) + Math.cos(i)*Math.sin(j)*Math.sin(k);
        double dgamma_dc = Math.sin(i)*Math.sin(j);
        
        Hep3Vector ddeltaR_da = new BasicHep3Vector(dalpha_da,dbeta_da,dgamma_da);
        Hep3Vector ddeltaR_db = new BasicHep3Vector(dalpha_db,dbeta_db,dgamma_db);
        Hep3Vector ddeltaR_dc = new BasicHep3Vector(dalpha_dc,dbeta_dc,dgamma_dc);
*/
        
        if(_debug>1) {
            System.out.printf("%s: - Lower right 3x3 of Jacobian ddeltaR/d(a,b,c): \n",this.getClass().getSimpleName());
            System.out.printf("%s: T: %s\n",this.getClass().getSimpleName(),T.toString());
        }

        
        
        //Now fill the Jacobian 6x6 matrix
        BasicMatrix da_db = new BasicMatrix(6,6);
        //upper left 3x3
        da_db.setElement(0,0,dq_dx.x());
        da_db.setElement(1,0,dq_dx.y());
        da_db.setElement(2,0,dq_dx.z());
        da_db.setElement(0,1,dq_dy.x());
        da_db.setElement(1,1,dq_dy.y());
        da_db.setElement(2,1,dq_dy.z());
        da_db.setElement(0,2,dq_dz.x());
        da_db.setElement(1,2,dq_dz.y());
        da_db.setElement(2,2,dq_dz.z());
        //upper right 3x3
        da_db.setElement(0,3,dq_da.x());
        da_db.setElement(1,3,dq_da.y());
        da_db.setElement(2,3,dq_da.z());
        da_db.setElement(0,4,dq_db.x());
        da_db.setElement(1,4,dq_db.y());
        da_db.setElement(2,4,dq_db.z());
        da_db.setElement(0,5,dq_dc.x());
        da_db.setElement(1,5,dq_dc.y());
        da_db.setElement(2,5,dq_dc.z());
        //lower right 3x3
        da_db.setElement(3,3,T.e(0, 0));
        da_db.setElement(4,3,T.e(1, 0));
        da_db.setElement(5,3,T.e(2, 0));
        da_db.setElement(3,4,T.e(0, 1));
        da_db.setElement(4,4,T.e(1, 1));
        da_db.setElement(5,4,T.e(2, 1));
        da_db.setElement(3,5,T.e(0, 2));
        da_db.setElement(4,5,T.e(1, 2));
        da_db.setElement(5,5,T.e(2, 2));
//        da_db.setElement(4,3,ddeltaR_da.y());
//        da_db.setElement(5,3,ddeltaR_da.z());
//        da_db.setElement(3,4,ddeltaR_db.x());
//        da_db.setElement(4,4,ddeltaR_db.y());
//        da_db.setElement(5,4,ddeltaR_db.z());
//        da_db.setElement(3,5,ddeltaR_dc.x());
//        da_db.setElement(4,5,ddeltaR_dc.y());
//        da_db.setElement(5,5,ddeltaR_dc.z());
        //lower left 3x3
        da_db.setElement(3,0,ddeltaR_dx.x());
        da_db.setElement(4,0,ddeltaR_dx.y());
        da_db.setElement(5,0,ddeltaR_dx.z());
        da_db.setElement(3,1,ddeltaR_dy.x());
        da_db.setElement(4,1,ddeltaR_dy.y());
        da_db.setElement(5,1,ddeltaR_dy.z());
        da_db.setElement(3,2,ddeltaR_dz.x());
        da_db.setElement(4,2,ddeltaR_dz.y());
        da_db.setElement(5,2,ddeltaR_dz.z());
        
        if(_debug>1) {
            System.out.printf("%s: da_db:\n%s \n",this.getClass().getSimpleName(),da_db.toString());
            System.out.printf("%s: det(da_db) = %.3f \n",this.getClass().getSimpleName(),da_db.det());
        }
        
        return da_db;
        
    }
    
    
    
    public void printJacobianInfo(BasicMatrix da_db) {
    
        System.out.printf("%s: Jacobian info ---\nda_db:\n%s \n",this.getClass().getSimpleName(),da_db.toString());
        System.out.printf("du_dx = %8.3f du_dy = %8.3f du_dz = %8.3f\n",da_db.e(0,0),da_db.e(0,1),da_db.e(0,2));
        System.out.printf("dv_dx = %8.3f dv_dy = %8.3f dv_dz = %8.3f\n",da_db.e(1,0),da_db.e(1,1),da_db.e(1,2));
        System.out.printf("dw_dx = %8.3f dw_dy = %8.3f dw_dz = %8.3f\n",da_db.e(2,0),da_db.e(2,1),da_db.e(2,2));
        System.out.printf("du_da = %8.3f du_db = %8.3f du_dc = %8.3f\n",da_db.e(0,3),da_db.e(0,4),da_db.e(0,5));
        System.out.printf("dv_da = %8.3f dv_db = %8.3f dv_dc = %8.3f\n",da_db.e(1,3),da_db.e(1,4),da_db.e(1,5));
        System.out.printf("dw_da = %8.3f dw_db = %8.3f dw_dc = %8.3f\n",da_db.e(2,3),da_db.e(2,4),da_db.e(2,5));
        System.out.printf("dalpha_dx = %8.3f dalpha_dy = %8.3f dalpha_dz = %8.3f\n",da_db.e(3,0),da_db.e(3,1),da_db.e(3,2));
        System.out.printf("dbeta_dx  = %8.3f dbeta_dy  = %8.3f dbeta_dz  = %8.3f\n",da_db.e(4,0),da_db.e(4,1),da_db.e(4,2));
        System.out.printf("dgamma_dx = %8.3f dgamma_dy = %8.3f dgamma_dz = %8.3f\n",da_db.e(5,0),da_db.e(5,1),da_db.e(5,2));
        System.out.printf("dalpha_da = %8.3f dalpha_db = %8.3f dalpha_dc = %8.3f\n",da_db.e(3,3),da_db.e(3,4),da_db.e(3,5));
        System.out.printf("dbeta_da  = %8.3f dbeta_db  = %8.3f dbeta_dc  = %8.3f\n",da_db.e(4,3),da_db.e(4,4),da_db.e(4,5));
        System.out.printf("dgamma_da = %8.3f dgamma_db = %8.3f dgamma_dc = %8.3f\n",da_db.e(5,3),da_db.e(5,4),da_db.e(5,5));
        
    }
    



    public BasicMatrix calculateGlobalHitPositionDers(Hep3Vector x_vec) {

        
        //****************************************************************************
        // Calculate the global derivatives in the global/tracking frame dq_a^gl/db
        //  q_a^gl is the alignment corrected hit position in the global/tracking frame
        // b = (dx,dy,dz,a,b,c) 
        
         
        //Derivatives of the rotation matrix deltaR' w.r.t. rotations a,b,c around axis x,y,z
        Hep3Matrix ddeltaRprime_da = rotationDer_da();
        Hep3Matrix ddeltaRprime_db = rotationDer_db();
        Hep3Matrix ddeltaRprime_dc = rotationDer_dc();

        if(_debug>1) {
            System.out.printf("%s: Derivatives of the rotation matrix deltaR' w.r.t. rotation a,b,c around x,y,z axis:\n",this.getClass().getSimpleName());
            System.out.printf("%s: ddeltaRprime_da:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_da.toString());
            System.out.printf("%s: ddeltaRprime_db:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_db.toString());
            System.out.printf("%s: ddeltaRprime_dc:\n %s\n",this.getClass().getSimpleName(),ddeltaRprime_dc.toString());
        }
        
        
        
        BasicMatrix dq_agl_db = new BasicMatrix(3,6);
        //Translations
        Hep3Vector dq_agl_dx = new BasicHep3Vector(1,0,0);
        Hep3Vector dq_agl_dy = new BasicHep3Vector(0,1,0);
        Hep3Vector dq_agl_dz = new BasicHep3Vector(0,0,1);
        //Rotations
        // Derivative of the rotations w.r.t. rotations a,b,c was already calculated above:
        // but they need to be avaluated at the hit positon
        Hep3Vector dq_agl_dalpha = VecOp.mult(ddeltaRprime_da,x_vec);
        Hep3Vector dq_agl_dbeta = VecOp.mult(ddeltaRprime_db,x_vec);
        Hep3Vector dq_agl_dgamma = VecOp.mult(ddeltaRprime_dc,x_vec);
        
        //put them all in one big matrix
        
        dq_agl_db.setElement(0, 0, dq_agl_dx.x());
        dq_agl_db.setElement(1, 0, dq_agl_dx.y());
        dq_agl_db.setElement(2, 0, dq_agl_dx.z());

        dq_agl_db.setElement(0, 1, dq_agl_dy.x());
        dq_agl_db.setElement(1, 1, dq_agl_dy.y());
        dq_agl_db.setElement(2, 1, dq_agl_dy.z());

        dq_agl_db.setElement(0, 2, dq_agl_dz.x());
        dq_agl_db.setElement(1, 2, dq_agl_dz.y());
        dq_agl_db.setElement(2, 2, dq_agl_dz.z());

        dq_agl_db.setElement(0, 3, dq_agl_dalpha.x());
        dq_agl_db.setElement(1, 3, dq_agl_dalpha.y());
        dq_agl_db.setElement(2, 3, dq_agl_dalpha.z());

        dq_agl_db.setElement(0, 4, dq_agl_dbeta.x());
        dq_agl_db.setElement(1, 4, dq_agl_dbeta.y());
        dq_agl_db.setElement(2, 4, dq_agl_dbeta.z());

        dq_agl_db.setElement(0, 5, dq_agl_dgamma.x());
        dq_agl_db.setElement(1, 5, dq_agl_dgamma.y());
        dq_agl_db.setElement(2, 5, dq_agl_dgamma.z());

        
        
        if(_debug>1) {
            System.out.printf("%s: Translation derivative of the alignment corrected hit:\n",this.getClass().getSimpleName());
            System.out.printf("dq_agl_dx=%s \ndq_agl_dy=%s \ndq_agl_dz=%s\n",dq_agl_dx.toString(),dq_agl_dy.toString(),dq_agl_dz.toString());
            System.out.printf("%s: Rotation derivative of the alignment corrected hit evaluated at trkpos(or x_vec)=%s:\n",this.getClass().getSimpleName(),x_vec.toString());
            System.out.printf("dq_agl_dalpha=%s \ndq_agl_dbeta=%s \ndq_agl_dgamma=%s\n",dq_agl_dalpha.toString(),dq_agl_dbeta.toString(),dq_agl_dgamma.toString());
            System.out.printf("%s: Putting it together\ndq_agl_db:\n%s\n",this.getClass().getSimpleName(),dq_agl_db.toString());
            
            //cross-check using manual calculation from note
            BasicMatrix dq_agl_db_tmp = new BasicMatrix(3,6);
            dq_agl_db_tmp.setElement(0, 0, 1);
            dq_agl_db_tmp.setElement(1, 0, 0);
            dq_agl_db_tmp.setElement(2, 0, 0);

            dq_agl_db_tmp.setElement(0, 1, 0);
            dq_agl_db_tmp.setElement(1, 1, 1);
            dq_agl_db_tmp.setElement(2, 1, 0);

            dq_agl_db_tmp.setElement(0, 2, 0);
            dq_agl_db_tmp.setElement(1, 2, 0);
            dq_agl_db_tmp.setElement(2, 2, 1);

            dq_agl_db_tmp.setElement(0, 3, 0);
            dq_agl_db_tmp.setElement(1, 3, x_vec.z());
            dq_agl_db_tmp.setElement(2, 3, -x_vec.y());

            dq_agl_db_tmp.setElement(0, 4, -x_vec.z());
            dq_agl_db_tmp.setElement(1, 4, 0);
            dq_agl_db_tmp.setElement(2, 4, x_vec.x());

            dq_agl_db_tmp.setElement(0, 5, x_vec.y());
            dq_agl_db_tmp.setElement(1, 5, -x_vec.x());
            dq_agl_db_tmp.setElement(2, 5, 0);
            
            System.out.printf("%s: Cross-check with this manual calculated matrix\ndq_agl_db_tmp:\n%s\n",this.getClass().getSimpleName(),dq_agl_db_tmp.toString());
            
        }

        return dq_agl_db;
         
    }
    
    
    public void printGlobalHitPositionDers(BasicMatrix dq_db) {
    
        System.out.printf("%s: Derivatives of the hit position w.r.t. to the global alignment parameters b: dq_agl_db---\n%s \n",this.getClass().getSimpleName(),dq_db.toString());
        System.out.printf("dx_dx = %8.3f dx_dy = %8.3f dx_dz = %8.3f\n",dq_db.e(0,0),dq_db.e(0,1),dq_db.e(0,2));
        System.out.printf("dy_dx = %8.3f dy_dy = %8.3f dy_dz = %8.3f\n",dq_db.e(1,0),dq_db.e(1,1),dq_db.e(1,2));
        System.out.printf("dz_dx = %8.3f dz_dy = %8.3f dz_dz = %8.3f\n",dq_db.e(2,0),dq_db.e(2,1),dq_db.e(2,2));        
        System.out.printf("dx_da = %8.3f dx_db = %8.3f dx_dc = %8.3f\n",dq_db.e(0,3),dq_db.e(0,4),dq_db.e(0,5));
        System.out.printf("dy_dx = %8.3f dy_dy = %8.3f dy_dz = %8.3f\n",dq_db.e(1,3),dq_db.e(1,4),dq_db.e(1,5));
        System.out.printf("dz_dx = %8.3f dz_dy = %8.3f dz_dz = %8.3f\n",dq_db.e(2,3),dq_db.e(2,4),dq_db.e(2,5));
        
    
    }
    
    public void printGlobalPredictedHitPositionDers(BasicMatrix dq_db) {
    
        System.out.printf("%s: Derivatives of the predicted hit position w.r.t. to the global alignment parameters b: dq_pgl_db---\n%s \n",this.getClass().getSimpleName(),dq_db.toString());
        System.out.printf("dx_dx = %8.3f dx_dy = %8.3f dx_dz = %8.3f\n",dq_db.e(0,0),dq_db.e(0,1),dq_db.e(0,2));
        System.out.printf("dy_dx = %8.3f dy_dy = %8.3f dy_dz = %8.3f\n",dq_db.e(1,0),dq_db.e(1,1),dq_db.e(1,2));
        System.out.printf("dz_dx = %8.3f dz_dy = %8.3f dz_dz = %8.3f\n",dq_db.e(2,0),dq_db.e(2,1),dq_db.e(2,2));        
        System.out.printf("dx_da = %8.3f dx_db = %8.3f dx_dc = %8.3f\n",dq_db.e(0,3),dq_db.e(0,4),dq_db.e(0,5));
        System.out.printf("dy_dx = %8.3f dy_dy = %8.3f dy_dz = %8.3f\n",dq_db.e(1,3),dq_db.e(1,4),dq_db.e(1,5));
        System.out.printf("dz_dx = %8.3f dz_dy = %8.3f dz_dz = %8.3f\n",dq_db.e(2,3),dq_db.e(2,4),dq_db.e(2,5));
        
    
    }
    
    
    public void printGlobalResidualDers(BasicMatrix dz_db) {
    
        System.out.printf("%s: Derivatives of the residual z=q_a - q_p w.r.t. to the global alignment parameters b: dz_db---\n%s \n",this.getClass().getSimpleName(),dz_db.toString());
        System.out.printf("dx_dx = %8.3f dx_dy = %8.3f dx_dz = %8.3f\n",dz_db.e(0,0),dz_db.e(0,1),dz_db.e(0,2));
        System.out.printf("dy_dx = %8.3f dy_dy = %8.3f dy_dz = %8.3f\n",dz_db.e(1,0),dz_db.e(1,1),dz_db.e(1,2));
        System.out.printf("dz_dx = %8.3f dz_dy = %8.3f dz_dz = %8.3f\n",dz_db.e(2,0),dz_db.e(2,1),dz_db.e(2,2));        
        System.out.printf("dx_da = %8.3f dx_db = %8.3f dx_dc = %8.3f\n",dz_db.e(0,3),dz_db.e(0,4),dz_db.e(0,5));
        System.out.printf("dy_da = %8.3f dy_db = %8.3f dy_dc = %8.3f\n",dz_db.e(1,3),dz_db.e(1,4),dz_db.e(1,5));
        System.out.printf("dz_da = %8.3f dz_db = %8.3f dz_dc = %8.3f\n",dz_db.e(2,3),dz_db.e(2,4),dz_db.e(2,5));
        
    
    }
    
    public void printLocalResidualDers(BasicMatrix dz_da) {
    
        System.out.printf("%s: Derivatives of the residual z=q_a - q_p w.r.t. to the local alignment parameters a: dz_da---\n%s \n",this.getClass().getSimpleName(),dz_da.toString());
        System.out.printf("du_du =     %8.3f du_dv =    %8.3f du_dw =     %8.3f\n",dz_da.e(0,0),dz_da.e(0,1),dz_da.e(0,2));
        System.out.printf("dv_du =     %8.3f dv_dv =    %8.3f dv_dw =     %8.3f\n",dz_da.e(1,0),dz_da.e(1,1),dz_da.e(1,2));
        System.out.printf("dw_du =     %8.3f dw_dv =    %8.3f dw_dw =     %8.3f\n",dz_da.e(2,0),dz_da.e(2,1),dz_da.e(2,2));        
        System.out.printf("du_dalpha = %8.3f du_dbeta = %8.3f du_dgamma = %8.3f\n",dz_da.e(0,3),dz_da.e(0,4),dz_da.e(0,5));
        System.out.printf("dv_dalpha = %8.3f dv_dbeta = %8.3f dv_dgamma = %8.3f\n",dz_da.e(1,3),dz_da.e(1,4),dz_da.e(1,5));
        System.out.printf("dw_dalpha = %8.3f dw_dbeta = %8.3f dw_dgamma = %8.3f\n",dz_da.e(2,3),dz_da.e(2,4),dz_da.e(2,5));
        
    
    }
    
    
    
    public BasicMatrix calculateGlobalPredictedHitPositionDers(HelicalTrackFit trk, Hep3Vector x_vec) {
            
        double d0 = trk.dca();
        double phi0 = trk.phi0();
        double R = trk.R();
        double s = HelixUtils.PathToXPlane(trk, x_vec.x(), 0, 0).get(0);
        double phi = -s/R + phi0;
        double slope = trk.slope();
        double xr = 0.0; 
        double yr = 0.0;
        
        
        //Derivatives of the predicted hit position qp for a translation of in x,y,z
            
        BasicMatrix dq_pgl_dx = new BasicMatrix(3,1);
        dq_pgl_dx.setElement(0, 0, dx_dx()); 
        dq_pgl_dx.setElement(1, 0, dy_dx(x_vec.x(), d0, phi0, R, phi)); 
        dq_pgl_dx.setElement(2, 0, dz_dx(x_vec.x(), xr, yr, d0, phi0, slope, R));

        BasicMatrix dq_pgl_dy = new BasicMatrix(3,1);
        dq_pgl_dy.setElement(0, 0, dx_dy(x_vec.y(), d0, phi0, R, phi));
        dq_pgl_dy.setElement(1, 0, dy_dy());
        dq_pgl_dy.setElement(2, 0, dz_dy(x_vec.y(), d0, phi0, slope, R, phi));
        
        BasicMatrix dq_pgl_dz = new BasicMatrix(3,1);
        dq_pgl_dz.setElement(0, 0, dx_dz(slope, R, phi));
        dq_pgl_dz.setElement(1, 0, dy_dz(slope, R, phi));
        dq_pgl_dz.setElement(2, 0, dz_dz());

        
        
        /*
         * Derivatives of the predicted hit position qp for a rotation of a,b,c around x,y,z
         * 
         * Note that since these rotations are not around the center of the sensor 
         * a rotation also leads to a translation, this is nto taken into account here
         * Since the lever arm might be substantial this might be non-negligible?
         * 
        */

        // THIS MIGHT NOT SINCE THE ROTATION IS NOT AROUND THE CENTER OF THE SENSOR!?
        
        Hep3Matrix ddeltaRprime_da = rotationDer_da();
        Hep3Matrix ddeltaRprime_db = rotationDer_db();
        Hep3Matrix ddeltaRprime_dc = rotationDer_dc();
        
        Hep3Vector dq_pgl_drota = VecOp.mult(ddeltaRprime_da, x_vec);
        Hep3Vector dq_pgl_drotb = VecOp.mult(ddeltaRprime_db, x_vec);
        Hep3Vector dq_pgl_drotc = VecOp.mult(ddeltaRprime_dc, x_vec);

        
        //put them all in one big matrix
        
        BasicMatrix dq_pgl_db = new BasicMatrix(3,6);
        
        dq_pgl_db.setElement(0, 0, dq_pgl_dx.e(0,0));
        dq_pgl_db.setElement(1, 0, dq_pgl_dx.e(1,0));
        dq_pgl_db.setElement(2, 0, dq_pgl_dx.e(2,0));

        dq_pgl_db.setElement(0, 1, dq_pgl_dy.e(0,0));
        dq_pgl_db.setElement(1, 1, dq_pgl_dy.e(1,0));
        dq_pgl_db.setElement(2, 1, dq_pgl_dy.e(2,0));

        dq_pgl_db.setElement(0, 2, dq_pgl_dz.e(0,0));
        dq_pgl_db.setElement(1, 2, dq_pgl_dz.e(1,0));
        dq_pgl_db.setElement(2, 2, dq_pgl_dz.e(2,0));

        dq_pgl_db.setElement(0, 3, dq_pgl_drota.x());
        dq_pgl_db.setElement(1, 3, dq_pgl_drota.y());
        dq_pgl_db.setElement(2, 3, dq_pgl_drota.z());

        dq_pgl_db.setElement(0, 4, dq_pgl_drotb.x());
        dq_pgl_db.setElement(1, 4, dq_pgl_drotb.y());
        dq_pgl_db.setElement(2, 4, dq_pgl_drotb.z());

        dq_pgl_db.setElement(0, 5, dq_pgl_drotc.x());
        dq_pgl_db.setElement(1, 5, dq_pgl_drotc.y());
        dq_pgl_db.setElement(2, 5, dq_pgl_drotc.z());
        
        
        
         if(_debug>1) {
            System.out.printf("%s: Translation derivative of the predicted hit position:\n",this.getClass().getSimpleName());
            System.out.printf("dq_pgl_dx=\n%s \ndq_pgl_dy=\n%s \ndq_pgl_dz=\n%s\n",dq_pgl_dx.toString(),dq_pgl_dy.toString(),dq_pgl_dz.toString());
            System.out.printf("%s: Rotation derivative of the predicted hit position:\n",this.getClass().getSimpleName());
            System.out.printf("dq_pgl_dalpha=\n%s \ndq_pgl_dbeta=\n%s \ndq_pgl_dgamma=\n%s\n",dq_pgl_drota.toString(),dq_pgl_drotb.toString(),dq_pgl_drotc.toString());
            System.out.printf("%s: Putting it together\ndq_pgl_db:\n%s\n",this.getClass().getSimpleName(),dq_pgl_db.toString());
         }
                 
        
         return dq_pgl_db;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //-------------------------------------------
    //Helper functions

    
    
    
    public double get_dphi(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        //Takes into account that phi=[-pi,pi] by checking where the points (x0,y0) and (x,y) are        
        double dphi = tmpdphi(x,xr,yr,d0,phi0,R,phi);        
        dphi = dphi > Math.PI ? dphi - 2*Math.PI :  dphi < -Math.PI ? dphi + 2*Math.PI : dphi;
        return dphi;
    }

    public double dsign_dR(double R) {
        return 0.0;
    }
    
    
    //Generic point y on circle/helix
    public double y(double x, double xr, double yr, double d0, double phi0, double R,double phi) {
        //double phi_angle = phi(x, xr, yr, d0, phi0, R);
        double value = yc(yr,d0,phi0,R) + R*Math.cos(phi);
        //double value = yolddangerous(x, xr, yr, d0, phi0, R);
        /*
        double ydanger = yolddangerous(x, xr, yr, d0, phi0, R);
        if(value!=ydanger) {
            System.out.println("ERROR ydanger " + ydanger + " is different than good y " + value +" !!");
            System.out.println("xint " + x + "  d0 " + d0 + " phi0 " + phi0 + " R " + R);
            System.out.println("R*Math.cos(phi0)=" + R*Math.cos(phi0) + " sign(R)*Math.sqrt( R*R - Math.pow(x - xc(xr,d0,phi0,R),2) )=" + sign(R)*Math.sqrt( R*R - Math.pow(x - xc(xr,d0,phi0,R),2) ) );
            System.out.println("Math.pow(x - xc(xr,d0,phi0,R),2)=" + Math.pow(x - xc(xr,d0,phi0,R),2) + "  ");
            System.exit(1);
        }
        
        */
        return value;
    }

    //Generic point y on circle/helix
    public double yolddangerous(double x, double xr, double yr, double d0, double phi0, double R) {
        return yc(yr,d0,phi0,R) + sign(R)*deltaxc(x,xr,d0,phi0,R);
    }

    //x-coordiante of center of circle/helix
    double xc(double xr,double d0, double phi0, double R) {
        return xr + (R-d0)*Math.sin(phi0);
    }

    //y-coordiante of center of circle/helix
    double yc(double yr,double d0, double phi0, double R) {
        return yr - (R-d0)*Math.cos(phi0);
    }
    
    // x-coordinate of point of closest approach
    double x0(double xr, double d0, double phi0) {
        return xr - d0*Math.sin(phi0);
    }
    
    // y-coordinate of point of closest approach
    double y0(double yr, double d0, double phi0) {
        return yr + d0*Math.cos(phi0);
    }
    
    
    // distance between generic point y and center of circle/helix => y-yc 
    // FIX THE NAME SO THAT THIS IS deltayc ->MAKES MORE SENSE
    public double deltaxc(double x, double xr, double d0,double phi0, double R) {
        return Math.sqrt( R*R - Math.pow(x - xc(xr,d0,phi0,R),2) );
    }
 
    // distance between generic point x and center of circle/helix => x-xc
    // FIX THE NAME SO THAT THIS IS deltaxc ->MAKES MORE SENSE
    public double deltayc(double x, double xr, double yr, double d0,double phi0, double R,double phi) {
        return Math.sqrt( R*R - Math.pow(y(x,xr,yr,d0,phi0,R,phi) - yc(yr,d0,phi0,R),2) );
    }

    // derivate of deltayc
    public double ddeltayc_dd0(double x, double xr, double d0,double phi0, double R) {
        //double num = sign(R)*sign(R) * Math.sin(phi0) * (x-xr-(-d0+R)*Math.sin(phi0));
        //double den = Math.sqrt( R*R - sign(R)*sign(R) * (R*R - Math.pow( x-xr-(-d0+R)*Math.sin(phi0), 2)));
        
        //Use xc to simplify
        double num = sign(R)*sign(R) * Math.sin(phi0) * (x-xc(xr,d0,phi0,R));
        double den = Math.sqrt( R*R - sign(R)*sign(R) * (R*R - Math.pow( x-xc(xr,d0,phi0,R), 2)));
        
        return num/den;
    }
    
    // derivate of deltayc
    public double ddeltayc_dphi0(double x, double xr, double yr, double d0, double phi0, double R) {
        //double num = (-d0+R)*Math.cos(phi0)*sign(R)*sign(R)*(x-xr-(-d0+R)*Math.sin(phi0));
        //double den = Math.sqrt( R*R - sign(R)*sign(R)*( R*R - Math.pow(x-xr-(-d0+R)*Math.sin(phi0), 2)  )     );
        //return -1.0*num/den;
        
        //Use yc and xc to simplify
        double num = ( yc(yr,d0,phi0,R)-yr)*sign(R)*sign(R)*(x-xc(xr,d0,phi0,R));
        double den = Math.sqrt( R*R - sign(R)*sign(R)*( R*R - Math.pow(x-xc(xr,d0,phi0,R), 2)  )   );
        return num/den;
        
    }
    
    // derivate of deltayc
    public double ddeltayc_dR(double x, double xr, double d0, double phi0, double R) {
        //simplifying using xc
        double num = ( 2*R - sign(R)*sign(R)*( 2*R + 2*Math.sin(phi0)*( x - xc(xr,d0,phi0,R)) )  -  2*sign(R)*( R*R - Math.pow(x-xc(xr,d0,phi0,R), 2))*dsign_dR(R)  );
        double den = 2*Math.sqrt( R*R - sign(R)*sign(R)*( R*R - Math.pow(x-xc(xr,d0,phi0,R), 2) )   );        
        return num/den;
    }
    
    // Azimuthal angle for PCA - used in dphi calculation
    public double phi1(double xr, double yr, double d0, double phi0, double R) {
        return Math.atan2( x0(xr,d0,phi0) - xc(xr,d0,phi0,R)  , y0(yr,d0,phi0) - yc(yr,d0,phi0,R) );
    }

    // Azimuthal angle for generic point - used in dphi calculation
    public double phi2(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        return Math.atan2( x - xc(xr,d0,phi0,R)  , y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R) );
    }

    
    // delta phi between point on helix (x,y) and point of closest approach (x0,y0)
    public double tmpdphi(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        return phi2(x,xr,yr,d0,phi0,R,phi) - phi1(xr,yr,d0,phi0,R);
    }
    
    // derivative of delta phi between point on helix (x,y) and point of closest approach (x0,y0)
    public double baddtmpdphi_dR(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        // this calculation has been large "simplified" from the original by using x, x0, xc, y, y0, yc
        double term1 = ((yc(yr,d0,phi0,R) - y0(yr,d0,phi0))*Math.sin(phi0) - (x0(xr,d0,phi0) - xc(xr,d0,phi0,R))*Math.cos(phi0) ) / (R*R);
        
        double num = Math.sin(phi0)*Math.pow(y(x, xr, yr, d0, phi0, R, phi) - y0(yr,d0,phi0),2) + ( x - xc(xr,d0,phi0,R))*( R + Math.sin(phi0)*(x-xc(xr,d0,phi0,R)) + Math.pow(y(x, xr, yr, d0, phi0, R, phi)-y0(yr,d0,phi0),2) );
        
        double den = (y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R))*Math.pow(x-xc(xr,d0,phi0,R),2) + sign(R)*sign(R)*Math.pow(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R),3);
        
        double term2 = sign(R) * num/den;
        
        return term1 + term2;
    }

   
     //public double phi(double xint,double xr, double yr, double d0, double phi0, double R) {
        
         //s = -dphi/C=-R*dphi=-R*(phi-phi0)
         //phi = -s/R+phi0
         //=>(z-z0)/slope = -R*(phi-phi0)
         //=>phi = (z-z0)/(slope*-R)+phi0
         //double z_pos = 0.0;
         //double p = z_pos
         
        //double arg1 = Math.sin(phi0) - (xint-x0(xr,d0,phi0))/R;
        //double arg2 = Math.cos(phi0) + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R;
        //double p = Math.atan2( arg1,arg2);
        
//        double arg1_2 = R*Math.sin(phi0) + (x0(xr,d0,phi0)-xint);
//        double arg2_2 = R*Math.cos(phi0) + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0));
//        double p_2 = Math.atan2( arg1,arg2);
//        
//        double arg1test = -sign(R )*Math.sin(phi0)+(x0(xr,d0,phi0)-xint)/R;
//        double arg2test = sign(R )*Math.cos(phi0)+(y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R;
//        double phitest=Math.atan2(arg1test, arg2test);

//        if(p!=p_2) {
//            System.out.println("ERROR phi " + p + " != p_2" + p_2 + "!!");
//            System.out.printf("arg1    =%f     arg2=%f\n",arg1,arg2);
//            System.out.printf("arg1_2=%f arg_2=%f\n",arg1_2,arg2_2);            
//            System.exit(1);
//        }

        /*
        if(p!=phitest) {
            System.out.println("ERROR phi " + p + " != phitest " + phitest + "!!");
            System.out.printf("arg1    =%f     arg2=%f\n",arg1,arg2);
            System.out.printf("arg1test=%f arg2test=%f\n",arg1test,arg2test);
            System.out.println("arg1: phi0=" + phi0 + " sin(phi0)=" + Math.sin(phi0) + " -(xint-x0(xr,d0,phi0))/R="+ - (xint-x0(xr,d0,phi0))/R);
            System.out.println("arg2: phi0=" + phi0 + " os(phi0)=" + Math.cos(phi0) + " + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R = " + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R);
            System.out.println("argtest1: -sign(R )*Math.sin(phi0) = " +-sign(R )*Math.sin(phi0)+ " + (x0(xr,d0,phi0)-xint)/R = " + (x0(xr,d0,phi0)-xint)/R );
            System.out.println("argtest2: sign(R )*Math.cos(phi0) = " + sign(R )*Math.cos(phi0) + " + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R = " + (y(xint,xr,yr,d0,phi0,R)-y0(yr,d0,phi0))/R);
            
            
            System.out.println(" phi " + p + " != p_2" + p_2 + "!!");
            System.out.printf("arg1    =%f     arg2=%f\n",arg1,arg2);
            System.out.printf("arg1_2=%f arg_2=%f\n",arg1_2,arg2_2);            
            
            
            System.exit(1);
        }
         */       
                
                
        //return p;
    //}
    
        // derivative of delta phi between point on helix (x,y) and point of closest approach (x0,y0)
    public double dtmpdphi_dR(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        // this calculation has been large "simplified" from the original by using x, x0, xc, y, y0, yc        
        //double num = R*sign(R)*(x-x0(xr,d0,phi0));
        //double den = (yc(yr,d0,phi0,R) - y0(yr,d0,phi0))*(2*(x-xc(xr,d0,phi0,R))-R*R);
        double num = -R*sign(R)*(x-x0(xr,d0,phi0));//PHA 30/08/12 fixed
        double den = -(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R))*(R*R); //PHA 30/08/12 fixed
        
        return num/den;
        
        
//        double term1 = ((yc(yr,d0,phi0,R) - y0(yr,d0,phi0))*Math.sin(phi0) +R*Math.cos(phi0) ) / (R*R);
//        
//        double num1 = sign(R)*Math.sin(phi0)*(y(x, xr, yr, d0, phi0, R) - y0(yr,d0,phi0));
//        
//        double num2 = sign(R)*(x-xc(xr,d0,phi0,R))*(R + Math.sin(phi0)*(x-xc(xr,d0,phi0,R)))/(y(x, xr, yr, d0, phi0, R)-yc(yr,d0,phi0,R));
//        
//        double den = Math.pow(x-xc(xr,d0,phi0,R),2) + sign(R)*sign(R)*Math.pow(y(x, xr, yr, d0, phi0, R) - yc(yr,d0,phi0,R),2);
//                
//        double term2 = (num1 + num2)/den;
//        
//        return term1 + term2;
    }

    
    // derivative of delta phi between point on helix (x,y) and point of closest approach (x0,y0)
    public double dtmpdphi_dd0(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
    
        double num = sign(R)*Math.sin(phi0)*Math.pow(x - xc(xr,d0,phi0,R),2) + sign(R)*Math.sin(phi0)*Math.pow(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R),2);
        
        double den = Math.pow(x-xc(xr,d0,phi0,R),2) + sign(R)*sign(R)*Math.pow( y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R),2);
        
        return -1/(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R))*num/den; 
        
    }

    
    // derivative of delta phi between point on helix (x,y) and point of closest approach (x0,y0)
    public double dtmpdphi_ddphi(double x, double xr, double yr, double d0, double phi0, double R, double phi) {
        
        double term1 = -(Math.pow(yc(yr,d0,phi0,R)-y0(yr,d0,phi0),2)+Math.pow(x0(xr,d0,phi0)-xc(xr,d0,phi0,R),2))/(R*R);
        
        double term2 = sign(R)*(yr-yc(yr,d0,phi0,R))/(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R))*(R*R)/( Math.pow(x-xc(xr,d0,phi0,R),2) + sign(R)*sign(R)*Math.pow(y(x, xr, yr, d0, phi0, R, phi) - yc(yr,d0,phi0,R),2) ); 
        
        return term1 + term2;
        
    }    
    
    double sign(double val) {
        return Math.signum(val);
    }
    
   
    
     /**
    *
    * Nested class that contains the numerical derivatives
    * Nested class to increase encapsulation and save this for future cross-checks
    */
    
    
    
    public class NumDerivatives {
       
        private double _eps = 1e-9;
        private int _type = 0; //0=five point stencil, 1=finite difference, 2=Newton difference quotient
        private Hep3Matrix[][] _rot_eps = new BasicHep3Matrix[3][2];
        private Hep3Matrix[][] _rot_twoeps = new BasicHep3Matrix[3][2];
        public NumDerivatives() {
            constructRotationMatrices();
        }
        public NumDerivatives(double eps, int type) {
            _eps = eps;
            _type = type;
            constructRotationMatrices();
        }
        public void setEpsilon(double eps) {
            _eps = eps;
        }
        public void setDebug(boolean debug) {
            _debug = debug ? 1 : 0;
        }
        public void setDebug(int debug) {
            _debug = debug;
        }
        public void setType(int type) {
            _type = type;
        }
        
        public final void constructRotationMatrices() { 
            String[] axis = {"x","y","z"};
            for(int i=0;i<3;++i) {
                _rot_eps[i][0] = getRotationMatrix(axis[i],_eps);
                _rot_twoeps[i][0] = getRotationMatrix(axis[i],2*_eps);
                _rot_eps[i][1] = getRotationMatrix(axis[i],-_eps);
                _rot_twoeps[i][1] = getRotationMatrix(axis[i],-2*_eps);
            }            
        }
    
        
        
        
        public BasicMatrix calculateLocalHelixDerivatives(HelicalTrackFit trk, double xint) {
            
            double d0 = trk.dca();
            double z0 = trk.z0();
            double R = trk.R();
            double phi0 = trk.phi0();
            double slope = trk.slope();
            
            BasicMatrix dfdq = new BasicMatrix(3,5); //3-dim,ntrackparams
            
            Hep3Vector df_dd0 = df_dd0(trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector df_dz0 = df_dz0(trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector df_dslope = df_dslope(trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector df_dphi0 = df_dphi0(trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector df_dR = df_dR(trk, xint, d0, z0, phi0, R, slope);
            
            dfdq.setElement(0, 0, df_dd0.x());
            dfdq.setElement(0, 1, df_dz0.x());
            dfdq.setElement(0, 2, df_dslope.x());
            dfdq.setElement(0, 3, df_dphi0.x());
            dfdq.setElement(0, 4, df_dR.x());
            
            dfdq.setElement(1, 0, df_dd0.y());
            dfdq.setElement(1, 1, df_dz0.y());
            dfdq.setElement(1, 2, df_dslope.y());
            dfdq.setElement(1, 3, df_dphi0.y());
            dfdq.setElement(1, 4, df_dR.y());
            
            dfdq.setElement(2, 0, df_dd0.z());
            dfdq.setElement(2, 1, df_dz0.z());
            dfdq.setElement(2, 2, df_dslope.z());
            dfdq.setElement(2, 3, df_dphi0.z());
            dfdq.setElement(2, 4, df_dR.z());
            
            return dfdq;
            
        }
        
        
        public BasicMatrix calculateGlobalPredictedHitPositionDers(HelicalTrackFit trk, Hep3Vector x_vec) {
          
            double d0 = trk.dca();
            double phi0 = trk.phi0();
            double R = trk.R();
            double s = HelixUtils.PathToXPlane(trk, x_vec.x(), 0, 0).get(0);
            double phi = -s/R + phi0;
            double slope = trk.slope();
            double xr = 0.0; 
            double yr = 0.0;


            //Derivatives of the predicted hit position qp for a translation of in x,y,z
            Hep3Vector dfdx = this.df_dx(trk, x_vec);
            Hep3Vector dfdy = this.df_dy(trk, x_vec);
            Hep3Vector dfdz = this.df_dz(trk, x_vec);
            
            //Use the same structure as the analytic derivatives
            
            BasicMatrix dq_pgl_dx = new BasicMatrix(3,1);
            dq_pgl_dx.setElement(0, 0, dfdx.x()); //dx_dx()); 
            dq_pgl_dx.setElement(1, 0, dfdx.y()); //dy_dx(x_vec.x(), d0, phi0, R, phi)); 
            dq_pgl_dx.setElement(2, 0, dfdx.z()); //dz_dx(x_vec.x(), xr, yr, d0, phi0, slope, R));

            BasicMatrix dq_pgl_dy = new BasicMatrix(3,1);
            dq_pgl_dy.setElement(0, 0, dfdy.x()); //dx_dy(x_vec.y(), d0, phi0, R, phi));
            dq_pgl_dy.setElement(1, 0, dfdy.y()); //dy_dy());
            dq_pgl_dy.setElement(2, 0, dfdy.z()); //dz_dy(x_vec.y(), d0, phi0, slope, R, phi));

            BasicMatrix dq_pgl_dz = new BasicMatrix(3,1);
            dq_pgl_dz.setElement(0, 0, dfdz.x()); //dx_dz(slope, R, phi));
            dq_pgl_dz.setElement(1, 0, dfdz.y()); //dy_dz(slope, R, phi));
            dq_pgl_dz.setElement(2, 0, dfdz.z()); //dz_dz());


            /*
             * Numerical derivative w.r.t. the rotation around x,y,z axes
             */
            
            Hep3Vector dfda = this.df_drot(trk, x_vec, "x");
            Hep3Vector dfdb = this.df_drot(trk, x_vec, "y");
            Hep3Vector dfdc = this.df_drot(trk, x_vec, "z");

            BasicMatrix dq_pgl_dalpha = new BasicMatrix(3,1);
            dq_pgl_dalpha.setElement(0, 0, dfda.x());
            dq_pgl_dalpha.setElement(1, 0, dfda.y());
            dq_pgl_dalpha.setElement(2, 0, dfda.z());

            BasicMatrix dq_pgl_dbeta = new BasicMatrix(3,1);
            dq_pgl_dbeta.setElement(0, 0, dfdb.x());
            dq_pgl_dbeta.setElement(1, 0, dfdb.y());
            dq_pgl_dbeta.setElement(2, 0, dfdb.z());

            BasicMatrix dq_pgl_dgamma = new BasicMatrix(3,1);
            dq_pgl_dgamma.setElement(0, 0, dfdc.x());
            dq_pgl_dgamma.setElement(1, 0, dfdc.y());
            dq_pgl_dgamma.setElement(2, 0, dfdc.z());


            //put them all in one big matrix

            BasicMatrix dq_pgl_db = new BasicMatrix(3,6);


            dq_pgl_db.setElement(0, 0, dq_pgl_dx.e(0,0));
            dq_pgl_db.setElement(1, 0, dq_pgl_dx.e(1,0));
            dq_pgl_db.setElement(2, 0, dq_pgl_dx.e(2,0));

            dq_pgl_db.setElement(0, 1, dq_pgl_dy.e(0,0));
            dq_pgl_db.setElement(1, 1, dq_pgl_dy.e(1,0));
            dq_pgl_db.setElement(2, 1, dq_pgl_dy.e(2,0));

            dq_pgl_db.setElement(0, 2, dq_pgl_dz.e(0,0));
            dq_pgl_db.setElement(1, 2, dq_pgl_dz.e(1,0));
            dq_pgl_db.setElement(2, 2, dq_pgl_dz.e(2,0));

            dq_pgl_db.setElement(0, 3, dq_pgl_dalpha.e(0,0));
            dq_pgl_db.setElement(1, 3, dq_pgl_dalpha.e(1,0));
            dq_pgl_db.setElement(2, 3, dq_pgl_dalpha.e(2,0));

            dq_pgl_db.setElement(0, 4, dq_pgl_dbeta.e(0,0));
            dq_pgl_db.setElement(1, 4, dq_pgl_dbeta.e(1,0));
            dq_pgl_db.setElement(2, 4, dq_pgl_dbeta.e(2,0));

            dq_pgl_db.setElement(0, 5, dq_pgl_dgamma.e(0,0));
            dq_pgl_db.setElement(1, 5, dq_pgl_dgamma.e(1,0));
            dq_pgl_db.setElement(2, 5, dq_pgl_dgamma.e(2,0));


            /*
            if(_debug) {
                System.out.printf("%s: Translation derivative of the predicted hit position:\n",this.getClass().getSimpleName());
                System.out.printf("dq_pgl_dx=\n%s \ndq_pgl_dy=\n%s \ndq_pgl_dz=\n%s\n",dq_pgl_dx.toString(),dq_pgl_dy.toString(),dq_pgl_dz.toString());
                System.out.printf("%s: Rotation derivative of the predicted hit position:\n",this.getClass().getSimpleName());
                System.out.printf("dq_pgl_dalpha=\n%s \ndq_pgl_dbeta=\n%s \ndq_pgl_dgamma=\n%s\n",dq_pgl_dalpha.toString(),dq_pgl_dbeta.toString(),dq_pgl_dgamma.toString());
                System.out.printf("%s: Putting it together\ndq_pgl_db:\n%s\n",this.getClass().getSimpleName(),dq_pgl_db.toString());
            }
            */

            return dq_pgl_db;
        
        }
        
        
        
        
        public Hep3Vector getNumDer(Hep3Vector f2h, Hep3Vector fh, Hep3Vector f, Hep3Vector fmh, Hep3Vector fm2h) {
            double[] ders = new double[3];
            for(int i=0;i<3;++i) {
                ders[i] = getNumDer(f2h.v()[i], fh.v()[i], f.v()[i], fmh.v()[i], fm2h.v()[i]);
            }
            return new BasicHep3Vector(ders);
        }
        public double getNumDer(double f2h, double fh, double f, double fmh, double fm2h) {
            double d = 0;
            try {
                switch (_type) {
                    case 0:                            
                        d = getFivePointStencilValue(f2h, fh, fmh, fm2h);
                        break;
                    case 1:
                        d = this.getFiniteDifference(fh, fmh);
                        break;
                    case 2:
                        d = this.getFiniteDifferenceNewton(fh, f);
                        break;
                    default:
                        throw new RuntimeException(this.getClass().getSimpleName() + ": error wrong numder type " + _type);
                }
            } catch (RuntimeException e) {
                System.out.println("Error " + e);
            }
            return d;
        }
        public double getFivePointStencilValue(double f2h, double fh, double fmh, double fm2h) {
            return 1/(12*_eps)*(-f2h + 8*fh - 8*fmh + fm2h);
        }
        public double getFiniteDifference(double fh, double fmh) {
            return 1/(2*_eps)*(fh - fmh);
        }
        public double getFiniteDifferenceNewton(double fh, double f) {
            return 1/(_eps)*(fh - f);
        }
        public double phiHelixFromX(double xint, double d0, double phi0, double R) {
            return Math.asin(1/R*((R-d0)*Math.sin(phi0) - xint));
        }
        public double phiHelixFromY(double y, double d0, double phi0, double R) {
            double A = R*R - Math.pow(y+(R-d0)*Math.cos(phi0),2);
            return Math.asin( 1/R*(-Math.signum(R))*Math.sqrt(A) );
        }
        public double phiHelixFromZ(double z, double z0, double slope, double R, double phi0) {
            return -1*(z-z0)/(R*slope)+phi0;
        }
        public Hep3Vector pointOnHelixFromPhi(double phi, double d0, double z0, double phi0, double R, double slope) {
             double dphi = phi-phi0;
            //Make sure dphi is in the valid range -pi,+pi
            if(dphi>Math.PI) dphi -= 2.0*Math.PI;
            if(dphi<-Math.PI) dphi += 2.0*Math.PI;
            
            double xc = (R-d0)*Math.sin(phi0);
            double yc = -1*(R-d0)*Math.cos(phi0);
            double x = xc - R*Math.sin(phi);
            double y = yc + R*Math.cos(phi);
            double s = -R*dphi;
            double z = z0 + s*slope;
            Hep3Vector p = new BasicHep3Vector(x,y,z);
            if(_debug>2) System.out.printf("PointOnHelix  s=%.3f dphi=%.3f  d0=%.3f R=%.2f phi0=%.3f phi=%.3f (xc=%.3f,yc=%.3f)\n",s,dphi,d0,R,phi0,phi,xc,yc);            
            return p;
        }
        public Hep3Vector pointOnHelix(double xint, double d0, double z0, double phi0, double R, double slope) {
            return this.pointOnHelix(null, xint, d0, z0, phi0, R, slope);
        }
        public Hep3Vector pointOnHelix(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            double phi = phiHelixFromX(xint,d0,phi0,R);
            Hep3Vector p = this.pointOnHelixFromPhi(phi, d0, z0, phi0, R, slope);
            if(trk!=null) {
                double s_tmp = HelixUtils.PathToXPlane(trk, xint, 0, 0).get(0);
                Hep3Vector p_tmp = HelixUtils.PointOnHelix(trk, s_tmp);
                Hep3Vector diff = VecOp.sub(p, p_tmp);
                if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": diff=%s p=%s p_tmp=%s\n",diff.toString(),p.toString(),p_tmp.toString());
            }
            return p;
        }
        public Hep3Vector pointOnHelixY(double yint, double d0, double z0, double phi0, double R, double slope) {
            return this.pointOnHelixY(null, yint, d0, z0, phi0, R, slope);
        }
        public Hep3Vector pointOnHelixY(HelicalTrackFit trk, double yint, double d0, double z0, double phi0, double R, double slope) {
            if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": pointOnHelixY\n");
            double phi = phiHelixFromY(yint,d0,phi0,R);
            Hep3Vector p = this.pointOnHelixFromPhi(phi, d0, z0, phi0, R, slope);
            if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": point = %s , phiHelixFromY=%.3f\n",p.toString(),phi);
            if(trk!=null) {
                //not sure this works as I use x here for the extrapolation
                double s_tmp = HelixUtils.PathToXPlane(trk, p.x(), 0, 0).get(0);
                Hep3Vector p_tmp = HelixUtils.PointOnHelix(trk, s_tmp);
                double RC_from_utils = trk.R();
                double phi_from_utils = trk.phi0() - s_tmp / RC_from_utils;
                Hep3Vector diff = VecOp.sub(p, p_tmp);
                if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": phi_from_utils=%.3f s_tmp=%s\n",phi_from_utils,s_tmp);
                if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": diff=%s p=%s p_tmp=%s\n",diff.toString(),p.toString(),p_tmp.toString());
            }
            return p;
        }
        public Hep3Vector pointOnHelixZ(double zint, double d0, double z0, double phi0, double R, double slope) {
            return this.pointOnHelixZ(null, zint, d0, z0, phi0, R, slope);
        }
        public Hep3Vector pointOnHelixZ(HelicalTrackFit trk, double zint, double d0, double z0, double phi0, double R, double slope) {
            double phi = this.phiHelixFromZ(zint, z0, slope, R, phi0);
            Hep3Vector p = this.pointOnHelixFromPhi(phi, d0, z0, phi0, R, slope);
            if(trk!=null) {
                //not sure this works as I use x here for the extrapolation
                double s_tmp = HelixUtils.PathToXPlane(trk, p.x(), 0, 0).get(0);
                Hep3Vector p_tmp = HelixUtils.PointOnHelix(trk, s_tmp);
                Hep3Vector diff = VecOp.sub(p, p_tmp);
                if(_debug>2) System.out.printf(this.getClass().getSimpleName()+": diff=%s p=%s p_tmp=%s\n",diff.toString(),p.toString(),p_tmp.toString());
            }
            return p;
        }

        
        public Hep3Vector df_dd0(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            Hep3Vector f = pointOnHelix(trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector fh = pointOnHelix(xint, d0+_eps, z0, phi0, R, slope);
            Hep3Vector fmh = pointOnHelix(xint, d0-_eps, z0, phi0, R, slope);
            Hep3Vector f2h = pointOnHelix(xint, d0+2*_eps, z0, phi0, R, slope);
            Hep3Vector fm2h = pointOnHelix(xint, d0-2*_eps, z0, phi0, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        public Hep3Vector df_dz0(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            Hep3Vector f = pointOnHelix( trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector fh = pointOnHelix(xint, d0, z0+_eps, phi0, R, slope);
            Hep3Vector fmh = pointOnHelix(xint, d0, z0-_eps, phi0, R, slope);
            Hep3Vector f2h = pointOnHelix(xint, d0, z0+2*_eps, phi0, R, slope);
            Hep3Vector fm2h = pointOnHelix(xint, d0, z0-2*_eps, phi0, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        public Hep3Vector df_dslope(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            Hep3Vector f = pointOnHelix( trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector fh = pointOnHelix(xint, d0, z0, phi0, R, slope+_eps);
            Hep3Vector fmh = pointOnHelix(xint, d0, z0, phi0, R, slope-_eps);
            Hep3Vector f2h = pointOnHelix(xint, d0, z0, phi0, R, slope+2*_eps);
            Hep3Vector fm2h = pointOnHelix(xint, d0, z0, phi0, R, slope-2*_eps);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        public Hep3Vector df_dphi0(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            Hep3Vector f = pointOnHelix( trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector fh = pointOnHelix(xint, d0, z0, phi0+_eps, R, slope);
            Hep3Vector fmh = pointOnHelix(xint, d0, z0, phi0-_eps, R, slope);
            Hep3Vector f2h = pointOnHelix(xint, d0, z0, phi0+2*_eps, R, slope);
            Hep3Vector fm2h = pointOnHelix(xint, d0, z0, phi0-2*_eps, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        public Hep3Vector df_dR(HelicalTrackFit trk, double xint, double d0, double z0, double phi0, double R, double slope) {
            Hep3Vector f = pointOnHelix( trk, xint, d0, z0, phi0, R, slope);
            Hep3Vector fh = pointOnHelix(xint, d0, z0, phi0, R+_eps, slope);
            Hep3Vector fmh = pointOnHelix(xint, d0, z0, phi0, R-_eps, slope);
            Hep3Vector f2h = pointOnHelix(xint, d0, z0, phi0, R+2*_eps, slope);
            Hep3Vector fm2h = pointOnHelix(xint, d0, z0, phi0, R-2*_eps, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }

        
        
        public Hep3Vector df_dx(HelicalTrackFit trk, Hep3Vector x_vec) {
            double d0 = trk.dca();
            double z0 = trk.z0();
            double R = trk.R();
            double phi0 = trk.phi0();
            double slope = trk.slope();
            Hep3Vector f = this.pointOnHelix(x_vec.x(), d0, z0, phi0, R, slope);
            Hep3Vector fh = this.pointOnHelix(x_vec.x()+_eps, d0, z0, phi0, R, slope);
            Hep3Vector fmh = this.pointOnHelix(x_vec.x()-_eps, d0, z0, phi0, R, slope);
            Hep3Vector f2h = this.pointOnHelix(x_vec.x()+2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector fm2h = this.pointOnHelix(x_vec.x()-2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        
        public Hep3Vector df_dy(HelicalTrackFit trk, Hep3Vector x_vec) {
            double d0 = trk.dca();
            double z0 = trk.z0();
            double R = trk.R();
            double phi0 = trk.phi0();
            double slope = trk.slope();
            Hep3Vector f = this.pointOnHelixY(trk,x_vec.y(), d0, z0, phi0, R, slope);
            Hep3Vector fh = this.pointOnHelixY(x_vec.y()+_eps, d0, z0, phi0, R, slope);
            Hep3Vector fmh = this.pointOnHelixY(x_vec.y()-_eps, d0, z0, phi0, R, slope);
            Hep3Vector f2h = this.pointOnHelixY(x_vec.y()+2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector fm2h = this.pointOnHelixY(x_vec.y()-2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        public Hep3Vector df_dz(HelicalTrackFit trk, Hep3Vector x_vec) {
            double d0 = trk.dca();
            double z0 = trk.z0();
            double R = trk.R();
            double phi0 = trk.phi0();
            double slope = trk.slope();
            Hep3Vector f = this.pointOnHelixZ(trk,x_vec.z(), d0, z0, phi0, R, slope);
            Hep3Vector fh = this.pointOnHelixZ(x_vec.z()+_eps, d0, z0, phi0, R, slope);
            Hep3Vector fmh = this.pointOnHelixZ(x_vec.z()-_eps, d0, z0, phi0, R, slope);
            Hep3Vector f2h = this.pointOnHelixZ(x_vec.z()+2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector fm2h = this.pointOnHelixZ(x_vec.z()-2*_eps, d0, z0, phi0, R, slope);
            Hep3Vector df = this.getNumDer(f2h, fh, f, fmh, fm2h);
            return df;
        }
        
       
        public int getAxisId(String axis) {
            if(!axis.matches("x|y|z")) {
                throw new RuntimeException(this.getClass().getSimpleName()+": this axis " + axis + " is nto defined");
            }
            return axis.equalsIgnoreCase("x") ? 0 : axis.equalsIgnoreCase("y") ? 1 : 2;
        }
        public Hep3Vector rotateEpsPlus(Hep3Vector x_vec,String axis) {
            return VecOp.mult(_rot_eps[getAxisId(axis)][0], x_vec);
        }
        public Hep3Vector rotateEpsMinus(Hep3Vector x_vec,String axis) {
            return VecOp.mult(_rot_eps[getAxisId(axis)][1], x_vec);
        }
        public Hep3Vector rotateTwoEpsPlus(Hep3Vector x_vec,String axis) {
            return VecOp.mult(_rot_twoeps[getAxisId(axis)][0], x_vec);
        }
        public Hep3Vector rotateTwoEpsMinus(Hep3Vector x_vec,String axis) {
            return VecOp.mult(_rot_twoeps[getAxisId(axis)][1], x_vec);
        }
        
        public Hep3Vector df_drot(HelicalTrackFit trk, Hep3Vector x_vec, String axis) {
            //NUmerical derivative w.r.t. the rotation around given axis (x,y,z)
            double d0 = trk.dca();
            double z0 = trk.z0();
            double R = trk.R();
            double phi0 = trk.phi0();
            double slope = trk.slope();
            
            /*
             * Apply an epsilon rotation around the axes and find the change 
             * that makes to the coordinates x,y,z -> dx,dy,dz
             * That relative change is used to calculate the num der 
             * w.r.t. the rotation
             */
            Hep3Vector x_vec_rot_a = this.rotateEpsPlus(x_vec, axis);
            Hep3Vector x_vec_rot_2a = this.rotateTwoEpsPlus(x_vec, axis);
            Hep3Vector x_vec_rot_ma = this.rotateEpsMinus(x_vec, axis);
            Hep3Vector x_vec_rot_m2a = this.rotateTwoEpsMinus(x_vec, axis);
            
            //dx/da
            Hep3Vector fx = this.pointOnHelix(x_vec.x(), d0, z0, phi0, R, slope);
            Hep3Vector fx_h = this.pointOnHelix(x_vec_rot_a.x(), d0, z0, phi0, R, slope);
            Hep3Vector fx_mh = this.pointOnHelix(x_vec_rot_ma.x(), d0, z0, phi0, R, slope);
            Hep3Vector fx_2h = this.pointOnHelix(x_vec_rot_2a.x(), d0, z0, phi0, R, slope);
            Hep3Vector fx_m2h = this.pointOnHelix(x_vec_rot_m2a.x(), d0, z0, phi0, R, slope);
            Hep3Vector dfx_da = this.getNumDer(fx_2h, fx_h, fx, fx_mh, fx_m2h);
            double dx_da = dfx_da.x();
            
            //dy/da
            Hep3Vector fy = this.pointOnHelixY(trk,x_vec.y(), d0, z0, phi0, R, slope);
            Hep3Vector fy_h = this.pointOnHelixY(x_vec_rot_a.y(), d0, z0, phi0, R, slope);
            Hep3Vector fy_mh = this.pointOnHelixY(x_vec_rot_ma.y(), d0, z0, phi0, R, slope);
            Hep3Vector fy_2h = this.pointOnHelixY(x_vec_rot_2a.y(), d0, z0, phi0, R, slope);
            Hep3Vector fy_m2h = this.pointOnHelixY(x_vec_rot_m2a.y(), d0, z0, phi0, R, slope);
            Hep3Vector dfy_da = this.getNumDer(fy_2h, fy_h, fy, fy_mh, fy_m2h);
            double dy_da = dfy_da.y();

            //dz/da
            Hep3Vector fz = this.pointOnHelixZ(trk,x_vec.z(), d0, z0, phi0, R, slope);
            Hep3Vector fz_h = this.pointOnHelixZ(x_vec_rot_a.z(), d0, z0, phi0, R, slope);
            Hep3Vector fz_mh = this.pointOnHelixZ(x_vec_rot_ma.z(), d0, z0, phi0, R, slope);
            Hep3Vector fz_2h = this.pointOnHelixZ(x_vec_rot_2a.z(), d0, z0, phi0, R, slope);
            Hep3Vector fz_m2h = this.pointOnHelixZ(x_vec_rot_m2a.z(), d0, z0, phi0, R, slope);
            Hep3Vector dfz_da = this.getNumDer(fz_2h, fz_h, fz, fz_mh, fz_m2h);
            double dz_da = dfz_da.z();
            Hep3Vector dfda = new BasicHep3Vector(dx_da,dy_da,dz_da);
            return dfda;
        }
        
        
        
        
        
    }
    
    
    
    
    
    
    
    
    
    
    //-------------------------------------------
    

    
    
    
    /**
    *
    * Nested class to increase encapsulation and save this for future cross-checks
    */
    public class OldAlignmentUtils {
        public OldAlignmentUtils() {}
        public BasicMatrix calculateLocalHelixDerivatives(HelicalTrackFit trk,double xint) {
             //get track parameters.
            double d0 = trk.dca();
            double z0 = trk.z0();
            double slope = trk.slope();
            double phi0 = trk.phi0();
            double R = trk.R();
    //strip origin is defined in the tracking coordinate system (x=beamline)
            double s = HelixUtils.PathToXPlane(trk, xint, 0, 0).get(0);
            double phi = -s/R + phi0;
            double[][] dfdq = new double[3][5];
            //dx/dq
            //these are wrong for X, but for now it doesn't matter
            dfdq[0][0] = Math.sin(phi0);
            dfdq[0][1] = 0;
            dfdq[0][2] = 0;
            dfdq[0][3] = d0 * Math.cos(phi0) + R * Math.sin(phi0) - s * Math.cos(phi0);
            dfdq[0][4] = (phi - phi0) * Math.cos(phi0);
            double[] mydydq = dydq(R, d0, phi0, xint, s);
            double[] mydzdq = dzdq(R, d0, phi0, xint, slope, s);
            for (int i = 0; i < 5; i++) {
                dfdq[1][i] = mydydq[i];
                dfdq[2][i] = mydzdq[i];
            }

            BasicMatrix dfdqGlobal = FillMatrix(dfdq, 3, 5);
            return dfdqGlobal;
        }
        private double[] dydq(double R, double d0, double phi0, double xint, double s) {
            double[] dy = new double[5];
    //        dy[0] = Math.cos(phi0) + Cot(phi0 - s / R) * Csc(phi0 - s / R) * dsdd0(R, d0, phi0, xint);
            dy[0] = Math.cos(phi0) - Sec(phi0 - s / R) * Math.tan(phi0 - s / R) * dsdd0(R, d0, phi0, xint);
            dy[1] = 0;
            dy[2] = 0;
    //        dy[3] = (-(d0 - R)) * Math.sin(phi0) - R * Cot(phi0 - s / R) * Csc(phi0 - s / R) * (1 - dsdphi(R, d0, phi0, xint) / R);
            dy[3] = (-(d0 - R)) * Math.sin(phi0) + Sec(phi0 - s / R) * Math.tan(phi0 - s / R) * (R - dsdphi(R, d0, phi0, xint));
            //        dy[4] = -Math.cos(phi0) + Csc(phi0 - s / R) - R * Cot(phi0 - s / R) * Csc(phi0 - s / R) * (s / (R * R) - dsdR(R, d0, phi0, xint) / R);
            dy[4] = -Math.cos(phi0) + Sec(phi0 - s / R) + (1 / R) * Sec(phi0 - s / R) * Math.tan(phi0 - s / R) * (s - R * dsdR(R, d0, phi0, xint));
            return dy;
        }
        private double[] dzdq(double R, double d0, double phi0, double xint, double slope, double s) {
            double[] dz = new double[5];
            dz[0] = slope * dsdd0(R, d0, phi0, xint);
            dz[1] = 1;
            dz[2] = s;
            dz[3] = slope * dsdphi(R, d0, phi0, xint);
            dz[4] = slope * dsdR(R, d0, phi0, xint);
            return dz;
        }
        
        private double dsdphi(double R, double d0, double phi0, double xint) {
            double sqrtTerm = Math.sqrt(R * R - Math.pow(((d0 - R) * Math.sin(phi0) + xint), 2));
            double rsign = Math.signum(R);
            double dsdphi = R * (sqrtTerm + rsign * d0 * Math.cos(phi0) - rsign * R * Math.cos(phi0)) / sqrtTerm;
    //        if (_debug>2)
    //            System.out.println("xint = " + xint + "; dsdphi = " + dsdphi);
            return dsdphi;
        }

        private double dsdd0(double R, double d0, double phi0, double xint) {
            double sqrtTerm = Math.sqrt(R * R - Math.pow(((d0 - R) * Math.sin(phi0) + xint), 2));
            double rsign = Math.signum(R);
            double dsdd0 = rsign * (R * Math.sin(phi0)) / sqrtTerm;
    //        if (_DEBUG)
    //            System.out.println("xint = " + xint + "; dsdd0 = " + dsdd0);
            return dsdd0;
        }
        
        private double dsdR(double R, double d0, double phi0, double xint) {
        double sqrtTerm = Math.sqrt(R * R - Math.pow(((d0 - R) * Math.sin(phi0) + xint), 2));

        double rsign = Math.signum(R);
            double dsdr = (1 / sqrtTerm) * ((-rsign * xint) + (-rsign) * d0 * Math.sin(phi0)
                    + Math.atan2(R * Math.cos(phi0), (-R) * Math.sin(phi0))
                    * sqrtTerm
                    - Math.atan2(rsign * sqrtTerm, xint + (d0 - R) * Math.sin(phi0))
                    * sqrtTerm);


    //        if (_DEBUG)
    //            System.out.println("xint = " + xint + "; dsdr = " + dsdr);
            return dsdr;

        }
        
        private double Sec(double val) {
            return 1 / Math.cos(val);
        }

        
    
    }
    
    
    
}
