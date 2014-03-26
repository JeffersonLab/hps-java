package org.hps.recon.tracking.kalman;


import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropDirected;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackDerivative;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfcyl.SurfCylinder;
import org.lcsim.recon.tracking.trfdca.SurfDCA;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
/**
 * Propagates tracks from a DCA surface to a Cylinder.
 *<p>
 * Propagation will fail if either the origin is not a DCA surface,
 * or the destination is not a Cylinder.
 *<p>
 * The default direction for propagation is forward.
 *<p>
 * Propagation to a cylinder at the radius of the DCA will succeed
 * and the track parameters are valid but the errors are not valid.
 *
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */
public class PropDCACyl extends PropDirected
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
    { return "PropDCACyl";
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
    public PropDCACyl(double bfield)
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
        return new PropDCACyl( bField() );
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
        return dcaCylPropagate( _bfac, trv, srf, dir, deriv );
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
        return "DCA propagation to a Cylinder with constant "
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
    public PropStat dcaCylPropagate(       double             _bfac,
            VTrack             trv,
            Surface            srf,
            PropDir            dir,
            TrackDerivative    deriv )
    {
        
        // construct return status
        PropStat pstat = new PropStat();
        
        // fetch the originating Surface and check it is a DCA surface
        Surface srf0 = trv.surface();
        Assert.assertTrue( srf0.pureType().equals(SurfDCA.staticType() ));
        if ( ! srf0.pureType( ).equals(SurfDCA.staticType()) ){
            System.out.println("Failing PropDCACyl because it's not a DCA surface");
            return pstat;
        }
        SurfDCA srf_dca = ( SurfDCA ) srf0;
        
        // Check that dca surface has zero tilt.
        
        boolean tilted = srf_dca.dXdZ() != 0 || srf_dca.dYdZ() != 0;
        Assert.assertTrue(!tilted);
        if(tilted)    {
             System.out.println("Failing PropDCACyl DCA surface is tilted");
            return pstat;
        }
        
        // check that the destination surface is a Cylinder
        Assert.assertTrue( srf.pureType().equals(SurfCylinder.staticType()) );
        if ( !srf.pureType( ).equals(SurfCylinder.staticType()) ){
             System.out.println("Failing PropDCACyl because it's not a Cyl surface");
            return pstat;
        }
        SurfCylinder srf_cyl = ( SurfCylinder ) srf;
        //SurfCylinder srf_cyl = new SurfCylinder( (SurfCylinder) srf);
        
        // fetch the originating TrackVector
        TrackVector vec_dca = trv.vector();
        double r1p   = Math.abs(vec_dca.get(IRSIGNED));        // r
        double z1    = vec_dca.get(IZ_DCA);                // z
        double phid1p= vec_dca.get(IPHID);                 // phi_direction
        double tlam1 = vec_dca.get(ITLM_DCA);              // tan(lambda)
        double qpt1  = vec_dca.get(IQPT_DCA);              // q/pT
        
        double xv = srf_dca.x();
        double yv = srf_dca.y();
        
        // calculate alf1 and phi1
        double sign_alf1p;
        double sign_r1p=0.;
        
        if ( !TRFMath.isZero( vec_dca.get(IRSIGNED) ) )
        {
            sign_alf1p  = vec_dca.get(IRSIGNED)/Math.abs(vec_dca.get(IRSIGNED));
            sign_r1p = sign_alf1p;
        }
        else
        {
            sign_alf1p  = 0.0;
        }
        
        if( (!TRFMath.isZero(xv) || !TRFMath.isZero(yv)) &&  TRFMath.isZero(sign_alf1p) )  sign_alf1p=1;
        
        double alf1p = sign_alf1p * TRFMath.PI2;                 // alpha
        double phi1p = phid1p - alf1p;                    // phi_position
        phi1p = TRFMath.fmod2( phi1p, TRFMath.TWOPI );
        
        // calculate salf1, calf1 and crv1
        double salf1p=0.;
        salf1p = alf1p>0. ? 1.:( alf1p < 0. ? -1. : 0.) ;
        
        double cosphi1p=Math.cos(phi1p);
        double sinphi1p=Math.sin(phi1p);
        
        double x1=r1p*cosphi1p+xv;
        double y1=r1p*sinphi1p+yv;
        
        double phi1 = Math.atan2(y1,x1);       // phi position in (xv,yv) coord system
        double r1 = Math.sqrt(x1*x1+y1*y1);    // r1 in (xv,yv) coord system
        double alf1 = alf1p + phi1p - phi1; // alf1 in (xv,yv) coord system
        if( TRFMath.isZero(x1) && TRFMath.isZero(y1)  )
        {
            phi1 = phid1p;
            alf1 = 0.;
        }
        if( sign_r1p == 0 && !TRFMath.isZero(r1) )
            sign_r1p=1;
        
        // calculate salf1, calf1 and crv1
        double salf1 = Math.sin( alf1 );
        double calf1 = Math.cos( alf1 );
        
        if( TRFMath.isZero(alf1) )
        {
            salf1=0.;
            calf1=1.;
        }
        if( TRFMath.isEqual(Math.abs(alf1),TRFMath.PI2) )
        {
            salf1= alf1 > 0 ? 1. : -1. ;
            calf1=0.;
        }
        
        double  crv1 = _bfac * qpt1;
        double sign_crv = 1.;
        
        // fetch r2 of the destination Cylinder
        double r2 = srf_cyl.parameter(SurfCylinder.RADIUS);
        
        // calculate tlam2, qpt2 and crv2 of the destination Cylinder
        double tlam2 = tlam1;
        double qpt2  = qpt1;
        double crv2  = crv1;
        
        // calculate salf2 of the destination Cylinder
        double salf2 = r1/r2*salf1 + 0.5*crv1/r2*(r2*r2-r1*r1);
        
        // If salf2 is close to 1 or -1, set it to that value.
        double diff = Math.abs( Math.abs(salf2) - 1.0 );
        if ( diff < 1.e-10 ) salf2 = salf2>0 ? 1.0 : -1.0;
        // if salf2 > 1, track does not cross cylinder
        if ( Math.abs(salf2) > 1.0 ) {
             System.out.println("Failing PropDCACyl track does not cross cylinder:  salf2 = "+salf2);
             System.out.println("r1 = "+r1+"; r2 = "+r2+"; salf1="+salf1+"; crv1 = "+crv1);
             System.out.println("r1p = "+r1p+"; x1 = "+x1+"; y1 ="+y1+"; phi1p = "+phi1p);
             return pstat;
        }
        
        // there are two possibilities for alf2
        double alf21 = Math.asin( salf2 );
        double alf22 = alf21>0 ? Math.PI-alf21 : -Math.PI-alf21;
        double calf21 = Math.cos( alf21 );
        double calf22 = Math.cos( alf22 );
        //double phi21 = phi1 + atan2( -sign_crv*calf21, r2*crv2-salf2 );
        //double phi22 = phi1 + atan2( -sign_crv*calf22, r2*crv2-salf2 );
        //        double cnst = salf1-r1*crv1 > 0 ? TRFMath.PI2 : -TRFMath.PI2;
        //        cnst = (r1 == 0.) ? 0. : cnst;
        
        double cnst = Math.atan2(salf1-r1*crv1,calf1);
        if( TRFMath.isEqual(Math.abs(alf1),TRFMath.PI2) )
        {
            cnst = salf1-r1*crv1 > 0 ? TRFMath.PI2 : -TRFMath.PI2;
            cnst = (r1==0.) ? 0. : cnst;
        }
        
        double phi21 = phi1 + cnst - Math.atan2( salf2-r2*crv2, calf21 );
        double phi22 = phi1 + cnst - Math.atan2( salf2-r2*crv2, calf22 );
        
        if( TRFMath.isZero(crv1) )
        {
            phi21 = phi1 + cnst  - alf21;
            phi22 = phi1 + cnst  - alf22;
        }
        
        if ( TRFMath.isZero(calf21) )
        {
            phi21 = phi1;
            phi22 = phi1;
        }
        
        // construct an sT object for each solution
        ST_DCACyl sto1 = new ST_DCACyl(r1,phi1,alf1,crv1,r2,phi21,alf21);
        ST_DCACyl sto2 = new ST_DCACyl(r1,phi1,alf1,crv1,r2,phi22,alf22);
        // check the two solutions are nonzero and have opposite sign
        // or at least one is nonzero
        
        // choose the correct solution
        boolean use_first_solution;
        
        if( dir.equals(PropDir.NEAREST))
            use_first_solution = Math.abs(sto2.st()) > Math.abs(sto1.st());
        
        else if( dir.equals(PropDir.FORWARD))
            use_first_solution = sto1.st() > 0.0;
        
        else if( dir.equals(PropDir.BACKWARD))
            use_first_solution = sto1.st() < 0.0;
        
        else
        {
            use_first_solution = false;
            System.out.println( "PropCyl._vec_propagate: Unknown direction.");
            System.exit(1);
        }
        
        // assign phi2, alf2 and sto2 for the chosen solution
        double phi2, alf2;
        ST_DCACyl sto = new ST_DCACyl();
        double calf2;
        if ( use_first_solution )
        {
            sto = sto1;
            phi2 = phi21;
            alf2 = alf21;
            calf2 = calf21;
        }
        else
        {
            sto = sto2;
            phi2 = phi22;
            alf2 = alf22;
            calf2 = calf22;
        }
        
        // check alpha range
        Assert.assertTrue( Math.abs(alf2) <= Math.PI );
        
        // fetch sT
        double st = sto.st();
        double s = st*Math.sqrt(1+tlam1*tlam1);
        
        // calculate z2 of the destination Cylinder
        double z2 = z1 + st * tlam1;
        
        // construct the destination TrackVector
        TrackVector vec_cyl = new TrackVector();
        /*
#ifdef TRF_PHIRANGE
  vec_cyl(IPHI)     = fmod1(phi2, TWOPI);
#else
  vec_cyl(IPHI)     = phi2;
#endif
         */
        vec_cyl.set(IPHI     , phi2);
        vec_cyl.set(IZ_CYL   , z2);
        vec_cyl.set(IALF     , alf2);
        vec_cyl.set(ITLM_CYL , tlam2);
        vec_cyl.set(IQPT_CYL , qpt2);
/*
   // For axial tracks, zero z and tan(lambda).
 
  if(trv.is_axial()) {
    vec_cyl(SurfDCA::IZ) = 0.;
    vec_cyl(SurfDCA::ITLM) = 0.;
  }
 */
        
        // set the surface of trv to the destination Cylinder
        trv.setSurface( srf_cyl.newPureSurface() );
        
        // set the vector of trv to the destination TrackVector (Cyl. coord.)
        trv.setVector(vec_cyl);
        
        // set the direction of trv
        if ( Math.abs(alf2) <= TRFMath.PI2 ) trv.setForward();
        else trv.setBackward();
        
        // set the return status
        pstat.setPathDistance(s);
        
        // exit now if user did not ask for error matrix.
        if ( deriv == null ) {
             System.out.println("PropDCACyl did not ask for error matrix");
            return pstat;
        }
        
        
        // calculate derivatives
        
        //double dalf2_dr1   = (salf1-r1*crv1)/(r2*calf2);
        //double dalf2_dr1   = (1.0-r1*crv1*salf1)/(r2*calf2);
        // commented out by SK ( DCA to DCA(xv,yv) change)
        double salf12 = sign_r1p*salf1;
        if( sign_r1p == 0 && salf1 == 0. )
            salf12 = 1;
        double dalf2_dr1_sign = (salf12-crv1*r1*sign_r1p)/r2/calf2;
        double dalf2_dr1 = (salf1-crv1*r1)/r2/calf2;
        double dalf2_dalf1_or = 1/r2*calf1/calf2; // over r1
        double dalf2_dalf1 = r1*dalf2_dalf1_or;
        double dalf2_dcrv1 = (r2*r2-r1*r1)/(2.0*r2*calf2);
        
        //double dphi2_dr1 = -sign_crv*
        //               (r2*crv2*salf2-1.0)*(r1*crv1-salf1)/
        //               (r2*calf2*(1.0 + r2*r2*crv2*crv2 - 2.0*r2*crv2*salf2));
        // commented out on 10.16.2001 by SK ( DCA to DCA(xv,yv) change)
        //        double dphi2_dr1 = -sign_crv *
        //        (r2*crv2*salf2-1.0)*(sign_alf1*r1*crv1-1.)/
        //        (r2*calf2*(1.0 + r2*r2*crv2*crv2 - 2.0*r2*crv2*salf2));
        //
        //        double dphi2_dcrv1 = -sign_crv*
        //        ((1.0-r2*crv2*salf2)*(r2*r2-r1*r1)-2.0*r2*r2*calf2*calf2)/
        //        (2.0*r2*calf2*(1.0 + r2*r2*crv2*crv2 - 2.0*r2*crv2*salf2));
        
        double dphi2_dphi1=1.;
        double dphi2_dr1_sign = -crv1*calf1*sign_r1p/(1.-2.*r1*crv1*salf1+r1*r1*crv1*crv1)
        - dalf2_dr1_sign*(1-salf2*crv2*r2)/(1.-2.*r2*crv2*salf2+r2*r2*crv2*crv2);
        
        double dphi2_dr1 = -crv1*calf1/(1.-2.*r1*crv1*salf1+r1*r1*crv1*crv1)
        - dalf2_dr1*(1-salf2*crv2*r2)/(1.-2.*r2*crv2*salf2+r2*r2*crv2*crv2);
        double dphi2_dalf1_m1_or = (-salf1*crv1-r1*crv1*crv1+2.*crv1*salf1)/(1.-2.*r1*crv1*salf1+r1*r1*crv1*crv1)
        - dalf2_dalf1_or*(1-salf2*crv2*r2)/(1.-2.*r2*crv2*salf2+r2*r2*crv2*crv2); // minus 1
        double dphi2_dalf1 = dphi2_dalf1_m1_or*r1 + 1;
        double dphi2_dcrv1 = -r1*calf1/(1.-2.*r1*crv1*salf1+r1*r1*crv1*crv1)
        -(dalf2_dcrv1*(1.-r2*crv2*salf2) - r2*calf2)/(1.-2.*r2*crv2*salf2+r2*r2*crv2*crv2);
        
        
        //double dst_dr1  = sto.d_st_dr1(dphi2_dr1,   dalf2_dr1  );
        //        double dst_dr1  = sto.d_st_dr1(sign_alf1*dphi2_dr1,sign_alf1*dalf2_dr1  );
        //        double dst_crv1 = sto.d_st_dcrv1(dphi2_dcrv1, dalf2_dcrv1);
        double dst_dr1_sign   = sto.d_st_dr1_sign(sign_r1p,dphi2_dr1_sign,dalf2_dr1_sign);
        double dst_dr1   = sto.d_st_dr1(dphi2_dr1,dalf2_dr1);
        double dst_dcrv1 = sto.d_st_dcrv1(dphi2_dcrv1, dalf2_dcrv1);
        double dst_dalf1_or = sto.d_st_dalf1_or(r1,dphi2_dalf1_m1_or, dalf2_dalf1_or);
        
        
        // build the derivative matrix
        // derivatives to prime coordinates
        
        double dphi1_dphi1p_tr = r1p*Math.cos(phi1p-phi1); // times r1
        double dphi1_dr1p_tr = sign_r1p*Math.sin(phi1p-phi1); // times r1
        
        double dalf1_dphi1p_tr = r1 - dphi1_dphi1p_tr;
        double dalf1_dr1p_tr = -dphi1_dr1p_tr;
        
        double dr1_dr1p_sign = Math.cos(phi1p-phi1);
        double dr1_dphi1p = r1p*Math.sin(phi1-phi1p);
        
        
        double dphi2_dphi1p = dphi2_dr1*dr1_dphi1p +dphi2_dalf1 - dphi1_dphi1p_tr*dphi2_dalf1_m1_or;
        double dphi2_dr1p = dphi2_dr1_sign*dr1_dr1p_sign - dphi2_dalf1_m1_or*dphi1_dr1p_tr;
        
        double dalf2_dphi1p = dalf2_dr1*dr1_dphi1p + dalf2_dalf1_or*dalf1_dphi1p_tr;
        double dalf2_dr1p = dalf2_dr1_sign*dr1_dr1p_sign + dalf2_dalf1_or*dalf1_dr1p_tr;
        
        double dst_dphi1p = dst_dr1*dr1_dphi1p + dst_dalf1_or*dalf1_dphi1p_tr;
        double dst_dr1p = dst_dr1_sign*dr1_dr1p_sign + dst_dalf1_or*dalf1_dr1p_tr;
        
        //deriv(IPHI,     IRSIGNED,  sign_alf1 * dphi2_dr1);
        deriv.set(IPHI,     IRSIGNED,  dphi2_dr1p);
        deriv.set(IPHI,     IZ_DCA,    0.0);
        deriv.set(IPHI,     IPHID,     dphi2_dphi1p);
        deriv.set(IPHI,     ITLM_DCA,  0.0);
        deriv.set(IPHI,     IQPT_DCA,  _bfac * dphi2_dcrv1);
        
        deriv.set(IZ_CYL,   IRSIGNED,  tlam1 * dst_dr1p);
        deriv.set(IZ_CYL,   IZ_DCA,    1.0);
        deriv.set(IZ_CYL,   IPHID,     tlam1 * dst_dphi1p);
        deriv.set(IZ_CYL,   ITLM_DCA,  st);
        deriv.set(IZ_CYL,   IQPT_DCA,  tlam1 * _bfac * dst_dcrv1);
        
        //deriv.set(IALF,     IRSIGNED,  sign_alf1 * dalf2_dr1);
        deriv.set(IALF,     IRSIGNED,  dalf2_dr1p);
        deriv.set(IALF,     IZ_DCA,    0.0);
        deriv.set(IALF,     IPHID,     dalf2_dphi1p);
        deriv.set(IALF,     ITLM_DCA,  0.0);
        deriv.set(IALF,     IQPT_DCA,  _bfac * dalf2_dcrv1);
        
        deriv.set(ITLM_CYL, IRSIGNED,  0.0);
        deriv.set(ITLM_CYL, IZ_DCA,    0.0);
        deriv.set(ITLM_CYL, IPHID,     0.0);
        deriv.set(ITLM_CYL, ITLM_DCA,  1.0);
        deriv.set(ITLM_CYL, IQPT_DCA,  0.0);
        
        deriv.set(IQPT_CYL, IRSIGNED,  0.0);
        deriv.set(IQPT_CYL, IZ_DCA,    0.0);
        deriv.set(IQPT_CYL, IPHID,     0.0);
        deriv.set(IQPT_CYL, ITLM_DCA,  0.0);
        deriv.set(IQPT_CYL, IQPT_DCA,  1.0);
        
        
/*
  // For axial tracks, zero all derivatives of or with respect to z or
  // tan(lambda), that are not already zero.  This will force the error
  // matrix to have zero errors for z and tan(lambda).
 
  if(trv.is_axial()) {
    deriv.set(IZ_CYL,   IRSIGNED,  0.);
    deriv.set(IZ_CYL,   IZ_DCA,    0.);
    deriv.set(IZ_CYL,   IPHID,     0.);
    deriv.set(IZ_CYL,   ITLM_DCA,  0.);
    deriv.set(IZ_CYL,   IQPT_DCA,  0.);
 
    deriv.set(ITLM_CYL, ITLM_DCA, 0.);
  }
 */
        return pstat;
        
    }
    
    
    // Private class STCalc.
    //
    // An STCalc_ object calculates sT (the signed transverse path length)
    // and its derivatives w.r.t. alf1 and crv1.  It is constructed from
    // the starting (r1, phi1, alf1, crv1) and final track parameters
    // (r2, phi2, alf2) assuming these are consistent.  Methods are
    // provided to retrieve sT and the two derivatives.
    
    class ST_DCACyl
    {
        
        private boolean _big_crv;
        private double _st;
        //  double _dst_dalf2;
        private double _dst_dr1;
        private double _dst_dcrv1;
        private double _dst_dphi2;
        double _dst_dphi2_or;
        private double _crv1; // was public? need to look into this
        // constructor
        public ST_DCACyl()
        {
        }
        public ST_DCACyl(double r1, double phi1, double alf1, double crv1,
                double r2, double phi2, double alf2)
        {
            
            _crv1 = crv1;
            Assert.assertTrue( r1 >= 0.0 );
            Assert.assertTrue( r2 >= 0.0 );
            double rmax = r1+r2;
            
            // Calculate the change in xy direction
            double phi_dir_diff = TRFMath.fmod2(phi2+alf2-phi1-alf1,TRFMath.TWOPI);
            Assert.assertTrue( Math.abs(phi_dir_diff) <= Math.PI );
            
            // Evaluate whether |C| is "big"
            _big_crv = rmax*Math.abs(crv1) > 0.001;
            
            // If the curvature is big we can use
            // sT = (phi_dir2 - phi_dir1)/crv1
            if ( _big_crv )
            {
                Assert.assertTrue( crv1 != 0.0 );
                _st = phi_dir_diff/crv1;
            }
            
            // Otherwise, we calculate the straight-line distance
            // between the points and use an approximate correction
            // for the (small) curvature.
            else
            {
                
                // evaluate the distance
                double d = Math.sqrt( r1*r1 + r2*r2 - 2.0*r1*r2*Math.cos(phi2-phi1) );
                double arg = 0.5*d*crv1;
                double arg2 = arg*arg;
                double st_minus_d = d*arg2*( 1.0/6.0 + 3.0/40.0*arg2 );
                _st = d + st_minus_d;
                
                // evaluate the sign
                // We define a metric xsign = abs( (dphid-d*C)/(d*C) ).
                // Because sT*C = dphid and d = abs(sT):
                // xsign = 0 for sT > 0
                // xsign = 2 for sT < 0
                // Numerical roundoff will smear these predictions.
                double sign = 0.0;
                if ( crv1*_st != 0. )
                {
                    double xsign = Math.abs( (phi_dir_diff - _st*crv1) / (_st*crv1) );
                    if ( xsign < 0.5 ) sign = 1.0;
                    if ( xsign > 1.5  &&  xsign < 3.0 ) sign = -1.0;
                }
                // If the above is indeterminate, assume zero curvature.
                // In this case abs(alpha) decreases monotonically
                // with sT.  Track passing through origin has alpha = 0 on one
                // side and alpha = +/-pi on the other.  If both points are on
                // the same side, we use dr/ds > 0 for |alpha|<pi/2.
                if ( sign == 0. )
                {
                    sign = 1.0;
                    if ( Math.abs(alf2) > Math.abs(alf1) ) sign = -1.0;
                    if ( Math.abs(alf2) == Math.abs(alf1) )
                    {
                        if ( Math.abs(alf2) < TRFMath.PI2 )
                        {
                            if ( r2 < r1 ) sign = -1.0;
                        }
                        else
                        {
                            if ( r2 > r1 ) sign = -1.0;
                        }
                    }
                }
                
                // Correct _st using the above sign.
                Assert.assertTrue( Math.abs(sign) == 1.0 );
                _st = sign*_st;
                
                // save derivatives
                //    _dst_dalf2 = (_st/d)*2.0*(r1-r2*cos(phi2-phi1));
                //_dst_dphi2 = (_st/d)*2.0*r1*r2*sin(phi2-phi1);
                //                _dst_dcrv1 = sign*d*d*arg*( 1.0/6.0 + 3.0/20.0*arg2);
                //                double root = (1.0 + 0.5*arg*arg + 3.0/8.0*arg*arg*arg*arg );
                //                _dst_dphi2 = sign*(r1*r2*Math.sin(phi2-phi1))*root/d;
                //                _dst_dr1 =   sign*(r1-r2*Math.cos(phi2-phi1))*root/d;
                
                if ( TRFMath.isZero(d) )
                {
                    _dst_dcrv1 = 0.0;
                    _dst_dphi2 = sign*r1;
                    _dst_dphi2_or = sign;
                    _dst_dr1 =  0.0;
                }
                else
                {
                    _dst_dcrv1 = sign*d*d*arg*( 1.0/6.0 + 3.0/20.0*arg2);
                    double root = (1.0 + 0.5*arg*arg + 3.0/8.0*arg*arg*arg*arg );
                    _dst_dphi2_or = sign*(r2*Math.sin(phi2-phi1))*root/d;
                    _dst_dphi2 = _dst_dphi2_or*r1;
                    _dst_dr1 =   sign*(r1-r2*Math.cos(phi2-phi1))*root/d;
                }
            }
            
        }
        public double st()
        {
            return _st;
        }
        
        public double d_st_dr1_sign(double sign_alf1,double dphi2_dr1_sign,double dalf2_dr1_sign)
        {
            if ( _big_crv ) return ( dphi2_dr1_sign + dalf2_dr1_sign ) / _crv1;
            else return   _dst_dphi2 * dphi2_dr1_sign + _dst_dr1*sign_alf1;
        }
        
        public double d_st_dr1(   double d_phi2_dr1,   double d_alf2_dr1   )
        {
            if ( _big_crv ) return ( d_phi2_dr1 + d_alf2_dr1 ) / _crv1;
            else return   _dst_dphi2 * d_phi2_dr1 + _dst_dr1;
        }
        public double d_st_dcrv1( double d_phi2_dcrv1, double d_alf2_dcrv1 )
        {
            if ( _big_crv ) return ( d_phi2_dcrv1 + d_alf2_dcrv1 - _st ) / _crv1;
            else return   _dst_dphi2 * d_phi2_dcrv1 + _dst_dcrv1;
        }
        
        public double d_st_dalf1_or(double r1,double dphi2_dalf1_m1_or, double dalf2_dalf1_or)
        {
            if ( _big_crv ) return ( dphi2_dalf1_m1_or + dalf2_dalf1_or) / _crv1;
            else return _dst_dphi2_or * (dphi2_dalf1_m1_or*r1+1);
        }
        
    }
    
}


