package org.hps.recon.tracking.kalman;
// ThinXYPlaneMs

import org.lcsim.recon.tracking.trfbase.ETrack;
import org.lcsim.recon.tracking.trfbase.Interactor;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackError;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfxyp.SurfXYPlane;
/**
 *
 * Class for modifying the covariance matrix of a track to account
 * for multiple scattering at a thin XY-plane whose material is
 *represented by the number of radiation lengths.
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */
public class ThinXYPlaneMs extends Interactor
{
    
    // static methods
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public static String typeName()
    { return "ThinXYPlaneMs"; }
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public static String staticType()
    { return typeName(); }
    
    // static attributes
    // Assign track parameter indices.
    
    private static final int IV = SurfXYPlane.IV;
    private static final int IZ   = SurfXYPlane.IZ;
    private static final int IDVDU = SurfXYPlane.IDVDU;
    private static final int IDZDU = SurfXYPlane.IDZDU;
    private static final int IQP  = SurfXYPlane.IQP;
    
    //attributes
    private  double _radLength;
    
    //non-static methods
    
    // Constructor.  The Interactor is constructed with the
    // appropriate number of radiation lengths.
    
    /**
     * Construct an instance from the number of radiation
     * lengths of the thin xy plane material.
     * The Interactor is constructed with the
     * appropriate number of radiation lengths.
     *
     * @param   radLength The thickness of the material in radiation lengths.
     */
    public ThinXYPlaneMs(double radLength)
    {
        _radLength = radLength;
    }
    
    /**
     *Interact the given track in this xy plane,
     *using the thin material approximation for multiple scattering.
     *Note that the track parameters are not modified. Only the
     *covariance matrix is updated to reflect the uncertainty caused
     *by traversing the thin xy plane of material.
     *
     * @param   tre The ETrack to scatter.
     */
    public void interact(ETrack tre)
    {
        
        // This can only be used with XYplanes... check that we have one..
        
        SurfXYPlane XYpl = new SurfXYPlane( 10.0, 0.0 );
        Surface srf = tre.surface();
        Assert.assertTrue( srf.pureType().equals(XYpl.pureType()) );
        
        TrackError cleanError = tre.error();
        TrackError newError = new TrackError(cleanError);
        
        // set the rms scattering appropriate to this momentum
        
        // Theta = (0.0136 GeV)*(z/p)*(sqrt(radLength))*(1+0.038*log(radLength))
        
        
        
        TrackVector theVec = tre.vector();
        double trackMomentum = theVec.get(IQP);
        
        double f = theVec.get(IDVDU);
        double g = theVec.get(IDZDU);
        
        double theta = Math.atan(Math.sqrt(f*f + g*g));
        double phi = f!=0. ? Math.atan(Math.sqrt((g*g)/(f*f))) : TRFMath.PI2;
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
        
        double uhat = Math.sqrt(1-Math.sin(theta)*Math.sin(theta));
        double vhat = Math.sin(theta)*Math.cos(phi);
        double zhat = Math.sin(theta)*Math.sin(phi);
        
        double Qvu,Qzu,Qvz;
        
        // The MS covariance matrix can now be set.
        
        // **************** code for matrix ***********************//
        
        // Insert values for upper triangle... use symmetry to set lower.
        
        double norm = Math.sqrt(vhat*vhat + zhat*zhat);
        
        Qvu = (zhat/(norm*uhat))*(zhat/(norm*uhat));
        Qvu += Math.pow((vhat/norm)*(1 + (norm*norm)/(uhat*uhat)),2.0);
        Qvu *= stdTheta;
        Qvu *= stdTheta;
        
        Qzu = (vhat/(norm*uhat))*(vhat/(norm*uhat));
        Qzu += Math.pow((zhat/norm)*(1 + (norm*norm)/(uhat*uhat)),2.0);
        Qzu *= stdTheta;
        Qzu *= stdTheta;
        
        Qvz = - vhat*zhat/(uhat*uhat);
        Qvz += vhat*zhat/(norm*norm)*Math.pow((1 + (norm*norm)/(uhat*uhat)),2.0);
        Qvz *= stdTheta;
        Qvz *= stdTheta;
        
        newError.set(IDVDU,IDVDU, newError.get(IDVDU,IDVDU) + Qvu);
        newError.set(IDZDU,IDZDU, newError.get(IDZDU,IDZDU) + Qzu);
        newError.set(IDVDU,IDZDU, newError.get(IDVDU,IDZDU) + Qvz);
        
        
        tre.setError( newError );
        
    }
    
    // method for adding the interaction with direction
    public void interact_dir(ETrack tre, PropDir dir)
    {
        interact(tre);
    }
    
    /**
     *Return the number of radiation lengths.
     *
     * @return The thickness of the scattering material in radiation lengths.
     */
    public double radLength()
    {
        return _radLength;
    }
    
    /**
     *Make a clone of this object.
     *
     * @return A Clone of this instance.
     */
    public Interactor newCopy()
    {
        return new ThinXYPlaneMs(_radLength);
    }
    
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public String type()
    { return staticType(); }
    
    /**
     *output stream
     *
     * @return  A String representation of this instance.
     */
    public String toString()
    {
        return "ThinXYPlaneMs with "+_radLength+" radiation lengths";
    }
    
    
    
    
}

