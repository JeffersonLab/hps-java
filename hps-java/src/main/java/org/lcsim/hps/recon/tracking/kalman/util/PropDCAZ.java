package org.lcsim.hps.recon.tracking.kalman.util;

// May need to remove next line when moved to proper package.
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropDirected;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackDerivative;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfcylplane.PropCylZ;
import org.lcsim.recon.tracking.trfdca.PropDCACyl;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;


/**
 * Propagates tracks from a DCA surface to a ZPlane.
 * It does so by creating an intermediate Cylinder surface,
 * and using existing code to perform the transformation in two steps.
 *<p>
 * Propagation will fail if either the origin is not a DCA surface,
 * or the destination is not a ZPlane.
 *<p>
 * The default direction for propagation is forward.
 *<p>
 * PropDCACyl and PropCylZ do not work in all circumstances.  See those
 * codes for details.
 *
 *
 *@author $Author: jeremy $
 *@version $Id: PropDCAZ.java,v 1.1 2011/07/28 18:28:19 jeremy Exp $
 *
 * Date $Date: 2011/07/28 18:28:19 $
 *
 */
public class PropDCAZ extends PropDirected
{
    
    // static variables
    // Assign track parameter indices
    
    public static final int IRSIGNED = SurfDCA.IRSIGNED;
    public static final int IZ_DCA   = SurfDCA.IZ;
    public static final int IPHID    = SurfDCA.IPHID;
    public static final int ITLM_DCA = SurfDCA.ITLM;
    public static final int IQPT_DCA = SurfDCA.IQPT;
    
    public static final int IPHI     = SurfCylinder.IPHI;
    public static final int IZ_CYL   = SurfCylinder.IZ;
    public static final int IALF     = SurfCylinder.IALF;
    public static final int ITLM_CYL = SurfCylinder.ITLM;
    public static final int IQPT_CYL = SurfCylinder.IQPT;
        
    private static final int IX      = SurfZPlane.IX;
    private static final int IY      = SurfZPlane.IY;
    private static final int IDXDZ   = SurfZPlane.IDXDZ;
    private static final int IDYDZ   = SurfZPlane.IDYDZ;
    private static final int IQP     = SurfZPlane.IQP;


    // Attributes   ***************************************************
     
    // bfield * BFAC
    private double _bfac;
    
    
    // Methods   ******************************************************
    
    // static methods
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public  static String typeName()
    { return "PropDCAZ";
    }
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public  static String staticType()
    { return typeName();
    }
    
    
    
    /**
     *Construct an instance from a constant solenoidal magnetic field in Tesla.
     *
     * @param   bfield The magnetic field strength in Tesla.
     */
    public PropDCAZ(double bfield)
    {
        super(PropDir.FORWARD);
        _bfac=bfield * TRFMath.BFAC;
    }
    
    /**
     *Clone an instance.
     *
     * @return A Clone of this instance.
     */
    public Propagator newPropagator()
    {
        return new PropDCAZ( bField() );
    }
    
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public String type()
    { return staticType();
    }
    
    /**
     *Propagate a track without error in the specified direction.
     *and return the derivative matrix in deriv.
     *
     * The track parameters for a cylinder are:
     * phi z alpha tan(lambda) curvature
     *
     * @param   trv The VTrack to propagate.
     * @param   srf The Surface to which to propagate.
     * @param   dir The direction in which to propagate.
     * @param   deriv The track derivatives to update at the surface srf.
     * @return The propagation status.
     */
    public PropStat vecDirProp( VTrack trv,  Surface srf,
            PropDir dir, TrackDerivative deriv )
    {
        return dcaZPropagate( _bfac, trv, srf, dir, deriv );
    }
    
    /**
     *Propagate a track without error in the specified direction.
     *
     * The track parameters for a cylinder are:
     * phi z alpha tan(lambda) curvature
     *
     * @param   trv The VTrack to propagate.
     * @param   srf The Surface to which to propagate.
     * @param   dir The direction in which to propagate.
     * @return The propagation status.
     */
    public PropStat vecDirProp( VTrack trv,  Surface srf,
            PropDir dir )
    {
        TrackDerivative deriv =null;
        return vecDirProp(trv, srf, dir, deriv);
        
    }
    
