package org.hps.recon.tracking.kalman;

// May need to remove next line when moved to proper package.
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropDirected;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackDerivative;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
//import org.lcsim.recon.tracking.trfdca.PropDCACyl;
import org.lcsim.recon.tracking.trfcylplane.PropCylXY;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfxyp.SurfXYPlane;

/**
 * Propagates tracks from a DCA surface to an XYPlane.
 * It does so by creating an intermediate Cylinder surface,
 * and using existing code to perform the transformation in two steps.
 *<p>
 * Propagation will fail if either the origin is not a DCA surface,
 * or the destination is not a XYPlane.
 *<p>
 * The default direction for propagation is forward.
 *<p>
 * PropDCACyl and PropCylXY do not work in all circumstances.  See those
 * codes for details.
 *
 *
 *@author $Author: mgraham $
 *@version $Id: PropDCAXY.java,v 1.3 2011/11/16 18:00:03 mgraham Exp $
 *
 * Date $Date: 2011/11/16 18:00:03 $
 *
 */
public class PropDCAXY extends PropDirected {

    // static variables
    // Assign track parameter indices
    public static final int IRSIGNED = SurfDCA.IRSIGNED;
    public static final int IZ_DCA = SurfDCA.IZ;
    public static final int IPHID = SurfDCA.IPHID;
    public static final int ITLM_DCA = SurfDCA.ITLM;
    public static final int IQPT_DCA = SurfDCA.IQPT;
    public static final int IPHI = SurfCylinder.IPHI;
    public static final int IZ_CYL = SurfCylinder.IZ;
    public static final int IALF = SurfCylinder.IALF;
    public static final int ITLM_CYL = SurfCylinder.ITLM;
    public static final int IQPT_CYL = SurfCylinder.IQPT;
    private static final int IV = SurfXYPlane.IV;
    private static final int IZC = SurfXYPlane.IZ;
    private static final int IDVDU = SurfXYPlane.IDVDU;
    private static final int IDZDU = SurfXYPlane.IDZDU;
    private static final int IQP_XY = SurfXYPlane.IQP;
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
    public static String typeName() {
        return "PropDCAXY";
    }

    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public static String staticType() {
        return typeName();
    }

    /**
     *Construct an instance from a constant solenoidal magnetic field in Tesla.
     *
     * @param   bfield The magnetic field strength in Tesla.
     */
    public PropDCAXY(double bfield) {
        super(PropDir.FORWARD);
        _bfac = bfield * TRFMath.BFAC;
    }

    /**
     *Clone an instance.
     *
     * @return A Clone of this instance.
     */
    public Propagator newPropagator() {
        return new PropDCAXY(bField());
    }

    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public String type() {
        return staticType();
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
    public PropStat vecDirProp(VTrack trv, Surface srf,
            PropDir dir, TrackDerivative deriv) {
        return dcaXYPropagate(_bfac, trv, srf, dir, deriv);
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
    public PropStat vecDirProp(VTrack trv, Surface srf,
            PropDir dir) {
        TrackDerivative deriv = null;
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
    public double bField() {
        return _bfac / TRFMath.BFAC;
    }

    /**
     *output stream
     * @return  A String representation of this instance.
     */
    public String toString() {
        return "DCA propagation to an XYPlane with constant "
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
    public PropStat dcaXYPropagate(double _bfac,
            VTrack trv,
            Surface srf,
            PropDir dir,
            TrackDerivative deriv) {
//        System.out.println("running PropDCAXY");

        // Construct return status
        PropStat pstat = new PropStat();

        // Fetch the originating Surface and check it is a DCA surface
        Surface srf0 = trv.surface();
        Assert.assertTrue(srf0.pureType().equals(SurfDCA.staticType()));
        if (!srf0.pureType().equals(SurfDCA.staticType())) {
            System.out.println("Failing PropDCAXY because it's not a DCA");
            return pstat;
        }
        SurfDCA srf_dca = (SurfDCA) srf0;

        // Check that dca surface has zero tilt.
        boolean tilted = srf_dca.dXdZ() != 0 || srf_dca.dYdZ() != 0;
        Assert.assertTrue(!tilted);
        if (tilted) {
            System.out.println("Failing PropDCAXY because it's tilted");
            return pstat;
        }
        // Check that the destination surface is an XYPlane
        Assert.assertTrue(srf.pureType().equals(SurfXYPlane.staticType()));
        if (!srf.pureType().equals(SurfXYPlane.staticType())) {
            System.out.println("Failing PropDCAXY because it's not a XY plane");
            return pstat;
        }
        SurfXYPlane srf_xyp = (SurfXYPlane) srf;

        // Instantiate the intermediate propagators.
        PropDCACyl prop1 = new PropDCACyl(bField());
        PropCylXY prop2 = new PropCylXY(bField());

        // Radius of a cylinder going through the PCA ...
        // But:
        //   PropDCACyl requires that the cylinder be at a different radius than the DCA
        //   so put it 100 microns outside the dca.
        double rhack = Math.abs(trv.vector(IRSIGNED)) + 0.01;

        // Instantiate the surface at the intermediate step.
        SurfCylinder srf_cyl = new SurfCylinder(rhack);

        // Setup for receiving the derivatives.
        TrackDerivative d1 = null;
        TrackDerivative d2 = null;
        if (deriv != null) {
            d1 = new TrackDerivative();
            d2 = new TrackDerivative();
        }

        // Save the track z direction for later use.
        boolean forward = trv.vector(ITLM_DCA) >= 0.;



        // Do the propagation in two steps.
        PropStat p1 = prop1.vecDirProp(trv, srf_cyl, dir, d1);
        if (!p1.success()) {
            System.out.println("Failing PropDCAXY propagation PropDCACyl failed");
            System.out.println("trv = "+trv.toString());
            System.out.println("cylinder = "+srf_cyl.toString());
//            trv.setTrackBackward();
//            p1=prop1.vecDirProp(trv, srf_cyl, dir, d1);
            if(!p1.success())return p1;
//            System.out.println("SUCCESS!!!!!!");
        }
        //    System.out.println("propagated to fake cylinder, track is: " + trv.toString());
        PropStat p2 = prop2.vecDirProp(trv, srf_xyp, dir, d2);
//    System.out.println("propagated to plane, track is: " + trv.toString());
        if (!p2.success()) {
            System.out.println("Failing PropDCAXY propagation PropCylXY failed");
            return p2;
        }

        // Forward/backward is defined wrt positive z axis.

        //mg...I'm not sure if this is what we want...seems to screw up PropXYXY
        //mg (months later)   ...  how can this screw up PropXYXY???
/*
        if ( forward ){
        trv.setForward();
        } else{
        trv.setBackward();
        }
         */
        trv.setForward();  //mg set to setForward by default

        // Set the transformation matrix to the product of the those from
        // the two intermediate steps.
        if (deriv != null) {
            deriv.set(d2.times(d1));
        }

        // Set properties of the return value.
        if (p2.forward()) {
            pstat.setForward();
        } else if (p2.backward()) {
            pstat.setBackward();
        } else if (p2.same()) {
            pstat.setSame();
        }
        double s = p1.pathDistance() + p2.pathDistance();
        pstat.setPathDistance(s);



        return pstat;

    }
}
