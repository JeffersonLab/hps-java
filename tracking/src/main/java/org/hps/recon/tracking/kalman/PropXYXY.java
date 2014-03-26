package org.hps.recon.tracking.kalman;

import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropDirected;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.Propagator;
import org.lcsim.recon.tracking.trfbase.Surface;
import org.lcsim.recon.tracking.trfbase.TrackDerivative;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfutil.Assert;
import org.lcsim.recon.tracking.trfutil.TRFMath;
import org.lcsim.recon.tracking.trfxyp.SurfXYPlane;

/**
 * Propagates tracks from one XYPlane to another in a constant field.
 *<p>
 * Propagation will fail if either the origin or destination is
 * not a XYPlane.
 * Propagator works incorrectly for tracks with very small curvatures
 *
 *
 *@author Norman A. Graf
 *@version 1.0
 *
 */
public class PropXYXY extends PropDirected {

    // attributes
    private boolean _debug=false;
    private double _bfac;
    private static final int IV = SurfXYPlane.IV;
    private static final int IZ = SurfXYPlane.IZ;
    private static final int IDVDU = SurfXYPlane.IDVDU;
    private static final int IDZDU = SurfXYPlane.IDZDU;
    private static final int IQP = SurfXYPlane.IQP;

    // static methods
    /**
     *Return a String representation of the class' type name.
     *Included for completeness with the C++ version.
     *
     * @return   A String representation of the class' type name.
     */
    public static String typeName() {
        return "PropXYXY";
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
    public PropXYXY(double bfield) {
        _bfac = TRFMath.BFAC * bfield;
    }

    /**
     *Clone an instance.
     *
     * @return A Clone of this instance.
     */
    public Propagator newPropagator() {
        return new PropXYXY(bField());
    }

    /**
     *Propagate a track without error in the specified direction
     *and return the derivative matrix in deriv.
     *
     * @param   trv The VTrack to propagate.
     * @param   srf The Surface to which to propagate.
     * @param   dir The direction in which to propagate.
     * @param   deriv The track derivatives to update at the surface srf.
     * @return The propagation status.
     **/
    public PropStat vecDirProp(VTrack trv, Surface srf,
            PropDir dir, TrackDerivative deriv) {
        PropStat pstat = vec_propagatexyxy_(_bfac, trv, srf, dir, deriv);
        return pstat;
    }

    /**
     *Propagate a track without error in the specified direction.
     *
     * @param   trv The VTrack to propagate.
     * @param   srf The Surface to which to propagate.
     * @param   dir The direction in which to propagate.
     * @return The propagation status.
     */
    @Override
    public PropStat vecDirProp(VTrack trv, Surface srf,
            PropDir dir) {
        TrackDerivative deriv = null;
        return vecDirProp(trv, srf, dir, deriv);
    }

    /**
     *Return the strength of the magnetic field in Tesla.
     *
     * @return The strength of the magnetic field in Tesla.
     */
    public double bField() {
        return _bfac / TRFMath.BFAC;
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
     *output stream
     *
     * @return  A String representation of this instance.
     */
    @Override
    public String toString() {
        return "XYPlane-XYPlane propagation with constant "
                + bField() + " Tesla field";
    }

    // Private function to determine dphi of the propagation
    double direction(int flag_forward, int sign_dphi,
            double du,
            double norm, double cat,
            double sinphi, double cosphi) {

        int sign_cat = 0;
        if (du * flag_forward > 0.)
            sign_cat = 1;
        if (du * flag_forward < 0.)
            sign_cat = -1;
        if (du == 0.) {
            if (sinphi >= 0.)
                sign_cat = 1;
            if (sinphi < 0.)
                sign_cat = -1;
        }

        double sin_dphi = norm * sinphi + cat * cosphi * sign_cat;
        double cos_dphi = -norm * cosphi + cat * sinphi * sign_cat;

        int sign_sindphi = 0;
        if (sin_dphi > 0.)
            sign_sindphi = 1;
        if (sin_dphi < 0.)
            sign_sindphi = -1;
        if (sin_dphi == 0.)
            sign_sindphi = sign_dphi;

        if (Math.abs(cos_dphi) > 1.) {
            double sn = -1.;
            if (cos_dphi > 0)
                sn = 1.;
            cos_dphi = sn * Math.sqrt(1. - sin_dphi * sin_dphi);
        }
        if (Math.abs(sin_dphi) > 1.) {
            double sn = -1.;
            if (sin_dphi > 0)
                sn = 1.;
            sin_dphi = sn * Math.sqrt(1. - cos_dphi * cos_dphi);
        }

        double dphi = Math.PI * (sign_dphi - sign_sindphi) + sign_sindphi * Math.acos(cos_dphi);
        return dphi;

    }
    //**********************************************************************

    // Private function to propagate a track without error
    // The corresponding track parameters are:
    // u (cm) is fixed
    // 0 - v (cm)
    // 1 - z (cm)
    // 2 - dv/du
    // 3 - dz/du
    // 4 - q/p   p is momentum of a track, q is its charge
    // If deriv is nonzero, return the derivative matrix there.
    PropStat vec_propagatexyxy_(double B, VTrack trv, Surface srf,
            PropDir dir1,
            TrackDerivative deriv) {

        // construct return status
        PropStat pstat = new PropStat();

        PropDir dir = dir1; //need to check constness of this
        boolean move = Propagator.reduceDirection(dir);
        if (move)
            dir = reduce(dir);
        // fetch the originating surface and vector
        Surface srf1 = trv.surface();
        // TrackVector vec1 = trv.get_vector();

        // Check origin is a XYPlane.
        Assert.assertTrue(srf1.pureType().equals(SurfXYPlane.staticType()));
        if (!srf1.pureType().equals(SurfXYPlane.staticType()))
            return pstat;
        SurfXYPlane sxyp1 = (SurfXYPlane) srf1;

        // Check destination is an XYPlane.
        Assert.assertTrue(srf.pureType().equals(SurfXYPlane.staticType()));
        if (!srf.pureType().equals(SurfXYPlane.staticType()))
            return pstat;
        SurfXYPlane sxyp2 = (SurfXYPlane) srf;


        // If surfaces are the same and no XXX_MOVE is requested we can return now.
        boolean same = srf.pureEqual(srf1);
        if(_debug)System.out.println("same=" + same + "; move=" + move + "; " + dir1.toString() + "    " + dir.toString());

        if (same && !move) {
            if (deriv != null) {
                deriv.setIdentity();
            }
            pstat.setSame();
            return pstat;
        }


        if (same && dir1.equals(PropDir.NEAREST_MOVE))
            dir = PropDir.FORWARD;

        //v00.63.04  cng 01/12/01
        if (Math.abs(B) < 1e-7)
            return zeroBField(trv, sxyp1, sxyp2, dir1, deriv);

        // Fetch the u's and phi's of the planes and the starting track vector.
        TrackVector vec = trv.vector();
        int iphi = SurfXYPlane.NORMPHI;
        int idist = SurfXYPlane.DISTNORM;
        double phi_0 = sxyp1.parameter(iphi);
        double u_0 = sxyp1.parameter(idist);
        double phi_n = sxyp2.parameter(iphi);
        double u_n = sxyp2.parameter(idist);
        if (same && move) {
            int forw = trv.isForward() ? 1 : -1;
            int forw_dir = (dir == PropDir.FORWARD) ? 1 : -1;
            int sn = vec.get(IDVDU) > 0. ? 1 : -1;
            u_0 += 1.e-7 * (double) (forw * forw_dir * sn);
        }

        double b1_0 = vec.get(IV);                  // v
        double b2_0 = vec.get(IZ);                  // z
        double b3_0 = vec.get(IDVDU);               // dv/du
        double b4_0 = vec.get(IDZDU);               // dz/du
        double b5_0 = vec.get(IQP);                 // q/p

        double phi_u = phi_0 - phi_n;

        double cosphi_u = Math.cos(phi_u);
        double sinphi_u = Math.sin(phi_u);
//         System.out.println( "PropXYXY._vec_propagate: phi_u = "+phi_u);
        // check if du == 0 ( that is track moves parallel to the destination plane )
        double du_du_0 = cosphi_u - b3_0 * sinphi_u;
        if (du_du_0 == 0.)
            return pstat;

        double a_hat_u = 1. / du_du_0;
        double a_hat_u2 = a_hat_u * a_hat_u;

        double u = u_0 * cosphi_u - b1_0 * sinphi_u;
        double b1 = b1_0 * cosphi_u + u_0 * sinphi_u;
        double b2 = b2_0;
        double b3 = (b3_0 * cosphi_u + sinphi_u) * a_hat_u;
        double b4 = b4_0 * a_hat_u;
        double b5 = b5_0;

        int sign_du0 = 0;
        if (trv.isForward())
            sign_du0 = 1;
        if (trv.isBackward())
            sign_du0 = -1;
        if (sign_du0 == 0) {
            System.out.println("PropXYXY._vec_propagate: Unknown direction of a track ");
            System.exit(1);
        }
        int sign_du = 0;
        if (du_du_0 * sign_du0 > 0)
            sign_du = 1;
        if (du_du_0 * sign_du0 < 0)
            sign_du = -1;

        // check that q/p != 0
        Assert.assertTrue(b5 != 0.);

        // 1/curv of the track is r
        double r = 1 / (b5 * B) * Math.sqrt(1 + b3_0 * b3_0) / Math.sqrt(1 + b3_0 * b3_0 + b4_0 * b4_0);
        double b3_hat = Math.sqrt(1 + b3 * b3);
        double b34_hat = Math.sqrt(1 + b3 * b3 + b4 * b4);
        double b3_hat2 = b3_hat * b3_hat;
        double b34_hat2 = b34_hat * b34_hat;
        double cosphi = -b3 * sign_du / b3_hat;
        double sinphi = sign_du / b3_hat;
        double rsinphi = 1. / (b5 * B) * sign_du / b34_hat;
        double rcosphi = -b3 / (b5 * B) * sign_du / b34_hat;

        double du = u_n - u;
        double norm = du / r - cosphi;
        if(_debug)System.out.println("PropXYXY._vec_propagate: u_n = " + u_n + "; u = " + u + "; r = " + r);

        // xy-xy propagation failed : noway to the new plane
        if (Math.abs(norm) > 1.)
            return pstat;

        double cat = Math.sqrt(1. - norm * norm);
        int flag_forward = 0;
        int sign_dphi = 0;

        if (dir.equals(PropDir.NEAREST)) {
            // try forward propagation
            flag_forward = 1;
            if (b5 * B > 0.)
                sign_dphi = 1;
            if (b5 * B < 0.)
                sign_dphi = -1;
            double dphi1 =
                    direction(flag_forward, sign_dphi, du, norm, cat, sinphi, cosphi);
            // try backward propagation
            flag_forward = -1;
            if (b5 * B > 0.)
                sign_dphi = -1;
            if (b5 * B < 0.)
                sign_dphi = 1;
            double dphi2 =
                    direction(flag_forward, sign_dphi, du, norm, cat, sinphi, cosphi);
            if (Math.abs(dphi2) > Math.abs(dphi1)) {
                flag_forward = -flag_forward;
                sign_dphi = -sign_dphi;
            }
        } else if (dir.equals(PropDir.FORWARD)) {
            flag_forward = 1;
            if (b5 * B > 0.)
                sign_dphi = 1;
            if (b5 * B < 0.)
                sign_dphi = -1;
        } else if (dir.equals(PropDir.BACKWARD)) {
            flag_forward = -1;
            if (b5 * B > 0.)
                sign_dphi = -1;
            if (b5 * B < 0.)
                sign_dphi = 1;
        } else {
            System.out.println("PropXYXY._vec_propagate: Unknown direction.");
            System.exit(1);
        }

        int sign_cat = 0;
        if (du * sign_dphi * b5 * B > 0.)
            sign_cat = 1;
        if (du * sign_dphi * b5 * B < 0.)
            sign_cat = -1;
        if (du == 0.) {
            if (sinphi >= 0.)
                sign_cat = 1;
            if (sinphi < 0.)
                sign_cat = -1;
        }

        double sin_dphi = norm * sinphi + cat * cosphi * sign_cat;
        double cos_dphi = -norm * cosphi + cat * sinphi * sign_cat;
        if (cos_dphi > 1.)
            cos_dphi = 1.;
        if (cos_dphi < -1.)
            cos_dphi = -1.;

        int sign_sindphi = 0;
        if (sin_dphi > 0.)
            sign_sindphi = 1;
        if (sin_dphi < 0.)
            sign_sindphi = -1;
        if (sin_dphi == 0.)
            sign_sindphi = sign_dphi;

        double dphi = Math.PI * (sign_dphi - sign_sindphi) + sign_sindphi * Math.acos(cos_dphi);

        // check that I didn't make any mistakes

        Assert.assertTrue(Math.abs(Math.sin(dphi) - sin_dphi) < 1.e-5);

        // check if dun == 0 ( that is track moves parallel to the destination plane)
        double du_n_du = cos_dphi - b3 * sin_dphi;
        if (du_n_du == 0.)
            return pstat;

        double a_hat_dphi = 1. / du_n_du;
        double a_hat_dphi2 = a_hat_dphi * a_hat_dphi;
        double c_hat_dphi = sin_dphi + b3 * cos_dphi;

        double b1_n = b1 + rsinphi * (1 - cos_dphi) - rcosphi * sin_dphi;
        double b2_n = b2 + b4 / (b5 * B) * sign_du / b34_hat * dphi;
        double b3_n = c_hat_dphi * a_hat_dphi;
        double b4_n = b4 * a_hat_dphi;
        double b5_n = b5;
        if(_debug)System.out.println(trv.toString());
        if(_debug) System.out.println("PropXYXY._vec_propagate: sign_du0=" + sign_du0 + "; du_du_0=" + du_du_0);
        if(_debug)System.out.println("PropXYXY._vec_propagate: sign_du=" + sign_du + "; b3_hat=" + b3_hat);
        if(_debug)System.out.println("PropXYXY._vec_propagate: sign_cat=" + sign_cat + "; cosphi=" + cosphi
                + "; sinphi=" + sinphi + "; cat=" + cat);
        if(_debug)System.out.println("PropXYXY._vec_propagate: a_hat_dphi=" + a_hat_dphi + "; cos_dphi=" + cos_dphi
                + "; norm=" + norm);
        if(_debug)System.out.println("PropXYXY._vec_propagate: b3=" + b3 + "; sin_dphi=" + sin_dphi);
        if(_debug)System.out.println("PropXYXY._vec_propagate: b4_0=" + b4_0 + "; b4=" + b4 + "; b4_n=" + b4_n);

        // double u_n_0 = u_n*cosphi_u + b1_n*sinphi_u;

        // check if track crossed original plane during the propagation
        // switch (dir) {
        //  if( dir.equals(PropDir.FORWARD:
        //   if((u_n_0 - u_0)*sign_du0<0) return pstat;
        //  break;
        //  if( dir.equals(PropDir.BACKWARD:
        //   if((u_n_0 - u_0)*sign_du0>0) return pstat;
        //  break;
        // }

        int sign_dun = 0;
        if (du_n_du * sign_du > 0)
            sign_dun = 1;
        if (du_n_du * sign_du < 0)
            sign_dun = -1;

        vec.set(IV, b1_n);
        vec.set(IZ, b2_n);
        vec.set(IDVDU, b3_n);
        vec.set(IDZDU, b4_n);
        vec.set(IQP, b5_n);
        if(_debug)System.out.println("PropXYXY._vec_propagate:  sign_dun = "+sign_dun+"; du_n_du = "+du_n_du+"; sign_du="+sign_du);

        // Update trv
        trv.setSurface(srf.newPureSurface());
        trv.setVector(vec);

        // set new direction of the track
        if (sign_dun == 1)
            trv.setForward();
        if (sign_dun == -1)
            trv.setBackward();

        // Calculate sT.
        double crv = B * b5;
        double dv = b1_n - b1;
        double dxy = Math.sqrt(du * du + dv * dv);
        double arg = 0.5 * crv * dxy;
        double dst = dxy * TRFMath.asinrat(arg);

        // Calculate s.
        double dz = b2_n - b2;
        Assert.assertTrue(flag_forward == 1 || flag_forward == -1);
        double ds = ((double) (flag_forward)) * Math.sqrt(dst * dst + dz * dz);

        // Set the return status.
        pstat.setPathDistance(ds);
        //(flag_forward==1)?pstat.set_forward():pstat.set_backward();

        // exit now if user did not ask for error matrix.
        if (deriv == null)
            return pstat;

        // du_d0

        double du_db1_0 = -sinphi_u;

        // db1_d0

        double db1_db1_0 = cosphi_u;

        // db3_d0

        double db3_db3_0 = a_hat_u2;

        // db4_d0

        double db4_db3_0 = b4_0 * sinphi_u * a_hat_u2;
        double db4_db4_0 = a_hat_u;

        // dr_d

        double dr_db3 = r * b3 * b4 * b4 / (b3_hat2 * b34_hat2);
        double dr_db4 = -r * b4 / b34_hat2;
        double dr_db5 = -r / b5;

        // dcosphi_d

        double dcosphi_db3 = -sign_du / b3_hat - cosphi * b3 / b3_hat2;

        // dsinphi_d

        double dsinphi_db3 = -sinphi * b3 / b3_hat2;

        // dcat_d

        double dcat_db3 = norm / cat * (du / (r * r) * dr_db3 + dcosphi_db3);
        double dcat_db4 = norm / cat * du / (r * r) * dr_db4;
        double dcat_db5 = norm / cat * du / (r * r) * dr_db5;
        double dcat_du = norm / (cat * r);

        // dnorm_d

        double dnorm_db3 = -du / (r * r) * dr_db3 - dcosphi_db3;
        double dnorm_db4 = -du / (r * r) * dr_db4;
        double dnorm_db5 = -du / (r * r) * dr_db5;
        double dnorm_du = -1. / r;

        // dcos_dphi_d

        double dcos_dphi_db3 = -cosphi * dnorm_db3 - norm * dcosphi_db3
                + sign_cat * (sinphi * dcat_db3 + cat * dsinphi_db3);
        double dcos_dphi_db4 = -cosphi * dnorm_db4 + sign_cat * sinphi * dcat_db4;
        double dcos_dphi_db5 = -cosphi * dnorm_db5 + sign_cat * sinphi * dcat_db5;
        double dcos_dphi_du = -cosphi * dnorm_du + sign_cat * sinphi * dcat_du;

        // dsin_dphi_d

        double dsin_dphi_db3 = sinphi * dnorm_db3 + norm * dsinphi_db3
                + sign_cat * (cosphi * dcat_db3 + cat * dcosphi_db3);
        double dsin_dphi_db4 = sinphi * dnorm_db4 + sign_cat * cosphi * dcat_db4;
        double dsin_dphi_db5 = sinphi * dnorm_db5 + sign_cat * cosphi * dcat_db5;
        double dsin_dphi_du = sinphi * dnorm_du + sign_cat * cosphi * dcat_du;

        // ddphi_d

        double ddphi_db3;
        double ddphi_db4;
        double ddphi_db5;
        double ddphi_du;
        if (Math.abs(sin_dphi) > 0.5) {
            ddphi_db3 = -dcos_dphi_db3 / sin_dphi;
            ddphi_db4 = -dcos_dphi_db4 / sin_dphi;
            ddphi_db5 = -dcos_dphi_db5 / sin_dphi;
            ddphi_du = -dcos_dphi_du / sin_dphi;
        } else {
            ddphi_db3 = dsin_dphi_db3 / cos_dphi;
            ddphi_db4 = dsin_dphi_db4 / cos_dphi;
            ddphi_db5 = dsin_dphi_db5 / cos_dphi;
            ddphi_du = dsin_dphi_du / cos_dphi;
        }

        // da_hat_dphi_d

        double da_hat_dphi_db3 = -a_hat_dphi2
                * (dcos_dphi_db3 - sin_dphi - b3 * dsin_dphi_db3);
        double da_hat_dphi_db4 = -a_hat_dphi2 * (dcos_dphi_db4 - b3 * dsin_dphi_db4);
        double da_hat_dphi_db5 = -a_hat_dphi2 * (dcos_dphi_db5 - b3 * dsin_dphi_db5);
        double da_hat_dphi_du = -a_hat_dphi2 * (dcos_dphi_du - b3 * dsin_dphi_du);

        // dc_hat_dphi_d

        double dc_hat_dphi_db3 = b3 * dcos_dphi_db3 + dsin_dphi_db3 + cos_dphi;
        double dc_hat_dphi_db4 = b3 * dcos_dphi_db4 + dsin_dphi_db4;
        double dc_hat_dphi_db5 = b3 * dcos_dphi_db5 + dsin_dphi_db5;
        double dc_hat_dphi_du = b3 * dcos_dphi_du + dsin_dphi_du;

        // db1_n_d

        double db1_n_db1 = 1;
        double db1_n_db3 = (dr_db3 * sinphi + r * dsinphi_db3) * (1 - cos_dphi)
                - rsinphi * dcos_dphi_db3
                - dr_db3 * cosphi * sin_dphi - r * dcosphi_db3 * sin_dphi
                - rcosphi * dsin_dphi_db3;
        double db1_n_db4 = dr_db4 * sinphi * (1 - cos_dphi) - rsinphi * dcos_dphi_db4
                - dr_db4 * cosphi * sin_dphi - rcosphi * dsin_dphi_db4;
        double db1_n_db5 = dr_db5 * sinphi * (1 - cos_dphi) - rsinphi * dcos_dphi_db5
                - dr_db5 * cosphi * sin_dphi - rcosphi * dsin_dphi_db5;
        double db1_n_du = -rsinphi * dcos_dphi_du - rcosphi * dsin_dphi_du;

        // db2_n_d

        double db2_n_db2 = 1.;
        double db2_n_db3 = 1. / (b5 * B) * b4 * sign_du / b34_hat
                * (-dphi * b3 / b34_hat2 + ddphi_db3);
        double db2_n_db4 = 1. / (b5 * B) * sign_du / b34_hat
                * (-dphi * b4 * b4 / b34_hat2 + b4 * ddphi_db4 + dphi);
        double db2_n_db5 = 1. / (b5 * B) * b4 * sign_du / b34_hat * (ddphi_db5 - dphi / b5);
        double db2_n_du = 1. / (b5 * B) * b4 * sign_du / b34_hat * ddphi_du;

        // db3_n_d

        double db3_n_db3 = a_hat_dphi * dc_hat_dphi_db3 + da_hat_dphi_db3 * c_hat_dphi;
        double db3_n_db4 = a_hat_dphi * dc_hat_dphi_db4 + da_hat_dphi_db4 * c_hat_dphi;
        double db3_n_db5 = a_hat_dphi * dc_hat_dphi_db5 + da_hat_dphi_db5 * c_hat_dphi;
        double db3_n_du = a_hat_dphi * dc_hat_dphi_du + da_hat_dphi_du * c_hat_dphi;

        // db4_n_d

        double db4_n_db3 = b4 * da_hat_dphi_db3;
        double db4_n_db4 = b4 * da_hat_dphi_db4 + a_hat_dphi;
        double db4_n_db5 = b4 * da_hat_dphi_db5;
        double db4_n_du = b4 * da_hat_dphi_du;

        // db5_n_d

        // db1_n_d0

        double db1_n_db1_0 = db1_n_du * du_db1_0 + db1_n_db1 * db1_db1_0;
        double db1_n_db2_0 = 0.;
        double db1_n_db3_0 = db1_n_db3 * db3_db3_0 + db1_n_db4 * db4_db3_0;
        double db1_n_db4_0 = db1_n_db4 * db4_db4_0;
        double db1_n_db5_0 = db1_n_db5;

        // db2_n_d0

        double db2_n_db1_0 = db2_n_du * du_db1_0;
        double db2_n_db2_0 = db2_n_db2;
        double db2_n_db3_0 = db2_n_db3 * db3_db3_0 + db2_n_db4 * db4_db3_0;
        double db2_n_db4_0 = db2_n_db4 * db4_db4_0;
        double db2_n_db5_0 = db2_n_db5;

        // db3_n_d0

        double db3_n_db1_0 = db3_n_du * du_db1_0;
        double db3_n_db2_0 = 0.;
        double db3_n_db3_0 = db3_n_db3 * db3_db3_0 + db3_n_db4 * db4_db3_0;
        double db3_n_db4_0 = db3_n_db4 * db4_db4_0;
        double db3_n_db5_0 = db3_n_db5;

        // db4_n_d0

        double db4_n_db1_0 = db4_n_du * du_db1_0;
        double db4_n_db2_0 = 0.;
        double db4_n_db3_0 = db4_n_db3 * db3_db3_0 + db4_n_db4 * db4_db3_0;
        double db4_n_db4_0 = db4_n_db4 * db4_db4_0;
        double db4_n_db5_0 = db4_n_db5;

        // db5_n_d0

        double db5_n_db1_0 = 0.;
        double db5_n_db2_0 = 0.;
        double db5_n_db3_0 = 0.;
        double db5_n_db4_0 = 0.;
        double db5_n_db5_0 = 1.;


        deriv.set(IV, IV, db1_n_db1_0);
        deriv.set(IV, IZ, db1_n_db2_0);
        deriv.set(IV, IDVDU, db1_n_db3_0);
        deriv.set(IV, IDZDU, db1_n_db4_0);
        deriv.set(IV, IQP, db1_n_db5_0);
        deriv.set(IZ, IV, db2_n_db1_0);
        deriv.set(IZ, IZ, db2_n_db2_0);
        deriv.set(IZ, IDVDU, db2_n_db3_0);
        deriv.set(IZ, IDZDU, db2_n_db4_0);
        deriv.set(IZ, IQP, db2_n_db5_0);
        deriv.set(IDVDU, IV, db3_n_db1_0);
        deriv.set(IDVDU, IZ, db3_n_db2_0);
        deriv.set(IDVDU, IDVDU, db3_n_db3_0);
        deriv.set(IDVDU, IDZDU, db3_n_db4_0);
        deriv.set(IDVDU, IQP, db3_n_db5_0);
        deriv.set(IDZDU, IV, db4_n_db1_0);
        deriv.set(IDZDU, IZ, db4_n_db2_0);
        deriv.set(IDZDU, IDVDU, db4_n_db3_0);
        deriv.set(IDZDU, IDZDU, db4_n_db4_0);
        deriv.set(IDZDU, IQP, db4_n_db5_0);
        deriv.set(IQP, IV, db5_n_db1_0);
        deriv.set(IQP, IZ, db5_n_db2_0);
        deriv.set(IQP, IDVDU, db5_n_db3_0);
        deriv.set(IQP, IDZDU, db5_n_db4_0);
        deriv.set(IQP, IQP, db5_n_db5_0);

        return pstat;

    }

    //cng
    PropStat zeroBField(VTrack trv, SurfXYPlane sxyp1,
            SurfXYPlane sxyp2, PropDir dir1,
            TrackDerivative deriv) {
        // construct return status
        PropStat pstat = new PropStat();

        PropDir dir = dir1; //need to check constness of this
        boolean move = Propagator.reduceDirection(dir);
        boolean same = sxyp2.pureEqual(sxyp1);

        // There is only one solution. Can't XXX_MOVE
        if (same && move)
            return pstat;

        if (same) {
            if (deriv != null) {
                deriv.setIdentity();
            }
            pstat.setSame();
            return pstat;
        }

        TrackVector vec = trv.vector();
        double v0 = vec.get(IV);
        double z0 = vec.get(IZ);
        double dvdu0 = vec.get(IDVDU);
        double dzdu0 = vec.get(IDZDU);

        double du0 = 1.;
        if (trv.isBackward())
            du0 = -1.;

        double phi0 = sxyp1.parameter(SurfXYPlane.NORMPHI);
        double cphi0 = Math.cos(phi0);
        double sphi0 = Math.sin(phi0);
        double u0 = sxyp1.parameter(SurfXYPlane.DISTNORM);

        double phi1 = sxyp2.parameter(SurfXYPlane.NORMPHI);
        double cphi1 = Math.cos(phi1);
        double sphi1 = Math.sin(phi1);
        double u1 = sxyp2.parameter(SurfXYPlane.DISTNORM);

        double a = (cphi0 - dvdu0 * sphi0) * du0;
        double b = (sphi0 + dvdu0 * cphi0) * du0;
        double c = dzdu0 * du0;

        double x0 = u0 * cphi0 - v0 * sphi0;
        double y0 = u0 * sphi0 + v0 * cphi0;

        double ap = u1 * cphi1;
        double bp = u1 * sphi1;
        double cp = 0;

        double xp = ap;
        double yp = bp;
        double zp = 0.0;

        double denom = a * ap + b * bp + c * cp;

        if (denom == 0.)
            return pstat;

        double S = ((xp - x0) * ap + (yp - y0) * bp + (zp - z0) * cp) / denom;

        double x1 = x0 + S * a;
        double y1 = y0 + S * b;
        double z1 = z0 + S * c;

        boolean forward = S > 0. ? true : false;

        if (dir == PropDir.FORWARD && !forward)
            return pstat;
        if (dir == PropDir.BACKWARD && forward)
            return pstat;

        double v1 = y1 * cphi1 - x1 * sphi1;

        double v01 = y0 * cphi1 - x0 * sphi1;
        double u01 = y0 * sphi1 + x0 * cphi1;
        double z01 = z0;

        if (u01 == u1)
            return pstat;

        double dvdu1 = (v1 - v01) / (u1 - u01);
        double dzdu1 = (z1 - z01) / (u1 - u01);

        vec.set(IV, v1);
        vec.set(IZ, z1);
        vec.set(IDVDU, dvdu1);
        vec.set(IDZDU, dzdu1);

        // Update trv
        trv.setSurface(sxyp2.newPureSurface());
        trv.setVector(vec);
        // set new direction of the track
        if (b * sphi1 + a * cphi1 > 0)
            trv.setForward();
        else
            trv.setBackward();

        // Calculate s.
        double ds = S * Math.sqrt(a * a + b * b + c * c);

        // Set the return status.
        pstat.setPathDistance(ds);

        if (deriv == null)
            return pstat;

        double dx0dv0 = -sphi0;
        double dy0dv0 = cphi0;

        double dadv_du = -sphi0 * du0;
        double dbdv_du = cphi0 * du0;
        double dcdz_du = du0;

        double ddenomdv_du = dadv_du * ap + dbdv_du * bp;
        double ddenomdz_du = dcdz_du * cp;


        double dSdv0 = -(dx0dv0 * ap + dy0dv0 * bp) / denom;
        double dSdz0 = -cp / denom;
        double dSdv_du = -S / denom * ddenomdv_du;
        double dSdz_du = -S / denom * ddenomdz_du;

        double dy1dv0 = dy0dv0 + dSdv0 * b;
        double dx1dv0 = dx0dv0 + dSdv0 * a;
        double dx1dv_du = dSdv_du * a + S * dadv_du;
        double dy1dv_du = dSdv_du * b + S * dbdv_du;

        double du01dv0 = dy0dv0 * sphi1 + dx0dv0 * cphi1;
        double dv01dv0 = dy0dv0 * cphi1 - dx0dv0 * sphi1;

        double dv1dv0 = dy1dv0 * cphi1 - dx1dv0 * sphi1;
        double dv1dv_du = dy1dv_du * cphi1 - dx1dv_du * sphi1;

        double dz1dz0 = 1 + dSdz0 * c;
        double dz1dv0 = dSdv0 * c;
        double dz1dv_du = dSdv_du * c;
        double dz1dz_du = dSdz_du * c + S * dcdz_du;

        double dv_du1dv0 = ((dv1dv0 - dv01dv0) * (u1 - u01) + du01dv0 * (v1 - v01)) / (u1 - u01) / (u1 - u01);
        double dv_du1dv_du = dv1dv_du / (u1 - u01);

        double dz_du1dv0 = (dz1dv0 * (u1 - u01) + du01dv0 * (z1 - z01)) / (u1 - u01) / (u1 - u01);
        double dz_du1dz0 = (dz1dz0 - 1.) / (u1 - u01);
        double dz_du1dv_du = dz1dv_du / (u1 - u01);
        double dz_du1dz_du = dz1dz_du / (u1 - u01);

        //Set the track derivatives...
        deriv.setIdentity();

        deriv.set(IV, IV, dv1dv0);
        deriv.set(IV, IDVDU, dv1dv_du);

        deriv.set(IZ, IV, dz1dv0);
        deriv.set(IZ, IZ, dz1dz0);
        deriv.set(IZ, IDVDU, dz1dv_du);
        deriv.set(IZ, IDZDU, dz1dz_du);

        deriv.set(IDVDU, IV, dv_du1dv0);
        deriv.set(IDVDU, IDVDU, dv_du1dv_du);

        deriv.set(IDZDU, IV, dz_du1dv0);
        deriv.set(IDZDU, IZ, dz_du1dz0);
        deriv.set(IDZDU, IDVDU, dz_du1dv_du);
        deriv.set(IDZDU, IDZDU, dz_du1dz_du);

        deriv.set(IQP, IQP, 1.0);

        return pstat;

    }
    //cng
}
