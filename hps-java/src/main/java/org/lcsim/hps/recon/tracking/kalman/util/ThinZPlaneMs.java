package  org.lcsim.hps.recon.tracking.kalman.util;
//package  org.lcsim.recon.tracking.trfzp;

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Interactor;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

/**
 * This class modifies the covariance matrix of a track
 *corresponding to multiple
 *scattering in a thin z plane whose material is
 *represented by the number of radiation lengths.
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */

public class ThinZPlaneMs extends Interactor
{
    
    // static methods
    
    //
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public  static String typeName()
    { return "ThinZPlaneMs"; }
    
    //
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public  static String staticType()
    { return typeName(); }
    
    // static attributes
    // Assign track parameter indices.
    
    private static final int IX = SurfZPlane.IX;
    private static final int IY   = SurfZPlane.IY;
    private static final int IDXDZ = SurfZPlane.IDXDZ;
    private static final int IDYDZ = SurfZPlane.IDYDZ;
    private static final int IQP  = SurfZPlane.IQP;
    
    
    // attributes
    private double _radLength;
    
    //
    
    /**
     * Construct an instance from the number of radiation
     * lengths of the thin z plane material.
     * The Interactor is constructed with the
     * appropriate number of radiation lengths.
     *
     * @param   radLength The thickness of the material in radiation lengths.
     */
    public ThinZPlaneMs( double radLength )
    {
        _radLength = radLength;
    }
    
    //
    
    /**
     *Interact the given track in this z plane,
     *using the thin material approximation for multiple scattering.
     *Note that the track parameters are not modified. Only the
     *covariance matrix is updated to reflect the uncertainty caused
     *by traversing the thin z plane of material.
     *
     * @param   tre The ETrack to scatter.
     */
    public void interact(ETrack tre)
    {
        // This can only be used with zplanes... check that we have one..
        SurfZPlane szp = new SurfZPlane(1.0);
        Surface srf = tre.surface();
        Assert.assertTrue( srf.pureType().equals(szp.pureType()) );
        
        TrackError cleanError = tre.error();
        TrackError newError = new TrackError(cleanError);
        
        // set the rms scattering appropriate to this momentum
        
        // Theta = (0.0136 GeV)*(z/p)*(sqrt(radLength))*(1+0.038*log(radLength))
        
        
        
        TrackVector theVec = tre.vector();
        double trackMomentum = theVec.get(IQP);
        
        double f = theVec.get(IDXDZ);
        double g = theVec.get(IDYDZ);
        
        double theta = Math.atan(Math.sqrt(f*f + g*g));
        double phi = f!=0 ? Math.atan(Math.sqrt((g*g)/(f*f))) : TRFMath.PI2;
        if ( f==0 && g<0 ) phi=3*TRFMath.PI2;
        if((f<0)&&(g>0))
            phi = Math.PI - phi;
        if((f<0)&&(g<0))
            phi = Math.PI + phi;
        if((f>0)&&(g<0))
            phi = 2*Math.PI - phi;
        
        double trueLength = _radLength/Math.cos(theta);
        
        double stdTheta = (0.0136)*trackMomentum*Math.sqrt(trueLength)*
                (1 + 0.038*Math.log(trueLength));
        
        double zhat = Math.sqrt(1-Math.sin(theta)*Math.sin(theta));
        double xhat = Math.sin(theta)*Math.cos(phi);
        double yhat = Math.sin(theta)*Math.sin(phi);
        
        double Qxz,Qyz,Qxy;
        
        // The MS covariance matrix can now be set.
        
        // **************** code for matrix *********************** //
        
        
        // Insert values for upper triangle... use symmetry to set lower.

        double norm = Math.sqrt(xhat*xhat + yhat*yhat);
        
	double fac = stdTheta*stdTheta/norm/norm/zhat/zhat;
	Qxz = fac*( yhat*yhat + f*f);
	Qyz = fac*( xhat*xhat + g*g);
	Qxy = fac*( -xhat*yhat + f*g);
	
        
        newError.set(IDXDZ,IDXDZ, newError.get(IDXDZ,IDXDZ) + Qxz);
        newError.set(IDYDZ,IDYDZ, newError.get(IDYDZ,IDYDZ) + Qyz);
        newError.set(IDXDZ,IDYDZ, newError.get(IDXDZ,IDYDZ) + Qxy);
        
        //**********************************************************************
        
        tre.setError( newError );
        
    }
    
    //
    
    /**
     *Return the number of radiation lengths.
     *
     * @return The thickness of the scattering material in radiation lengths.
     */
    public double radLength()
    {
        return _radLength;
    }
    
    
    //
    
    /**
     *Make a clone of this object.
     *
     * @return A Clone of this instance.
     */
    public Interactor newCopy()
    {
        return new ThinZPlaneMs(_radLength);
    }
    
    
    //
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public String type()
    {
        return staticType();
    }
    
    
    
    /**
     *output stream
     *
     * @return  A String representation of this instance.
     */
    public String toString()
    {
        return "ThinZPlaneMs with "+_radLength+" radiation lengths";
    }
    
}