    // propagate a track with error in the specified direction
    //  PropStat err_dir_prop( ETrack& tre,  Surface& srf,
    //                                            PropDir dir ) ;
    
    //
    
    /**
     *Return the strength of the magnetic field in Tesla.
     *
     * @return The strength of the magnetic field in Tesla.
     */
    public  double bField()
    {
        return _bfac/TRFMath.BFAC;
    }
    
    
    /**
     *output stream
     * @return  A String representation of this instance.
     */
    public String toString()
    {
        return "DCA propagation to a ZPlane with constant "
                + bField() + " Tesla field";
    }
    
    
    
    /**
     * Propagate from dca to cylinder.
     * Why is this public?
     *
     * @param   _bfac The numerical factor (including the field)
     * @param   trv The VTrack to propagate.
     * @param   srf The cylindrical surface to which to propagate.
     * @param   dir The direction in which to propagate.
     * @param   deriv The track derivatives to update at the surface srf.
     * @return The propagation status.
     */
    public PropStat dcaZPropagate( double          _bfac,
				   VTrack          trv,
				   Surface         srf,
				   PropDir         dir,
				   TrackDerivative deriv )
    {

        // Construct return status
        PropStat pstat = new PropStat();
        
        // Fetch the originating Surface and check it is a DCA surface
        Surface srf0 = trv.surface();
        Assert.assertTrue( srf0.pureType().equals(SurfDCA.staticType() ));
        if ( ! srf0.pureType( ).equals(SurfDCA.staticType()) )
            return pstat;
        SurfDCA srf_dca = ( SurfDCA ) srf0;
        
        // Check that dca surface has zero tilt.
        boolean tilted = srf_dca.dXdZ() != 0 || srf_dca.dYdZ() != 0;
        Assert.assertTrue(!tilted);
        if(tilted)    return pstat;
        
        // Check that the destination surface is a ZPlane
        Assert.assertTrue( srf.pureType().equals(SurfZPlane.staticType()) );
        if ( !srf.pureType( ).equals(SurfZPlane.staticType()) )
            return pstat;
        SurfZPlane srf_zp = ( SurfZPlane ) srf;

	// Instantiate the intermediate propagators.
	PropDCACyl prop1 = new PropDCACyl( bField() );
	PropCylZ   prop2 = new PropCylZ( bField() );

	// Radius of a cylinder going through the PCA ...
	// But:
	//   PropDCACyl requires that the cylinder be at a different radius than the DCA
	//   so put it 100 microns outside the dca.
	double rhack = Math.abs(trv.vector(IRSIGNED))+0.01;
	
	// Instantiate the surface at the intermediate step.
	SurfCylinder srf_cyl = new SurfCylinder(rhack);

	// Setup for receiving the derivatives.
	TrackDerivative d1 = null;
	TrackDerivative d2 = null;
	if ( deriv != null ){
	    d1 = new TrackDerivative();
	    d2 = new TrackDerivative();
	}
	
	// Save the track z direction for later use.
	boolean forward = trv.vector(ITLM_DCA)>=0.;

	// Do the propagation in two steps.
	PropStat p1 = prop1.vecDirProp(trv, srf_cyl, dir, d1);
	if ( !p1.success() ) return p1;
	PropStat p2 = prop2.vecDirProp(trv, srf_zp,  dir, d2);
	if ( !p2.success() ) return p2;

	// Forward/backward is defined wrt positive z axis.
	if ( forward ){
	    trv.setForward();
	} else{
	    trv.setBackward();
	}

	// Set the transformation matrix to the product of the those from
	// the two intermediate steps.
	if ( deriv != null ){
	    deriv.set(d2.times(d1));
	}

	// Set properties of the return value.
	if ( p2.forward() ){
	    pstat.setForward();
	}else if ( p2.backward() ){
	    pstat.setBackward();
	} else if ( p2.same() ){
	    pstat.setSame();
	}
	double s = p1.pathDistance() + p2.pathDistance();
	pstat.setPathDistance(s);



	return pstat;
	
    }
    
}
